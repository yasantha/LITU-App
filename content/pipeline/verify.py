#!/usr/bin/env python3
"""Step 3b: AI fact-check of drafts before human review.

    GEMINI_API_KEY=... python verify.py --section CH3-TUD
    python verify.py --all

A second model checks each unverified draft against what the 3rd edition handbook says. Passing
drafts are marked "ai_checked"; failing drafts move to drafts/rejected/<section>.json with the
reason, so they never reach review. This does not replace checking against the handbook itself.
"""
from __future__ import annotations

import argparse
import json
import sys
from typing import Literal

from pydantic import BaseModel

from common import DRAFTS_DIR, PROMPTS_DIR, load_syllabus, sections_by_id, write_drafts
from gemini_util import generate_json

# A different model from the drafting one, so the check is independent.
DEFAULT_MODEL = "gemini-3.7-flash"
BATCH = 15
VERIFY_FALLBACKS = ("gemini-3.5-flash", "gemini-3.1-flash-lite", "gemini-flash-lite-latest")


class Verdict(BaseModel):
    id: str
    verdict: Literal["pass", "fail"]
    reason: str


class Verdicts(BaseModel):
    results: list[Verdict]


def describe(q: dict) -> str:
    opts = "\n".join(f"  {'[correct] ' if o['correct'] else ''}{o['label']}" for o in q["options"])
    return f"ID {q['id']} ({q['type']}, {q['handbook_ref']})\nQ: {q['stem']}\n{opts}\nExplanation: {q['explanation']}"


def verify_section(section_id: str, model: str) -> tuple[int, int]:
    path = DRAFTS_DIR / f"{section_id}.json"
    if not path.exists():
        return 0, 0
    drafts = json.loads(path.read_text(encoding="utf-8"))
    todo = [d for d in drafts if not d.get("ai_checked")]
    if not todo:
        return 0, 0
    system = (PROMPTS_DIR / "verify_questions.md").read_text(encoding="utf-8")
    verdicts: dict[str, Verdict] = {}
    for i in range(0, len(todo), BATCH):
        batch = todo[i:i + BATCH]
        user = "Check these questions. Return one result per ID.\n\n" + "\n\n".join(describe(q) for q in batch)
        for v in generate_json(model, system, user, Verdicts, fallbacks=VERIFY_FALLBACKS).results:
            verdicts[v.id] = v
    kept, rejected = [], []
    for d in drafts:
        v = verdicts.get(d["id"])
        if d.get("ai_checked") or (v and v.verdict == "pass"):
            kept.append({**d, "ai_checked": True})
        elif v is None:
            kept.append(d)  # no verdict returned; leave unchecked for the next run
        else:
            rejected.append({**d, "rejected_reason": v.reason})
    write_drafts(section_id, kept)
    if rejected:
        rdir = DRAFTS_DIR / "rejected"
        rdir.mkdir(exist_ok=True)
        rpath = rdir / f"{section_id}.json"
        old = json.loads(rpath.read_text(encoding="utf-8")) if rpath.exists() else []
        rpath.write_text(json.dumps(old + rejected, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return len(todo) - len(rejected), len(rejected)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    group = ap.add_mutually_exclusive_group(required=True)
    group.add_argument("--section")
    group.add_argument("--all", action="store_true")
    ap.add_argument("--model", default=DEFAULT_MODEL)
    args = ap.parse_args()
    sections = list(sections_by_id(load_syllabus())) if args.all else [args.section]
    for sid in sections:
        passed, failed = verify_section(sid, args.model)
        if passed or failed:
            print(f"{sid}: {passed} passed, {failed} rejected")
    return 0


if __name__ == "__main__":
    sys.exit(main())

#!/usr/bin/env python3
"""Coverage check: which handbook facts have no question yet (and optionally fill the gaps).

    GEMINI_API_KEY=... python coverage.py              # report only -> review/coverage.md
    python coverage.py --fill                           # also draft questions for the gaps

For each section a model lists the facts the 3rd edition handbook covers there and marks which our
questions already test. Gaps are drafted with generate.py's prompt, aimed at the missing facts, then
must still pass verify.py. Like the rest of the pipeline this is AI-based: it finds missing topics
systematically but cannot read the handbook, so a person should still compare against the book.
"""
from __future__ import annotations

import argparse
import json
import sys

from pydantic import BaseModel

from common import CONTENT_DIR, DRAFTS_DIR, PROMPTS_DIR, handbook_ref, load_drafts, load_syllabus, sections_by_id
from gemini_util import generate_json

DEFAULT_MODEL = "gemini-3.8-flash"
FALLBACKS = ("gemini-3.6-flash", "gemini-3.7-flash", "gemini-3.5-flash", "gemini-3-flash-preview", "gemini-flash-latest")
REPORT = CONTENT_DIR / "review" / "coverage.md"
GAPS = CONTENT_DIR / "review" / "coverage_gaps.json"


class Fact(BaseModel):
    fact: str
    covered: bool


class Facts(BaseModel):
    facts: list[Fact]


def check_section(section: dict, stems: list[str], model: str) -> list[Fact]:
    system = (PROMPTS_DIR / "coverage.md").read_text(encoding="utf-8")
    user = f"Handbook section: {handbook_ref(section)}\n\nExisting questions:\n" + "\n".join(f"- {s}" for s in stems)
    return generate_json(model, system, user, Facts, fallbacks=FALLBACKS).facts


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--fill", action="store_true", help="draft questions for uncovered facts")
    ap.add_argument("--section", help="only this section")
    ap.add_argument("--model", default=DEFAULT_MODEL)
    args = ap.parse_args()

    sections = sections_by_id(load_syllabus())
    drafts = load_drafts()
    wanted = [args.section] if args.section else list(sections)
    gaps = json.loads(GAPS.read_text()) if GAPS.exists() else {}
    lines = ["# Handbook coverage", "",
             "AI estimate of the facts each 3rd edition section covers, and whether a question tests them.",
             "Check against the handbook itself before launch.", ""]
    total = covered = 0
    for sid in wanted:
        stems = [d["stem"] for d in drafts if d["section_id"] == sid]
        facts = check_section(sections[sid], stems, args.model)
        done = [f for f in facts if f.covered]
        missing = [f.fact for f in facts if not f.covered]
        total += len(facts)
        covered += len(done)
        gaps[sid] = missing
        pct = 100 * len(done) // max(len(facts), 1)
        print(f"{sid}: {len(done)}/{len(facts)} facts covered ({pct}%), {len(stems)} questions")
        lines += [f"## {handbook_ref(sections[sid])} — {len(done)}/{len(facts)} covered", ""]
        lines += [f"- [x] {f.fact}" for f in done] + [f"- [ ] {m}" for m in missing] + [""]
    lines.insert(4, f"**Overall: {covered}/{total} facts covered ({100 * covered // max(total, 1)}%).**\n")
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    if not args.section:
        REPORT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    GAPS.write_text(json.dumps(gaps, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Overall: {covered}/{total} facts covered. Report: {REPORT.relative_to(CONTENT_DIR)}")

    if args.fill:
        import subprocess
        for sid in wanted:
            missing = gaps.get(sid, [])
            if not missing:
                continue
            print(f"{sid}: drafting questions for {len(missing)} uncovered facts")
            subprocess.run([sys.executable, "generate.py", "--section", sid, "--count", str(len(missing)),
                            "--focus", json.dumps(missing)], check=False, cwd=DRAFTS_DIR.parent / "pipeline")
    return 0


if __name__ == "__main__":
    sys.exit(main())

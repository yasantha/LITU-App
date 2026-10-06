#!/usr/bin/env python3
"""Step 2: draft questions for one handbook section with an AI model.

    GEMINI_API_KEY=... python generate.py --section CH3-TUD --count 40
    python generate.py --section CH3-TUD --provider claude      # uses ANTHROPIC_API_KEY

Drafts are appended to drafts/<section>.json. Rejected questions in review/review.csv that have a
reviewer note are sent back as feedback so the next batch avoids the same problems. Nothing here
reaches users: every draft goes through validate.py and content review first.

Gemini (default) needs GEMINI_API_KEY from Google AI Studio; its free tier is enough for drafting.
Claude needs ANTHROPIC_API_KEY. The model is open decision 3 in the spec; override it with --model.
"""
from __future__ import annotations

import argparse
import json
import sys
from typing import Literal

from pydantic import BaseModel, Field

from common import (
    PROMPTS_DIR, handbook_ref, ID_RE, load_drafts, load_review, load_syllabus,
    released_question_ids, sections_by_id, write_drafts,
)

DEFAULT_MODELS = {"gemini": "gemini-3.8-flash", "claude": "claude-opus-5-5"}


class DraftOption(BaseModel):
    label: str = Field(max_length=70)
    correct: bool


class DraftQuestion(BaseModel):
    type: Literal["single", "multi", "truefalse"]
    stem: str = Field(max_length=160)
    options: list[DraftOption]
    explanation: str
    difficulty: int = Field(ge=1, le=3)


class DraftBatch(BaseModel):
    questions: list[DraftQuestion]


def draft_with_gemini(model: str, system: str, user: str) -> DraftBatch:
    from gemini_util import generate_json

    return generate_json(model, system, user, DraftBatch, fallbacks=("gemini-3.6-flash", "gemini-3.7-flash", "gemini-3.5-flash", "gemini-3-flash-preview", "gemini-flash-latest"))


def draft_with_claude(model: str, system: str, user: str) -> DraftBatch:
    import anthropic

    client = anthropic.Anthropic()
    response = client.beta.messages.parse(
        model=model,
        max_tokens=32000,
        system=system,
        messages=[{"role": "user", "content": user}],
        output_format=DraftBatch,
        output_config={"effort": "high"},
        # On a safety decline, the API re-runs the request on a suitable fallback model.
        betas=["server-side-fallback-2026-07-01"],
        fallbacks="default",
    )
    if response.stop_reason == "refusal":
        sys.exit(f"The model declined the request: {response.stop_details}")
    if response.stop_reason == "max_tokens":
        sys.exit("Output was cut off; ask for fewer questions with --count.")
    return response.parsed_output


def next_number(section_id: str, taken: set[str]) -> int:
    nums = [int(m.group(3)) for i in taken if (m := ID_RE.match(i)) and f"{m.group(1)}-{m.group(2)}" == section_id]
    return max(nums, default=0) + 1


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--section", required=True, help="section ID, e.g. CH3-TUD")
    ap.add_argument("--count", type=int, default=40)
    ap.add_argument("--focus", help="JSON list of facts the new questions must test (from coverage.py)")
    ap.add_argument("--provider", choices=sorted(DEFAULT_MODELS), default="gemini")
    ap.add_argument("--model", help="defaults to gemini-3.8-flash or claude-opus-5-5")
    args = ap.parse_args()

    sections = sections_by_id(load_syllabus())
    section = sections.get(args.section)
    if section is None:
        sys.exit(f"Unknown section {args.section}. Known: {', '.join(sections)}")

    all_drafts = load_drafts()
    existing = [d for d in all_drafts if d["section_id"] == args.section]
    # Facts often belong to two sections, so show the whole chapter to avoid cross-section repeats.
    chapter_prefix = args.section.split("-")[0] + "-"
    nearby = [d for d in all_drafts if d["section_id"].startswith(chapter_prefix) and d["section_id"] != args.section]
    review = load_review()
    feedback = [
        f"- {r['stem']} -> {r['Reviewer note']}"
        for r in review.values()
        if r["section_id"] == args.section and r["Status"].strip().lower() == "reject" and r["Reviewer note"].strip()
    ]

    system = (PROMPTS_DIR / "generate_questions.md").read_text(encoding="utf-8")
    user = "\n".join([
        f"Handbook section: {handbook_ref(section)}",
        f"Write {args.count} new questions for this section, mixing about 70% single, 15% multi and 15% truefalse.",
        *(["", "Each new question must test one of these facts, which no question covers yet:",
           *(f"- {f}" for f in json.loads(args.focus))] if args.focus else []),
        "",
        "Existing questions (do not repeat these facts):",
        *(f"- {d['stem']}" for d in existing),
        *(["", "Questions in other sections of this chapter (do not repeat these facts either):",
           *(f"- {d['stem']}" for d in nearby)] if nearby else []),
        *(["", "The reviewer rejected these questions. Learn from the notes:", *feedback] if feedback else []),
    ])

    model = args.model or DEFAULT_MODELS[args.provider]
    draft = draft_with_gemini if args.provider == "gemini" else draft_with_claude
    batch = draft(model, system, user)

    taken = {d["id"] for d in all_drafts} | set(released_question_ids())
    n = next_number(args.section, taken)
    chapter, short = args.section.split("-", 1)
    for q in batch.questions:
        existing.append({
            "id": f"Q-{chapter}-{short}-{n:03d}",
            "section_id": args.section,
            "type": q.type,
            "stem": q.stem,
            "options": [o.model_dump() for o in q.options],
            "explanation": q.explanation,
            "handbook_ref": handbook_ref(section),
            "difficulty": q.difficulty,
        })
        n += 1
    write_drafts(args.section, existing)
    print(f"Added {len(batch.questions)} drafts to drafts/{args.section}.json. Next: python validate.py --export")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""Step 3: automatic checks on drafts, or on a built content.db.

    python validate.py                  # check every draft in drafts/
    python validate.py --export         # ...and write passing drafts to review/review.csv
    python validate.py --db PATH        # check a built content.db (used in CI)

The review CSV is imported into the Google Sheet Amila reviews. Existing Status, Edited text and
Reviewer note values are kept when the CSV is regenerated.
"""
from __future__ import annotations

import argparse
import csv
import json
import re
import sqlite3
import sys
from difflib import SequenceMatcher
from pathlib import Path

from common import (
    AUDIO_PACK_ASSETS, ID_RE, REVIEW_COLUMNS, REVIEW_CSV, TYPES, load_drafts, load_review,
    load_syllabus, release_dbs, sections_by_id,
)

BANNED = re.compile(r"\b(all|none) of the above\b", re.I)
MAX_STEM, MAX_OPTION, MIN_EXPL, MAX_EXPL = 160, 70, 40, 300
DUPLICATE_RATIO = 0.9


def normalise(text: str) -> str:
    return re.sub(r"[^a-z0-9 ]", "", text.lower()).strip()


def syllables(word: str) -> int:
    groups = re.findall(r"[aeiouy]+", word.lower())
    return max(1, len(groups) - (1 if word.lower().endswith("e") and len(groups) > 1 else 0))


def readability_warning(text: str) -> str | None:
    """Rough B2 ceiling: long sentences or many long words suggest text above about B1-B2."""
    sentences = [s for s in re.split(r"[.!?]+", text) if s.strip()]
    words = re.findall(r"[A-Za-z']+", text)
    if not sentences or not words:
        return None
    per_sentence = len(words) / len(sentences)
    hard = sum(1 for w in words if syllables(w) >= 4) / len(words)
    if per_sentence > 22 or hard > 0.12:
        return f"explanation may be above B2 ({per_sentence:.0f} words/sentence, {hard:.0%} long words)"
    return None


def check_question(q: dict, sections: dict) -> tuple[list[str], list[str]]:
    errors, warnings = [], []
    m = ID_RE.match(q.get("id", ""))
    if not m:
        errors.append("ID must look like Q-<chapter>-<section>-<nnn>")
    elif f"{m.group(1)}-{m.group(2)}" != q.get("section_id"):
        errors.append("ID does not match section_id")
    if q.get("section_id") not in sections:
        errors.append(f"unknown section {q.get('section_id')}")
    qtype = q.get("type")
    if qtype not in TYPES:
        errors.append(f"type must be one of {sorted(TYPES)}")
    options = q.get("options", [])
    correct = sum(1 for o in options if o.get("correct"))
    expected = {"single": (4, 1), "multi": (4, 2), "truefalse": (2, 1)}.get(qtype)
    if expected and (len(options), correct) != expected:
        errors.append(f"{qtype} needs {expected[0]} options with {expected[1]} correct, has {len(options)}/{correct}")
    if qtype == "truefalse" and [o.get("label") for o in options] != ["True", "False"]:
        errors.append('truefalse options must be "True" then "False"')
    labels = [normalise(o.get("label", "")) for o in options]
    if len(set(labels)) != len(labels):
        errors.append("duplicate options")
    stem = q.get("stem", "")
    if not stem or len(stem) > MAX_STEM:
        errors.append(f"stem must be 1-{MAX_STEM} characters (is {len(stem)})")
    for o in options:
        if len(o.get("label", "")) > MAX_OPTION:
            errors.append(f"option over {MAX_OPTION} characters: {o['label']!r}")
        if BANNED.search(o.get("label", "")):
            errors.append('no "all/none of the above"')
    if qtype == "multi" and "two" not in stem.lower():
        warnings.append('multi stems should say "Which TWO"')
    expl = q.get("explanation", "")
    if not MIN_EXPL <= len(expl) <= MAX_EXPL:
        errors.append(f"explanation must be {MIN_EXPL}-{MAX_EXPL} characters (is {len(expl)})")
    if (w := readability_warning(expl)):
        warnings.append(w)
    if q.get("difficulty") not in (1, 2, 3):
        errors.append("difficulty must be 1, 2 or 3")
    if not q.get("handbook_ref"):
        errors.append("handbook_ref missing")
    return errors, warnings


def near_duplicates(drafts: list[dict]) -> list[tuple[str, str, float]]:
    found = []
    stems = [(d["id"], normalise(d["stem"])) for d in drafts]
    for i, (a, sa) in enumerate(stems):
        for b, sb in stems[i + 1:]:
            ratio = SequenceMatcher(None, sa, sb).ratio()
            if ratio > DUPLICATE_RATIO:
                found.append((a, b, ratio))
    return found


def validate_drafts(export: bool) -> int:
    sections = sections_by_id(load_syllabus())
    drafts = load_drafts()
    failures = 0
    ids = [d.get("id") for d in drafts]
    dupes = {i for i in ids if ids.count(i) > 1}
    for i in sorted(dupes):
        print(f"ERROR {i}: ID used more than once")
        failures += 1
    passing = []
    for q in drafts:
        errors, warnings = check_question(q, sections)
        for e in errors:
            print(f"ERROR {q.get('id')}: {e}")
        for w in warnings:
            print(f"warn  {q.get('id')}: {w}")
        if errors or q.get("id") in dupes:
            failures += 1
        else:
            passing.append(q)
    for a, b, r in near_duplicates(passing):
        print(f"warn  {a} and {b} have near-duplicate stems ({r:.2f})")
    print(f"{len(passing)} of {len(drafts)} drafts pass")
    if export:
        export_review(passing)
    return 1 if failures else 0


def export_review(passing: list[dict]) -> None:
    previous = load_review()
    REVIEW_CSV.parent.mkdir(parents=True, exist_ok=True)
    with REVIEW_CSV.open("w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=REVIEW_COLUMNS)
        w.writeheader()
        for q in passing:
            old = previous.get(q["id"], {})
            w.writerow({
                "id": q["id"],
                "section_id": q["section_id"],
                "type": q["type"],
                "stem": q["stem"],
                "options": " | ".join(("*" if o["correct"] else "") + o["label"] for o in q["options"]),
                "explanation": q["explanation"],
                "handbook_ref": q["handbook_ref"],
                "difficulty": q["difficulty"],
                "Status": old.get("Status", ""),
                "Edited text": old.get("Edited text", ""),
                "Reviewer note": old.get("Reviewer note", ""),
            })
    print(f"Wrote {REVIEW_CSV.relative_to(REVIEW_CSV.parent.parent)} for review (import into the Google Sheet)")


def validate_db(path: Path, require_reviewed: bool, require_audio: bool = False) -> int:
    con = sqlite3.connect(path)
    con.row_factory = sqlite3.Row
    errors = []
    meta = {r["key"]: r["value"] for r in con.execute("SELECT key, value FROM meta")}
    for key in ("content_version", "schema_version", "built_at", "question_count", "handbook_edition"):
        if key not in meta:
            errors.append(f"meta.{key} missing")
    if require_reviewed and meta.get("review_status") != "approved":
        errors.append(f"content is not fully reviewed (review_status={meta.get('review_status')})")
    sections = {r["id"] for r in con.execute("SELECT id FROM section")}
    rows = list(con.execute("SELECT * FROM question"))
    for q in rows:
        opts = list(con.execute("SELECT * FROM answer_option WHERE question_id = ? ORDER BY sort", (q["id"],)))
        draft = {
            "id": q["id"], "section_id": q["section_id"], "type": q["type"], "stem": q["stem"],
            "explanation": q["explanation"], "handbook_ref": q["handbook_ref"], "difficulty": q["difficulty"],
            "options": [{"label": o["label"], "correct": bool(o["is_correct"])} for o in opts],
        }
        errs, _ = check_question(draft, {s: None for s in sections})
        errors += [f"{q['id']}: {e}" for e in errs]
    # No deleted IDs: everything in the newest earlier release must still be present.
    for version, release in release_dbs():
        if release.resolve() == path.resolve() or version >= int(meta.get("content_version", 0)):
            continue
        old = sqlite3.connect(release)
        old_ids = {r[0] for r in old.execute("SELECT id FROM question")}
        missing = old_ids - {q["id"] for q in rows}
        errors += [f"{i}: deleted (retire with active = 0 instead)" for i in sorted(missing)]
    # Audio keys resolve once tts.py has generated the pack (it writes a manifest); releases require it.
    manifest = AUDIO_PACK_ASSETS.parent / "audio_manifest.json"
    if require_audio and not manifest.exists():
        errors.append("audio pack not generated: run content/pipeline/tts.py")
    if manifest.exists():
        for q in rows:
            if q["active"] and q["audio_key"] and not (AUDIO_PACK_ASSETS / "audio" / q["audio_key"]).exists():
                errors.append(f"{q['id']}: audio {q['audio_key']} missing from audio_pack")
    fk = list(con.execute("PRAGMA foreign_key_check"))
    if fk:
        errors.append(f"{len(fk)} foreign key violations")
    if con.execute("PRAGMA integrity_check").fetchone()[0] != "ok":
        errors.append("integrity_check failed")
    sample = json.loads(meta.get("sample_question_ids", "[]"))
    active = {q["id"] for q in rows if q["active"]}
    errors += [f"sample question {s} is not active" for s in sample if s not in active]
    for e in errors:
        print(f"ERROR {e}")
    print(f"{path.name}: content v{meta.get('content_version')}, {len(active)} active questions, {len(errors)} errors")
    return 1 if errors else 0


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--export", action="store_true", help="write passing drafts to review/review.csv")
    ap.add_argument("--db", type=Path, help="validate a built content.db instead of the drafts")
    ap.add_argument("--require-reviewed", action="store_true", help="fail unless every question was approved (release builds)")
    ap.add_argument("--require-audio", action="store_true", help="fail unless the audio pack covers every question (release builds)")
    args = ap.parse_args()
    if args.db:
        return validate_db(args.db, args.require_reviewed, args.require_audio)
    return validate_drafts(args.export)


if __name__ == "__main__":
    sys.exit(main())

#!/usr/bin/env python3
"""Step 5: build content.db from approved questions and section notes.

    python build_db.py                      # approved rows only (release)
    python build_db.py --allow-unreviewed   # also drafts with no Status yet (development only)

Creates the tables from the schema Room exports for core/content, so the bundled file always
passes Room's prepackaged-database check. Questions from earlier releases that are no longer
approved are kept with active = 0: IDs are never deleted or reused (spec section 8.3).

Writes releases/content-v<N>.db, appends releases/CHANGELOG.md, and copies the database and its
version marker to app/src/main/assets/content/.
"""
from __future__ import annotations

import argparse
import json
import shutil
import sqlite3
import sys
from datetime import datetime, timezone

from common import (
    APP_ASSETS, RELEASES_DIR, ROOM_SCHEMA, SAMPLE, load_drafts, load_notes, load_review,
    load_syllabus, release_dbs, released_question_ids, text_hash,
)


def apply_review(draft: dict, row: dict | None, allow_unreviewed: bool) -> dict | None:
    status = (row or {}).get("Status", "").strip().lower()
    if status == "reject":
        return None
    if status == "edit":
        edited = row.get("Edited text", "").strip()
        if edited.startswith("{"):
            # JSON with any fields to override, e.g. {"stem": "...", "explanation": "..."}
            return {**draft, **json.loads(edited)}
        return {**draft, "stem": edited} if edited else draft
    if status == "approve":
        return draft
    return draft if allow_unreviewed else None


def create_schema(con: sqlite3.Connection) -> None:
    schema = json.loads(ROOM_SCHEMA.read_text(encoding="utf-8"))["database"]
    for entity in schema["entities"]:
        name = entity["tableName"]
        con.execute(entity["createSql"].replace("${TABLE_NAME}", name))
        for index in entity.get("indices", []):
            con.execute(index["createSql"].replace("${TABLE_NAME}", name))
    for sql in schema["setupQueries"]:
        con.execute(sql)
    con.execute(
        "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, ?)",
        (schema["identityHash"],),
    )
    con.execute(f"PRAGMA user_version = {schema['version']}")


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--allow-unreviewed", action="store_true", help="include drafts not yet reviewed (never for release)")
    ap.add_argument("--version", type=int, help="content_version to write (default: previous + 1)")
    args = ap.parse_args()

    syllabus = load_syllabus()
    review = load_review()
    drafts = load_drafts()
    notes = load_notes()
    previous = released_question_ids()
    releases = release_dbs()
    version = args.version or (releases[-1][0] + 1 if releases else 1)
    if any(v == version for v, _ in releases):
        sys.exit(f"content-v{version}.db already exists; pass a new --version")

    chosen = [q for d in drafts if (q := apply_review(d, review.get(d["id"]), args.allow_unreviewed))]
    unreviewed = sum(1 for d in drafts if not review.get(d["id"], {}).get("Status", "").strip())
    review_status = "approved" if not args.allow_unreviewed or unreviewed == 0 else "unreviewed"

    RELEASES_DIR.mkdir(parents=True, exist_ok=True)
    out = RELEASES_DIR / f"content-v{version}.db"
    tmp = out.with_suffix(".tmp")
    tmp.unlink(missing_ok=True)
    con = sqlite3.connect(tmp)
    con.execute("PRAGMA foreign_keys = ON")
    create_schema(con)

    for ch in syllabus["chapters"]:
        con.execute("INSERT INTO chapter (id, number, title, in_test) VALUES (?, ?, ?, ?)",
                    (ch["id"], ch["number"], ch["title"], 1 if ch["in_test"] else 0))
        for sort, s in enumerate(ch["sections"]):
            con.execute("INSERT INTO section (id, chapter_id, title, sort) VALUES (?, ?, ?, ?)",
                        (s["id"], ch["id"], s["title"], sort))

    added, changed = [], []
    chosen_ids = set()
    for q in chosen:
        chosen_ids.add(q["id"])
        old = previous.get(q["id"])
        added_in = old["added_in"] if old else version
        if not old:
            added.append(q["id"])
        elif text_hash(old["stem"], old["explanation"]) != text_hash(q["stem"], q["explanation"]):
            changed.append(q["id"])
        con.execute(
            "INSERT INTO question (id, section_id, type, stem, explanation, handbook_ref, difficulty, audio_key, active, added_in)"
            " VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, ?)",
            (q["id"], q["section_id"], q["type"], q["stem"], q["explanation"], q["handbook_ref"],
             q["difficulty"], f"q/{q['id']}.opus", added_in),
        )
        for sort, o in enumerate(q["options"]):
            con.execute(
                "INSERT INTO answer_option (id, question_id, label, is_correct, sort) VALUES (?, ?, ?, ?, ?)",
                (f"{q['id']}-{chr(ord('A') + sort)}", q["id"], o["label"], 1 if o["correct"] else 0, sort),
            )

    # Retire, never delete: carry earlier questions forward as inactive.
    retired = []
    for qid, old in previous.items():
        if qid in chosen_ids:
            continue
        retired.append(qid)
        con.execute(
            "INSERT INTO question (id, section_id, type, stem, explanation, handbook_ref, difficulty, audio_key, active, added_in)"
            " VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?)",
            (qid, old["section_id"], old["type"], old["stem"], old["explanation"], old["handbook_ref"],
             old["difficulty"], old["audio_key"], old["added_in"]),
        )
        for _, release in reversed(release_dbs()):
            src = sqlite3.connect(release)
            opts = src.execute("SELECT id, label, is_correct, sort FROM answer_option WHERE question_id = ?", (qid,)).fetchall()
            src.close()
            if opts:
                con.executemany("INSERT INTO answer_option (id, question_id, label, is_correct, sort) VALUES (?, ?, ?, ?, ?)",
                                [(i, qid, label, c, s) for i, label, c, s in opts])
                break

    for section_id, note in notes.items():
        con.execute("INSERT INTO note (section_id, body_md, key_facts, audio_key) VALUES (?, ?, ?, ?)",
                    (section_id, note["body_md"], json.dumps(note["key_facts"], ensure_ascii=False), f"n/{section_id}.opus"))

    sample = [i for i in json.loads(SAMPLE.read_text(encoding="utf-8")) if i in chosen_ids] if SAMPLE.exists() else []
    meta = {
        "content_version": str(version),
        "schema_version": str(json.loads(ROOM_SCHEMA.read_text())["database"]["version"]),
        "built_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "question_count": str(len(chosen_ids)),
        "handbook_edition": syllabus["handbook_edition"],
        "sample_question_ids": json.dumps(sample),
        "review_status": review_status,
    }
    con.executemany("INSERT INTO meta (key, value) VALUES (?, ?)", meta.items())
    con.commit()

    problems = con.execute("PRAGMA foreign_key_check").fetchall()
    ok = con.execute("PRAGMA integrity_check").fetchone()[0]
    con.execute("VACUUM")
    con.close()
    if problems or ok != "ok":
        tmp.unlink()
        sys.exit(f"Integrity check failed: {problems or ok}")
    tmp.replace(out)

    with (RELEASES_DIR / "CHANGELOG.md").open("a", encoding="utf-8") as log:
        log.write(f"\n## Content v{version} ({meta['built_at'][:10]})\n\n")
        log.write(f"- {len(chosen_ids)} active questions, {len(notes)} section notes, review status: {review_status}\n")
        log.write(f"- Added {len(added)}, changed {len(changed)}, retired {len(retired)}\n")
        for label, ids in (("Changed", changed), ("Retired", retired)):
            if ids:
                log.write(f"- {label}: {', '.join(ids)}\n")

    APP_ASSETS.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(out, APP_ASSETS / "content.db")
    (APP_ASSETS / "content_version.txt").write_text(f"{version}\n", encoding="utf-8")
    print(f"Built {out.name}: {len(chosen_ids)} active, {len(added)} added, {len(changed)} changed, "
          f"{len(retired)} retired, review status {review_status}. Copied to app assets.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

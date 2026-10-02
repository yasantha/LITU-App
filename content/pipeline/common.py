"""Shared paths and helpers for the content pipeline (spec section 10)."""
from __future__ import annotations

import csv
import hashlib
import json
import re
import sqlite3
from pathlib import Path

CONTENT_DIR = Path(__file__).resolve().parent.parent
REPO_DIR = CONTENT_DIR.parent
DRAFTS_DIR = CONTENT_DIR / "drafts"
NOTES_DIR = CONTENT_DIR / "notes"
PROMPTS_DIR = CONTENT_DIR / "prompts"
REVIEW_CSV = CONTENT_DIR / "review" / "review.csv"
RELEASES_DIR = CONTENT_DIR / "releases"
SYLLABUS = CONTENT_DIR / "syllabus.json"
SAMPLE = CONTENT_DIR / "sample.json"
ROOM_SCHEMA = (
    REPO_DIR / "core/content/schemas/com.myday.litu.core.content.db.ContentDatabase/1.json"
)
APP_ASSETS = REPO_DIR / "app/src/main/assets/content"
AUDIO_PACK_ASSETS = REPO_DIR / "audio_pack/src/main/assets"

ID_RE = re.compile(r"^Q-(CH[1-9])-([A-Z0-9]{2,4})-(\d{3})$")
TYPES = {"single", "multi", "truefalse"}
REVIEW_COLUMNS = [
    "id", "section_id", "type", "stem", "options", "explanation", "handbook_ref", "difficulty",
    "Status", "Edited text", "Reviewer note",
]


def load_syllabus() -> dict:
    return json.loads(SYLLABUS.read_text(encoding="utf-8"))


def sections_by_id(syllabus: dict) -> dict[str, dict]:
    out = {}
    for ch in syllabus["chapters"]:
        for s in ch["sections"]:
            out[s["id"]] = {**s, "chapter": ch}
    return out


def handbook_ref(section: dict) -> str:
    return f"Chapter {section['chapter']['number']} · {section['title']}"


def load_drafts() -> list[dict]:
    drafts = []
    for path in sorted(DRAFTS_DIR.glob("*.json")):
        drafts.extend(json.loads(path.read_text(encoding="utf-8")))
    return drafts


def write_drafts(section_id: str, drafts: list[dict]) -> None:
    path = DRAFTS_DIR / f"{section_id}.json"
    path.write_text(json.dumps(drafts, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def load_review() -> dict[str, dict]:
    if not REVIEW_CSV.exists():
        return {}
    with REVIEW_CSV.open(newline="", encoding="utf-8") as f:
        return {row["id"]: row for row in csv.DictReader(f)}


def load_notes() -> dict[str, dict]:
    return {
        p.stem: json.loads(p.read_text(encoding="utf-8")) for p in sorted(NOTES_DIR.glob("*.json"))
    }


def release_dbs() -> list[tuple[int, Path]]:
    found = []
    for p in RELEASES_DIR.glob("content-v*.db"):
        m = re.match(r"content-v(\d+)\.db$", p.name)
        if m:
            found.append((int(m.group(1)), p))
    return sorted(found)


def released_question_ids() -> dict[str, dict]:
    """Every question ID ever released, with its row from the newest release containing it."""
    out: dict[str, dict] = {}
    for _, path in release_dbs():
        con = sqlite3.connect(path)
        con.row_factory = sqlite3.Row
        for row in con.execute("SELECT * FROM question"):
            out[row["id"]] = dict(row)
        con.close()
    return out


def text_hash(*parts: str) -> str:
    return hashlib.sha256("␟".join(parts).encode("utf-8")).hexdigest()[:16]


def option_letters(n: int) -> list[str]:
    return [chr(ord("A") + i) for i in range(n)]

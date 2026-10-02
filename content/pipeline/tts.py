#!/usr/bin/env python3
"""Step 5b: voice new or changed text for the audio pack (spec section 11).

    ELEVENLABS_API_KEY=... python tts.py --voice <voice_id> [--content-db PATH]

Clips (Opus in Ogg, mono, 24 kbps, -16 LUFS) are written to audio_pack/src/main/assets/audio/:
    q/<questionId>.opus   question stem
    o/<optionId>.opus     each answer option
    e/<questionId>.opus   explanation
    n/<sectionId>.opus    section note
    l/A.opus ... l/D.opus option letters
Options are shuffled at display time, so the app queues letter and option clips in the order shown.

A manifest of text hashes makes runs incremental: only new or changed text is re-voiced. The TTS
service and voice are open decision 3 in the spec; this script targets the ElevenLabs REST API.
Needs ffmpeg on PATH.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import sqlite3
import subprocess
import sys
import tempfile
from pathlib import Path

import requests

from common import APP_ASSETS, AUDIO_PACK_ASSETS, option_letters, text_hash

API = "https://api.elevenlabs.io/v1/text-to-speech/{voice}"
MANIFEST = AUDIO_PACK_ASSETS.parent / "audio_manifest.json"


def markdown_to_speech(md: str) -> str:
    text = re.sub(r"[#*_>`]", "", md)
    return re.sub(r"\s+", " ", text).strip()


def clips(con: sqlite3.Connection) -> dict[str, str]:
    con.row_factory = sqlite3.Row
    out = {f"l/{letter}.opus": f"Option {letter}." for letter in option_letters(4)}
    for q in con.execute("SELECT id, stem, explanation FROM question WHERE active = 1"):
        out[f"q/{q['id']}.opus"] = q["stem"]
        out[f"e/{q['id']}.opus"] = q["explanation"]
        for o in con.execute("SELECT id, label FROM answer_option WHERE question_id = ?", (q["id"],)):
            out[f"o/{o['id']}.opus"] = o["label"]
    for n in con.execute("SELECT section_id, body_md, key_facts FROM note"):
        facts = " ".join(json.loads(n["key_facts"]))
        out[f"n/{n['section_id']}.opus"] = markdown_to_speech(n["body_md"]) + " Key facts. " + facts
    return out


def synthesise(text: str, voice: str, key: str, model: str) -> bytes:
    r = requests.post(
        API.format(voice=voice),
        headers={"xi-api-key": key, "accept": "audio/mpeg"},
        json={"text": text, "model_id": model, "language_code": "en"},
        timeout=120,
    )
    r.raise_for_status()
    return r.content


def to_opus(mp3: bytes, dest: Path) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(suffix=".mp3") as src:
        src.write(mp3)
        src.flush()
        subprocess.run(
            ["ffmpeg", "-y", "-loglevel", "error", "-i", src.name,
             "-af", "loudnorm=I=-16:TP=-1.5:LRA=11", "-ac", "1", "-ar", "48000",
             "-c:a", "libopus", "-b:a", "24k", "-f", "ogg", str(dest)],
            check=True,
        )


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--voice", required=True, help="British English voice ID")
    ap.add_argument("--model", default="eleven_multilingual_v2")
    ap.add_argument("--content-db", type=Path, default=APP_ASSETS / "content.db")
    ap.add_argument("--dry-run", action="store_true", help="list what would be voiced")
    args = ap.parse_args()
    key = os.environ.get("ELEVENLABS_API_KEY")
    if not key and not args.dry_run:
        sys.exit("Set ELEVENLABS_API_KEY")

    manifest = json.loads(MANIFEST.read_text()) if MANIFEST.exists() else {}
    wanted = clips(sqlite3.connect(args.content_db))
    root = AUDIO_PACK_ASSETS / "audio"
    todo = {k: t for k, t in wanted.items() if manifest.get(k) != text_hash(t, args.voice) or not (root / k).exists()}
    print(f"{len(wanted)} clips, {len(todo)} new or changed")
    if args.dry_run:
        return 0
    for i, (k, text) in enumerate(sorted(todo.items()), 1):
        to_opus(synthesise(text, args.voice, key, args.model), root / k)
        manifest[k] = text_hash(text, args.voice)
        if i % 25 == 0:
            MANIFEST.write_text(json.dumps(manifest, indent=1, sort_keys=True))
            print(f"  {i}/{len(todo)}")
    MANIFEST.write_text(json.dumps(manifest, indent=1, sort_keys=True))
    size = sum(p.stat().st_size for p in root.rglob("*.opus")) / 1_000_000
    print(f"Done. Audio pack is {size:.1f} MB (budget 80 MB)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

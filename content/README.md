# Content pipeline

AI drafts the question bank, Amila approves every question, and a script turns the approved set
into `content.db` (spec section 10). Nothing reaches users without approval.

```
syllabus.json ─► generate.py ─► drafts/*.json ─► validate.py --export ─► review/review.csv
                                                                              │ (Google Sheet)
app/src/main/assets/content/ ◄── build_db.py ◄── review/review.csv + notes/*.json
audio_pack/src/main/assets/audio/ ◄── tts.py
```

| Step | Command | Notes |
|---|---|---|
| 1 Syllabus | edit `syllabus.json` | Chapter and section IDs are stable forever. |
| 2 Draft | `python pipeline/generate.py --section CH3-TUD --count 40` | Needs `ANTHROPIC_API_KEY`. Rejected rows with a reviewer note are fed back. |
| 3 Check | `python pipeline/validate.py --export` | Schema, answer counts, unique IDs, lengths, near-duplicates, readability. Writes `review/review.csv`. |
| 4 Review | Import `review.csv` into the review Google Sheet, then download it back to the same path. | `Status` is `approve`, `edit` or `reject`. For `edit`, put the new stem in `Edited text`, or a JSON object such as `{"explanation": "..."}`. |
| 5 Build | `python pipeline/build_db.py` | Approved rows only. Writes `releases/content-v<N>.db`, appends `releases/CHANGELOG.md` and copies the database into the app. |
| 5b Audio | `python pipeline/tts.py --voice <id>` | Needs `ELEVENLABS_API_KEY` and ffmpeg. Only new or changed text is voiced. |
| 6 Release | commit, tag, CI | `release.yml` runs `validate.py --db ... --require-reviewed`. |

Setup: `python3 -m venv .venv && .venv/bin/pip install -r requirements.txt`.

## Rules

- A question ID (`Q-CH3-TUD-014`) never changes and is never reused. Fixing a typo keeps the ID.
- Retire a question by rejecting it; `build_db.py` keeps it with `active = 0`. Never delete rows.
- All wording is original. Do not copy text from the official handbook or other prep sites.
- To hide a faulty question immediately, add its ID to the Remote Config key `hidden_question_ids`,
  then fix it in the next content release.

## Current state

`content-v1.db` is a **development seed**: 161 original draft questions and 23 section notes,
built with `--allow-unreviewed` (`meta.review_status = unreviewed`). Amila must review every row
before the first release; the release workflow refuses unreviewed content.

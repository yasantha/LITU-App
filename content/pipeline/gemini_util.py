"""Gemini calls with retries for the free tier's per-minute limits."""
from __future__ import annotations

import re
import sys
import time


class Overloaded(Exception):
    pass


def generate_json(model: str, system: str, user: str, schema, fallbacks: tuple[str, ...] = ()):
    """Calls Gemini with a Pydantic response schema; moves to the next model if one stays overloaded."""
    for m in (model, *fallbacks):
        try:
            return _generate_json(m, system, user, schema)
        except Overloaded:
            print(f"  {m} is overloaded; trying the next model", file=sys.stderr)
    sys.exit("Every Gemini model was overloaded; try again later")


def _generate_json(model: str, system: str, user: str, schema, attempts: int = 4):
    from google import genai
    from google.genai import errors, types

    client = genai.Client()  # reads GEMINI_API_KEY
    for attempt in range(1, attempts + 1):
        try:
            response = client.models.generate_content(
                model=model,
                contents=user,
                config=types.GenerateContentConfig(
                    system_instruction=system,
                    response_mime_type="application/json",
                    response_schema=schema,
                ),
            )
            if response.parsed is not None:
                return response.parsed
            print(f"  empty or invalid JSON (attempt {attempt}), retrying", file=sys.stderr)
        except errors.APIError as e:
            if e.code not in (429, 500, 503):
                raise
            if attempt == attempts:
                raise Overloaded() from e
            match = re.search(r"retry in ([0-9.]+)s", str(e)) or re.search(r"'retryDelay': '([0-9]+)s'", str(e))
            wait = float(match.group(1)) + 2 if match else 20 * attempt
            print(f"  rate limited or busy ({e.code}); waiting {wait:.0f}s", file=sys.stderr)
            time.sleep(wait)
    raise Overloaded()

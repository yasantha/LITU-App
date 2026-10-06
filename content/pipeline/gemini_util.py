"""Gemini calls that keep working when the free tier is busy.

When a model is overloaded (503) or rate limited (429), the call moves straight to the next model in
the list instead of waiting. Only when every model is busy does it pause and go round again.
"""
from __future__ import annotations

import sys
import time

ROUNDS = 6
PAUSE_SECONDS = 30
REQUEST_TIMEOUT_MS = 180_000


def _client():
    from google import genai
    from google.genai import types

    # Our own loop handles retries; the SDK's hidden retries would multiply the waiting.
    return genai.Client(http_options=types.HttpOptions(
        timeout=REQUEST_TIMEOUT_MS,
        retry_options=types.HttpRetryOptions(attempts=1),
    ))  # reads GEMINI_API_KEY


def generate_json(model: str, system: str, user: str, schema, fallbacks: tuple[str, ...] = ()):
    """Calls Gemini with a Pydantic response schema and returns the parsed object."""
    from google.genai import errors, types

    client = _client()
    models = [model, *[m for m in fallbacks if m != model]]
    config = types.GenerateContentConfig(
        system_instruction=system,
        response_mime_type="application/json",
        response_schema=schema,
    )
    for round_ in range(1, ROUNDS + 1):
        for m in models:
            try:
                response = client.models.generate_content(model=m, contents=user, config=config)
            except errors.APIError as e:
                if e.code in (429, 500, 503, 504):
                    continue
                raise
            except Exception as e:  # network timeouts and similar
                print(f"  {m}: {type(e).__name__}; trying the next model", file=sys.stderr)
                continue
            if response.parsed is not None:
                if m != model:
                    print(f"  answered by {m}", file=sys.stderr)
                return response.parsed
        if round_ < ROUNDS:
            print(f"  every model busy (round {round_}); waiting {PAUSE_SECONDS}s", file=sys.stderr)
            time.sleep(PAUSE_SECONDS)
    sys.exit("Every Gemini model stayed busy; try again later")

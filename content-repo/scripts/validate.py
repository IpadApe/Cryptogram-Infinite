#!/usr/bin/env python3
"""Validator for the Cryptogram Infinite content repo (design doc section 3.4).

Exit 1 on any hard-rule failure. Run for every PR and every daily publish.

    python scripts/validate.py            # validate corpus + all daily files
    python scripts/validate.py --corpus   # corpus only
"""
from __future__ import annotations

import json
import re
import sys
from collections import Counter
from datetime import date, timedelta
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
QUOTES = ROOT / "corpus" / "quotes.json"
PROFANITY = ROOT / "corpus" / "profanity.txt"
MIN_APP_ID = ROOT / "corpus" / "min_app_corpus_id.txt"
DAILY_DIR = ROOT / "daily"
INDEX = DAILY_DIR / "index.json"

BANDS = [("EASY", 20, 30), ("MEDIUM", 31, 45), ("HARD", 46, 70), ("EXTREME", 71, 100)]
TEXT_RE = re.compile(r"^[A-Za-z ,.!?';:\-\"]+$")
WORD_RE = re.compile(r"[A-Za-z']+")

errors: list[str] = []
warnings: list[str] = []


def err(msg: str) -> None:
    errors.append(msg)


def warn(msg: str) -> None:
    warnings.append(msg)


def band_for(n: int) -> str | None:
    for name, lo, hi in BANDS:
        if lo <= n <= hi:
            return name
    return None


def load_profanity() -> set[str]:
    if not PROFANITY.exists():
        return set()
    return {w.strip().lower() for w in PROFANITY.read_text(encoding="utf-8").splitlines() if w.strip()}


def norm_key(text: str) -> str:
    """Case- and punctuation-insensitive comparison key."""
    return re.sub(r"[^a-z0-9]", "", text.lower())


def validate_corpus() -> dict[int, dict]:
    if not QUOTES.exists():
        err(f"missing {QUOTES}")
        return {}
    rows = json.loads(QUOTES.read_text(encoding="utf-8"))
    if not isinstance(rows, list):
        err("quotes.json must be a JSON array")
        return {}

    profanity = load_profanity()
    ids_seen: set[int] = set()
    text_keys: dict[str, int] = {}
    per_author = Counter()
    per_band = Counter()
    by_id: dict[int, dict] = {}

    prev_id = None
    for i, row in enumerate(rows):
        rid = row.get("id")
        text = (row.get("text") or "").strip()
        author = (row.get("author") or "").strip()
        url = row.get("sourceUrl") or ""
        band = row.get("band")

        if not isinstance(rid, int):
            err(f"row {i}: id must be an int")
            continue
        if rid in ids_seen:
            err(f"id {rid}: duplicate id")
        ids_seen.add(rid)
        if prev_id is not None and rid < prev_id:
            err(f"id {rid}: array not sorted ascending by id")
        prev_id = rid
        by_id[rid] = row

        n = len(text)
        if not (20 <= n <= 100):
            err(f"id {rid}: text length {n} out of 20..100")
        if not TEXT_RE.match(text):
            err(f"id {rid}: text has characters outside the allowed set")
        letters = {c for c in text.lower() if c.isalpha()}
        if len(letters) < 10:
            err(f"id {rid}: fewer than 10 distinct letters")
        if len(WORD_RE.findall(text)) < 3:
            err(f"id {rid}: fewer than 3 words")

        computed = band_for(n)
        if computed is None:
            err(f"id {rid}: length {n} is in no band")
        elif band != computed:
            err(f"id {rid}: band {band!r} != computed {computed!r}")
        if computed:
            per_band[computed] += 1

        if not author:
            err(f"id {rid}: author is empty")
        if not url.startswith("https://"):
            err(f"id {rid}: sourceUrl must start with https://")

        tokens = {t.lower() for t in WORD_RE.findall(text)}
        bad = tokens & profanity
        if bad:
            err(f"id {rid}: profanity {sorted(bad)}")

        key = norm_key(text)
        if key in text_keys:
            err(f"id {rid}: duplicate text of id {text_keys[key]}")
        else:
            text_keys[key] = rid

        per_author[author] += 1

    for author, count in per_author.items():
        if count > 10:
            err(f"author {author!r}: {count} rows (max 10)")

    for name, _, _ in BANDS:
        c = per_band[name]
        if c < 100:
            err(f"band {name}: only {c} rows (hard minimum 100)")
        elif c < 300:
            warn(f"band {name}: only {c} rows (target 300)")

    return by_id


def validate_daily(by_id: dict[int, dict]) -> None:
    if not DAILY_DIR.exists():
        return
    min_app_id = None
    if MIN_APP_ID.exists():
        raw = MIN_APP_ID.read_text(encoding="utf-8").strip()
        if raw:
            min_app_id = int(raw)

    listed: set[str] = set()
    if INDEX.exists():
        listed = set(json.loads(INDEX.read_text(encoding="utf-8")).get("dates", []))

    used_by_date: dict[str, set[int]] = {}
    files = sorted(DAILY_DIR.glob("*.json"))
    for f in files:
        if f.name == "index.json":
            continue
        stem = f.stem
        try:
            file_date = date.fromisoformat(stem)
        except ValueError:
            err(f"{f.name}: filename is not a valid ISO date")
            continue
        data = json.loads(f.read_text(encoding="utf-8"))
        if data.get("date") != stem:
            err(f"{f.name}: date field {data.get('date')!r} != filename")
        puzzles = data.get("puzzles", {})
        picks: set[int] = set()
        for name, _, _ in BANDS:
            if name not in puzzles:
                err(f"{f.name}: missing band {name}")
                continue
            qid = puzzles[name].get("quoteId")
            row = by_id.get(qid)
            if row is None:
                err(f"{f.name}/{name}: quoteId {qid} not in corpus")
                continue
            if row["band"] != name:
                err(f"{f.name}/{name}: quoteId {qid} is band {row['band']}")
            if min_app_id is not None and qid > min_app_id:
                err(f"{f.name}/{name}: quoteId {qid} > min_app_corpus_id {min_app_id}")
            picks.add(qid)
        used_by_date[stem] = picks
        if stem not in listed:
            err(f"{f.name}: not listed in index.json")

    # No quoteId reused within 365 days.
    for stem, picks in used_by_date.items():
        d0 = date.fromisoformat(stem)
        for other, opicks in used_by_date.items():
            if other == stem:
                continue
            d1 = date.fromisoformat(other)
            if abs((d0 - d1).days) <= 365 and picks & opicks:
                err(f"{stem}: quoteId(s) {sorted(picks & opicks)} reused within 365 days ({other})")


def main() -> int:
    corpus_only = "--corpus" in sys.argv
    by_id = validate_corpus()
    if not corpus_only:
        validate_daily(by_id)

    for w in warnings:
        print(f"WARNING: {w}")
    for e in errors:
        print(f"ERROR: {e}")
    if errors:
        print(f"\n{len(errors)} error(s).")
        return 1
    print(f"OK ({len(by_id)} quotes).")
    return 0


if __name__ == "__main__":
    sys.exit(main())

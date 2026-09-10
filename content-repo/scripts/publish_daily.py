#!/usr/bin/env python3
"""Deterministic daily generator (design doc section 3.5).

Runs from daily.yml cron `0 3 * * *` UTC. Targets today_utc + 2 days and backfills
any missing date in [today_utc, target]. No LLM at publish time.

    python scripts/publish_daily.py
"""
from __future__ import annotations

import json
import random
import subprocess
import sys
from datetime import date, datetime, timedelta, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
QUOTES = ROOT / "corpus" / "quotes.json"
MIN_APP_ID = ROOT / "corpus" / "min_app_corpus_id.txt"
DAILY_DIR = ROOT / "daily"
INDEX = DAILY_DIR / "index.json"
BANDS = ["EASY", "MEDIUM", "HARD", "EXTREME"]
INDEX_KEEP = 14


def load_corpus() -> list[dict]:
    return json.loads(QUOTES.read_text(encoding="utf-8"))


def min_app_id() -> int:
    raw = MIN_APP_ID.read_text(encoding="utf-8").strip()
    return int(raw) if raw else 10**9


def used_within_365(target: date) -> set[int]:
    used: set[int] = set()
    for f in DAILY_DIR.glob("*.json"):
        if f.name == "index.json":
            continue
        try:
            d = date.fromisoformat(f.stem)
        except ValueError:
            continue
        if abs((target - d).days) <= 365:
            data = json.loads(f.read_text(encoding="utf-8"))
            for pick in data.get("puzzles", {}).values():
                used.add(pick["quoteId"])
    return used


def seed_for(d: date, quote_id: int) -> int:
    return int(d.strftime("%Y%m%d")) * 10_000 + quote_id


def build_day(target: date, corpus: list[dict], cap: int) -> dict:
    used = used_within_365(target)
    rng = random.Random(int(target.strftime("%Y%m%d")))
    puzzles: dict[str, dict] = {}
    for band in BANDS:
        pool = [q for q in corpus if q["band"] == band and q["id"] <= cap and q["id"] not in used]
        if not pool:
            pool = [q for q in corpus if q["band"] == band and q["id"] <= cap]
        if not pool:
            raise SystemExit(f"no quotes available for band {band} at id <= {cap}")
        pick = rng.choice(pool)
        puzzles[band] = {"quoteId": pick["id"], "seed": seed_for(target, pick["id"])}
        used.add(pick["id"])
    return {"date": target.isoformat(), "puzzles": puzzles}


def update_index() -> None:
    dates = sorted(
        f.stem for f in DAILY_DIR.glob("*.json") if f.name != "index.json"
    )
    INDEX.write_text(
        json.dumps({"dates": dates[-INDEX_KEEP:]}, indent=2) + "\n", encoding="utf-8"
    )


def main() -> int:
    DAILY_DIR.mkdir(exist_ok=True)
    corpus = load_corpus()
    cap = min_app_id()

    today = datetime.now(timezone.utc).date()
    target = today + timedelta(days=2)

    wrote = []
    d = today
    while d <= target:
        out = DAILY_DIR / f"{d.isoformat()}.json"
        if not out.exists():
            payload = build_day(d, corpus, cap)
            out.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
            wrote.append(out.name)
        d += timedelta(days=1)

    update_index()

    if not wrote:
        print("nothing to publish; all dates present")
        return 0

    print("wrote:", ", ".join(wrote))
    rc = subprocess.call([sys.executable, str(ROOT / "scripts" / "validate.py")])
    if rc != 0:
        raise SystemExit("validation failed; not committing")
    return 0


if __name__ == "__main__":
    sys.exit(main())

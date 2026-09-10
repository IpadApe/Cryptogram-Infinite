#!/usr/bin/env python3
"""Writes the app's bundled corpus asset (design doc section 3.1 / 5.3).

    python scripts/build_assets.py [OUTPUT]

OUTPUT defaults to ../app/src/main/assets/corpus.json relative to this repo, i.e.
the layout when the content repo sits next to the app checkout. The version is the
git commit count so it strictly increases with every content change.
"""
from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
QUOTES = ROOT / "corpus" / "quotes.json"
DEFAULT_OUT = ROOT.parent / "app" / "src" / "main" / "assets" / "corpus.json"


def commit_count() -> int:
    try:
        out = subprocess.check_output(
            ["git", "rev-list", "--count", "HEAD"], cwd=ROOT, text=True
        ).strip()
        return int(out)
    except Exception:
        return 1


def main() -> int:
    out_path = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_OUT
    quotes = json.loads(QUOTES.read_text(encoding="utf-8"))
    quotes.sort(key=lambda q: q["id"])
    doc = {"version": commit_count(), "quotes": quotes}
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps(doc, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {out_path} (version {doc['version']}, {len(quotes)} quotes)")
    return 0


if __name__ == "__main__":
    sys.exit(main())

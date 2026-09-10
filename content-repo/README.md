# Cryptogram Infinite — content repo

Source of truth for the game's quote corpus and daily puzzles. Mirrors design
doc section 3. Push this tree to `github.com/IpadApe/Cryptogram-Infinite`.

## Layout

```
corpus/quotes.json          verified quote DB, JSON array, append-only, sorted by id
corpus/profanity.txt        one lowercase word per line
corpus/min_app_corpus_id.txt  highest quote id the oldest supported app build bundles
daily/index.json            { "dates": [...] }, last 14 ascending
daily/YYYY-MM-DD.json        one file per local date, no plaintext
scripts/validate.py         CI gate (design doc 3.4)
scripts/publish_daily.py    cron generator (design doc 3.5)
scripts/build_assets.py     writes app/src/main/assets/corpus.json (design doc 5.3)
curation/authors.txt        Wikiquote/Wikisource slugs for the offline curation pipeline
.github/workflows/validate.yml   runs on every push / PR
.github/workflows/daily.yml      cron 0 3 * * *, contents: write, commits as github-actions[bot]
```

## Status

- Scripts + workflows + schema: complete and self-testing (`python scripts/validate.py`).
- **Corpus content: NOT complete.** `corpus/quotes.json` holds only a 40-row dev
  sample. The launch corpus (2,000 rows, ≥300 per band, verbatim, attributed) is
  produced by the offline AI-assisted curation pipeline in design doc 3.6 and must
  be reviewed by a human before commit. `validate.py` enforces the hard minimum of
  100 rows per band and will fail CI until the real corpus lands.

## Workflow

1. Curate quotes offline → append to `corpus/quotes.json` with new ids.
2. `python scripts/validate.py`
3. `python scripts/build_assets.py` → regenerates the app's bundled asset.
4. Bump `corpus/min_app_corpus_id.txt` when a new app version ships a larger corpus.
5. Commit. CI validates; the daily cron keeps `daily/` two days ahead.

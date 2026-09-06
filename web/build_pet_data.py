#!/usr/bin/env python3
"""Regenerate the embedded <script id="pet-data"> block in pet-wheel.html.

Merges the two pet datasets that already exist in this repo:

  - src/main/resources/com/alecray/pethunter/pets.json
    (wikiUrl, type, itemId, per-pet metadata)
  - pet_ranker.py's PETS tuple
    (rarity/rate, minutes_per_attempt, hiscore activity name, assumed flag
    -- the only source with a timing estimate)

Only pets present in BOTH sources are emitted, because the wheel's
"expected hours" and "chance so far" math needs a rate and a
minutes-per-attempt figure, which only pet_ranker.py provides. Pets that
exist only in pets.json (skilling pets with rarity 0, and other
non-fixed-rate pets pet_ranker.py doesn't model yet -- e.g. Rocky, Broav,
Cat, Bloodhound, Chompy chick, Herbi, Lil' creator, Pet penance queen,
Quetzin) are intentionally left out of the hunt wheel until pet_ranker.py
grows a timing estimate for them. This script prints what it skipped.

When a pet has multiple PETS rows (different kill methods, e.g. Callisto
cub via Callisto or via Artio), the fastest method (lowest expected
hours) is used for the wheel's default weighting -- this mirrors
pet_ranker.py's own "quickest first" ranking philosophy.

Run this after editing pets.json or pet_ranker.py's PETS tuple:
    python web/build_pet_data.py
"""
import importlib.util
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PETS_JSON_PATH = ROOT / "src/main/resources/com/alecray/pethunter/pets.json"
PET_RANKER_PATH = ROOT / "pet_ranker.py"
HTML_PATH = Path(__file__).resolve().parent / "pet-wheel.html"

PET_DATA_BLOCK_RE = re.compile(
    r'(<script id="pet-data" type="application/json">\n).*?(\n</script>)',
    re.DOTALL,
)

# pet_ranker.py's "activity" (OSRS hiscores name) -> Wise Old Man's snake_case
# boss metric, verified live against https://api.wiseoldman.net/v2/efficiency
# /rates?metric=ehb&type=main (2026-09-05). Anything not in that response
# (Guardians of the Rift, Wintertodt, Zalcano, Tempoross -- all ehb:0
# skilling/minigame content) maps to None: the wheel falls back to the
# hardcoded minutes_per_attempt estimate for those.
WOM_METRIC_BY_ACTIVITY = {
    "Abyssal Sire": "abyssal_sire",
    "Rifts closed": None,
    "Duke Sucellus": "duke_sucellus",
    "Brutus": "brutus",
    "The Royal Titans": "the_royal_titans",
    "Vardorvis": "vardorvis",
    "Callisto": "callisto",
    "Artio": "artio",
    "Doom of Mokhaiotl": "doom_of_mokhaiotl",
    "Cerberus": "cerberus",
    "The Hueycoatl": "the_hueycoatl",
    "Alchemical Hydra": "alchemical_hydra",
    "TzKal-Zuk": "tzkal_zuk",
    "Kalphite Queen": "kalphite_queen",
    "Theatre of Blood": "theatre_of_blood",
    "The Leviathan": "the_leviathan",
    "The Nightmare": "nightmare",
    "Phosani's Nightmare": "phosanis_nightmare",
    "Amoxliatl": "amoxliatl",
    "Phantom Muspah": "phantom_muspah",
    "Nex": "nex",
    "Araxxor": "araxxor",
    "Grotesque Guardians": "grotesque_guardians",
    "Chambers of Xeric": "chambers_of_xeric",
    "Chaos Elemental": "chaos_elemental",
    "Chaos Fanatic": "chaos_fanatic",
    "Dagannoth Prime": "dagannoth_prime",
    "Dagannoth Rex": "dagannoth_rex",
    "Dagannoth Supreme": "dagannoth_supreme",
    "Corporeal Beast": "corporeal_beast",
    "General Graardor": "general_graardor",
    "K'ril Tsutsaroth": "kril_tsutsaroth",
    "Kraken": "kraken",
    "Kree'Arra": "kreearra",
    "Thermonuclear Smoke Devil": "thermonuclear_smoke_devil",
    "Zulrah": "zulrah",
    "Commander Zilyana": "commander_zilyana",
    "Wintertodt": None,
    "King Black Dragon": "king_black_dragon",
    "Scorpia": "scorpia",
    "Scurrius": "scurrius",
    "Skotizo": "skotizo",
    "Sol Heredit": "sol_heredit",
    "Zalcano": None,
    "Sarachnis": "sarachnis",
    "Tempoross": None,
    "Tombs of Amascut: Expert Mode": "tombs_of_amascut_expert",
    "TzTok-Jad": "tztok_jad",
    "Venenatis": "venenatis",
    "Spindel": "spindel",
    "Vet'ion": "vetion",
    "Calvar'ion": "calvarion",
    "Vorkath": "vorkath",
    "The Whisperer": "the_whisperer",
    "Yama": "yama",
    "The Gauntlet": "the_gauntlet",
    "The Corrupted Gauntlet": "the_corrupted_gauntlet",
}


def load_pet_ranker():
    """Import pet_ranker.py by path so this script works regardless of cwd.

    pet_ranker.py only fetches network data / prompts inside main(), which
    runs under `if __name__ == "__main__"` -- importing it is side-effect
    free and just gives us the PETS tuple.
    """
    spec = importlib.util.spec_from_file_location("pet_ranker", PET_RANKER_PATH)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def build_dataset():
    pet_ranker = load_pet_ranker()
    pets_json = json.loads(PETS_JSON_PATH.read_text(encoding="utf-8"))
    meta_by_name = {p["name"]: p for p in pets_json}

    best_by_name = {}
    for pet, source, activity, rarity, minutes, item_id, assumed in pet_ranker.PETS:
        expected_hours = rarity * minutes / 60.0
        current = best_by_name.get(pet)
        if current is None or expected_hours < current["expected_hours"]:
            best_by_name[pet] = {
                "name": pet,
                "source": source,
                "activity": activity,
                "rarity": rarity,
                "minutes_per_attempt": minutes,
                "item_id": item_id,
                "assumed": assumed,
                "expected_hours": expected_hours,
            }

    dataset = []
    skipped_no_json_match = []
    for name, row in best_by_name.items():
        meta = meta_by_name.get(name)
        if meta is None:
            skipped_no_json_match.append(name)
            continue
        # pets.json is missing itemId for several newer pets (recorded as 0);
        # pet_ranker.py's PETS tuple has real ids for most of those, so prefer
        # pets.json's id only when it is actually populated (item 7 of the
        # 2026-09-05 spec refinement).
        item_id = meta.get("itemId") or row["item_id"]
        dataset.append(
            {
                "name": row["name"],
                "source": row["source"],
                "activity": row["activity"],
                "rarity": row["rarity"],
                "minutes_per_attempt": row["minutes_per_attempt"],
                "assumed": row["assumed"],
                "item_id": item_id,
                "wikiUrl": meta.get("wikiUrl", ""),
                "type": meta.get("type", "SOLO"),
                "wom": WOM_METRIC_BY_ACTIVITY.get(row["activity"]),
            }
        )

    dataset.sort(key=lambda r: r["name"])

    if skipped_no_json_match:
        print(
            f"Note: {len(skipped_no_json_match)} PETS row(s) had no pets.json name match, skipped: "
            + ", ".join(sorted(skipped_no_json_match)),
            file=sys.stderr,
        )

    only_in_json = sorted(set(meta_by_name) - set(best_by_name))
    if only_in_json:
        print(
            f"Note: {len(only_in_json)} pets.json pet(s) skipped (no timing estimate in pet_ranker.py): "
            + ", ".join(only_in_json),
            file=sys.stderr,
        )

    return dataset


def inject(dataset):
    html = HTML_PATH.read_text(encoding="utf-8")
    payload = json.dumps(dataset, indent=2)
    if not PET_DATA_BLOCK_RE.search(html):
        raise SystemExit('Could not find <script id="pet-data"> block in pet-wheel.html')
    html = PET_DATA_BLOCK_RE.sub(lambda m: m.group(1) + payload + m.group(2), html, count=1)
    HTML_PATH.write_text(html, encoding="utf-8")


def main():
    dataset = build_dataset()
    inject(dataset)
    print(f"Wrote {len(dataset)} pets into {HTML_PATH}")


if __name__ == "__main__":
    main()

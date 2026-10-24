#!/usr/bin/env python3
"""Sprawdza paczki zadań: schemat JSON + reguły, których schemat nie wyrazi.

Użycie: python3 tools/validate_packs.py [plik.json ...]
Bez argumentów sprawdza wszystkie paczki z app/src/main/assets/packs.
Wymaga: pip install jsonschema
"""
import json
import sys
from pathlib import Path

import jsonschema

ROOT = Path(__file__).resolve().parent.parent
SCHEMA = json.loads((ROOT / "docs/schema/pack.schema.json").read_text(encoding="utf-8"))


def check(path: Path) -> list[str]:
    pack = json.loads(path.read_text(encoding="utf-8"))
    errors = [f"schemat: {e.json_path}: {e.message}" for e in jsonschema.Draft202012Validator(SCHEMA).iter_errors(pack)]
    if errors:
        return errors
    if pack["id"] != path.stem:
        errors.append(f"id paczki '{pack['id']}' różni się od nazwy pliku '{path.stem}'")
    levels = {lvl["number"] for lvl in pack.get("levels", [])}
    seen = set()
    for card in pack["cards"]:
        cid = card["id"]
        if cid in seen:
            errors.append(f"{cid}: zduplikowane id")
        seen.add(cid)
        if levels and card.get("level", 1) not in levels:
            errors.append(f"{cid}: poziom {card.get('level', 1)} nie jest opisany w 'levels'")
        answer = card["answer"]
        if answer["kind"] == "choice":
            if answer["correct"] >= len(answer["options"]):
                errors.append(f"{cid}: correct = {answer['correct']} poza listą opcji")
            if len(set(answer["options"])) != len(answer["options"]):
                errors.append(f"{cid}: powtórzone opcje")
    return errors


def main() -> int:
    paths = [Path(p) for p in sys.argv[1:]] or sorted((ROOT / "app/src/main/assets/packs").glob("*.json"))
    failed = False
    for path in paths:
        errors = check(path)
        status = "OK" if not errors else "BŁĄD"
        print(f"{status}  {path.name}")
        for err in errors:
            print(f"      {err}")
        failed |= bool(errors)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())

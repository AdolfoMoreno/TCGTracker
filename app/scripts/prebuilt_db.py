#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
import sqlite3
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SEED_DATA_PATH = ROOT / "src/main/java/com/pokemontcg/tracker/data/repository/SeedData.kt"
ASSET_DB_PATH = ROOT / "src/main/assets/database/pokemon_tcg_tracker.db"
ASSET_ROOT = ROOT / "src/main/assets"

SET_RE = re.compile(
    r'PokemonSet\("([^"]+)", "([^"]+)", "([^"]+)", (\d+), (\d+), "([^"]+)"(?:, "([^"]*)", "([^"]*)")?\)'
)
CARD_RE = re.compile(
    r'PokemonCard\("([^"]+)", "([^"]+)", "([^"]+)", "([^"]+)", "([^"]+)", "([^"]*)", "([^"]+)"\)'
)
PAIR_RE = re.compile(r'"([^"]+)" -> Pair\((\d+), "([^"]+)"\)')
QUOTED_STRING_RE = re.compile(r'"([^"]*)"')

GENERIC_RARITIES = [
    "Common",
    "Common",
    "Common",
    "Uncommon",
    "Uncommon",
    "Rare",
    "Rare Holo",
    "Rare Ultra",
    "Rare Secret",
]
GENERIC_TYPES = [
    "Fire",
    "Water",
    "Grass",
    "Lightning",
    "Psychic",
    "Fighting",
    "Darkness",
    "Metal",
    "Dragon",
    "Fairy",
    "Colorless",
]
GENERIC_POKEMON_NAMES = [
    "Bulbasaur",
    "Charmander",
    "Squirtle",
    "Caterpie",
    "Metapod",
    "Butterfree",
    "Weedle",
    "Kakuna",
    "Beedrill",
    "Pidgey",
    "Pidgeotto",
    "Pidgeot",
    "Rattata",
    "Raticate",
    "Spearow",
    "Fearow",
    "Ekans",
    "Arbok",
    "Pikachu",
    "Raichu",
    "Sandshrew",
    "Sandslash",
    "Nidoran♀",
    "Nidorina",
]
GENERIC_TRAINER_NAMES = [
    "Professor's Research",
    "Boss's Orders",
    "Marnie",
    "Quick Ball",
    "Evolution Incense",
    "Air Balloon",
]
GENERIC_ENERGY_NAMES = [
    "Fire Energy",
    "Water Energy",
    "Grass Energy",
    "Lightning Energy",
    "Psychic Energy",
    "Fighting Energy",
]


def load_seed_source() -> str:
    return SEED_DATA_PATH.read_text(encoding="utf-8")


def extract_section(source: str, start_marker: str, end_marker: str) -> str:
    start = source.index(start_marker)
    end = source.index(end_marker, start)
    return source[start:end]


def parse_sets(source: str) -> list[tuple[str, str, str, int, int, str, str, str]]:
    section = extract_section(
        source,
        "private fun getSets(): List<PokemonSet> = listOf(",
        "\n    )\n\n    /**",
    )
    sets = []
    for match in SET_RE.finditer(section):
        set_id, name, series, printed_total, total, release_date, logo_url, symbol_url = match.groups()
        sets.append(
            (
                set_id,
                name,
                series,
                int(printed_total),
                int(total),
                release_date,
                logo_url or "",
                symbol_url or "",
            )
        )
    return sets


def parse_set_info(source: str) -> dict[str, tuple[int, str]]:
    section = extract_section(
        source,
        "private fun getSetInfo(setId: String): Pair<Int, String> {",
        "\n    }\n\n    private fun generateGenericCards",
    )
    info: dict[str, tuple[int, str]] = {}
    for set_id, total, series in PAIR_RE.findall(section):
        info[set_id] = (int(total), series)
    return info


def parse_explicit_cards(source: str, start_marker: str, end_marker: str) -> list[tuple[str, str, str, str, str, str, str, str, str]]:
    section = extract_section(source, start_marker, end_marker)
    cards = []
    for match in CARD_RE.finditer(section):
        card_id, name, number, set_id, rarity, types, supertype = match.groups()
        image_small, image_large = card_image_paths(card_id)
        cards.append((
            card_id,
            name,
            number,
            set_id,
            rarity,
            types,
            supertype,
            image_small,
            image_large,
        ))
    return cards


def parse_swsh1_names(source: str) -> list[str]:
    section = extract_section(
        source,
        "private fun generateSwSh1Cards(): List<PokemonCard> {",
        "\n        names.forEachIndexed",
    )
    list_start = section.index("val names = listOf(")
    list_body = section[list_start:]
    return QUOTED_STRING_RE.findall(list_body)


def generate_generic_cards(set_id: str, count: int) -> list[tuple[str, str, str, str, str, str, str, str, str]]:
    cards = []
    for num in range(1, count + 1):
        if num <= count * 0.65:
            supertype = "Pokémon"
            name = GENERIC_POKEMON_NAMES[num % len(GENERIC_POKEMON_NAMES)]
        elif num <= count * 0.85:
            supertype = "Trainer"
            name = GENERIC_TRAINER_NAMES[num % len(GENERIC_TRAINER_NAMES)]
        else:
            supertype = "Energy"
            name = GENERIC_ENERGY_NAMES[num % len(GENERIC_ENERGY_NAMES)]

        card_id = f"{set_id}-{num}"
        image_small, image_large = card_image_paths(card_id)
        cards.append(
            (
                card_id,
                name,
                str(num),
                set_id,
                GENERIC_RARITIES[num % len(GENERIC_RARITIES)],
                GENERIC_TYPES[num % len(GENERIC_TYPES)],
                supertype,
                image_small,
                image_large,
            )
        )
    return cards


def generate_swsh1_cards(names: list[str]) -> list[tuple[str, str, str, str, str, str, str, str, str]]:
    cards = []
    for index, name in enumerate(names, start=1):
        if "Energy" in name:
            supertype = "Energy"
        elif any(token in name for token in ("Tower", "Mine", "Court")):
            supertype = "Trainer"
        elif any(
            token in name
            for token in ("Ball", "Research", "Marnie", "Boss", "Sonia", "Switch", "Incense", "Balloon")
        ):
            supertype = "Trainer"
        else:
            supertype = "Pokémon"

        if "VMAX" in name:
            rarity = "Rare VMAX"
        elif " V" in name or name.endswith("V"):
            rarity = "Rare Ultra"
        elif index <= 17:
            rarity = "Rare Holo"
        elif index <= 40:
            rarity = "Uncommon"
        else:
            rarity = "Common"

        if any(token in name for token in ("Grass", "Grookey", "Rillaboom", "Caterpie", "Celebi")):
            types = "Grass"
        elif any(token in name for token in ("Fire", "Scorbunny", "Cinderace", "Arcanine", "Torkoal", "Coalossal")):
            types = "Fire"
        elif any(token in name for token in ("Water", "Sobble", "Inteleon", "Lapras", "Drednaw", "Chewtle")):
            types = "Water"
        elif any(token in name for token in ("Lightning", "Pikachu", "Boltund", "Morpeko")):
            types = "Lightning"
        elif any(token in name for token in ("Psychic", "Hatterene", "Indeedee")):
            types = "Psychic"
        elif any(token in name for token in ("Darkness", "Grimmsnarl", "Crobat", "Eternatus")):
            types = "Darkness"
        elif any(token in name for token in ("Metal", "Zacian", "Zamazenta", "Falinks", "Stonjourner")):
            types = "Metal"
        else:
            types = ""

        card_id = f"swsh1-{index}"
        image_small, image_large = card_image_paths(card_id)
        cards.append((
            card_id,
            name,
            str(index),
            "swsh1",
            rarity,
            types,
            supertype,
            image_small,
            image_large,
        ))
    return cards


def card_image_paths(card_id: str) -> tuple[str, str]:
    return f"cards/small/{card_id}.png", f"cards/large/{card_id}.png"


def build_dataset() -> tuple[
    list[tuple[str, str, str, int, int, str, str, str]],
    list[tuple[str, str, str, str, str, str, str, str, str]],
]:
    source = load_seed_source()
    sets = parse_sets(source)
    set_info = parse_set_info(source)
    sv1_cards = parse_explicit_cards(
        source,
        "private fun generateSV1Cards(): List<PokemonCard> {",
        "\n    }\n\n    private fun generateBase1Cards",
    )
    base1_cards = parse_explicit_cards(
        source,
        "private fun generateBase1Cards(): List<PokemonCard> = listOf(",
        "\n    )\n\n    private fun generateSwSh1Cards",
    )
    swsh1_cards = generate_swsh1_cards(parse_swsh1_names(source))

    cards_by_set = {
        "sv1": sv1_cards,
        "base1": base1_cards,
        "swsh1": swsh1_cards,
    }
    all_cards: list[tuple[str, str, str, str, str, str, str, str, str]] = []
    for set_id, *_rest in sets:
        if set_id in cards_by_set:
            all_cards.extend(cards_by_set[set_id])
            continue
        count, _series = set_info.get(set_id, (100, "Other"))
        all_cards.extend(generate_generic_cards(set_id, count))
    return sets, all_cards


def rebuild_database(db_path: Path) -> tuple[int, int]:
    sets, cards = build_dataset()
    db_path.parent.mkdir(parents=True, exist_ok=True)
    if db_path.exists():
        db_path.unlink()

    conn = sqlite3.connect(db_path)
    try:
        conn.execute("PRAGMA journal_mode=DELETE")
        conn.executescript(
            """
            PRAGMA foreign_keys = OFF;
            CREATE TABLE `sets` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `series` TEXT NOT NULL,
                `printedTotal` INTEGER NOT NULL,
                `total` INTEGER NOT NULL,
                `releaseDate` TEXT NOT NULL,
                `logoUrl` TEXT NOT NULL,
                `symbolUrl` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            );
            CREATE TABLE `cards` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `number` TEXT NOT NULL,
                `setId` TEXT NOT NULL,
                `rarity` TEXT NOT NULL,
                `types` TEXT NOT NULL,
                `supertype` TEXT NOT NULL,
                `imageSmall` TEXT NOT NULL,
                `imageLarge` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`setId`) REFERENCES `sets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            );
            CREATE INDEX `index_cards_setId` ON `cards` (`setId`);
            CREATE TABLE `collection` (
                `cardId` TEXT NOT NULL,
                `quantity` INTEGER NOT NULL,
                `condition` TEXT NOT NULL,
                `isFoil` INTEGER NOT NULL,
                `notes` TEXT NOT NULL,
                `dateAdded` INTEGER NOT NULL,
                PRIMARY KEY(`cardId`),
                FOREIGN KEY(`cardId`) REFERENCES `cards`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            );
            CREATE INDEX `index_collection_cardId` ON `collection` (`cardId`);
            CREATE TABLE `wishlists` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            );
            CREATE TABLE `wishlist_cards` (
                `wishlistId` INTEGER NOT NULL,
                `cardId` TEXT NOT NULL,
                `addedAt` INTEGER NOT NULL,
                PRIMARY KEY(`wishlistId`, `cardId`),
                FOREIGN KEY(`wishlistId`) REFERENCES `wishlists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`cardId`) REFERENCES `cards`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            );
            CREATE INDEX `index_wishlist_cards_cardId` ON `wishlist_cards` (`cardId`);
            CREATE TABLE `storage_containers` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `capacity` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            );
            CREATE TABLE `stored_card_assignments` (
                `containerId` INTEGER NOT NULL,
                `cardId` TEXT NOT NULL,
                `quantity` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`containerId`, `cardId`),
                FOREIGN KEY(`containerId`) REFERENCES `storage_containers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`cardId`) REFERENCES `cards`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            );
            CREATE INDEX `index_stored_card_assignments_cardId` ON `stored_card_assignments` (`cardId`);
            PRAGMA foreign_keys = ON;
            """
        )
        conn.execute("PRAGMA user_version = 4")
        conn.executemany(
            """
            INSERT INTO sets (id, name, series, printedTotal, total, releaseDate, logoUrl, symbolUrl)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            sets,
        )
        conn.executemany(
            """
            INSERT INTO cards (id, name, number, setId, rarity, types, supertype, imageSmall, imageLarge)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            cards,
        )
        conn.commit()
    finally:
        conn.close()

    return len(sets), len(cards)


def fetch_one(conn: sqlite3.Connection, query: str) -> int:
    return int(conn.execute(query).fetchone()[0])


def assert_columns(conn: sqlite3.Connection, table: str, expected: list[tuple[str, str, int, int]]) -> None:
    rows = conn.execute(f"PRAGMA table_info(`{table}`)").fetchall()
    actual = [(row[1], row[2], row[3], row[5]) for row in rows]
    if actual != expected:
        raise AssertionError(f"{table} columns mismatch.\nExpected: {expected}\nFound: {actual}")


def assert_foreign_keys(conn: sqlite3.Connection, table: str, expected: list[tuple[str, str, str, str, str]]) -> None:
    rows = conn.execute(f"PRAGMA foreign_key_list(`{table}`)").fetchall()
    actual = [(row[2], row[3], row[4], row[5], row[6]) for row in rows]
    if actual != expected:
        raise AssertionError(f"{table} foreign keys mismatch.\nExpected: {expected}\nFound: {actual}")


def assert_indices(conn: sqlite3.Connection, table: str, expected: dict[str, list[str]]) -> None:
    rows = conn.execute(f"PRAGMA index_list(`{table}`)").fetchall()
    actual_names = {row[1] for row in rows if not row[1].startswith("sqlite_autoindex_")}
    if actual_names != set(expected):
        raise AssertionError(f"{table} indexes mismatch.\nExpected: {set(expected)}\nFound: {actual_names}")
    for index_name, expected_columns in expected.items():
        info_rows = conn.execute(f"PRAGMA index_info(`{index_name}`)").fetchall()
        actual_columns = [row[2] for row in info_rows]
        if actual_columns != expected_columns:
            raise AssertionError(
                f"{table}.{index_name} columns mismatch.\nExpected: {expected_columns}\nFound: {actual_columns}"
            )


def validate_database(db_path: Path) -> tuple[int, int]:
    expected_sets, expected_cards = build_dataset()
    if not db_path.exists():
        raise AssertionError(f"Missing prebuilt database: {db_path}")

    conn = sqlite3.connect(db_path)
    try:
        user_version = fetch_one(conn, "PRAGMA user_version")
        if user_version != 4:
            raise AssertionError(f"Expected PRAGMA user_version=4, found {user_version}")

        actual_tables = {
            row[0]
            for row in conn.execute(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'"
            ).fetchall()
        }
        required_tables = {
            "sets",
            "cards",
            "collection",
            "wishlists",
            "wishlist_cards",
            "storage_containers",
            "stored_card_assignments",
        }
        if not required_tables.issubset(actual_tables):
            raise AssertionError(f"Missing required tables. Expected at least {required_tables}, found {actual_tables}")

        assert_columns(
            conn,
            "sets",
            [
                ("id", "TEXT", 1, 1),
                ("name", "TEXT", 1, 0),
                ("series", "TEXT", 1, 0),
                ("printedTotal", "INTEGER", 1, 0),
                ("total", "INTEGER", 1, 0),
                ("releaseDate", "TEXT", 1, 0),
                ("logoUrl", "TEXT", 1, 0),
                ("symbolUrl", "TEXT", 1, 0),
            ],
        )
        assert_columns(
            conn,
            "cards",
            [
                ("id", "TEXT", 1, 1),
                ("name", "TEXT", 1, 0),
                ("number", "TEXT", 1, 0),
                ("setId", "TEXT", 1, 0),
                ("rarity", "TEXT", 1, 0),
                ("types", "TEXT", 1, 0),
                ("supertype", "TEXT", 1, 0),
                ("imageSmall", "TEXT", 1, 0),
                ("imageLarge", "TEXT", 1, 0),
            ],
        )
        assert_columns(
            conn,
            "collection",
            [
                ("cardId", "TEXT", 1, 1),
                ("quantity", "INTEGER", 1, 0),
                ("condition", "TEXT", 1, 0),
                ("isFoil", "INTEGER", 1, 0),
                ("notes", "TEXT", 1, 0),
                ("dateAdded", "INTEGER", 1, 0),
            ],
        )
        assert_columns(
            conn,
            "wishlists",
            [
                ("id", "INTEGER", 1, 1),
                ("name", "TEXT", 1, 0),
                ("createdAt", "INTEGER", 1, 0),
                ("updatedAt", "INTEGER", 1, 0),
            ],
        )
        assert_columns(
            conn,
            "wishlist_cards",
            [
                ("wishlistId", "INTEGER", 1, 1),
                ("cardId", "TEXT", 1, 2),
                ("addedAt", "INTEGER", 1, 0),
            ],
        )
        assert_columns(
            conn,
            "storage_containers",
            [
                ("id", "INTEGER", 1, 1),
                ("name", "TEXT", 1, 0),
                ("type", "TEXT", 1, 0),
                ("capacity", "INTEGER", 1, 0),
                ("createdAt", "INTEGER", 1, 0),
                ("updatedAt", "INTEGER", 1, 0),
            ],
        )
        assert_columns(
            conn,
            "stored_card_assignments",
            [
                ("containerId", "INTEGER", 1, 1),
                ("cardId", "TEXT", 1, 2),
                ("quantity", "INTEGER", 1, 0),
                ("updatedAt", "INTEGER", 1, 0),
            ],
        )

        assert_foreign_keys(conn, "cards", [("sets", "setId", "id", "NO ACTION", "CASCADE")])
        assert_foreign_keys(conn, "collection", [("cards", "cardId", "id", "NO ACTION", "CASCADE")])
        assert_foreign_keys(
            conn,
            "wishlist_cards",
            [
                ("cards", "cardId", "id", "NO ACTION", "CASCADE"),
                ("wishlists", "wishlistId", "id", "NO ACTION", "CASCADE"),
            ],
        )
        assert_foreign_keys(
            conn,
            "stored_card_assignments",
            [
                ("cards", "cardId", "id", "NO ACTION", "CASCADE"),
                ("storage_containers", "containerId", "id", "NO ACTION", "CASCADE"),
            ],
        )
        assert_indices(conn, "cards", {"index_cards_setId": ["setId"]})
        assert_indices(conn, "collection", {"index_collection_cardId": ["cardId"]})
        assert_indices(conn, "wishlist_cards", {"index_wishlist_cards_cardId": ["cardId"]})
        assert_indices(conn, "stored_card_assignments", {"index_stored_card_assignments_cardId": ["cardId"]})

        set_count = fetch_one(conn, "SELECT COUNT(*) FROM sets")
        card_count = fetch_one(conn, "SELECT COUNT(*) FROM cards")
        collection_count = fetch_one(conn, "SELECT COUNT(*) FROM collection")
        wishlist_count = fetch_one(conn, "SELECT COUNT(*) FROM wishlists")
        wishlist_card_count = fetch_one(conn, "SELECT COUNT(*) FROM wishlist_cards")
        storage_container_count = fetch_one(conn, "SELECT COUNT(*) FROM storage_containers")
        stored_assignment_count = fetch_one(conn, "SELECT COUNT(*) FROM stored_card_assignments")
        if set_count != len(expected_sets):
            raise AssertionError(f"Expected {len(expected_sets)} sets, found {set_count}")
        if card_count != len(expected_cards):
            raise AssertionError(f"Expected {len(expected_cards)} cards, found {card_count}")
        if collection_count != 0:
            raise AssertionError(f"Expected empty collection table in asset DB, found {collection_count} rows")
        if wishlist_count != 0:
            raise AssertionError(f"Expected empty wishlists table in asset DB, found {wishlist_count} rows")
        if wishlist_card_count != 0:
            raise AssertionError(f"Expected empty wishlist_cards table in asset DB, found {wishlist_card_count} rows")
        if storage_container_count != 0:
            raise AssertionError(
                f"Expected empty storage_containers table in asset DB, found {storage_container_count} rows"
            )
        if stored_assignment_count != 0:
            raise AssertionError(
                f"Expected empty stored_card_assignments table in asset DB, found {stored_assignment_count} rows"
            )

        missing_small = fetch_one(conn, "SELECT COUNT(*) FROM cards WHERE imageSmall = '' OR imageSmall IS NULL")
        missing_large = fetch_one(conn, "SELECT COUNT(*) FROM cards WHERE imageLarge = '' OR imageLarge IS NULL")
        if missing_small or missing_large:
            raise AssertionError(
                f"Expected every card to have image paths, found missingSmall={missing_small}, missingLarge={missing_large}"
            )

        small_asset_root = ASSET_ROOT / "cards/small"
        large_asset_root = ASSET_ROOT / "cards/large"
        if small_asset_root.exists() or large_asset_root.exists():
            missing_assets = 0
            for small_path, large_path in conn.execute("SELECT imageSmall, imageLarge FROM cards"):
                if not (ASSET_ROOT / small_path).exists():
                    missing_assets += 1
                if not (ASSET_ROOT / large_path).exists():
                    missing_assets += 1
            if missing_assets:
                raise AssertionError(f"Missing {missing_assets} referenced card image asset files")

        orphan_cards = fetch_one(
            conn,
            """
            SELECT COUNT(*)
            FROM cards c
            LEFT JOIN sets s ON s.id = c.setId
            WHERE s.id IS NULL
            """,
        )
        if orphan_cards != 0:
            raise AssertionError(f"Found {orphan_cards} cards without a parent set")
    finally:
        conn.close()

    return len(expected_sets), len(expected_cards)


def main() -> int:
    parser = argparse.ArgumentParser(description="Rebuild or validate the bundled prebuilt Room database.")
    parser.add_argument("command", choices=["rebuild", "validate"])
    args = parser.parse_args()

    try:
        if args.command == "rebuild":
            set_count, card_count = rebuild_database(ASSET_DB_PATH)
            print(f"Rebuilt {ASSET_DB_PATH} with {set_count} sets and {card_count} cards.")
            return 0

        set_count, card_count = validate_database(ASSET_DB_PATH)
        print(f"Validated {ASSET_DB_PATH}: {set_count} sets, {card_count} cards, schema OK.")
        return 0
    except Exception as exc:  # pragma: no cover - CLI failure path
        print(f"Prebuilt DB {args.command} failed: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sqlite3
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ASSET_ROOT = ROOT / "src/main/assets"
CARD_IMAGES_ROOT = ROOT / "card_images_source"
ASSET_DB_PATH = ASSET_ROOT / "database/pokemon_tcg_tracker.db"
GENERATED_CARD_SCAN_DIR = ASSET_ROOT / "cards/scans"
CARD_FILE_RE = re.compile(r"\[(?P<card_id>[^\]]+)\]\.(?P<ext>png|jpg|jpeg|webp)$", re.IGNORECASE)
SAFE_FILE_CHARS_RE = re.compile(r"[^A-Za-z0-9._-]+")


def normalize_manifest_set(raw_set: dict) -> tuple[str, str, str, int, int, str, str, str]:
    return (
        raw_set["id"],
        raw_set["name"],
        raw_set["series"],
        int(raw_set["printedTotal"]),
        int(raw_set["total"]),
        raw_set["releaseDate"],
        "",
        "",
    )


def collect_card_file_map(set_dir: Path) -> dict[str, Path]:
    card_files: dict[str, Path] = {}
    for path in set_dir.iterdir():
        if not path.is_file() or path.name == "manifest.json":
            continue
        match = CARD_FILE_RE.search(path.name)
        if not match:
            continue
        card_id = match.group("card_id")
        card_files[card_id] = path
    return card_files


def resolve_card_image_path(card_id: str, card_file_map: dict[str, Path]) -> Path | None:
    exact_match = card_file_map.get(card_id)
    if exact_match is not None:
        return exact_match

    sanitized_id = card_id.replace("?", "")
    if sanitized_id != card_id:
        return card_file_map.get(sanitized_id)

    return None


def generated_card_asset_path(card_id: str) -> str:
    safe_card_id = SAFE_FILE_CHARS_RE.sub("_", card_id).strip("._-") or "card"
    digest = hashlib.sha1(card_id.encode("utf-8")).hexdigest()[:10]
    return f"cards/scans/{safe_card_id}-{digest}.jpg"


def build_dataset() -> tuple[
    list[tuple[str, str, str, int, int, str, str, str]],
    list[tuple[str, str, str, str, str, str, str, str, str]],
]:
    if not CARD_IMAGES_ROOT.exists():
        raise FileNotFoundError(f"Missing card image asset root: {CARD_IMAGES_ROOT}")

    sets: list[tuple[str, str, str, int, int, str, str, str]] = []
    cards: list[tuple[str, str, str, str, str, str, str, str, str]] = []
    seen_set_ids: set[str] = set()
    seen_card_rows: dict[str, tuple[str, str, str]] = {}
    duplicate_card_ids_by_manifest: dict[Path, list[str]] = {}

    manifest_paths = sorted(CARD_IMAGES_ROOT.rglob("manifest.json"))
    if not manifest_paths:
        raise AssertionError(f"No manifest.json files found under {CARD_IMAGES_ROOT}")

    for manifest_path in manifest_paths:
        data = json.loads(manifest_path.read_text(encoding="utf-8"))
        raw_set = data.get("set") or {}
        raw_cards = data.get("cards") or []
        if not raw_set or not raw_cards:
            raise AssertionError(f"Manifest is missing set or cards data: {manifest_path}")

        set_row = normalize_manifest_set(raw_set)
        set_id = set_row[0]
        if set_id in seen_set_ids:
            raise AssertionError(f"Duplicate set id {set_id} in {manifest_path}")
        seen_set_ids.add(set_id)
        sets.append(set_row)

        card_file_map = collect_card_file_map(manifest_path.parent)

        for raw_card in raw_cards:
            card_id = raw_card["id"]
            card_signature = (set_id, raw_card["name"], str(raw_card["number"]))
            if card_id in seen_card_rows:
                duplicate_card_ids_by_manifest.setdefault(manifest_path, []).append(card_id)
                continue
            seen_card_rows[card_id] = card_signature

            source_image_path = resolve_card_image_path(card_id, card_file_map)
            image_path = generated_card_asset_path(card_id) if source_image_path is not None else ""

            cards.append(
                (
                    card_id,
                    raw_card["name"],
                    str(raw_card["number"]),
                    set_id,
                    "",
                    "",
                    "",
                    image_path,
                    image_path,
                )
            )

    for manifest_path, duplicate_ids in sorted(duplicate_card_ids_by_manifest.items()):
        sample = ", ".join(duplicate_ids[:5])
        print(
            f"Warning: skipped {len(duplicate_ids)} duplicate card rows in {manifest_path} "
            f"(sample ids: {sample}).",
            file=sys.stderr,
        )

    return sets, cards


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

        missing_assets = 0
        for small_path, large_path in conn.execute("SELECT imageSmall, imageLarge FROM cards"):
            if small_path and not (ASSET_ROOT / small_path).exists():
                missing_assets += 1
            if large_path and not (ASSET_ROOT / large_path).exists():
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

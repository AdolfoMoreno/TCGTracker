# How To Add Or Change Artwork

This app currently expects card artwork to be stored as local asset files and referenced from the database.

## Where To Put The Files

Create these folders if they do not exist yet:

- `app/src/main/assets/cards/small/`
- `app/src/main/assets/cards/large/`

Use:

- `small` for grid/list thumbnails
- `large` for the card detail page

## Required Filename Format

The current database path convention is:

- `cards/small/<cardId>.png`
- `cards/large/<cardId>.png`

Examples:

- `app/src/main/assets/cards/small/sv1-1.png`
- `app/src/main/assets/cards/large/sv1-1.png`
- `app/src/main/assets/cards/small/base1-4.png`
- `app/src/main/assets/cards/large/base1-4.png`

The filename must match the card `id` in the `cards` table exactly.

## Important Note About File Types

Right now the app and DB are set up for `.png` paths.

That means:

- `PNG` will work immediately
- `JPG` or `JPEG` will not be picked up automatically unless we also update the stored DB paths

If your source files are JPEGs, the easiest path is to convert them to PNG and keep the filename format above.

## Minimum Artwork Set

For each card, add:

- one small PNG thumbnail in `cards/small`
- one large PNG scan in `cards/large`

If a file is missing, the app will show a placeholder instead of crashing.

## How To Replace Existing Artwork

To change a card image:

1. Find the card id.
2. Replace the matching file in `small` and/or `large`.
3. Keep the same filename.

Example:

- replace `app/src/main/assets/cards/small/swsh1-25.png`
- replace `app/src/main/assets/cards/large/swsh1-25.png`

No DB change is needed if the filename stays the same.

## How To Add Artwork For New Cards

If the DB already contains the card and follows the current path convention:

1. Add the two PNG files using the card id as the filename.
2. Rebuild or run the app.

If the card id does not exist in the DB yet, the artwork alone is not enough. The card record must also exist in the database.

## How To Check The Card Id

Card ids come from the `cards` table in:

- `app/src/main/assets/database/pokemon_tcg_tracker.db`

Examples of ids used by the app:

- `sv1-1`
- `base1-4`
- `swsh1-25`

If you want, I can also add a small helper script later to export a full list of expected filenames from the database.

## Validation Behavior

The DB validation script is:

- `app/scripts/prebuilt_db.py`

Current behavior:

- if no artwork folders exist yet, validation still passes
- once `app/src/main/assets/cards/small` or `app/src/main/assets/cards/large` exists, validation expects the referenced files to exist

So once you start dropping artwork into those folders, it is best to add them in a complete, consistent way.

## Recommended Workflow

1. Prepare PNG files named by card id.
2. Put thumbnails in `app/src/main/assets/cards/small/`.
3. Put large scans in `app/src/main/assets/cards/large/`.
4. Run validation when you are ready.

If you want, the next step can be one of these:

1. I add a script that prints every expected artwork filename from the DB.
2. I add support for `.jpg` / `.jpeg` files too.
3. I add a bulk import script that renames your artwork files automatically.

# How To Add Or Change Artwork

This app now builds its card catalog from the local source artwork under `app/card_images_source/`.

## Current Source Of Truth

The database rebuild script reads:

- set metadata from each set folder's `manifest.json`
- card ids from each manifest card entry
- local artwork files from that same set folder

The database file is:

- `app/src/main/assets/database/pokemon_tcg_tracker.db`

The rebuild script is:

- `app/scripts/prebuilt_db.py`

The packaged in-app image files are generated into:

- `app/src/main/assets/cards/scans/`

Important:

- `card_images_source/` is the raw source library
- `cards/scans/` is the compact generated app-ready artwork
- only `database/` and `cards/` are packaged into the app

## Folder Layout

Artwork should live under:

- `app/card_images_source/<Era>/<Set>/`

Examples:

- `app/card_images_source/Black & White/Black and White/`
- `app/card_images_source/Scarlet & Violet/151/`
- `app/card_images_source/Base/Base/`

Each set folder should contain:

- a `manifest.json`
- local image files for as many cards as you currently have

## File Naming

The importer matches cards by the card id inside square brackets in the filename.

Example:

- `85 - Tranquill [bw1-85].png`
- `4 - Charizard [base1-4].png`
- `3 - Venusaur ex [sv3pt5-3].png`

The important part is the bracketed id:

- `[bw1-85]`
- `[base1-4]`
- `[sv3pt5-3]`

The rest of the filename can be human-friendly.

## Image Sizes

Right now the app uses one compact generated file for both:

- `imageSmall`
- `imageLarge`

So one raw source image file per card is enough for now.

If a card is listed in the manifest but its local image file is missing:

- the card still stays in the database
- the image path is left blank
- the app shows the placeholder artwork for that card

## Supported File Types

These local image formats are supported by the importer:

- `.png`
- `.jpg`
- `.jpeg`
- `.webp`

## How To Replace Existing Artwork

1. Go to the card's set folder in `app/card_images_source/`.
2. Replace the file for that card.
3. Keep the same bracketed card id in the filename.

If the bracketed id stays the same, the importer will keep matching it correctly.

## How To Add New Artwork

If the card already exists in the set `manifest.json`:

1. Add the image file to that set folder.
2. Include the card id in brackets in the filename.
3. Rebuild the database.

If the card is not in the manifest yet, the artwork file alone is not enough. The card must also exist in the set's `manifest.json`.

## Validation Behavior

`app/scripts/prebuilt_db.py validate` checks that:

- every manifest-backed set is in the DB
- every manifest-backed card is in the DB
- every non-empty referenced image file exists on disk

So if a manifest card exists but its local file is missing, validation still passes and that card will render with the placeholder until artwork is added later.

## Recommended Workflow

1. Add or replace files inside the correct `card_images_source/<Era>/<Set>/` folder.
2. Keep the bracketed card id in the filename.
3. Run the compact asset generator.
4. Run the DB rebuild.
5. Run validation.

If you want later, I can also add:

1. a helper that reports missing local artwork by set
2. a helper that renames files automatically from card ids
3. a metadata import path for rarity, types, and supertype once you have that source

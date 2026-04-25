# Prebuilt Database Workflow

The app now boots Room from the bundled asset database at:

- `src/main/assets/database/pokemon_tcg_tracker.db`

Fresh installs copy that asset once through `Room.databaseBuilder(...).createFromAsset(...)`.
After install, the app keeps using the app-local database file and does not overwrite it on every launch.

## Developer commands

From the `app` module:

```bash
python3 scripts/prebuilt_db.py rebuild
python3 scripts/prebuilt_db.py validate
```

Or via Gradle tasks:

```bash
gradle :app:rebuildPrebuiltDb
gradle :app:validatePrebuiltDb
```

`validatePrebuiltDb` also runs before `preBuild` so schema or data drift is caught during development/CI.

## Source of truth

- The shipped SQLite asset is the install-time source of truth.
- `SeedData.kt` remains the developer reference dataset used to regenerate the asset.
- Runtime Room seeding is retired.

## Migration policy

When the schema changes in a future version:

- bump the Room database version
- add an explicit Room migration
- preserve `collection` data
- update catalog tables (`sets`, `cards`) through migration SQL or staged import logic

Do not replace the local database on every app start, and do not use destructive migration as the default production path.

## Current schema contents

The bundled database currently includes:

- catalog tables: `sets`, `cards`
- user-data tables: `collection`, `wishlists`, `wishlist_cards`

Fresh installs start with the catalog prepopulated and the user-data tables empty.

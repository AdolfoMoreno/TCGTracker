# Pokemon TCG Tracker Navigation + Feature Plan

## Current App Context

The app is currently a small Android Navigation component app with two top-level destinations:

- `Home` (`HomeFragment`): collection summary with owned card count, active sets, completed sets, and a list of owned-progress sets.
- `All Sets` / `Browse` (`BrowseFragment`): searchable list of all sets grouped by series.
- `Set Detail` (`SetDetailFragment`): card grid for a set with owned/all filtering and bulk toggle support.

Relevant files:

- `app/src/main/res/menu/bottom_nav_menu.xml`
- `app/src/main/res/navigation/nav_graph.xml`
- `app/src/main/java/com/pokemontcg/tracker/MainActivity.kt`
- `app/src/main/java/com/pokemontcg/tracker/ui/home/*`
- `app/src/main/java/com/pokemontcg/tracker/ui/browse/*`
- `app/src/main/java/com/pokemontcg/tracker/data/*`

## Key Constraint

The current "navbar" is a `BottomNavigationView` rendered inside the toolbar area. That works for the current two-item setup, but it is not a good fit for six destinations:

- `My Collection`
- `All`
- `Storage`
- `Wants`
- `Decks`
- `Settings`

Even if all six items were technically rendered, the current layout would become cramped, harder to scan, and less maintainable.

## Recommendation

Refactor navigation in two layers instead of forcing six items into the current top bar:

1. Keep 3 to 4 primary destinations visible in the main nav.
2. Move secondary destinations into a drawer, overflow, or a dedicated "More" destination.

Recommended structure for MVP:

- Primary nav: `My Collection`, `All`, `Wants`, `Storage`
- Secondary nav: `Decks`, `Settings`

If the product goal is to always show all six options, the best long-term solution is to replace the current embedded `BottomNavigationView` with a navigation drawer or a custom top navigation layout. That is a larger UI refactor, but it will scale much better.

## Proposed Information Architecture

### 1. My Collection

Purpose:
Give the user a home base centered on owned cards and collection progress.

What can reuse existing code:

- `HomeFragment` already acts like a collection dashboard.
- `HomeViewModel` already computes owned card count, completed sets, and set progress.
- `SetDetailFragment` already supports ownership toggling and owned-only filtering.

Recommended MVP scope:

- Rename `Home` to `My Collection`.
- Keep the summary stats at the top.
- Keep the list of started sets below.
- Add quick filters later:
  - In-progress sets
  - Completed sets
  - Recently updated sets

Data impact:

- No schema change required for the initial rename/refocus.

Implementation notes:

- Rename strings and nav IDs where helpful.
- Consider renaming `HomeFragment` to `MyCollectionFragment` later, but this can be deferred if we want to keep the first refactor low risk.

### 2. All

Purpose:
Show the full catalog of sets and remain the discovery/browsing surface.

What can reuse existing code:

- `BrowseFragment`
- `BrowseViewModel`
- Search and series grouping logic already exist.

Recommended MVP scope:

- Rename `Browse` to `All`.
- Keep set search and grouped browsing.
- Continue routing into `SetDetailFragment`.

Nice follow-ups:

- Add sort options:
  - Release date
  - Series
  - Completion percent
- Add filters:
  - Started only
  - Not started
  - Completed

Data impact:

- No schema change required for the initial rename.

### 3. Storage

Purpose:
Track where physical cards are stored so the app becomes useful for retrieval, organization, and not just ownership.

Why this matters:

The current `collection` table stores quantity, condition, foil state, notes, and date added, but it does not model physical storage locations.

Recommended MVP scope:

- Add the concept of a storage location:
  - Binder
  - Box
  - Shelf
  - Deck box
  - Custom location name
- Let each owned card reference one storage location.
- Provide a `Storage` screen with:
  - List of storage locations
  - Card counts per location
  - Tap into cards stored there

Suggested schema additions:

- New `StorageLocation` entity
  - `id`
  - `name`
  - `type`
  - `notes`
  - `sortOrder`
- Extend `CollectionEntry` with `storageLocationId`

Questions to settle before implementation:

- Can one card be split across multiple locations?
- Do we track location per card entry or per individual copy?

Recommended answer for MVP:

- Track one location per collected card entry.
- If quantity is greater than 1, assume all copies share the same storage location.

That keeps the data model simple and avoids inventory-splitting complexity too early.

### 4. Wants

Purpose:
Track target cards the user does not own yet, or cards they want additional copies of.

Why this matters:

Ownership and wanting are separate states. Right now the app can tell whether a card is owned, but there is no way to track chase targets, trade goals, or upgrade targets.

Recommended MVP scope:

- Add a `Want` state for cards.
- Support:
  - Wanting a card not yet owned
  - Wanting extra copies of a card already owned
- Add a `Wants` screen with:
  - Search or filter wanted cards
  - Group by set
  - Optional priority flag

Suggested schema additions:

- New `WantedCard` entity
  - `cardId`
  - `desiredQuantity`
  - `priority`
  - `notes`
  - `dateAdded`

Implementation options:

- Option A: Separate `WantedCard` table
  - Best for clarity
  - Avoids overloading `collection`
- Option B: Add want fields to `CollectionEntry`
  - Simpler short term
  - Harder to reason about because owned and wanted are different concepts

Recommended answer:

- Use a separate `WantedCard` table.

### 5. Decks

Purpose:
Let users group cards into playable or idea decks and compare deck contents with owned inventory.

Why this matters:

Deck building is a natural next step once the app tracks collection state. It also creates a bridge between collecting and playing.

Recommended MVP scope:

- Create a `Decks` list screen.
- Support deck create/edit/delete.
- Allow a deck to contain cards with quantities.
- Show whether required copies are fully owned.

Suggested schema additions:

- New `Deck` entity
  - `id`
  - `name`
  - `format`
  - `notes`
  - `dateUpdated`
- New `DeckCard` entity
  - `deckId`
  - `cardId`
  - `quantity`

Important product decision:

- Do decks represent exact card prints or just card names?

Recommended answer for MVP:

- Use exact `cardId`.

That aligns with the current data model, makes ownership comparison easier, and avoids card-name normalization problems.

Follow-up enhancement:

- Later we can support "proxy by card name" or "any print of this card" if deck flexibility becomes important.

### 6. Settings

Purpose:
Give the user control over app behavior, display choices, and data actions.

Recommended MVP scope:

- App preferences screen for:
  - Default card sort
  - Default set filter
  - Show/hide completed sets in collection views
  - Export/import data
  - Theme options later

Implementation approach:

- Use `PreferenceFragmentCompat` or a standard fragment with RecyclerView rows.

Suggested persistence:

- `SharedPreferences` or `DataStore`

Recommended answer:

- Use `DataStore` if we are comfortable adding it now.
- Use `SharedPreferences` if we want the simplest path for MVP.

## Navigation Refactor Plan

### Phase 1: Rename and stabilize current destinations

- Rename `Home` to `My Collection`
- Rename `Browse` to `All`
- Update strings, labels, icons, and menu IDs
- Keep existing fragments mostly intact to reduce risk

### Phase 2: Decide the six-destination navigation pattern

Choose one of these:

1. Navigation drawer
2. Bottom nav plus `More`
3. Custom top tab/navigation row

Recommended choice:

- `Bottom nav plus More` for the shortest path
- `Navigation drawer` for the cleanest long-term structure

### Phase 3: Add placeholder destinations

Before building full features, create skeleton fragments for:

- `Storage`
- `Wants`
- `Decks`
- `Settings`

This lets navigation be tested end-to-end early.

### Phase 4: Implement features by dependency order

Recommended build order:

1. `My Collection` rename/refine
2. `All` rename/refine
3. `Wants`
4. `Storage`
5. `Decks`
6. `Settings`

Reasoning:

- `My Collection` and `All` are mostly existing work.
- `Wants` is isolated and does not depend on storage or deck logic.
- `Storage` extends owned-card modeling.
- `Decks` is the most feature-heavy because it needs creation flows and ownership comparison.
- `Settings` is flexible and can land at almost any point.

## Technical Impact Summary

### Navigation layer

- Update `bottom_nav_menu.xml`
- Update `nav_graph.xml`
- Update top-level destination config in `MainActivity.kt`
- Potentially replace the current navigation container approach

### Data layer

Likely Room version bump needed for:

- `StorageLocation`
- `WantedCard`
- `Deck`
- `DeckCard`
- Possible `CollectionEntry` update for storage support

### UI layer

New fragments/view models/adapters likely needed for:

- `Storage`
- `Wants`
- `Decks`
- `Settings`

### Migration risk

The database is currently version `1` with seed-on-create behavior. Once new tables are added, we should decide whether to:

- add proper Room migrations, or
- use destructive migration during early development only

Recommended answer:

- Use proper migrations if this app already has meaningful local user data.
- Use destructive migration only if we are still in throwaway prototype mode.

## Suggested First Implementation Slice

The safest first slice is:

1. Refactor navigation labels:
   - `Home` -> `My Collection`
   - `Browse` -> `All`
2. Decide whether six items should be:
   - drawer-based, or
   - bottom-nav-plus-more
3. Add placeholder screens for:
   - `Storage`
   - `Wants`
   - `Decks`
   - `Settings`
4. Land navigation wiring before deeper feature work

This gives us a stable shell before we touch database schema.

## Open Decisions

These are the main product decisions we should resolve before implementation:

1. Should all six destinations always be visible, or is `More` acceptable?
2. Should storage be tracked per card entry or per individual copy?
3. Should wants support priorities, notes, and desired quantities from day one?
4. Should decks track exact prints only, or support flexible card-name matching later?
5. Are we treating this as a prototype where destructive DB migration is okay, or as user data we need to preserve?

## Recommended Next Deep Dives

If we continue this plan, the next useful deep dives are:

1. Final navigation pattern selection
2. Room schema design for `Storage`, `Wants`, and `Decks`
3. MVP wireframe for each new section
4. Fragment/ViewModel file structure before implementation

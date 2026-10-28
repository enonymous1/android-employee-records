# Changelog

> Public export note: this repository is a squashed single-commit export of the 2.0.0 modernization below. The original private history is not included.

## 2.0.0 — 2026-10-28

Modernization of the original AD2 assignment (LyonsJustin15).

- Rewritten in Kotlin with Jetpack Compose Material 3 and Navigation Compose; the XML layouts, fragments, and four activities are gone. One activity hosts list, form, details, and settings destinations.
- Data is persisted with Room in a fresh `employees.db`. The original queried SQLite directly with a new helper and cursor on every spinner event and never closed any of them. Version 1.0 data is not migrated: the schema differs (hire dates are epoch days, not text), so an upgrade starts with an empty list.
- Add and edit are separate paths on the record id. The original switched on the save button's text and fell through from add into update on every create, keying the update on a `"_id"` value a new record did not have. Editing now updates exactly one row and never duplicates it.
- Details loads its employee by id from the database and formats the hire date with `java.time` from the saved setting. The original passed a ContentValues copy of a cursor row in intent extras, parsed an unpadded `yyyy-M-d` string with a `yy-M-d` pattern (crashing on failure), and showed nothing when no date-format preference was set yet.
- The dead `Employee` model is replaced by a real data class. The original model was never instantiated and stored the hire date as a `java.sql.Time`.
- Validation is real: blank and whitespace-only fields are rejected with inline errors, input is trimmed, the employee number must be a whole number that is zero or more, and a hire date must be picked. The original only checked for empty text and stored the number as text in an INTEGER column.
- Sorting is by status or number, ascending or descending, through Room queries. The original rebuilt the same ORDER BY string in two duplicated spinner listeners.
- Settings is a normal destination with back navigation: the date-format choice (DD/MM/YY, YY/MM/DD, MM/DD/YY, persisted in DataStore) and a confirmed delete-all. The original swapped a preference fragment in place of the list and toggled toolbar items by menu index. Delete-all and single delete are both confirmed and by id; `onUpgrade` no longer drops the table, since Room owns the schema at version 1.
- The list shows name, employee number, and employment status, matching the original cells. Form drafts survive rotation.
- Toolchain: AGP 8.13.0, Gradle 8.14.3, Kotlin 2.2.0, Java 17, compile/target SDK 36, min SDK 24, Kotlin DSL with a version catalog, R8 for release builds, core library desugaring for `java.time`.
- Package and application ID changed from `com.example.lyonsjustin15` to `com.enonymous1.employeerecords`; the app is now called Employee Records.
- Added GitHub Actions CI (unit tests + debug build), an MIT LICENSE, and this changelog.
- Removed from tracking: the committed 4.8 MB release APK and the IDE configuration files. They were build output and editor state, not source. The stock launcher icon and the Material 3 color palette from the original are kept.
- Tests: 18 validation/format unit tests, 9 ViewModel tests with fake repositories (including the edit-without-duplicating regression, both sort orders, delete-all, and the date-format default), and 7 instrumented tests (package name plus 6 Compose UI tests covering the empty state, add flow, validation errors, details, edit, delete, and the date-format setting — the UI tests need a device or emulator and are not run in CI).

## 1.0.0

Original AD2 assignment: Java, AppCompat, XML layouts and fragments, SQLiteOpenHelper (`Employees.db`) with a ListView and CursorAdapter, plus a settings fragment for the date format and delete-all. The app was titled "LyonsJustin1.5".

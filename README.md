# Employee Records

A small Android employee records app: add an employee (first name, last name, employee number, hire date, employment status), see them in a sortable list, tap one for details, edit the record, or delete it. Entries are saved on the device, so they survive rotation and app restarts.

This started as a Full Sail AD2 assignment. Version 2.0.0 is a ground-up modernization of that app, keeping its behavior and fixing its bugs — see [CHANGELOG.md](CHANGELOG.md) for the full list.

## What it does

- Four screens: list, add/edit form, employee details, and settings, in one activity with Navigation Compose.
- The list shows every employee with their name, employee number, and employment status, and can be sorted by status or by employee number, ascending or descending.
- Adding an employee updates the list immediately. Editing changes that one record by its database id and never creates a duplicate — the original fell through from add into update on every create.
- Blank or whitespace-only fields are rejected with inline errors, input is trimmed, the employee number must be a whole number that is zero or more, and a hire date must be picked. The original accepted any text and stored it in an INTEGER column unchecked.
- Hire dates are stored as real dates and shown in the format chosen in settings (DD/MM/YY, YY/MM/DD, or MM/DD/YY). The original wrote an unpadded text date, parsed it with a mismatched pattern, and could crash or show nothing.
- Details loads the employee by id from the database instead of reading a copied cursor row from intent extras, and delete asks first, removes exactly that employee by id, and returns to the list.
- Settings also has a confirmed delete-all. Deleting all employees, or one employee, leaves the rest of the list intact.
- Works in portrait and landscape.

No network, no permissions, no accounts, no tracking. Everything lives in a local Room database, and the date-format setting lives in DataStore.

Data from version 1.0 is not carried over: version 2.0 uses a fresh Room database (`employees.db`) with a different schema from the original SQLite file, so an upgrade starts with an empty list.

## Tech stack

- Kotlin 2.2.0, Jetpack Compose Material 3, Navigation Compose
- ViewModel + StateFlow, Room for persistence, DataStore for the date-format setting
- `java.time` hire dates with core library desugaring (min SDK 24)
- AGP 8.13.0, Gradle 8.14.3, Java 17
- Compile/target SDK 36, min SDK 24
- Kotlin DSL with a version catalog, R8 for release builds
- GitHub Actions CI: JVM unit tests + debug build on every push to `main`

## Project structure

```text
app/src/main/java/com/enonymous1/employeerecords/
├── MainActivity.kt            # Builds the Room database, DataStore, and ViewModel, sets the Compose content
├── EmployeesViewModel.kt      # Employee state, sort state, date-format setting, save/delete
├── model/Employee.kt          # Employee model, date formats, sort options, pure validation
├── data/EmployeesDatabase.kt  # Room entity, DAO, database, repository
├── data/SettingsRepository.kt # DataStore date-format setting
└── ui/
    ├── EmployeeRecordsApp.kt  # Navigation host: list / form / details / settings
    ├── ListScreen.kt          # Employee list with empty state and sort controls
    ├── FormScreen.kt          # Add/edit form with date picker and validation errors
    ├── DetailsScreen.kt       # Employee details with edit and confirmed delete
    └── SettingsScreen.kt      # Date-format setting and confirmed delete-all
```

## Build and test

Open the project in Android Studio and run the `app` configuration, or from a terminal:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

The instrumented tests (including the Compose UI tests) need a device or emulator:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

## License

MIT — see [LICENSE](LICENSE).

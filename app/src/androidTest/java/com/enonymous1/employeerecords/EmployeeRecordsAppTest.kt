package com.enonymous1.employeerecords

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.enonymous1.employeerecords.data.DataStoreSettingsRepository
import com.enonymous1.employeerecords.data.EmployeesDatabase
import com.enonymous1.employeerecords.data.RoomEmployeesRepository
import com.enonymous1.employeerecords.ui.EmployeeRecordsApp
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on a device or emulator only; the GitHub Actions workflow runs the
 * JVM unit tests and a debug build, not these tests.
 */
@RunWith(AndroidJUnit4::class)
class EmployeeRecordsAppTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun launch() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, EmployeesDatabase::class.java).build()
        // A fresh settings file per launch, so a format changed in one test
        // cannot leak into the next test's default.
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        ) { context.cacheDir.resolve("settings-${System.nanoTime()}.preferences_pb") }
        val viewModel = EmployeesViewModel(
            RoomEmployeesRepository(database.employeesDao()),
            DataStoreSettingsRepository(dataStore),
        )
        composeRule.setContent {
            MaterialTheme { EmployeeRecordsApp(viewModel) }
        }
    }

    private fun addEmployee(
        firstName: String = "Ada",
        lastName: String = "Lovelace",
        number: String = "101",
        status: String = "Full-time",
    ) {
        composeRule.onNodeWithContentDescription("Add employee").performClick()
        composeRule.onNodeWithTag("firstNameField").performTextInput(firstName)
        composeRule.onNodeWithTag("lastNameField").performTextInput(lastName)
        composeRule.onNodeWithTag("employeeNumberField").performTextInput(number)
        // Accept the date picker's default, today, so the test does not
        // depend on picking a particular calendar day.
        composeRule.onNodeWithTag("pickDateButton").performClick()
        composeRule.onNodeWithText("OK").performClick()
        composeRule.onNodeWithTag("employmentStatusField").performTextInput(status)
        composeRule.onNodeWithTag("saveButton").performClick()
    }

    private fun todayFormatted(pattern: String): String =
        LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern(pattern))

    @Test
    fun emptyStateIsShownBeforeAnythingIsAdded() {
        launch()
        composeRule.onNodeWithText(
            "No employees yet. Add an employee and they will appear here.",
        ).assertIsDisplayed()
    }

    @Test
    fun addingAnEmployeeShowsNameNumberAndStatusInTheList() {
        launch()
        addEmployee()
        composeRule.onNodeWithText("Ada Lovelace").assertIsDisplayed()
        composeRule.onNodeWithText("Employee #101").assertIsDisplayed()
        composeRule.onNodeWithText("Full-time").assertIsDisplayed()
    }

    @Test
    fun emptyFieldsShowValidationErrorsInsteadOfAdding() {
        launch()
        composeRule.onNodeWithContentDescription("Add employee").performClick()
        composeRule.onNodeWithTag("saveButton").performClick()
        composeRule.onNodeWithText("First name is required.").assertIsDisplayed()
    }

    @Test
    fun tappingAnEmployeeOpensDetailsShowingEveryField() {
        launch()
        addEmployee()
        composeRule.onNodeWithText("Ada Lovelace").performClick()
        composeRule.onNodeWithText("Employee details").assertIsDisplayed()
        composeRule.onNodeWithText("Ada").assertIsDisplayed()
        composeRule.onNodeWithText("Lovelace").assertIsDisplayed()
        composeRule.onNodeWithText("101").assertIsDisplayed()
        composeRule.onNodeWithText("Full-time").assertIsDisplayed()
        composeRule.onNodeWithText(todayFormatted("dd/MM/yy")).assertIsDisplayed()
    }

    @Test
    fun editingFromDetailsUpdatesWithoutDuplicating() {
        launch()
        addEmployee()
        composeRule.onNodeWithText("Ada Lovelace").performClick()
        composeRule.onNodeWithContentDescription("Edit employee").performClick()
        composeRule.onNodeWithTag("employmentStatusField").performTextClearance()
        composeRule.onNodeWithTag("employmentStatusField").performTextInput("Contractor")
        composeRule.onNodeWithTag("saveButton").performClick()
        composeRule.onNodeWithText("Contractor").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onAllNodesWithText("Ada Lovelace").assertCountEquals(1)
        composeRule.onNodeWithText("Contractor").assertIsDisplayed()
    }

    @Test
    fun deletingFromDetailsReturnsToTheEmptyList() {
        launch()
        addEmployee()
        composeRule.onNodeWithText("Ada Lovelace").performClick()
        composeRule.onNodeWithContentDescription("Delete employee").performClick()
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.onNodeWithText(
            "No employees yet. Add an employee and they will appear here.",
        ).assertIsDisplayed()
    }

    @Test
    fun changingTheDateFormatChangesTheDetailsDate() {
        launch()
        addEmployee()
        composeRule.onNodeWithText("Ada Lovelace").performClick()
        composeRule.onNodeWithText(todayFormatted("dd/MM/yy")).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithTag("dateFormat-MM/DD/YY").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("Ada Lovelace").performClick()
        composeRule.onNodeWithText(todayFormatted("MM/dd/yy")).assertIsDisplayed()
    }
}

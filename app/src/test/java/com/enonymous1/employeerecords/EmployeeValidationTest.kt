package com.enonymous1.employeerecords

import com.enonymous1.employeerecords.model.DateFormat
import com.enonymous1.employeerecords.model.EmployeeValidation
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmployeeValidationTest {

    private val hireDate = LocalDate.of(2025, 5, 14)

    @Test
    fun blankFirstNameIsAnError() {
        assertEquals(
            listOf("First name is required."),
            EmployeeValidation.validate("", "Lovelace", "101", hireDate, "Full-time"),
        )
    }

    @Test
    fun blankLastNameIsAnError() {
        assertEquals(
            listOf("Last name is required."),
            EmployeeValidation.validate("Ada", "", "101", hireDate, "Full-time"),
        )
    }

    @Test
    fun blankEmploymentStatusIsAnError() {
        assertEquals(
            listOf("Employment status is required."),
            EmployeeValidation.validate("Ada", "Lovelace", "101", hireDate, ""),
        )
    }

    @Test
    fun whitespaceOnlyFieldsAreRejected() {
        val errors = EmployeeValidation.validate("   ", "  ", " ", null, "  ")
        assertEquals(5, errors.size)
    }

    @Test
    fun blankEmployeeNumberIsItsOwnError() {
        assertEquals(
            listOf("Employee number is required."),
            EmployeeValidation.validate("Ada", "Lovelace", "", hireDate, "Full-time"),
        )
    }

    @Test
    fun nonNumericEmployeeNumberIsAnError() {
        assertEquals(
            listOf("Employee number must be a whole number."),
            EmployeeValidation.validate("Ada", "Lovelace", "abc", hireDate, "Full-time"),
        )
    }

    @Test
    fun fractionalEmployeeNumberIsAnError() {
        assertEquals(
            listOf("Employee number must be a whole number."),
            EmployeeValidation.validate("Ada", "Lovelace", "1.5", hireDate, "Full-time"),
        )
    }

    @Test
    fun overflowingEmployeeNumberIsAnError() {
        // toIntOrNull returns null instead of throwing, as parseInt would.
        assertEquals(
            listOf("Employee number must be a whole number."),
            EmployeeValidation.validate(
                "Ada",
                "Lovelace",
                "99999999999999999999",
                hireDate,
                "Full-time",
            ),
        )
    }

    @Test
    fun negativeEmployeeNumberIsAnError() {
        assertEquals(
            listOf("Employee number cannot be negative."),
            EmployeeValidation.validate("Ada", "Lovelace", "-1", hireDate, "Full-time"),
        )
    }

    @Test
    fun missingHireDateIsAnError() {
        assertEquals(
            listOf("Hire date is required."),
            EmployeeValidation.validate("Ada", "Lovelace", "101", null, "Full-time"),
        )
    }

    @Test
    fun zeroEmployeeNumberIsValid() {
        assertTrue(
            EmployeeValidation.validate("Ada", "Lovelace", "0", hireDate, "Full-time").isEmpty(),
        )
    }

    @Test
    fun validInputHasNoErrors() {
        assertTrue(
            EmployeeValidation.validate("Ada", "Lovelace", "101", hireDate, "Full-time").isEmpty(),
        )
    }

    @Test
    fun createTrimsInput() {
        val employee = EmployeeValidation.create(
            firstName = "  Ada ",
            lastName = " Lovelace ",
            employeeNumberText = " 101 ",
            hireDate = hireDate,
            employmentStatus = " Full-time ",
        )
        assertEquals("Ada", employee?.firstName)
        assertEquals("Lovelace", employee?.lastName)
        assertEquals(101, employee?.employeeNumber)
        assertEquals("Full-time", employee?.employmentStatus)
        assertEquals(hireDate, employee?.hireDate)
    }

    @Test
    fun createReturnsNullForInvalidInput() {
        assertNull(
            EmployeeValidation.create(
                firstName = "Ada",
                lastName = "Lovelace",
                employeeNumberText = "abc",
                hireDate = null,
                employmentStatus = "Full-time",
            ),
        )
    }

    @Test
    fun dayMonthYearFormat() {
        assertEquals("14/05/25", DateFormat.DAY_MONTH_YEAR.format(hireDate))
    }

    @Test
    fun yearMonthDayFormat() {
        assertEquals("25/05/14", DateFormat.YEAR_MONTH_DAY.format(hireDate))
    }

    @Test
    fun monthDayYearFormat() {
        assertEquals("05/14/25", DateFormat.MONTH_DAY_YEAR.format(hireDate))
    }

    @Test
    fun unknownDateFormatLabelFallsBackToDefault() {
        assertEquals(DateFormat.DEFAULT, DateFormat.fromLabel("not a format"))
        assertEquals(DateFormat.DEFAULT, DateFormat.fromLabel(null))
    }
}

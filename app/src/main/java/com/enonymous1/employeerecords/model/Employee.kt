package com.enonymous1.employeerecords.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * One employee record. Identity is the Room row id: two employees with the
 * same name are still distinct, and identity never depends on a name or
 * number that the user can edit. The original Java model was never
 * instantiated at all — the UI passed ContentValues between screens — and
 * it stored the hire date as a java.sql.Time, a time of day, not a date.
 */
data class Employee(
    val id: Long = 0,
    val firstName: String,
    val lastName: String,
    val employeeNumber: Int,
    val hireDate: LocalDate,
    val employmentStatus: String,
)

/** How a hire date is shown on the details screen. */
enum class DateFormat(val label: String, private val pattern: String) {
    DAY_MONTH_YEAR("DD/MM/YY", "dd/MM/yy"),
    YEAR_MONTH_DAY("YY/MM/DD", "yy/MM/dd"),
    MONTH_DAY_YEAR("MM/DD/YY", "MM/dd/yy");

    fun format(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern(pattern))

    companion object {
        val DEFAULT = DAY_MONTH_YEAR

        /** Unknown or unset values fall back to the first format in the list. */
        fun fromLabel(label: String?): DateFormat =
            entries.find { it.label == label } ?: DEFAULT
    }
}

/** The list can be sorted by employment status or by employee number. */
enum class SortBy {
    STATUS,
    NUMBER,
}

enum class SortOrder {
    ASCENDING,
    DESCENDING,
}

/** Pure validation and construction, so it can be unit-tested without Android. */
object EmployeeValidation {

    fun validate(
        firstName: String,
        lastName: String,
        employeeNumberText: String,
        hireDate: LocalDate?,
        employmentStatus: String,
    ): List<String> {
        val errors = mutableListOf<String>()
        if (firstName.isBlank()) errors += "First name is required."
        if (lastName.isBlank()) errors += "Last name is required."
        when {
            employeeNumberText.isBlank() ->
                errors += "Employee number is required."
            employeeNumberText.trim().toIntOrNull() == null ->
                errors += "Employee number must be a whole number."
            employeeNumberText.trim().toInt() < 0 ->
                errors += "Employee number cannot be negative."
        }
        if (hireDate == null) errors += "Hire date is required."
        if (employmentStatus.isBlank()) errors += "Employment status is required."
        return errors
    }

    /**
     * Returns null when validation fails. Input is trimmed, so
     * whitespace-only text is rejected, and the employee number is parsed
     * safely — the original stored whatever text was typed into an INTEGER
     * column and never checked it at all.
     */
    fun create(
        id: Long = 0,
        firstName: String,
        lastName: String,
        employeeNumberText: String,
        hireDate: LocalDate?,
        employmentStatus: String,
    ): Employee? {
        if (validate(firstName, lastName, employeeNumberText, hireDate, employmentStatus).isNotEmpty()) {
            return null
        }
        return Employee(
            id = id,
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            employeeNumber = employeeNumberText.trim().toInt(),
            hireDate = hireDate!!,
            employmentStatus = employmentStatus.trim(),
        )
    }
}

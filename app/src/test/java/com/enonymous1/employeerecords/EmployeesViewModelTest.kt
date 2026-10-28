package com.enonymous1.employeerecords

import com.enonymous1.employeerecords.data.EmployeesRepository
import com.enonymous1.employeerecords.data.SettingsRepository
import com.enonymous1.employeerecords.model.DateFormat
import com.enonymous1.employeerecords.model.Employee
import com.enonymous1.employeerecords.model.SortBy
import com.enonymous1.employeerecords.model.SortOrder
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeEmployeesRepository : EmployeesRepository {
    private val state = MutableStateFlow<List<Employee>>(emptyList())
    private var nextId = 1L

    override fun observeEmployees(sortBy: SortBy, sortOrder: SortOrder): Flow<List<Employee>> =
        state.map { list -> sortList(list, sortBy, sortOrder) }

    private fun sortList(
        list: List<Employee>,
        sortBy: SortBy,
        sortOrder: SortOrder,
    ): List<Employee> {
        val sorted = when (sortBy) {
            SortBy.STATUS -> list.sortedBy { it.employmentStatus }
            SortBy.NUMBER -> list.sortedBy { it.employeeNumber }
        }
        return if (sortOrder == SortOrder.DESCENDING) sorted.reversed() else sorted
    }

    override fun observeEmployee(id: Long): Flow<Employee?> =
        state.map { list -> list.find { it.id == id } }

    override suspend fun add(employee: Employee) {
        state.value = state.value + employee.copy(id = nextId++)
    }

    override suspend fun update(employee: Employee) {
        state.value = state.value.map { if (it.id == employee.id) employee else it }
    }

    override suspend fun delete(employee: Employee) {
        state.value = state.value.filterNot { it.id == employee.id }
    }

    override suspend fun deleteAll() {
        state.value = emptyList()
    }
}

private class FakeSettingsRepository : SettingsRepository {
    private val state = MutableStateFlow(DateFormat.DEFAULT)
    override val dateFormat: Flow<DateFormat> = state

    override suspend fun setDateFormat(format: DateFormat) {
        state.value = format
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class EmployeesViewModelTest {

    private val hireDate = LocalDate.of(2025, 5, 14)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        EmployeesViewModel(FakeEmployeesRepository(), FakeSettingsRepository())

    private fun addEmployee(
        viewModel: EmployeesViewModel,
        firstName: String = "Ada",
        lastName: String = "Lovelace",
        number: String = "101",
        status: String = "Full-time",
    ): List<String> = viewModel.saveEmployee(
        id = null,
        firstName = firstName,
        lastName = lastName,
        employeeNumberText = number,
        hireDate = hireDate,
        employmentStatus = status,
    )

    @Test
    fun addingAValidEmployeeAppearsInTheList() = runTest {
        val viewModel = viewModel()
        assertTrue(addEmployee(viewModel).isEmpty())
        advanceUntilIdle()
        assertEquals(1, viewModel.employees.value.size)
        val employee = viewModel.employees.value.single()
        assertEquals("Ada", employee.firstName)
        assertEquals("Lovelace", employee.lastName)
        assertEquals(101, employee.employeeNumber)
        assertEquals(hireDate, employee.hireDate)
        assertEquals("Full-time", employee.employmentStatus)
    }

    @Test
    fun invalidAddReturnsErrorsAndAddsNothing() = runTest {
        val viewModel = viewModel()
        val errors = viewModel.saveEmployee(
            id = null,
            firstName = "Ada",
            lastName = "Lovelace",
            employeeNumberText = "abc",
            hireDate = hireDate,
            employmentStatus = "Full-time",
        )
        assertTrue(errors.isNotEmpty())
        advanceUntilIdle()
        assertTrue(viewModel.employees.value.isEmpty())
    }

    @Test
    fun deletingRemovesOnlyTheEmployeeWithThatId() = runTest {
        // Two employees share a name. Deleting one must never take the other.
        val viewModel = viewModel()
        addEmployee(viewModel, status = "Full-time")
        addEmployee(viewModel, status = "Part-time")
        advanceUntilIdle()
        assertEquals(2, viewModel.employees.value.size)
        viewModel.deleteEmployee(viewModel.employees.value.first())
        advanceUntilIdle()
        assertEquals(listOf("Part-time"), viewModel.employees.value.map { it.employmentStatus })
    }

    @Test
    fun deletingTheLastEmployeeEmptiesTheList() = runTest {
        val viewModel = viewModel()
        addEmployee(viewModel)
        advanceUntilIdle()
        viewModel.deleteEmployee(viewModel.employees.value.single())
        advanceUntilIdle()
        assertTrue(viewModel.employees.value.isEmpty())
    }

    @Test
    fun updatingKeepsTheSameIdAndDoesNotDuplicate() = runTest {
        // Regression test for the original fall-through: every create ran
        // an update too. An edit must change the one row, not add a second.
        val viewModel = viewModel()
        addEmployee(viewModel)
        advanceUntilIdle()
        val original = viewModel.employees.value.single()
        val errors = viewModel.saveEmployee(
            id = original.id,
            firstName = "Ada",
            lastName = "Lovelace",
            employeeNumberText = "101",
            hireDate = hireDate,
            employmentStatus = "Contractor",
        )
        assertTrue(errors.isEmpty())
        advanceUntilIdle()
        assertEquals(1, viewModel.employees.value.size)
        val updated = viewModel.employees.value.single()
        assertEquals(original.id, updated.id)
        assertEquals("Contractor", updated.employmentStatus)
    }

    @Test
    fun sortingByNumberDescendingOrdersByEmployeeNumber() = runTest {
        val viewModel = viewModel()
        addEmployee(viewModel, number = "2")
        addEmployee(viewModel, number = "10")
        addEmployee(viewModel, number = "1")
        advanceUntilIdle()
        viewModel.setSortBy(SortBy.NUMBER)
        viewModel.setSortOrder(SortOrder.DESCENDING)
        advanceUntilIdle()
        assertEquals(
            listOf(10, 2, 1),
            viewModel.employees.value.map { it.employeeNumber },
        )
    }

    @Test
    fun sortingByStatusAscendingOrdersByStatus() = runTest {
        val viewModel = viewModel()
        addEmployee(viewModel, status = "Part-time")
        addEmployee(viewModel, status = "Full-time")
        advanceUntilIdle()
        viewModel.setSortBy(SortBy.STATUS)
        viewModel.setSortOrder(SortOrder.ASCENDING)
        advanceUntilIdle()
        assertEquals(
            listOf("Full-time", "Part-time"),
            viewModel.employees.value.map { it.employmentStatus },
        )
    }

    @Test
    fun deleteAllEmptiesTheList() = runTest {
        val viewModel = viewModel()
        addEmployee(viewModel)
        addEmployee(viewModel, number = "102")
        advanceUntilIdle()
        viewModel.deleteAllEmployees()
        advanceUntilIdle()
        assertTrue(viewModel.employees.value.isEmpty())
    }

    @Test
    fun dateFormatDefaultsAndCanBeChanged() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(DateFormat.DAY_MONTH_YEAR, viewModel.dateFormat.value)
        viewModel.setDateFormat(DateFormat.MONTH_DAY_YEAR)
        advanceUntilIdle()
        assertEquals(DateFormat.MONTH_DAY_YEAR, viewModel.dateFormat.value)
    }
}

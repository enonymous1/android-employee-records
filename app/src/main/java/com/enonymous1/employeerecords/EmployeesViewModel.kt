package com.enonymous1.employeerecords

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.enonymous1.employeerecords.data.EmployeesRepository
import com.enonymous1.employeerecords.data.SettingsRepository
import com.enonymous1.employeerecords.model.DateFormat
import com.enonymous1.employeerecords.model.Employee
import com.enonymous1.employeerecords.model.EmployeeValidation
import com.enonymous1.employeerecords.model.SortBy
import com.enonymous1.employeerecords.model.SortOrder
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Owns the employee state. The list is a live flow from the repository, so
 * adding, editing, or deleting an employee updates the list immediately, and
 * nothing is lost on rotation or when returning from another screen — the
 * original re-queried with a fresh cursor on every spinner event, leaked
 * every cursor it opened, and rebuilt the list fragment on restart.
 */
class EmployeesViewModel(
    private val repository: EmployeesRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val sortBy = MutableStateFlow(SortBy.STATUS)
    private val sortOrder = MutableStateFlow(SortOrder.ASCENDING)

    val sortByState: StateFlow<SortBy> = sortBy.asStateFlow()
    val sortOrderState: StateFlow<SortOrder> = sortOrder.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val employees: StateFlow<List<Employee>> =
        combine(sortBy, sortOrder) { by, order -> by to order }
            .flatMapLatest { (by, order) -> repository.observeEmployees(by, order) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val dateFormat: StateFlow<DateFormat> = settingsRepository.dateFormat
        .stateIn(viewModelScope, SharingStarted.Eagerly, DateFormat.DEFAULT)

    fun setSortBy(sortBy: SortBy) {
        this.sortBy.value = sortBy
    }

    fun setSortOrder(sortOrder: SortOrder) {
        this.sortOrder.value = sortOrder
    }

    fun setDateFormat(format: DateFormat) {
        viewModelScope.launch { settingsRepository.setDateFormat(format) }
    }

    fun employee(id: Long): Flow<Employee?> = repository.observeEmployee(id)

    /**
     * Saves a new employee when [id] is null, or updates the employee with
     * that id otherwise. Returns validation errors, or an empty list when
     * the save was queued. Add and update are separate paths — the original
     * switched on the save button's text and fell through from add into
     * update on every create.
     */
    fun saveEmployee(
        id: Long?,
        firstName: String,
        lastName: String,
        employeeNumberText: String,
        hireDate: LocalDate?,
        employmentStatus: String,
    ): List<String> {
        val errors = EmployeeValidation.validate(
            firstName = firstName,
            lastName = lastName,
            employeeNumberText = employeeNumberText,
            hireDate = hireDate,
            employmentStatus = employmentStatus,
        )
        if (errors.isNotEmpty()) return errors
        val employee = EmployeeValidation.create(
            id = id ?: 0,
            firstName = firstName,
            lastName = lastName,
            employeeNumberText = employeeNumberText,
            hireDate = hireDate,
            employmentStatus = employmentStatus,
        ) ?: return errors
        viewModelScope.launch {
            if (id == null) repository.add(employee) else repository.update(employee)
        }
        return emptyList()
    }

    fun deleteEmployee(employee: Employee) {
        viewModelScope.launch { repository.delete(employee) }
    }

    fun deleteAllEmployees() {
        viewModelScope.launch { repository.deleteAll() }
    }
}

class EmployeesViewModelFactory(
    private val repository: EmployeesRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EmployeesViewModel::class.java)) {
            return EmployeesViewModel(repository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

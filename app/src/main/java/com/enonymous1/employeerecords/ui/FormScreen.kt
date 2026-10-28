package com.enonymous1.employeerecords.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.enonymous1.employeerecords.EmployeesViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * The add/edit form. One screen serves both: [employeeId] is 0 for a new
 * employee, and the id of an existing employee when editing from details.
 * Draft text survives rotation (rememberSaveable), and saving validates
 * before anything is stored. The original decided add-vs-update by reading
 * the save button's text and fell through from add into update on every
 * create; here the two paths are separate branches on the id.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(
    viewModel: EmployeesViewModel,
    employeeId: Long,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEditing = employeeId != 0L
    val existing by viewModel.employee(employeeId).collectAsState(initial = null)
    val dateFormat by viewModel.dateFormat.collectAsState()

    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var employeeNumberText by rememberSaveable { mutableStateOf("") }
    var employmentStatus by rememberSaveable { mutableStateOf("") }
    var hireDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var formInitialized by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var errors by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(existing) {
        val employee = existing
        if (employee != null && !formInitialized) {
            firstName = employee.firstName
            lastName = employee.lastName
            employeeNumberText = employee.employeeNumber.toString()
            employmentStatus = employee.employmentStatus
            hireDateMillis = employee.hireDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
            formInitialized = true
        }
    }

    val hireDate: LocalDate? = hireDateMillis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
    }

    if (showDatePicker) {
        val todayMillis = LocalDate.now(ZoneOffset.UTC)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = hireDateMillis ?: todayMillis,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        hireDateMillis = datePickerState.selectedDateMillis
                        showDatePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit employee" else "Add employee") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "First name", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("firstNameField"),
            )
            Text(text = "Last name", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("lastNameField"),
            )
            Text(text = "Employee number", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = employeeNumberText,
                onValueChange = { employeeNumberText = it },
                label = { Text("Employee Number") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("employeeNumberField"),
            )
            Text(text = "Hire date", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth().testTag("pickDateButton"),
            ) {
                Text(
                    hireDate?.let { "Hire date: ${dateFormat.format(it)}" }
                        ?: "Pick hire date",
                )
            }
            Text(text = "Employment status", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = employmentStatus,
                onValueChange = { employmentStatus = it },
                label = { Text("Employment Status") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("employmentStatusField"),
            )
            errors.forEach { error ->
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = {
                    errors = viewModel.saveEmployee(
                        id = if (isEditing) employeeId else null,
                        firstName = firstName,
                        lastName = lastName,
                        employeeNumberText = employeeNumberText,
                        hireDate = hireDate,
                        employmentStatus = employmentStatus,
                    )
                    if (errors.isEmpty()) onSaved()
                },
                modifier = Modifier.fillMaxWidth().testTag("saveButton"),
            ) { Text(if (isEditing) "Update employee" else "Add employee") }
        }
    }
}

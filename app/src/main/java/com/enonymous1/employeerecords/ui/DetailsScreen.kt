package com.enonymous1.employeerecords.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.enonymous1.employeerecords.EmployeesViewModel

/**
 * Details for one employee, loaded by id from the database and formatted
 * with the saved date-format setting. The original passed a ContentValues
 * copy of a cursor row in intent extras, parsed the hire date with a
 * mismatched pattern that could crash, and finished the activity on
 * restart; this screen cannot see another employee's data, and delete
 * removes exactly this row by id.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    viewModel: EmployeesViewModel,
    employeeId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val employee by viewModel.employee(employeeId).collectAsState(initial = null)
    val dateFormat by viewModel.dateFormat.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog && employee != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete employee record") },
            text = {
                Text(
                    "You are about to delete the record for " +
                        "${employee!!.firstName} ${employee!!.lastName}, continue?",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteEmployee(employee!!)
                        onDeleted()
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Employee details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit employee",
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete employee",
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (employee == null) {
                Text(text = "Employee not found.")
            } else {
                Text(text = "First name", style = MaterialTheme.typography.titleMedium)
                Text(text = employee!!.firstName, style = MaterialTheme.typography.bodyLarge)
                Text(text = "Last name", style = MaterialTheme.typography.titleMedium)
                Text(text = employee!!.lastName, style = MaterialTheme.typography.bodyLarge)
                Text(text = "Employee number", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = employee!!.employeeNumber.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(text = "Employment status", style = MaterialTheme.typography.titleMedium)
                Text(text = employee!!.employmentStatus, style = MaterialTheme.typography.bodyLarge)
                Text(text = "Hire date", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = dateFormat.format(employee!!.hireDate),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

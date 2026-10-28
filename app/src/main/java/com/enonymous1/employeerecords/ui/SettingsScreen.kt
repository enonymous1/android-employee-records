package com.enonymous1.employeerecords.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.enonymous1.employeerecords.EmployeesViewModel
import com.enonymous1.employeerecords.model.DateFormat

/**
 * Settings: the hire-date display format, and delete-all. The original
 * swapped a preference fragment in place of the list without a back stack
 * and toggled toolbar items by index; this is a normal destination with
 * back navigation, and delete-all stays behind a confirmation dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: EmployeesViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormat by viewModel.dateFormat.collectAsState()
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete all records") },
            text = {
                Text(
                    "Are you sure you want to delete all records? " +
                        "This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAllDialog = false
                        viewModel.deleteAllEmployees()
                        onDeleted()
                    },
                ) { Text("Delete all") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "Date format", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "How hire dates are shown on the details screen.",
                style = MaterialTheme.typography.bodyMedium,
            )
            DateFormat.entries.forEach { format ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dateFormat-${format.label}")
                        .clickable { viewModel.setDateFormat(format) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = dateFormat == format,
                        onClick = { viewModel.setDateFormat(format) },
                    )
                    Text(text = format.label)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Data", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = { showDeleteAllDialog = true },
                modifier = Modifier.fillMaxWidth().testTag("deleteAllButton"),
            ) { Text("Delete all employees") }
        }
    }
}

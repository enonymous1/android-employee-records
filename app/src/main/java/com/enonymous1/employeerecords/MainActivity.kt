package com.enonymous1.employeerecords

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.enonymous1.employeerecords.data.DataStoreSettingsRepository
import com.enonymous1.employeerecords.data.EmployeesDatabase
import com.enonymous1.employeerecords.data.RoomEmployeesRepository
import com.enonymous1.employeerecords.data.settingsDataStore
import com.enonymous1.employeerecords.ui.EmployeeRecordsApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val database = Room.databaseBuilder(
            applicationContext,
            EmployeesDatabase::class.java,
            "employees.db",
        ).build()
        val viewModel = ViewModelProvider(
            this,
            EmployeesViewModelFactory(
                RoomEmployeesRepository(database.employeesDao()),
                DataStoreSettingsRepository(applicationContext.settingsDataStore),
            ),
        )[EmployeesViewModel::class.java]
        setContent {
            MaterialTheme {
                EmployeeRecordsApp(viewModel)
            }
        }
    }
}

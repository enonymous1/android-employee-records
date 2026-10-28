package com.enonymous1.employeerecords.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.enonymous1.employeerecords.EmployeesViewModel

/**
 * One activity, four destinations. The original used four activities and
 * five fragments passing ContentValues copies of a cursor row between them;
 * here every screen reads the same ViewModel, so the list on screen is
 * always the list in the database, and details always shows the employee
 * with the id in the route.
 */
@Composable
fun EmployeeRecordsApp(viewModel: EmployeesViewModel, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "list",
        modifier = modifier,
    ) {
        composable("list") {
            val employees by viewModel.employees.collectAsState()
            val sortBy by viewModel.sortByState.collectAsState()
            val sortOrder by viewModel.sortOrderState.collectAsState()
            ListScreen(
                employees = employees,
                sortBy = sortBy,
                sortOrder = sortOrder,
                onSortByChange = viewModel::setSortBy,
                onSortOrderChange = viewModel::setSortOrder,
                onAddEmployee = { navController.navigate("form") },
                onEmployeeClick = { employee ->
                    navController.navigate("details/${employee.id}")
                },
                onSettingsClick = { navController.navigate("settings") },
            )
        }
        composable("form") {
            FormScreen(
                viewModel = viewModel,
                employeeId = 0L,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "form/{employeeId}",
            arguments = listOf(navArgument("employeeId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val employeeId = backStackEntry.arguments?.getLong("employeeId") ?: 0L
            FormScreen(
                viewModel = viewModel,
                employeeId = employeeId,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "details/{employeeId}",
            arguments = listOf(navArgument("employeeId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val employeeId = backStackEntry.arguments?.getLong("employeeId") ?: 0L
            DetailsScreen(
                viewModel = viewModel,
                employeeId = employeeId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate("form/$employeeId") },
                onDeleted = { navController.popBackStack() },
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() },
            )
        }
    }
}

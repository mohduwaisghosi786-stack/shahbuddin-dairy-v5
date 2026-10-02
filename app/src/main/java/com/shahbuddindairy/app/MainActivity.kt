package com.shahbuddindairy.app

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shahbuddindairy.app.ui.Screen
import com.shahbuddindairy.app.ui.screens.BackupViewModel
import com.shahbuddindairy.app.ui.screens.CustomerDetailScreen
import com.shahbuddindairy.app.ui.screens.CustomerDetailViewModel
import com.shahbuddindairy.app.ui.screens.CustomerDetailViewModelFactory
import com.shahbuddindairy.app.ui.screens.CustomersScreen
import com.shahbuddindairy.app.ui.screens.CustomersViewModel
import com.shahbuddindairy.app.ui.screens.DailyTaskScreen
import com.shahbuddindairy.app.ui.screens.DailyTaskViewModel
import com.shahbuddindairy.app.ui.screens.MilkSaleScreen
import com.shahbuddindairy.app.ui.screens.ReportsScreen
import com.shahbuddindairy.app.ui.screens.ReportsViewModel
import com.shahbuddindairy.app.ui.screens.TotalHisabScreen
import com.shahbuddindairy.app.ui.theme.ShahbuddinDairyTheme

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShahbuddinDairyTheme {
                MainAppNavHost()
            }
        }
    }
}

@Composable
fun MainAppNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val colorScheme = MaterialTheme.colorScheme

    val bottomNavItems = listOf(
        BottomNavItem(Screen.DailyTask.route, "Daily Task", Icons.Default.Today),
        BottomNavItem(Screen.MilkSale.route, "Milk Sale", Icons.Default.LocalDrink),
        BottomNavItem(Screen.Reports.route, "Daily Report", Icons.Default.Assessment),
        BottomNavItem(Screen.Customers.route, "Customer Khata", Icons.Default.People),
        BottomNavItem(Screen.TotalHisab.route, "Total Hisab", Icons.Default.Calculate)
    )

    // Hide bottom bar on detail screens to maximize vertical screen space for Khata ledger
    val showBottomBar = currentRoute in bottomNavItems.map { it.route } || currentRoute == "backup"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route || (item.route == Screen.TotalHisab.route && currentRoute == "backup")
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colorScheme.primary,
                                selectedTextColor = colorScheme.primary,
                                indicatorColor = colorScheme.primaryContainer,
                                unselectedIconColor = colorScheme.onSurfaceVariant,
                                unselectedTextColor = colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.DailyTask.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.DailyTask.route) {
                val dailyViewModel: DailyTaskViewModel = viewModel()
                DailyTaskScreen(
                    viewModel = dailyViewModel,
                    onNavigateToCustomerKhata = { customerId ->
                        navController.navigate(Screen.CustomerDetail.createRoute(customerId))
                    }
                )
            }

            composable(Screen.MilkSale.route) {
                val dailyViewModel: DailyTaskViewModel = viewModel()
                MilkSaleScreen(
                    viewModel = dailyViewModel,
                    onNavigateToCustomerKhata = { customerId ->
                        navController.navigate(Screen.CustomerDetail.createRoute(customerId))
                    }
                )
            }

            composable(Screen.Reports.route) {
                val reportsViewModel: ReportsViewModel = viewModel()
                ReportsScreen(
                    viewModel = reportsViewModel,
                    onNavigateToCustomerKhata = { customerId ->
                        navController.navigate(Screen.CustomerDetail.createRoute(customerId))
                    }
                )
            }

            composable(Screen.Customers.route) {
                val customersViewModel: CustomersViewModel = viewModel()
                CustomersScreen(
                    viewModel = customersViewModel,
                    onNavigateToCustomerDetail = { customerId ->
                        navController.navigate(Screen.CustomerDetail.createRoute(customerId))
                    }
                )
            }

            composable(Screen.TotalHisab.route) {
                val backupViewModel: BackupViewModel = viewModel()
                TotalHisabScreen(viewModel = backupViewModel)
            }

            composable("backup") {
                val backupViewModel: BackupViewModel = viewModel()
                TotalHisabScreen(viewModel = backupViewModel)
            }

            composable(
                route = Screen.CustomerDetail.route,
                arguments = listOf(
                    navArgument("customerId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val customerId = backStackEntry.arguments?.getLong("customerId") ?: 0L
                val app = LocalContext.current.applicationContext as Application
                val detailViewModel: CustomerDetailViewModel = viewModel(
                    factory = CustomerDetailViewModelFactory(app, customerId)
                )
                CustomerDetailScreen(
                    viewModel = detailViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

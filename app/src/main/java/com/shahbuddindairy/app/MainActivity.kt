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
import androidx.compose.material.icons.filled.Dashboard
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
import androidx.compose.runtime.collectAsState
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
import com.shahbuddindairy.app.ui.screens.DashboardScreen
import com.shahbuddindairy.app.ui.screens.DashboardViewModel
import com.shahbuddindairy.app.ui.screens.MilkSaleScreen
import com.shahbuddindairy.app.ui.screens.ReportsScreen
import com.shahbuddindairy.app.ui.screens.ReportsViewModel
import com.shahbuddindairy.app.ui.screens.TotalHisabScreen
import com.shahbuddindairy.app.ui.theme.ShahbuddinDairyTheme

import androidx.compose.material.icons.filled.Description
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.shahbuddindairy.app.ui.screens.NotesScreen
import com.shahbuddindairy.app.ui.screens.NotesViewModel
import com.shahbuddindairy.app.security.AppLockManager
import com.shahbuddindairy.app.ui.screens.PinLockScreen

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

class MainActivity : ComponentActivity() {
    private lateinit var appLockManager: AppLockManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appLockManager = AppLockManager.getInstance(this)
        setContent {
            ShahbuddinDairyTheme {
                val isLockEnabled by appLockManager.isAppLockEnabled.collectAsState()
                val isUnlocked by appLockManager.isUnlocked.collectAsState()

                if (isLockEnabled && !isUnlocked) {
                    PinLockScreen(
                        onPinEntered = { pin ->
                            appLockManager.unlockWithPin(pin)
                        }
                    )
                } else {
                    MainAppNavHost()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appLockManager.onAppMovedToForeground()
    }

    override fun onPause() {
        super.onPause()
        appLockManager.onAppMovedToBackground()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainAppNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val colorScheme = MaterialTheme.colorScheme

    val bottomNavItems = listOf(
        BottomNavItem(Screen.Dashboard.route, "Dashboard", Icons.Default.Dashboard),
        BottomNavItem(Screen.DailyTask.route, "Runner", Icons.Default.Today),
        BottomNavItem(Screen.MilkSale.route, "Milk Sale", Icons.Default.LocalDrink),
        BottomNavItem(Screen.Reports.route, "Daily Report", Icons.Default.Assessment),
        BottomNavItem(Screen.Notes.route, "Notes", Icons.Default.Description),
        BottomNavItem(Screen.Customers.route, "Customer Khata", Icons.Default.People),
        BottomNavItem(Screen.TotalHisab.route, "Total Hisab", Icons.Default.Calculate)
    )

    // In Runner Mode, run in full immersion without bottom bar so delivery cards & keyboard get 100% height
    val showBottomBar = currentRoute != Screen.DailyTask.route &&
        (currentRoute in bottomNavItems.map { it.route } || currentRoute == "backup")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    color = colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val navScrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(navScrollState)
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bottomNavItems.forEach { item ->
                            val isSelected = currentRoute == item.route || (item.route == Screen.TotalHisab.route && currentRoute == "backup")
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (currentRoute != item.route) {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(height = 30.dp, width = 50.dp)
                                        .clip(RoundedCornerShape(15.dp))
                                        .background(if (isSelected) colorScheme.primaryContainer else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.title,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                val dashboardViewModel: DashboardViewModel = viewModel()
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToDailyTask = { navController.navigate(Screen.DailyTask.route) },
                    onNavigateToKhata = { navController.navigate(Screen.Customers.route) },
                    onNavigateToReport = { navController.navigate(Screen.Reports.route) }
                )
            }

            composable(Screen.DailyTask.route) {
                val dailyViewModel: DailyTaskViewModel = viewModel()
                DailyTaskScreen(
                    viewModel = dailyViewModel,
                    onNavigateToCustomerKhata = { customerId ->
                        navController.navigate(Screen.CustomerDetail.createRoute(customerId))
                    },
                    onNavigateBack = {
                        navController.popBackStack()
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

            composable(Screen.Notes.route) {
                val notesViewModel: NotesViewModel = viewModel()
                NotesScreen(
                    viewModel = notesViewModel,
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

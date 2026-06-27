package com.zeal.btctrack.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zeal.btctrack.AppContainer
import com.zeal.btctrack.ui.screens.AddAddressScreen
import com.zeal.btctrack.ui.screens.DashboardScreen
import com.zeal.btctrack.ui.screens.EditAddressScreen
import com.zeal.btctrack.ui.screens.ExportScreen
import com.zeal.btctrack.ui.screens.GroupedAddressesScreen
import com.zeal.btctrack.ui.screens.ImportScreen
import com.zeal.btctrack.ui.screens.ProxySettingsScreen
import com.zeal.btctrack.ui.screens.QrScanScreen
import com.zeal.btctrack.ui.screens.SettingsScreen

private data class TabItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun AppNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val tabs = listOf(
        TabItem(Routes.OverviewTab.route, "Overview", Icons.Default.Home),
        TabItem(Routes.AddressesTab.route, "Addresses", Icons.Default.List),
        TabItem(Routes.SettingsTab.route, "Settings", Icons.Default.Settings),
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.OverviewTab.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            navigation(
                startDestination = Routes.Dashboard.route,
                route = Routes.OverviewTab.route,
            ) {
                composable(Routes.Dashboard.route) {
                    DashboardScreen(container = container)
                }
            }

            navigation(
                startDestination = Routes.AddressList.route,
                route = Routes.AddressesTab.route,
            ) {
                composable(Routes.AddressList.route) {
                    GroupedAddressesScreen(
                        container = container,
                        onAddClick = { navController.navigate(Routes.AddAddress.route) },
                        onEditClick = { address ->
                            navController.navigate(Routes.EditAddress.withAddress(address))
                        },
                    )
                }
                composable(Routes.AddAddress.route) { backStackEntry ->
                    val scannedAddressFlow = backStackEntry.savedStateHandle
                        .getStateFlow<String?>("scanned_address", null)
                    AddAddressScreen(
                        container = container,
                        scannedAddressFlow = scannedAddressFlow,
                        onBack = { navController.popBackStack() },
                        onScanQr = { navController.navigate(Routes.QrScan.route) },
                    )
                }
                composable(
                    route = Routes.EditAddress.route,
                    arguments = listOf(navArgument("address") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val address = backStackEntry.arguments?.getString("address") ?: return@composable
                    EditAddressScreen(
                        container = container,
                        address = address,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.QrScan.route) {
                    QrScanScreen(
                        onScanned = { address ->
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("scanned_address", address)
                            navController.popBackStack()
                        },
                        onCancel = { navController.popBackStack() },
                    )
                }
            }

            navigation(
                startDestination = Routes.Settings.route,
                route = Routes.SettingsTab.route,
            ) {
                composable(Routes.Settings.route) {
                    SettingsScreen(
                        container = container,
                        onProxySettingsClick = { navController.navigate(Routes.ProxySettings.route) },
                        onImportClick = { navController.navigate(Routes.Import.route) },
                        onExportClick = { navController.navigate(Routes.Export.route) },
                    )
                }
                composable(Routes.ProxySettings.route) {
                    ProxySettingsScreen(
                        container = container,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.Import.route) {
                    ImportScreen(
                        container = container,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.Export.route) {
                    ExportScreen(
                        container = container,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

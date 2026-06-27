package com.zeal.btctrack.ui.navigation

sealed class Routes(val route: String) {
    // Tab roots (used by NavigationBar item selection)
    data object OverviewTab : Routes("overview_tab")
    data object AddressesTab : Routes("addresses_tab")
    data object SettingsTab : Routes("settings_tab")

    // Overview sub-routes
    data object Dashboard : Routes("dashboard")

    // Addresses sub-routes
    data object AddressList : Routes("address_list")
    data object AddAddress : Routes("add_address")
    data object EditAddress : Routes("edit_address/{address}") {
        fun withAddress(address: String) = "edit_address/$address"
    }
    data object QrScan : Routes("qr_scan")

    // Settings sub-routes
    data object Settings : Routes("settings_main")
    data object ProxySettings : Routes("proxy_settings")
    data object Import : Routes("import")
    data object Export : Routes("export")
}

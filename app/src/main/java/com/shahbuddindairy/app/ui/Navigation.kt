package com.shahbuddindairy.app.ui

sealed class Screen(val route: String, val title: String) {
    object Dashboard : Screen("dashboard", "Dashboard")
    object DailyTask : Screen("daily_task", "Daily Task")
    object MilkSale : Screen("milk_sale", "Milk Sale")
    object Reports : Screen("reports", "Daily Report")
    object Customers : Screen("customers", "Customer Khata")
    object TotalHisab : Screen("total_hisab", "Total Hisab")
    object Notes : Screen("notes", "Notes")

    // Aliases for backwards compatibility
    val Backup get() = TotalHisab

    object CustomerDetail : Screen("customer_detail/{customerId}", "Khata") {
        fun createRoute(customerId: Long) = "customer_detail/$customerId"
    }
}

package com.sarsonasgino.quickinvoicemobile.main

interface MainContract {
    interface View {
        fun updateNav(isLoggedIn: Boolean)
        fun toggleDrawer(isVisible: Boolean)
        fun scrollTo(y: Int)
        fun navigateToLogin()
        fun navigateToDashboard()
        fun navigateToCreateInvoice()
        fun showProfilePopup(anchor: android.view.View)
        fun showToast(message: String)
    }

    interface Presenter {
        fun checkSession()
        fun onActionClicked() // For "Get Started" / "Start Now"
        fun onLogoutConfirmed()
        fun toggleMenu(currentVisibility: Int)
        fun onDestroy()
    }
}
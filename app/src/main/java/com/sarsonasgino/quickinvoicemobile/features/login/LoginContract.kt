package com.sarsonasgino.quickinvoicemobile.features.login

interface LoginContract {
    interface View {
        fun showLoading()
        fun hideLoading()
        fun onLoginSuccess()
        fun onError(message: String)
        fun navigateToRegister()
        fun navigateToDashboard()
        fun setPasswordVisibility(visible: Boolean)
    }

    interface Presenter {
        fun login(email: String, password: String)
        fun checkSession()
        fun togglePasswordVisibility(currentlyVisible: Boolean)
        fun onDestroy()
    }
}
package com.sarsonasgino.quickinvoicemobile.features.register

interface RegisterContract {
    interface View {
        fun showLoading()
        fun hideLoading()
        fun onRegisterSuccess()
        fun onError(message: String)
        fun navigateToDashboard()
        fun navigateToLogin()
        fun setPasswordVisibility(visible: Boolean, isConfirmField: Boolean)
    }

    interface Presenter {
        fun register(firstName: String, lastName: String, email: String, pass: String, confirmPass: String)
        fun toggleVisibility(currentlyVisible: Boolean, isConfirmField: Boolean)
        fun onDestroy()
    }
}
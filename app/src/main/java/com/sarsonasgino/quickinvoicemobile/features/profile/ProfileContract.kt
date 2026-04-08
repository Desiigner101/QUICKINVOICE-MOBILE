package com.sarsonasgino.quickinvoicemobile.features.profile

interface ProfileContract {

    interface View {
        fun showLoading()
        fun hideLoading()
        fun showProfile(firstName: String, lastName: String, email: String, photoUrl: String)
        fun showSuccess(message: String)
        fun showError(message: String)
        fun showPasswordMismatch()
        fun navigateToLogin()
        fun showDeleteConfirmation()
    }

    interface Presenter {
        fun loadProfile()
        fun onUpdateProfile(firstName: String, lastName: String, password: String, confirmPassword: String)
        fun onDeleteAccount()
        fun onConfirmDelete()
        fun onPhotoSelected(base64Photo: String)
        fun onDestroy()
    }
}
package com.sarsonasgino.quickinvoicemobile.features.profile

import android.content.Context
import com.sarsonasgino.quickinvoicemobile.core.model.UpdateProfileRequest
import com.sarsonasgino.quickinvoicemobile.core.network.RetrofitClient
import com.sarsonasgino.quickinvoicemobile.core.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfilePresenter(
    private var view: ProfileContract.View?,
    private val context: Context
) : ProfileContract.Presenter {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private var currentPhotoBase64: String = ""

    override fun loadProfile() {
        val firstName = SessionManager.getFirstName(context)
        val lastName = SessionManager.getLastName(context)
        val email = SessionManager.getEmail(context)
        val photoUrl = SessionManager.getPhotoUrl(context)
        view?.showProfile(firstName, lastName, email, photoUrl)
    }

    override fun onUpdateProfile(firstName: String, lastName: String, password: String, confirmPassword: String) {
        if (firstName.isEmpty() || lastName.isEmpty()) {
            view?.showError("First name and last name are required")
            return
        }

        if (password.isNotEmpty()) {
            if (password.length < 8) {
                view?.showError("Password must be at least 8 characters")
                return
            }
            if (password != confirmPassword) {
                view?.showPasswordMismatch()
                return
            }
        }

        val token = SessionManager.getToken(context)
        if (token != null) RetrofitClient.setToken(token)

        view?.showLoading()

        scope.launch {
            try {
                val request = UpdateProfileRequest(
                    firstName = firstName,
                    lastName = lastName,
                    password = if (password.isNotEmpty()) password else null,
                    photoUrl = if (currentPhotoBase64.isNotEmpty()) currentPhotoBase64 else null
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.api.updateProfile(request)
                }

                if (response.isSuccessful) {
                    val body = response.body()
                    SessionManager.saveUserInfo(
                        context,
                        body?.firstName ?: firstName,
                        body?.lastName ?: lastName,
                        body?.email ?: SessionManager.getEmail(context),
                        currentPhotoBase64
                    )
                    view?.showSuccess("Profile updated successfully! ✅")
                } else {
                    view?.showError("Failed to update profile")
                }
            } catch (e: Exception) {
                view?.showError("Network error: ${e.message}")
            } finally {
                view?.hideLoading()
            }
        }
    }

    override fun onDeleteAccount() {
        view?.showDeleteConfirmation()
    }

    override fun onConfirmDelete() {
        val token = SessionManager.getToken(context)
        if (token != null) RetrofitClient.setToken(token)

        view?.showLoading()

        scope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.api.deleteAccount()
                }

                if (response.isSuccessful) {
                    SessionManager.clearSession(context)
                    view?.navigateToLogin()
                } else {
                    view?.showError("Failed to delete account")
                }
            } catch (e: Exception) {
                view?.showError("Network error: ${e.message}")
            } finally {
                view?.hideLoading()
            }
        }
    }

    override fun onPhotoSelected(base64Photo: String) {
        currentPhotoBase64 = base64Photo
    }

    override fun onDestroy() {
        job.cancel()
        view = null
    }
}
package com.sarsonasgino.quickinvoicemobile.features.register

import android.content.Context
import com.sarsonasgino.quickinvoicemobile.core.model.RegisterRequest
import com.sarsonasgino.quickinvoicemobile.core.network.RetrofitClient
import com.sarsonasgino.quickinvoicemobile.core.utils.SessionManager
import kotlinx.coroutines.*

class RegisterPresenter(
    private var view: RegisterContract.View?,
    private val context: Context
) : RegisterContract.Presenter {

    private val presenterScope = CoroutineScope(Dispatchers.Main + Job())

    override fun register(firstName: String, lastName: String, email: String, pass: String, confirmPass: String) {
        // 1. Validation Logic
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            view?.onError("Please fill in all fields")
            return
        }

        if (pass.length < 8) {
            view?.onError("Password must be at least 8 characters")
            return
        }

        if (pass != confirmPass) {
            view?.onError("Passwords do not match!")
            return
        }

        // 2. Network Call
        view?.showLoading()
        presenterScope.launch {
            try {
                val response = RetrofitClient.api.register(
                    RegisterRequest(email, pass, firstName, lastName)
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    val token = body?.token
                    val userId = body?.userId

                    if (token != null && userId != null) {
                        SessionManager.saveUserInfo(
                            context,
                            body.firstName ?: "",
                            body.lastName ?: "",
                            body.email ?: ""
                        )
                        SessionManager.saveToken(context, token)
                        SessionManager.saveClerkId(context, userId)
                        RetrofitClient.setToken(token)
                        view?.onRegisterSuccess()
                    } else {
                        view?.onError("Registration failed. Missing data.")
                    }
                } else {
                    view?.onError("Registration failed. Email may already be in use.")
                }
            } catch (e: Exception) {
                view?.onError("Network error: ${e.message}")
            } finally {
                view?.hideLoading()
            }
        }
    }

    override fun toggleVisibility(currentlyVisible: Boolean, isConfirmField: Boolean) {
        view?.setPasswordVisibility(!currentlyVisible, isConfirmField)
    }

    override fun onDestroy() {
        presenterScope.cancel()
        view = null
    }
}
package com.sarsonasgino.quickinvoicemobile.features.login

import android.content.Context
import com.sarsonasgino.quickinvoicemobile.core.model.LoginRequest
import com.sarsonasgino.quickinvoicemobile.core.network.RetrofitClient
import com.sarsonasgino.quickinvoicemobile.core.utils.SessionManager
import kotlinx.coroutines.*

class LoginPresenter(
    private var view: LoginContract.View?,
    private val context: Context // Needed for SessionManager
) : LoginContract.Presenter {

    private val presenterScope = CoroutineScope(Dispatchers.Main + Job())

    override fun checkSession() {
        if (SessionManager.isLoggedIn(context)) {
            view?.navigateToDashboard()
        }
    }

    override fun login(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            view?.onError("Please fill in all fields")
            return
        }

        view?.showLoading()

        presenterScope.launch {
            try {
                val response = RetrofitClient.api.login(LoginRequest(email, password))

                if (response.isSuccessful) {
                    val body = response.body()
                    val token = body?.token
                    val userId = body?.userId

                    if (token != null && userId != null) {
                        SessionManager.saveToken(context, token)
                        SessionManager.saveClerkId(context, userId)
                        RetrofitClient.setToken(token)
                        view?.onLoginSuccess()
                    } else {
                        view?.onError("Login failed. Data missing.")
                    }
                } else {
                    view?.onError("Invalid email or password.")
                }
            } catch (e: Exception) {
                view?.onError("Network error: ${e.message}")
            } finally {
                view?.hideLoading()
            }
        }
    }

    override fun togglePasswordVisibility(currentlyVisible: Boolean) {
        view?.setPasswordVisibility(!currentlyVisible)
    }

    override fun onDestroy() {
        presenterScope.cancel() // Clean up coroutines
        view = null // Prevent memory leaks
    }
}
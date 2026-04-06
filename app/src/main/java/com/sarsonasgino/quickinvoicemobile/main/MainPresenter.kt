package com.sarsonasgino.quickinvoicemobile

import android.content.Context
import android.view.View
import com.sarsonasgino.quickinvoicemobile.core.utils.SessionManager
import com.sarsonasgino.quickinvoicemobile.main.MainContract

class MainPresenter(
    private var view: MainContract.View?,
    private val context: Context
) : MainContract.Presenter {

    override fun checkSession() {
        view?.updateNav(SessionManager.isLoggedIn(context))
    }

    override fun onActionClicked() {
        if (SessionManager.isLoggedIn(context)) {
            view?.navigateToCreateInvoice()
        } else {
            view?.navigateToLogin()
        }
    }

    override fun onLogoutConfirmed() {
        SessionManager.clearSession(context)
        view?.updateNav(false)
        view?.showToast("Logged out")
    }

    override fun toggleMenu(currentVisibility: Int) {
        view?.toggleDrawer(currentVisibility == View.GONE)
    }

    override fun onDestroy() {
        view = null
    }
}
package com.sarsonasgino.quickinvoicemobile.features.dashboard

import android.content.Context
import com.google.gson.Gson
import com.sarsonasgino.quickinvoicemobile.core.network.RetrofitClient
import com.sarsonasgino.quickinvoicemobile.core.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardPresenter(
    private var view: DashboardContract.View?,
    private val context: Context
) : DashboardContract.Presenter {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun loadInvoices() {
        val token = SessionManager.getToken(context)
        if (token != null) RetrofitClient.setToken(token)

        view?.showLoading()

        scope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.api.getInvoices()
                }

                if (response.isSuccessful) {
                    val invoices = response.body() ?: emptyList()
                    if (invoices.isEmpty()) {
                        view?.showEmptyState()
                    } else {
                        view?.showInvoices(invoices)
                    }
                } else {
                    view?.showError("Failed to load invoices")
                }
            } catch (e: Exception) {
                view?.showError("Network error: ${e.message}")
            } finally {
                view?.hideLoading()
            }
        }
    }

    override fun onCreateInvoiceClicked() {
        view?.navigateToCreateInvoice()
    }

    override fun onInvoiceClicked(invoiceJson: String) {
        view?.navigateToInvoiceDetail(invoiceJson)
    }

    override fun onLogoutClicked() {
        SessionManager.clearSession(context)
        view?.logout()
    }

    override fun onHomeClicked() {
        view?.navigateToHome()
    }

    override fun onDestroy() {
        job.cancel()
        view = null  // Prevent memory leaks
    }
}
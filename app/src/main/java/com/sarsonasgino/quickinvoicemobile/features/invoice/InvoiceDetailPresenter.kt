package com.sarsonasgino.quickinvoicemobile.features.invoice

import android.content.Context
import com.sarsonasgino.quickinvoicemobile.core.network.RetrofitClient
import com.sarsonasgino.quickinvoicemobile.core.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InvoiceDetailPresenter(
    private var view: InvoiceDetailContract.View?,
    private val context: Context
) : InvoiceDetailContract.Presenter {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun deleteInvoice(invoiceId: String) {
        scope.launch {
            try {
                val token = SessionManager.getToken(context)
                if (token != null) RetrofitClient.setToken(token)

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.api.deleteInvoice(invoiceId)
                }

                if (response.isSuccessful) {
                    view?.showDeleteSuccess()
                    view?.closeScreen()
                } else {
                    view?.showDeleteError("Failed to delete invoice")
                }
            } catch (e: Exception) {
                view?.showNetworkError("Network error: ${e.message}")
            }
        }
    }

    override fun onPreviewClicked(invoiceJson: String) {
        view?.navigateToPreview(invoiceJson)
    }

    override fun onDestroy() {
        job.cancel()
        view = null
    }
}
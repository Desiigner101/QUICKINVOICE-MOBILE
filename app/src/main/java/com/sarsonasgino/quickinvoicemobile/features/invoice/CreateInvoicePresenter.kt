package com.sarsonasgino.quickinvoicemobile.features.invoice

import android.content.Context
import com.sarsonasgino.quickinvoicemobile.core.model.Invoice
import com.sarsonasgino.quickinvoicemobile.core.network.RetrofitClient
import com.sarsonasgino.quickinvoicemobile.core.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateInvoicePresenter(
    private var view: CreateInvoiceContract.View?,
    private val context: Context
) : CreateInvoiceContract.Presenter {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun saveInvoice(invoice: Invoice) {
        view?.setSaveButtonsEnabled(false)

        scope.launch {
            try {
                val token = SessionManager.getToken(context)
                if (token != null) RetrofitClient.setToken(token)

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.api.saveInvoice(invoice)
                }

                if (response.isSuccessful) {
                    view?.showSaveSuccess()
                    view?.closeScreen()
                } else {
                    view?.showSaveError("Failed to save invoice")
                }
            } catch (e: Exception) {
                view?.showSaveError("Network error: ${e.message}")
            } finally {
                view?.setSaveButtonsEnabled(true)
            }
        }
    }

    override fun onDestroy() {
        job.cancel()
        view = null
    }
}
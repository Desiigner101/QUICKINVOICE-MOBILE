package com.sarsonasgino.quickinvoicemobile.features.invoice

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.sarsonasgino.quickinvoicemobile.core.model.Invoice
import com.sarsonasgino.quickinvoicemobile.core.network.RetrofitClient
import com.sarsonasgino.quickinvoicemobile.core.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InvoicePreviewPresenter(
    private var view: InvoicePreviewContract.View?,
    private val context: Context
) : InvoicePreviewContract.Presenter {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun saveInvoice(invoiceJson: String, selectedTemplate: String) {
        val invoice = Gson().fromJson(invoiceJson, Invoice::class.java)
        val updatedInvoice = invoice.copy(template = selectedTemplate)

        view?.showLoading()
        view?.setSaveButtonEnabled(false)

        scope.launch {
            try {
                val token = SessionManager.getToken(context)
                if (token != null) RetrofitClient.setToken(token)

                val invoiceId = updatedInvoice.id
                Log.d("PREVIEW", "Invoice ID: $invoiceId | Template: $selectedTemplate")

                val response = withContext(Dispatchers.IO) {
                    if (!invoiceId.isNullOrEmpty()) {
                        Log.d("PREVIEW", "Updating existing invoice...")
                        RetrofitClient.api.updateInvoice(invoiceId, updatedInvoice)
                    } else {
                        Log.d("PREVIEW", "Creating new invoice...")
                        RetrofitClient.api.saveInvoice(updatedInvoice)
                    }
                }

                if (response.isSuccessful) {
                    view?.showSaveSuccess()
                    view?.closeScreen()
                } else {
                    Log.e("PREVIEW", "Save error: ${response.errorBody()?.string()}")
                    view?.showSaveError("Failed to save")
                }
            } catch (e: Exception) {
                Log.e("PREVIEW", "Exception: ${e.message}")
                view?.showSaveError("Network error: ${e.message}")
            } finally {
                view?.hideLoading()
                view?.setSaveButtonEnabled(true)
            }
        }
    }

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
                    view?.showDeleteError("Failed to delete")
                }
            } catch (e: Exception) {
                view?.showDeleteError("Network error: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        job.cancel()
        view = null
    }
}
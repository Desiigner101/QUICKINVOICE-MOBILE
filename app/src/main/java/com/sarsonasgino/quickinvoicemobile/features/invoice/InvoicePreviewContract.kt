package com.sarsonasgino.quickinvoicemobile.features.invoice

interface InvoicePreviewContract {

    interface View {
        fun showLoading()
        fun hideLoading()
        fun showSaveSuccess()
        fun showSaveError(message: String)
        fun showDeleteSuccess()
        fun showDeleteError(message: String)
        fun setSaveButtonEnabled(enabled: Boolean)
        fun closeScreen()
    }

    interface Presenter {
        fun saveInvoice(invoiceJson: String, selectedTemplate: String)
        fun deleteInvoice(invoiceId: String)
        fun onDestroy()
    }
}
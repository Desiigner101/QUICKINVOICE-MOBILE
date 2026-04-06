package com.sarsonasgino.quickinvoicemobile.features.invoice

import com.sarsonasgino.quickinvoicemobile.core.model.Invoice

interface InvoiceDetailContract {

    interface View {
        fun showDeleteSuccess()
        fun showDeleteError(message: String)
        fun showNetworkError(message: String)
        fun navigateToPreview(invoiceJson: String)
        fun closeScreen()
    }

    interface Presenter {
        fun deleteInvoice(invoiceId: String)
        fun onPreviewClicked(invoiceJson: String)
        fun onDestroy()
    }
}
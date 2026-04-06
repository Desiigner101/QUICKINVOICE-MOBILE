package com.sarsonasgino.quickinvoicemobile.features.invoice

import com.sarsonasgino.quickinvoicemobile.core.model.Invoice

interface CreateInvoiceContract {

    interface View {
        fun showSaveSuccess()
        fun showSaveError(message: String)
        fun setSaveButtonsEnabled(enabled: Boolean)
        fun closeScreen()
    }

    interface Presenter {
        fun saveInvoice(invoice: Invoice)
        fun onDestroy()
    }
}
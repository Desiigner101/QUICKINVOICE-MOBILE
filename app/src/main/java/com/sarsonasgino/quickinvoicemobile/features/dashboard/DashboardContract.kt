package com.sarsonasgino.quickinvoicemobile.features.dashboard

import com.sarsonasgino.quickinvoicemobile.core.model.Invoice

interface DashboardContract {

    interface View {
        fun showLoading()
        fun hideLoading()
        fun showInvoices(invoices: List<Invoice>)
        fun showEmptyState()
        fun showError(message: String)
        fun navigateToCreateInvoice()
        fun navigateToInvoiceDetail(invoiceJson: String)
        fun navigateToHome()
        fun logout()
    }

    interface Presenter {
        fun loadInvoices()
        fun onCreateInvoiceClicked()
        fun onInvoiceClicked(invoiceJson: String)
        fun onLogoutClicked()
        fun onHomeClicked()
        fun onDestroy()
    }
}
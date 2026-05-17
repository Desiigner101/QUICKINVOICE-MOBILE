package com.sarsonasgino.quickinvoicemobile.features.subscription

interface SubscriptionContract {

    interface View {
        fun showLoading()
        fun hideLoading()
        fun showCurrentPlan(isPremium: Boolean)
        fun showSuccess(message: String)
        fun showError(message: String)
        fun showCancelConfirmation()
    }

    interface Presenter {
        fun loadSubscriptionStatus()
        fun onUpgradeClicked(paymentMethodId: String)
        fun onCancelClicked()
        fun onConfirmCancel()
        fun onDestroy()
    }
}

package com.sarsonasgino.quickinvoicemobile.features.subscription

import android.content.Context
import com.sarsonasgino.quickinvoicemobile.core.model.UpgradeRequest
import com.sarsonasgino.quickinvoicemobile.core.network.RetrofitClient
import com.sarsonasgino.quickinvoicemobile.core.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SubscriptionPresenter(
    private var view: SubscriptionContract.View?,
    private val context: Context
) : SubscriptionContract.Presenter {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun loadSubscriptionStatus() {
        val token = SessionManager.getToken(context) ?: return
        RetrofitClient.setToken(token)

        view?.showLoading()
        scope.launch {
            try {
                val response = withContext(Dispatchers.IO) { RetrofitClient.api.getSubscriptionStatus() }
                if (response.isSuccessful) {
                    val isPremium = response.body()?.subscriptionType == "PREMIUM"
                    SessionManager.saveIsPremium(context, isPremium)
                    view?.showCurrentPlan(isPremium)
                } else {
                    view?.showCurrentPlan(SessionManager.getIsPremium(context))
                }
            } catch (e: Exception) {
                view?.showCurrentPlan(SessionManager.getIsPremium(context))
            } finally {
                view?.hideLoading()
            }
        }
    }

    override fun onUpgradeClicked(paymentMethodId: String) {
        val token = SessionManager.getToken(context) ?: return
        RetrofitClient.setToken(token)

        view?.showLoading()
        scope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.api.upgradeToPremium(UpgradeRequest(paymentMethodId))
                }
                if (response.isSuccessful && response.body()?.status == "SUCCESS") {
                    SessionManager.saveIsPremium(context, true)
                    view?.showSuccess("Successfully upgraded to Premium! ✨")
                    view?.showCurrentPlan(true)
                } else {
                    val msg = response.body()?.message ?: "Payment failed. Please try again."
                    view?.showError(msg)
                }
            } catch (e: Exception) {
                view?.showError("Network error: ${e.message}")
            } finally {
                view?.hideLoading()
            }
        }
    }

    override fun onCancelClicked() {
        view?.showCancelConfirmation()
    }

    override fun onConfirmCancel() {
        val token = SessionManager.getToken(context) ?: return
        RetrofitClient.setToken(token)

        view?.showLoading()
        scope.launch {
            try {
                val response = withContext(Dispatchers.IO) { RetrofitClient.api.cancelSubscription() }
                if (response.isSuccessful) {
                    SessionManager.saveIsPremium(context, false)
                    view?.showSuccess("Subscription cancelled.")
                    view?.showCurrentPlan(false)
                } else {
                    view?.showError("Failed to cancel subscription.")
                }
            } catch (e: Exception) {
                view?.showError("Network error: ${e.message}")
            } finally {
                view?.hideLoading()
            }
        }
    }

    override fun onDestroy() {
        job.cancel()
        view = null
    }
}

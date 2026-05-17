package com.sarsonasgino.quickinvoicemobile.features.subscription

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.sarsonasgino.quickinvoicemobile.BuildConfig
import com.sarsonasgino.quickinvoicemobile.R
import com.sarsonasgino.quickinvoicemobile.databinding.ActivitySubscriptionBinding
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.CardInputWidget

class SubscriptionActivity : AppCompatActivity(), SubscriptionContract.View {

    private lateinit var binding: ActivitySubscriptionBinding
    private lateinit var presenter: SubscriptionPresenter
    private lateinit var stripe: Stripe

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        PaymentConfiguration.init(this, BuildConfig.STRIPE_PUBLISHABLE_KEY)
        stripe = Stripe(this, BuildConfig.STRIPE_PUBLISHABLE_KEY)

        presenter = SubscriptionPresenter(this, this)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnUpgrade.setOnClickListener { showCardInputDialog() }
        binding.btnCancel.setOnClickListener { presenter.onCancelClicked() }

        presenter.loadSubscriptionStatus()
    }

    override fun onResume() {
        super.onResume()
        presenter.loadSubscriptionStatus()
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }

    private fun showCardInputDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_card_input, null)
        val cardInputWidget = dialogView.findViewById<CardInputWidget>(R.id.cardInputWidget)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Upgrade to Premium")
            .setView(dialogView)
            .setPositiveButton("Pay ₱199/mo", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val params = cardInputWidget.paymentMethodCreateParams
                if (params == null) {
                    Toast.makeText(this, "Please enter valid card details.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                dialog.dismiss()
                processPayment(params)
            }
        }
        dialog.show()
    }

    private fun processPayment(params: com.stripe.android.model.PaymentMethodCreateParams) {
        showLoading()
        stripe.createPaymentMethod(
            paymentMethodCreateParams = params,
            callback = object : ApiResultCallback<PaymentMethod> {
                override fun onSuccess(result: PaymentMethod) {
                    val paymentMethodId = result.id
                    if (paymentMethodId != null) {
                        presenter.onUpgradeClicked(paymentMethodId)
                    } else {
                        hideLoading()
                        showError("Failed to process card. Try again.")
                    }
                }

                override fun onError(e: Exception) {
                    hideLoading()
                    showError("Card error: ${e.localizedMessage}")
                }
            }
        )
    }

    // ─── SubscriptionContract.View ────────────────────────────────────────────

    override fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnUpgrade.isEnabled = false
        binding.btnCancel.isEnabled = false
    }

    override fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.btnUpgrade.isEnabled = true
        binding.btnCancel.isEnabled = true
    }

    override fun showCurrentPlan(isPremium: Boolean) {
        val badgeDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24f
        }
        if (isPremium) {
            badgeDrawable.setColor(android.graphics.Color.parseColor("#F59E0B"))
            binding.tvCurrentBadge.background = badgeDrawable
            binding.tvCurrentBadge.text = "✨ PREMIUM"
            binding.tvCurrentBadge.setTextColor(android.graphics.Color.parseColor("#000000"))
            binding.tvCurrentDesc.text = "You're on the Premium plan. Enjoy all 10 templates!"
            binding.btnUpgrade.visibility = View.GONE
            binding.btnCancel.visibility = View.VISIBLE
        } else {
            badgeDrawable.setColor(android.graphics.Color.parseColor("#E5E7EB"))
            binding.tvCurrentBadge.background = badgeDrawable
            binding.tvCurrentBadge.text = "FREE"
            binding.tvCurrentBadge.setTextColor(android.graphics.Color.parseColor("#374151"))
            binding.tvCurrentDesc.text = "Upgrade to Premium to unlock all 10 templates and more."
            binding.btnUpgrade.visibility = View.VISIBLE
            binding.btnCancel.visibility = View.GONE
        }
    }

    override fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun showCancelConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Cancel Subscription")
            .setMessage("Are you sure you want to cancel your Premium subscription? You will lose access to Premium templates.")
            .setPositiveButton("Cancel Subscription") { _, _ -> presenter.onConfirmCancel() }
            .setNegativeButton("Keep Premium", null)
            .show()
    }
}

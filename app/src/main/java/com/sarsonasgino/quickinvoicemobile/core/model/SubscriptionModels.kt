package com.sarsonasgino.quickinvoicemobile.core.model

data class SubscriptionStatus(
    val status: String? = null,
    val message: String? = null,
    val subscriptionType: String? = null
)

data class UpgradeRequest(
    val paymentMethodId: String
)

data class UpgradeResponse(
    val status: String? = null,
    val message: String? = null,
    val subscriptionType: String? = null
)

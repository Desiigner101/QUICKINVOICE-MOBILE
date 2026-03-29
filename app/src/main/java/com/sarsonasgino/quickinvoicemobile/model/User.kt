package com.sarsonasgino.quickinvoicemobile.model

data class User(
    val id: String? = null,
    val clerkId: String? = null,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val photoUrl: String? = null,
    val subscriptionType: String? = "FREE"
)
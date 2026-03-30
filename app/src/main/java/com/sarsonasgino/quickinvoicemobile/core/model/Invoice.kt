package com.sarsonasgino.quickinvoicemobile.core.model

data class Invoice(
    val id: String? = null,
    val clerkId: String? = null,
    val title: String? = null,
    val company: Company? = null,
    val billing: Billing? = null,
    val shipping: Shipping? = null,
    val invoice: InvoiceDetails? = null,
    val items: List<Item>? = null,
    val notes: String? = null,
    val logo: String? = null,
    val tax: Double = 0.0,
    val template: String? = null,
    val thumbnailUrl: String? = null,
    val createdAt: String? = null,
    val lastUpdatedAt: String? = null
)

data class Company(val name: String? = null, val phone: String? = null, val address: String? = null)
data class Billing(val name: String? = null, val phone: String? = null, val address: String? = null)
data class Shipping(val name: String? = null, val phone: String? = null, val address: String? = null)
data class InvoiceDetails(val number: String? = null, val date: String? = null, val dueDate: String? = null)
data class Item(val name: String? = null, val qty: Int = 0, val amount: Double = 0.0, val description: String? = null)
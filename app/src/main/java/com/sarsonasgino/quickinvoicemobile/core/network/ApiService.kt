package com.sarsonasgino.quickinvoicemobile.core.network

import com.sarsonasgino.quickinvoicemobile.core.model.Invoice
import com.sarsonasgino.quickinvoicemobile.core.model.User
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // User
    @POST("api/users")
    suspend fun createOrUpdateUser(@Body user: User): Response<User>

    // Invoices
    @GET("api/invoices")
    suspend fun getInvoices(): Response<List<Invoice>>

    @POST("api/invoices")
    suspend fun saveInvoice(@Body invoice: Invoice): Response<Invoice>

    @DELETE("api/invoices/{id}")
    suspend fun deleteInvoice(@Path("id") id: String): Response<Void>
}
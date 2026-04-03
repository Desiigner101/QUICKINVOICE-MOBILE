package com.sarsonasgino.quickinvoicemobile.core.network

import com.sarsonasgino.quickinvoicemobile.core.model.Invoice
import com.sarsonasgino.quickinvoicemobile.core.model.User
import com.sarsonasgino.quickinvoicemobile.core.model.LoginRequest
import com.sarsonasgino.quickinvoicemobile.core.model.MobileAuthResponse
import com.sarsonasgino.quickinvoicemobile.core.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Mobile Auth
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<MobileAuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<MobileAuthResponse>

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

    @PUT("api/invoices/{id}")
    suspend fun updateInvoice(@Path("id") id: String, @Body invoice: Invoice): Response<Invoice>
}
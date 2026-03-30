package com.sarsonasgino.quickinvoicemobile.core.network

import com.sarsonasgino.quickinvoicemobile.core.model.AuthResponse
import com.sarsonasgino.quickinvoicemobile.core.model.SignInRequest
import com.sarsonasgino.quickinvoicemobile.core.model.SignUpRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ClerkApiService {

    @POST("v1/client/sign_ins")
    suspend fun signIn(@Body request: SignInRequest): Response<AuthResponse>

    @POST("v1/client/sign_ups")
    suspend fun signUp(@Body request: SignUpRequest): Response<AuthResponse>
}
package com.sarsonasgino.quickinvoicemobile.network

import com.sarsonasgino.quickinvoicemobile.model.AuthResponse
import com.sarsonasgino.quickinvoicemobile.model.SignInRequest
import com.sarsonasgino.quickinvoicemobile.model.SignUpRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ClerkApiService {

    @POST("v1/client/sign_ins")
    suspend fun signIn(@Body request: SignInRequest): Response<AuthResponse>

    @POST("v1/client/sign_ups")
    suspend fun signUp(@Body request: SignUpRequest): Response<AuthResponse>
}
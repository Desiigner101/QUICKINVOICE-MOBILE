package com.sarsonasgino.quickinvoicemobile.core.model

data class SignInRequest(
    val identifier: String,
    val password: String
)

data class SignUpRequest(
    val email_address: String,
    val password: String,
    val first_name: String,
    val last_name: String
)

data class AuthResponse(
    val client: ClientData?
)

data class ClientData(
    val sessions: List<SessionData>?
)

data class SessionData(
    val last_active_token: TokenData?
)

data class TokenData(
    val jwt: String?
)
package com.sarsonasgino.quickinvoicemobile.core.model

// Clerk models (keep for future use)
data class SignInRequest(
    val identifier: String,
    val password: String,
    val strategy: String = "password"
)

data class SignUpRequest(
    val email_address: String,
    val password: String,
    val first_name: String,
    val last_name: String
)

data class UpdateProfileRequest(
    val firstName: String,
    val lastName: String,
    val password: String? = null,
    val photoUrl: String? = null
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

// Mobile Auth models
data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String
)

data class MobileAuthResponse(
    val token: String?,
    val userId: String?,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
    val message: String?
)
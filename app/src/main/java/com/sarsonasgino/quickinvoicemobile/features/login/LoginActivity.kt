package com.sarsonasgino.quickinvoicemobile.features.login

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sarsonasgino.quickinvoicemobile.R
import com.sarsonasgino.quickinvoicemobile.core.model.LoginRequest
import com.sarsonasgino.quickinvoicemobile.core.network.RetrofitClient
import com.sarsonasgino.quickinvoicemobile.core.utils.SessionManager
import com.sarsonasgino.quickinvoicemobile.databinding.ActivityLoginBinding
import com.sarsonasgino.quickinvoicemobile.features.dashboard.DashboardActivity
import com.sarsonasgino.quickinvoicemobile.features.register.RegisterActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (SessionManager.isLoggedIn(this)) {
            goToDashboard()
            return
        }

        // Toggle password visibility
        binding.btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.etPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.btnTogglePassword.setImageResource(R.drawable.ic_eye_on)
            } else {
                binding.etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.btnTogglePassword.setImageResource(R.drawable.ic_eye_off)
            }
            binding.etPassword.setSelection(binding.etPassword.text.length)
        }

        binding.btnContinue.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            login(email, password)
        }

        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun login(email: String, password: String) {
        binding.btnContinue.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.login(
                    LoginRequest(email = email, password = password)
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("LOGIN", "Response: $body")

                    val token = body?.token
                    val userId = body?.userId

                    if (token != null && userId != null) {
                        SessionManager.saveToken(this@LoginActivity, token)
                        SessionManager.saveClerkId(this@LoginActivity, userId)
                        RetrofitClient.setToken(token)
                        goToDashboard()
                    } else {
                        showError("Login failed. Please try again.")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("LOGIN", "Error: $errorBody")
                    showError("Invalid email or password.")
                }
            } catch (e: Exception) {
                Log.e("LOGIN", "Exception: ${e.message}")
                showError("Network error: ${e.message}")
            } finally {
                binding.btnContinue.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun goToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
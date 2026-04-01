package com.sarsonasgino.quickinvoicemobile.features.register

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sarsonasgino.quickinvoicemobile.core.model.RegisterRequest
import com.sarsonasgino.quickinvoicemobile.core.network.RetrofitClient
import com.sarsonasgino.quickinvoicemobile.core.utils.SessionManager
import com.sarsonasgino.quickinvoicemobile.databinding.ActivityRegisterBinding
import com.sarsonasgino.quickinvoicemobile.features.dashboard.DashboardActivity
import com.sarsonasgino.quickinvoicemobile.features.login.LoginActivity
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnContinue.setOnClickListener {
            val firstName = binding.etFirstName.text.toString().trim()
            val lastName = binding.etLastName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty() ||
                email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 8) {
                Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            register(firstName, lastName, email, password)
        }

        binding.tvSignIn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun register(firstName: String, lastName: String, email: String, password: String) {
        binding.btnContinue.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.register(
                    RegisterRequest(
                        email = email,
                        password = password,
                        firstName = firstName,
                        lastName = lastName
                    )
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("REGISTER", "Response: $body")

                    val token = body?.token
                    val userId = body?.userId

                    if (token != null && userId != null) {
                        SessionManager.saveToken(this@RegisterActivity, token)
                        SessionManager.saveClerkId(this@RegisterActivity, userId)
                        RetrofitClient.setToken(token)
                        goToDashboard()
                    } else {
                        showError("Registration failed. Please try again.")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("REGISTER", "Error: $errorBody")
                    showError("Registration failed. Email may already be in use.")
                }
            } catch (e: Exception) {
                Log.e("REGISTER", "Exception: ${e.message}")
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
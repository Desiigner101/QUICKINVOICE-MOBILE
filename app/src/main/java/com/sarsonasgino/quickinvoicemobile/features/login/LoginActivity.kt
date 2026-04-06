package com.sarsonasgino.quickinvoicemobile.features.login

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sarsonasgino.quickinvoicemobile.R
import com.sarsonasgino.quickinvoicemobile.databinding.ActivityLoginBinding
import com.sarsonasgino.quickinvoicemobile.features.dashboard.DashboardActivity
import com.sarsonasgino.quickinvoicemobile.features.register.RegisterActivity


class LoginActivity : AppCompatActivity(), LoginContract.View {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var presenter: LoginContract.Presenter
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        presenter = LoginPresenter(this, this)
        presenter.checkSession()

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnTogglePassword.setOnClickListener {
            presenter.togglePasswordVisibility(isPasswordVisible)
        }

        binding.btnContinue.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            presenter.login(email, password)
        }

        binding.tvSignUp.setOnClickListener {
            navigateToRegister()
        }
    }

    // --- View Interface Implementations ---

    override fun setPasswordVisibility(visible: Boolean) {
        isPasswordVisible = visible
        if (visible) {
            binding.etPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.btnTogglePassword.setImageResource(R.drawable.ic_eye_on)
        } else {
            binding.etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.btnTogglePassword.setImageResource(R.drawable.ic_eye_off)
        }
        binding.etPassword.setSelection(binding.etPassword.text.length)
    }

    override fun showLoading() {
        binding.btnContinue.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
    }

    override fun hideLoading() {
        binding.btnContinue.isEnabled = true
        binding.progressBar.visibility = View.GONE
    }

    override fun onLoginSuccess() {
        navigateToDashboard()
    }

    override fun onError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun navigateToRegister() {
        startActivity(Intent(this, RegisterActivity::class.java))
    }

    override fun navigateToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        presenter.onDestroy() // Always kill the presenter to stop network calls
        super.onDestroy()
    }
}
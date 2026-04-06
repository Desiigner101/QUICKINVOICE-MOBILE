package com.sarsonasgino.quickinvoicemobile.features.register

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sarsonasgino.quickinvoicemobile.R
import com.sarsonasgino.quickinvoicemobile.databinding.ActivityRegisterBinding
import com.sarsonasgino.quickinvoicemobile.features.dashboard.DashboardActivity
import com.sarsonasgino.quickinvoicemobile.features.login.LoginActivity


class RegisterActivity : AppCompatActivity(), RegisterContract.View {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var presenter: RegisterContract.Presenter
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        presenter = RegisterPresenter(this, this)
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnTogglePassword.setOnClickListener {
            presenter.toggleVisibility(isPasswordVisible, isConfirmField = false)
        }

        binding.btnToggleConfirmPassword.setOnClickListener {
            presenter.toggleVisibility(isConfirmPasswordVisible, isConfirmField = true)
        }

        binding.btnContinue.setOnClickListener {
            presenter.register(
                binding.etFirstName.text.toString().trim(),
                binding.etLastName.text.toString().trim(),
                binding.etEmail.text.toString().trim(),
                binding.etPassword.text.toString().trim(),
                binding.etConfirmPassword.text.toString().trim()
            )
        }

        binding.tvSignIn.setOnClickListener { navigateToLogin() }
    }

    // --- View Implementations ---

    override fun setPasswordVisibility(visible: Boolean, isConfirmField: Boolean) {
        val editText = if (isConfirmField) binding.etConfirmPassword else binding.etPassword
        val button = if (isConfirmField) binding.btnToggleConfirmPassword else binding.btnTogglePassword

        if (isConfirmField) isConfirmPasswordVisible = visible else isPasswordVisible = visible

        editText.inputType = if (visible) {
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        button.setImageResource(if (visible) R.drawable.ic_eye_on else R.drawable.ic_eye_off)
        editText.setSelection(editText.text.length)
    }

    override fun showLoading() {
        binding.btnContinue.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
    }

    override fun hideLoading() {
        binding.btnContinue.isEnabled = true
        binding.progressBar.visibility = View.GONE
    }

    override fun onRegisterSuccess() {
        navigateToDashboard()
    }

    override fun onError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun navigateToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    override fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }
}
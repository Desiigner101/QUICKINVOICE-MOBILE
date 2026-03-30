package com.sarsonasgino.quickinvoicemobile.features.login

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sarsonasgino.quickinvoicemobile.features.register.RegisterActivity
import com.sarsonasgino.quickinvoicemobile.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Continue button → login logic (Step 10)
        binding.btnContinue.setOnClickListener {
            // TODO: login logic
        }

        // Sign up link → go to Register
        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
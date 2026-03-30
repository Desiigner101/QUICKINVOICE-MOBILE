package com.sarsonasgino.quickinvoicemobile

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.sarsonasgino.quickinvoicemobile.databinding.ActivityMainBinding
import com.sarsonasgino.quickinvoicemobile.ui.LoginActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isMenuOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hamburger toggle
        binding.btnHamburger.setOnClickListener {
            if (isMenuOpen) {
                binding.drawerMenu.visibility = View.GONE
                isMenuOpen = false
            } else {
                binding.drawerMenu.visibility = View.VISIBLE
                isMenuOpen = true
            }
        }

        // Menu items
        binding.menuHome.setOnClickListener {
            binding.drawerMenu.visibility = View.GONE
            isMenuOpen = false
        }

        binding.menuLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // Hero + CTA buttons
        binding.btnGetStarted.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        binding.btnStartNow.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
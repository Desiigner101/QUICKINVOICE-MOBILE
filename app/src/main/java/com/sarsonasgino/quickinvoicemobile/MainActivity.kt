package com.sarsonasgino.quickinvoicemobile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sarsonasgino.quickinvoicemobile.core.utils.SessionManager
import com.sarsonasgino.quickinvoicemobile.databinding.ActivityMainBinding
import com.sarsonasgino.quickinvoicemobile.features.dashboard.DashboardActivity
import com.sarsonasgino.quickinvoicemobile.features.invoice.CreateInvoiceActivity
import com.sarsonasgino.quickinvoicemobile.features.login.LoginActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = android.graphics.Color.WHITE
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        }

        setupNavbar()

        binding.btnLearnMore.setOnClickListener {
            binding.scrollView.smoothScrollTo(0, 3450)
        }

        binding.tvLogo.setOnClickListener {
            binding.scrollView.smoothScrollTo(0, 0)
        }

        binding.btnGetStarted.setOnClickListener {
            if (SessionManager.isLoggedIn(this)) {
                startActivity(Intent(this, CreateInvoiceActivity::class.java))
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }

        binding.btnStartNow.setOnClickListener {
            if (SessionManager.isLoggedIn(this)) {
                startActivity(Intent(this, CreateInvoiceActivity::class.java))
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupNavbar()
    }

    private fun setupNavbar() {
        val isLoggedIn = SessionManager.isLoggedIn(this)

        // Show/hide profile icon
        if (isLoggedIn) {
            binding.btnProfile.visibility = View.VISIBLE
            binding.navLoggedOut.visibility = View.GONE
            binding.navLoggedIn.visibility = View.VISIBLE
        } else {
            binding.btnProfile.visibility = View.GONE
            binding.navLoggedOut.visibility = View.VISIBLE
            binding.navLoggedIn.visibility = View.GONE
        }

        binding.btnHamburger.setOnClickListener {
            binding.drawerMenu.visibility =
                if (binding.drawerMenu.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        binding.btnProfile.setOnClickListener {
            showProfilePopup(binding.btnProfile)
        }

        binding.tvLogo.setOnClickListener {
            binding.scrollView.smoothScrollTo(0, 0)
            binding.drawerMenu.visibility = View.GONE
        }

        binding.menuHome.setOnClickListener {
            binding.scrollView.smoothScrollTo(0, 0)
            binding.drawerMenu.visibility = View.GONE
        }

        binding.menuHomeLoggedIn.setOnClickListener {
            binding.scrollView.smoothScrollTo(0, 0)
            binding.drawerMenu.visibility = View.GONE
        }

        binding.menuLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            binding.drawerMenu.visibility = View.GONE
        }

        binding.menuDashboard.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            binding.drawerMenu.visibility = View.GONE
        }

        binding.menuGenerate.setOnClickListener {
            startActivity(Intent(this, CreateInvoiceActivity::class.java))
            binding.drawerMenu.visibility = View.GONE
        }
    }

    private fun showProfilePopup(view: View) {
        val popup = android.widget.PopupMenu(this, view)
        popup.menu.add(0, 1, 0, "👤 Profile")
        popup.menu.add(0, 2, 0, "🚪 Logout")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    Toast.makeText(this, "Profile coming soon!", Toast.LENGTH_SHORT).show()
                    true
                }
                2 -> {
                    SessionManager.clearSession(this)
                    setupNavbar()
                    Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
}
package com.sarsonasgino.quickinvoicemobile.main

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sarsonasgino.quickinvoicemobile.MainPresenter
import com.sarsonasgino.quickinvoicemobile.databinding.ActivityMainBinding
import com.sarsonasgino.quickinvoicemobile.features.dashboard.DashboardActivity
import com.sarsonasgino.quickinvoicemobile.features.invoice.CreateInvoiceActivity
import com.sarsonasgino.quickinvoicemobile.features.login.LoginActivity

class MainActivity : AppCompatActivity(), MainContract.View {

    private lateinit var binding: ActivityMainBinding
    private lateinit var presenter: MainContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        presenter = MainPresenter(this, this)
        setupStatusBar()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        presenter.checkSession()
    }

    private fun setupStatusBar() {
        window.statusBarColor = android.graphics.Color.WHITE
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        }
    }

    private fun setupListeners() {
        // Navigation & Scrolling
        binding.btnLearnMore.setOnClickListener { scrollTo(3450) }
        binding.tvLogo.setOnClickListener {
            scrollTo(0)
            toggleDrawer(false)
        }

        // Logical Actions
        binding.btnGetStarted.setOnClickListener { presenter.onActionClicked() }
        binding.btnStartNow.setOnClickListener { presenter.onActionClicked() }
        binding.btnHamburger.setOnClickListener { presenter.toggleMenu(binding.drawerMenu.visibility) }
        binding.btnProfile.setOnClickListener { showProfilePopup(it) }

        // Menu Items
        binding.menuHome.setOnClickListener { scrollTo(0); toggleDrawer(false) }
        binding.menuHomeLoggedIn.setOnClickListener { scrollTo(0); toggleDrawer(false) }
        binding.menuLogin.setOnClickListener { navigateToLogin(); toggleDrawer(false) }
        binding.menuDashboard.setOnClickListener { navigateToDashboard(); toggleDrawer(false) }
        binding.menuGenerate.setOnClickListener { navigateToCreateInvoice(); toggleDrawer(false) }
    }

    // --- View Implementation ---

    override fun updateNav(isLoggedIn: Boolean) {
        binding.btnProfile.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
        binding.navLoggedOut.visibility = if (isLoggedIn) View.GONE else View.VISIBLE
        binding.navLoggedIn.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
    }

    override fun toggleDrawer(isVisible: Boolean) {
        binding.drawerMenu.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    override fun scrollTo(y: Int) {
        binding.scrollView.smoothScrollTo(0, y)
    }

    override fun navigateToLogin() = startActivity(Intent(this, LoginActivity::class.java))

    override fun navigateToDashboard() = startActivity(Intent(this, DashboardActivity::class.java))

    override fun navigateToCreateInvoice() = startActivity(Intent(this, CreateInvoiceActivity::class.java))

    override fun showProfilePopup(anchor: View) {
        val popup = android.widget.PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "👤 Profile")
        popup.menu.add(0, 2, 0, "🚪 Logout")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> { showToast("Profile coming soon!"); true }
                2 -> { presenter.onLogoutConfirmed(); true }
                else -> false
            }
        }
        popup.show()
    }

    override fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }
}
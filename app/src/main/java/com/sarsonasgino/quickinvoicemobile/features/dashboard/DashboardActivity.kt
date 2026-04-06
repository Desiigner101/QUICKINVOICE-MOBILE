package com.sarsonasgino.quickinvoicemobile.features.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import com.sarsonasgino.quickinvoicemobile.core.model.Invoice
import com.sarsonasgino.quickinvoicemobile.databinding.ActivityDashboardBinding
import com.sarsonasgino.quickinvoicemobile.features.invoice.CreateInvoiceActivity
import com.sarsonasgino.quickinvoicemobile.features.invoice.InvoiceDetailActivity
import com.sarsonasgino.quickinvoicemobile.main.MainActivity

class DashboardActivity : AppCompatActivity(), DashboardContract.View {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var presenter: DashboardPresenter
    private lateinit var adapter: InvoiceAdapter
    private var isMenuOpen = false
    private var shouldRefresh = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        presenter = DashboardPresenter(this, this)

        binding.tvWelcome.text = "Welcome back 👋"

        setupRecyclerView()
        setupNavbar()
        presenter.loadInvoices()

        binding.btnCreateInvoice.setOnClickListener {
            presenter.onCreateInvoiceClicked()
        }

        binding.tvLogo.setOnClickListener {
            binding.recyclerView.smoothScrollToPosition(0)
        }
    }

    override fun onResume() {
        super.onResume()
        if (shouldRefresh) {
            presenter.loadInvoices()
            shouldRefresh = false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        shouldRefresh = true
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }

    // --- DashboardContract.View implementations ---

    override fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.layoutEmpty.visibility = View.GONE
    }

    override fun hideLoading() {
        binding.progressBar.visibility = View.GONE
    }

    override fun showInvoices(invoices: List<Invoice>) {
        binding.layoutEmpty.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
        adapter.updateInvoices(invoices)
    }

    override fun showEmptyState() {
        binding.layoutEmpty.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun navigateToCreateInvoice() {
        startActivityForResult(Intent(this, CreateInvoiceActivity::class.java), 100)
    }

    override fun navigateToInvoiceDetail(invoiceJson: String) {
        val intent = Intent(this, InvoiceDetailActivity::class.java)
        intent.putExtra("invoice_json", invoiceJson)
        startActivityForResult(intent, 101)
    }

    override fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    override fun logout() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    // --- Private helpers (UI only, no logic) ---

    private fun setupRecyclerView() {
        adapter = InvoiceAdapter(mutableListOf()) { invoice ->
            presenter.onInvoiceClicked(Gson().toJson(invoice))
        }
        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = adapter
    }

    private fun setupNavbar() {
        binding.btnHamburger.setOnClickListener {
            if (isMenuOpen) {
                binding.drawerMenu.visibility = View.GONE
                isMenuOpen = false
            } else {
                binding.drawerMenu.visibility = View.VISIBLE
                isMenuOpen = true
            }
        }

        binding.btnRefresh.setOnClickListener {
            presenter.loadInvoices()
            Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show()
        }

        binding.btnProfile.setOnClickListener { showProfileMenu(it) }

        binding.menuHome.setOnClickListener {
            binding.drawerMenu.visibility = View.GONE
            isMenuOpen = false
            presenter.onHomeClicked()
        }

        binding.menuDashboard.setOnClickListener {
            binding.drawerMenu.visibility = View.GONE
            isMenuOpen = false
        }

        binding.menuGenerate.setOnClickListener {
            binding.drawerMenu.visibility = View.GONE
            isMenuOpen = false
            presenter.onCreateInvoiceClicked()
        }
    }

    private fun showProfileMenu(view: View) {
        val popup = android.widget.PopupMenu(this, view)
        popup.menu.add(0, 1, 0, "👤 Profile")
        popup.menu.add(0, 2, 0, "🚪 Logout")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> { Toast.makeText(this, "Profile coming soon!", Toast.LENGTH_SHORT).show(); true }
                2 -> { presenter.onLogoutClicked(); true }
                else -> false
            }
        }
        popup.show()
    }
}
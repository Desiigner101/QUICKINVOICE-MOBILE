package com.sarsonasgino.quickinvoicemobile.features.dashboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import com.sarsonasgino.quickinvoicemobile.core.network.RetrofitClient
import com.sarsonasgino.quickinvoicemobile.core.utils.SessionManager
import com.sarsonasgino.quickinvoicemobile.databinding.ActivityDashboardBinding
import com.sarsonasgino.quickinvoicemobile.features.invoice.CreateInvoiceActivity
import com.sarsonasgino.quickinvoicemobile.features.invoice.InvoiceDetailActivity
import com.sarsonasgino.quickinvoicemobile.MainActivity
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var adapter: InvoiceAdapter
    private var isMenuOpen = false
    private var shouldRefresh = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvWelcome.text = "Welcome back 👋"

        setupRecyclerView()
        loadInvoices()
        setupNavbar()

        binding.btnCreateInvoice.setOnClickListener {
            startActivityForResult(Intent(this, CreateInvoiceActivity::class.java), 100)
        }

        binding.tvLogo.setOnClickListener {
            binding.recyclerView.smoothScrollToPosition(0)
        }

    }

    override fun onResume() {
        super.onResume()
        if (shouldRefresh) {
            loadInvoices()
            shouldRefresh = false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        shouldRefresh = true
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
            loadInvoices()
            Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show()
        }

        binding.btnProfile.setOnClickListener {
            showProfileMenu(it)
        }

        binding.menuHome.setOnClickListener {
            binding.drawerMenu.visibility = View.GONE
            isMenuOpen = false
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        binding.menuDashboard.setOnClickListener {
            binding.drawerMenu.visibility = View.GONE
            isMenuOpen = false
        }

        binding.menuGenerate.setOnClickListener {
            binding.drawerMenu.visibility = View.GONE
            isMenuOpen = false
            startActivityForResult(Intent(this, CreateInvoiceActivity::class.java), 100)
        }
    }

    private fun showProfileMenu(view: View) {
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
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun setupRecyclerView() {
        adapter = InvoiceAdapter(mutableListOf()) { invoice ->
            val intent = Intent(this, InvoiceDetailActivity::class.java)
            intent.putExtra("invoice_json", Gson().toJson(invoice))
            startActivityForResult(intent, 101)
        }
        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = adapter
    }

    private fun loadInvoices() {
        binding.progressBar.visibility = View.VISIBLE
        binding.layoutEmpty.visibility = View.GONE

        val token = SessionManager.getToken(this)
        if (token != null) RetrofitClient.setToken(token)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getInvoices()

                if (response.isSuccessful) {
                    val invoices = response.body() ?: emptyList()
                    Log.d("DASHBOARD", "Invoices loaded: ${invoices.size}")

                    if (invoices.isEmpty()) {
                        binding.layoutEmpty.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    } else {
                        binding.layoutEmpty.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                        adapter.updateInvoices(invoices)
                    }
                } else {
                    val error = response.errorBody()?.string()
                    Log.e("DASHBOARD", "Error: $error")
                    Toast.makeText(
                        this@DashboardActivity,
                        "Failed to load invoices", Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("DASHBOARD", "Exception: ${e.message}")
                Toast.makeText(
                    this@DashboardActivity,
                    "Network error: ${e.message}", Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}
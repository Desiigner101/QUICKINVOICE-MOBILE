package com.sarsonasgino.quickinvoicemobile.features.invoice

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.sarsonasgino.quickinvoicemobile.R
import com.sarsonasgino.quickinvoicemobile.core.model.Invoice
import com.sarsonasgino.quickinvoicemobile.core.network.RetrofitClient
import com.sarsonasgino.quickinvoicemobile.core.utils.SessionManager
import com.sarsonasgino.quickinvoicemobile.databinding.ActivityInvoiceDetailBinding
import com.google.gson.Gson
import kotlinx.coroutines.launch

class InvoiceDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInvoiceDetailBinding
    private lateinit var invoice: Invoice

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInvoiceDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get invoice from intent
        val invoiceJson = intent.getStringExtra("invoice_json")
        if (invoiceJson == null) {
            finish()
            return
        }

        invoice = Gson().fromJson(invoiceJson, Invoice::class.java)
        populateData()
        setupListeners()

        binding.btnPreview.setOnClickListener {
            val intent = Intent(this, InvoicePreviewActivity::class.java)
            intent.putExtra("invoice_json", Gson().toJson(invoice))
            startActivity(intent)
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Invoice")
                .setMessage("Are you sure you want to delete \"${invoice.title}\"?")
                .setPositiveButton("Delete") { _, _ -> deleteInvoice() }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }


    private fun populateData() {
        // Title
        binding.tvInvoiceTitle.text = invoice.title ?: "Invoice Detail"

        // Thumbnail
        if (!invoice.thumbnailUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(invoice.thumbnailUrl)
                .centerCrop()
                .into(binding.ivThumbnail)
        } else {
            binding.ivThumbnail.setImageResource(R.drawable.ic_launcher_background)
        }

        // Invoice info
        binding.tvInvoiceNumber.text = invoice.invoice?.number ?: "--"
        binding.tvInvoiceDate.text = invoice.invoice?.date ?: "--"
        binding.tvInvoiceDueDate.text = invoice.invoice?.dueDate ?: "--"

        // Company
        binding.tvCompanyName.text = invoice.company?.name ?: "--"
        binding.tvCompanyPhone.text = invoice.company?.phone ?: "--"
        binding.tvCompanyAddress.text = invoice.company?.address ?: "--"

        // Billing
        binding.tvBillingName.text = invoice.billing?.name ?: "--"
        binding.tvBillingPhone.text = invoice.billing?.phone ?: "--"
        binding.tvBillingAddress.text = invoice.billing?.address ?: "--"

        // Items
        var subtotal = 0.0
        invoice.items?.forEach { item ->
            val itemView = LayoutInflater.from(this)
                .inflate(R.layout.item_invoice_detail_row, binding.itemsContainer, false)

            itemView.findViewById<TextView>(R.id.tvItemName).text = item.name ?: "--"
            itemView.findViewById<TextView>(R.id.tvItemQty).text = item.qty.toString()
            itemView.findViewById<TextView>(R.id.tvItemAmount).text = "₱${String.format("%.2f", item.amount)}"
            val total = item.qty * item.amount
            itemView.findViewById<TextView>(R.id.tvItemTotal).text = "₱${String.format("%.2f", total)}"
            subtotal += total

            binding.itemsContainer.addView(itemView)
        }

        // Totals
        val taxRate = invoice.tax
        val taxAmount = subtotal * taxRate / 100
        val grandTotal = subtotal + taxAmount

        binding.tvSubtotal.text = "₱${String.format("%.2f", subtotal)}"
        binding.tvTaxAmount.text = "₱${String.format("%.2f", taxAmount)}"
        binding.tvGrandTotal.text = "₱${String.format("%.2f", grandTotal)}"

        // Notes
        binding.tvNotes.text = if (!invoice.notes.isNullOrEmpty()) invoice.notes else "--"
    }

    private fun deleteInvoice() {
        val invoiceId = invoice.id ?: return

        lifecycleScope.launch {
            try {
                val token = SessionManager.getToken(this@InvoiceDetailActivity)
                if (token != null) RetrofitClient.setToken(token)

                val response = RetrofitClient.api.deleteInvoice(invoiceId)

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@InvoiceDetailActivity,
                        "Invoice deleted ✅",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@InvoiceDetailActivity,
                        "Failed to delete invoice",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@InvoiceDetailActivity,
                    "Network error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
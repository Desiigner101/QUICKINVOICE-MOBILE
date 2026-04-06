package com.sarsonasgino.quickinvoicemobile.features.invoice

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.sarsonasgino.quickinvoicemobile.R
import com.sarsonasgino.quickinvoicemobile.core.model.Invoice
import com.sarsonasgino.quickinvoicemobile.databinding.ActivityInvoiceDetailBinding

class InvoiceDetailActivity : AppCompatActivity(), InvoiceDetailContract.View {

    private lateinit var binding: ActivityInvoiceDetailBinding
    private lateinit var presenter: InvoiceDetailPresenter
    private lateinit var invoice: Invoice

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInvoiceDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val invoiceJson = intent.getStringExtra("invoice_json") ?: run { finish(); return }
        invoice = Gson().fromJson(invoiceJson, Invoice::class.java)

        presenter = InvoiceDetailPresenter(this, this)

        populateData()
        setupListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }

    // --- InvoiceDetailContract.View implementations ---

    override fun showDeleteSuccess() {
        Toast.makeText(this, "Invoice deleted ✅", Toast.LENGTH_SHORT).show()
    }

    override fun showDeleteError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showNetworkError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun navigateToPreview(invoiceJson: String) {
        val intent = Intent(this, InvoicePreviewActivity::class.java)
        intent.putExtra("invoice_json", invoiceJson)
        startActivity(intent)
    }

    override fun closeScreen() {
        finish()
    }

    // --- Private UI helpers ---

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnPreview.setOnClickListener {
            presenter.onPreviewClicked(Gson().toJson(invoice))
        }

        binding.btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Invoice")
                .setMessage("Are you sure you want to delete \"${invoice.title}\"?")
                .setPositiveButton("Delete") { _, _ ->
                    invoice.id?.let { presenter.deleteInvoice(it) }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun populateData() {
        binding.tvInvoiceTitle.text = invoice.title ?: "Invoice Detail"

        if (!invoice.thumbnailUrl.isNullOrEmpty()) {
            Glide.with(this).load(invoice.thumbnailUrl).centerCrop().into(binding.ivThumbnail)
        } else {
            binding.ivThumbnail.setImageResource(R.drawable.ic_launcher_background)
        }

        binding.tvInvoiceNumber.text = invoice.invoice?.number ?: "--"
        binding.tvInvoiceDate.text = invoice.invoice?.date ?: "--"
        binding.tvInvoiceDueDate.text = invoice.invoice?.dueDate ?: "--"

        binding.tvCompanyName.text = invoice.company?.name ?: "--"
        binding.tvCompanyPhone.text = invoice.company?.phone ?: "--"
        binding.tvCompanyAddress.text = invoice.company?.address ?: "--"

        binding.tvBillingName.text = invoice.billing?.name ?: "--"
        binding.tvBillingPhone.text = invoice.billing?.phone ?: "--"
        binding.tvBillingAddress.text = invoice.billing?.address ?: "--"

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

        val taxAmount = subtotal * invoice.tax / 100
        val grandTotal = subtotal + taxAmount
        binding.tvSubtotal.text = "₱${String.format("%.2f", subtotal)}"
        binding.tvTaxAmount.text = "₱${String.format("%.2f", taxAmount)}"
        binding.tvGrandTotal.text = "₱${String.format("%.2f", grandTotal)}"
        binding.tvNotes.text = if (!invoice.notes.isNullOrEmpty()) invoice.notes else "--"
    }
}
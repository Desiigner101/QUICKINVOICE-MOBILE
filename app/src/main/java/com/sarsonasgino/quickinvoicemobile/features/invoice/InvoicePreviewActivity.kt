package com.sarsonasgino.quickinvoicemobile.features.invoice

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.sarsonasgino.quickinvoicemobile.core.model.Invoice
import com.sarsonasgino.quickinvoicemobile.core.network.RetrofitClient
import com.sarsonasgino.quickinvoicemobile.core.utils.SessionManager
import com.sarsonasgino.quickinvoicemobile.databinding.ActivityInvoicePreviewBinding
import kotlinx.coroutines.launch

class InvoicePreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInvoicePreviewBinding
    private lateinit var invoice: Invoice
    private var selectedTemplate = "template1"
    private val templateButtons = mutableListOf<Button>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInvoicePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val invoiceJson = intent.getStringExtra("invoice_json") ?: run { finish(); return }
        invoice = Gson().fromJson(invoiceJson, Invoice::class.java)
        selectedTemplate = invoice.template ?: "template1"

        setupWebView()
        setupTemplateButtons()
        setupListeners()
        renderTemplate(selectedTemplate)
    }

    private fun setupWebView() {
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.webViewClient = WebViewClient()
    }

    private fun setupTemplateButtons() {
        templateButtons.addAll(listOf(
            binding.btnTemplate1,
            binding.btnTemplate2,
            binding.btnTemplate3,
            binding.btnTemplate4,
            binding.btnTemplate5
        ))

        binding.btnTemplate1.setOnClickListener { selectTemplate("template1", binding.btnTemplate1) }
        binding.btnTemplate2.setOnClickListener { selectTemplate("template2", binding.btnTemplate2) }
        binding.btnTemplate3.setOnClickListener { selectTemplate("template3", binding.btnTemplate3) }
        binding.btnTemplate4.setOnClickListener { selectTemplate("template4", binding.btnTemplate4) }
        binding.btnTemplate5.setOnClickListener { selectTemplate("template5", binding.btnTemplate5) }

        // Highlight current template
        val activeBtn = when (selectedTemplate) {
            "template2" -> binding.btnTemplate2
            "template3" -> binding.btnTemplate3
            "template4" -> binding.btnTemplate4
            "template5" -> binding.btnTemplate5
            else -> binding.btnTemplate1
        }
        highlightButton(activeBtn)
    }

    private fun selectTemplate(template: String, button: Button) {
        selectedTemplate = template
        highlightButton(button)
        renderTemplate(template)
    }

    private fun highlightButton(activeButton: Button) {
        templateButtons.forEach { btn ->
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#E5E7EB")
            )
            btn.setTextColor(android.graphics.Color.parseColor("#374151"))
        }
        activeButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor("#0D6EFD")
        )
        activeButton.setTextColor(android.graphics.Color.WHITE)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnSave.setOnClickListener {
            saveInvoice()
        }

        binding.btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Invoice")
                .setMessage("Are you sure you want to delete \"${invoice.title}\"?")
                .setPositiveButton("Delete") { _, _ -> deleteInvoice() }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun renderTemplate(template: String) {
        val html = when (template) {
            "template2" -> generateTemplate2Html(invoice)
            "template3" -> generateTemplate3Html(invoice)
            "template4" -> generateTemplate4Html(invoice)
            "template5" -> generateTemplate5Html(invoice)
            else -> generateTemplate1Html(invoice)
        }
        binding.webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    private fun buildItemsRows(invoice: Invoice): String {
        return invoice.items?.joinToString("") { item ->
            val total = item.qty * item.amount
            "<tr><td>${item.name ?: ""}</td><td style='text-align:center'>${item.qty}</td><td style='text-align:right'>₱${String.format("%.2f", item.amount)}</td><td style='text-align:right'>₱${String.format("%.2f", total)}</td></tr>"
        } ?: ""
    }

    private fun calcTotals(invoice: Invoice): Triple<Double, Double, Double> {
        val subtotal = invoice.items?.sumOf { it.qty * it.amount } ?: 0.0
        val taxAmount = subtotal * (invoice.tax) / 100
        val total = subtotal + taxAmount
        return Triple(subtotal, taxAmount, total)
    }

    // ─── TEMPLATE 1 ────────────────────────────────────────────────────────────
    private fun generateTemplate1Html(invoice: Invoice): String {
        val (subtotal, taxAmount, total) = calcTotals(invoice)
        return """
        <!DOCTYPE html><html><head><meta charset='UTF-8'>
        <meta name='viewport' content='width=device-width, initial-scale=1.0'>
        <style>
            body { font-family: Arial, sans-serif; padding: 16px; font-size: 14px; }
            .header { display: flex; justify-content: space-between; margin-bottom: 16px; }
            .invoice-title { color: #ea580c; font-size: 28px; font-weight: bold; }
            .company-title { font-size: 18px; font-weight: bold; }
            hr { border-color: #ea580c; }
            .billing-box { background: #fff7ed; padding: 12px; border-radius: 8px; margin-bottom: 12px; }
            .billing-title { color: #ea580c; font-weight: bold; }
            table { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
            th { background: #ea580c; color: white; padding: 8px; text-align: left; }
            td { padding: 8px; border-bottom: 1px solid #eee; }
            .totals-box { background: #fff7ed; padding: 12px; border-radius: 8px; float: right; min-width: 200px; }
            .total-highlight { color: #ea580c; font-weight: bold; }
            .clearfix::after { content: ''; display: table; clear: both; }
        </style></head><body>
        <div class='header'>
            <div>
                <div class='company-title'>${invoice.company?.name ?: ""}</div>
                <div>${invoice.company?.address ?: ""}</div>
                <div>Phone: ${invoice.company?.phone ?: ""}</div>
            </div>
            <div style='text-align:right'>
                <div class='invoice-title'>Invoice</div>
                <div><strong>Invoice#:</strong> ${invoice.invoice?.number ?: ""}</div>
                <div><strong>Date:</strong> ${invoice.invoice?.date ?: ""}</div>
                <div><strong>Due:</strong> ${invoice.invoice?.dueDate ?: ""}</div>
            </div>
        </div>
        <hr/>
        <div class='billing-box'>
            <div class='billing-title'>Billed To</div>
            <strong>${invoice.billing?.name ?: ""}</strong>
            <div>${invoice.billing?.address ?: ""}</div>
            <div>Phone: ${invoice.billing?.phone ?: ""}</div>
        </div>
        <table>
            <thead><tr><th>Item</th><th>Qty</th><th>Rate</th><th>Amount</th></tr></thead>
            <tbody>${buildItemsRows(invoice)}</tbody>
        </table>
        <div class='clearfix'>
            <div class='totals-box'>
                <div style='display:flex;justify-content:space-between'><span>Subtotal:</span><span>₱${String.format("%.2f", subtotal)}</span></div>
                <div style='display:flex;justify-content:space-between'><span>Tax (${invoice.tax}%):</span><span>₱${String.format("%.2f", taxAmount)}</span></div>
                <div class='total-highlight' style='display:flex;justify-content:space-between'><span>Total:</span><span>₱${String.format("%.2f", total)}</span></div>
            </div>
        </div>
        ${if (!invoice.notes.isNullOrEmpty()) "<div style='margin-top:16px'><div class='billing-title'>Notes</div><p>${invoice.notes}</p></div>" else ""}
        </body></html>
        """.trimIndent()
    }

    // ─── TEMPLATE 2 ────────────────────────────────────────────────────────────
    private fun generateTemplate2Html(invoice: Invoice): String {
        val (subtotal, taxAmount, total) = calcTotals(invoice)
        return """
        <!DOCTYPE html><html><head><meta charset='UTF-8'>
        <meta name='viewport' content='width=device-width, initial-scale=1.0'>
        <style>
            body { font-family: Arial, sans-serif; padding: 16px; font-size: 14px; }
            .header { display: flex; justify-content: space-between; margin-bottom: 16px; }
            h2 { color: #198754; }
            .company-title { color: #198754; font-weight: bold; }
            table { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
            th { background: #198754; color: white; padding: 8px; text-align: left; }
            td { padding: 8px; border-bottom: 1px solid #eee; }
            tr:nth-child(even) { background: #f8f9fa; }
            .notes-section { background: #f0fdf4; border-top: 3px solid #22c55e; padding: 12px; }
            .total { color: #198754; font-weight: bold; font-size: 16px; }
        </style></head><body>
        <div class='header'>
            <h2>Invoice</h2>
            <div style='text-align:right'>
                <div class='company-title'>${invoice.company?.name ?: ""}</div>
                <div>${invoice.company?.address ?: ""}</div>
                <div>${invoice.company?.phone ?: ""}</div>
            </div>
        </div>
        <div style='display:flex;justify-content:space-between;margin-bottom:16px'>
            <div>
                <div style='color:#198754;font-weight:600'>Billed To</div>
                <strong>${invoice.billing?.name ?: ""}</strong>
                <div>${invoice.billing?.address ?: ""}</div>
                <div>${invoice.billing?.phone ?: ""}</div>
            </div>
            <div style='text-align:right'>
                <div style='color:#198754;font-weight:600'>Invoice Details</div>
                <div><strong>Invoice #:</strong> ${invoice.invoice?.number ?: ""}</div>
                <div><strong>Date:</strong> ${invoice.invoice?.date ?: ""}</div>
                <div><strong>Due:</strong> ${invoice.invoice?.dueDate ?: ""}</div>
            </div>
        </div>
        <table>
            <thead><tr><th>Item Description</th><th>Qty</th><th>Rate</th><th>Amount</th></tr></thead>
            <tbody>${buildItemsRows(invoice)}</tbody>
        </table>
        <div style='text-align:right;margin-bottom:16px'>
            <div><strong>Sub Total:</strong> ₱${String.format("%.2f", subtotal)}</div>
            <div><strong>Tax (${invoice.tax}%):</strong> ₱${String.format("%.2f", taxAmount)}</div>
            <div class='total'>Total Due: ₱${String.format("%.2f", total)}</div>
        </div>
        ${if (!invoice.notes.isNullOrEmpty()) "<div class='notes-section'><div style='color:#198754;font-weight:600'>Additional Notes</div><p>${invoice.notes}</p></div>" else ""}
        </body></html>
        """.trimIndent()
    }

    // ─── TEMPLATE 3 ────────────────────────────────────────────────────────────
    private fun generateTemplate3Html(invoice: Invoice): String {
        val (subtotal, taxAmount, total) = calcTotals(invoice)
        return """
        <!DOCTYPE html><html><head><meta charset='UTF-8'>
        <meta name='viewport' content='width=device-width, initial-scale=1.0'>
        <style>
            body { font-family: Arial, sans-serif; padding: 16px; font-size: 14px; }
            .heading { color: #8a3ff3; font-size: 28px; font-weight: bold; }
            .subheading { color: #8a3ff3; font-weight: bold; }
            .bill-box { background: #f5f0ff; padding: 12px; border-radius: 8px; margin-bottom: 12px; }
            table { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
            th { background: #8a3ff3; color: white; padding: 8px; }
            td { padding: 8px; border-bottom: 1px solid #eee; }
            .total-box { float: right; min-width: 200px; }
            .total { color: #8a3ff3; font-weight: bold; }
            .clearfix::after { content: ''; display: table; clear: both; }
        </style></head><body>
        <div style='display:flex;justify-content:space-between;margin-bottom:16px'>
            <div>
                <div class='heading'>Invoice</div>
                <div><strong>Invoice#:</strong> ${invoice.invoice?.number ?: ""}</div>
                <div><strong>Date:</strong> ${invoice.invoice?.date ?: ""}</div>
                <div><strong>Due:</strong> ${invoice.invoice?.dueDate ?: ""}</div>
            </div>
            <div style='text-align:right'>
                <strong>${invoice.company?.name ?: ""}</strong>
                <div>${invoice.company?.address ?: ""}</div>
                <div>${invoice.company?.phone ?: ""}</div>
            </div>
        </div>
        <div class='bill-box'>
            <div class='subheading'>Billed To</div>
            <div>${invoice.billing?.name ?: ""}</div>
            <div>${invoice.billing?.address ?: ""}</div>
            <div>${invoice.billing?.phone ?: ""}</div>
        </div>
        <table>
            <thead><tr><th>Item</th><th>Qty</th><th>Rate</th><th>Amount</th></tr></thead>
            <tbody>${buildItemsRows(invoice)}</tbody>
        </table>
        <div class='clearfix'>
            <div class='total-box'>
                <div style='display:flex;justify-content:space-between'><span>Sub Total:</span><span>₱${String.format("%.2f", subtotal)}</span></div>
                <div style='display:flex;justify-content:space-between'><span>Tax (${invoice.tax}%):</span><span>₱${String.format("%.2f", taxAmount)}</span></div>
                <div class='total' style='display:flex;justify-content:space-between'><span>Total:</span><span>₱${String.format("%.2f", total)}</span></div>
            </div>
        </div>
        ${if (!invoice.notes.isNullOrEmpty()) "<div style='margin-top:32px;border-top:1px solid #eee;padding-top:12px'><div class='subheading'>Note</div><p>${invoice.notes}</p></div>" else ""}
        </body></html>
        """.trimIndent()
    }

    // ─── TEMPLATE 4 ────────────────────────────────────────────────────────────
    private fun generateTemplate4Html(invoice: Invoice): String {
        val (subtotal, taxAmount, total) = calcTotals(invoice)
        return """
        <!DOCTYPE html><html><head><meta charset='UTF-8'>
        <meta name='viewport' content='width=device-width, initial-scale=1.0'>
        <style>
            body { font-family: 'Segoe UI', Arial, sans-serif; padding: 16px; font-size: 14px; }
            .company-name { color: #00a9e0; font-weight: bold; font-size: 18px; }
            .primary { color: #00a9e0; }
            table { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
            th { background: #00a9e0; color: white; padding: 8px; }
            td { padding: 8px; border: 1px solid #dee2e6; }
            .total-row td { background: #00a9e0; color: white; font-weight: bold; }
        </style></head><body>
        <div style='display:flex;justify-content:space-between;margin-bottom:16px'>
            <div>
                <div class='company-name'>${invoice.company?.name ?: ""}</div>
                <div>${invoice.company?.address ?: ""}</div>
                <div>${invoice.company?.phone ?: ""}</div>
            </div>
            <div style='text-align:right'>
                <div class='primary' style='font-size:18px;font-weight:600'>Invoice</div>
                <div><strong>Invoice No:</strong> ${invoice.invoice?.number ?: ""}</div>
                <div><strong>Date:</strong> ${invoice.invoice?.date ?: ""}</div>
                <div><strong>Due:</strong> ${invoice.invoice?.dueDate ?: ""}</div>
            </div>
        </div>
        <div style='margin-bottom:16px'>
            <div class='primary' style='font-weight:bold'>Billed To</div>
            <div>${invoice.billing?.name ?: ""}</div>
            <div>${invoice.billing?.address ?: ""}</div>
            <div>${invoice.billing?.phone ?: ""}</div>
        </div>
        <table>
            <thead><tr><th>Item</th><th>Qty</th><th>Rate</th><th>Amount</th></tr></thead>
            <tbody>${buildItemsRows(invoice)}</tbody>
        </table>
        <div style='float:right;min-width:250px'>
            <table>
                <tbody>
                    <tr><td><strong>Sub Total</strong></td><td style='text-align:right'>₱${String.format("%.2f", subtotal)}</td></tr>
                    <tr><td><strong>Tax (${invoice.tax}%)</strong></td><td style='text-align:right'>₱${String.format("%.2f", taxAmount)}</td></tr>
                    <tr class='total-row'><td><strong>Total Due</strong></td><td style='text-align:right'><strong>₱${String.format("%.2f", total)}</strong></td></tr>
                </tbody>
            </table>
        </div>
        ${if (!invoice.notes.isNullOrEmpty()) "<div style='margin-top:80px'><p>${invoice.notes}</p></div>" else ""}
        </body></html>
        """.trimIndent()
    }

    // ─── TEMPLATE 5 ────────────────────────────────────────────────────────────
    private fun generateTemplate5Html(invoice: Invoice): String {
        val (subtotal, taxAmount, total) = calcTotals(invoice)
        return """
        <!DOCTYPE html><html><head><meta charset='UTF-8'>
        <meta name='viewport' content='width=device-width, initial-scale=1.0'>
        <style>
            body { font-family: Arial, sans-serif; padding: 16px; font-size: 14px; }
            table { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
            thead tr { border-bottom: 2px solid black; }
            tr.items-row { border-bottom: 2px solid black; }
            th { padding: 8px; text-align: left; font-weight: bold; }
            td { padding: 8px; }
        </style></head><body>
        <div style='display:flex;justify-content:space-between;margin-bottom:16px'>
            <div>
                <strong style='font-size:18px'>${invoice.company?.name ?: ""}</strong>
                <div>${invoice.company?.address ?: ""}</div>
                <div>${invoice.company?.phone ?: ""}</div>
            </div>
            <div style='text-align:right'>
                <div style='font-weight:bold;font-size:16px'>INVOICE</div>
                <div><strong>Invoice No:</strong> ${invoice.invoice?.number ?: ""}</div>
                <div><strong>Date:</strong> ${invoice.invoice?.date ?: ""}</div>
                <div><strong>Due:</strong> ${invoice.invoice?.dueDate ?: ""}</div>
            </div>
        </div>
        <div style='display:flex;justify-content:space-between;margin-bottom:16px'>
            <div>
                <div style='font-weight:bold'>Bill To:</div>
                <div>${invoice.billing?.name ?: ""}</div>
                <div>${invoice.billing?.address ?: ""}</div>
                <div>${invoice.billing?.phone ?: ""}</div>
            </div>
        </div>
        <table>
            <thead><tr><th>Item</th><th style='text-align:center'>Qty</th><th style='text-align:center'>Rate</th><th style='text-align:right'>Amount</th></tr></thead>
            <tbody>${invoice.items?.joinToString("") { item ->
            val t = item.qty * item.amount
            "<tr class='items-row'><td>${item.name ?: ""}</td><td style='text-align:center'>${item.qty}</td><td style='text-align:center'>₱${String.format("%.2f", item.amount)}</td><td style='text-align:right'>₱${String.format("%.2f", t)}</td></tr>"
        } ?: ""}</tbody>
        </table>
        <div style='float:right;min-width:200px'>
            <table>
                <tbody>
                    <tr><td><strong>Subtotal</strong></td><td style='text-align:right'>₱${String.format("%.2f", subtotal)}</td></tr>
                    <tr><td><strong>Tax (${invoice.tax}%)</strong></td><td style='text-align:right'>₱${String.format("%.2f", taxAmount)}</td></tr>
                    <tr><td><strong>Total</strong></td><td style='text-align:right;font-weight:bold'>₱${String.format("%.2f", total)}</td></tr>
                </tbody>
            </table>
        </div>
        ${if (!invoice.notes.isNullOrEmpty()) "<div style='margin-top:80px'><div style='font-weight:bold'>Notes</div><p>${invoice.notes}</p></div>" else ""}
        </body></html>
        """.trimIndent()
    }

    private fun saveInvoice() {
        val updatedInvoice = invoice.copy(template = selectedTemplate)
        binding.btnSave.isEnabled = false

        lifecycleScope.launch {
            try {
                val token = SessionManager.getToken(this@InvoicePreviewActivity)
                if (token != null) RetrofitClient.setToken(token)

                val response = RetrofitClient.api.saveInvoice(updatedInvoice)
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@InvoicePreviewActivity,
                        "Invoice saved! ✅", Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@InvoicePreviewActivity,
                        "Failed to save", Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@InvoicePreviewActivity,
                    "Network error: ${e.message}", Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.btnSave.isEnabled = true
            }
        }
    }

    private fun deleteInvoice() {
        val invoiceId = invoice.id ?: return
        lifecycleScope.launch {
            try {
                val token = SessionManager.getToken(this@InvoicePreviewActivity)
                if (token != null) RetrofitClient.setToken(token)

                val response = RetrofitClient.api.deleteInvoice(invoiceId)
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@InvoicePreviewActivity,
                        "Invoice deleted ✅", Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@InvoicePreviewActivity,
                        "Failed to delete", Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@InvoicePreviewActivity,
                    "Network error: ${e.message}", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
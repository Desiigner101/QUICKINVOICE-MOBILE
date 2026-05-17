package com.sarsonasgino.quickinvoicemobile.features.invoice

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.sarsonasgino.quickinvoicemobile.R
import com.sarsonasgino.quickinvoicemobile.core.model.Invoice
import com.sarsonasgino.quickinvoicemobile.core.network.RetrofitClient
import com.sarsonasgino.quickinvoicemobile.core.utils.SessionManager
import com.sarsonasgino.quickinvoicemobile.databinding.ActivityInvoicePreviewBinding
import com.sarsonasgino.quickinvoicemobile.features.subscription.SubscriptionActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

class InvoicePreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInvoicePreviewBinding
    private lateinit var invoice: Invoice
    private var selectedTemplate = "template1"
    private val templateButtons = mutableListOf<Button>()
    private val premiumTemplates = setOf("template6", "template7", "template8", "template9", "template10")

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
        binding.webView.webViewClient = android.webkit.WebViewClient()
    }

    private fun setupTemplateButtons() {
        templateButtons.addAll(listOf(
            binding.btnTemplate1, binding.btnTemplate2, binding.btnTemplate3,
            binding.btnTemplate4, binding.btnTemplate5, binding.btnTemplate6,
            binding.btnTemplate7, binding.btnTemplate8, binding.btnTemplate9,
            binding.btnTemplate10
        ))

        val ids = listOf(
            "template1", "template2", "template3", "template4", "template5",
            "template6", "template7", "template8", "template9", "template10"
        )
        templateButtons.forEachIndexed { i, btn ->
            btn.setOnClickListener { handleSelectTemplate(ids[i], btn) }
        }

        val activeBtn = templateButtons.getOrElse(
            ids.indexOf(selectedTemplate).coerceAtLeast(0)
        ) { templateButtons[0] }
        highlightButton(activeBtn)
    }

    private fun handleSelectTemplate(templateId: String, button: Button) {
        if (premiumTemplates.contains(templateId) && !SessionManager.getIsPremium(this)) {
            AlertDialog.Builder(this)
                .setTitle("Premium Template")
                .setMessage("This template requires a Premium subscription. Upgrade to unlock all 10 templates.")
                .setPositiveButton("Upgrade") { _, _ ->
                    startActivity(Intent(this, SubscriptionActivity::class.java))
                }
                .setNegativeButton("Maybe Later", null)
                .show()
            return
        }
        selectedTemplate = templateId
        highlightButton(button)
        renderTemplate(templateId)
    }

    private fun highlightButton(activeButton: Button) {
        templateButtons.forEach { btn ->
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                Color.parseColor("#E5E7EB"))
            btn.setTextColor(Color.parseColor("#374151"))
        }
        activeButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
            Color.parseColor("#0D6EFD"))
        activeButton.setTextColor(Color.WHITE)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { saveInvoice() }
        binding.btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Invoice")
                .setMessage("Are you sure you want to delete \"${invoice.title}\"?")
                .setPositiveButton("Delete") { _, _ -> deleteInvoice() }
                .setNegativeButton("Cancel", null)
                .show()
        }
        binding.btnSendEmail.setOnClickListener { showEmailDialog() }
        binding.btnDownloadPdf.setOnClickListener { downloadPdf() }
    }

    private fun showEmailDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_send_email, null)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEmail)

        AlertDialog.Builder(this)
            .setTitle("Send Invoice via Email")
            .setView(dialogView)
            .setPositiveButton("Send") { _, _ ->
                val email = etEmail.text.toString().trim()
                if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
                } else {
                    sendInvoiceEmail(email)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendInvoiceEmail(email: String) {
        binding.btnSendEmail.isEnabled = false
        Toast.makeText(this, "Preparing PDF...", Toast.LENGTH_SHORT).show()

        // capturePicture() must run on the main thread — lifecycleScope.launch is already main
        lifecycleScope.launch {
            try {
                val pdfBytes = generatePdfBytes()
                if (pdfBytes.isEmpty()) {
                    Toast.makeText(this@InvoicePreviewActivity, "Invoice not ready. Wait for it to fully load, then try again.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val token = SessionManager.getToken(this@InvoicePreviewActivity)
                if (token != null) RetrofitClient.setToken(token)

                val requestFile = pdfBytes.toRequestBody("application/pdf".toMediaType())
                val filePart = MultipartBody.Part.createFormData("file", "invoice.pdf", requestFile)
                val emailBody = email.toRequestBody("text/plain".toMediaType())

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.api.sendInvoice(filePart, emailBody)
                }

                if (response.isSuccessful) {
                    Toast.makeText(this@InvoicePreviewActivity, "Invoice sent to $email!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@InvoicePreviewActivity, "Failed to send email.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@InvoicePreviewActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnSendEmail.isEnabled = true
            }
        }
    }

    private fun downloadPdf() {
        binding.btnDownloadPdf.isEnabled = false
        Toast.makeText(this, "Generating PDF...", Toast.LENGTH_SHORT).show()

        // capturePicture() must run on the main thread — lifecycleScope.launch is already main
        lifecycleScope.launch {
            try {
                val pdfBytes = generatePdfBytes()
                if (pdfBytes.isEmpty()) {
                    Toast.makeText(this@InvoicePreviewActivity, "Invoice not ready. Wait for it to fully load, then try again.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val fileName = "invoice_${System.currentTimeMillis()}.pdf"
                withContext(Dispatchers.IO) { savePdfToDownloads(pdfBytes, fileName) }
                Toast.makeText(this@InvoicePreviewActivity, "PDF saved to Downloads!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@InvoicePreviewActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnDownloadPdf.isEnabled = true
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun generatePdfBytes(): ByteArray {
        val picture = binding.webView.capturePicture()
        if (picture.width <= 0 || picture.height <= 0) return ByteArray(0)

        val bitmap = Bitmap.createBitmap(picture.width, picture.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        picture.draw(canvas)

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = document.startPage(pageInfo)
        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
        document.finishPage(page)

        val stream = ByteArrayOutputStream()
        document.writeTo(stream)
        document.close()
        bitmap.recycle()
        return stream.toByteArray()
    }

    private fun savePdfToDownloads(pdfBytes: ByteArray, fileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let { contentResolver.openOutputStream(it)?.use { os -> os.write(pdfBytes) } }
        } else {
            val file = java.io.File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )
            file.writeBytes(pdfBytes)
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Template rendering
    // ─────────────────────────────────────────────────────────────────

    private fun renderTemplate(template: String) {
        val html = when (template) {
            "template2" -> generateTemplate2Html(invoice)
            "template3" -> generateTemplate3Html(invoice)
            "template4" -> generateTemplate4Html(invoice)
            "template5" -> generateTemplate5Html(invoice)
            "template6" -> generateTemplate6Html(invoice)
            "template7" -> generateTemplate7Html(invoice)
            "template8" -> generateTemplate8Html(invoice)
            "template9" -> generateTemplate9Html(invoice)
            "template10" -> generateTemplate10Html(invoice)
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
        val taxAmount = subtotal * invoice.tax / 100
        return Triple(subtotal, taxAmount, subtotal + taxAmount)
    }

    // ─── FREE TEMPLATES ────────────────────────────────────────────────────────

    private fun generateTemplate1Html(invoice: Invoice): String {
        val (subtotal, taxAmount, total) = calcTotals(invoice)
        return """<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1.0'>
        <style>body{font-family:Arial,sans-serif;padding:16px;font-size:14px}.header{display:flex;justify-content:space-between;margin-bottom:16px}.invoice-title{color:#ea580c;font-size:28px;font-weight:bold}.company-title{font-size:18px;font-weight:bold}hr{border-color:#ea580c}.billing-box{background:#fff7ed;padding:12px;border-radius:8px;margin-bottom:12px}.billing-title{color:#ea580c;font-weight:bold}table{width:100%;border-collapse:collapse;margin-bottom:16px}th{background:#ea580c;color:white;padding:8px;text-align:left}td{padding:8px;border-bottom:1px solid #eee}.totals-box{background:#fff7ed;padding:12px;border-radius:8px;float:right;min-width:200px}.total-highlight{color:#ea580c;font-weight:bold}.clearfix::after{content:'';display:table;clear:both}</style></head><body>
        <div class='header'><div><div class='company-title'>${invoice.company?.name ?: ""}</div><div>${invoice.company?.address ?: ""}</div><div>Phone: ${invoice.company?.phone ?: ""}</div></div>
        <div style='text-align:right'><div class='invoice-title'>Invoice</div><div><strong>Invoice#:</strong> ${invoice.invoice?.number ?: ""}</div><div><strong>Date:</strong> ${invoice.invoice?.date ?: ""}</div><div><strong>Due:</strong> ${invoice.invoice?.dueDate ?: ""}</div></div></div>
        <hr/>
        <div class='billing-box'><div class='billing-title'>Billed To</div><strong>${invoice.billing?.name ?: ""}</strong><div>${invoice.billing?.address ?: ""}</div><div>Phone: ${invoice.billing?.phone ?: ""}</div></div>
        <table><thead><tr><th>Item</th><th>Qty</th><th>Rate</th><th>Amount</th></tr></thead><tbody>${buildItemsRows(invoice)}</tbody></table>
        <div class='clearfix'><div class='totals-box'><div style='display:flex;justify-content:space-between'><span>Subtotal:</span><span>₱${String.format("%.2f", subtotal)}</span></div><div style='display:flex;justify-content:space-between'><span>Tax (${invoice.tax}%):</span><span>₱${String.format("%.2f", taxAmount)}</span></div><div class='total-highlight' style='display:flex;justify-content:space-between'><span>Total:</span><span>₱${String.format("%.2f", total)}</span></div></div></div>
        ${if (!invoice.notes.isNullOrEmpty()) "<div style='margin-top:16px'><div class='billing-title'>Notes</div><p>${invoice.notes}</p></div>" else ""}
        </body></html>""".trimIndent()
    }

    private fun generateTemplate2Html(invoice: Invoice): String {
        val (subtotal, taxAmount, total) = calcTotals(invoice)
        return """<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1.0'>
        <style>body{font-family:Arial,sans-serif;padding:16px;font-size:14px}.header{display:flex;justify-content:space-between;margin-bottom:16px}h2{color:#198754}.company-title{color:#198754;font-weight:bold}table{width:100%;border-collapse:collapse;margin-bottom:16px}th{background:#198754;color:white;padding:8px;text-align:left}td{padding:8px;border-bottom:1px solid #eee}tr:nth-child(even){background:#f8f9fa}.notes-section{background:#f0fdf4;border-top:3px solid #22c55e;padding:12px}.total{color:#198754;font-weight:bold;font-size:16px}</style></head><body>
        <div class='header'><h2>Invoice</h2><div style='text-align:right'><div class='company-title'>${invoice.company?.name ?: ""}</div><div>${invoice.company?.address ?: ""}</div><div>${invoice.company?.phone ?: ""}</div></div></div>
        <div style='display:flex;justify-content:space-between;margin-bottom:16px'><div><div style='color:#198754;font-weight:600'>Billed To</div><strong>${invoice.billing?.name ?: ""}</strong><div>${invoice.billing?.address ?: ""}</div><div>${invoice.billing?.phone ?: ""}</div></div>
        <div style='text-align:right'><div style='color:#198754;font-weight:600'>Invoice Details</div><div><strong>Invoice #:</strong> ${invoice.invoice?.number ?: ""}</div><div><strong>Date:</strong> ${invoice.invoice?.date ?: ""}</div><div><strong>Due:</strong> ${invoice.invoice?.dueDate ?: ""}</div></div></div>
        <table><thead><tr><th>Item Description</th><th>Qty</th><th>Rate</th><th>Amount</th></tr></thead><tbody>${buildItemsRows(invoice)}</tbody></table>
        <div style='text-align:right;margin-bottom:16px'><div><strong>Sub Total:</strong> ₱${String.format("%.2f", subtotal)}</div><div><strong>Tax (${invoice.tax}%):</strong> ₱${String.format("%.2f", taxAmount)}</div><div class='total'>Total Due: ₱${String.format("%.2f", total)}</div></div>
        ${if (!invoice.notes.isNullOrEmpty()) "<div class='notes-section'><div style='color:#198754;font-weight:600'>Additional Notes</div><p>${invoice.notes}</p></div>" else ""}
        </body></html>""".trimIndent()
    }

    private fun generateTemplate3Html(invoice: Invoice): String {
        val (subtotal, taxAmount, total) = calcTotals(invoice)
        return """<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1.0'>
        <style>body{font-family:Arial,sans-serif;padding:16px;font-size:14px}.heading{color:#8a3ff3;font-size:28px;font-weight:bold}.subheading{color:#8a3ff3;font-weight:bold}.bill-box{background:#f5f0ff;padding:12px;border-radius:8px;margin-bottom:12px}table{width:100%;border-collapse:collapse;margin-bottom:16px}th{background:#8a3ff3;color:white;padding:8px}td{padding:8px;border-bottom:1px solid #eee}.total-box{float:right;min-width:200px}.total{color:#8a3ff3;font-weight:bold}.clearfix::after{content:'';display:table;clear:both}</style></head><body>
        <div style='display:flex;justify-content:space-between;margin-bottom:16px'><div><div class='heading'>Invoice</div><div><strong>Invoice#:</strong> ${invoice.invoice?.number ?: ""}</div><div><strong>Date:</strong> ${invoice.invoice?.date ?: ""}</div><div><strong>Due:</strong> ${invoice.invoice?.dueDate ?: ""}</div></div>
        <div style='text-align:right'><strong>${invoice.company?.name ?: ""}</strong><div>${invoice.company?.address ?: ""}</div><div>${invoice.company?.phone ?: ""}</div></div></div>
        <div class='bill-box'><div class='subheading'>Billed To</div><div>${invoice.billing?.name ?: ""}</div><div>${invoice.billing?.address ?: ""}</div><div>${invoice.billing?.phone ?: ""}</div></div>
        <table><thead><tr><th>Item</th><th>Qty</th><th>Rate</th><th>Amount</th></tr></thead><tbody>${buildItemsRows(invoice)}</tbody></table>
        <div class='clearfix'><div class='total-box'><div style='display:flex;justify-content:space-between'><span>Sub Total:</span><span>₱${String.format("%.2f", subtotal)}</span></div><div style='display:flex;justify-content:space-between'><span>Tax (${invoice.tax}%):</span><span>₱${String.format("%.2f", taxAmount)}</span></div><div class='total' style='display:flex;justify-content:space-between'><span>Total:</span><span>₱${String.format("%.2f", total)}</span></div></div></div>
        ${if (!invoice.notes.isNullOrEmpty()) "<div style='margin-top:32px;border-top:1px solid #eee;padding-top:12px'><div class='subheading'>Note</div><p>${invoice.notes}</p></div>" else ""}
        </body></html>""".trimIndent()
    }

    private fun generateTemplate4Html(invoice: Invoice): String {
        val (subtotal, taxAmount, total) = calcTotals(invoice)
        return """<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1.0'>
        <style>body{font-family:'Segoe UI',Arial,sans-serif;padding:16px;font-size:14px}.company-name{color:#00a9e0;font-weight:bold;font-size:18px}.primary{color:#00a9e0}table{width:100%;border-collapse:collapse;margin-bottom:16px}th{background:#00a9e0;color:white;padding:8px}td{padding:8px;border:1px solid #dee2e6}.total-row td{background:#00a9e0;color:white;font-weight:bold}</style></head><body>
        <div style='display:flex;justify-content:space-between;margin-bottom:16px'><div><div class='company-name'>${invoice.company?.name ?: ""}</div><div>${invoice.company?.address ?: ""}</div><div>${invoice.company?.phone ?: ""}</div></div>
        <div style='text-align:right'><div class='primary' style='font-size:18px;font-weight:600'>Invoice</div><div><strong>Invoice No:</strong> ${invoice.invoice?.number ?: ""}</div><div><strong>Date:</strong> ${invoice.invoice?.date ?: ""}</div><div><strong>Due:</strong> ${invoice.invoice?.dueDate ?: ""}</div></div></div>
        <div style='margin-bottom:16px'><div class='primary' style='font-weight:bold'>Billed To</div><div>${invoice.billing?.name ?: ""}</div><div>${invoice.billing?.address ?: ""}</div><div>${invoice.billing?.phone ?: ""}</div></div>
        <table><thead><tr><th>Item</th><th>Qty</th><th>Rate</th><th>Amount</th></tr></thead><tbody>${buildItemsRows(invoice)}</tbody></table>
        <div style='float:right;min-width:250px'><table><tbody><tr><td><strong>Sub Total</strong></td><td style='text-align:right'>₱${String.format("%.2f", subtotal)}</td></tr><tr><td><strong>Tax (${invoice.tax}%)</strong></td><td style='text-align:right'>₱${String.format("%.2f", taxAmount)}</td></tr><tr class='total-row'><td><strong>Total Due</strong></td><td style='text-align:right'><strong>₱${String.format("%.2f", total)}</strong></td></tr></tbody></table></div>
        ${if (!invoice.notes.isNullOrEmpty()) "<div style='margin-top:80px'><p>${invoice.notes}</p></div>" else ""}
        </body></html>""".trimIndent()
    }

    private fun generateTemplate5Html(invoice: Invoice): String {
        val (subtotal, taxAmount, total) = calcTotals(invoice)
        return """<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1.0'>
        <style>body{font-family:Arial,sans-serif;padding:16px;font-size:14px}table{width:100%;border-collapse:collapse;margin-bottom:16px}thead tr{border-bottom:2px solid black}tr.items-row{border-bottom:2px solid black}th{padding:8px;text-align:left;font-weight:bold}td{padding:8px}</style></head><body>
        <div style='display:flex;justify-content:space-between;margin-bottom:16px'><div><strong style='font-size:18px'>${invoice.company?.name ?: ""}</strong><div>${invoice.company?.address ?: ""}</div><div>${invoice.company?.phone ?: ""}</div></div>
        <div style='text-align:right'><div style='font-weight:bold;font-size:16px'>INVOICE</div><div><strong>Invoice No:</strong> ${invoice.invoice?.number ?: ""}</div><div><strong>Date:</strong> ${invoice.invoice?.date ?: ""}</div><div><strong>Due:</strong> ${invoice.invoice?.dueDate ?: ""}</div></div></div>
        <div style='display:flex;justify-content:space-between;margin-bottom:16px'><div><div style='font-weight:bold'>Bill To:</div><div>${invoice.billing?.name ?: ""}</div><div>${invoice.billing?.address ?: ""}</div><div>${invoice.billing?.phone ?: ""}</div></div></div>
        <table><thead><tr><th>Item</th><th style='text-align:center'>Qty</th><th style='text-align:center'>Rate</th><th style='text-align:right'>Amount</th></tr></thead>
        <tbody>${invoice.items?.joinToString("") { "<tr class='items-row'><td>${it.name ?: ""}</td><td style='text-align:center'>${it.qty}</td><td style='text-align:center'>₱${String.format("%.2f", it.amount)}</td><td style='text-align:right'>₱${String.format("%.2f", it.qty * it.amount)}</td></tr>" } ?: ""}</tbody></table>
        <div style='float:right;min-width:200px'><table><tbody><tr><td><strong>Subtotal</strong></td><td style='text-align:right'>₱${String.format("%.2f", subtotal)}</td></tr><tr><td><strong>Tax (${invoice.tax}%)</strong></td><td style='text-align:right'>₱${String.format("%.2f", taxAmount)}</td></tr><tr><td><strong>Total</strong></td><td style='text-align:right;font-weight:bold'>₱${String.format("%.2f", total)}</td></tr></tbody></table></div>
        ${if (!invoice.notes.isNullOrEmpty()) "<div style='margin-top:80px'><div style='font-weight:bold'>Notes</div><p>${invoice.notes}</p></div>" else ""}
        </body></html>""".trimIndent()
    }

    // ─── PREMIUM TEMPLATES ─────────────────────────────────────────────────────

    private fun generateTemplate6Html(invoice: Invoice): String {
        val (subtotal, taxAmount, total) = calcTotals(invoice)
        return """<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1.0'>
        <style>body{font-family:'Segoe UI',Arial,sans-serif;margin:0;padding:0;background:#fff}
        .header{background:#1e3a5f;color:#fff;padding:24px 20px}.header-inner{display:flex;justify-content:space-between;align-items:flex-start}
        .co-name{font-size:20px;font-weight:700;margin-bottom:4px}.co-sub{font-size:12px;opacity:.8;line-height:1.5}
        .inv-label{font-size:30px;font-weight:700;color:#7dd3fc;letter-spacing:2px}.inv-meta{font-size:12px;color:#cbd5e1;margin-top:6px;line-height:1.8}
        .body{padding:20px}.section-label{color:#1e3a5f;font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:1px;margin-bottom:6px}
        .bill-row{display:flex;justify-content:space-between;margin-bottom:20px;gap:16px}.bill-col{flex:1}
        table{width:100%;border-collapse:collapse;margin-bottom:20px}
        th{background:#1e3a5f;color:#fff;padding:10px;font-size:13px;text-align:left}td{padding:10px;border-bottom:1px solid #e2e8f0;font-size:13px}
        .totals{float:right;min-width:220px;background:#f8fafc;padding:16px;border-radius:8px;border:1px solid #e2e8f0}
        .t-row{display:flex;justify-content:space-between;margin-bottom:6px;font-size:13px}
        .grand{color:#1e3a5f;font-weight:700;font-size:15px;border-top:2px solid #1e3a5f;padding-top:8px;margin-top:6px;display:flex;justify-content:space-between}
        .clearfix::after{content:'';display:table;clear:both}
        .notes{margin-top:80px;border-top:2px solid #1e3a5f;padding-top:12px}.notes-label{color:#1e3a5f;font-weight:700;font-size:13px;margin-bottom:6px}</style></head><body>
        <div class='header'><div class='header-inner'>
          <div><div class='co-name'>${invoice.company?.name ?: ""}</div><div class='co-sub'>${invoice.company?.address ?: ""}<br>${invoice.company?.phone ?: ""}</div></div>
          <div style='text-align:right'><div class='inv-label'>INVOICE</div><div class='inv-meta'><strong>#${invoice.invoice?.number ?: ""}</strong><br>Date: ${invoice.invoice?.date ?: ""}<br>Due: ${invoice.invoice?.dueDate ?: ""}</div></div>
        </div></div>
        <div class='body'>
          <div class='bill-row'><div class='bill-col'><div class='section-label'>Bill To</div><div style='font-weight:600'>${invoice.billing?.name ?: ""}</div><div style='font-size:13px;color:#374151'>${invoice.billing?.address ?: ""}</div><div style='font-size:13px;color:#374151'>${invoice.billing?.phone ?: ""}</div></div></div>
          <table><thead><tr><th>Description</th><th>Qty</th><th>Rate</th><th style='text-align:right'>Total</th></tr></thead><tbody>${buildItemsRows(invoice)}</tbody></table>
          <div class='clearfix'><div class='totals'><div class='t-row'><span>Subtotal</span><span>₱${String.format("%.2f", subtotal)}</span></div><div class='t-row'><span>Tax (${invoice.tax}%)</span><span>₱${String.format("%.2f", taxAmount)}</span></div><div class='grand'><span>Total Due</span><span>₱${String.format("%.2f", total)}</span></div></div></div>
          ${if (!invoice.notes.isNullOrEmpty()) "<div class='notes'><div class='notes-label'>Notes</div><p style='font-size:13px;color:#374151'>${invoice.notes}</p></div>" else ""}
        </div></body></html>""".trimIndent()
    }

    private fun generateTemplate7Html(invoice: Invoice): String {
        val (subtotal, taxAmount, total) = calcTotals(invoice)
        return """<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1.0'>
        <style>body{font-family:Georgia,'Times New Roman',serif;padding:20px;font-size:14px;color:#1a1a1a}
        .top-bar{background:#7f1d1d;height:6px;margin:-20px -20px 20px -20px}
        .title{color:#7f1d1d;font-size:32px;font-weight:bold;letter-spacing:3px;margin-bottom:4px}
        .from-box{border-left:3px solid #7f1d1d;padding-left:12px;margin-bottom:20px}
        .label{color:#7f1d1d;font-size:11px;text-transform:uppercase;letter-spacing:1px;font-weight:bold;margin-bottom:4px}
        table{width:100%;border-collapse:collapse;margin:20px 0}
        th{background:#7f1d1d;color:#fff;padding:10px;text-align:left;font-size:13px}td{padding:10px;border-bottom:1px solid #e5e7eb;font-size:13px}
        tr:nth-child(even){background:#fef2f2}
        .total-section{text-align:right;margin-top:8px}
        .grand-total{color:#7f1d1d;font-size:18px;font-weight:bold}
        .bottom-bar{background:#7f1d1d;height:4px;margin:20px -20px -20px -20px}</style></head><body>
        <div class='top-bar'></div>
        <div style='display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:20px'>
          <div><div class='title'>INVOICE</div><div style='font-size:13px;color:#4b5563'>#${invoice.invoice?.number ?: ""} &nbsp;|&nbsp; ${invoice.invoice?.date ?: ""}</div></div>
          <div class='from-box'><div class='label'>From</div><div style='font-weight:bold'>${invoice.company?.name ?: ""}</div><div style='font-size:13px'>${invoice.company?.address ?: ""}</div><div style='font-size:13px'>${invoice.company?.phone ?: ""}</div></div>
        </div>
        <div style='display:flex;justify-content:space-between;margin-bottom:8px'>
          <div><div class='label'>Bill To</div><div style='font-weight:bold'>${invoice.billing?.name ?: ""}</div><div style='font-size:13px'>${invoice.billing?.address ?: ""}</div><div style='font-size:13px'>${invoice.billing?.phone ?: ""}</div></div>
          <div style='text-align:right'><div class='label'>Due Date</div><div style='font-weight:bold;color:#7f1d1d'>${invoice.invoice?.dueDate ?: ""}</div></div>
        </div>
        <table><thead><tr><th>Item</th><th>Qty</th><th>Rate</th><th>Amount</th></tr></thead><tbody>${buildItemsRows(invoice)}</tbody></table>
        <div class='total-section'><div style='font-size:13px'>Subtotal: <strong>₱${String.format("%.2f", subtotal)}</strong></div><div style='font-size:13px'>Tax (${invoice.tax}%): <strong>₱${String.format("%.2f", taxAmount)}</strong></div><div class='grand-total'>Total Due: ₱${String.format("%.2f", total)}</div></div>
        ${if (!invoice.notes.isNullOrEmpty()) "<div style='margin-top:20px;border-top:1px solid #e5e7eb;padding-top:12px'><div class='label'>Notes</div><p style='font-size:13px'>${invoice.notes}</p></div>" else ""}
        <div class='bottom-bar'></div>
        </body></html>""".trimIndent()
    }

    private fun generateTemplate8Html(invoice: Invoice): String {
        val (subtotal, taxAmount, total) = calcTotals(invoice)
        return """<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1.0'>
        <style>body{font-family:'Segoe UI',Arial,sans-serif;padding:20px;font-size:14px;background:#f0fdfa;color:#134e4a}
        .card{background:#fff;border-radius:12px;padding:20px;box-shadow:0 1px 4px rgba(0,0,0,.08)}
        .header{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:20px}
        .inv-title{font-size:28px;font-weight:800;color:#0f766e;letter-spacing:1px}
        .teal-label{color:#0f766e;font-weight:700;font-size:12px;text-transform:uppercase;letter-spacing:1px;margin-bottom:4px}
        .info-chip{background:#f0fdfa;border:1px solid #99f6e4;border-radius:6px;padding:8px 12px;font-size:13px;margin-bottom:8px}
        table{width:100%;border-collapse:collapse;margin:16px 0}
        th{background:#0f766e;color:#fff;padding:10px;text-align:left;font-size:13px;border-radius:0}
        td{padding:10px;border-bottom:1px solid #ccfbf1;font-size:13px}
        .totals-box{background:#f0fdfa;border:2px solid #5eead4;border-radius:10px;padding:16px;float:right;min-width:210px}
        .t-line{display:flex;justify-content:space-between;margin-bottom:6px;font-size:13px}
        .grand{font-size:16px;font-weight:800;color:#0f766e;border-top:2px solid #0f766e;padding-top:8px;margin-top:6px;display:flex;justify-content:space-between}
        .clearfix::after{content:'';display:table;clear:both}</style></head><body>
        <div class='card'>
          <div class='header'><div><div class='inv-title'>Invoice</div><div style='font-size:13px;color:#64748b'>#${invoice.invoice?.number ?: ""} &nbsp;·&nbsp; ${invoice.invoice?.date ?: ""} &nbsp;·&nbsp; Due ${invoice.invoice?.dueDate ?: ""}</div></div>
          <div style='text-align:right'><div class='teal-label'>From</div><div style='font-weight:600'>${invoice.company?.name ?: ""}</div><div style='font-size:12px;color:#64748b'>${invoice.company?.address ?: ""}</div><div style='font-size:12px;color:#64748b'>${invoice.company?.phone ?: ""}</div></div></div>
          <div class='info-chip'><div class='teal-label'>Bill To</div><div style='font-weight:600'>${invoice.billing?.name ?: ""}</div><div style='font-size:12px'>${invoice.billing?.address ?: ""} &nbsp; ${invoice.billing?.phone ?: ""}</div></div>
          <table><thead><tr><th>Description</th><th>Qty</th><th>Rate</th><th>Total</th></tr></thead><tbody>${buildItemsRows(invoice)}</tbody></table>
          <div class='clearfix'><div class='totals-box'><div class='t-line'><span>Subtotal</span><span>₱${String.format("%.2f", subtotal)}</span></div><div class='t-line'><span>Tax (${invoice.tax}%)</span><span>₱${String.format("%.2f", taxAmount)}</span></div><div class='grand'><span>Total</span><span>₱${String.format("%.2f", total)}</span></div></div></div>
          ${if (!invoice.notes.isNullOrEmpty()) "<div style='margin-top:80px;border-top:2px solid #ccfbf1;padding-top:12px'><div class='teal-label'>Notes</div><p style='font-size:13px'>${invoice.notes}</p></div>" else ""}
        </div></body></html>""".trimIndent()
    }

    private fun generateTemplate9Html(invoice: Invoice): String {
        val (subtotal, taxAmount, total) = calcTotals(invoice)
        return """<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1.0'>
        <style>body{font-family:'Helvetica Neue',Arial,sans-serif;padding:28px;font-size:13px;color:#111;background:#fff}
        .top{display:flex;justify-content:space-between;align-items:flex-end;border-bottom:3px solid #111;padding-bottom:16px;margin-bottom:20px}
        .co-name{font-size:22px;font-weight:900;letter-spacing:-0.5px}
        .inv-word{font-size:40px;font-weight:900;letter-spacing:4px;color:#111;text-transform:uppercase}
        .inv-num{font-size:13px;color:#555;text-align:right}
        .meta-row{display:flex;justify-content:space-between;margin-bottom:20px}
        .meta-block .lbl{font-size:10px;text-transform:uppercase;letter-spacing:1.5px;color:#888;margin-bottom:3px}
        .meta-block .val{font-size:14px;font-weight:600}
        table{width:100%;border-collapse:collapse;margin:16px 0}
        th{font-size:10px;text-transform:uppercase;letter-spacing:1px;padding:8px;border-bottom:2px solid #111;text-align:left}
        td{padding:10px 8px;border-bottom:1px solid #e5e7eb;font-size:13px}
        .totals{margin-top:8px;border-top:3px solid #111;padding-top:12px;text-align:right}
        .tot-line{font-size:13px;margin-bottom:4px}.grand{font-size:20px;font-weight:900;margin-top:4px}</style></head><body>
        <div class='top'>
          <div><div class='co-name'>${invoice.company?.name ?: ""}</div><div style='font-size:12px;color:#555;margin-top:2px'>${invoice.company?.address ?: ""} &nbsp; ${invoice.company?.phone ?: ""}</div></div>
          <div style='text-align:right'><div class='inv-word'>Invoice</div><div class='inv-num'>#${invoice.invoice?.number ?: ""}</div></div>
        </div>
        <div class='meta-row'>
          <div class='meta-block'><div class='lbl'>Bill To</div><div class='val'>${invoice.billing?.name ?: ""}</div><div style='font-size:12px;color:#555'>${invoice.billing?.address ?: ""}</div><div style='font-size:12px;color:#555'>${invoice.billing?.phone ?: ""}</div></div>
          <div class='meta-block' style='text-align:right'><div class='lbl'>Invoice Date</div><div class='val'>${invoice.invoice?.date ?: ""}</div><div class='lbl' style='margin-top:8px'>Due Date</div><div class='val'>${invoice.invoice?.dueDate ?: ""}</div></div>
        </div>
        <table><thead><tr><th>Item</th><th>Qty</th><th>Rate</th><th style='text-align:right'>Amount</th></tr></thead><tbody>${buildItemsRows(invoice)}</tbody></table>
        <div class='totals'><div class='tot-line'>Subtotal: ₱${String.format("%.2f", subtotal)}</div><div class='tot-line'>Tax (${invoice.tax}%): ₱${String.format("%.2f", taxAmount)}</div><div class='grand'>₱${String.format("%.2f", total)}</div><div style='font-size:10px;color:#888;text-transform:uppercase;letter-spacing:1px'>Amount Due</div></div>
        ${if (!invoice.notes.isNullOrEmpty()) "<div style='margin-top:32px;border-top:1px solid #e5e7eb;padding-top:12px'><div style='font-size:10px;text-transform:uppercase;letter-spacing:1.5px;color:#888;margin-bottom:4px'>Notes</div><p style='font-size:13px'>${invoice.notes}</p></div>" else ""}
        </body></html>""".trimIndent()
    }

    private fun generateTemplate10Html(invoice: Invoice): String {
        val (subtotal, taxAmount, total) = calcTotals(invoice)
        return """<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1.0'>
        <style>body{font-family:'Segoe UI',Arial,sans-serif;margin:0;padding:0;background:#fff}
        .gradient-header{background:linear-gradient(135deg,#4f46e5 0%,#7c3aed 50%,#2563eb 100%);padding:28px 24px;color:#fff}
        .header-inner{display:flex;justify-content:space-between;align-items:center}
        .co-name{font-size:22px;font-weight:800;margin-bottom:2px}.co-sub{font-size:12px;opacity:.75;line-height:1.6}
        .inv-badge{background:rgba(255,255,255,.15);backdrop-filter:blur(4px);border:1px solid rgba(255,255,255,.3);border-radius:10px;padding:12px 16px;text-align:right}
        .inv-word{font-size:24px;font-weight:800;letter-spacing:3px}.inv-detail{font-size:12px;opacity:.85;margin-top:4px;line-height:1.6}
        .body{padding:20px}
        .bill-card{background:#f5f3ff;border-left:4px solid #6d28d9;border-radius:6px;padding:12px 16px;margin-bottom:20px}
        .bill-label{color:#6d28d9;font-weight:700;font-size:11px;text-transform:uppercase;letter-spacing:1px;margin-bottom:4px}
        table{width:100%;border-collapse:collapse;margin-bottom:20px}
        th{background:linear-gradient(90deg,#4f46e5,#7c3aed);color:#fff;padding:10px;font-size:13px;text-align:left}
        td{padding:10px;border-bottom:1px solid #ede9fe;font-size:13px}tr:nth-child(even){background:#faf5ff}
        .totals-wrap{float:right;min-width:230px}
        .totals{background:#f5f3ff;border:2px solid #ddd6fe;border-radius:10px;padding:16px}
        .t-row{display:flex;justify-content:space-between;margin-bottom:6px;font-size:13px}
        .grand{font-size:16px;font-weight:800;color:#4f46e5;border-top:2px solid #4f46e5;padding-top:8px;margin-top:6px;display:flex;justify-content:space-between}
        .clearfix::after{content:'';display:table;clear:both}
        .pro-badge{display:inline-block;background:linear-gradient(90deg,#f59e0b,#fbbf24);color:#fff;font-size:10px;font-weight:800;padding:2px 8px;border-radius:10px;letter-spacing:1px;margin-left:8px;vertical-align:middle}</style></head><body>
        <div class='gradient-header'><div class='header-inner'>
          <div><div class='co-name'>${invoice.company?.name ?: ""} <span class='pro-badge'>PRO</span></div><div class='co-sub'>${invoice.company?.address ?: ""}<br>${invoice.company?.phone ?: ""}</div></div>
          <div class='inv-badge'><div class='inv-word'>INVOICE</div><div class='inv-detail'>#${invoice.invoice?.number ?: ""}<br>Date: ${invoice.invoice?.date ?: ""}<br>Due: ${invoice.invoice?.dueDate ?: ""}</div></div>
        </div></div>
        <div class='body'>
          <div class='bill-card'><div class='bill-label'>Bill To</div><div style='font-weight:600'>${invoice.billing?.name ?: ""}</div><div style='font-size:13px;color:#4b5563'>${invoice.billing?.address ?: ""} &nbsp; ${invoice.billing?.phone ?: ""}</div></div>
          <table><thead><tr><th>Description</th><th>Qty</th><th>Rate</th><th>Amount</th></tr></thead><tbody>${buildItemsRows(invoice)}</tbody></table>
          <div class='clearfix'><div class='totals-wrap'><div class='totals'><div class='t-row'><span>Subtotal</span><span>₱${String.format("%.2f", subtotal)}</span></div><div class='t-row'><span>Tax (${invoice.tax}%)</span><span>₱${String.format("%.2f", taxAmount)}</span></div><div class='grand'><span>Total Due</span><span>₱${String.format("%.2f", total)}</span></div></div></div></div>
          ${if (!invoice.notes.isNullOrEmpty()) "<div style='margin-top:80px;border-top:2px solid #ede9fe;padding-top:12px'><div class='bill-label'>Notes</div><p style='font-size:13px'>${invoice.notes}</p></div>" else ""}
        </div></body></html>""".trimIndent()
    }

    // ─────────────────────────────────────────────────────────────────
    // Save / Delete
    // ─────────────────────────────────────────────────────────────────

    private fun saveInvoice() {
        val updatedInvoice = invoice.copy(template = selectedTemplate)
        binding.btnSave.isEnabled = false

        val loadingDialog = AlertDialog.Builder(this)
            .setMessage("Saving invoice...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        lifecycleScope.launch {
            try {
                val token = SessionManager.getToken(this@InvoicePreviewActivity)
                if (token != null) RetrofitClient.setToken(token)

                val response = if (!updatedInvoice.id.isNullOrEmpty()) {
                    RetrofitClient.api.updateInvoice(updatedInvoice.id, updatedInvoice)
                } else {
                    RetrofitClient.api.saveInvoice(updatedInvoice)
                }

                loadingDialog.dismiss()
                if (response.isSuccessful) {
                    Toast.makeText(this@InvoicePreviewActivity, "Invoice saved! ✅", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@InvoicePreviewActivity, "Failed to save", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                loadingDialog.dismiss()
                Toast.makeText(this@InvoicePreviewActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this@InvoicePreviewActivity, "Invoice deleted ✅", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@InvoicePreviewActivity, "Failed to delete", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@InvoicePreviewActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

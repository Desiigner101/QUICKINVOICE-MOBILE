package com.sarsonasgino.quickinvoicemobile.features.invoice

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sarsonasgino.quickinvoicemobile.R
import com.sarsonasgino.quickinvoicemobile.core.model.*
import com.sarsonasgino.quickinvoicemobile.core.utils.SessionManager
import com.sarsonasgino.quickinvoicemobile.databinding.ActivityCreateInvoiceBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CreateInvoiceActivity : AppCompatActivity(), CreateInvoiceContract.View {

    private lateinit var binding: ActivityCreateInvoiceBinding
    private lateinit var presenter: CreateInvoicePresenter
    private val itemViews = mutableListOf<View>()
    private var selectedTemplate = "template1"
    private val templateButtons = mutableListOf<Button>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateInvoiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        presenter = CreateInvoicePresenter(this, this)

        setupInvoiceNumber()
        setupInvoiceDate()
        addItemRow()
        setupListeners()
        setupTemplateSelector()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }

    // --- CreateInvoiceContract.View implementations ---

    override fun showSaveSuccess() {
        Toast.makeText(this, "Invoice saved successfully! ✅", Toast.LENGTH_SHORT).show()
    }

    override fun showSaveError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun setSaveButtonsEnabled(enabled: Boolean) {
        binding.btnSave.isEnabled = enabled
        binding.btnSaveBottom.isEnabled = enabled
    }

    override fun closeScreen() {
        finish()
    }

    // --- Private UI helpers ---

    private fun setupInvoiceNumber() {
        val invoiceNumber = "INV-${(100000 + Math.random() * 900000).toInt()}"
        binding.etInvoiceNumber.setText(invoiceNumber)
    }

    private fun setupInvoiceDate() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        binding.etInvoiceDate.setText(dateFormat.format(Date()))
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { submitInvoice() }
        binding.btnSaveBottom.setOnClickListener { submitInvoice() }
        binding.btnAddItem.setOnClickListener { addItemRow() }

        binding.cbSameAsBilling.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.etShippingName.setText(binding.etBillingName.text)
                binding.etShippingPhone.setText(binding.etBillingPhone.text)
                binding.etShippingAddress.setText(binding.etBillingAddress.text)
            }
        }

        binding.etTaxRate.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { updateTotals() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun addItemRow() {
        val itemView = LayoutInflater.from(this)
            .inflate(R.layout.item_invoice_row, binding.itemsContainer, false)

        val etQty = itemView.findViewById<EditText>(R.id.etItemQty)
        val etAmount = itemView.findViewById<EditText>(R.id.etItemAmount)
        val tvTotal = itemView.findViewById<TextView>(R.id.tvItemTotal)
        val btnDelete = itemView.findViewById<ImageButton>(R.id.btnDeleteItem)

        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val qty = etQty.text.toString().toDoubleOrNull() ?: 0.0
                val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
                tvTotal.text = "₱${String.format("%.2f", qty * amount)}"
                updateTotals()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        etQty.addTextChangedListener(watcher)
        etAmount.addTextChangedListener(watcher)

        btnDelete.setOnClickListener {
            if (itemViews.size > 1) {
                binding.itemsContainer.removeView(itemView)
                itemViews.remove(itemView)
                updateTotals()
            } else {
                Toast.makeText(this, "At least one item is required", Toast.LENGTH_SHORT).show()
            }
        }

        binding.itemsContainer.addView(itemView)
        itemViews.add(itemView)
    }

    private fun updateTotals() {
        var subtotal = 0.0
        for (itemView in itemViews) {
            val qty = itemView.findViewById<EditText>(R.id.etItemQty).text.toString().toDoubleOrNull() ?: 0.0
            val amount = itemView.findViewById<EditText>(R.id.etItemAmount).text.toString().toDoubleOrNull() ?: 0.0
            subtotal += qty * amount
        }
        val taxRate = binding.etTaxRate.text.toString().toDoubleOrNull() ?: 0.0
        val taxAmount = subtotal * taxRate / 100
        val grandTotal = subtotal + taxAmount

        binding.tvSubtotal.text = "₱${String.format("%.2f", subtotal)}"
        binding.tvTaxAmount.text = "₱${String.format("%.2f", taxAmount)}"
        binding.tvGrandTotal.text = "₱${String.format("%.2f", grandTotal)}"
    }

    private fun setupTemplateSelector() {
        templateButtons.addAll(listOf(
            binding.btnTemplate1, binding.btnTemplate2, binding.btnTemplate3,
            binding.btnTemplate4, binding.btnTemplate5
        ))
        binding.btnTemplate1.setOnClickListener { selectTemplate("template1", binding.btnTemplate1) }
        binding.btnTemplate2.setOnClickListener { selectTemplate("template2", binding.btnTemplate2) }
        binding.btnTemplate3.setOnClickListener { selectTemplate("template3", binding.btnTemplate3) }
        binding.btnTemplate4.setOnClickListener { selectTemplate("template4", binding.btnTemplate4) }
        binding.btnTemplate5.setOnClickListener { selectTemplate("template5", binding.btnTemplate5) }
    }

    private fun selectTemplate(template: String, button: Button) {
        selectedTemplate = template
        val colors = mapOf(
            "template1" to "#ea580c", "template2" to "#198754",
            "template3" to "#8a3ff3", "template4" to "#00a9e0", "template5" to "#1f2937"
        )
        templateButtons.forEach { btn ->
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#E5E7EB"))
            btn.setTextColor(android.graphics.Color.parseColor("#374151"))
        }
        button.backgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor(colors[template] ?: "#0D6EFD"))
        button.setTextColor(android.graphics.Color.WHITE)
    }

    private fun submitInvoice() {
        val title = binding.etTitle.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter an invoice title", Toast.LENGTH_SHORT).show()
            return
        }

        val items = mutableListOf<Item>()
        for (itemView in itemViews) {
            val name = itemView.findViewById<EditText>(R.id.etItemName).text.toString().trim()
            val qty = itemView.findViewById<EditText>(R.id.etItemQty).text.toString().toIntOrNull() ?: 0
            val amount = itemView.findViewById<EditText>(R.id.etItemAmount).text.toString().toDoubleOrNull() ?: 0.0
            val description = itemView.findViewById<EditText>(R.id.etItemDescription).text.toString().trim()
            items.add(Item(name = name, qty = qty, amount = amount, description = description))
        }

        val clerkId = SessionManager.getClerkId(this) ?: return

        val invoice = Invoice(
            clerkId = clerkId,
            title = title,
            company = Company(
                name = binding.etCompanyName.text.toString().trim(),
                phone = binding.etCompanyPhone.text.toString().trim(),
                address = binding.etCompanyAddress.text.toString().trim()
            ),
            billing = Billing(
                name = binding.etBillingName.text.toString().trim(),
                phone = binding.etBillingPhone.text.toString().trim(),
                address = binding.etBillingAddress.text.toString().trim()
            ),
            shipping = Shipping(
                name = binding.etShippingName.text.toString().trim(),
                phone = binding.etShippingPhone.text.toString().trim(),
                address = binding.etShippingAddress.text.toString().trim()
            ),
            invoice = InvoiceDetails(
                number = binding.etInvoiceNumber.text.toString().trim(),
                date = binding.etInvoiceDate.text.toString().trim(),
                dueDate = binding.etInvoiceDueDate.text.toString().trim()
            ),
            items = items,
            tax = binding.etTaxRate.text.toString().toDoubleOrNull() ?: 0.0,
            notes = binding.etNotes.text.toString().trim(),
            template = selectedTemplate
        )

        presenter.saveInvoice(invoice)
    }
}
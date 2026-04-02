package com.sarsonasgino.quickinvoicemobile.features.invoice

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sarsonasgino.quickinvoicemobile.R
import com.sarsonasgino.quickinvoicemobile.core.model.Billing
import com.sarsonasgino.quickinvoicemobile.core.model.Company
import com.sarsonasgino.quickinvoicemobile.core.model.Invoice
import com.sarsonasgino.quickinvoicemobile.core.model.InvoiceDetails
import com.sarsonasgino.quickinvoicemobile.core.model.Item
import com.sarsonasgino.quickinvoicemobile.core.model.Shipping
import com.sarsonasgino.quickinvoicemobile.core.network.RetrofitClient
import com.sarsonasgino.quickinvoicemobile.core.utils.SessionManager
import com.sarsonasgino.quickinvoicemobile.databinding.ActivityCreateInvoiceBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CreateInvoiceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateInvoiceBinding
    private val itemViews = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateInvoiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInvoiceNumber()
        setupInvoiceDate()
        addItemRow()
        setupListeners()
    }

    private fun setupInvoiceNumber() {
        val invoiceNumber = "INV-${(100000 + Math.random() * 900000).toInt()}"
        binding.etInvoiceNumber.setText(invoiceNumber)
    }

    private fun setupInvoiceDate() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        binding.etInvoiceDate.setText(dateFormat.format(Date()))
    }

    private fun setupListeners() {
        // Back button
        binding.btnBack.setOnClickListener { finish() }

        // Save buttons
        binding.btnSave.setOnClickListener { saveInvoice() }
        binding.btnSaveBottom.setOnClickListener { saveInvoice() }

        // Add item button
        binding.btnAddItem.setOnClickListener { addItemRow() }

        // Same as billing checkbox
        binding.cbSameAsBilling.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.etShippingName.setText(binding.etBillingName.text)
                binding.etShippingPhone.setText(binding.etBillingPhone.text)
                binding.etShippingAddress.setText(binding.etBillingAddress.text)
            }
        }

        // Tax rate change
        binding.etTaxRate.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { updateTotals() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun addItemRow() {
        val itemView = LayoutInflater.from(this)
            .inflate(R.layout.item_invoice_row, binding.itemsContainer, false)

        val etName = itemView.findViewById<EditText>(R.id.etItemName)
        val etQty = itemView.findViewById<EditText>(R.id.etItemQty)
        val etAmount = itemView.findViewById<EditText>(R.id.etItemAmount)
        val tvTotal = itemView.findViewById<TextView>(R.id.tvItemTotal)
        val btnDelete = itemView.findViewById<ImageButton>(R.id.btnDeleteItem)

        // Calculate total when qty or amount changes
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

        // Delete item row
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
            val qty = itemView.findViewById<EditText>(R.id.etItemQty)
                .text.toString().toDoubleOrNull() ?: 0.0
            val amount = itemView.findViewById<EditText>(R.id.etItemAmount)
                .text.toString().toDoubleOrNull() ?: 0.0
            subtotal += qty * amount
        }

        val taxRate = binding.etTaxRate.text.toString().toDoubleOrNull() ?: 0.0
        val taxAmount = subtotal * taxRate / 100
        val grandTotal = subtotal + taxAmount

        binding.tvSubtotal.text = "₱${String.format("%.2f", subtotal)}"
        binding.tvTaxAmount.text = "₱${String.format("%.2f", taxAmount)}"
        binding.tvGrandTotal.text = "₱${String.format("%.2f", grandTotal)}"
    }

    private fun saveInvoice() {
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
        val taxRate = binding.etTaxRate.text.toString().toDoubleOrNull() ?: 0.0

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
            tax = taxRate,
            notes = binding.etNotes.text.toString().trim(),
            template = "template1"
        )

        binding.btnSave.isEnabled = false
        binding.btnSaveBottom.isEnabled = false

        lifecycleScope.launch {
            try {
                val token = SessionManager.getToken(this@CreateInvoiceActivity)
                if (token != null) RetrofitClient.setToken(token)

                val response = RetrofitClient.api.saveInvoice(invoice)

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@CreateInvoiceActivity,
                        "Invoice saved successfully! ✅",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@CreateInvoiceActivity,
                        "Failed to save invoice",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@CreateInvoiceActivity,
                    "Network error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.btnSave.isEnabled = true
                binding.btnSaveBottom.isEnabled = true
            }
        }
    }
}
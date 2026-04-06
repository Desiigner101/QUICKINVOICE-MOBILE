package com.sarsonasgino.quickinvoicemobile.features.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sarsonasgino.quickinvoicemobile.core.model.Invoice
import com.sarsonasgino.quickinvoicemobile.databinding.ItemInvoiceBinding // Ensure this is generated
import java.text.SimpleDateFormat
import java.util.Locale

class InvoiceAdapter(
    private var invoices: MutableList<Invoice> = mutableListOf(),
    private val onInvoiceClick: (Invoice) -> Unit
) : RecyclerView.Adapter<InvoiceAdapter.InvoiceViewHolder>() {

    // 1. Use ViewBinding in the ViewHolder
    class InvoiceViewHolder(val binding: ItemInvoiceBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvoiceViewHolder {
        val binding = ItemInvoiceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return InvoiceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InvoiceViewHolder, position: Int) {
        val invoice = invoices[position]

        with(holder.binding) {
            tvTitle.text = invoice.title ?: "Untitled Invoice"
            tvDate.text = "Last updated: ${formatDate(invoice.lastUpdatedAt)}"

            // Handle Image Visibility
            if (!invoice.thumbnailUrl.isNullOrEmpty()) {
                ivThumbnail.visibility = View.VISIBLE
                layoutNoThumbnail.visibility = View.GONE
                Glide.with(root.context)
                    .load(invoice.thumbnailUrl)
                    .centerCrop()
                    .into(ivThumbnail)
            } else {
                ivThumbnail.visibility = View.GONE
                layoutNoThumbnail.visibility = View.VISIBLE
            }

            root.setOnClickListener { onInvoiceClick(invoice) }
        }
    }

    override fun getItemCount() = invoices.size

    // 2. Use DiffUtil for high-performance updates
    fun updateInvoices(newInvoices: List<Invoice>) {
        val diffCallback = InvoiceDiffCallback(this.invoices, newInvoices)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.invoices.clear()
        this.invoices.addAll(newInvoices)
        diffResult.dispatchUpdatesTo(this)
    }

    private fun formatDate(dateStr: String?): String {
        if (dateStr == null) return "--"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            inputFormat.parse(dateStr)?.let { outputFormat.format(it) } ?: dateStr
        } catch (e: Exception) {
            dateStr
        }
    }
}

// 3. Helper class for DiffUtil
class InvoiceDiffCallback(
    private val oldList: List<Invoice>,
    private val newList: List<Invoice>
) : DiffUtil.Callback() {
    override fun getOldListSize() = oldList.size
    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
        return oldList[oldPos].id == newList[newPos].id
    }

    override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
        return oldList[oldPos] == newList[newPos]
    }
}
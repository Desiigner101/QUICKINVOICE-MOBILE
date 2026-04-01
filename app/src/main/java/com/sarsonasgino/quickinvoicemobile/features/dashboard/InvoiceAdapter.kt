package com.sarsonasgino.quickinvoicemobile.features.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sarsonasgino.quickinvoicemobile.R
import com.sarsonasgino.quickinvoicemobile.core.model.Invoice
import java.text.SimpleDateFormat
import java.util.Locale

class InvoiceAdapter(
    private val invoices: MutableList<Invoice>,
    private val onInvoiceClick: (Invoice) -> Unit
) : RecyclerView.Adapter<InvoiceAdapter.InvoiceViewHolder>() {

    class InvoiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        val layoutNoThumbnail: LinearLayout = itemView.findViewById(R.id.layoutNoThumbnail)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvoiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_invoice, parent, false)
        return InvoiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: InvoiceViewHolder, position: Int) {
        val invoice = invoices[position]

        holder.tvTitle.text = invoice.title ?: "Untitled Invoice"
        holder.tvDate.text = "Last updated: ${formatDate(invoice.lastUpdatedAt)}"

        if (!invoice.thumbnailUrl.isNullOrEmpty()) {
            holder.ivThumbnail.visibility = View.VISIBLE
            holder.layoutNoThumbnail.visibility = View.GONE
            Glide.with(holder.itemView.context)
                .load(invoice.thumbnailUrl)
                .centerCrop()
                .into(holder.ivThumbnail)
        } else {
            holder.ivThumbnail.visibility = View.GONE
            holder.layoutNoThumbnail.visibility = View.VISIBLE
        }

        holder.itemView.setOnClickListener {
            onInvoiceClick(invoice)
        }
    }

    override fun getItemCount() = invoices.size

    fun updateInvoices(newInvoices: List<Invoice>) {
        invoices.clear()
        invoices.addAll(newInvoices)
        notifyDataSetChanged()
    }

    private fun formatDate(dateStr: String?): String {
        if (dateStr == null) return "--"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            dateStr
        }
    }
}
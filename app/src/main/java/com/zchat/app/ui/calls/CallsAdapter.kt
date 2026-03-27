package com.zchat.app.ui.calls

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zchat.app.R
import com.zchat.app.data.model.Call
import com.zchat.app.databinding.ItemCallBinding
import java.text.SimpleDateFormat
import java.util.*

class CallsAdapter(private val onCallClick: (Call) -> Unit) :
    ListAdapter<Call, CallsAdapter.CallViewHolder>(CallDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallViewHolder {
        val binding = ItemCallBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CallViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CallViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CallViewHolder(private val binding: ItemCallBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(call: Call) {
            val context = binding.root.context

            // Caller name
            binding.tvName.text = call.callerName.ifEmpty { context.getString(R.string.unknown) }

            // Avatar first letter
            val firstLetter = call.callerName.takeIf { it.isNotEmpty() }?.first()?.uppercase() ?: "?"
            binding.tvAvatar.text = firstLetter

            // Call type icon
            when (call.type) {
                "VIDEO" -> binding.ivCallType.setImageResource(R.drawable.ic_video)
                else -> binding.ivCallType.setImageResource(R.drawable.ic_phone)
            }

            // Call status
            val statusText = when (call.status) {
                "INCOMING" -> context.getString(R.string.incoming_call)
                "OUTGOING" -> context.getString(R.string.outgoing_call)
                "MISSED" -> context.getString(R.string.missed_call)
                else -> context.getString(R.string.call_ended)
            }

            // Time in Minsk timezone (UTC+3)
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("Europe/Minsk")
            val timeStr = sdf.format(Date(call.timestamp))

            binding.tvStatus.text = "$statusText • $timeStr"

            // Duration
            if (call.duration > 0) {
                val minutes = call.duration / 60
                val seconds = call.duration % 60
                binding.tvDuration.text = String.format("%02d:%02d", minutes, seconds)
                binding.tvDuration.visibility = android.view.View.VISIBLE
            } else {
                binding.tvDuration.visibility = android.view.View.GONE
            }

            // Color for missed calls
            if (call.status == "MISSED") {
                binding.tvStatus.setTextColor(context.getColor(android.R.color.holo_red_dark))
            } else {
                binding.tvStatus.setTextColor(context.getColor(android.R.color.darker_gray))
            }

            binding.root.setOnClickListener { onCallClick(call) }
        }
    }

    class CallDiffCallback : DiffUtil.ItemCallback<Call>() {
        override fun areItemsTheSame(oldItem: Call, newItem: Call) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Call, newItem: Call) = oldItem == newItem
    }
}

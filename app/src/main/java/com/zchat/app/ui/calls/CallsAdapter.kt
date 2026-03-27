package com.zchat.app.ui.calls

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zchat.app.R
import com.zchat.app.data.model.Call
import com.zchat.app.databinding.ItemCallBinding
import com.zchat.app.ui.theme.ThemeManager
import java.text.SimpleDateFormat
import java.util.*

class CallsAdapter(
    private val currentUserId: String
) : ListAdapter<Call, CallsAdapter.CallViewHolder>(DiffCallback()) {
    
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallViewHolder {
        val binding = ItemCallBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CallViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: CallViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class CallViewHolder(private val binding: ItemCallBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(call: Call) {
            val isIncoming = call.isIncoming(currentUserId)
            val isMissed = call.isMissed()
            
            // Имя собеседника
            val name = if (isIncoming) call.callerName else call.receiverName
            binding.tvName.text = name
            
            // Тип звонка
            val typeText = if (call.type == "VIDEO") "📹" else "📞"
            
            // Статус и направление
            val statusIcon = when {
                isMissed -> "❌"
                isIncoming -> "📥"
                else -> "📤"
            }
            
            // Время звонка (по минскому времени UTC+3)
            val minskTime = call.timestamp + (3 * 60 * 60 * 1000)
            val date = Date(minskTime)
            val now = Date()
            val isToday = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(date) == 
                          SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(now)
            
            val timeText = if (isToday) {
                timeFormat.format(date)
            } else {
                "${dateFormat.format(date)}, ${timeFormat.format(date)}"
            }
            
            // Длительность
            val durationText = if (call.duration > 0) {
                val minutes = call.duration / 60
                val seconds = call.duration % 60
                "($minutes:${String.format("%02d", seconds)})"
            } else {
                ""
            }
            
            binding.tvTime.text = "$statusIcon $timeText $durationText"
            
            // Цвет для пропущенных
            val colors = ThemeManager.getColors()
            if (isMissed) {
                binding.tvName.setTextColor("#EF4444".toColorInt())
                binding.tvTime.setTextColor("#EF4444".toColorInt())
            } else {
                binding.tvName.setTextColor(colors.textPrimary.toColorInt())
                binding.tvTime.setTextColor(colors.textSecondary.toColorInt())
            }
            
            // Тип звонка
            binding.tvCallType.text = typeText
            
            // Аватар
            val avatarBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(colors.primary.toColorInt())
            }
            binding.ivAvatar.background = avatarBg
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<Call>() {
        override fun areItemsTheSame(oldItem: Call, newItem: Call) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Call, newItem: Call) = oldItem == newItem
    }
}

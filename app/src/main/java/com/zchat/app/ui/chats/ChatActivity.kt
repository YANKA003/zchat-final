package com.zchat.app.ui.chats

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.MenuItem
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.zchat.app.R
import com.zchat.app.data.Repository
import com.zchat.app.databinding.ActivityChatBinding
import com.zchat.app.ui.theme.ThemeManager
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var repository: Repository
    private lateinit var adapter: MessagesAdapter
    private var userId: String? = null
    private var username: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.init(applicationContext)
        applyThemeColors()
        
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = Repository(applicationContext)
        userId = intent.getStringExtra("userId")
        username = intent.getStringExtra("username")
        
        binding.tvTitle.text = username ?: "Чат"
        binding.btnBack.setOnClickListener { saveDraftAndFinish() }
        
        adapter = MessagesAdapter(
            currentUserId = repository.currentUser?.uid ?: "",
            onMessageLongClick = { message, view -> showMessageOptions(message, view) }
        )
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.rvMessages.adapter = adapter
        
        binding.btnCall.setOnClickListener { Toast.makeText(this, "Голосовой звонок", Toast.LENGTH_SHORT).show() }
        binding.btnVideoCall.setOnClickListener { Toast.makeText(this, "Видеозвонок", Toast.LENGTH_SHORT).show() }
        
        val currentUserId = repository.currentUser?.uid ?: return
        val otherUserId = userId ?: return
        
        // Load draft if exists
        loadDraft()
        
        lifecycleScope.launch {
            repository.observeMessages(currentUserId, otherUserId).collect { messages ->
                adapter.submitList(messages) { binding.rvMessages.scrollToPosition(messages.size - 1) }
            }
        }
        
        binding.btnSend.setOnClickListener {
            val content = binding.etMessage.text.toString().trim()
            if (content.isNotEmpty()) {
                lifecycleScope.launch { repository.sendMessage(content, userId ?: return@launch) }
                binding.etMessage.text?.clear()
                // Clear draft after sending
                repository.preferencesManager.clearDraft(otherUserId)
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Save draft when leaving the chat
        saveDraft()
    }
    
    override fun onBackPressed() {
        saveDraftAndFinish()
    }
    
    private fun loadDraft() {
        userId?.let { chatId ->
            val draft = repository.preferencesManager.getDraft(chatId)
            if (draft.isNotEmpty()) {
                binding.etMessage.setText(draft)
                binding.etMessage.setSelection(draft.length)
            }
        }
    }
    
    private fun saveDraft() {
        userId?.let { chatId ->
            val draft = binding.etMessage.text.toString().trim()
            if (draft.isNotEmpty()) {
                repository.preferencesManager.saveDraft(chatId, draft)
            } else {
                repository.preferencesManager.clearDraft(chatId)
            }
        }
    }
    
    private fun saveDraftAndFinish() {
        saveDraft()
        finish()
    }
    
    override fun onResume() {
        super.onResume()
        applyThemeColors()
    }
    
    private fun applyThemeColors() {
        val colors = ThemeManager.getColors()
        window.statusBarColor = colors.primaryDark.toColorInt()
        
        if (::binding.isInitialized) {
            // Кнопки звонков
            val callBg = GradientDrawable().apply {
                setColor(colors.primary.toColorInt())
                cornerRadius = 24f
            }
            binding.btnCall.background = callBg.constantState?.newDrawable()?.mutate()
            binding.btnVideoCall.background = callBg.constantState?.newDrawable()?.mutate()
            
            // Кнопка отправки
            val sendBg = GradientDrawable().apply {
                setColor(colors.primary.toColorInt())
                cornerRadius = 24f
            }
            binding.btnSend.background = sendBg
            
            // Цвет статус индикатора
            val statusBg = GradientDrawable().apply {
                setColor(colors.onlineIndicator.toColorInt())
                cornerRadius = 5f
            }
            binding.statusIndicator.background = statusBg
            
            // Цвета текста
            binding.tvTitle.setTextColor(colors.textPrimary.toColorInt())
            binding.tvStatus.setTextColor(colors.textSecondary.toColorInt())
            binding.etMessage.setTextColor(colors.textPrimary.toColorInt())
            binding.etMessage.setHintTextColor(colors.textSecondary.toColorInt())
        }
    }
    
    private fun showMessageOptions(message: com.zchat.app.data.model.Message, view: android.view.View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.message_context_menu, popup.menu)
        
        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.action_edit -> editMessage(message)
                R.id.action_delete -> deleteMessage(message)
                R.id.action_copy -> copyMessage(message)
            }
            true
        }
        popup.show()
    }
    
    private fun editMessage(message: com.zchat.app.data.model.Message) {
        val input = android.widget.EditText(this).apply {
            setText(message.content)
            setSelection(message.content.length)
            // Fix text color
            setTextColor(resources.getColor(R.color.text_primary, theme))
            setHintTextColor(resources.getColor(R.color.text_secondary, theme))
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Редактировать сообщение")
            .setView(input)
            .setPositiveButton("Сохранить") { _, _ ->
                val newContent = input.text.toString().trim()
                if (newContent.isNotEmpty() && newContent != message.content) {
                    lifecycleScope.launch {
                        repository.editMessage(
                            message.id,
                            repository.currentUser?.uid ?: "",
                            userId ?: "",
                            newContent
                        )
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun deleteMessage(message: com.zchat.app.data.model.Message) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Удалить сообщение")
            .setMessage("Вы уверены, что хотите удалить это сообщение?")
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch {
                    repository.deleteMessage(
                        message.id,
                        repository.currentUser?.uid ?: "",
                        userId ?: ""
                    )
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun copyMessage(message: com.zchat.app.data.model.Message) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("message", message.content)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Скопировано", Toast.LENGTH_SHORT).show()
    }
}

package com.zchat.app.ui.chats

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.zchat.app.R
import com.zchat.app.data.Repository
import com.zchat.app.data.model.Message
import com.zchat.app.data.model.User
import com.zchat.app.databinding.ActivityChatBinding
import com.zchat.app.ui.calls.CallActivity
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private var repository: Repository? = null
    private lateinit var adapter: MessagesAdapter
    private var otherUserId: String? = null
    private var currentUserId: String? = null
    private var userPhone: String? = null
    private var username: String? = null
    private var selectedMessage: Message? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityChatBinding.inflate(layoutInflater)
            setContentView(binding.root)

            otherUserId = intent.getStringExtra("userId")
            username = intent.getStringExtra("username")
            userPhone = intent.getStringExtra("userPhone")

            binding.tvTitle.text = username

            if (!userPhone.isNullOrEmpty()) {
                binding.tvPhone.text = formatPhoneNumber(userPhone!!)
                binding.tvPhone.visibility = View.VISIBLE
            }

            binding.btnBack.setOnClickListener { finish() }

            // Video call button
            binding.btnVideoCall.setOnClickListener { startCall(isVideo = true) }

            // Voice call button
            binding.btnCall.setOnClickListener { startCall(isVideo = false) }

            repository = Repository(applicationContext)
            currentUserId = repository?.currentUser?.uid

            if (otherUserId == null || currentUserId == null) {
                Toast.makeText(this, "Ошибка: пользователь не найден", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            adapter = MessagesAdapter(currentUserId!!) { message, view ->
                selectedMessage = message
                registerForContextMenu(view)
                openContextMenu(view)
            }
            binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
                stackFromEnd = true
            }
            binding.rvMessages.adapter = adapter

            binding.btnSend.setOnClickListener { sendMessage() }

            loadMessagesRealtime()
            observeUserStatus()
            markMessagesAsRead()
        } catch (e: Exception) {
            Log.e("ChatActivity", "Initialization error", e)
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun formatPhoneNumber(phone: String): String {
        val digits = phone.replace(Regex("[^0-9]"), "")
        return if (digits.length >= 10) {
            val last10 = digits.takeLast(10)
            "+${if (digits.length > 10) digits.dropLast(10) else ""} (${last10.take(3)}) ${last10.drop(3).take(3)}-${last10.drop(6).take(2)}-${last10.drop(8)}"
        } else {
            "+$phone"
        }
    }

    private fun loadMessagesRealtime() {
        val uid = otherUserId ?: return
        val current = currentUserId ?: return

        lifecycleScope.launch {
            try {
                repository?.observeMessages(current, uid)?.collect { messages ->
                    adapter.submitList(messages) {
                        binding.rvMessages.scrollToPosition(messages.size - 1)
                    }
                    binding.tvEmpty.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Load messages error", e)
            }
        }
    }

    private fun observeUserStatus() {
        val uid = otherUserId ?: return

        lifecycleScope.launch {
            try {
                repository?.observeUserStatus(uid)?.collect { user ->
                    updateUserStatusUI(user)
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Observe user status error", e)
            }
        }
    }

    private fun updateUserStatusUI(user: User) {
        if (user.isOnline) {
            binding.tvStatus.text = "онлайн"
            binding.tvStatus.setTextColor(getColor(R.color.primary))
            binding.statusIndicator.setBackgroundResource(R.drawable.status_online)
        } else {
            val lastSeen = user.lastSeen
            if (lastSeen > 0) {
                val timeAgo = DateUtils.getRelativeTimeSpanString(
                    lastSeen,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                )
                binding.tvStatus.text = "был(а) $timeAgo"
            } else {
                binding.tvStatus.text = "не в сети"
            }
            binding.tvStatus.setTextColor(getColor(R.color.text_secondary))
            binding.statusIndicator.setBackgroundResource(R.drawable.status_offline)
        }
    }

    private fun markMessagesAsRead() {
        val uid = otherUserId ?: return
        val current = currentUserId ?: return

        lifecycleScope.launch {
            try {
                repository?.markMessagesAsRead(current, uid)
            } catch (e: Exception) {
                Log.e("ChatActivity", "Mark messages read error", e)
            }
        }
    }

    private fun sendMessage() {
        val content = binding.etMessage.text.toString().trim()
        if (content.isEmpty()) return

        val uid = otherUserId ?: return

        binding.etMessage.text?.clear()
        lifecycleScope.launch {
            try {
                repository?.sendMessage(content, uid)
            } catch (e: Exception) {
                Log.e("ChatActivity", "Send message error", e)
                Toast.makeText(this@ChatActivity, "Ошибка отправки", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCall(isVideo: Boolean) {
        val intent = Intent(this, CallActivity::class.java).apply {
            putExtra("otherUserId", otherUserId)
            putExtra("callerName", username)
            putExtra("isCaller", true)
            putExtra("isVideo", isVideo)
        }
        startActivity(intent)
    }

    // Context menu for message actions
    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (selectedMessage?.senderId == currentUserId) {
            menu.add(0, 1, 0, "Редактировать")
            menu.add(0, 2, 0, "Удалить")
        }
        menu.add(0, 3, 0, "Копировать")
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val message = selectedMessage ?: return super.onContextItemSelected(item)
        val uid = otherUserId ?: return super.onContextItemSelected(item)
        val current = currentUserId ?: return super.onContextItemSelected(item)

        when (item.itemId) {
            1 -> showEditDialog(message, current, uid)
            2 -> showDeleteDialog(message, current, uid)
            3 -> copyMessage(message)
        }
        return super.onContextItemSelected(item)
    }

    private fun showEditDialog(message: Message, currentUserId: String, otherUserId: String) {
        val editText = EditText(this).apply {
            setText(message.content)
            setSelection(message.content.length)
        }

        AlertDialog.Builder(this)
            .setTitle("Редактировать сообщение")
            .setView(editText)
            .setPositiveButton("Сохранить") { _, _ ->
                val newContent = editText.text.toString().trim()
                if (newContent.isNotEmpty()) {
                    lifecycleScope.launch {
                        repository?.editMessage(message.id, currentUserId, otherUserId, newContent)
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showDeleteDialog(message: Message, currentUserId: String, otherUserId: String) {
        AlertDialog.Builder(this)
            .setTitle("Удалить сообщение?")
            .setMessage("Это действие нельзя отменить")
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch {
                    repository?.deleteMessage(message.id, currentUserId, otherUserId)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun copyMessage(message: Message) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("message", message.content)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Скопировано", Toast.LENGTH_SHORT).show()
    }
}

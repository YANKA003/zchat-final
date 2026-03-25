package com.zchat.app.ui.chats

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.zchat.app.data.Repository
import com.zchat.app.databinding.ActivityChatBinding
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private var repository: Repository? = null
    private lateinit var adapter: MessagesAdapter
    private var otherUserId: String? = null
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityChatBinding.inflate(layoutInflater)
            setContentView(binding.root)

            otherUserId = intent.getStringExtra("userId")
            val username = intent.getStringExtra("username") ?: "Чат"

            binding.tvTitle.text = username
            binding.btnBack.setOnClickListener { finish() }

            repository = Repository(applicationContext)
            currentUserId = repository?.currentUser?.uid

            if (otherUserId == null || currentUserId == null) {
                Toast.makeText(this, "Ошибка: пользователь не найден", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            adapter = MessagesAdapter(currentUserId!!)
            binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
                stackFromEnd = true
            }
            binding.rvMessages.adapter = adapter

            binding.btnSend.setOnClickListener { sendMessage() }

            loadMessages()
        } catch (e: Exception) {
            Log.e("ChatActivity", "Initialization error", e)
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadMessages() {
        val uid = otherUserId ?: return
        val current = currentUserId ?: return

        lifecycleScope.launch {
            try {
                repository?.getMessages(uid, current)?.collect { messages ->
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
}

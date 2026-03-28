package com.zchat.app.ui.chats

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.zchat.app.R
import com.zchat.app.data.Repository
import com.zchat.app.databinding.ActivityChatBinding
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var repository: Repository
    private lateinit var adapter: MessagesAdapter
    private var userId: String? = null
    private var username: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = Repository(applicationContext)
        userId = intent.getStringExtra("userId")
        username = intent.getStringExtra("username")
        
        binding.toolbar.title = username ?: "Чат"
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        adapter = MessagesAdapter(repository.currentUser?.uid ?: "")
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.rvMessages.adapter = adapter
        
        binding.btnVoiceCall.setOnClickListener { Toast.makeText(this, "Голосовой звонок", Toast.LENGTH_SHORT).show() }
        binding.btnVideoCall.setOnClickListener { Toast.makeText(this, "Видеозвонок", Toast.LENGTH_SHORT).show() }
        
        val currentUserId = repository.currentUser?.uid ?: return
        val otherUserId = userId ?: return
        lifecycleScope.launch {
            repository.getMessages(otherUserId, currentUserId).collect { messages ->
                adapter.submitList(messages) { binding.rvMessages.scrollToPosition(messages.size - 1) }
            }
        }
        
        binding.btnSend.setOnClickListener {
            val content = binding.etMessage.text.toString().trim()
            if (content.isNotEmpty()) {
                lifecycleScope.launch { repository.sendMessage(content, userId ?: return@launch) }
                binding.etMessage.text?.clear()
            }
        }
    }
}

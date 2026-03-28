package com.zchat.app.ui.chats

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
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
import com.zchat.app.util.LanguageHelper
import kotlinx.coroutines.launch
import java.util.*

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var repository: Repository
    private lateinit var adapter: MessagesAdapter
    private var userId: String? = null
    private var username: String? = null
    private var otherUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        applyLanguage()
        applyTheme()
        super.onCreate(savedInstanceState)

        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)

        userId = intent.getStringExtra("userId")
        username = intent.getStringExtra("username")

        setupToolbar()
        setupRecyclerView()
        observeMessages()
        loadUserInfo()

        binding.btnSend.setOnClickListener { sendMessage() }
    }

    private fun applyLanguage() {
        try {
            val repo = Repository(applicationContext)
            LanguageHelper.setLanguage(this, repo.language)
        } catch (e: Exception) {
            Log.e("ChatActivity", "Error applying language", e)
        }
    }

    private fun applyTheme() {
        try {
            val repo = Repository(applicationContext)
            when (repo.theme) {
                0 -> setTheme(R.style.Theme_GOODOK_Classic)
                1 -> setTheme(R.style.Theme_GOODOK_Modern)
                2 -> setTheme(R.style.Theme_GOODOK_Neon)
                3 -> setTheme(R.style.Theme_GOODOK_Childish)
            }
        } catch (e: Exception) {
            setTheme(R.style.Theme_GOODOK_Classic)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = username ?: "Chat"
    }

    private fun setupRecyclerView() {
        val currentId = try { repository.currentUserId ?: "" } catch (e: Exception) { "" }
        adapter = MessagesAdapter(
            currentUserId = currentId,
            onMessageLongClick = { message -> show_message_options(message) }
        )
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter
    }

    private fun observeMessages() {
        val uid = userId ?: return
        lifecycleScope.launch {
            try {
                val flow = repository.observeMessages(uid)
                if (flow != null) {
                    flow.collect { messages ->
                        adapter.submitList(messages)
                        if (messages.isNotEmpty()) {
                            binding.rvMessages.scrollToPosition(messages.size - 1)
                        }
                        binding.tvEmpty.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE
                    }
                } else {
                    binding.tvEmpty.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Error observing messages", e)
                binding.tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun loadUserInfo() {
        val uid = userId ?: return
        lifecycleScope.launch {
            try {
                val flow = repository.observeUserStatus(uid)
                if (flow != null) {
                    flow.collect { user ->
                        otherUser = user
                        supportActionBar?.subtitle = if (user.isOnline) {
                            getString(R.string.online)
                        } else {
                            if (user.lastSeen > 0) {
                                val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                                "${getString(R.string.last_seen)}: ${sdf.format(Date(user.lastSeen))}"
                            } else {
                                getString(R.string.offline)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Error loading user info", e)
            }
        }
    }

    private fun sendMessage() {
        val content = binding.etMessage.text.toString().trim()
        if (content.isEmpty() || userId == null) return

        val senderId = try { repository.currentUserId } catch (e: Exception) { null } ?: return

        val message = Message(
            id = UUID.randomUUID().toString(),
            senderId = senderId,
            receiverId = userId!!,
            content = content,
            timestamp = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            try {
                repository.sendMessage(message)
                binding.etMessage.text?.clear()
            } catch (e: Exception) {
                Log.e("ChatActivity", "Error sending message", e)
                Toast.makeText(this@ChatActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun show_message_options(message: Message) {
        val currentId = try { repository.currentUserId } catch (e: Exception) { null }
        if (message.senderId != currentId) return

        val options = arrayOf(
            getString(R.string.edit_message),
            getString(R.string.delete_message),
            getString(R.string.copy_message)
        )

        AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditDialog(message)
                    1 -> deleteMessage(message)
                    2 -> copyMessage(message)
                }
            }
            .show()
    }

    private fun showEditDialog(message: Message) {
        val input = EditText(this).apply {
            setText(message.content)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.edit_message)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val newContent = input.text.toString().trim()
                if (newContent.isNotEmpty() && userId != null) {
                    lifecycleScope.launch {
                        try {
                            repository.editMessage(message.id, userId!!, newContent)
                        } catch (e: Exception) {
                            Toast.makeText(this@ChatActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteMessage(message: Message) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_message)
            .setMessage(getString(R.string.delete_confirm))
            .setPositiveButton(R.string.delete) { _, _ ->
                userId?.let { uid ->
                    lifecycleScope.launch {
                        try {
                            repository.deleteMessage(message.id, uid)
                        } catch (e: Exception) {
                            Toast.makeText(this@ChatActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun copyMessage(message: Message) {
        try {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("message", message.content)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, getString(R.string.copied), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("ChatActivity", "Error copying message", e)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.chat_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_call -> {
                Toast.makeText(this, "Voice call feature coming soon", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_video -> {
                Toast.makeText(this, "Video call feature coming soon", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

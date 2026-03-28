package com.zchat.app.ui.chats

import android.app.AlertDialog
import android.os.Bundle
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
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)

        userId = intent.getStringExtra("userId")
        username = intent.getStringExtra("username")

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = username

        setupRecyclerView()
        observeMessages()
        loadUserInfo()

        binding.btnSend.setOnClickListener { sendMessage() }
    }

    private fun setupRecyclerView() {
        adapter = MessagesAdapter(
            currentUserId = repository.currentUserId ?: "",
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
            val flow = repository.observeMessages(uid)
            if (flow != null) {
                flow.collect { messages ->
                    adapter.submitList(messages)
                    if (messages.isNotEmpty()) {
                        binding.rvMessages.scrollToPosition(messages.size - 1)
                    }
                    binding.tvEmpty.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun loadUserInfo() {
        val uid = userId ?: return
        lifecycleScope.launch {
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
        }
    }

    private fun sendMessage() {
        val content = binding.etMessage.text.toString().trim()
        if (content.isEmpty() || userId == null) return

        val message = Message(
            id = UUID.randomUUID().toString(),
            senderId = repository.currentUserId ?: return,
            receiverId = userId!!,
            content = content,
            timestamp = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            repository.sendMessage(message)
            binding.etMessage.text?.clear()
        }
    }

    private fun show_message_options(message: Message) {
        if (message.senderId != repository.currentUserId) return

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
                        repository.editMessage(message.id, userId!!, newContent)
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteMessage(message: Message) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_message)
            .setMessage("Are you sure?")
            .setPositiveButton(R.string.delete) { _, _ ->
                userId?.let { uid ->
                    lifecycleScope.launch {
                        repository.deleteMessage(message.id, uid)
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun copyMessage(message: Message) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("message", message.content)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
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
                // TODO: Start voice call
                Toast.makeText(this, "Voice call feature coming soon", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_video -> {
                // TODO: Start video call
                Toast.makeText(this, "Video call feature coming soon", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

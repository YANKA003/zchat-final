package com.zchat.app.ui.chats

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
    private var userPhone: String? = null
    private val CALL_PERMISSION_REQUEST = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityChatBinding.inflate(layoutInflater)
            setContentView(binding.root)

            otherUserId = intent.getStringExtra("userId")
            val username = intent.getStringExtra("username") ?: "Чат"
            userPhone = intent.getStringExtra("userPhone")

            binding.tvTitle.text = username

            // Show phone number if available
            if (!userPhone.isNullOrEmpty()) {
                binding.tvPhone.text = formatPhoneNumber(userPhone!!)
                binding.tvPhone.visibility = View.VISIBLE
            }

            binding.btnBack.setOnClickListener { finish() }
            binding.btnCall.setOnClickListener { makeCall() }

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

    private fun formatPhoneNumber(phone: String): String {
        val digits = phone.replace(Regex("[^0-9]"), "")
        return if (digits.length >= 10) {
            val last10 = digits.takeLast(10)
            "+${if (digits.length > 10) digits.dropLast(10) else ""} (${last10.take(3)}) ${last10.drop(3).take(3)}-${last10.drop(6).take(2)}-${last10.drop(8)}"
        } else {
            "+$phone"
        }
    }

    private fun makeCall() {
        if (userPhone.isNullOrEmpty()) {
            Toast.makeText(this, "Номер телефона недоступен", Toast.LENGTH_SHORT).show()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CALL_PHONE),
                CALL_PERMISSION_REQUEST
            )
            return
        }

        try {
            val phoneDigits = userPhone!!.replace(Regex("[^0-9]"), "")
            val callIntent = Intent(Intent.ACTION_CALL)
            callIntent.data = Uri.parse("tel:+$phoneDigits")
            startActivity(callIntent)
        } catch (e: Exception) {
            Log.e("ChatActivity", "Call error", e)
            Toast.makeText(this, "Не удалось совершить звонок", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CALL_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                makeCall()
            } else {
                Toast.makeText(this, "Разрешение на звонки необходимо для совершения вызовов", Toast.LENGTH_LONG).show()
            }
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

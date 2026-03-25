package com.zchat.app.ui.calls

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
import com.zchat.app.R
import com.zchat.app.data.Repository
import com.zchat.app.data.model.Call
import com.zchat.app.databinding.ActivityCallBinding
import kotlinx.coroutines.launch
import java.util.*

class CallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallBinding
    private var repository: Repository? = null

    private var callId: String? = null
    private var callerName: String? = null
    private var isCaller: Boolean = false
    private var otherUserId: String? = null
    private var otherUserPhone: String? = null
    private var isVideoCall = false
    private var isMuted = false

    private val CALL_PERMISSION_REQUEST = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)

        callId = intent.getStringExtra("callId")
        callerName = intent.getStringExtra("callerName") ?: intent.getStringExtra("username")
        isCaller = intent.getBooleanExtra("isCaller", false)
        otherUserId = intent.getStringExtra("otherUserId") ?: intent.getStringExtra("callerId")
        otherUserPhone = intent.getStringExtra("userPhone")
        isVideoCall = intent.getBooleanExtra("isVideo", false)

        if (!isCaller) {
            showIncomingCallUI()
        } else {
            startOutgoingCall()
        }

        setupButtons()
    }

    private fun showIncomingCallUI() {
        binding.tvCallStatus.text = "Входящий звонок"
        binding.tvCallerName.text = callerName ?: "Неизвестный"
        binding.llIncomingCall.visibility = View.VISIBLE
        binding.llActiveCall.visibility = View.GONE
    }

    private fun showActiveCallUI() {
        binding.llIncomingCall.visibility = View.GONE
        binding.llActiveCall.visibility = View.VISIBLE
        binding.tvCallerName.text = callerName ?: "Неизвестный"
        binding.tvCallStatus.text = if (isVideoCall) "Видеозвонок" else "Голосовой звонок"
    }

    private fun setupButtons() {
        binding.btnAccept.setOnClickListener {
            acceptCall()
        }

        binding.btnDecline.setOnClickListener {
            declineCall()
        }

        binding.btnEndCall.setOnClickListener {
            endCall()
        }

        binding.btnMute.setOnClickListener {
            toggleMute()
        }

        binding.btnSpeaker.setOnClickListener {
            toggleSpeaker()
        }

        // Video button - for future implementation
        binding.btnVideo.visibility = View.GONE
    }

    private fun acceptCall() {
        lifecycleScope.launch {
            callId?.let { id ->
                repository?.updateCallStatus(id, "ACTIVE")
            }
        }
        showActiveCallUI()
        
        // For now, initiate a regular phone call
        // In the future, this would connect WebRTC
        showCallOptions()
    }

    private fun showCallOptions() {
        if (!otherUserPhone.isNullOrEmpty()) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Способ связи")
                .setMessage("VoIP звонки в разработке. Использовать обычный звонок?")
                .setPositiveButton("Позвонить") { _, _ ->
                    makePhoneCall()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    private fun startOutgoingCall() {
        showActiveCallUI()
        binding.tvCallStatus.text = "Вызов..."

        lifecycleScope.launch {
            try {
                val id = callId ?: UUID.randomUUID().toString()
                callId = id

                val call = Call(
                    id = id,
                    callerId = repository?.currentUser?.uid ?: "",
                    callerName = repository?.currentUser?.displayName ?: "Пользователь",
                    receiverId = otherUserId ?: "",
                    receiverName = callerName ?: "",
                    timestamp = System.currentTimeMillis(),
                    type = if (isVideoCall) "VIDEO" else "VOICE",
                    status = "RINGING"
                )

                repository?.initiateCall(call)

                // Show call options after a delay
                binding.tvCallStatus.text = "Соединение..."
                binding.root.postDelayed({
                    showCallOptions()
                }, 2000)

            } catch (e: Exception) {
                Log.e("CallActivity", "Failed to start call", e)
                Toast.makeText(this@CallActivity, "Ошибка звонка", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun makePhoneCall() {
        if (otherUserPhone.isNullOrEmpty()) {
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
            val phoneDigits = otherUserPhone!!.replace(Regex("[^0-9]"), "")
            val callIntent = Intent(Intent.ACTION_CALL)
            callIntent.data = Uri.parse("tel:+$phoneDigits")
            startActivity(callIntent)
        } catch (e: Exception) {
            Log.e("CallActivity", "Call error", e)
            Toast.makeText(this, "Не удалось совершить звонок", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CALL_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                makePhoneCall()
            } else {
                Toast.makeText(this, "Разрешение на звонки необходимо", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun declineCall() {
        lifecycleScope.launch {
            callId?.let { id ->
                repository?.updateCallStatus(id, "DECLINED")
            }
        }
        finish()
    }

    private fun endCall() {
        lifecycleScope.launch {
            callId?.let { id ->
                repository?.updateCallStatus(id, "ENDED")
            }
        }
        finish()
    }

    private fun toggleMute() {
        isMuted = !isMuted
        Toast.makeText(this, if (isMuted) "Микрофон выключен" else "Микрофон включён", Toast.LENGTH_SHORT).show()
    }

    private fun toggleSpeaker() {
        val audioManager = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
        audioManager.mode = android.media.AudioManager.MODE_IN_CALL
        audioManager.isSpeakerphoneOn = !audioManager.isSpeakerphoneOn
        Toast.makeText(this, if (audioManager.isSpeakerphoneOn) "Динамик включён" else "Динамик выключен", Toast.LENGTH_SHORT).show()
    }
}

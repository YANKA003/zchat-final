package com.zchat.app.ui.calls

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.zchat.app.R
import com.zchat.app.data.Repository
import com.zchat.app.data.model.Call
import com.zchat.app.databinding.ActivityCallBinding
import kotlinx.coroutines.launch
import java.util.*

class CallActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCallBinding
    private lateinit var repository: Repository
    private var callId: String? = null
    private var callerId: String? = null
    private var callerName: String? = null
    private var receiverId: String? = null
    private var receiverName: String? = null
    private var callType: String = "VOICE"
    private var callStatus: String = "OUTGOING"
    private var timer: CountDownTimer? = null
    private var callDuration: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)

        // Get intent extras
        callId = intent.getStringExtra("callId")
        callerId = intent.getStringExtra("callerId")
        callerName = intent.getStringExtra("callerName")
        receiverId = intent.getStringExtra("receiverId")
        receiverName = intent.getStringExtra("receiverName")
        callType = intent.getStringExtra("type") ?: "VOICE"
        callStatus = intent.getStringExtra("status") ?: "OUTGOING"

        setupUI()

        binding.btnEndCall.setOnClickListener { endCall() }
        binding.btnMute.setOnClickListener { toggleMute() }
        binding.btnSpeaker.setOnClickListener { toggleSpeaker() }
    }

    private fun setupUI() {
        binding.tvCallerName.text = callerName ?: receiverName ?: getString(R.string.unknown)
        binding.tvCallStatus.text = if (callStatus == "OUTGOING") {
            getString(R.string.outgoing_call)
        } else {
            getString(R.string.incoming_call)
        }

        if (callType == "VIDEO") {
            binding.ivCallType.setImageResource(R.drawable.ic_video)
        } else {
            binding.ivCallType.setImageResource(R.drawable.ic_phone)
        }

        startCallTimer()
    }

    private fun startCallTimer() {
        updateCallStatus("CONNECTING")

        // Simulate call connection after 2 seconds
        timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                callDuration++
                val minutes = callDuration / 60
                val seconds = callDuration % 60
                binding.tvCallDuration.text = String.format("%02d:%02d", minutes, seconds)
                binding.tvCallStatus.text = getString(R.string.call_ended).let {
                    if (callDuration < 3) "Connecting..." else "Connected"
                }
            }

            override fun onFinish() {}
        }.start()
    }

    private fun updateCallStatus(status: String) {
        binding.tvCallStatus.text = status
    }

    private fun endCall() {
        timer?.cancel()

        // Save call to history
        val call = Call(
            id = callId ?: UUID.randomUUID().toString(),
            callerId = callerId ?: "",
            callerName = callerName ?: "",
            receiverId = receiverId ?: "",
            receiverName = receiverName ?: "",
            timestamp = System.currentTimeMillis(),
            duration = callDuration,
            type = callType,
            status = if (callDuration == 0L) "MISSED" else "ENDED"
        )

        lifecycleScope.launch {
            repository.saveCall(call)
        }

        finish()
    }

    private fun toggleMute() {
        Toast.makeText(this, "Mute toggled", Toast.LENGTH_SHORT).show()
    }

    private fun toggleSpeaker() {
        Toast.makeText(this, "Speaker toggled", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}

package com.zchat.app.ui.calls

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.zchat.app.data.Repository
import com.zchat.app.data.model.Call
import com.zchat.app.data.model.CallSignal
import com.zchat.app.databinding.ActivityCallBinding
import com.zchat.app.services.CallService
import io.getstream.webrtc.android.ui.VideoView
import kotlinx.coroutines.launch
import org.webrtc.*
import java.util.*

class CallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallBinding
    private var repository: Repository? = null

    private var callId: String? = null
    private var callerName: String? = null
    private var isCaller: Boolean = false
    private var otherUserId: String? = null
    private var currentUserId: String? = null

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var isVideoCall = false
    private var isMuted = false
    private var isCameraOff = false

    private val PERMISSIONS_REQUEST = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)
        currentUserId = repository?.currentUser?.uid

        callId = intent.getStringExtra("callId")
        callerName = intent.getStringExtra("callerName")
        isCaller = intent.getBooleanExtra("isCaller", false)
        otherUserId = intent.getStringExtra("otherUserId")
        isVideoCall = intent.getBooleanExtra("isVideo", false)

        if (!isCaller) {
            // Incoming call
            otherUserId = intent.getStringExtra("callerId")
            showIncomingCallUI()
        } else {
            // Outgoing call
            startCall()
        }

        setupButtons()

        // Start foreground service
        startCallService()
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

        binding.btnVideo.setOnClickListener {
            toggleVideo()
        }
    }

    private fun checkPermissions(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), PERMISSIONS_REQUEST)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initializeWebRTC()
            } else {
                Toast.makeText(this, "Нужны разрешения для звонка", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun acceptCall() {
        lifecycleScope.launch {
            callId?.let { id ->
                repository?.updateCallStatus(id, "ACTIVE")
            }
        }
        showActiveCallUI()
        if (checkPermissions()) {
            initializeWebRTC()
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

    private fun startCall() {
        showActiveCallUI()
        binding.tvCallStatus.text = "Вызов..."

        lifecycleScope.launch {
            try {
                val id = UUID.randomUUID().toString()
                callId = id

                val call = Call(
                    id = id,
                    callerId = currentUserId ?: "",
                    callerName = repository?.currentUser?.displayName ?: "Пользователь",
                    receiverId = otherUserId ?: "",
                    receiverName = callerName ?: "",
                    timestamp = System.currentTimeMillis(),
                    type = if (isVideoCall) "VIDEO" else "VOICE",
                    status = "RINGING"
                )

                repository?.initiateCall(call)

                // Listen for answer
                observeCallSignals()

                if (checkPermissions()) {
                    initializeWebRTC()
                }
            } catch (e: Exception) {
                Log.e("CallActivity", "Failed to start call", e)
                Toast.makeText(this@CallActivity, "Ошибка звонка", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun initializeWebRTC() {
        try {
            // Initialize PeerConnectionFactory
            val options = PeerConnectionFactory.InitializationOptions.builder(this)
                .setEnableInternalTracerCapture(true)
                .setEnableVideoHwAcceleration(true)
                .createInitializationOptions()
            PeerConnectionFactory.initialize(options)

            peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoDecoderFactory(DefaultVideoDecoderFactory(binding.localView.eglBaseContext))
                .setVideoEncoderFactory(DefaultVideoEncoderFactory(binding.localView.eglBaseContext, true, true))
                .createPeerConnectionFactory()

            // Create peer connection
            val iceServers = listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
            )

            val rtcConfig = PeerConnection.RTCConfiguration(iceServers)

            peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    runOnUiThread {
                        when (state) {
                            PeerConnection.IceConnectionState.CONNECTED -> {
                                binding.tvCallStatus.text = "Соединено"
                            }
                            PeerConnection.IceConnectionState.DISCONNECTED,
                            PeerConnection.IceConnectionState.CLOSED -> {
                                endCall()
                            }
                            else -> {}
                        }
                    }
                }
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let {
                        sendIceCandidate(it)
                    }
                }
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                override fun onAddStream(stream: MediaStream?) {
                    stream?.videoTracks?.firstOrNull()?.addSink(binding.remoteView)
                }
                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onDataChannel(channel: DataChannel?) {}
                override fun onRenegotiationNeeded() {
                    if (isCaller) {
                        createOffer()
                    }
                }
                override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
            })

            // Add audio track
            val audioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
            localAudioTrack = peerConnectionFactory?.createAudioTrack("audio", audioSource)
            peerConnection?.addTrack(localAudioTrack)

            // Add video track if video call
            if (isVideoCall) {
                val videoCapturer = createVideoCapturer()
                val videoSource = peerConnectionFactory?.createVideoSource(videoCapturer?.isScreencast ?: false)
                videoCapturer?.initialize(
                    SurfaceTextureHelper.create("VideoThread", binding.localView.eglBaseContext),
                    this, videoSource?.capturerObserver
                )
                videoCapturer?.startCapture(640, 480, 30)

                localVideoTrack = peerConnectionFactory?.createVideoTrack("video", videoSource)
                localVideoTrack?.addSink(binding.localView)
                peerConnection?.addTrack(localVideoTrack)
            }

        } catch (e: Exception) {
            Log.e("CallActivity", "Failed to initialize WebRTC", e)
        }
    }

    private fun createVideoCapturer(): VideoCapturer? {
        return try {
            val enumerator = Camera2Enumerator(this)
            enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }?.let {
                enumerator.createCapturer(it, null)
            }
        } catch (e: Exception) {
            Log.e("CallActivity", "Failed to create video capturer", e)
            null
        }
    }

    private fun createOffer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", isVideoCall.toString()))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        sendSdp(sdp)
                    }
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, sdp)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Log.e("CallActivity", "Create offer failed: $error")
            }
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    private fun sendSdp(sdp: SessionDescription?) {
        lifecycleScope.launch {
            val signal = CallSignal(
                id = "${callId}_${System.currentTimeMillis()}",
                callerId = currentUserId ?: "",
                receiverId = otherUserId ?: "",
                type = sdp?.type?.canonicalForm() ?: "offer",
                sdp = sdp?.description ?: "",
                timestamp = System.currentTimeMillis()
            )
            repository?.sendCallSignal(signal)
        }
    }

    private fun sendIceCandidate(candidate: IceCandidate) {
        lifecycleScope.launch {
            val signal = CallSignal(
                id = "${callId}_ice_${System.currentTimeMillis()}",
                callerId = currentUserId ?: "",
                receiverId = otherUserId ?: "",
                type = "ice-candidate",
                iceCandidates = "${candidate.sdpMid}:${candidate.sdpMLineIndex}:${candidate.sdp}",
                timestamp = System.currentTimeMillis()
            )
            repository?.sendCallSignal(signal)
        }
    }

    private fun observeCallSignals() {
        lifecycleScope.launch {
            callId?.let { id ->
                repository?.observeCallSignals(id)?.collect { signal ->
                    handleSignal(signal)
                }
            }
        }
    }

    private fun handleSignal(signal: CallSignal) {
        when (signal.type) {
            "offer" -> {
                val sdp = SessionDescription(SessionDescription.Type.OFFER, signal.sdp)
                peerConnection?.setRemoteDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        createAnswer()
                    }
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, sdp)
            }
            "answer" -> {
                val sdp = SessionDescription(SessionDescription.Type.ANSWER, signal.sdp)
                peerConnection?.setRemoteDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, sdp)
            }
            "ice-candidate" -> {
                val parts = signal.iceCandidates.split(":")
                if (parts.size >= 3) {
                    val candidate = IceCandidate(parts[0], parts[1].toInt(), parts.drop(2).joinToString(":"))
                    peerConnection?.addIceCandidate(candidate)
                }
            }
        }
    }

    private fun createAnswer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", isVideoCall.toString()))
        }

        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        sendSdp(sdp)
                    }
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, sdp)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    private fun toggleMute() {
        isMuted = !isMuted
        localAudioTrack?.setEnabled(!isMuted)
        binding.btnMute.setImageResource(
            if (isMuted) android.R.drawable.ic_btn_speak_now
            else android.R.drawable.ic_btn_speak_now
        )
    }

    private fun toggleSpeaker() {
        // Toggle speakerphone
        val audioManager = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
        audioManager.mode = android.media.AudioManager.MODE_IN_CALL
        audioManager.isSpeakerphoneOn = !audioManager.isSpeakerphoneOn
    }

    private fun toggleVideo() {
        isVideoCall = !isVideoCall
        if (isVideoCall) {
            binding.localView.visibility = View.VISIBLE
            binding.remoteView.visibility = View.VISIBLE
        } else {
            binding.localView.visibility = View.GONE
            binding.remoteView.visibility = View.GONE
        }
    }

    private fun endCall() {
        lifecycleScope.launch {
            callId?.let { id ->
                repository?.updateCallStatus(id, "ENDED")
            }
        }
        cleanup()
        finish()
    }

    private fun cleanup() {
        localVideoTrack?.dispose()
        localAudioTrack?.dispose()
        peerConnection?.close()
        peerConnectionFactory?.dispose()
        stopService(Intent(this, CallService::class.java))
    }

    private fun startCallService() {
        val intent = Intent(this, CallService::class.java).apply {
            putExtra(CallService.EXTRA_CALL_ID, callId)
            putExtra(CallService.EXTRA_CALLER_NAME, callerName)
            putExtra(CallService.EXTRA_IS_CALLER, isCaller)
        }
        startService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }
}

package com.zchat.app.ui.calls

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.zchat.app.R
import com.zchat.app.data.Repository
import com.zchat.app.data.model.Call
import com.zchat.app.databinding.ActivityCallsBinding
import com.zchat.app.ui.theme.ThemeManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CallsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCallsBinding
    private lateinit var repository: Repository
    private lateinit var adapter: CallsAdapter
    private var currentFilter = "all" // all, missed, outgoing
    
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.init(applicationContext)
        applyThemeColors()
        
        super.onCreate(savedInstanceState)
        binding = ActivityCallsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = Repository(applicationContext)
        
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        adapter = CallsAdapter(repository.currentUser?.uid ?: "")
        binding.rvCalls.layoutManager = LinearLayoutManager(this)
        binding.rvCalls.adapter = adapter
        
        setupTabs()
        loadCalls()
    }
    
    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                currentFilter = when (tab?.position) {
                    1 -> "missed"
                    2 -> "outgoing"
                    else -> "all"
                }
                loadCalls()
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }
    
    private fun loadCalls() {
        binding.emptyState.visibility = View.GONE
        
        // Demo data - в реальном приложении загружать из Firebase
        val demoCalls = createDemoCalls()
        
        val filteredCalls = when (currentFilter) {
            "missed" -> demoCalls.filter { it.isMissed() }
            "outgoing" -> demoCalls.filter { it.isOutgoing(repository.currentUser?.uid ?: "") }
            else -> demoCalls
        }
        
        if (filteredCalls.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.tvEmpty.text = when (currentFilter) {
                "missed" -> getString(R.string.no_missed_calls)
                "outgoing" -> getString(R.string.no_outgoing_calls)
                else -> getString(R.string.no_calls)
            }
        } else {
            adapter.submitList(filteredCalls)
        }
    }
    
    private fun createDemoCalls(): List<Call> {
        val currentTime = System.currentTimeMillis()
        val minskOffset = 3 * 60 * 60 * 1000 // UTC+3
        val uid = repository.currentUser?.uid ?: ""
        
        return listOf(
            Call(
                id = "1",
                callerId = "other1",
                receiverId = uid,
                timestamp = currentTime - 3600000, // 1 час назад
                duration = 180,
                type = "VOICE",
                status = "ACCEPTED",
                callerName = "Анна Иванова",
                callerAvatar = ""
            ),
            Call(
                id = "2",
                callerId = uid,
                receiverId = "other2",
                timestamp = currentTime - 7200000, // 2 часа назад
                duration = 0,
                type = "VIDEO",
                status = "MISSED",
                receiverName = "Петр Петров",
                receiverAvatar = ""
            ),
            Call(
                id = "3",
                callerId = "other3",
                receiverId = uid,
                timestamp = currentTime - 86400000, // вчера
                duration = 600,
                type = "VOICE",
                status = "ACCEPTED",
                callerName = "Мария Сидорова",
                callerAvatar = ""
            )
        )
    }
    
    private fun applyThemeColors() {
        val colors = ThemeManager.getColors()
        window.statusBarColor = colors.primaryDark.toColorInt()
    }
}

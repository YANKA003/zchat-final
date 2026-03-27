package com.zchat.app.ui.channels

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import com.zchat.app.R
import com.zchat.app.data.Repository
import com.zchat.app.data.model.Channel
import com.zchat.app.databinding.ActivityChannelsBinding
import com.zchat.app.ui.theme.ThemeManager
import kotlinx.coroutines.launch
import java.util.*

class ChannelsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityChannelsBinding
    private lateinit var repository: Repository
    private lateinit var adapter: ChannelsAdapter
    private var isPremiumUser = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.init(applicationContext)
        applyThemeColors()
        
        super.onCreate(savedInstanceState)
        binding = ActivityChannelsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = Repository(applicationContext)
        
        isPremiumUser = repository.preferencesManager.settings.value.premiumEnabled
        
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        adapter = ChannelsAdapter(
            currentUserId = repository.currentUser?.uid ?: "",
            onChannelClick = { channel -> openChannel(channel) },
            onSubscribeClick = { channel -> toggleSubscription(channel) }
        )
        binding.rvChannels.layoutManager = LinearLayoutManager(this)
        binding.rvChannels.adapter = adapter
        
        // Поиск каналов (только для Premium)
        if (isPremiumUser) {
            binding.searchLayout.visibility = View.VISIBLE
            binding.etSearch.setOnTextChangedListener { text ->
                searchChannels(text?.toString() ?: "")
            }
        } else {
            binding.searchLayout.visibility = View.GONE
        }
        
        // Создание канала
        binding.fabCreateChannel.setOnClickListener {
            showCreateChannelDialog()
        }
        
        loadChannels()
    }
    
    private fun loadChannels() {
        binding.emptyState.visibility = View.GONE
        
        lifecycleScope.launch {
            // Demo данные
            val channels = createDemoChannels()
            
            if (channels.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.btnCreateFirstChannel.setOnClickListener {
                    showCreateChannelDialog()
                }
            } else {
                adapter.submitList(channels)
            }
        }
    }
    
    private fun searchChannels(query: String) {
        lifecycleScope.launch {
            val channels = createDemoChannels()
            val filtered = if (query.isEmpty()) {
                channels
            } else {
                channels.filter { 
                    it.name.contains(query, ignoreCase = true) || 
                    it.description.contains(query, ignoreCase = true) 
                }
            }
            adapter.submitList(filtered)
        }
    }
    
    private fun createDemoChannels(): List<Channel> {
        return listOf(
            Channel(
                id = "ch1",
                name = "Tech News",
                description = "Последние новости технологий",
                ownerId = "user1",
                ownerName = "Tech Team",
                subscribersCount = 15234,
                createdAt = System.currentTimeMillis() - 100000000,
                category = "technology"
            ),
            Channel(
                id = "ch2",
                name = "Cooking Recipes",
                description = "Лучшие рецепты со всего мира",
                ownerId = "user2",
                ownerName = "Chef Anna",
                subscribersCount = 8562,
                createdAt = System.currentTimeMillis() - 200000000,
                category = "food"
            ),
            Channel(
                id = "ch3",
                name = "Travel Tips",
                description = "Советы путешественникам",
                ownerId = "user3",
                ownerName = "Travel Master",
                subscribersCount = 23041,
                createdAt = System.currentTimeMillis() - 50000000,
                category = "travel"
            )
        )
    }
    
    private fun openChannel(channel: Channel) {
        // Открыть канал
        val intent = android.content.Intent(this, ChannelDetailActivity::class.java)
        intent.putExtra("channelId", channel.id)
        intent.putExtra("channelName", channel.name)
        startActivity(intent)
    }
    
    private fun toggleSubscription(channel: Channel) {
        // Подписаться/отписаться
    }
    
    private fun showCreateChannelDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.channel_name)
            setSingleLine()
        }
        
        AlertDialog.Builder(this)
            .setTitle(R.string.create_channel)
            .setView(input)
            .setPositiveButton(R.string.create) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    createChannel(name)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun createChannel(name: String) {
        lifecycleScope.launch {
            val channel = Channel(
                id = UUID.randomUUID().toString(),
                name = name,
                description = "",
                ownerId = repository.currentUser?.uid ?: "",
                ownerName = repository.currentUser?.displayName ?: "",
                createdAt = System.currentTimeMillis()
            )
            loadChannels()
        }
    }
    
    private fun applyThemeColors() {
        val colors = ThemeManager.getColors()
        window.statusBarColor = colors.primaryDark.toColorInt()
    }
}

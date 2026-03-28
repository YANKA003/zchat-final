package com.zchat.app.ui.channels

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.zchat.app.R
import com.zchat.app.data.Repository
import com.zchat.app.data.model.Channel
import com.zchat.app.databinding.ActivityChannelsBinding
import com.zchat.app.util.LanguageHelper
import kotlinx.coroutines.launch
import java.util.UUID

class ChannelsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChannelsBinding
    private lateinit var repository: Repository
    private lateinit var adapter: ChannelsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        applyLanguage()
        applyTheme()
        super.onCreate(savedInstanceState)

        binding = ActivityChannelsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)

        setupToolbar()
        setupRecyclerView()
        loadChannels()

        binding.fabCreateChannel.setOnClickListener {
            showCreateChannelDialog()
        }
    }

    private fun applyLanguage() {
        try {
            val repo = Repository(applicationContext)
            LanguageHelper.setLanguage(this, repo.language)
        } catch (e: Exception) {
            Log.e("ChannelsActivity", "Error applying language", e)
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
        supportActionBar?.title = getString(R.string.channels)
    }

    private fun setupRecyclerView() {
        adapter = ChannelsAdapter { channel ->
            // Handle channel click
            onChannelClick(channel)
        }
        binding.rvChannels.layoutManager = LinearLayoutManager(this)
        binding.rvChannels.adapter = adapter
    }

    private fun onChannelClick(channel: Channel) {
        // Show subscribe dialog
        lifecycleScope.launch {
            val result = repository.subscribeToChannel(channel.id)
            result.fold(
                onSuccess = {
                    android.widget.Toast.makeText(
                        this@ChannelsActivity,
                        getString(R.string.subscribe),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                onFailure = { e ->
                    android.widget.Toast.makeText(
                        this@ChannelsActivity,
                        "Error: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    private fun loadChannels() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val result = repository.searchChannels("")
                binding.progressBar.visibility = View.GONE

                result.fold(
                    onSuccess = { channels ->
                        adapter.submitList(channels)
                        binding.tvEmpty.visibility = if (channels.isEmpty()) View.VISIBLE else View.GONE
                    },
                    onFailure = { e ->
                        Log.e("ChannelsActivity", "Error loading channels", e)
                        // Try local database
                        val localChannels = repository.getLocalChannels()
                        adapter.submitList(localChannels)
                        binding.tvEmpty.visibility = if (localChannels.isEmpty()) View.VISIBLE else View.GONE
                    }
                )
            } catch (e: Exception) {
                Log.e("ChannelsActivity", "Error loading channels", e)
                binding.progressBar.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun showCreateChannelDialog() {
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.create_channel))
            .setView(R.layout.dialog_create_channel)
            .setPositiveButton(getString(R.string.create)) { dialog, _ ->
                val dialogView = (dialog as android.app.AlertDialog).findViewById<android.view.View>(android.R.id.content)
                val nameInput = dialogView?.findViewById<android.widget.EditText>(R.id.etChannelName)
                val descInput = dialogView?.findViewById<android.widget.EditText>(R.id.etChannelDescription)

                val name = nameInput?.text?.toString()?.trim() ?: ""
                val description = descInput?.text?.toString()?.trim() ?: ""

                if (name.isNotEmpty()) {
                    createChannel(name, description)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
        dialog.show()
    }

    private fun createChannel(name: String, description: String) {
        lifecycleScope.launch {
            try {
                val channel = Channel(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    description = description,
                    ownerId = repository.currentUserId ?: "",
                    createdAt = System.currentTimeMillis()
                )

                val result = repository.createChannel(channel)
                result.fold(
                    onSuccess = {
                        android.widget.Toast.makeText(
                            this@ChannelsActivity,
                            getString(R.string.success),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        loadChannels()
                    },
                    onFailure = { e ->
                        android.widget.Toast.makeText(
                            this@ChannelsActivity,
                            "Error: ${e.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            } catch (e: Exception) {
                Log.e("ChannelsActivity", "Error creating channel", e)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

package com.zchat.app.ui.channels

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.zchat.app.R
import com.zchat.app.data.Repository
import com.zchat.app.data.model.Channel
import com.zchat.app.databinding.ActivityChannelsBinding
import kotlinx.coroutines.launch
import java.util.*

class ChannelsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChannelsBinding
    private lateinit var repository: Repository
    private lateinit var adapter: ChannelsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChannelsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.channels)

        setupRecyclerView()
        setupSearch()
        loadChannels()

        binding.fabCreateChannel.setOnClickListener { showCreateChannelDialog() }
    }

    private fun setupRecyclerView() {
        adapter = ChannelsAdapter(
            currentUserId = repository.currentUserId ?: "",
            onSubscribe = { channel -> subscribeToChannel(channel) }
        )
        binding.rvChannels.layoutManager = LinearLayoutManager(this)
        binding.rvChannels.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            searchChannels(text?.toString() ?: "")
        }
    }

    private fun loadChannels() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = repository.searchChannels("")
            binding.progressBar.visibility = View.GONE

            result.fold(
                onSuccess = { channels ->
                    adapter.submitList(channels)
                    binding.tvEmpty.visibility = if (channels.isEmpty()) View.VISIBLE else View.GONE
                },
                onFailure = { e ->
                    Toast.makeText(this@ChannelsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun searchChannels(query: String) {
        lifecycleScope.launch {
            val result = repository.searchChannels(query)
            result.fold(
                onSuccess = { channels -> adapter.submitList(channels) },
                onFailure = {}
            )
        }
    }

    private fun showCreateChannelDialog() {
        val view = layoutInflater.inflate(android.R.layout.simple_list_item_1, null)
        val nameInput = EditText(this).apply {
            hint = getString(R.string.channel_name)
        }
        val descInput = EditText(this).apply {
            hint = getString(R.string.channel_description)
        }

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
            addView(nameInput)
            addView(descInput)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.create_channel)
            .setView(layout)
            .setPositiveButton(R.string.create) { _, _ ->
                val name = nameInput.text.toString().trim()
                val desc = descInput.text.toString().trim()

                if (name.isNotEmpty()) {
                    createChannel(name, desc)
                } else {
                    Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun createChannel(name: String, description: String) {
        val channel = Channel(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            ownerId = repository.currentUserId ?: return
        )

        lifecycleScope.launch {
            val result = repository.createChannel(channel)
            result.fold(
                onSuccess = {
                    Toast.makeText(this@ChannelsActivity, R.string.success, Toast.LENGTH_SHORT).show()
                    loadChannels()
                },
                onFailure = { e ->
                    Toast.makeText(this@ChannelsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun subscribeToChannel(channel: Channel) {
        lifecycleScope.launch {
            val result = repository.subscribeToChannel(channel.id)
            result.fold(
                onSuccess = {
                    Toast.makeText(this@ChannelsActivity, R.string.success, Toast.LENGTH_SHORT).show()
                },
                onFailure = { e ->
                    Toast.makeText(this@ChannelsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

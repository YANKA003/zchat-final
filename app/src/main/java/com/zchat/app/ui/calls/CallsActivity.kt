package com.zchat.app.ui.calls

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.zchat.app.R
import com.zchat.app.data.Repository
import com.zchat.app.data.model.Call
import com.zchat.app.databinding.ActivityCallsBinding
import com.zchat.app.util.LanguageHelper
import kotlinx.coroutines.launch

class CallsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCallsBinding
    private lateinit var repository: Repository
    private lateinit var adapter: CallsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        applyLanguage()
        applyTheme()
        super.onCreate(savedInstanceState)

        binding = ActivityCallsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)

        setupToolbar()
        setupRecyclerView()
        loadCalls()
    }

    private fun applyLanguage() {
        try {
            val repo = Repository(applicationContext)
            LanguageHelper.setLanguage(this, repo.language)
        } catch (e: Exception) {
            Log.e("CallsActivity", "Error applying language", e)
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
        supportActionBar?.title = getString(R.string.calls)
    }

    private fun setupRecyclerView() {
        adapter = CallsAdapter { call ->
            // Handle call click - redial
            onCallClick(call)
        }
        binding.rvCalls.layoutManager = LinearLayoutManager(this)
        binding.rvCalls.adapter = adapter
    }

    private fun onCallClick(call: Call) {
        // TODO: Implement redial functionality
        // For now, just show a toast
        android.widget.Toast.makeText(this, "Redial: ${call.callerName}", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun loadCalls() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Try to load from Firebase first
                val flow = repository.observeCalls()
                if (flow != null) {
                    flow.collect { calls ->
                        binding.progressBar.visibility = View.GONE
                        adapter.submitList(calls)
                        binding.tvEmpty.visibility = if (calls.isEmpty()) View.VISIBLE else View.GONE
                    }
                } else {
                    // Fallback to local database
                    val localCalls = repository.getLocalCalls()
                    binding.progressBar.visibility = View.GONE
                    adapter.submitList(localCalls)
                    binding.tvEmpty.visibility = if (localCalls.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                Log.e("CallsActivity", "Error loading calls", e)
                binding.progressBar.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

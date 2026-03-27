package com.zchat.app.ui.calls

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.zchat.app.R
import com.zchat.app.data.Repository
import com.zchat.app.databinding.ActivityCallsBinding
import kotlinx.coroutines.launch
import java.util.*

class CallsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCallsBinding
    private lateinit var repository: Repository
    private lateinit var adapter: CallsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.calls)

        setupRecyclerView()
        loadCalls()
    }

    private fun setupRecyclerView() {
        adapter = CallsAdapter { call ->
            // TODO: Open user profile or redial
        }
        binding.rvCalls.layoutManager = LinearLayoutManager(this)
        binding.rvCalls.adapter = adapter
    }

    private fun loadCalls() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            repository.observeCalls().collect { calls ->
                binding.progressBar.visibility = View.GONE

                // Sort by timestamp descending and format with Minsk timezone
                val sortedCalls = calls.sortedByDescending { it.timestamp }

                adapter.submitList(sortedCalls)
                binding.tvEmpty.visibility = if (sortedCalls.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

package com.zchat.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.zchat.app.R
import com.zchat.app.data.Repository
import com.zchat.app.databinding.ActivityAuthBinding
import com.zchat.app.ui.MainActivity
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private lateinit var repository: Repository
    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)

        // Check if already logged in
        if (repository.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        updateUI()

        binding.btnLogin.setOnClickListener {
            if (isLoginMode) {
                performLogin()
            } else {
                isLoginMode = true
                updateUI()
            }
        }

        binding.btnRegister.setOnClickListener {
            if (!isLoginMode) {
                performRegister()
            } else {
                isLoginMode = false
                updateUI()
            }
        }
    }

    private fun updateUI() {
        binding.tvTitle.text = if (isLoginMode) getString(R.string.login) else getString(R.string.register)
        binding.btnLogin.text = getString(R.string.login)
        binding.btnRegister.text = getString(R.string.register)
        binding.tilUsername.visibility = if (isLoginMode) View.GONE else View.VISIBLE
        binding.tilConfirmPassword.visibility = if (isLoginMode) View.GONE else View.VISIBLE
    }

    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = repository.login(email, password)
            binding.progressBar.visibility = View.GONE

            result.fold(
                onSuccess = {
                    repository.setOnlineStatus(true)
                    startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                    finish()
                },
                onFailure = { e ->
                    Toast.makeText(this@AuthActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun performRegister() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = repository.register(email, password, username)
            binding.progressBar.visibility = View.GONE

            result.fold(
                onSuccess = {
                    repository.setOnlineStatus(true)
                    startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                    finish()
                },
                onFailure = { e ->
                    Toast.makeText(this@AuthActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

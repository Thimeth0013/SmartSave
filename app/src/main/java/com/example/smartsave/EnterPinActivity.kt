package com.example.smartsave

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.SharedPreferences
import android.widget.Toast

class EnterPinActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private val TAG = "EnterPinActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("SmartSavePrefs", MODE_PRIVATE)
        Log.d(TAG, "Loaded SharedPreferences: is_pin_set=${sharedPreferences.getBoolean("is_pin_set", false)}, app_pin=${sharedPreferences.getString("app_pin", "null")}")

        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Find views
        val pinInput = findViewById<EditText>(R.id.pin_input)
        val continueButton = findViewById<Button>(R.id.signInBtn)

        // Continue button click
        continueButton.setOnClickListener {
            val storedPin = sharedPreferences.getString("app_pin", null)
            if (storedPin == null) {
                Log.d(TAG, "No PIN stored, redirecting to MainActivity")
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return@setOnClickListener
            }

            val pin = pinInput.text.toString().trim()
            if (pin.isEmpty()) {
                Toast.makeText(this, "Please enter a PIN", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pin.length != 4 || !pin.all { it.isDigit() }) {
                Toast.makeText(this, "Please enter a 4-digit PIN", Toast.LENGTH_SHORT).show()
                pinInput.text.clear()
                return@setOnClickListener
            }

            if (pin == storedPin) {
                Log.d(TAG, "PIN correct, navigating to Home")
                Toast.makeText(this, "Access granted", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, Home::class.java))
                finish()
            } else {
                Log.d(TAG, "PIN incorrect")
                Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                pinInput.text.clear()
            }
        }
    }
}
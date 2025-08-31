package com.example.smartsave

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.SharedPreferences

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val prefs = getSharedPreferences("SmartSavePrefs", MODE_PRIVATE)
        val isPinSet = prefs.getBoolean("is_pin_set", false)

        // Check if coming from EnterPinActivity after successful PIN validation
        val isPinValidated = intent.getBooleanExtra("PIN_VALIDATED", false)

        if (isPinSet && !isPinValidated) {
            startActivity(Intent(this, EnterPinActivity::class.java))
            finish()
            return
        }

        // Transition to OnboardingActivity or Home after 2 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            val isOnboardingCompleted = prefs.getBoolean("is_onboarding_completed", false)
            if (isOnboardingCompleted) {
                startActivity(Intent(this, Home::class.java))
            } else {
                startActivity(Intent(this, OnboardingActivity::class.java))
            }
            finish()
        }, 2000) // 2000ms = 2 seconds
    }
}
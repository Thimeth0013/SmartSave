package com.example.smartsave

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2

class OnboardingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_onboarding)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val viewPager = findViewById<ViewPager2>(R.id.view_pager)
        val skipButton = findViewById<Button>(R.id.skip_button)
        val nextButton = findViewById<Button>(R.id.next_button)

        viewPager.adapter = OnboardingAdapter(this)

        skipButton.setOnClickListener {
            completeOnboarding()
        }

        nextButton.setOnClickListener {
            val currentItem = viewPager.currentItem
            if (currentItem < 2) {
                viewPager.currentItem = currentItem + 1
            } else {
                completeOnboarding()
            }
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                nextButton.text = if (position == 2) "Get Started ➔" else "Next ➔"
            }
        })
    }

    private fun completeOnboarding() {
        getSharedPreferences("SmartSavePrefs", MODE_PRIVATE)
            .edit()
            .putBoolean("is_onboarding_completed", true)
            .apply()
        startActivity(Intent(this, Home::class.java))
        finish()
    }
}
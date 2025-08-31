package com.example.smartsave

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

class OnboardingFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_onboarding, container, false)

        val position = arguments?.getInt("position") ?: 0
        val image1 = view.findViewById<ImageView>(R.id.image1)
        val image2 = view.findViewById<ImageView>(R.id.image2)
        val description = view.findViewById<TextView>(R.id.description)

        when (position) {
            0 -> {
                image1.setImageResource(R.drawable.splash1)
                image2.setImageResource(R.drawable.slide1)
                description.text = "Track every expense and set monthly budgets effortlessly."
            }
            1 -> {
                image1.setImageResource(R.drawable.splash2)
                image2.setImageResource(R.drawable.slide2)
                description.text = "Visualize your spending habits with clear, real-time insights."
            }
            2 -> {
                image1.setImageResource(R.drawable.splash3)
                image2.setImageResource(R.drawable.slide3)
                description.text = "Your data stays secure while you control your finances."
            }
        }

        return view
    }

    companion object {
        fun newInstance(position: Int): OnboardingFragment {
            val fragment = OnboardingFragment()
            val args = Bundle()
            args.putInt("position", position)
            fragment.arguments = args
            return fragment
        }
    }
}
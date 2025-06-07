package com.example.foodakinator.ui.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.foodakinator.R
import com.example.foodakinator.databinding.FragmentWelcomeBinding

class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up start button
        binding.startButton.setOnClickListener {
            // Navigate to question fragment
            findNavController().navigate(R.id.action_welcome_to_question)
        }

        // Set up about button (optional)
        binding.aboutButton.setOnClickListener {
            // You can add an about dialog or navigate to an about fragment
            // For now, just show a simple message
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
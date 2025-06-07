package com.example.foodakinator.ui.results

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.foodakinator.R
import com.example.foodakinator.databinding.FragmentResultsBinding
import com.example.foodakinator.ui.question.RecommendationHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ResultsFragment : Fragment() {
    private val TAG = "ResultsFragment"

    private var _binding: FragmentResultsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ResultsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    // In your ResultsFragment onViewCreated, add this delay:

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(TAG, "ResultsFragment started")

        // Add small delay to ensure session info is saved
        lifecycleScope.launch {
            delay(100) // Wait 100ms for session info to be saved

            val recommendations = RecommendationHolder.getRecommendations()
            Log.d(TAG, "Got ${recommendations.size} recommendations")

            if (recommendations.isNotEmpty()) {
                val topRecommendation = recommendations.first()
                Log.d(TAG, "Top recommended dish: ${topRecommendation.name} (ID: ${topRecommendation.id})")

                // Display the recommendation
                binding.dishName.text = topRecommendation.name

                // Get session info (should be available now)
                val sessionInfo = RecommendationHolder.getSessionInfo()
                Log.d(TAG, "Session info: $sessionInfo")

                binding.dishDescription.text = "${topRecommendation.description}\n\n$sessionInfo"

                // "That's it!" button - user is satisfied
                binding.thatsItButton.setOnClickListener {
                    Log.d(TAG, "User is satisfied with recommendation")
                    navigateToDishDetails(topRecommendation.id)
                }

                // Handle continue vs try again logic
                // Update your ResultsFragment continue button click:

// Handle continue vs try again logic
                if (RecommendationHolder.canContinueAsking()) {
                    Log.d(TAG, "Can continue asking - showing continue button")
                    // User can continue asking questions
                    binding.tryAgainButton.text = "Not quite right? Continue asking questions"
                    binding.tryAgainButton.setOnClickListener {
                        Log.d(TAG, "User wants to continue asking questions")

                        // SET A FLAG to indicate this is a continue action
                        RecommendationHolder.setContinueFlag(true)

                        findNavController().navigate(R.id.action_results_to_question)
                    }
                } else {
                    Log.d(TAG, "Cannot continue - showing start over")
                    // Cannot continue
                    binding.tryAgainButton.text = "Start Over"
                    binding.tryAgainButton.setOnClickListener {
                        Log.d(TAG, "User wants to start completely over")

                        // CLEAR THE FLAG for fresh start
                        RecommendationHolder.setContinueFlag(false)
                        RecommendationHolder.setSessionInfo(0, 0, 100) // Reset session

                        findNavController().navigate(R.id.action_results_to_question)
                    }
                }
            } else {
                Log.e(TAG, "No recommendations found! Using fallback")
                binding.dishName.text = "No recommendations available"
                binding.dishDescription.text = "Please try again"

                binding.thatsItButton.text = "OK"
                binding.thatsItButton.setOnClickListener {
                    findNavController().navigate(R.id.action_results_to_question)
                }

                binding.tryAgainButton.setOnClickListener {
                    findNavController().navigate(R.id.action_results_to_question)
                }
            }
        }
    }
    private fun navigateToDishDetails(dishId: Int) {
        val bundle = bundleOf("dishId" to dishId)
        findNavController().navigate(R.id.action_results_to_details, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
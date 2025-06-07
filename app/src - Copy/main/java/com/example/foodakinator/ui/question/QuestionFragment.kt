package com.example.foodakinator.ui.question

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.foodakinator.R
import com.example.foodakinator.data.model.Question
import com.example.foodakinator.databinding.FragmentQuestionBinding

class QuestionFragment : Fragment() {
    private val TAG = "QuestionFragment"
    private var _binding: FragmentQuestionBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: QuestionViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuestionBinding.inflate(inflater, container, false)
        return binding.root
    }
    private fun displayQuestion(question: Question) {
        Log.d(TAG, "Displaying question: ${question.questionText}")
        binding.questionText.text = question.questionText

        // Clear previous answers
        binding.answersContainer.removeAllViews()

        // Display choices based on question type
        when (question.questionType) {
            "BINARY" -> {
                addAnswerButton("Yes") {
                    Log.d(TAG, "User selected: Yes for question ${question.id}")
                    viewModel.processAnswer(question.id, "Yes")
                }

                addAnswerButton("No") {
                    Log.d(TAG, "User selected: No for question ${question.id}")
                    viewModel.processAnswer(question.id, "No")
                }

                addAnswerButton("Don't Care") {
                    Log.d(TAG, "User selected: Don't Care for question ${question.id}")
                    viewModel.processAnswer(question.id, "Don't Care")
                }
            }

            "MULTIPLE_CHOICE" -> {
                // Parse the choices and create buttons for each
                val choices = question.choices.split(",")
                Log.d(TAG, "Creating ${choices.size} choice buttons: $choices")

                choices.forEach { choice ->
                    val trimmedChoice = choice.trim()
                    addAnswerButton(trimmedChoice) {
                        Log.d(TAG, "User selected: $trimmedChoice for question ${question.id}")
                        viewModel.processAnswer(question.id, trimmedChoice)
                    }
                }
            }

            else -> {
                Log.e(TAG, "Unknown question type: ${question.questionType}")
                // Fallback - treat as binary
                addAnswerButton("Yes") {
                    viewModel.processAnswer(question.id, "Yes")
                }
                addAnswerButton("No") {
                    viewModel.processAnswer(question.id, "No")
                }
            }
        }
    }
    private fun addAnswerButton(text: String, clickListener: () -> Unit) {
        val button = Button(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 16)
            }
            this.text = text
            setOnClickListener {
                Log.d(TAG, "Answer button clicked: $text")
                clickListener()
            }
        }
        binding.answersContainer.addView(button)
    }

    // Replace your onViewCreated in QuestionFragment with this:

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(TAG, "onViewCreated called")

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[QuestionViewModel::class.java]

        // Set up observers
        viewModel.currentQuestion.observe(viewLifecycleOwner) { question ->
            Log.d(TAG, "Question updated: $question")
            if (question != null) {
                displayQuestion(question)
            } else {
                Log.e(TAG, "Received null question")
                binding.questionText.text = "Error loading question"
            }
        }

        viewModel.questionProgress.observe(viewLifecycleOwner) { progress ->
            Log.d(TAG, "Progress updated: $progress")
            val (current, total) = progress
            binding.questionProgress.text = "Question $current/$total"
        }

        viewModel.hasRecommendation.observe(viewLifecycleOwner) { hasRecommendation ->
            Log.d(TAG, "Has recommendation: $hasRecommendation")
            if (hasRecommendation) {
                // Save recommendations before navigating
                viewModel.saveRecommendations()
                findNavController().navigate(R.id.action_question_to_results)
            }
        }

        // NEW: Check if we're continuing or starting fresh
        viewModel.checkIfContinuing()

        // Keep your existing button if it exists
        binding.goToResultsButton?.setOnClickListener {
            Log.d(TAG, "Go to results button clicked")
            findNavController().navigate(R.id.action_question_to_results)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
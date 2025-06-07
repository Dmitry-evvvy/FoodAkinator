package com.example.foodakinator.ui.question

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.foodakinator.data.database.FoodAkinatorDatabase
import com.example.foodakinator.data.model.Question
import com.example.foodakinator.engine.DishRecommender
import com.example.foodakinator.util.Constants
import com.example.foodakinator.util.PerformanceMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuestionViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "QuestionViewModel"

    // Database access
    private val database = FoodAkinatorDatabase.getDatabase(application)

    // Recommendation engine
    private val recommender by lazy { DishRecommender(database) }
    private val performanceMonitor = PerformanceMonitor()

    // LiveData for UI
    private val _currentQuestion = MutableLiveData<Question?>()
    val currentQuestion: LiveData<Question?> = _currentQuestion

    private val _questionProgress = MutableLiveData<Pair<Int, Int>>()
    val questionProgress: LiveData<Pair<Int, Int>> = _questionProgress

    private val _hasRecommendation = MutableLiveData<Boolean>()
    val hasRecommendation: LiveData<Boolean> = _hasRecommendation

    // Question counters
    private var questionsAsked = 0

    init {
        Log.d(TAG, "ViewModel initialized")
        // Initialize recommender with performance monitoring
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting to initialize recommender")

                performanceMonitor.measureTime("database_question_count") {
                    val questionCount = withContext(Dispatchers.IO) {
                        database.questionDao().getCount()
                    }
                    Log.d(TAG, "Database has $questionCount questions")
                }

                performanceMonitor.measureTime("recommender_initialization") {
                    withContext(Dispatchers.IO) {
                        recommender.initializeScores()
                    }
                }
                Log.d(TAG, "Recommender initialized")

                // Load first question with performance monitoring
                loadNextQuestion()
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing recommender", e)
            }
        }
    }

    // NEW: Function to handle "Continue" action
    fun continueAsking() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "User wants to continue asking questions")

                // Get current top dish ID from holder
                val topDishId = RecommendationHolder.getCurrentTopDishId()

                if (topDishId != null) {
                    Log.d(TAG, "Rejecting dish ID: $topDishId")

                    // Add to excluded dishes
                    RecommendationHolder.addRejectedDish(topDishId)

                    val canContinue = performanceMonitor.measureTime("reject_and_continue") {
                        withContext(Dispatchers.IO) {
                            // Reject the specific dish
                            recommender.addToExcluded(topDishId)

                            // Check if we can continue
                            recommender.getAvailableDishCount() > 0
                        }
                    }

                    if (canContinue) {
                        val remaining = withContext(Dispatchers.IO) {
                            recommender.getAvailableDishCount()
                        }
                        Log.d(TAG, "Continuing with $remaining dishes remaining")

                        // Reset for new round but keep existing state
                        recommender.resetForNextRound()

                        // Load next question
                        loadNextQuestion()
                    } else {
                        Log.d(TAG, "No dishes remaining - showing final results")
                        _hasRecommendation.value = true
                    }
                } else {
                    Log.e(TAG, "No top dish ID to reject")
                    _hasRecommendation.value = true
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error continuing", e)
                initializeNormally()
            }
        }
    }

    // NEW: Get current session info for UI
    fun getSessionInfo(): String {
        val (round, total, excluded) = recommender.getCurrentRoundInfo()
        val available = recommender.getAvailableDishCount()
        return "Round questions: $round | Total: $total | Available dishes: $available"
    }

    // UPDATED: Save recommendations with session info
    fun saveRecommendations() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Saving recommendations")
                val topDishes = withContext(Dispatchers.IO) {
                    recommender.getTopRecommendations(5)
                }

                Log.d(TAG, "Got ${topDishes.size} top dishes for saving")

                // Get session summary
                val (round, total, excluded) = withContext(Dispatchers.IO) {
                    recommender.getCurrentRoundInfo()
                }
                val available = withContext(Dispatchers.IO) {
                    recommender.getAvailableDishCount()
                }

                // Get detailed session state
                val askedQuestions = withContext(Dispatchers.IO) {
                    recommender.getAskedQuestions()
                }
                val userAnswers = withContext(Dispatchers.IO) {
                    recommender.getUserAnswers()
                }
                val rejectedDishes = withContext(Dispatchers.IO) {
                    recommender.getExcludedDishes()
                }

                Log.d(TAG, "Session Summary - Round: $round, Total Questions: $total, Excluded: $excluded, Available: $available")

                // Save recommendations and session info
                RecommendationHolder.setRecommendations(topDishes)
                RecommendationHolder.setSessionInfo(total, excluded, available)

                // NEW: Save detailed session state
                RecommendationHolder.setDetailedSessionState(
                    round, askedQuestions, userAnswers, rejectedDishes
                )

                Log.d(TAG, "All data and session state saved to holder")

            } catch (e: Exception) {
                Log.e(TAG, "Error saving recommendations", e)
            }
        }
    }

    fun loadNextQuestion() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading next question (asked so far: $questionsAsked)")

                // Check if max questions reached
                if (questionsAsked >= Constants.MAX_QUESTIONS) {
                    Log.d(TAG, "Max questions reached, showing results")
                    _hasRecommendation.value = true
                    return@launch
                }

                // Get next question with performance monitoring
                val nextQuestion = performanceMonitor.measureTime("get_next_question") {
                    withContext(Dispatchers.IO) {
                        // For debugging, try to get ALL questions first
                        val allQuestions = database.questionDao().getAllQuestions()
                        Log.d(TAG, "All questions in database: ${allQuestions.size}")
                        if (allQuestions.isNotEmpty()) {
                            Log.d(TAG, "Sample question: ${allQuestions[0].questionText}")
                        }

                        // Now get the next question from recommender
                        recommender.getNextQuestion()
                    }
                }

                if (nextQuestion != null) {
                    Log.d(TAG, "Loaded question: ${nextQuestion.questionText}")
                    questionsAsked++
                    _currentQuestion.value = nextQuestion
                    _questionProgress.value = Pair(questionsAsked, Constants.MAX_QUESTIONS)
                } else {
                    Log.d(TAG, "No question returned from recommender!")

                    // As a fallback, get the first question directly
                    val firstQuestion = withContext(Dispatchers.IO) {
                        val questions = database.questionDao().getAllQuestions()
                        questions.firstOrNull()
                    }

                    if (firstQuestion != null) {
                        Log.d(TAG, "Using first question from database as fallback")
                        questionsAsked++
                        _currentQuestion.value = firstQuestion
                        _questionProgress.value = Pair(questionsAsked, Constants.MAX_QUESTIONS)
                    } else {
                        Log.d(TAG, "No questions available at all, showing results")
                        _hasRecommendation.value = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading next question", e)
            }
        }
    }

    private suspend fun initializeNormally() {
        withContext(Dispatchers.IO) {
            recommender.reset()
            recommender.initializeScores()
        }
        performanceMonitor.clearMetrics() // NEW: Clear metrics on reset
        loadNextQuestion()
    }

    fun getPerformanceMetrics(): Map<String, Long> {
        return performanceMonitor.getAllMetrics()
    }

    // NEW: Add method to check for slow operations
    fun checkPerformance() {
        val slowOps = performanceMonitor.getSlowOperations(50)
        if (slowOps.isNotEmpty()) {
            Log.w(TAG, "Slow operations detected: $slowOps")
        }
    }

    fun processAnswer(questionId: Int, answer: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "ViewModel processing answer: Question $questionId = $answer")

                // Process answer with performance monitoring
                performanceMonitor.measureTime("process_answer") {
                    withContext(Dispatchers.IO) {
                        recommender.processAnswer(questionId, answer)
                    }
                }

                // Check if we have confident recommendation with monitoring
                val isConfident = performanceMonitor.measureTime("confidence_check") {
                    withContext(Dispatchers.IO) {
                        recommender.hasConfidentRecommendation()
                    }
                }

                if (isConfident) {
                    Log.d(TAG, "Have confident recommendation, saving and navigating")

                    // FIRST: Save all the data with performance monitoring
                    val topDishes = performanceMonitor.measureTime("get_recommendations") {
                        withContext(Dispatchers.IO) {
                            recommender.getTopRecommendations(5)
                        }
                    }

                    // Get session info
                    val (round, total, excluded) = withContext(Dispatchers.IO) {
                        recommender.getCurrentRoundInfo()
                    }
                    val available = withContext(Dispatchers.IO) {
                        recommender.getAvailableDishCount()
                    }

                    Log.d(TAG, "Session Summary - Round: $round, Total Questions: $total, Excluded: $excluded, Available: $available")

                    // Save everything at once
                    RecommendationHolder.setRecommendations(topDishes)
                    RecommendationHolder.setSessionInfo(total, excluded, available)

                    Log.d(TAG, "All data saved to holder")

                    // Log performance report
                    performanceMonitor.logPerformanceReport()

                    // Small delay to ensure everything is saved
                    delay(50)

                    // THEN: Trigger navigation
                    _hasRecommendation.value = true
                } else {
                    loadNextQuestion()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing answer", e)
            }
        }
    }

    private var isFromResults = false

    // Call this when fragment loads to detect if coming from results
    fun checkIfContinuing() {
        viewModelScope.launch {
            try {
                val shouldContinue = RecommendationHolder.shouldContinueAsking()
                val totalQuestions = RecommendationHolder.getTotalQuestions()
                val availableCount = RecommendationHolder.getAvailableCount()

                Log.d(TAG, "Continue check - ShouldContinue: $shouldContinue, Total: $totalQuestions, Available: $availableCount")

                if (shouldContinue && totalQuestions > 0 && availableCount > 0) {
                    Log.d(TAG, "CONTINUING from previous session")

                    // Clear the flag so we don't continue again accidentally
                    RecommendationHolder.clearContinueFlag()

                    // Get the detailed session state
                    val sessionState = RecommendationHolder.getDetailedSessionState()

                    // IMPORTANT: Restore the session state before rejecting
                    val success = withContext(Dispatchers.IO) {
                        restoreSessionState(sessionState)
                    }

                    if (success) {
                        // Now reject the current top recommendation and continue
                        continueAsking()
                    } else {
                        Log.e(TAG, "Failed to restore session state, starting fresh")
                        initializeNormally()
                    }
                } else {
                    Log.d(TAG, "STARTING FRESH session")
                    initializeNormally()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking continue state", e)
                initializeNormally()
            }
        }
    }

    // NEW: Method to restore session state
    private suspend fun restoreSessionState(sessionState: SessionState): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                // Initialize the recommender first
                recommender.reset()
                recommender.initializeScores()

                // Restore the excluded dishes
                sessionState.rejectedDishIds.forEach { dishId ->
                    // Add to excluded set in recommender
                    recommender.addToExcluded(dishId)
                }

                // Restore user answers and update scores
                sessionState.userAnswers.forEach { (attribute, answer) ->
                    // You'll need to add this method to DishRecommender
                    recommender.restoreAnswer(attribute, answer)
                }

                // Set the asked questions
                sessionState.askedQuestionIds.forEach { questionId ->
                    recommender.markQuestionAsAsked(questionId)
                }

                // Set round info
                val (round, total, excluded) = recommender.getCurrentRoundInfo()
                questionsAsked = sessionState.roundQuestions

                Log.d(TAG, "Restored session: Round=$round, Total=$total, Excluded=$excluded, QuestionsAsked=$questionsAsked")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring session state", e)
            false
        }
    }
}
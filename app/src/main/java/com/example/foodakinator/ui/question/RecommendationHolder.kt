package com.example.foodakinator.ui.question

import com.example.foodakinator.data.model.Dish

object RecommendationHolder {
    private var recommendations = listOf<Dish>()
    private var totalQuestions = 0
    private var excludedDishes = 0
    private var availableDishes = 0
    private var canContinue = true
    private var shouldContinue = false

    // NEW: Store the actual session state
    private var currentRoundQuestions = 0
    private var askedQuestionIds = mutableSetOf<Int>()
    private var userAnswers = mutableMapOf<String, String>()
    private var rejectedDishIds = mutableSetOf<Int>()
    private var currentTopDishId: Int? = null

    fun setRecommendations(dishes: List<Dish>) {
        recommendations = dishes
        // Store the current top dish for rejection
        currentTopDishId = dishes.firstOrNull()?.id
    }

    fun getRecommendations(): List<Dish> {
        return recommendations
    }

    fun setSessionInfo(total: Int, excluded: Int, available: Int) {
        totalQuestions = total
        excludedDishes = excluded
        availableDishes = available
        canContinue = available > 1 && total < 50
    }

    // NEW: Store detailed session state
    fun setDetailedSessionState(
        roundQuestions: Int,
        askedIds: Set<Int>,
        answers: Map<String, String>,
        rejectedIds: Set<Int>
    ) {
        currentRoundQuestions = roundQuestions
        askedQuestionIds = askedIds.toMutableSet()
        userAnswers = answers.toMutableMap()
        rejectedDishIds = rejectedIds.toMutableSet()
    }

    fun getDetailedSessionState(): SessionState {
        return SessionState(
            currentRoundQuestions,
            askedQuestionIds,
            userAnswers,
            rejectedDishIds,
            currentTopDishId
        )
    }

    fun getSessionInfo(): String {
        return "Questions asked: $totalQuestions | Dishes excluded: $excludedDishes | Remaining: $availableDishes"
    }

    fun canContinueAsking(): Boolean {
        return canContinue && availableDishes > 1 && totalQuestions < 50
    }

    fun getTotalQuestions(): Int = totalQuestions
    fun getExcludedCount(): Int = excludedDishes
    fun getAvailableCount(): Int = availableDishes

    // Continue flag methods
    fun setContinueFlag(continueFlag: Boolean) {
        shouldContinue = continueFlag
    }

    fun shouldContinueAsking(): Boolean {
        return shouldContinue
    }

    fun clearContinueFlag() {
        shouldContinue = false
    }

    // NEW: Add rejected dish to the set
    fun addRejectedDish(dishId: Int) {
        rejectedDishIds.add(dishId)
        currentTopDishId?.let { topId ->
            if (topId == dishId) {
                currentTopDishId = null
            }
        }
    }

    fun getCurrentTopDishId(): Int? = currentTopDishId
}

// NEW: Data class to hold session state
data class SessionState(
    val roundQuestions: Int,
    val askedQuestionIds: Set<Int>,
    val userAnswers: Map<String, String>,
    val rejectedDishIds: Set<Int>,
    val currentTopDishId: Int?
)
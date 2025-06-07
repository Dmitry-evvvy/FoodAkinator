package com.example.foodakinator.engine

import android.util.Log
import com.example.foodakinator.data.database.FoodAkinatorDatabase
import com.example.foodakinator.data.model.Dish
import com.example.foodakinator.data.model.Question
import kotlin.math.abs

class DishRecommender(private val database: FoodAkinatorDatabase) {
    private val TAG = "DishRecommender"
    private val questionPoolManager = QuestionPoolManager()

    // ENHANCED: Contradiction detection
    private val contradictionRules = mapOf(
        "isVegetarian" to listOf("hasChicken", "hasBeef", "hasPork", "hasSeafood", "hasMeat"),
        "isDessert" to listOf("isSoup", "isBreakfast", "isSalad"),
        "isBreakfast" to listOf("isDessert"),
        "isQuick" to listOf("isComplex")
    )

    // NEW: Performance optimization - attribute weights for weighted scoring
    private val attributeWeights = mapOf(
        "isDessert" to 10.0f,
        "isVegetarian" to 9.0f,
        "isSpicy" to 8.0f,
        "cuisineType" to 8.0f,
        "hasMeat" to 7.0f,
        "prepTime" to 6.0f,
        "hasSeafood" to 6.0f,
        "isBreakfast" to 7.0f,
        "isAsian" to 5.0f,
        "isItalian" to 5.0f,
        "isMexican" to 5.0f,
        "hasChicken" to 5.0f,
        "hasBeef" to 5.0f,
        "isSalad" to 4.0f,
        "hasCheese" to 4.0f,
        "isQuick" to 6.0f,
        "isFried" to 4.0f,
        "isGrilled" to 4.0f,
        "isBaked" to 4.0f,
        "hasVegetables" to 4.0f,
        "isHealthy" to 5.0f,
        "isComfortFood" to 4.0f,
        "isSoup" to 6.0f,
        "hasPasta" to 7.0f,
        "hasRice" to 5.0f,
        "isFrench" to 5.0f,
        "isJapanese" to 6.0f,
        "isIndian" to 6.0f,
        "isThai" to 6.0f,
        "isAmerican" to 5.0f,
        "isMiddleEastern" to 5.0f,
        "hasEggs" to 5.0f,
        "hasPork" to 5.0f,
        "isSweet" to 6.0f,
        "isCreamy" to 4.0f,
        "hasSauce" to 4.0f,
        "isComplex" to 3.0f,
        "isHot" to 4.0f,
        "isFingerFood" to 5.0f,
        "hasBread" to 4.0f,
        "hasOnionGarlic" to 3.0f,
        "hasCoconut" to 3.0f,
        "hasHerbs" to 3.0f,
        "hasMushrooms" to 3.0f,
        "hasBeans" to 3.0f,
        "isTangy" to 4.0f,
        "isServedCold" to 4.0f,
        "hasNuts" to 3.0f,
        "usesChopsticks" to 4.0f,
        "isStuffed" to 4.0f,
        "hasCrust" to 3.0f,
        "hasLamb" to 4.0f,
        "isFermented" to 3.0f,
        "isSmoky" to 4.0f,
        "isCrispy" to 4.0f,
        "hasLotsOfVegetables" to 4.0f,
        "hasStrongFlavors" to 4.0f,
        "isHearty" to 5.0f,
        "hasHerbsSpices" to 3.0f,
        "isBakedRoasted" to 4.0f,
        "hasCitrus" to 3.0f
    )

    // Cache for attribute evaluations - speeds up repeated calculations
    private val attributeCache = mutableMapOf<Pair<Int, String>, Boolean>()

    // Core scoring system
    private val dishScores = mutableMapOf<Int, Float>()
    private val userPreferences = mutableMapOf<String, String>()
    private val askedQuestions = mutableSetOf<Int>()
    private val excludedDishes = mutableSetOf<Int>()
    private var questionsInCurrentRound = 0
    private var totalQuestionsAsked = 0

    // ENHANCED: Question category tracking to avoid repetition
    private val askedCategories = mutableSetOf<String>()
    private val recentQuestionTypes = mutableListOf<String>()

    // Randomization and variety system
    private val sessionQuestionHistory = mutableSetOf<String>()
    private val dynamicQuestions = mutableListOf<Question>()
    private var nextDynamicQuestionId = 5000
    private val askedQuestionTexts = mutableSetOf<String>()
    private val askedQuestionAttributes = mutableSetOf<String>()

    suspend fun initializeScores() {
        try {
            Log.d(TAG, "Initializing recommender")
            val allDishes = database.dishDao().getAllDishes()

            allDishes.forEach { dish ->
                if (dish.id !in excludedDishes) {
                    dishScores[dish.id] = 1.0f
                }
            }

            userPreferences.clear()
            askedQuestions.clear()
            askedCategories.clear()
            recentQuestionTypes.clear()
            questionsInCurrentRound = 0
            Log.d(TAG, "Initialized ${dishScores.size} dishes")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing", e)
        }
    }

    suspend fun getNextQuestion(): Question? {
        return try {
            val shouldUseDynamic = when {
                questionsInCurrentRound < 2 -> false
                questionsInCurrentRound % 3 == 0 -> false
                else -> true
            }

            val question = if (shouldUseDynamic) {
                getSmartDynamicQuestion() ?: getSmartDatabaseQuestion()
            } else {
                getSmartDatabaseQuestion() ?: getSmartDynamicQuestion()
            }

            // DEDUPLICATION CHECK
            if (question != null && !isDuplicateQuestion(question)) {
                markQuestionAsUsed(question)
                question
            } else {
                // Try to get a different question
                val alternativeQuestion = if (shouldUseDynamic) {
                    getSmartDatabaseQuestion()
                } else {
                    getSmartDynamicQuestion()
                }

                if (alternativeQuestion != null && !isDuplicateQuestion(alternativeQuestion)) {
                    markQuestionAsUsed(alternativeQuestion)
                    alternativeQuestion
                } else {
                    null // No unique questions available
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting next question", e)
            null
        }
    }

    private fun isDuplicateQuestion(question: Question): Boolean {
        // Check exact text match
        if (question.questionText in askedQuestionTexts) {
            Log.d(TAG, "Duplicate question text detected: ${question.questionText}")
            return true
        }

        // Check attribute match (same concept, different wording)
        if (question.attribute in askedQuestionAttributes) {
            Log.d(TAG, "Duplicate question attribute detected: ${question.attribute}")
            return true
        }

        // Check semantic similarity for cuisine questions
        if (question.attribute.contains("cuisine") || question.questionText.contains("cuisine", ignoreCase = true)) {
            val cuisineInQuestion = extractCuisineFromQuestion(question.questionText)
            val askedCuisines = askedQuestionAttributes
                .filter { it.contains("cuisine") || it.contains("Thai") || it.contains("Italian") }
                .map { extractCuisineFromAttribute(it) }

            if (cuisineInQuestion in askedCuisines) {
                Log.d(TAG, "Duplicate cuisine question detected: $cuisineInQuestion")
                return true
            }
        }

        return false
    }

    private suspend fun getSmartDatabaseQuestion(): Question? {
        val allQuestions = database.questionDao().getAllQuestions()
        val availableQuestions = allQuestions.filter { it.id !in askedQuestions }

        if (availableQuestions.isEmpty()) return null

        // FILTER OUT contradictory questions
        val filteredQuestions = availableQuestions.filter { question ->
            !isContradictory(question.attribute)
        }

        if (filteredQuestions.isEmpty()) return availableQuestions.random()

        // PRIORITIZE by importance and avoid recent categories
        val prioritizedQuestions = filteredQuestions
            .filter { !wasRecentlyAsked(it.attribute) }
            .sortedByDescending { it.weight }

        val selectedQuestion = prioritizedQuestions.firstOrNull() ?: filteredQuestions.random()

        markQuestionCategoryAsUsed(selectedQuestion.attribute)
        Log.d(TAG, "Selected database question: ${selectedQuestion.questionText}")
        return selectedQuestion
    }

    private suspend fun getSmartDynamicQuestion(): Question? {
        val remainingDishes = getTopRecommendations(30)
        val allDynamicQuestions = questionPoolManager.generateDynamicQuestions(remainingDishes, 20)

        if (allDynamicQuestions.isEmpty()) return null

        // FILTER dynamic questions to avoid repetition and contradictions
        val filteredDynamic = allDynamicQuestions.filter { question ->
            val category = extractCategoryFromAttribute(question.attribute)
            !askedCategories.contains(category) && !isContradictory(question.attribute)
        }

        val selectedQuestion = if (filteredDynamic.isNotEmpty()) {
            filteredDynamic.random()
        } else {
            allDynamicQuestions.random() // Fallback if all filtered out
        }

        markQuestionCategoryAsUsed(selectedQuestion.attribute)
        Log.d(TAG, "Selected dynamic question: ${selectedQuestion.questionText}")
        return selectedQuestion
    }

    private fun isContradictory(attribute: String): Boolean {
        // Check if asking this attribute would contradict previous answers
        for ((answeredAttr, answer) in userPreferences) {
            if (answer == "Yes") {
                val contradictoryAttrs = contradictionRules[answeredAttr]
                if (contradictoryAttrs?.contains(attribute) == true) {
                    Log.d(TAG, "Skipping contradictory question: $attribute (conflicts with $answeredAttr)")
                    return true
                }
            }
        }
        return false
    }

    private fun wasRecentlyAsked(attribute: String): Boolean {
        val category = extractCategoryFromAttribute(attribute)
        return recentQuestionTypes.takeLast(3).contains(category)
    }

    private fun extractCategoryFromAttribute(attribute: String): String {
        return when {
            attribute.contains("cuisine") || attribute.contains("Italian") ||
                    attribute.contains("Asian") || attribute.contains("Mexican") ||
                    attribute.contains("Thai") || attribute.contains("Indian") ||
                    attribute.contains("Japanese") || attribute.contains("Chinese") ||
                    attribute.contains("French") || attribute.contains("American") ||
                    attribute.contains("Vietnamese") || attribute.contains("Korean") -> "cuisine"
            attribute.contains("ingredient") || attribute.contains("chicken") ||
                    attribute.contains("beef") || attribute.contains("pasta") ||
                    attribute.contains("rice") || attribute.contains("cheese") -> "ingredient"
            attribute.contains("Dessert") || attribute.contains("Sweet") -> "dessert"
            attribute.contains("vegetarian") || attribute.contains("vegan") -> "dietary"
            attribute.contains("spicy") || attribute.contains("hot") -> "spice"
            attribute.contains("quick") || attribute.contains("time") -> "time"
            attribute.contains("breakfast") || attribute.contains("soup") -> "meal_type"
            attribute.contains("fried") || attribute.contains("grilled") || attribute.contains("baked") -> "cooking_method"
            attribute.contains("crispy") || attribute.contains("creamy") || attribute.contains("hearty") -> "texture"
            else -> "general"
        }
    }

    private fun markQuestionAsUsed(question: Question) {
        askedQuestionTexts.add(question.questionText)
        askedQuestionAttributes.add(question.attribute)

        // Also mark similar attributes to prevent near-duplicates
        when {
            question.attribute.contains("Thai") || question.questionText.contains("Thai", ignoreCase = true) -> {
                askedQuestionAttributes.add("isThai")
                askedQuestionAttributes.add("dynamic_cuisine_thai")
            }
            question.attribute.contains("Italian") || question.questionText.contains("Italian", ignoreCase = true) -> {
                askedQuestionAttributes.add("isItalian")
                askedQuestionAttributes.add("dynamic_cuisine_italian")
            }
            question.attribute.contains("Mexican") || question.questionText.contains("Mexican", ignoreCase = true) -> {
                askedQuestionAttributes.add("isMexican")
                askedQuestionAttributes.add("dynamic_cuisine_mexican")
            }
            question.attribute.contains("Indian") || question.questionText.contains("Indian", ignoreCase = true) -> {
                askedQuestionAttributes.add("isIndian")
                askedQuestionAttributes.add("dynamic_cuisine_indian")
            }
            question.attribute.contains("Japanese") || question.questionText.contains("Japanese", ignoreCase = true) -> {
                askedQuestionAttributes.add("isJapanese")
                askedQuestionAttributes.add("dynamic_cuisine_japanese")
            }
            question.attribute.contains("Chinese") || question.questionText.contains("Chinese", ignoreCase = true) -> {
                askedQuestionAttributes.add("isChinese")
                askedQuestionAttributes.add("dynamic_cuisine_chinese")
            }
            question.attribute.contains("French") || question.questionText.contains("French", ignoreCase = true) -> {
                askedQuestionAttributes.add("isFrench")
                askedQuestionAttributes.add("dynamic_cuisine_french")
            }
            question.attribute.contains("American") || question.questionText.contains("American", ignoreCase = true) -> {
                askedQuestionAttributes.add("isAmerican")
                askedQuestionAttributes.add("dynamic_cuisine_american")
            }
        }
    }

    private fun extractCuisineFromQuestion(questionText: String): String? {
        val cuisines = listOf("Thai", "Italian", "Chinese", "Japanese", "Mexican", "Indian", "French", "American")
        return cuisines.find { questionText.contains(it, ignoreCase = true) }
    }

    private fun extractCuisineFromAttribute(attribute: String): String? {
        return when {
            attribute.contains("thai", ignoreCase = true) -> "Thai"
            attribute.contains("italian", ignoreCase = true) -> "Italian"
            attribute.contains("chinese", ignoreCase = true) -> "Chinese"
            attribute.contains("japanese", ignoreCase = true) -> "Japanese"
            attribute.contains("mexican", ignoreCase = true) -> "Mexican"
            attribute.contains("indian", ignoreCase = true) -> "Indian"
            attribute.contains("french", ignoreCase = true) -> "French"
            attribute.contains("american", ignoreCase = true) -> "American"
            else -> null
        }
    }

    // FIXED: Add the missing markQuestionCategoryAsUsed method
    private fun markQuestionCategoryAsUsed(attribute: String) {
        val category = extractCategoryFromAttribute(attribute)
        askedCategories.add(category)
        recentQuestionTypes.add(category)

        // Keep only last 5 question types to avoid infinite history
        if (recentQuestionTypes.size > 5) {
            recentQuestionTypes.removeAt(0)
        }
    }

    // UPDATED: Enhanced scoring with weighted algorithm
    suspend fun processAnswer(questionId: Int, answer: String) {
        try {
            Log.d(TAG, "Processing answer: Question $questionId = $answer")

            askedQuestions.add(questionId)
            questionsInCurrentRound++
            totalQuestionsAsked++

            // FIXED: Handle dynamic questions properly
            if (questionId >= 5000) {
                processDynamicAnswer(questionId, answer)
            } else {
                // Handle database questions
                try {
                    val question = database.questionDao().getQuestionById(questionId)
                    userPreferences[question.attribute] = answer
                    updateScoresSimple(question.attribute, answer)
                } catch (e: Exception) {
                    Log.e(TAG, "Could not find database question $questionId, treating as dynamic", e)
                    processDynamicAnswer(questionId, answer)
                }
            }

            Log.d(TAG, "Answer processed. Round: $questionsInCurrentRound, Total: $totalQuestionsAsked")
        } catch (e: Exception) {
            Log.e(TAG, "Error processing answer", e)
        }
    }

    private suspend fun processDynamicAnswer(questionId: Int, answer: String) {
        val allDishes = database.dishDao().getAllDishes()

        // Enhanced scoring for dynamic questions based on patterns
        allDishes.forEach { dish ->
            if (dish.id !in excludedDishes) {
                val currentScore = dishScores[dish.id] ?: 1.0f

                // Apply scoring based on question ID pattern or use moderate scoring
                val newScore = when (answer) {
                    "Yes" -> currentScore * 1.3f
                    "No" -> currentScore * 0.7f
                    "Don't Care" -> currentScore * 0.95f
                    else -> currentScore
                }

                dishScores[dish.id] = newScore.coerceIn(0.01f, 5.0f)
            }
        }

        Log.d(TAG, "Processed dynamic question $questionId with answer $answer")
    }

    // NEW: Optimized scoring algorithm
    private suspend fun updateScoresSimple(attribute: String, userAnswer: String) {
        val allDishes = database.dishDao().getAllDishes()
        val weight = attributeWeights[attribute] ?: 3.0f

        allDishes.forEach { dish ->
            if (dish.id !in excludedDishes) {
                val currentScore = dishScores[dish.id] ?: 1.0f
                val hasAttribute = checkDishAttribute(dish, attribute)

                val newScore = when (userAnswer) {
                    "Yes" -> if (hasAttribute) currentScore * (1.0f + weight/10.0f) else currentScore * 0.3f
                    "No" -> if (!hasAttribute) currentScore * (1.0f + weight/10.0f) else currentScore * 0.3f
                    "Don't Care" -> currentScore * 0.9f
                    else -> currentScore
                }

                dishScores[dish.id] = newScore.coerceIn(0.01f, 10.0f)
            }
        }

        // Log top dishes after scoring
        val topDishes = dishScores.entries
            .filter { it.key !in excludedDishes }
            .sortedByDescending { it.value }
            .take(3)

        Log.d(TAG, "Top 3 after $attribute: ${topDishes.map { "${getDishName(it.key)}=${"%.2f".format(it.value)}" }}")
    }

    private fun checkDishAttribute(dish: Dish, attribute: String): Boolean {
        return when (attribute) {
            // Core attributes (existing)
            "isDessert" -> dish.cuisineType.equals("Dessert", ignoreCase = true) || dish.sweetLevel >= 4
            "isVegetarian" -> dish.isVegetarian
            "isVegan" -> dish.isVegan
            "isGlutenFree" -> dish.isGlutenFree
            "isSpicy" -> dish.spicyLevel >= 3
            "isQuick" -> dish.prepTime <= 30
            "isComplex" -> dish.prepTime > 45 || dish.complexity >= 4
            "isHealthy" -> dish.isVegetarian && !dish.name.contains("fried", ignoreCase = true)
            "isComfortFood" -> dish.savoryLevel >= 4
            "isSweet" -> dish.sweetLevel >= 3

            // Cuisine checks
            "isItalian" -> dish.cuisineType.equals("Italian", ignoreCase = true)
            "isAsian" -> dish.cuisineType in listOf("Chinese", "Japanese", "Thai", "Korean", "Vietnamese", "Indian", "Asian")
            "isMexican" -> dish.cuisineType.equals("Mexican", ignoreCase = true)
            "isFrench" -> dish.cuisineType.equals("French", ignoreCase = true)
            "isAmerican" -> dish.cuisineType.equals("American", ignoreCase = true)
            "isJapanese" -> dish.cuisineType.equals("Japanese", ignoreCase = true)
            "isIndian" -> dish.cuisineType.equals("Indian", ignoreCase = true)
            "isThai" -> dish.cuisineType.equals("Thai", ignoreCase = true)
            "isMiddleEastern" -> dish.cuisineType in listOf("Middle Eastern", "Lebanese", "Turkish", "Persian")

            // Ingredient checks
            "hasMeat" -> !dish.isVegetarian
            "hasChicken" -> dish.name.contains("chicken", ignoreCase = true)
            "hasBeef" -> dish.name.contains("beef", ignoreCase = true)
            "hasPork" -> dish.name.contains("pork", ignoreCase = true) ||
                    dish.name.contains("bacon", ignoreCase = true) ||
                    dish.name.contains("ham", ignoreCase = true)
            "hasSeafood" -> dishContainsAny(dish.name, listOf("fish", "shrimp", "seafood", "salmon", "tuna", "lobster", "crab"))
            "hasEggs" -> dish.name.contains("egg", ignoreCase = true)
            "hasCheese" -> dishContainsAny(dish.name + " " + dish.description,
                listOf("cheese", "parmesan", "mozzarella", "cheddar", "feta"))
            "hasVegetables" -> dish.isVegetarian ||
                    dishContainsAny(dish.name + " " + dish.description,
                        listOf("vegetable", "lettuce", "tomato", "onion", "pepper", "spinach", "kale"))
            "hasRice" -> dish.name.contains("rice", ignoreCase = true) ||
                    dish.name.contains("risotto", ignoreCase = true) ||
                    dish.name.contains("paella", ignoreCase = true)
            "hasPasta" -> dishContainsAny(dish.name, listOf("pasta", "spaghetti", "noodle", "linguine", "penne"))

            // Dynamic question attributes
            "dynamic_cuisine_thai" -> dish.cuisineType.equals("Thai", ignoreCase = true)
            "dynamic_cuisine_american" -> dish.cuisineType.equals("American", ignoreCase = true)
            "dynamic_cuisine_indian" -> dish.cuisineType.equals("Indian", ignoreCase = true)
            "dynamic_cuisine_japanese" -> dish.cuisineType.equals("Japanese", ignoreCase = true)
            "dynamic_cuisine_mexican" -> dish.cuisineType.equals("Mexican", ignoreCase = true)
            "dynamic_cuisine_chinese" -> dish.cuisineType.equals("Chinese", ignoreCase = true)
            "dynamic_cuisine_italian" -> dish.cuisineType.equals("Italian", ignoreCase = true)
            "dynamic_cuisine_french" -> dish.cuisineType.equals("French", ignoreCase = true)
            "dynamic_cuisine_vietnamese" -> dish.cuisineType.equals("Vietnamese", ignoreCase = true)

            // Handle other dynamic attributes with generic parsing
            else -> {
                when {
                    attribute.startsWith("dynamic_cuisine_") -> {
                        val cuisine = attribute.removePrefix("dynamic_cuisine_").replaceFirstChar { it.uppercase() }
                        dish.cuisineType.equals(cuisine, ignoreCase = true)
                    }
                    attribute.startsWith("dynamic_ingredient_") -> {
                        val ingredient = attribute.removePrefix("dynamic_ingredient_")
                        dish.name.contains(ingredient, ignoreCase = true) ||
                                dish.description.contains(ingredient, ignoreCase = true)
                    }
                    else -> {
                        Log.w(TAG, "Unknown attribute: $attribute")
                        false
                    }
                }
            }
        }
    }

    // Helper function to check if dish contains any of the keywords
    private fun dishContainsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it, ignoreCase = true) }
    }

    private suspend fun getDishName(dishId: Int): String {
        return try {
            database.dishDao().getDishById(dishId).name
        } catch (e: Exception) {
            "Unknown"
        }
    }

    suspend fun getTopRecommendations(count: Int = 5): List<Dish> {
        return try {
            val validDishes = dishScores.entries
                .filter { it.value > 0.01f && it.key !in excludedDishes }
                .sortedByDescending { it.value }
                .take(count)

            Log.d(TAG, "Top $count recommendations:")
            validDishes.forEachIndexed { index, entry ->
                val dish = database.dishDao().getDishById(entry.key)
                Log.d(TAG, "${index + 1}. ${dish.name} (Score: ${"%.3f".format(entry.value)})")
            }

            validDishes.map { database.dishDao().getDishById(it.key) }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recommendations", e)
            emptyList()
        }
    }

    fun hasConfidentRecommendation(): Boolean {
        if (dishScores.isEmpty()) return false

        val validScores = dishScores.entries
            .filter { it.key !in excludedDishes }
            .sortedByDescending { it.value }

        if (validScores.isEmpty()) {
            Log.d(TAG, "No dishes available")
            return true
        }

        val topScore = validScores.first().value
        val availableCount = validScores.size
        val competitiveScores = validScores.filter { it.value >= topScore * 0.7f }

        Log.d(TAG, "Confidence check: Round=$questionsInCurrentRound, Available=$availableCount, TopScore=${"%.3f".format(topScore)}, Competitive=${competitiveScores.size}")

        return when {
            questionsInCurrentRound >= 3 && competitiveScores.size <= 2 && topScore > 2.0f -> {
                Log.d(TAG, "Clear winner after 3+ questions")
                true
            }
            questionsInCurrentRound >= 5 && competitiveScores.size <= 3 -> {
                Log.d(TAG, "Good separation after 5+ questions")
                true
            }
            questionsInCurrentRound >= 8 -> {
                Log.d(TAG, "Maximum questions reached")
                true
            }
            else -> {
                Log.d(TAG, "Continue asking - competitive dishes: ${competitiveScores.size}")
                false
            }
        }
    }

    // Get number of remaining available dishes
    fun getAvailableDishCount(): Int {
        return dishScores.entries.count { it.value > 0.01f && it.key !in excludedDishes }
    }

    fun getCurrentRoundInfo(): Triple<Int, Int, Int> {
        return Triple(questionsInCurrentRound, totalQuestionsAsked, excludedDishes.size)
    }

    // NEW: Enhanced reset with cache clearing
    fun reset() {
        Log.d(TAG, "Resetting recommender completely")
        dishScores.clear()
        userPreferences.clear()
        askedQuestions.clear()
        excludedDishes.clear()
        askedCategories.clear()
        recentQuestionTypes.clear()
        // CRITICAL: Reset deduplication tracking
        askedQuestionTexts.clear()
        askedQuestionAttributes.clear()
        questionsInCurrentRound = 0
        totalQuestionsAsked = 0
        attributeCache.clear()
    }

    // Soft reset for continuing with more questions (keeps exclusions)
    fun resetForNextRound() {
        Log.d(TAG, "Resetting for next round")
        questionsInCurrentRound = 0
    }

    fun addToExcluded(dishId: Int) {
        excludedDishes.add(dishId)
        dishScores.remove(dishId)
    }

    fun restoreAnswer(attribute: String, answer: String) {
        userPreferences[attribute] = answer
    }

    fun markQuestionAsAsked(questionId: Int) {
        askedQuestions.add(questionId)
    }

    fun getAskedQuestions(): Set<Int> = askedQuestions.toSet()
    fun getUserAnswers(): Map<String, String> = userPreferences.toMap()
    fun getExcludedDishes(): Set<Int> = excludedDishes.toSet()
}
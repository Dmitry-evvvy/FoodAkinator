package com.example.foodakinator.engine

import android.util.Log
import com.example.foodakinator.data.database.FoodAkinatorDatabase
import com.example.foodakinator.data.model.Dish
import com.example.foodakinator.data.model.Question
import com.example.foodakinator.engine.QuestionPoolManager
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
        "hasVegetables" to 4.0f,
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

    // NEW: Cache for attribute evaluations - speeds up repeated calculations
    //private val attributeCache = mutableMapOf<Pair<Int, String>, Boolean>()

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
    //private val askedQuestionAttributes = mutableSetOf<String>()
    // Question categorization for smart selection
    private val questionCategories = mapOf(
        "high_impact" to listOf("isDessert", "isVegetarian", "hasMeat", "isSpicy"),
        "cuisine" to listOf("isItalian", "isAsian", "isMexican", "isFrench", "isIndian", "isJapanese", "isAmerican", "isMiddleEastern"),
        "meal_type" to listOf("isBreakfast", "isSoup", "isSalad", "isSandwich"),
        "cooking_method" to listOf("isGrilled", "isFried", "isBaked", "isRaw"),
        "ingredients" to listOf("hasPasta", "hasRice", "hasCheese", "hasSeafood", "hasChicken", "hasBeef", "hasPork"),
        "dietary" to listOf("isHealthy", "isLowCarb", "isHighProtein", "isGlutenFree"),
        "mood" to listOf("isComfortFood", "isRefreshing", "isExotic", "isCelebration"),
        "practical" to listOf("isQuick", "isWorkFriendly", "isFingerFood"),
        "flavor" to listOf("isSweet", "isTangy", "isSmoky", "hasStrongFlavor"),
        "texture" to listOf("isCrispy", "isCreamy", "isHearty")
    )

    private suspend fun generateMoreQuestions(): List<Question> {
        val remainingDishes = getTopRecommendations(50)
        return questionPoolManager.generateDynamicQuestions(remainingDishes, 20)
    }

    // Dynamic question templates
    private val questionTemplates = mapOf(
        "cuisine_specific" to listOf(
            "Are you craving authentic {cuisine} flavors right now?",
            "Do you want something that reminds you of {cuisine} street food?",
            "Are you in the mood for traditional {cuisine} spices?"
        ),
        "ingredient_focused" to listOf(
            "Do you want something with {ingredient} as the main ingredient?",
            "Are you craving the taste of {ingredient} today?",
            "Would you like {ingredient} to be prominent in your meal?"
        ),
        "comparison" to listOf(
            "Would you prefer {option1} or {option2} style?",
            "Are you more in the mood for {option1} or {option2}?",
            "Between {option1} and {option2}, what sounds better?"
        )
    )

    // Data for dynamic question generation
    private val cuisineList = listOf(
        "Italian", "Chinese", "Japanese", "Mexican", "Indian", "Thai", "French", "Greek",
        "Korean", "Vietnamese", "Lebanese", "Turkish", "Spanish", "Moroccan", "Brazilian"
    )

    private val ingredientList = listOf(
        "garlic", "chicken", "beef", "fish", "cheese", "rice", "pasta", "mushrooms",
        "tomatoes", "peppers", "herbs", "spices", "coconut", "peanuts"
    )
    private val attributeCache = mutableMapOf<Pair<Int, String>, Boolean>()
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
    //private val askedQuestionTexts = mutableSetOf<String>()
    private val askedQuestionAttributes = mutableSetOf<String>()
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
    private suspend fun getAdaptiveQuestion(): Question? {
        try {
            val remainingValidDishes = getTopRecommendations(100)
            val baseQuestions = database.questionDao().getAllQuestions()
                .filter { it.id !in askedQuestions }

            // If we have enough base questions, use randomized selection
            if (baseQuestions.size > 10) {
                return getNextQuestionRandomized()
            }

            // If running low on questions, generate contextual ones
            Log.d(TAG, "Running low on questions (${baseQuestions.size} left), generating contextual questions")
            val contextualQuestions = generateContextualQuestions()

            // Combine base questions with contextual ones
            val allAvailableQuestions = baseQuestions + contextualQuestions

            return if (allAvailableQuestions.isNotEmpty()) {
                // Prefer contextual questions when running low
                contextualQuestions.randomOrNull() ?: baseQuestions.randomOrNull()
            } else {
                // Absolute fallback - generate comparison questions
                generateComparisonQuestion(remainingValidDishes)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error getting adaptive question", e)
            return null
        }
    }

    private suspend fun getNextQuestionRandomized(): Question? {
        try {
            val allQuestions = database.questionDao().getAllQuestions()
            val remainingQuestions = allQuestions.filter { it.id !in askedQuestions }

            if (remainingQuestions.isEmpty()) {
                // Generate new questions using QuestionPoolManager
                Log.d(TAG, "No base questions left, generating new ones")
                val remainingDishes = getTopRecommendations(50)
                val newQuestions = questionPoolManager.generateDynamicQuestions(remainingDishes, 10)
                return newQuestions.randomOrNull()
            }

            // Use QuestionPoolManager for smart question selection
            val remainingDishes = getTopRecommendations(100)
            val smartQuestions = questionPoolManager.selectRelevantQuestions(remainingDishes, remainingQuestions)

            // Phase-based selection from smart questions
            if (questionsInCurrentRound < 3) {
                return getQuestionFromCategory(smartQuestions, "high_impact")
            }
            else if (questionsInCurrentRound < 7) {
                val categories = listOf("cuisine", "meal_type")
                val randomCategory = categories.random()
                return getQuestionFromCategory(smartQuestions, randomCategory)
            }
            else if (questionsInCurrentRound < 12) {
                val categories = listOf("ingredients", "cooking_method", "dietary")
                val randomCategory = categories.random()
                return getQuestionFromCategory(smartQuestions, randomCategory)
            }
            else {
                val categories = listOf("flavor", "texture", "mood", "practical")
                val randomCategory = categories.random()
                return getQuestionFromCategory(smartQuestions, randomCategory)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error getting randomized question", e)
            val allQuestions = database.questionDao().getAllQuestions()
            val remainingQuestions = allQuestions.filter { it.id !in askedQuestions }
            return remainingQuestions.randomOrNull()
        }
    }

    private suspend fun generateIngredientQuestions(): List<Question> {
        val remainingDishes = getTopRecommendations(50)

        // Get common ingredients from remaining dishes
        val commonIngredients = mutableListOf<String>()
        remainingDishes.forEach { dish ->
            if (dish.name.contains("chicken", ignoreCase = true)) commonIngredients.add("chicken")
            if (dish.name.contains("beef", ignoreCase = true)) commonIngredients.add("beef")
            if (dish.name.contains("pasta", ignoreCase = true)) commonIngredients.add("pasta")
            if (dish.name.contains("rice", ignoreCase = true)) commonIngredients.add("rice")
            // Add more ingredient detection logic
        }

        return questionPoolManager.generateIngredientBasedQuestions(commonIngredients.distinct())
    }

    private fun getQuestionFromCategory(questions: List<Question>, category: String): Question? {
        val categoryAttributes = questionCategories[category] ?: emptyList()
        val categoryQuestions = questions.filter { it.attribute in categoryAttributes }

        return if (categoryQuestions.isNotEmpty()) {
            // Weighted random selection within category
            categoryQuestions.maxByOrNull { it.weight + (0..3).random() }
        } else {
            // Fallback to any remaining question
            questions.randomOrNull()
        }
    }

    fun generateIngredientBasedQuestions(targetIngredients: List<String>): List<Question> {
        val questions = mutableListOf<Question>()
        var questionId = 6000 // Start ingredient questions at 6000

        targetIngredients.forEach { ingredient ->
            questions.add(Question(
                id = questionId++,
                questionText = "Are you craving something with $ingredient as the main focus?",
                questionType = "BINARY",
                choices = "Yes,No,Don't Care",
                attribute = "dynamic_ingredient_${ingredient.replace(" ", "").lowercase()}",
                weight = 4
            ))
        }

        return questions
    }

    private suspend fun generateContextualQuestions(): List<Question> {
        try {
            val validDishes = getTopRecommendations(50)
            val contextualQuestions = mutableListOf<Question>()

            // Use QuestionPoolManager for advanced question generation
            val poolQuestions = questionPoolManager.generateDynamicQuestions(validDishes, 15)
            contextualQuestions.addAll(poolQuestions)

            // Keep your existing logic as fallback
            val cuisineTypes = validDishes.map { it.cuisineType }.distinct()
            val avgPrepTime = validDishes.map { it.prepTime }.average()
            val avgSpicyLevel = validDishes.map { it.spicyLevel }.average()

            // Add cuisine-specific questions if QuestionPoolManager didn't generate enough
            if (contextualQuestions.size < 10 && cuisineTypes.size > 1) {
                cuisineTypes.take(5).forEach { cuisine ->
                    val question = Question(
                        id = nextDynamicQuestionId++,
                        questionText = "Are you specifically craving $cuisine cuisine right now?",
                        questionType = "BINARY",
                        choices = "Yes,No,Don't Care",
                        attribute = "specific${cuisine.replace(" ", "")}",
                        weight = 7
                    )
                    contextualQuestions.add(question)
                }
            }

            dynamicQuestions.addAll(contextualQuestions)
            Log.d(TAG, "Generated ${contextualQuestions.size} contextual questions using QuestionPoolManager")
            return contextualQuestions

        } catch (e: Exception) {
            Log.e(TAG, "Error generating contextual questions", e)
            return emptyList()
        }
    }

    private suspend fun generateComparisonQuestion(dishes: List<Dish>): Question? {
        if (dishes.size < 2) return null

        try {
            val dishPairs = dishes.take(10).zipWithNext()

            for ((dish1, dish2) in dishPairs) {
                // Compare cuisine types
                if (dish1.cuisineType != dish2.cuisineType) {
                    val question = Question(
                        id = nextDynamicQuestionId++,
                        questionText = "Would you prefer ${dish1.cuisineType} or ${dish2.cuisineType} style food?",
                        questionType = "MULTIPLE_CHOICE",
                        choices = "${dish1.cuisineType},${dish2.cuisineType},Either is fine",
                        attribute = "cuisineComparison_${dish1.cuisineType.replace(" ", "")}_vs_${dish2.cuisineType.replace(" ", "")}",
                        weight = 8
                    )
                    dynamicQuestions.add(question)
                    return question
                }

                // Compare prep times
                if (abs(dish1.prepTime - dish2.prepTime) > 20) {
                    val quick = if (dish1.prepTime < dish2.prepTime) dish1 else dish2
                    val slow = if (dish1.prepTime > dish2.prepTime) dish1 else dish2

                    val question = Question(
                        id = nextDynamicQuestionId++,
                        questionText = "Would you prefer something quick like ${quick.name} (${quick.prepTime} min) or something that takes time like ${slow.name} (${slow.prepTime} min)?",
                        questionType = "MULTIPLE_CHOICE",
                        choices = "Quick option,Longer prep option,Don't care about time",
                        attribute = "prepTimeComparison_${quick.prepTime}_vs_${slow.prepTime}",
                        weight = 7
                    )
                    dynamicQuestions.add(question)
                    return question
                }

                // Compare spice levels
                if (abs(dish1.spicyLevel - dish2.spicyLevel) > 1) {
                    val mild = if (dish1.spicyLevel < dish2.spicyLevel) dish1 else dish2
                    val spicy = if (dish1.spicyLevel > dish2.spicyLevel) dish1 else dish2

                    val question = Question(
                        id = nextDynamicQuestionId++,
                        questionText = "Would you prefer something mild like ${mild.name} or spicier like ${spicy.name}?",
                        questionType = "MULTIPLE_CHOICE",
                        choices = "Milder option,Spicier option,Either is fine",
                        attribute = "spiceComparison_${mild.spicyLevel}_vs_${spicy.spicyLevel}",
                        weight = 8
                    )
                    dynamicQuestions.add(question)
                    return question
                }
            }

            return null

        } catch (e: Exception) {
            Log.e(TAG, "Error generating comparison question", e)
            return null
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

    // NEW: Weighted scoring calculation
    private fun calculateWeightedScore(currentScore: Float, matchScore: Float, weight: Float): Float {
        // Use additive weighted scoring instead of multiplicative
        val contribution = matchScore * (weight / 10.0f) // Normalize weight
        val decayFactor = 0.95f // Slight decay to prevent scores from growing too large

        return ((currentScore * decayFactor) + contribution).coerceIn(0.01f, 2.0f)
    }

    // NEW: Optimized attribute matching with caching
    private fun getAttributeMatchScore(dish: Dish, attribute: String, userAnswer: String): Float {
        val hasAttribute = evaluateAttributeCached(dish, attribute)

        return when (userAnswer) {
            "Yes" -> if (hasAttribute) 1.0f else 0.15f
            "No" -> if (!hasAttribute) 1.0f else 0.15f
            "Don't Care" -> 0.6f
            else -> 0.5f
        }
    }

    // NEW: Cached attribute evaluation
    private fun evaluateAttributeCached(dish: Dish, attribute: String): Boolean {
        val cacheKey = Pair(dish.id, attribute)

        return attributeCache.getOrPut(cacheKey) {
            checkDishAttribute(dish, attribute)
        }
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

            // MISSING ATTRIBUTES - CRITICAL FIXES:

            // Cuisine checks
            "isItalian" -> dish.cuisineType.equals("Italian", ignoreCase = true)
            "isAsian" -> dish.cuisineType in listOf("Chinese", "Japanese", "Thai", "Korean", "Vietnamese", "Indian", "Asian")
            "isMexican" -> dish.cuisineType.equals("Mexican", ignoreCase = true)
            "isFrench" -> dish.cuisineType.equals("French", ignoreCase = true)
            "isAmerican" -> dish.cuisineType.equals("American", ignoreCase = true)
            "isJapanese" -> dish.cuisineType.equals("Japanese", ignoreCase = true)
            "isIndian" -> dish.cuisineType.equals("Indian", ignoreCase = true)
            "isThai" -> dish.cuisineType.equals("Thai", ignoreCase = true)  // CRITICAL: This was missing!
            "isMiddleEastern" -> dish.cuisineType in listOf("Middle Eastern", "Lebanese", "Turkish", "Persian")
            "isGreek" -> dish.cuisineType.equals("Greek", ignoreCase = true)
            "isSpanish" -> dish.cuisineType.equals("Spanish", ignoreCase = true)
            "isChinese" -> dish.cuisineType.equals("Chinese", ignoreCase = true)
            "isKorean" -> dish.cuisineType.equals("Korean", ignoreCase = true)
            "isVietnamese" -> dish.cuisineType.equals("Vietnamese", ignoreCase = true)
            "isBritish" -> dish.cuisineType.equals("British", ignoreCase = true)
            "isGerman" -> dish.cuisineType.equals("German", ignoreCase = true)
            "isRussian" -> dish.cuisineType.equals("Russian", ignoreCase = true)
            "isBrazilian" -> dish.cuisineType.equals("Brazilian", ignoreCase = true)

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
            "hasBread" -> dishContainsAny(dish.name, listOf("bread", "toast", "sandwich", "burger", "bun"))
            "hasNuts" -> dishContainsAny(dish.name + " " + dish.description,
                listOf("nut", "almond", "walnut", "peanut", "cashew"))
            "hasBeans" -> dishContainsAny(dish.name + " " + dish.description,
                listOf("bean", "lentil", "chickpea", "hummus", "falafel"))
            "hasMushrooms" -> dish.name.contains("mushroom", ignoreCase = true)
            "hasCoconut" -> dish.name.contains("coconut", ignoreCase = true) ||
                    dish.description.contains("coconut", ignoreCase = true)
            "hasOnionGarlic" -> dishContainsAny(dish.description, listOf("onion", "garlic", "scallion"))

            // Cooking method checks
            "isFried" -> dish.name.contains("fried", ignoreCase = true)
            "isGrilled" -> dish.name.contains("grilled", ignoreCase = true)
            "isBaked" -> dishContainsAny(dish.name, listOf("baked", "pie", "casserole"))
            "isSmoky" -> dishContainsAny(dish.name, listOf("smoked", "bbq", "barbecue", "grilled")) ||
                    dishContainsAny(dish.description, listOf("smoked", "smoky", "barbecue"))
            "isBakedRoasted" -> dishContainsAny(dish.name, listOf("baked", "roasted", "roast"))

            // Texture and characteristics
            "isCrispy" -> dishContainsAny(dish.name, listOf("fried", "crispy", "tempura", "chips", "wings")) ||
                    dish.description.contains("crispy", ignoreCase = true)
            "isCreamy" -> dishContainsAny(dish.name, listOf("cream", "creamy", "alfredo", "carbonara", "bisque")) ||
                    dish.description.contains("cream", ignoreCase = true)
            "isHearty" -> dish.savoryLevel >= 4 || dish.prepTime >= 45 ||
                    dishContainsAny(dish.name, listOf("stew", "casserole", "burger", "pasta"))
            "isTangy" -> dishContainsAny(dish.description, listOf("lime", "lemon", "vinegar", "citrus")) ||
                    dishContainsAny(dish.name, listOf("ceviche", "salad"))
            "hasCitrus" -> dishContainsAny(dish.name + " " + dish.description,
                listOf("lemon", "lime", "orange", "citrus"))

            // Meal type and service
            "isBreakfast" -> dishContainsAny(dish.name, listOf("pancake", "waffle", "toast", "egg", "benedict"))
            "isSoup" -> dishContainsAny(dish.name, listOf("soup", "bisque", "broth", "pho", "ramen", "chowder"))
            "isSalad" -> dish.name.contains("salad", ignoreCase = true)
            "isSandwich" -> dishContainsAny(dish.name, listOf("sandwich", "burger", "wrap", "sub"))
            "isFingerFood" -> dishContainsAny(dish.name, listOf("burger", "sandwich", "pizza", "taco", "wings"))
            "isServedCold" -> dishContainsAny(dish.name, listOf("salad", "gazpacho", "ceviche", "sushi")) ||
                    dish.cuisineType.equals("Dessert", ignoreCase = true)
            "isHot" -> !dishContainsAny(dish.name, listOf("salad", "gazpacho", "ice", "cold"))

            // Cultural and preparation
            "usesChopsticks" -> dish.cuisineType in listOf("Chinese", "Japanese", "Korean", "Vietnamese") ||
                    dishContainsAny(dish.name, listOf("ramen", "noodle", "sushi", "pho"))
            "isStuffed" -> dishContainsAny(dish.name, listOf("stuffed", "filled", "dumpling", "ravioli", "burrito"))
            "hasCrust" -> dishContainsAny(dish.name, listOf("pizza", "pie", "tart", "quiche"))
            "hasLamb" -> dish.name.contains("lamb", ignoreCase = true) ||
                    (dish.cuisineType.contains("Middle Eastern", ignoreCase = true) &&
                            dish.name.contains("kebab", ignoreCase = true))
            "isFermented" -> dishContainsAny(dish.name, listOf("kimchi", "sauerkraut", "miso", "cheese"))
            "hasHerbs" -> dish.spicyLevel > 0 ||
                    dishContainsAny(dish.description, listOf("herb", "basil", "oregano", "cilantro"))
            "hasSauce" -> dishContainsAny(dish.description, listOf("sauce", "gravy")) ||
                    dishContainsAny(dish.name, listOf("alfredo", "carbonara", "scampi"))
            "hasStrongFlavors" -> dish.spicyLevel >= 3 || dish.savoryLevel >= 4 ||
                    dishContainsAny(dish.name, listOf("curry", "chili", "garlic"))
            "hasHerbsSpices" -> dish.spicyLevel > 0 ||
                    dish.cuisineType in listOf("Indian", "Thai", "Mexican", "Middle Eastern")
            "hasLotsOfVegetables" -> dish.isVegetarian || dish.isVegan ||
                    dishContainsAny(dish.name, listOf("salad", "stir fry", "vegetable"))

            // DYNAMIC QUESTION ATTRIBUTES - CRITICAL!
            // These handle the dynamic questions that were causing the Thai issue
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
                        val cuisine = attribute.removePrefix("dynamic_cuisine_").capitalize()
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
    // NEW: Optimized version of checkDishAttribute method with all missing attributes
    private fun checkDishAttributeOptimized1(dish: Dish, attribute: String): Boolean {
        return when (attribute) {
            // Fast boolean checks first
            "isDessert" -> dish.cuisineType.equals("Dessert", ignoreCase = true) || dish.sweetLevel >= 4
            "isVegetarian" -> dish.isVegetarian
            "isVegan" -> dish.isVegan
            "isGlutenFree" -> dish.isGlutenFree
            "isSpicy" -> dish.spicyLevel >= 3
            "isQuick" -> dish.prepTime <= 30
            "isComplex" -> dish.prepTime > 45 || dish.complexity >= 4
            "isHealthy" -> dish.isVegetarian && !dish.name.contains("fried", ignoreCase = true)
            "isComfortFood" -> dish.savoryLevel >= 4

            // Optimized string matching using new fields when available
            "hasChicken" -> {
                if (dish.mainIngredients.isNotEmpty()) {
                    dish.mainIngredients.contains("chicken", ignoreCase = true)
                } else {
                    dish.name.contains("chicken", ignoreCase = true)
                }
            }

            "hasBeef" -> {
                if (dish.mainIngredients.isNotEmpty()) {
                    dish.mainIngredients.contains("beef", ignoreCase = true)
                } else {
                    dish.name.contains("beef", ignoreCase = true)
                }
            }

            "hasSeafood" -> {
                if (dish.mainIngredients.isNotEmpty()) {
                    dishContainsAny(dish.mainIngredients, listOf("fish", "shrimp", "seafood", "salmon"))
                } else {
                    dishContainsAny(dish.name, listOf("fish", "shrimp", "seafood", "salmon"))
                }
            }

            // Fast cuisine checks
            "isItalian" -> dish.cuisineType.equals("Italian", ignoreCase = true)
            "isAsian" -> dish.cuisineType in listOf("Chinese", "Japanese", "Thai", "Korean", "Vietnamese", "Indian", "Asian")
            "isMexican" -> dish.cuisineType.equals("Mexican", ignoreCase = true)
            "isFrench" -> dish.cuisineType.equals("French", ignoreCase = true)
            "isAmerican" -> dish.cuisineType.equals("American", ignoreCase = true)
            "isJapanese" -> dish.cuisineType.equals("Japanese", ignoreCase = true)
            "isIndian" -> dish.cuisineType.equals("Indian", ignoreCase = true)
            "isThai" -> dish.cuisineType.equals("Thai", ignoreCase = true)
            "isMiddleEastern" -> dish.cuisineType.contains("Middle Eastern", ignoreCase = true) ||
                    dish.cuisineType.contains("Lebanese", ignoreCase = true) ||
                    dish.cuisineType.contains("Turkish", ignoreCase = true)

            // Fast cooking method checks using new fields
            "isFried" -> {
                if (dish.cookingMethods.isNotEmpty()) {
                    dish.cookingMethods.contains("fried", ignoreCase = true)
                } else {
                    dish.name.contains("fried", ignoreCase = true)
                }
            }

            "isGrilled" -> {
                if (dish.cookingMethods.isNotEmpty()) {
                    dish.cookingMethods.contains("grilled", ignoreCase = true)
                } else {
                    dish.name.contains("grilled", ignoreCase = true)
                }
            }

            "isBaked" -> {
                if (dish.cookingMethods.isNotEmpty()) {
                    dish.cookingMethods.contains("baked", ignoreCase = true)
                } else {
                    dishContainsAny(dish.name, listOf("baked", "pie", "casserole"))
                }
            }

            // Basic food properties
            "hasMeat" -> !dish.isVegetarian
            "isSoup" -> dishContainsAny(dish.name, listOf("soup", "bisque", "broth", "pho", "ramen", "chowder", "stew"))
            "hasPasta" -> dishContainsAny(dish.name, listOf("pasta", "spaghetti", "fettuccine", "lasagna", "carbonara", "alfredo"))
            "isSalad" -> dishContainsAny(dish.name, listOf("salad", "greens", "lettuce"))
            "hasCheese" -> dishContainsAny(dish.name + " " + dish.description, listOf("cheese", "parmesan", "mozzarella", "cheddar", "feta"))
            "hasRice" -> dishContainsAny(dish.name, listOf("rice", "risotto", "paella", "biryani", "pilaf"))
            "isBreakfast" -> dishContainsAny(dish.name.lowercase(),
                listOf("pancake", "waffle", "toast", "egg", "breakfast", "benedict", "omelet", "bagel"))
            "isSandwich" -> dishContainsAny(dish.name.lowercase(),
                listOf("sandwich", "burger", "wrap", "sub", "panini", "club", "melt"))
            "isSweet" -> dish.sweetLevel >= 3

            // ALL MISSING ATTRIBUTES - NEW IMPLEMENTATIONS:

            "hasVegetables" -> {
                if (dish.mainIngredients.isNotEmpty()) {
                    dishContainsAny(dish.mainIngredients, listOf("vegetables", "tomato", "onion", "pepper"))
                } else {
                    dish.isVegetarian || dishContainsAny(dish.name + " " + dish.description,
                        listOf("vegetable", "lettuce", "tomato", "onion", "pepper", "broccoli",
                            "spinach", "kale", "cabbage", "celery", "cucumber", "zucchini"))
                }
            }

            "isHot" -> {
                !dishContainsAny(dish.name.toLowerCase(),
                    listOf("salad", "gazpacho", "ice", "cold", "chilled", "smoothie", "frozen")) &&
                        !dish.cuisineType.equals("Dessert", ignoreCase = true)
            }

            "isFingerFood" -> {
                dishContainsAny(dish.name.toLowerCase(),
                    listOf("burger", "sandwich", "pizza", "taco", "wrap", "wings", "fries",
                        "chicken", "ribs", "spring roll", "dumpling", "sushi", "kebab",
                        "hot dog", "pretzel", "cookie", "donut"))
            }

            "hasBread" -> {
                dishContainsAny(dish.name.toLowerCase(),
                    listOf("bread", "toast", "sandwich", "burger", "bun", "roll", "pita",
                        "bagel", "croissant", "focaccia", "flatbread", "naan", "baguette"))
            }

            "hasOnionGarlic" -> {
                dishContainsAny(dish.description.toLowerCase(),
                    listOf("onion", "garlic", "scallion", "shallot", "chive", "leek")) ||
                        dishContainsAny(dish.name.toLowerCase(), listOf("onion", "garlic"))
            }

            "hasCoconut" -> {
                dishContainsAny(dish.name.toLowerCase() + " " + dish.description.toLowerCase(),
                    listOf("coconut", "curry", "thai", "tropical"))
            }

            "hasHerbs" -> {
                dishContainsAny(dish.description.toLowerCase(),
                    listOf("herb", "basil", "oregano", "thyme", "rosemary", "parsley", "cilantro",
                        "mint", "sage", "dill", "tarragon", "chives", "fresh")) ||
                        dish.spicyLevel > 0
            }

            "hasMushrooms" -> {
                dishContainsAny(dish.name.toLowerCase() + " " + dish.description.toLowerCase(),
                    listOf("mushroom", "shiitake", "portobello", "button", "cremini", "funghi"))
            }

            "hasBeans" -> {
                dishContainsAny(dish.name.toLowerCase() + " " + dish.description.toLowerCase(),
                    listOf("bean", "lentil", "chickpea", "black bean", "kidney", "pinto",
                        "navy bean", "lima", "garbanzo", "legume", "hummus", "falafel"))
            }

            "isTangy" -> {
                dishContainsAny(dish.description.toLowerCase(),
                    listOf("lime", "lemon", "vinegar", "citrus", "sour", "acidic", "tangy")) ||
                        dishContainsAny(dish.name.toLowerCase(),
                            listOf("ceviche", "salad", "pickled", "fermented"))
            }

            "isServedCold" -> {
                dishContainsAny(dish.name.toLowerCase(),
                    listOf("salad", "gazpacho", "ceviche", "sushi", "ice cream", "smoothie",
                        "cold", "chilled", "frozen")) ||
                        dish.cuisineType.equals("Dessert", ignoreCase = true)
            }

            "hasNuts" -> {
                dishContainsAny(dish.name.toLowerCase() + " " + dish.description.toLowerCase(),
                    listOf("nut", "almond", "walnut", "pecan", "cashew", "pistachio", "hazelnut",
                        "peanut", "pine nut", "macadamia", "brazil nut"))
            }

            "usesChopsticks" -> {
                dish.cuisineType.equals("Chinese", ignoreCase = true) ||
                        dish.cuisineType.equals("Japanese", ignoreCase = true) ||
                        dish.cuisineType.equals("Korean", ignoreCase = true) ||
                        dish.cuisineType.equals("Vietnamese", ignoreCase = true) ||
                        dishContainsAny(dish.name.toLowerCase(),
                            listOf("ramen", "noodle", "stir fry", "sushi", "dim sum", "pho"))
            }

            "isStuffed" -> {
                dishContainsAny(dish.name.toLowerCase(),
                    listOf("stuffed", "filled", "dumpling", "ravioli", "pierogi", "empanada",
                        "burrito", "wrap", "sandwich", "burger", "calzone", "spring roll"))
            }

            "hasCrust" -> {
                dishContainsAny(dish.name.toLowerCase(),
                    listOf("pizza", "pie", "tart", "quiche", "bread", "crust", "pastry", "wellington"))
            }

            "hasLamb" -> {
                dishContainsAny(dish.name.toLowerCase(),
                    listOf("lamb", "mutton", "gyros", "kebab", "moussaka")) ||
                        (dish.cuisineType.contains("Middle Eastern", ignoreCase = true) &&
                                dishContainsAny(dish.name.toLowerCase(), listOf("meat", "kebab")))
            }

            "isFermented" -> {
                dishContainsAny(dish.name.toLowerCase(),
                    listOf("kimchi", "sauerkraut", "miso", "tempeh", "cheese", "yogurt",
                        "kefir", "pickled", "fermented", "sourdough"))
            }

            "isSmoky" -> {
                dishContainsAny(dish.name.toLowerCase() + " " + dish.description.toLowerCase(),
                    listOf("smoked", "bbq", "barbecue", "grilled", "bacon", "brisket", "ribs")) ||
                        dishContainsAny(dish.name.toLowerCase(), listOf("barbecue", "grilled"))
            }

            "isCrispy" -> {
                dishContainsAny(dish.name.toLowerCase(),
                    listOf("fried", "crispy", "tempura", "chips", "fries", "crackling",
                        "crunchy", "wings", "chicken", "duck")) ||
                        dishContainsAny(dish.description.toLowerCase(), listOf("crispy", "fried"))
            }

            "hasLotsOfVegetables" -> {
                dishContainsAny(dish.name.toLowerCase(),
                    listOf("salad", "stir fry", "ratatouille", "minestrone", "vegetable", "veggie")) ||
                        dish.isVegetarian || dish.isVegan
            }

            "hasStrongFlavors" -> {
                dish.spicyLevel >= 3 || dish.savoryLevel >= 4 ||
                        dishContainsAny(dish.name.toLowerCase(),
                            listOf("curry", "chili", "garlic", "blue cheese", "kimchi", "anchovy"))
            }

            "isHearty" -> {
                dish.savoryLevel >= 4 || dish.complexity >= 3 || dish.prepTime >= 45 ||
                        dishContainsAny(dish.name.toLowerCase(),
                            listOf("stew", "casserole", "pot pie", "burger", "pasta", "risotto",
                                "lasagna", "beef", "pork", "hearty"))
            }

            "hasHerbsSpices" -> {
                dish.spicyLevel > 0 ||
                        dishContainsAny(dish.description.toLowerCase(),
                            listOf("spice", "herb", "seasoned", "aromatic", "fragrant")) ||
                        dish.cuisineType.contains("Indian", ignoreCase = true) ||
                        dish.cuisineType.contains("Thai", ignoreCase = true) ||
                        dish.cuisineType.contains("Mexican", ignoreCase = true)
            }

            "isBakedRoasted" -> {
                dishContainsAny(dish.name.toLowerCase(),
                    listOf("baked", "roasted", "roast", "pie", "casserole", "gratin", "au gratin")) ||
                        dishContainsAny(dish.description.toLowerCase(), listOf("baked", "roasted"))
            }

            "hasCitrus" -> {
                dishContainsAny(dish.name.toLowerCase() + " " + dish.description.toLowerCase(),
                    listOf("lemon", "lime", "orange", "citrus", "grapefruit", "yuzu")) ||
                        dishContainsAny(dish.name.toLowerCase(), listOf("ceviche", "key lime"))
            }

            "hasEggs" -> {
                dishContainsAny(dish.name.toLowerCase(),
                    listOf("egg", "benedict", "omelet", "frittata"))
            }

            "hasPork" -> {
                dishContainsAny(dish.name.toLowerCase(),
                    listOf("pork", "bacon", "ham", "sausage", "chorizo", "pancetta"))
            }

            "isCreamy" -> {
                dishContainsAny(dish.name.toLowerCase(),
                    listOf("cream", "creamy", "alfredo", "carbonara", "bisque", "chowder")) ||
                        dish.description.contains("cream", ignoreCase = true)
            }

            "hasSauce" -> {
                dishContainsAny(dish.description.toLowerCase(),
                    listOf("sauce", "gravy", "dressing")) ||
                        dishContainsAny(dish.name.toLowerCase(),
                            listOf("alfredo", "carbonara", "scampi", "marsala"))
            }

            else -> {
                Log.w(TAG, "Unknown attribute: $attribute")
                false
            }
        }
    }

    // Helper function to check if dish contains any of the keywords
    private fun dishContainsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it, ignoreCase = true) }
    }

    private suspend fun processDynamicQuestionAnswer(questionId: Int, answer: String) {
        val allDishes = database.dishDao().getAllDishes()

        // Find the dynamic question
        val dynamicQuestion = dynamicQuestions.find { it.id == questionId }
        if (dynamicQuestion != null) {
            markQuestionAsUsedInSession(dynamicQuestion.attribute)

            when {
                dynamicQuestion.attribute.startsWith("specific") -> {
                    // Cuisine-specific questions
                    val cuisine = dynamicQuestion.attribute.removePrefix("specific")
                    allDishes.forEach { dish ->
                        val matches = dish.cuisineType.replace(" ", "").equals(cuisine, ignoreCase = true)
                        val currentScore = dishScores[dish.id] ?: 1.0f

                        val newScore = when (answer) {
                            "Yes" -> if (matches) currentScore * 2.0f else currentScore * 0.3f
                            "No" -> if (matches) currentScore * 0.2f else currentScore * 1.1f
                            else -> currentScore * 0.8f
                        }

                        dishScores[dish.id] = newScore
                    }
                }

                dynamicQuestion.attribute.contains("cuisineComparison") -> {
                    // Handle cuisine comparison questions
                    processCuisineComparison(answer, allDishes)
                }

                dynamicQuestion.attribute.contains("prepTimeComparison") -> {
                    // Handle prep time comparison questions
                    processPrepTimeComparison(answer, allDishes)
                }

                dynamicQuestion.attribute.contains("spiceComparison") -> {
                    // Handle spice comparison questions
                    processSpiceComparison(answer, allDishes)
                }

                else -> {
                    // Generic dynamic question handling
                    allDishes.forEach { dish ->
                        val currentScore = dishScores[dish.id] ?: 1.0f
                        val newScore = when (answer) {
                            "Yes" -> currentScore * 1.2f
                            "No" -> currentScore * 0.8f
                            else -> currentScore * 0.9f
                        }
                        dishScores[dish.id] = newScore
                    }
                }
            }
        }
    }

    private fun processCuisineComparison(answer: String, dishes: List<Dish>) {
        dishes.forEach { dish ->
            val currentScore = dishScores[dish.id] ?: 1.0f

            val newScore = when {
                answer.contains("Italian", ignoreCase = true) && dish.cuisineType.contains("Italian", ignoreCase = true) -> currentScore * 1.8f
                answer.contains("Chinese", ignoreCase = true) && dish.cuisineType.contains("Chinese", ignoreCase = true) -> currentScore * 1.8f
                answer.contains("Mexican", ignoreCase = true) && dish.cuisineType.contains("Mexican", ignoreCase = true) -> currentScore * 1.8f
                answer == "Either is fine" -> currentScore * 0.9f
                else -> currentScore * 0.5f
            }

            dishScores[dish.id] = newScore
        }
    }

    private fun processPrepTimeComparison(answer: String, dishes: List<Dish>) {
        dishes.forEach { dish ->
            val currentScore = dishScores[dish.id] ?: 1.0f

            val newScore = when (answer) {
                "Quick option" -> if (dish.prepTime <= 30) currentScore * 1.8f else currentScore * 0.4f
                "Longer prep option" -> if (dish.prepTime > 45) currentScore * 1.8f else currentScore * 0.4f
                "Don't care about time" -> currentScore * 0.9f
                else -> currentScore
            }

            dishScores[dish.id] = newScore
        }
    }

    private fun processSpiceComparison(answer: String, dishes: List<Dish>) {
        dishes.forEach { dish ->
            val currentScore = dishScores[dish.id] ?: 1.0f

            val newScore = when (answer) {
                "Milder option" -> if (dish.spicyLevel <= 2) currentScore * 1.8f else currentScore * 0.3f
                "Spicier option" -> if (dish.spicyLevel >= 3) currentScore * 1.8f else currentScore * 0.3f
                "Either is fine" -> currentScore * 0.9f
                else -> currentScore
            }

            dishScores[dish.id] = newScore
        }
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

    // Function to reject the current top recommendation and continue
    suspend fun rejectTopRecommendation(): Boolean {
        return try {
            val topDish = getTopRecommendations(1).firstOrNull()
            if (topDish != null) {
                excludedDishes.add(topDish.id)
                dishScores.remove(topDish.id)

                Log.d(TAG, "Rejected dish: ${topDish.name}")
                questionsInCurrentRound = 0
                askedQuestions.clear()

                val remainingCount = getAvailableDishCount()
                Log.d(TAG, "Dishes remaining: $remainingCount")

                return remainingCount > 0
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error rejecting recommendation", e)
            false
        }
    }

    // Get number of remaining available dishes
    fun getAvailableDishCount(): Int {
        return dishScores.entries.count { it.value > 0.01f && it.key !in excludedDishes }
    }

    fun getCurrentRoundInfo(): Triple<Int, Int, Int> {
        return Triple(questionsInCurrentRound, totalQuestionsAsked, excludedDishes.size)
    }

    // Session variety tracking methods
    fun markQuestionAsUsedInSession(attribute: String) {
        sessionQuestionHistory.add(attribute)
    }

    fun hasUsedQuestionTypeInSession(attribute: String): Boolean {
        return attribute in sessionQuestionHistory
    }

    suspend fun debugCurrentState(): String {
        val totalDishes = dishScores.size
        val excludedCount = excludedDishes.size
        val availableCount = getAvailableDishCount()
        val topScores = dishScores.entries
            .filter { it.key !in excludedDishes }
            .sortedByDescending { it.value }
            .take(5)

        return "Debug State: Total=$totalDishes, Excluded=$excludedCount, Available=$availableCount, " +
                "TopScores=${topScores.map { "${getDishName(it.key)}=${String.format("%.3f", it.value)}" }}"
    }

    fun resetSessionHistory() {
        sessionQuestionHistory.clear()
        dynamicQuestions.clear()
        nextDynamicQuestionId = 5000
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

     // NEW: Get cache statistics for debugging
    fun getCacheStats(): String {
        return "Cache size: ${attributeCache.size} entries"
    }

    // NEW: Performance optimization methods for future client-server architecture
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
    fun clearAttributeCache() = attributeCache.clear()

    // Debug methods for testing
    fun getSessionStats(): String {
        return "Round: $questionsInCurrentRound, Total: $totalQuestionsAsked, " +
                "Excluded: ${excludedDishes.size}, Available: ${getAvailableDishCount()}, " +
                "Categories used: ${sessionQuestionHistory.map { attr ->
                    questionCategories.entries.find { (_, attributes) -> attr in attributes }?.key
                }.distinct().size}, " +
                "Dynamic questions: ${getDynamicQuestionCount()}, " +
                "Cache entries: ${attributeCache.size}"
    }

    fun getDynamicQuestionCount(): Int {
        return dynamicQuestions.size
    }

    suspend fun generateCuisineComparisonQuestions(cuisines: List<String>): List<Question> {
        val questions = mutableListOf<Question>()

        cuisines.chunked(2).forEach { pair ->
            if (pair.size == 2) {
                questionTemplates["comparison"]?.random()?.let { template ->
                    questions.add(Question(
                        id = nextDynamicQuestionId++,
                        questionText = template.replace("{option1}", pair[0]).replace("{option2}", pair[1]),
                        questionType = "MULTIPLE_CHOICE",
                        choices = "${pair[0]},${pair[1]},Either is fine",
                        attribute = "dynamic_comparison_${pair[0].replace(" ", "").lowercase()}_vs_${pair[1].replace(" ", "").lowercase()}",
                        weight = 7
                    ))
                }
            }
        }

        dynamicQuestions.addAll(questions)
        return questions
    }

    // Method to save/load user preferences for future sessions
    fun exportUserPreferences(): Map<String, String> {
        return userPreferences.toMap()
    }

    fun importUserPreferences(preferences: Map<String, String>) {
        userPreferences.putAll(preferences)
    }

    // Method to get question suggestions based on current state
    suspend fun getQuestionSuggestions(): List<String> {
        val suggestions = mutableListOf<String>()
        val topDishes = getTopRecommendations(10)

        if (topDishes.isNotEmpty()) {
            val cuisines = topDishes.map { it.cuisineType }.distinct()
            val avgSpice = topDishes.map { it.spicyLevel }.average()
            val avgPrep = topDishes.map { it.prepTime }.average()

            suggestions.add("Remaining dishes span ${cuisines.size} different cuisines")
            suggestions.add("Average spice level of remaining dishes: ${String.format("%.1f", avgSpice)}")
            suggestions.add("Average prep time: ${String.format("%.0f", avgPrep)} minutes")
            suggestions.add("Categories explored: ${sessionQuestionHistory.size}")

            if (cuisines.size > 3) {
                suggestions.add("Consider asking more cuisine-specific questions")
            }
            if (avgSpice > 2.5) {
                suggestions.add("Consider asking about spice tolerance")
            }
            if (avgPrep > 45) {
                suggestions.add("Consider asking about available cooking time")
            }
        }

        return suggestions
    }
}
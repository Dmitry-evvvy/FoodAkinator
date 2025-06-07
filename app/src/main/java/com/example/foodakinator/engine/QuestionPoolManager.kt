package com.example.foodakinator.engine

import android.util.Log
import com.example.foodakinator.data.model.Dish
import com.example.foodakinator.data.model.Question

class QuestionPoolManager {

    // Base question templates that can be dynamically generated
    private val questionTemplates = mapOf(
        "cuisine_specific" to listOf(
            "Are you craving authentic {cuisine} flavors right now?",
            "Do you want something that reminds you of {cuisine} street food?",
            "Are you in the mood for traditional {cuisine} spices?",
            "Would you like something from the {cuisine} region specifically?"
        ),

        "ingredient_focused" to listOf(
            "Do you want something with {ingredient} as the main ingredient?",
            "Are you craving the taste of {ingredient} today?",
            "Would you like {ingredient} to be prominent in your meal?",
            "Do you want something where {ingredient} really stands out?"
        ),

        "mood_based" to listOf(
            "Are you feeling {mood} and want food to match that vibe?",
            "Do you want something {mood} to suit your current mood?",
            "Are you looking for food that makes you feel {mood}?",
            "Would {mood} food hit the spot right now?"
        ),

        "texture_specific" to listOf(
            "Are you craving something with a {texture} texture?",
            "Do you want something really {texture}?",
            "Are you in the mood for food that's {texture}?",
            "Would you like something with a nice {texture} bite?"
        ),

        "comparison" to listOf(
            "Would you prefer {option1} or {option2} style?",
            "Are you more in the mood for {option1} or {option2}?",
            "Between {option1} and {option2}, what sounds better?",
            "Would you lean towards {option1} or {option2} today?"
        ),

        "preparation_method" to listOf(
            "Do you want something that's {method}?",
            "Are you craving {method} food right now?",
            "Would you like something prepared by {method}?",
            "Do you want the smoky/crispy/tender flavor that comes from {method}?"
        ),

        "situational" to listOf(
            "Do you want something perfect for {situation}?",
            "Are you looking for {situation} appropriate food?",
            "Do you need something that works well for {situation}?",
            "Would you like food that's ideal for {situation}?"
        )
    )

    // Dynamic data for filling templates
    private val cuisineList = listOf(
        "Italian", "Chinese", "Japanese", "Mexican", "Indian", "Thai", "French", "Greek",
        "Korean", "Vietnamese", "Lebanese", "Turkish", "Spanish", "Moroccan", "Ethiopian",
        "Brazilian", "Peruvian", "Argentine", "Russian", "German", "Polish", "Hungarian",
        "American", "British", "Australian", "Canadian"
    )

    private val ingredientList = listOf(
        "garlic", "onions", "tomatoes", "mushrooms", "peppers", "spinach", "avocado",
        "chicken", "beef", "pork", "fish", "shrimp", "tofu", "eggs", "cheese", "bacon",
        "rice", "pasta", "noodles", "bread", "potatoes", "beans", "lentils", "quinoa",
        "coconut", "peanuts", "almonds", "herbs", "spices", "ginger", "lime", "lemon"
    )

    private val moodList = listOf(
        "adventurous", "comforting", "energetic", "relaxed", "indulgent", "healthy",
        "nostalgic", "exotic", "festive", "cozy", "refreshing", "warming", "cooling",
        "uplifting", "sophisticated", "playful", "rustic", "elegant", "casual"
    )

    private val textureList = listOf(
        "crispy", "creamy", "crunchy", "smooth", "chewy", "tender", "flaky", "silky",
        "hearty", "light", "dense", "airy", "juicy", "moist", "firm", "soft"
    )

    private val methodList = listOf(
        "grilled", "fried", "baked", "roasted", "steamed", "boiled", "sautéed", "braised",
        "stewed", "smoked", "barbecued", "poached", "broiled", "pan-seared", "deep-fried",
        "stir-fried", "slow-cooked"
    )

    private val situationList = listOf(
        "eating at your desk", "sharing with friends", "date night", "family dinner",
        "quick lunch", "late night snack", "weekend brunch", "picnic", "party",
        "game day", "movie night", "study session", "comfort after a bad day"
    )

    // Generate questions dynamically
    fun generateDynamicQuestions(remainingDishes: List<Dish>, questionCount: Int = 50): List<Question> {
        val questions = mutableListOf<Question>()
        var questionId = 5000 // Start dynamic questions at ID 5000

        try {
            // Analyze remaining dishes to generate relevant questions
            val cuisines = remainingDishes.map { it.cuisineType }.distinct().take(10)
            val avgSpiceLevel = if (remainingDishes.isNotEmpty()) {
                remainingDishes.map { it.spicyLevel }.average()
            } else 0.0
            val avgPrepTime = if (remainingDishes.isNotEmpty()) {
                remainingDishes.map { it.prepTime }.average()
            } else 0.0

            // Generate cuisine-specific questions
            cuisines.forEach { cuisine ->
                questionTemplates["cuisine_specific"]?.forEach { template ->
                    if (questions.size < questionCount) {
                        questions.add(Question(
                            id = questionId++,
                            questionText = template.replace("{cuisine}", cuisine),
                            questionType = "BINARY",
                            choices = "Yes,No,Don't Care",
                            attribute = "dynamic_cuisine_${cuisine.replace(" ", "").lowercase()}",
                            weight = 6
                        ))
                    }
                }
            }

            // Generate ingredient-based questions
            ingredientList.shuffled().take(15).forEach { ingredient ->
                questionTemplates["ingredient_focused"]?.random()?.let { template ->
                    if (questions.size < questionCount) {
                        questions.add(Question(
                            id = questionId++,
                            questionText = template.replace("{ingredient}", ingredient),
                            questionType = "BINARY",
                            choices = "Yes,No,Don't Care",
                            attribute = "dynamic_ingredient_${ingredient.replace(" ", "").lowercase()}",
                            weight = 4
                        ))
                    }
                }
            }

            // Generate mood-based questions
            moodList.shuffled().take(10).forEach { mood ->
                questionTemplates["mood_based"]?.random()?.let { template ->
                    if (questions.size < questionCount) {
                        questions.add(Question(
                            id = questionId++,
                            questionText = template.replace("{mood}", mood),
                            questionType = "BINARY",
                            choices = "Yes,No,Don't Care",
                            attribute = "dynamic_mood_${mood.replace(" ", "").lowercase()}",
                            weight = 5
                        ))
                    }
                }
            }

            // Generate texture questions
            textureList.shuffled().take(8).forEach { texture ->
                questionTemplates["texture_specific"]?.random()?.let { template ->
                    if (questions.size < questionCount) {
                        questions.add(Question(
                            id = questionId++,
                            questionText = template.replace("{texture}", texture),
                            questionType = "BINARY",
                            choices = "Yes,No,Don't Care",
                            attribute = "dynamic_texture_${texture.replace(" ", "").lowercase()}",
                            weight = 4
                        ))
                    }
                }
            }

            // Generate comparison questions
            cuisines.chunked(2).forEach { pair ->
                if (pair.size == 2 && questions.size < questionCount) {
                    questionTemplates["comparison"]?.random()?.let { template ->
                        questions.add(Question(
                            id = questionId++,
                            questionText = template.replace("{option1}", pair[0]).replace("{option2}", pair[1]),
                            questionType = "MULTIPLE_CHOICE",
                            choices = "${pair[0]},${pair[1]},Either is fine",
                            attribute = "dynamic_comparison_${pair[0].replace(" ", "").lowercase()}_vs_${pair[1].replace(" ", "").lowercase()}",
                            weight = 7
                        ))
                    }
                }
            }

            // Generate preparation method questions
            methodList.shuffled().take(8).forEach { method ->
                questionTemplates["preparation_method"]?.random()?.let { template ->
                    if (questions.size < questionCount) {
                        questions.add(Question(
                            id = questionId++,
                            questionText = template.replace("{method}", method),
                            questionType = "BINARY",
                            choices = "Yes,No,Don't Care",
                            attribute = "dynamic_method_${method.replace(" ", "").lowercase()}",
                            weight = 4
                        ))
                    }
                }
            }

            // Generate situational questions
            situationList.shuffled().take(6).forEach { situation ->
                questionTemplates["situational"]?.random()?.let { template ->
                    if (questions.size < questionCount) {
                        questions.add(Question(
                            id = questionId++,
                            questionText = template.replace("{situation}", situation),
                            questionType = "BINARY",
                            choices = "Yes,No,Don't Care",
                            attribute = "dynamic_situation_${situation.replace(" ", "").replace(",", "").lowercase()}",
                            weight = 3
                        ))
                    }
                }
            }

            Log.d("QuestionPoolManager", "Generated ${questions.size} dynamic questions")
            return questions.shuffled() // Randomize order

        } catch (e: Exception) {
            Log.e("QuestionPoolManager", "Error generating dynamic questions", e)
            return emptyList()
        }
    }

    // Smart question selection based on remaining dish analysis
    fun selectRelevantQuestions(remainingDishes: List<Dish>, allQuestions: List<Question>): List<Question> {
        if (remainingDishes.isEmpty()) return allQuestions.shuffled()

        val relevantQuestions = mutableListOf<Question>()

        // Analyze dish characteristics
        val cuisineVariety = remainingDishes.map { it.cuisineType }.distinct().size
        val spiceVariety = remainingDishes.map { it.spicyLevel }.distinct().size
        val prepTimeVariety = remainingDishes.map { it.prepTime }.distinct().size
        val dietaryVariety = remainingDishes.count { it.isVegetarian }

        // Prioritize questions that can best discriminate
        allQuestions.forEach { question ->
            val priority = when {
                cuisineVariety > 5 && question.attribute.contains("cuisine", ignoreCase = true) -> 9
                spiceVariety > 2 && question.attribute.contains("spic", ignoreCase = true) -> 8
                prepTimeVariety > 30 && question.attribute.contains("quick", ignoreCase = true) -> 7
                dietaryVariety > 0 && question.attribute.contains("vegetarian", ignoreCase = true) -> 8
                question.attribute.contains("meat", ignoreCase = true) -> 6
                else -> question.weight
            }

            relevantQuestions.add(question.copy(weight = priority))
        }

        return relevantQuestions.sortedByDescending { it.weight }
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
}
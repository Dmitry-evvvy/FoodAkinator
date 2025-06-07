// Fix for DataLoader.kt - Skip relations for now to avoid foreign key errors

package com.example.foodakinator.util

import android.content.Context
import android.util.Log
import com.example.foodakinator.data.database.FoodAkinatorDatabase
import com.example.foodakinator.data.model.Dish
import com.example.foodakinator.data.model.Question
import com.example.foodakinator.data.model.QuestionDishRelation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class DataLoader(private val context: Context) {
    private val TAG = "DataLoader"

    // Load dishes from JSON file
    private fun loadDishesFromAssets(): List<Dish> {
        val jsonString = readAssetFile("dishes.json")
        if (jsonString == null) {
            Log.e(TAG, "Failed to read dishes.json")
            return emptyList()
        }

        try {
            val jsonArray = JSONArray(jsonString)
            val dishes = mutableListOf<Dish>()

            Log.d(TAG, "Found ${jsonArray.length()} dishes in JSON")

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val dish = Dish(
                    id = jsonObject.getInt("id"),
                    name = jsonObject.getString("name"),
                    description = jsonObject.getString("description"),
                    imageResourceId = jsonObject.getInt("imageResourceId"),
                    cuisineType = jsonObject.getString("cuisineType"),
                    prepTime = jsonObject.getInt("prepTime"),
                    isVegetarian = jsonObject.getBoolean("isVegetarian"),
                    isVegan = jsonObject.getBoolean("isVegan"),
                    isGlutenFree = jsonObject.getBoolean("isGlutenFree"),
                    spicyLevel = jsonObject.getInt("spicyLevel"),
                    sweetLevel = jsonObject.getInt("sweetLevel"),
                    savoryLevel = jsonObject.getInt("savoryLevel"),
                    complexity = jsonObject.getInt("complexity"),
                    // Handle optional new fields with defaults
                    proteinType = jsonObject.optString("proteinType", ""),
                    cookingMethod = jsonObject.optString("cookingMethod", ""),
                    isComfortFood = jsonObject.optBoolean("isComfortFood", false),
                    mealType = jsonObject.optString("mealType", ""),
                    mainIngredients = jsonObject.optString("mainIngredients", ""),
                    cookingMethods = jsonObject.optString("cookingMethods", ""),
                    servingTemperature = jsonObject.optString("servingTemperature", "hot"),
                    textureProfile = jsonObject.optString("textureProfile", ""),
                    allergens = jsonObject.optString("allergens", ""),
                    nutritionalHighlights = jsonObject.optString("nutritionalHighlights", "")
                )
                dishes.add(dish)
            }

            Log.d(TAG, "Successfully parsed ${dishes.size} dishes from assets")
            return dishes
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing dishes.json", e)
            return emptyList()
        }
    }

    // Load questions from JSON file
    private fun loadQuestionsFromAssets(): List<Question> {
        val jsonString = readAssetFile("questions.json")
        if (jsonString == null) {
            Log.e(TAG, "Failed to read questions.json")
            return emptyList()
        }

        try {
            val jsonArray = JSONArray(jsonString)
            val questions = mutableListOf<Question>()

            Log.d(TAG, "Found ${jsonArray.length()} questions in JSON")

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val question = Question(
                    id = jsonObject.getInt("id"),
                    questionText = jsonObject.getString("questionText"),
                    questionType = jsonObject.getString("questionType"),
                    choices = jsonObject.getString("choices"),
                    attribute = jsonObject.getString("attribute"),
                    weight = jsonObject.getInt("weight")
                )
                questions.add(question)
            }

            Log.d(TAG, "Successfully parsed ${questions.size} questions from assets")
            return questions
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing questions.json", e)
            return emptyList()
        }
    }

    fun verifyAssets() {
        try {
            val files = context.assets.list("")
            Log.e("DataLoader", "Assets found: ${files?.joinToString(", ")}")

            // Check for specific files
            val dishesExists = context.assets.list("")?.contains("dishes.json") == true
            val questionsExists = context.assets.list("")?.contains("questions.json") == true

            Log.e("DataLoader", "dishes.json exists: $dishesExists")
            Log.e("DataLoader", "questions.json exists: $questionsExists")
        } catch (e: Exception) {
            Log.e("DataLoader", "Error checking assets", e)
        }
    }

    // Helper function to read file from assets
    private fun readAssetFile(fileName: String): String? {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            Log.e(TAG, "Error reading file: $fileName", e)
            null
        }
    }

    // Load all data into the database
    suspend fun loadDataIntoDatabase(database: FoodAkinatorDatabase) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Force loading data for debugging")

                // Load data from assets
                val dishes = loadDishesFromAssets()
                val questions = loadQuestionsFromAssets()

                Log.e(TAG, "LOADED FROM ASSETS: ${dishes.size} dishes, ${questions.size} questions")

                Log.d(TAG, "Inserting data into database...")

                // Insert data
                if (dishes.isNotEmpty()) {
                    database.dishDao().insertAll(dishes)
                    Log.d(TAG, "Inserted ${dishes.size} dishes")
                }

                if (questions.isNotEmpty()) {
                    database.questionDao().insertAll(questions)
                    Log.d(TAG, "Inserted ${questions.size} questions")
                }

                // SKIP relations for now to avoid foreign key issues
                Log.d(TAG, "Skipping relations to avoid foreign key constraint issues")

                // Verify the data was inserted
                val finalDishCount = database.dishDao().getCount()
                val finalQuestionCount = database.questionDao().getCount()

                Log.e(TAG, "VERIFICATION: Database now has $finalDishCount dishes, $finalQuestionCount questions")

                if (finalDishCount == 0 || finalQuestionCount == 0) {
                    Log.e(TAG, "DATABASE INSERTION FAILED! Counts are still zero.")
                } else {
                    Log.d(TAG, "Data inserted successfully")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading data into database", e)
            }
        }
    }
}
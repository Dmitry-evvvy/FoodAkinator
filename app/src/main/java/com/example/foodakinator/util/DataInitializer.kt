package com.example.foodakinator.util

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.foodakinator.R
import com.example.foodakinator.data.database.FoodAkinatorDatabase
import com.example.foodakinator.data.database.dao.DishDao
import com.example.foodakinator.data.database.dao.QuestionDao
import com.example.foodakinator.data.database.dao.RelationDao
import com.example.foodakinator.data.model.Dish
import com.example.foodakinator.data.model.Question
import com.example.foodakinator.data.model.QuestionDishRelation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DataInitializer {

    // Create database with callback to populate data
    fun getPopulatedDatabase(context: Context): FoodAkinatorDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            FoodAkinatorDatabase::class.java,
            "food_akinator_database"
        ).addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Populate the database when it's created
                CoroutineScope(Dispatchers.IO).launch {
                    populateDatabase(getDatabase(context))
                }
            }
        }).fallbackToDestructiveMigration().build()
    }

    // Get database without callback
    private fun getDatabase(context: Context): FoodAkinatorDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            FoodAkinatorDatabase::class.java,
            "food_akinator_database"
        ).build()
    }

    // Populate database with sample data
    private suspend fun populateDatabase(database: FoodAkinatorDatabase) {
        val dishDao = database.dishDao()
        val questionDao = database.questionDao()
        val relationDao = database.relationDao()

        // Create and insert sample dishes
        val dishes = listOf(
            Dish(
                id = 1,
                name = "Pizza Margherita",
                description = "Classic Italian pizza with tomato sauce, mozzarella, and basil",
                imageResourceId = R.drawable.ic_launcher_foreground, // Placeholder
                cuisineType = "Italian",
                prepTime = 30,
                isVegetarian = true,
                isVegan = false,
                isGlutenFree = false,
                spicyLevel = 1,
                sweetLevel = 2,
                savoryLevel = 4,
                complexity = 2
            ),
            Dish(
                id = 2,
                name = "Beef Burger",
                description = "Classic beef patty with lettuce, tomato, cheese, and sauce on a bun",
                imageResourceId = R.drawable.ic_launcher_foreground, // Placeholder
                cuisineType = "American",
                prepTime = 20,
                isVegetarian = false,
                isVegan = false,
                isGlutenFree = false,
                spicyLevel = 1,
                sweetLevel = 2,
                savoryLevel = 5,
                complexity = 1
            ),
            Dish(
                id = 3,
                name = "Pad Thai",
                description = "Stir-fried rice noodles with eggs, tofu, bean sprouts, peanuts, and lime",
                imageResourceId = R.drawable.ic_launcher_foreground, // Placeholder
                cuisineType = "Thai",
                prepTime = 25,
                isVegetarian = false,
                isVegan = false,
                isGlutenFree = true,
                spicyLevel = 3,
                sweetLevel = 3,
                savoryLevel = 4,
                complexity = 3
            ),
            Dish(
                id = 4,
                name = "Vegetable Curry",
                description = "Mixed vegetables in a flavorful curry sauce with spices",
                imageResourceId = R.drawable.ic_launcher_foreground, // Placeholder
                cuisineType = "Indian",
                prepTime = 35,
                isVegetarian = true,
                isVegan = true,
                isGlutenFree = true,
                spicyLevel = 4,
                sweetLevel = 1,
                savoryLevel = 5,
                complexity = 3
            ),
            Dish(
                id = 5,
                name = "Caesar Salad",
                description = "Romaine lettuce with croutons, parmesan, and caesar dressing",
                imageResourceId = R.drawable.ic_launcher_foreground, // Placeholder
                cuisineType = "Italian",
                prepTime = 15,
                isVegetarian = true,
                isVegan = false,
                isGlutenFree = false,
                spicyLevel = 0,
                sweetLevel = 1,
                savoryLevel = 3,
                complexity = 1
            ),
            Dish(
                id = 6,
                name = "Sushi Roll",
                description = "Rice and fish wrapped in seaweed with wasabi and ginger",
                imageResourceId = R.drawable.ic_launcher_foreground, // Placeholder
                cuisineType = "Japanese",
                prepTime = 40,
                isVegetarian = false,
                isVegan = false,
                isGlutenFree = true,
                spicyLevel = 1,
                sweetLevel = 2,
                savoryLevel = 4,
                complexity = 4
            ),
            Dish(
                id = 7,
                name = "Grilled Chicken",
                description = "Seasoned chicken breast grilled to perfection",
                imageResourceId = R.drawable.ic_launcher_foreground, // Placeholder
                cuisineType = "American",
                prepTime = 25,
                isVegetarian = false,
                isVegan = false,
                isGlutenFree = true,
                spicyLevel = 1,
                sweetLevel = 0,
                savoryLevel = 4,
                complexity = 2
            ),
            Dish(
                id = 8,
                name = "Tacos",
                description = "Corn tortillas filled with meat, vegetables, and salsa",
                imageResourceId = R.drawable.ic_launcher_foreground, // Placeholder
                cuisineType = "Mexican",
                prepTime = 30,
                isVegetarian = false,
                isVegan = false,
                isGlutenFree = true,
                spicyLevel = 3,
                sweetLevel = 1,
                savoryLevel = 4,
                complexity = 2
            ),
            Dish(
                id = 9,
                name = "Chocolate Cake",
                description = "Rich chocolate cake with frosting",
                imageResourceId = R.drawable.ic_launcher_foreground, // Placeholder
                cuisineType = "Dessert",
                prepTime = 50,
                isVegetarian = true,
                isVegan = false,
                isGlutenFree = false,
                spicyLevel = 0,
                sweetLevel = 5,
                savoryLevel = 1,
                complexity = 3
            ),
            Dish(
                id = 10,
                name = "Pho",
                description = "Vietnamese soup with rice noodles, herbs, and meat",
                imageResourceId = R.drawable.ic_launcher_foreground, // Placeholder
                cuisineType = "Vietnamese",
                prepTime = 45,
                isVegetarian = false,
                isVegan = false,
                isGlutenFree = true,
                spicyLevel = 2,
                sweetLevel = 1,
                savoryLevel = 5,
                complexity = 3
            )
        )

        // Create sample questions
        val questions = listOf(
            Question(
                id = 1,
                questionText = "Are you in the mood for something spicy?",
                questionType = Constants.QUESTION_TYPE_BINARY,
                choices = "Yes,No",
                attribute = "spicyLevel",
                weight = 8
            ),
            Question(
                id = 2,
                questionText = "Would you prefer a vegetarian dish?",
                questionType = Constants.QUESTION_TYPE_BINARY,
                choices = "Yes,No",
                attribute = "isVegetarian",
                weight = 9
            ),
            Question(
                id = 3,
                questionText = "Do you want something with meat?",
                questionType = Constants.QUESTION_TYPE_BINARY,
                choices = "Yes,No",
                attribute = "hasMeat",
                weight = 7
            ),
            Question(
                id = 4,
                questionText = "Are you looking for something quick to prepare (under 30 minutes)?",
                questionType = Constants.QUESTION_TYPE_BINARY,
                choices = "Yes,No",
                attribute = "prepTime",
                weight = 6
            ),
            Question(
                id = 5,
                questionText = "Do you prefer Asian cuisine?",
                questionType = Constants.QUESTION_TYPE_BINARY,
                choices = "Yes,No",
                attribute = "cuisineType",
                weight = 5
            ),
            Question(
                id = 6,
                questionText = "Do you want something gluten-free?",
                questionType = Constants.QUESTION_TYPE_BINARY,
                choices = "Yes,No",
                attribute = "isGlutenFree",
                weight = 7
            ),
            Question(
                id = 7,
                questionText = "Are you in the mood for Italian food?",
                questionType = Constants.QUESTION_TYPE_BINARY,
                choices = "Yes,No",
                attribute = "cuisineType",
                weight = 5
            ),
            Question(
                id = 8,
                questionText = "Are you looking for comfort food?",
                questionType = Constants.QUESTION_TYPE_BINARY,
                choices = "Yes,No",
                attribute = "comfort",
                weight = 4
            ),
            Question(
                id = 9,
                questionText = "Do you want a dessert?",
                questionType = Constants.QUESTION_TYPE_BINARY,
                choices = "Yes,No",
                attribute = "isDessert",
                weight = 9
            ),
            Question(
                id = 10,
                questionText = "Do you prefer something light?",
                questionType = Constants.QUESTION_TYPE_BINARY,
                choices = "Yes,No",
                attribute = "isLight",
                weight = 6
            )
        )

        // Create sample relations (simplified)
        val relations = mutableListOf<QuestionDishRelation>()

        // Q1: Spicy preference
        relations.addAll(generateSpicyRelations())

        // Q2: Vegetarian preference
        relations.addAll(generateVegetarianRelations())

        // Q3: Meat preference
        relations.addAll(generateMeatRelations())

        // Q4: Quick preparation
        relations.addAll(generateQuickPrepRelations())

        // Q5: Asian cuisine
        relations.addAll(generateAsianCuisineRelations())

        // Q6: Gluten-free
        relations.addAll(generateGlutenFreeRelations())

        // Q7: Italian food
        relations.addAll(generateItalianFoodRelations())

        // Q8: Comfort food
        relations.addAll(generateComfortFoodRelations())

        // Q9: Dessert
        relations.addAll(generateDessertRelations())

        // Q10: Light food
        relations.addAll(generateLightFoodRelations())

        // Insert all the data
        // Note: You'll need to add insert methods to your DAOs
        for (dish in dishes) {
            insertDish(dishDao, dish)
        }

        for (question in questions) {
            insertQuestion(questionDao, question)
        }

        for (relation in relations) {
            insertRelation(relationDao, relation)
        }
        // Insert data
        dishDao.insertAllDishes(dishes)
        questionDao.insertAllQuestions(questions)
        relationDao.insertAllRelations(relations)
    }

    // Helper methods to generate relations

    private fun generateSpicyRelations(): List<QuestionDishRelation> {
        val relations = mutableListOf<QuestionDishRelation>()

        // High spicy foods (3-5)
        for (dishId in listOf(3, 4, 8, 10)) {
            relations.add(
                QuestionDishRelation(
                    questionId = 1,
                    dishId = dishId,
                    answerValue = "Yes",
                    matchStrength = 0.9f
                )
            )
            relations.add(
                QuestionDishRelation(
                    questionId = 1,
                    dishId = dishId,
                    answerValue = "No",
                    matchStrength = 0.2f
                )
            )
        }

        // Low spicy foods (0-2)
        for (dishId in listOf(1, 2, 5, 6, 7, 9)) {
            relations.add(
                QuestionDishRelation(
                    questionId = 1,
                    dishId = dishId,
                    answerValue = "Yes",
                    matchStrength = 0.3f
                )
            )
            relations.add(
                QuestionDishRelation(
                    questionId = 1,
                    dishId = dishId,
                    answerValue = "No",
                    matchStrength = 0.9f
                )
            )
        }

        return relations
    }

    private fun generateVegetarianRelations(): List<QuestionDishRelation> {
        val relations = mutableListOf<QuestionDishRelation>()

        // Vegetarian dishes
        for (dishId in listOf(1, 4, 5, 9)) {
            relations.add(
                QuestionDishRelation(
                    questionId = 2,
                    dishId = dishId,
                    answerValue = "Yes",
                    matchStrength = 1.0f
                )
            )
            relations.add(
                QuestionDishRelation(
                    questionId = 2,
                    dishId = dishId,
                    answerValue = "No",
                    matchStrength = 0.3f
                )
            )
        }

        // Non-vegetarian dishes
        for (dishId in listOf(2, 3, 6, 7, 8, 10)) {
            relations.add(
                QuestionDishRelation(
                    questionId = 2,
                    dishId = dishId,
                    answerValue = "Yes",
                    matchStrength = 0.1f
                )
            )
            relations.add(
                QuestionDishRelation(
                    questionId = 2,
                    dishId = dishId,
                    answerValue = "No",
                    matchStrength = 0.9f
                )
            )
        }

        return relations
    }

    // Add similar methods for other question relations...

    private fun generateMeatRelations(): List<QuestionDishRelation> {
        val relations = mutableListOf<QuestionDishRelation>()

        // Meat dishes
        for (dishId in listOf(2, 3, 6, 7, 8, 10)) {
            relations.add(
                QuestionDishRelation(
                    questionId = 3,
                    dishId = dishId,
                    answerValue = "Yes",
                    matchStrength = 0.9f
                )
            )
            relations.add(
                QuestionDishRelation(
                    questionId = 3,
                    dishId = dishId,
                    answerValue = "No",
                    matchStrength = 0.2f
                )
            )
        }

        // Non-meat dishes
        for (dishId in listOf(1, 4, 5, 9)) {
            relations.add(
                QuestionDishRelation(
                    questionId = 3,
                    dishId = dishId,
                    answerValue = "Yes",
                    matchStrength = 0.2f
                )
            )
            relations.add(
                QuestionDishRelation(
                    questionId = 3,
                    dishId = dishId,
                    answerValue = "No",
                    matchStrength = 0.9f
                )
            )
        }

        return relations
    }

    private fun generateQuickPrepRelations(): List<QuestionDishRelation> {
        val relations = mutableListOf<QuestionDishRelation>()

        // Quick prep dishes (< 30 min)
        for (dishId in listOf(2, 3, 5, 7)) {
            relations.add(
                QuestionDishRelation(
                    questionId = 4,
                    dishId = dishId,
                    answerValue = "Yes",
                    matchStrength = 0.9f
                )
            )
            relations.add(
                QuestionDishRelation(
                    questionId = 4,
                    dishId = dishId,
                    answerValue = "No",
                    matchStrength = 0.3f
                )
            )
        }

        // Longer prep dishes (>= 30 min)
        for (dishId in listOf(1, 4, 6, 8, 9, 10)) {
            relations.add(
                QuestionDishRelation(
                    questionId = 4,
                    dishId = dishId,
                    answerValue = "Yes",
                    matchStrength = 0.3f
                )
            )
            relations.add(
                QuestionDishRelation(
                    questionId = 4,
                    dishId = dishId,
                    answerValue = "No",
                    matchStrength = 0.9f
                )
            )
        }

        return relations
    }

    private fun generateAsianCuisineRelations(): List<QuestionDishRelation> {
        val relations = mutableListOf<QuestionDishRelation>()

        // Asian cuisine dishes
        for (dishId in listOf(3, 6, 10)) {
            relations.add(
                QuestionDishRelation(
                    questionId = 5,
                    dishId = dishId,
                    answerValue = "Yes",
                    matchStrength = 1.0f
                )
            )
            relations.add(
                QuestionDishRelation(
                    questionId = 5,
                    dishId = dishId,
                    answerValue = "No",
                    matchStrength = 0.2f
                )
            )
        }

        // Non-Asian cuisine dishes
        for (dishId in listOf(1, 2, 4, 5, 7, 8, 9)) {
            relations.add(
                QuestionDishRelation(
                    questionId = 5,
                    dishId = dishId,
                    answerValue = "Yes",
                    matchStrength = 0.1f
                )
            )
            relations.add(
                QuestionDishRelation(
                    questionId = 5,
                    dishId = dishId,
                    answerValue = "No",
                    matchStrength = 0.9f
                )
            )
        }

        return relations
    }

    // Continue with the remaining relation generators...

    private fun generateGlutenFreeRelations(): List<QuestionDishRelation> {
        // Similar implementation
        return listOf()
    }

    private fun generateItalianFoodRelations(): List<QuestionDishRelation> {
        // Similar implementation
        return listOf()
    }

    private fun generateComfortFoodRelations(): List<QuestionDishRelation> {
        // Similar implementation
        return listOf()
    }

    private fun generateDessertRelations(): List<QuestionDishRelation> {
        // Similar implementation
        return listOf()
    }

    private fun generateLightFoodRelations(): List<QuestionDishRelation> {
        // Similar implementation
        return listOf()
    }

    // You'll need to add these methods to your DAOs
    private suspend fun insertDish(dishDao: DishDao, dish: Dish) {
        // Implementation depends on your DAO
    }

    private suspend fun insertQuestion(questionDao: QuestionDao, question: Question) {
        // Implementation depends on your DAO
    }

    private suspend fun insertRelation(relationDao: RelationDao, relation: QuestionDishRelation) {
        // Implementation depends on your DAO
    }

}
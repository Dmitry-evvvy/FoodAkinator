package com.example.foodakinator.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dishes")
data class Dish(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val imageResourceId: Int,
    val cuisineType: String,
    val prepTime: Int,
    val isVegetarian: Boolean,
    val isVegan: Boolean,
    val isGlutenFree: Boolean,
    val spicyLevel: Int, // 0-5 scale
    val sweetLevel: Int, // 0-5 scale
    val savoryLevel: Int, // 0-5 scale
    val complexity: Int, // 1-5 scale

    // KEEP: Your existing optional attributes
    val proteinType: String = "", // "chicken", "beef", "fish", "none"
    val cookingMethod: String = "", // "grilled", "fried", "baked"
    val isComfortFood: Boolean = false,
    val mealType: String = "", // "breakfast", "lunch", "dinner", "dessert", "snack"

    // UPDATE: Replace these with the new enhanced fields
    val mainIngredients: String = "", // comma-separated list: "chicken,garlic,herbs"
    val cookingMethods: String = "", // comma-separated list: "grilled,seasoned"
    val servingTemperature: String = "hot", // "hot", "cold", "room temperature"
    val textureProfile: String = "", // comma-separated: "crispy,tender" or "creamy,smooth"
    val allergens: String = "", // comma-separated: "nuts,dairy,gluten"
    val nutritionalHighlights: String = "" // comma-separated: "high-protein,low-carb,fiber-rich"
)

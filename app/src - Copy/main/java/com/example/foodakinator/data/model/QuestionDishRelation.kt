package com.example.foodakinator.data.model

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "question_dish_relations",
    primaryKeys = ["questionId", "dishId", "answerValue"],
    foreignKeys = [
        ForeignKey(
            entity = Question::class,
            parentColumns = ["id"],
            childColumns = ["questionId"]
        ),
        ForeignKey(
            entity = Dish::class,
            parentColumns = ["id"],
            childColumns = ["dishId"]
        )
    ]
)
data class QuestionDishRelation(
    val questionId: Int,
    val dishId: Int,
    val answerValue: String, // The answer that links this question to this dish
    val matchStrength: Float // How strongly this answer matches this dish (0.0-1.0)
)
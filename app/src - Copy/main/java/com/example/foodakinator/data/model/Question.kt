package com.example.foodakinator.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class Question(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val questionText: String,
    val questionType: String, // BINARY (yes/no), MULTIPLE_CHOICE, RATING
    val choices: String, // JSON string representation of choices
    val attribute: String, // Which dish attribute this question relates to
    val weight: Int // Importance of this question (1-10)
)
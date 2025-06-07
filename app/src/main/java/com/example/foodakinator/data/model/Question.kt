package com.example.foodakinator.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class Question(
    @PrimaryKey
    val id: Int,
    val questionText: String,
    val questionType: String,
    val choices: String,
    val attribute: String,
    val weight: Int
)
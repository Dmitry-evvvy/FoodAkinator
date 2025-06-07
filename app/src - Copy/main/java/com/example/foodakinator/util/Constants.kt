package com.example.foodakinator.util

object Constants {
    // Question types
    const val QUESTION_TYPE_BINARY = "BINARY"
    const val QUESTION_TYPE_MULTIPLE_CHOICE = "MULTIPLE_CHOICE"
    const val QUESTION_TYPE_RATING = "RATING"

    // Max number of questions
    const val MAX_QUESTIONS = 25

    // Score thresholds
    const val CONFIDENCE_THRESHOLD = 0.7f
    const val SCORE_DIFFERENCE_THRESHOLD = 0.3f
}
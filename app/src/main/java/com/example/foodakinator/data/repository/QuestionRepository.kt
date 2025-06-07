package com.example.foodakinator.data.repository

import com.example.foodakinator.data.database.dao.QuestionDao
import com.example.foodakinator.data.model.Question

class QuestionRepository(private val questionDao: QuestionDao) {
    suspend fun getAllQuestions(): List<Question> {
        return questionDao.getAllQuestions()
    }

    suspend fun getQuestionById(id: Int): Question {
        return questionDao.getQuestionById(id)
    }
}
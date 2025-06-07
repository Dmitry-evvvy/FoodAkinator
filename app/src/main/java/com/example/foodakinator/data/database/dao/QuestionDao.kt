package com.example.foodakinator.data.database.dao

import androidx.room.*
import com.example.foodakinator.data.model.Question

@Dao
interface QuestionDao {

    @Query("SELECT * FROM questions")
    suspend fun getAllQuestions(): List<Question>

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getQuestionById(id: Int): Question

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(question: Question)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllQuestions(questions: List<Question>)  // RENAMED to avoid conflict

    @Update
    suspend fun update(question: Question)

    @Delete
    suspend fun delete(question: Question)

    @Query("DELETE FROM questions")
    suspend fun deleteAll()

    @Query("SELECT * FROM questions WHERE questionType = :type")
    suspend fun getQuestionsByType(type: String): List<Question>

    @Query("SELECT * FROM questions WHERE weight >= :minWeight ORDER BY weight DESC")
    suspend fun getHighPriorityQuestions(minWeight: Int): List<Question>
}
package com.example.foodakinator.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.foodakinator.data.model.QuestionDishRelation

@Dao
interface RelationDao {
    @Query("SELECT * FROM question_dish_relations WHERE questionId = :questionId")
    suspend fun getRelationsForQuestion(questionId: Int): List<QuestionDishRelation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(relation: QuestionDishRelation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(relations: List<QuestionDishRelation>)

    @Query("SELECT COUNT(*) FROM question_dish_relations")
    suspend fun getCount(): Int
}
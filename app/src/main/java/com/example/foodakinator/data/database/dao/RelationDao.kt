package com.example.foodakinator.data.database.dao

import androidx.room.*
import com.example.foodakinator.data.model.QuestionDishRelation

@Dao
interface RelationDao {

    @Query("SELECT * FROM question_dish_relations")
    suspend fun getAllRelations(): List<QuestionDishRelation>

    @Query("SELECT * FROM question_dish_relations WHERE questionId = :questionId")
    suspend fun getRelationsByQuestion(questionId: Int): List<QuestionDishRelation>

    @Query("SELECT * FROM question_dish_relations WHERE dishId = :dishId")
    suspend fun getRelationsByDish(dishId: Int): List<QuestionDishRelation>

    @Query("SELECT COUNT(*) FROM question_dish_relations")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(relation: QuestionDishRelation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRelations(relations: List<QuestionDishRelation>)  // RENAMED to avoid conflict

    @Update
    suspend fun update(relation: QuestionDishRelation)

    @Delete
    suspend fun delete(relation: QuestionDishRelation)

    @Query("DELETE FROM question_dish_relations")
    suspend fun deleteAll()
}
package com.example.foodakinator.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.foodakinator.data.model.Dish

// DishDao.kt
@Dao
interface DishDao {
    @Query("SELECT * FROM dishes")
    suspend fun getAllDishes(): List<Dish>

    @Query("SELECT * FROM dishes WHERE id = :id")
    suspend fun getDishById(id: Int): Dish

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dish: Dish)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dishes: List<Dish>)

    @Query("SELECT COUNT(*) FROM dishes")
    suspend fun getCount(): Int
}

// Similar updates for QuestionDao and RelationDao
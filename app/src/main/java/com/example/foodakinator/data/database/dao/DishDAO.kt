package com.example.foodakinator.data.database.dao

import androidx.room.*
import com.example.foodakinator.data.model.Dish

@Dao
interface DishDao {

    @Query("SELECT * FROM dishes")
    suspend fun getAllDishes(): List<Dish>

    @Query("SELECT * FROM dishes WHERE id = :id")
    suspend fun getDishById(id: Int): Dish

    @Query("SELECT COUNT(*) FROM dishes")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dish: Dish)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDishes(dishes: List<Dish>)  // RENAMED to avoid conflict

    @Update
    suspend fun update(dish: Dish)

    @Delete
    suspend fun delete(dish: Dish)

    @Query("DELETE FROM dishes")
    suspend fun deleteAll()

    @Query("SELECT * FROM dishes WHERE cuisineType = :cuisineType")
    suspend fun getDishesByCuisine(cuisineType: String): List<Dish>

    @Query("SELECT * FROM dishes WHERE isVegetarian = 1")
    suspend fun getVegetarianDishes(): List<Dish>

    @Query("SELECT * FROM dishes WHERE prepTime <= :maxTime")
    suspend fun getQuickDishes(maxTime: Int): List<Dish>
}
package com.example.foodakinator.data.repository

import com.example.foodakinator.data.database.dao.DishDao
import com.example.foodakinator.data.database.dao.RelationDao
import com.example.foodakinator.data.model.Dish

class DishRepository(
    private val dishDao: DishDao,
    private val relationDao: RelationDao
) {

    suspend fun getAllDishes(): List<Dish> {
        return dishDao.getAllDishes()
    }

    suspend fun getDishById(id: Int): Dish {
        return dishDao.getDishById(id)
    }

    suspend fun insertDish(dish: Dish) {
        dishDao.insert(dish)
    }

    suspend fun insertAllDishes(dishes: List<Dish>) {
        dishDao.insertAllDishes(dishes)  // UPDATED method name
    }
}
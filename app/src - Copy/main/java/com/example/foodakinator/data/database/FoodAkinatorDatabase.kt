package com.example.foodakinator.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.foodakinator.data.database.dao.DishDao
import com.example.foodakinator.data.database.dao.QuestionDao
import com.example.foodakinator.data.database.dao.RelationDao
import com.example.foodakinator.data.model.Dish
import com.example.foodakinator.data.model.Question
import com.example.foodakinator.data.model.QuestionDishRelation

@Database(
    entities = [Dish::class, Question::class, QuestionDishRelation::class],
    version = 3, // CHANGED: Incremented from 1 to 2
    exportSchema = false
)
abstract class FoodAkinatorDatabase : RoomDatabase() {
    abstract fun dishDao(): DishDao
    abstract fun questionDao(): QuestionDao
    abstract fun relationDao(): RelationDao

    companion object {
        @Volatile
        private var INSTANCE: FoodAkinatorDatabase? = null

        fun getDatabase(context: Context): FoodAkinatorDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FoodAkinatorDatabase::class.java,
                    "food_akinator_database"
                )
                    .fallbackToDestructiveMigration() // ADDED: This will recreate the database when schema changes
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
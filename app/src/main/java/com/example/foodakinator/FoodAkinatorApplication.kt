package com.example.foodakinator

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.foodakinator.data.database.FoodAkinatorDatabase
import com.example.foodakinator.util.DataLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FoodAkinatorApplication : Application() {

    private val applicationScope = CoroutineScope(Dispatchers.Default)

    // Simplified database initialization - NO DataInitializer
    val database by lazy {
        FoodAkinatorDatabase.getDatabase(this)
    }

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreate() {
        super.onCreate()

        Log.d("FoodAkinator", "APPLICATION STARTED - Loading data from JSON only")

        applicationScope.launch {
            try {
                Log.d("FoodAkinator", "Loading data from JSON files...")
                loadDataFromJsonOnly()
                Log.d("FoodAkinator", "JSON data loading completed")
            } catch (e: Exception) {
                Log.e("FoodAkinator", "ERROR loading JSON data", e)
            }
        }
    }

    private suspend fun loadDataFromJsonOnly() {
        val dataVersion = prefs.getInt("DATA_VERSION", 0)
        val currentDataVersion = 5 // Increment when you update JSON files

        Log.d("FoodAkinatorApp", "Current data version: $dataVersion, target: $currentDataVersion")

        if (dataVersion < currentDataVersion) {
            try {
                Log.d("FoodAkinatorApp", "Loading data version $currentDataVersion (was $dataVersion)")

                val dataLoader = DataLoader(this)

                // Clear existing data first
                database.dishDao().deleteAll()
                database.questionDao().deleteAll()
                database.relationDao().deleteAll()

                // Load fresh data from JSON
                dataLoader.loadDataIntoDatabase(database)

                // Update data version preference
                prefs.edit().putInt("DATA_VERSION", currentDataVersion).apply()

                Log.d("FoodAkinatorApp", "Data loaded successfully from JSON files")

                // Log database statistics
                val dishCount = database.dishDao().getCount()
                val questionCount = database.questionDao().getCount()
                val relationCount = database.relationDao().getCount()

                Log.d("FoodAkinatorApp", "FINAL DATABASE STATS: $dishCount dishes, $questionCount questions, $relationCount relations")

            } catch (e: Exception) {
                Log.e("FoodAkinatorApp", "Error loading JSON data", e)
            }
        } else {
            Log.d("FoodAkinatorApp", "Data is up to date, skipping load")
        }
    }
}
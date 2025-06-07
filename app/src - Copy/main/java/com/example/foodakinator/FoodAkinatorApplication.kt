// STEP 1: Update FoodAkinatorApplication.kt

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

    // Lazy initialization of database
    val database by lazy {
        FoodAkinatorDatabase.getDatabase(this)
    }

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreate() {
        super.onCreate()

        Log.e("FoodAkinator", "APPLICATION STARTED - WILL LOAD DATA")

        applicationScope.launch {
            try {
                Log.e("FoodAkinator", "ATTEMPTING TO LOAD DATA")
                initializeData()
                Log.e("FoodAkinator", "DATA LOADING COMPLETED")
            } catch (e: Exception) {
                Log.e("FoodAkinator", "ERROR LOADING DATA", e)
            }
        }
    }

    private suspend fun initializeData() {
        val dataVersion = prefs.getInt("DATA_VERSION", 0)
        val currentDataVersion = 4 // INCREASED: Force reload

        Log.d("FoodAkinatorApp", "Current data version: $dataVersion, target: $currentDataVersion")

        // ALWAYS load data for now to debug the issue
        try {
            Log.d("FoodAkinatorApp", "Loading data version $currentDataVersion (was $dataVersion)")

            val dataLoader = DataLoader(this)

            // Verify assets first
            dataLoader.verifyAssets()

            // Force load data
            dataLoader.loadDataIntoDatabase(database)

            // Update data version preference
            prefs.edit().putInt("DATA_VERSION", currentDataVersion).apply()

            Log.d("FoodAkinatorApp", "Data loaded successfully from JSON files")

            // Log database statistics for debugging
            val dishCount = database.dishDao().getCount()
            val questionCount = database.questionDao().getCount()
            val relationCount = database.relationDao().getCount()

            Log.e("FoodAkinatorApp", "FINAL DATABASE STATS: $dishCount dishes, $questionCount questions, $relationCount relations")

        } catch (e: Exception) {
            Log.e("FoodAkinatorApp", "Error loading data", e)
        }
    }
}
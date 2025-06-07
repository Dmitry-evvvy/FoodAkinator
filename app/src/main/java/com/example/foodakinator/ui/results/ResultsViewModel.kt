package com.example.foodakinator.ui.results

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.foodakinator.data.database.FoodAkinatorDatabase
import com.example.foodakinator.data.model.Dish
import com.example.foodakinator.data.repository.DishRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResultsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = FoodAkinatorDatabase.getDatabase(application)
    private val dishRepository = DishRepository(database.dishDao(), database.relationDao())

    private val _dish = MutableLiveData<Dish>()
    val dish: LiveData<Dish> = _dish

    fun loadDish(id: Int) {
        viewModelScope.launch {
            val loadedDish = withContext(Dispatchers.IO) {
                dishRepository.getDishById(id)
            }
            _dish.value = loadedDish
        }
    }
}
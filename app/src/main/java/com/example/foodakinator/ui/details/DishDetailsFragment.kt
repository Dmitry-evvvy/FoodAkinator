package com.example.foodakinator.ui.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.foodakinator.databinding.FragmentDishDetailsBinding

class DishDetailsFragment : Fragment() {

    private var _binding: FragmentDishDetailsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DishDetailsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDishDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[DishDetailsViewModel::class.java]

        // Get dish ID from arguments
        val dishId = arguments?.getInt("dishId") ?: -1

        if (dishId != -1) {
            // Load dish details
            viewModel.loadDish(dishId)
        }

        // Observe dish data
        viewModel.dish.observe(viewLifecycleOwner) { dish ->
            // Update UI with dish details
            binding.dishName.text = dish.name
            binding.dishDescription.text = dish.description

            // Set attributes - these match the minimal layout
            binding.cuisineType.text = "Cuisine: ${dish.cuisineType}"
            binding.prepTime.text = "Prep time: ${dish.prepTime} minutes"
            binding.vegetarian.text = "Vegetarian: ${if (dish.isVegetarian) "Yes" else "No"}"
            binding.vegan.text = "Vegan: ${if (dish.isVegan) "Yes" else "No"}"
            binding.glutenFree.text = "Gluten-free: ${if (dish.isGlutenFree) "Yes" else "No"}"
            binding.spicyLevel.text = "Spicy level: ${getSpicyLevelText(dish.spicyLevel)}"
        }

        // Set up back button
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun getSpicyLevelText(level: Int): String {
        return when (level) {
            0 -> "Not Spicy"
            1 -> "Very Mild"
            2 -> "Mild"
            3 -> "Medium"
            4 -> "Spicy"
            5 -> "Very Spicy"
            else -> "Unknown"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.example.foodakinator.ui.results

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodakinator.data.model.Dish

class SuggestionAdapter(private val onItemClick: (Dish) -> Unit) :
    RecyclerView.Adapter<SuggestionAdapter.ViewHolder>() {

    private val items = mutableListOf<Dish>()

    fun submitList(newItems: List<Dish>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dish = items[position]
        holder.textView.text = dish.name

        holder.itemView.setOnClickListener {
            onItemClick(dish)
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(android.R.id.text1)
    }
}
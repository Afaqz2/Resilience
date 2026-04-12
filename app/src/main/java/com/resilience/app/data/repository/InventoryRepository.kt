package com.resilience.app.data.repository

import com.resilience.app.data.model.InventoryItem
import com.resilience.app.data.model.ItemCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRepository @Inject constructor() {

    // Initial default items, with quantities set to 0 as requested for "most important supplies"
    private val defaultItems = listOf(
        InventoryItem(1, "Water (Gallons)",   ItemCategory.HYDRATION,  0),
        InventoryItem(2, "MRE Rations",       ItemCategory.NUTRITION,  0),
        InventoryItem(3, "First Aid Kit",     ItemCategory.MEDICAL,     0),
        InventoryItem(4, "Batteries (AA)",    ItemCategory.POWER,       0),
        InventoryItem(5, "Iodine Tablets",    ItemCategory.MEDICAL,     0),
        InventoryItem(6, "Radio (Hand Crank)",ItemCategory.COMMS,        0),
        InventoryItem(7, "Gas Mask Filters",  ItemCategory.PROTECTION,  0),
        InventoryItem(8, "Flashlight",        ItemCategory.GEAR,         0)
    )

    private val _items = MutableStateFlow<List<InventoryItem>>(defaultItems)
    val items: StateFlow<List<InventoryItem>> = _items.asStateFlow()

    private var nextId = defaultItems.size.toLong() + 1

    fun addItem(name: String, category: ItemCategory, quantity: Int) {
        _items.update { currentList ->
            currentList + InventoryItem(nextId++, name, category, quantity)
        }
    }

    fun updateQuantity(id: Long, newQuantity: Int) {
        val validQuantity = maxOf(0, newQuantity)
        _items.update { currentList ->
            currentList.map { item ->
                if (item.id == id) item.copy(quantity = validQuantity) else item
            }
        }
    }
}

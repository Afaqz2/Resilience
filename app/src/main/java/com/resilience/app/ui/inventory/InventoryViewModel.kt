package com.resilience.app.ui.inventory

import androidx.lifecycle.ViewModel
import com.resilience.app.data.model.InventoryItem
import com.resilience.app.data.model.ItemCategory
import com.resilience.app.data.repository.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repository: InventoryRepository
) : ViewModel() {

    val items: StateFlow<List<InventoryItem>> = repository.items

    // In-memory simplistic onboarding check for demonstration
    // True if NO items have any quantity.
    private val _hasSeenOnboarding = MutableStateFlow(false)
    val hasSeenOnboarding: StateFlow<Boolean> = _hasSeenOnboarding.asStateFlow()

    init {
        // Automatically determine if we should show onboarding
        // if everything is empty and user hasn't seen it yet.
        _hasSeenOnboarding.value = repository.items.value.all { it.quantity == 0 }
    }

    fun markOnboardingSeen() {
        _hasSeenOnboarding.value = true
    }

    fun addItem(name: String, category: ItemCategory, quantity: Int) {
        repository.addItem(name, category, quantity)
    }

    fun updateQuantity(id: Long, newQuantity: Int) {
        repository.updateQuantity(id, newQuantity)
    }
}

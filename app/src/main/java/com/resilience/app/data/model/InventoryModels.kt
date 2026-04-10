package com.resilience.app.data.model

enum class ItemCategory(val label: String) {
    HYDRATION("HYDRATION"),
    NUTRITION("NUTRITION"),
    MEDICAL("MEDICAL"),
    POWER("POWER"),
    COMMS("COMMS"),
    PROTECTION("PROTECTION"),
    GEAR("GEAR"),
    OTHER("OTHER") // Added as requested
}

enum class StockStatus { STOCKED, LOW, CRITICAL }

data class InventoryItem(
    val id: Long,
    val name: String,
    val category: ItemCategory,
    val quantity: Int
) {
    val status: StockStatus
        get() = when {
            quantity <= 3  -> StockStatus.CRITICAL
            quantity <= 8  -> StockStatus.LOW
            else           -> StockStatus.STOCKED
        }
}

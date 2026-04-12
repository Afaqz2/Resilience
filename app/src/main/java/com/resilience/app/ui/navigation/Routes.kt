package com.resilience.app.ui.navigation

/**
 * All named routes for the Resilience Nav graph.
 *
 * Simple string constants keep the nav calls compile-safe without
 * the overhead of a sealed class until we need typed arguments.
 */
object Routes {
    const val DASHBOARD       = "dashboard"
    const val PLAYBOOK_LIST   = "playbook_list"
    const val PLAYBOOK_DETAIL = "playbook_detail/{playbookId}"
    const val FAMILY_VAULT    = "family_vault"
    const val MAPS            = "maps"
    const val INVENTORY       = "inventory"
    const val RADIO           = "radio"
    const val AI_CHAT         = "ai_chat"
    const val ALERTS          = "alerts"
    const val SETTINGS        = "settings"

    fun playbookDetail(id: String) = "playbook_detail/$id"
}

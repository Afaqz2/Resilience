package com.resilience.app.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.resilience.app.data.db.entity.PlaybookEntity
import com.resilience.app.ui.aichat.AiChatScreen
import com.resilience.app.ui.alerts.AlertsScreen
import com.resilience.app.ui.dashboard.DashboardScreen
import com.resilience.app.ui.familyvault.FamilyVaultScreen
import com.resilience.app.ui.inventory.InventoryScreen
import com.resilience.app.ui.maps.MapScreen
import com.resilience.app.ui.playbooks.PlaybookDetailScreen
import com.resilience.app.ui.playbooks.PlaybookListScreen
import com.resilience.app.ui.playbooks.PlaybookViewModel
import com.resilience.app.ui.radio.RadioFrequencyScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.resilience.app.ui.settings.SettingsScreen

/**
 * Root navigation graph for the Resilience app.
 *
 * Note: PlaybookViewModel is scoped to the nav back-stack entry so both
 * the List and Detail screens share the same instance and its state
 * (selected playbook, TTS status, category filter) survives screen transitions.
 */
@Composable
fun ResilienceNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD
    ) {

        // ── Dashboard ──────────────────────────────────────────────────────
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onNavigateToPlaybooks   = { navController.navigate(Routes.PLAYBOOK_LIST) },
                onNavigateToFamilyVault = { navController.navigate(Routes.FAMILY_VAULT) },
                onNavigateToMaps        = { navController.navigate(Routes.MAPS) },
                onNavigateToInventory   = { navController.navigate(Routes.INVENTORY) },
                onNavigateToRadio       = { navController.navigate(Routes.RADIO) },
                onNavigateToAiChat      = { navController.navigate(Routes.AI_CHAT) },
                onNavigateToAlerts      = { navController.navigate(Routes.ALERTS) },
                onNavigateToSettings    = { navController.navigate(Routes.SETTINGS) }
            )
        }

        // ── Playbook List ─────────────────────────────────────────────────
        composable(Routes.PLAYBOOK_LIST) { backEntry ->
            val viewModel: PlaybookViewModel = hiltViewModel(backEntry)
            PlaybookListScreen(
                viewModel = viewModel,
                onPlaybookClick = { playbook ->
                    // Pass ID via route; ViewModel is retrieved from back-stack in Detail
                    navController.navigate(Routes.playbookDetail(playbook.id))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Playbook Detail ───────────────────────────────────────────────
        composable(
            route = Routes.PLAYBOOK_DETAIL,
            arguments = listOf(navArgument("playbookId") { type = NavType.StringType })
        ) { backEntry ->
            val playbookId = backEntry.arguments?.getString("playbookId") ?: return@composable

            // Share ViewModel with the List entry so TTS state is preserved on Back
            val listEntry = remember(backEntry) {
                navController.getBackStackEntry(Routes.PLAYBOOK_LIST)
            }
            val viewModel: PlaybookViewModel = hiltViewModel(listEntry)
            val filteredPlaybooks by viewModel.filteredPlaybooks.collectAsState()

            var playbook by remember { mutableStateOf<PlaybookEntity?>(null) }

            LaunchedEffect(playbookId, filteredPlaybooks) {
                playbook = filteredPlaybooks.find { it.id == playbookId }
            }

            playbook?.let { book ->
                PlaybookDetailScreen(
                    playbook = book,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // ── Family Vault ──────────────────────────────────────────────────
        composable(Routes.FAMILY_VAULT) {
            FamilyVaultScreen(onBack = { navController.popBackStack() })
        }

        // ── Maps ──────────────────────────────────────────────────────────
        composable(Routes.MAPS) {
            MapScreen(onBack = { navController.popBackStack() })
        }

        // ── Inventory ─────────────────────────────────────────────────────
        composable(Routes.INVENTORY) {
            InventoryScreen(onBack = { navController.popBackStack() })
        }

        // ── RF Mesh Radio ─────────────────────────────────────────────────
        composable(Routes.RADIO) {
            RadioFrequencyScreen(
                onBack            = { navController.popBackStack() },
                onNavigateToAiChat = { navController.navigate(Routes.AI_CHAT) }
            )
        }

        // ── AI Chat ───────────────────────────────────────────────────────
        composable(Routes.AI_CHAT) {
            AiChatScreen(onBack = { navController.popBackStack() })
        }

        // ── Alerts ────────────────────────────────────────────────────────
        composable(Routes.ALERTS) {
            AlertsScreen(onBack = { navController.popBackStack() })
        }

        // ── Settings ──────────────────────────────────────────────────────
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

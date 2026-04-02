package com.resilience.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.resilience.app.ui.navigation.ResilienceNavGraph
import com.resilience.app.ui.theme.ResilienceTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var dataStore: com.resilience.app.data.datastore.CrisisModeDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isCrisisMode by dataStore.isCrisisMode.collectAsState(initial = false)
            val isExtremeBatteryMode by dataStore.isExtremeBatteryMode.collectAsState(initial = false)

            ResilienceTheme(
                isCrisisMode = isCrisisMode,
                isExtremeBatteryMode = isExtremeBatteryMode
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    ResilienceNavGraph(navController = navController)
                }
            }
        }
    }
}

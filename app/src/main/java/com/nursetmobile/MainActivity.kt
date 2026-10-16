package com.nursetmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.nursetmobile.ui.navigation.NursetNavHost
import com.nursetmobile.viewmodel.HomeViewModel

/**
 * Главная активность приложения.
 * Настраивает навигацию и тему Material 3.
 */
class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModel.Factory((application as NursetApplication).database)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Включаем edge-to-edge (опционально, для красивого вида)
        enableEdgeToEdge()
        setContent {
            // Тема Material 3 с поддержкой светлой/тёмной темы
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Навигационный хост
                    val navController = rememberNavController()
                    NursetNavHost(navController, homeViewModel)
                }
            }
        }
    }
}

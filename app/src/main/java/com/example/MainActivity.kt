package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.UangKuAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinancialViewModel
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: FinancialViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            
            // Dynamic evaluation of Light / Dark mode based on user's manual preference or timezone time
            val isDarkTheme = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> { // AUTO MODE (18:00 to 06:00 is dark)
                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val isSystemDark = isSystemInDarkTheme()
                    // Default to night check fallback to system if user wants adaptive
                    hour < 6 || hour >= 18
                }
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                UangKuAppScreen(viewModel = viewModel)
            }
        }
    }
}

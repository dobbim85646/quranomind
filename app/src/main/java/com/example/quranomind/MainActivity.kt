package com.example.quranomind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.quranomind.ui.screens.MainScreen
import com.example.quranomind.ui.theme.QuranoMindTheme
import com.example.quranomind.ui.viewmodel.QuranViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: QuranViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val isEnglish by viewModel.isEnglish.collectAsState()

            QuranoMindTheme(darkTheme = isDarkMode) {
                // Support proper RTL for Arabic, LTR for English
                val layoutDirection = if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl

                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

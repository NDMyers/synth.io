package com.synthio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synthio.app.ui.screens.SynthesizerScreen
import com.synthio.app.ui.theme.BackgroundCream
import com.synthio.app.ui.theme.DarkSurface
import com.synthio.app.ui.theme.SynthioTheme
import com.synthio.app.viewmodel.SynthViewModel

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            SynthioTheme {
                val synthViewModel: SynthViewModel = viewModel()
                val context = LocalContext.current
                val isDarkMode = synthViewModel.isDarkMode
                
                // Update system bar colors when dark mode changes
                DisposableEffect(isDarkMode) {
                    val statusBarColor = if (isDarkMode) DarkSurface.toArgb() else BackgroundCream.toArgb()
                    val navigationBarColor = if (isDarkMode) DarkSurface.toArgb() else BackgroundCream.toArgb()
                    
                    enableEdgeToEdge(
                        statusBarStyle = if (isDarkMode) {
                            SystemBarStyle.dark(statusBarColor)
                        } else {
                            SystemBarStyle.light(statusBarColor, statusBarColor)
                        },
                        navigationBarStyle = if (isDarkMode) {
                            SystemBarStyle.dark(navigationBarColor)
                        } else {
                            SystemBarStyle.light(navigationBarColor, navigationBarColor)
                        }
                    )
                    onDispose { }
                }
                
                // Start/stop engine with lifecycle - inside setContent to avoid race condition
                DisposableEffect(synthViewModel) {
                    synthViewModel.startEngine()
                    onDispose {
                        synthViewModel.stopEngine()
                    }
                }
                
                // Initialize MIDI support
                LaunchedEffect(Unit) {
                    synthViewModel.initializeMidi(context)
                }
                
                SynthesizerScreen(
                    viewModel = synthViewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                )
            }
        }
    }
}

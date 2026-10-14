package io.github.deveshb1.peptideessentials

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.deveshb1.peptideessentials.ui.AppRoot
import io.github.deveshb1.peptideessentials.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val data by repository.data.collectAsStateWithLifecycle()
            AppTheme(dynamicColor = data.settings.dynamicColor) {
                AppRoot()
            }
        }
    }
}

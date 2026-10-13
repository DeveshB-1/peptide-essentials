package io.github.deveshb1.doselog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.deveshb1.doselog.ui.DoseLogRoot
import io.github.deveshb1.doselog.ui.theme.DoseLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            DoseLogTheme {
                DoseLogRoot()
            }
        }
    }
}

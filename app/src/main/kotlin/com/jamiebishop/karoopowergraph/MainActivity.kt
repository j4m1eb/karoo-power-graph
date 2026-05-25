package com.jamiebishop.karoopowergraph

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.hammerhead.karooext.KarooSystemService
import com.jamiebishop.karoopowergraph.screens.MainScreen
import com.jamiebishop.karoopowergraph.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                MainScreen(close = { finish() })
            }
        }
    }
}

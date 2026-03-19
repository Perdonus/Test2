package com.perdonus.r34viewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.perdonus.r34viewer.ui.R34App
import com.perdonus.r34viewer.ui.theme.R34Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            R34Theme {
                R34App()
            }
        }
    }
}

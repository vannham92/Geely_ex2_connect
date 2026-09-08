package com.example.ex2_phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ex2_phone.ui.MainApp
import com.example.ex2_phone.ui.theme.Ex2phoneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Ex2phoneTheme {
                MainApp()
            }
        }
    }
}
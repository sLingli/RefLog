package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.MaterialTheme

class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 确保 Activity 窗口背景为黑色
        window.decorView.setBackgroundColor(android.graphics.Color.BLACK)

        setContent {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                MaterialTheme {
                    AboutScreen()
                }
            }
        }
    }
}


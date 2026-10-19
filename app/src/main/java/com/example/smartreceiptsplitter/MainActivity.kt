package com.example.smartreceiptsplitter

import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.example.smartreceiptsplitter.ui.navigation.SmartReceiptSplitterNavHost
import com.example.smartreceiptsplitter.ui.theme.SmartReceiptSplitterTheme
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartReceiptSplitterTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SmartReceiptSplitterNavHost()
                }
            }
        }
    }
}
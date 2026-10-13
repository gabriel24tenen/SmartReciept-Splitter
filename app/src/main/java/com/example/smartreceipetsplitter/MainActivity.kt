package com.example.smartreceipetsplitter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.smartreceipetsplitter.ui.theme.SmartReceipetSplitterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartReceipetSplitterTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    //TODO: replace it with NavHost when first screens are created
                    Text(text = stringPlaceholder)
                }
            }
        }
    }
}

//placeholder for current rendering
private const val stringPlaceholder = "Smart Receipt & Tax Splitter"
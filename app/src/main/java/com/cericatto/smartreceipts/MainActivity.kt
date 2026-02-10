package com.cericatto.smartreceipts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.cericatto.smartreceipts.ui.navigation.NavHostComposable
import com.cericatto.smartreceipts.ui.theme.ReceiptScannerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReceiptScannerTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    NavHostComposable(
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

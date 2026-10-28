package com.casasegura

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.casasegura.ui.AppShell
import com.casasegura.ui.theme.CasaSeguraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val credentialService = (application as CasaSeguraApp).credentialService
        setContent {
            CasaSeguraTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppShell(credentialService = credentialService)
                }
            }
        }
    }
}

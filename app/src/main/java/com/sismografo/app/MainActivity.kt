package com.sismografo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.sismografo.app.ui.AppSismografo
import com.sismografo.app.ui.tema.TemaSismografo

class MainActivity : ComponentActivity() {

    private val viewModel: SismografoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TemaSismografo {
                AppSismografo(viewModel)
            }
        }
    }
}

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.CountdownNavGraph
import com.example.ui.navigation.DetailDestination
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    val eventIdFromIntent = intent.getIntExtra("eventId", -1)
    
    setContent {
      MyApplicationTheme {
          val navController = rememberNavController()
          CountdownNavGraph(navController = navController)
          
          LaunchedEffect(eventIdFromIntent) {
              if (eventIdFromIntent != -1) {
                  navController.navigate(DetailDestination(eventIdFromIntent))
              }
          }
      }
    }
  }
}

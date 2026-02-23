package com.finoria

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.finoria.notifications.NotificationScheduler
import com.finoria.ui.navigation.AppNavigation
import com.finoria.ui.theme.FinoriaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialisation des rappels hebdomadaires
        NotificationScheduler.scheduleWeeklyReminder(this)

        setContent {
            FinoriaTheme {
                AppNavigation()
            }
        }
    }
}

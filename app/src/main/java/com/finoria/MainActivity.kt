package com.finoria

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.finoria.notifications.NotificationScheduler
import com.finoria.ui.navigation.AppNavigation
import com.finoria.ui.theme.FinoriaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialisation des rappels hebdomadaires (après le premier frame pour éviter tout blocage)
        window.decorView.post {
            NotificationScheduler.scheduleWeeklyReminder(this)
        }

        setContent {
            FinoriaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

/** Observer de cycle de vie pour notifier quand l'app revient au premier plan. Retourne une fonction pour retirer l'observer. */
fun LifecycleOwner.observeResumed(onResumed: () -> Unit): () -> Unit {
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            onResumed()
        }
    }
    lifecycle.addObserver(observer)
    return { lifecycle.removeObserver(observer) }
}

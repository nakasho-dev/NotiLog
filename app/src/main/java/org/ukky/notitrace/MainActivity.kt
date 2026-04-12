package org.ukky.notitrace

import android.content.ComponentName
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import org.ukky.notitrace.service.NotiTraceListenerService
import org.ukky.notitrace.ui.navigation.NotiTraceNavGraph
import org.ukky.notitrace.ui.navigation.Route
import org.ukky.notitrace.ui.theme.NotiTraceTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotiTraceTheme {
                val navController = rememberNavController()
                val startDestination = if (isNotificationListenerEnabled()) {
                    Route.Home.route
                } else {
                    Route.Onboarding.route
                }
                NotiTraceNavGraph(
                    navController = navController,
                    startDestination = startDestination,
                )
            }
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
        val cn = ComponentName(this, NotiTraceListenerService::class.java).flattenToString()
        return flat.contains(cn)
    }
}
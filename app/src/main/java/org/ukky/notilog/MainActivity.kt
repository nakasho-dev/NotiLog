package org.ukky.notilog

import android.content.ComponentName
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import org.ukky.notilog.service.NotiLogListenerService
import org.ukky.notilog.ui.navigation.NotiLogNavGraph
import org.ukky.notilog.ui.navigation.Route
import org.ukky.notilog.ui.theme.NotiLogTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotiLogTheme {
                val navController = rememberNavController()
                val startDestination = if (isNotificationListenerEnabled()) {
                    Route.Home.route
                } else {
                    Route.Onboarding.route
                }
                NotiLogNavGraph(
                    navController = navController,
                    startDestination = startDestination,
                )
            }
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
        val cn = ComponentName(this, NotiLogListenerService::class.java).flattenToString()
        return flat.contains(cn)
    }
}
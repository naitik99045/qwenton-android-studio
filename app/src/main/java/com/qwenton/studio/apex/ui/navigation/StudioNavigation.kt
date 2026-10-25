package com.qwenton.studio.apex.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Studio", Icons.Default.Dashboard)
    object Hub : Screen("hub", "Model Hub", Icons.Default.CloudDownload)
    object Playground : Screen("playground", "Playground", Icons.Default.Chat)

    companion object {
        val items = listOf(Dashboard, Hub, Playground)
    }
}

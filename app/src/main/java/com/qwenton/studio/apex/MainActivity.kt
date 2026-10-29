package com.qwenton.studio.apex

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.qwenton.studio.apex.ui.components.AboutSystemDialog
import com.qwenton.studio.apex.ui.components.CollapsibleTerminalDrawer
import com.qwenton.studio.apex.ui.components.StudioTopAppBar
import com.qwenton.studio.apex.ui.dashboard.StudioDashboardScreen
import com.qwenton.studio.apex.ui.hub.HuggingFaceHubScreen
import com.qwenton.studio.apex.ui.navigation.Screen
import com.qwenton.studio.apex.ui.playground.PlaygroundScreen
import com.qwenton.studio.apex.ui.theme.CyanAccent
import com.qwenton.studio.apex.ui.theme.CyanBorder
import com.qwenton.studio.apex.ui.theme.DarkCyanSurfaceSolid
import com.qwenton.studio.apex.ui.theme.MutedSlate
import com.qwenton.studio.apex.ui.theme.ObsidianSlate
import com.qwenton.studio.apex.ui.theme.QwentonStudioTheme
import com.qwenton.studio.apex.ui.theme.SoftPink
import com.qwenton.studio.apex.ui.theme.WarmCream
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            requestNotificationPermissionSafely()
        } catch (t: Throwable) {
            android.util.Log.w("MainActivity", "Safe permission handling", t)
        }

        val app = (application as? QwentonApplication) ?: run {
            android.util.Log.w("MainActivity", "Fallback application context casting")
            applicationContext as QwentonApplication
        }

        setContent {
            var hasRenderError by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf("") }

            QwentonStudioTheme {
                if (hasRenderError) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ObsidianSlate)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Qwenton Studio Apex Recovery",
                                color = SoftPink,
                                fontSize = 18.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage.ifEmpty { "A render issue occurred. Tap below to reload." },
                                color = WarmCream,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.Button(
                                onClick = { hasRenderError = false },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = CyanAccent)
                            ) {
                                Text("Reload Studio", color = ObsidianSlate, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    MainStudioShell(app = app)
                }
            }
        }
    }

    private fun requestNotificationPermissionSafely() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } catch (t: Throwable) {
                android.util.Log.w("MainActivity", "Notification permission launcher warning", t)
            }
        }
    }
}

@Composable
fun MainStudioShell(app: QwentonApplication) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    val serverStatus by app.terminalBridge.serverStatus.collectAsState()
    var showAboutDialog by remember { mutableStateOf(false) }
    var isTerminalDrawerExpanded by remember { mutableStateOf(false) }

    if (showAboutDialog) {
        AboutSystemDialog(onDismissRequest = { showAboutDialog = false })
    }

    Scaffold(
        topBar = {
            StudioTopAppBar(
                serverStatus = serverStatus,
                onInfoClick = { showAboutDialog = true },
                onTerminalToggle = { isTerminalDrawerExpanded = !isTerminalDrawerExpanded }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkCyanSurfaceSolid,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .border(1.dp, CyanBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                Screen.items.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                tint = if (selected) SoftPink else MutedSlate
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                color = if (selected) WarmCream else MutedSlate,
                                fontSize = 11.sp,
                                fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianSlate)
        ) {
            // Screen Navigation Host
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Dashboard.route) {
                    StudioDashboardScreen(
                        bridge = app.terminalBridge,
                        repository = app.modelCatalogRepository,
                        apiKeyManager = app.apiKeyManager
                    )
                }
                composable(Screen.Hub.route) {
                    HuggingFaceHubScreen(
                        bridge = app.terminalBridge,
                        repository = app.modelCatalogRepository
                    )
                }
                composable(Screen.Playground.route) {
                    PlaygroundScreen(
                        bridge = app.terminalBridge,
                        apiKeyManager = app.apiKeyManager
                    )
                }
            }

            // Persistent Collapsible Terminal Bottom Sheet Drawer
            CollapsibleTerminalDrawer(
                bridge = app.terminalBridge,
                isExpanded = isTerminalDrawerExpanded,
                onToggleExpand = { isTerminalDrawerExpanded = !isTerminalDrawerExpanded },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

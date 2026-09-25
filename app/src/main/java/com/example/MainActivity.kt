package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.NetworkMonitorService
import com.example.ui.MainViewModel
import com.example.ui.components.LivePulseDot
import com.example.ui.screens.AlertRulesScreen
import com.example.ui.screens.AuditLogsScreen
import com.example.ui.screens.LiveMonitorScreen
import com.example.ui.screens.TrafficInterfacesScreen
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberCyanPrimary
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceCard
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start background service
        NetworkMonitorService.startService(this)

        setContent {
            MyApplicationTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

enum class NavTab(val title: String, val icon: ImageVector, val tag: String) {
    LIVE("Live", Icons.Default.Radar, "tab_live"),
    AUDIT("Audit Logs", Icons.Default.History, "tab_audit"),
    TRAFFIC("Traffic", Icons.Default.DataUsage, "tab_traffic"),
    RULES("Rules", Icons.Default.Tune, "tab_rules")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val isMonitoring by viewModel.isMonitoring.collectAsStateWithLifecycle()

    // Request notification permission on Android 13+ (API 33+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberCyanPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "NetSentry Shield",
                                tint = CyberCyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "NetSentry",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                actions = {
                    // Live status pill in TopBar
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CyberSurfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LivePulseDot(isActive = isMonitoring, color = CyberCyanPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isMonitoring) "SHIELD ACTIVE" else "PAUSED",
                                color = if (isMonitoring) SafeGreen else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberBackground,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CyberSurface,
                tonalElevation = 8.dp
            ) {
                NavTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberBackground,
                            selectedTextColor = CyberCyanPrimary,
                            indicatorColor = CyberCyanPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(
                targetState = selectedTabIndex,
                label = "tab_crossfade"
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> LiveMonitorScreen(viewModel = viewModel)
                    1 -> AuditLogsScreen(viewModel = viewModel)
                    2 -> TrafficInterfacesScreen(viewModel = viewModel)
                    3 -> AlertRulesScreen(viewModel = viewModel)
                }
            }
        }
    }
}

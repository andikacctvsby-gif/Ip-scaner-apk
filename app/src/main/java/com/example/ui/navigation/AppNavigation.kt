package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import com.example.ui.screens.CustomScanScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.PingScreen
import com.example.ui.screens.PortScannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.viewmodel.HistoryViewModel
import com.example.viewmodel.PingViewModel
import com.example.viewmodel.PortScannerViewModel
import com.example.viewmodel.ScannerViewModel
import com.example.viewmodel.SettingsViewModel

enum class NavigationTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Scanner", Icons.Default.Lan),
    CUSTOM("Kustom", Icons.Default.Tune),
    PING("Ping", Icons.Default.Speed),
    PORT_SCAN("Port", Icons.Default.Security),
    HISTORY("Riwayat", Icons.Default.History),
    SETTINGS("Setelan", Icons.Default.Settings)
}

@Composable
fun MainAppNavigation(
    scannerViewModel: ScannerViewModel,
    pingViewModel: PingViewModel,
    portScannerViewModel: PortScannerViewModel,
    historyViewModel: HistoryViewModel,
    settingsViewModel: SettingsViewModel,
    onRequestLocationPermission: () -> Unit
) {
    var currentTab by remember { mutableStateOf(NavigationTab.DASHBOARD) }
    var pingInitialIp by remember { mutableStateOf("") }
    var portScanInitialIp by remember { mutableStateOf("") }

    // If on a secondary tab and system back is pressed, return to Dashboard
    if (currentTab != NavigationTab.DASHBOARD) {
        BackHandler {
            currentTab = NavigationTab.DASHBOARD
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                NavigationTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentTab) {
            NavigationTab.DASHBOARD -> {
                DashboardScreen(
                    viewModel = scannerViewModel,
                    onRequestLocationPermission = onRequestLocationPermission,
                    onNavigateToPing = { ip ->
                        pingInitialIp = ip
                        currentTab = NavigationTab.PING
                    },
                    onNavigateToPortScan = { ip ->
                        portScanInitialIp = ip
                        currentTab = NavigationTab.PORT_SCAN
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavigationTab.CUSTOM -> {
                CustomScanScreen(
                    viewModel = scannerViewModel,
                    onNavigateToPing = { ip ->
                        pingInitialIp = ip
                        currentTab = NavigationTab.PING
                    },
                    onNavigateToPortScan = { ip ->
                        portScanInitialIp = ip
                        currentTab = NavigationTab.PORT_SCAN
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavigationTab.PING -> {
                PingScreen(
                    viewModel = pingViewModel,
                    initialIp = pingInitialIp,
                    onNavigateBack = { currentTab = NavigationTab.DASHBOARD },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavigationTab.PORT_SCAN -> {
                PortScannerScreen(
                    viewModel = portScannerViewModel,
                    initialIp = portScanInitialIp,
                    onNavigateBack = { currentTab = NavigationTab.DASHBOARD },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavigationTab.HISTORY -> {
                HistoryScreen(
                    viewModel = historyViewModel,
                    onNavigateToPing = { ip ->
                        pingInitialIp = ip
                        currentTab = NavigationTab.PING
                    },
                    onNavigateToPortScan = { ip ->
                        portScanInitialIp = ip
                        currentTab = NavigationTab.PORT_SCAN
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavigationTab.SETTINGS -> {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

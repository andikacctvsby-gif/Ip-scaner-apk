package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.network.NetworkManager
import com.example.network.scanner.IpScannerEngine
import com.example.network.scanner.PingEngine
import com.example.network.scanner.PortScannerEngine
import com.example.repository.ScanRepository
import com.example.repository.SettingsRepository
import com.example.ui.navigation.MainAppNavigation
import com.example.ui.theme.IPScannerTheme
import com.example.viewmodel.HistoryViewModel
import com.example.viewmodel.PingViewModel
import com.example.viewmodel.PortScannerViewModel
import com.example.viewmodel.ScannerViewModel
import com.example.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val scanRepository = ScanRepository(database.scanHistoryDao())
        val settingsRepository = SettingsRepository(applicationContext)
        val networkManager = NetworkManager(applicationContext)
        val ipScannerEngine = IpScannerEngine()
        val pingEngine = PingEngine()
        val portScannerEngine = PortScannerEngine()

        setContent {
            val settings by settingsRepository.settings.collectAsState()

            // ViewModels created and remembered for activity lifecycle
            val scannerViewModel = remember {
                ScannerViewModel(
                    networkManager = networkManager,
                    scannerEngine = ipScannerEngine,
                    scanRepository = scanRepository,
                    settingsRepository = settingsRepository
                )
            }

            val pingViewModel = remember {
                PingViewModel(pingEngine = pingEngine)
            }

            val portScannerViewModel = remember {
                PortScannerViewModel(
                    portScannerEngine = portScannerEngine,
                    settingsRepository = settingsRepository
                )
            }

            val historyViewModel = remember {
                HistoryViewModel(repository = scanRepository)
            }

            val settingsViewModel = remember {
                SettingsViewModel(
                    settingsRepository = settingsRepository,
                    scanRepository = scanRepository
                )
            }

            // Location permission launcher for Wi-Fi SSID
            val locationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    scannerViewModel.refreshNetworkInfo()
                }
            }

            IPScannerTheme(themeMode = settings.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainAppNavigation(
                        scannerViewModel = scannerViewModel,
                        pingViewModel = pingViewModel,
                        portScannerViewModel = portScannerViewModel,
                        historyViewModel = historyViewModel,
                        settingsViewModel = settingsViewModel,
                        onRequestLocationPermission = {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    )
                }
            }
        }
    }
}

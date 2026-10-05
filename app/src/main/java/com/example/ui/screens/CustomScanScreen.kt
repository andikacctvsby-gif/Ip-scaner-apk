package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeviceModel
import com.example.ui.components.DeviceCard
import com.example.ui.components.DeviceDetailSheet
import com.example.ui.theme.OfflineRed
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.PrimaryCyan
import com.example.utils.IpUtils
import com.example.viewmodel.ScannerViewModel

@Composable
fun CustomScanScreen(
    viewModel: ScannerViewModel,
    onNavigateToPing: (String) -> Unit,
    onNavigateToPortScan: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val rawDevices by viewModel.rawScannedDevices.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Custom Range, 1: CIDR

    // Range fields initialized from current network if available
    var startIp by remember(uiState.networkInfo.rangeStart) {
        mutableStateOf(if (uiState.networkInfo.rangeStart != "0.0.0.0") uiState.networkInfo.rangeStart else "192.168.1.1")
    }
    var endIp by remember(uiState.networkInfo.rangeEnd) {
        mutableStateOf(if (uiState.networkInfo.rangeEnd != "0.0.0.0") uiState.networkInfo.rangeEnd else "192.168.1.254")
    }

    // CIDR field
    var cidrInput by remember(uiState.networkInfo.cidr) {
        mutableStateOf(if (uiState.networkInfo.cidr != "0.0.0.0/24") uiState.networkInfo.cidr else "192.168.1.0/24")
    }

    var selectedDeviceForDetail by remember { mutableStateOf<DeviceModel?>(null) }

    // Host count derivations
    val rangeHostCount by remember(startIp, endIp) {
        derivedStateOf {
            if (IpUtils.isValidIpv4(startIp) && IpUtils.isValidIpv4(endIp)) {
                val start = IpUtils.ipToLong(startIp)
                val end = IpUtils.ipToLong(endIp)
                if (start <= end) ((end - start) + 1).toInt() else -1
            } else -1
        }
    }

    val cidrHostCount by remember(cidrInput) {
        derivedStateOf {
            val range = IpUtils.calculateCidrRange(cidrInput)
            range?.third ?: -1
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "CUSTOM SCAN",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Pindai rentang IP kustom atau subnet CIDR spesifik",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        // Tab Selector: Custom Range vs CIDR
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Rentang IP (Range)", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_custom_range")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("CIDR Scanner", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_cidr_scan")
                )
            }
        }

        // Input Form Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (selectedTab == 0) {
                        // Custom IP Range
                        OutlinedTextField(
                            value = startIp,
                            onValueChange = { startIp = it.trim() },
                            label = { Text("IP Start") },
                            placeholder = { Text("192.168.1.1") },
                            isError = startIp.isNotBlank() && !IpUtils.isValidIpv4(startIp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("custom_ip_start_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = endIp,
                            onValueChange = { endIp = it.trim() },
                            label = { Text("IP End") },
                            placeholder = { Text("192.168.1.254") },
                            isError = endIp.isNotBlank() && !IpUtils.isValidIpv4(endIp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("custom_ip_end_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Host count preview badge
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = if (rangeHostCount > 0)
                                    "Total host yang akan dipindai: $rangeHostCount host"
                                else
                                    "Masukkan format IP valid (IP Start <= IP End)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (rangeHostCount > 0) MaterialTheme.colorScheme.primary else OfflineRed,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Scan Button
                        if (!uiState.isScanning) {
                            Button(
                                onClick = { viewModel.startCustomRangeScan(startIp, endIp) },
                                enabled = rangeHostCount > 0,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("custom_range_scan_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SCAN RENTANG IP", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.stopScan() },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OfflineRed)
                            ) {
                                Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("HENTIKAN SCAN", fontWeight = FontWeight.Bold)
                            }
                        }

                    } else {
                        // CIDR Scanner
                        OutlinedTextField(
                            value = cidrInput,
                            onValueChange = { cidrInput = it.trim() },
                            label = { Text("Notasi CIDR") },
                            placeholder = { Text("192.168.1.0/24") },
                            isError = cidrInput.isNotBlank() && !IpUtils.isValidCidr(cidrInput),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("cidr_input_field"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Quick suggestion chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("192.168.1.0/24", "10.0.0.0/24", "172.16.0.0/24").forEach { preset ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .clickable { cidrInput = preset }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = preset,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Host preview
                        val cidrRange = IpUtils.calculateCidrRange(cidrInput)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = if (cidrHostCount > 0 && cidrRange != null)
                                        "Rentang Host: ${cidrRange.first} - ${cidrRange.second}\nJumlah Host: $cidrHostCount host"
                                    else
                                        "Masukkan format CIDR yang valid (contoh: 192.168.1.0/24)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (cidrHostCount > 0) MaterialTheme.colorScheme.primary else OfflineRed,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Scan Button
                        if (!uiState.isScanning) {
                            Button(
                                onClick = { viewModel.startCidrScan(cidrInput) },
                                enabled = cidrHostCount > 0,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("cidr_scan_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SCAN CIDR", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.stopScan() },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OfflineRed)
                            ) {
                                Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("HENTIKAN SCAN", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Live Scanning Progress
        if (uiState.isScanning || uiState.progressTotal > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        val fraction = if (uiState.progressTotal > 0)
                            uiState.progressCurrent.toFloat() / uiState.progressTotal.toFloat()
                        else 0f

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Scanning ${uiState.progressCurrent} / ${uiState.progressTotal}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${(fraction * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                    }
                }
            }
        }

        // Error message if any
        if (uiState.errorMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = OfflineRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = OfflineRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = uiState.errorMessage ?: "", color = OfflineRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.clearErrorMessage() }, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = OfflineRed)
                        }
                    }
                }
            }
        }

        // Discovered Devices
        if (rawDevices.isNotEmpty()) {
            item {
                Text(
                    text = "Perangkat Ditemukan (${rawDevices.count { it.isOnline }} Online / ${rawDevices.size} Total):",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(rawDevices, key = { it.ip }) { device ->
                DeviceCard(
                    device = device,
                    onClick = { selectedDeviceForDetail = device },
                    onPingClick = { onNavigateToPing(device.ip) },
                    onPortScanClick = { onNavigateToPortScan(device.ip) }
                )
            }
        }
    }

    selectedDeviceForDetail?.let { device ->
        DeviceDetailSheet(
            device = device,
            onDismiss = { selectedDeviceForDetail = null },
            onPingClick = onNavigateToPing,
            onPortScanClick = onNavigateToPortScan
        )
    }
}

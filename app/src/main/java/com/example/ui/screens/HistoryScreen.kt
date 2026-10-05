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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ScanHistoryEntity
import com.example.model.DeviceModel
import com.example.model.NetworkInfoModel
import com.example.ui.components.DeviceCard
import com.example.ui.components.DeviceDetailSheet
import com.example.ui.components.ExportDialog
import com.example.ui.theme.OfflineRed
import com.example.ui.theme.OnlineGreen
import com.example.utils.IpUtils
import com.example.viewmodel.HistoryViewModel

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateToPing: (String) -> Unit,
    onNavigateToPortScan: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val historyList by viewModel.historyList.collectAsState()
    val selectedHistory by viewModel.selectedHistory.collectAsState()
    val devicesForSelected by viewModel.devicesForSelected.collectAsState()

    var showClearAllDialog by remember { mutableStateOf(false) }
    var historyToDelete by remember { mutableStateOf<Long?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var selectedDeviceForDetail by remember { mutableStateOf<DeviceModel?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RIWAYAT SCAN",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Daftar sesi pemindaian yang tersimpan secara lokal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }

                if (historyList.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearAllDialog = true },
                        modifier = Modifier.testTag("clear_all_history_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Hapus Semua Riwayat",
                            tint = OfflineRed
                        )
                    }
                }
            }
        }

        if (historyList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum Ada Riwayat Pemindaian",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Hasil pemindaian otomatis akan disimpan di sini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        } else {
            items(historyList, key = { it.id }) { item ->
                HistoryItemCard(
                    history = item,
                    isSelected = selectedHistory?.id == item.id,
                    devices = if (selectedHistory?.id == item.id) devicesForSelected else emptyList(),
                    onClick = {
                        if (selectedHistory?.id == item.id) {
                            viewModel.clearSelected()
                        } else {
                            viewModel.selectHistory(item)
                        }
                    },
                    onDelete = { historyToDelete = item.id },
                    onExport = {
                        viewModel.selectHistory(item)
                        showExportDialog = true
                    },
                    onDeviceClick = { selectedDeviceForDetail = it },
                    onPingClick = onNavigateToPing,
                    onPortScanClick = onNavigateToPortScan
                )
            }
        }
    }

    // Confirmation dialog for clearing all history
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Hapus Semua Riwayat?") },
            text = { Text("Semua data riwayat pemindaian jaringan akan dihapus permanen dari perangkat.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearAllDialog = false
                    }
                ) {
                    Text("Hapus Semua", color = OfflineRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Confirmation dialog for deleting single history item
    historyToDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { historyToDelete = null },
            title = { Text("Hapus Sesi Riwayat?") },
            text = { Text("Sesi riwayat pemindaian ini akan dihapus dari penyimpanan.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteHistory(id)
                        historyToDelete = null
                    }
                ) {
                    Text("Hapus", color = OfflineRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { historyToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Export dialog for selected history
    if (showExportDialog && selectedHistory != null) {
        val h = selectedHistory!!
        ExportDialog(
            networkInfo = NetworkInfoModel(
                ssid = h.networkName,
                cidr = h.subnetCidr
            ),
            devices = devicesForSelected,
            durationSeconds = h.durationSeconds,
            onDismiss = { showExportDialog = false }
        )
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

@Composable
private fun HistoryItemCard(
    history: ScanHistoryEntity,
    isSelected: Boolean,
    devices: List<DeviceModel>,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onDeviceClick: (DeviceModel) -> Unit,
    onPingClick: (String) -> Unit,
    onPortScanClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_${history.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Date & Time, Delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = IpUtils.formatDate(history.timestamp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Pukul ${IpUtils.formatTime(history.timestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onExport, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Export Sesi", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = OfflineRed, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Details: Network Name, Subnet, Devices, Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = history.networkName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Subnet: ${history.subnetCidr}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${history.onlineCount} Online / ${history.deviceCount} Total",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnlineGreen
                    )
                    Text(
                        text = "Durasi: ${IpUtils.formatDuration(history.durationSeconds)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Toggle expansion to show devices
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onClick() }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSelected) "Sembunyikan Hasil (${devices.size})" else "Buka Hasil Lama (${history.deviceCount} perangkat)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (isSelected) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Expanded device list for this session
            AnimatedVisibility(visible = isSelected) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(4.dp))

                    devices.forEach { device ->
                        DeviceCard(
                            device = device,
                            onClick = { onDeviceClick(device) },
                            onPingClick = { onPingClick(device.ip) },
                            onPortScanClick = { onPortScanClick(device.ip) }
                        )
                    }
                }
            }
        }
    }
}

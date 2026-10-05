package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.DeviceModel
import com.example.model.NetworkInfoModel
import com.example.utils.ClipboardUtils
import com.example.utils.ExportFormat
import com.example.utils.Exporter

@Composable
fun ExportDialog(
    networkInfo: NetworkInfoModel,
    devices: List<DeviceModel>,
    durationSeconds: Long,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Export Hasil Scan", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Pilih format file untuk dibagikan atau disalin (${devices.size} perangkat):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                ExportFormat.values().forEach { format ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedFormat = format },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedFormat == format)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedFormat == format,
                                onClick = { selectedFormat = format }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = when (format) {
                                    ExportFormat.CSV -> Icons.Default.TableChart
                                    ExportFormat.TXT -> Icons.Default.Description
                                    ExportFormat.JSON -> Icons.Default.Code
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = format.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedFormat == format) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val content = when (selectedFormat) {
                        ExportFormat.CSV -> Exporter.generateCsv(devices)
                        ExportFormat.TXT -> Exporter.generateTxt(networkInfo, devices, durationSeconds)
                        ExportFormat.JSON -> Exporter.generateJson(networkInfo, devices, durationSeconds)
                    }
                    Exporter.shareExport(context, content, selectedFormat, "Hasil Scan ${networkInfo.ssid}")
                    onDismiss()
                },
                modifier = Modifier.testTag("share_export_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export & Bagikan")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    val content = when (selectedFormat) {
                        ExportFormat.CSV -> Exporter.generateCsv(devices)
                        ExportFormat.TXT -> Exporter.generateTxt(networkInfo, devices, durationSeconds)
                        ExportFormat.JSON -> Exporter.generateJson(networkInfo, devices, durationSeconds)
                    }
                    ClipboardUtils.copyToClipboard(context, "Hasil Scan (${selectedFormat.name})", content)
                    onDismiss()
                },
                modifier = Modifier.testTag("copy_export_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Salin Teks")
            }
        }
    )
}

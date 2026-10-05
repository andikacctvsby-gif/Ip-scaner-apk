package com.example.network.scanner

import com.example.model.DeviceModel
import com.example.network.OuiLookup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.Locale

sealed class ScanProgressEvent {
    data class ProgressUpdate(val current: Int, val total: Int, val currentIp: String) : ScanProgressEvent()
    data class DeviceFound(val device: DeviceModel) : ScanProgressEvent()
    data class Completed(val totalScanned: Int, val onlineCount: Int) : ScanProgressEvent()
    data class Cancelled(val current: Int) : ScanProgressEvent()
    data class Error(val message: String) : ScanProgressEvent()
}

class IpScannerEngine {

    companion object {
        private val COMMON_PROBE_PORTS = intArrayOf(80, 443, 5357, 445, 139, 8080, 22)
    }

    fun scanIps(
        ipList: List<String>,
        concurrencyLimit: Int = 20,
        timeoutMs: Int = 350
    ): Flow<ScanProgressEvent> = channelFlow {
        if (ipList.isEmpty()) {
            send(ScanProgressEvent.Error("Daftar IP kosong atau format tidak valid."))
            return@channelFlow
        }

        val total = ipList.size
        var currentScanned = 0
        var onlineCount = 0
        val semaphore = Semaphore(concurrencyLimit.coerceIn(5, 50))

        // Preload ARP table once if available
        val arpMap = withContext(Dispatchers.IO) { readArpTable() }

        for (ip in ipList) {
            if (!coroutineContext.isActive) {
                send(ScanProgressEvent.Cancelled(currentScanned))
                return@channelFlow
            }

            semaphore.withPermit {
                if (!coroutineContext.isActive) {
                    send(ScanProgressEvent.Cancelled(currentScanned))
                    return@channelFlow
                }

                currentScanned++
                send(ScanProgressEvent.ProgressUpdate(currentScanned, total, ip))

                val device = withContext(Dispatchers.IO) {
                    checkHost(ip, timeoutMs, arpMap)
                }

                if (device != null) {
                    if (device.isOnline) {
                        onlineCount++
                    }
                    send(ScanProgressEvent.DeviceFound(device))
                }
            }
        }

        send(ScanProgressEvent.Completed(total, onlineCount))
        awaitClose { }
    }

    private fun checkHost(ip: String, timeoutMs: Int, arpMap: Map<String, String>): DeviceModel? {
        var isOnline = false
        var responseTime = 0L

        try {
            val inetAddress = InetAddress.getByName(ip)
            val startTime = System.currentTimeMillis()

            // Step 1: Check ICMP reachability
            if (inetAddress.isReachable(timeoutMs)) {
                isOnline = true
                responseTime = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
            } else {
                // Step 2: TCP probe to common ports
                for (port in COMMON_PROBE_PORTS) {
                    val portStartTime = System.currentTimeMillis()
                    val socket = Socket()
                    try {
                        socket.connect(InetSocketAddress(ip, port), timeoutMs)
                        isOnline = true
                        responseTime = (System.currentTimeMillis() - portStartTime).coerceAtLeast(1L)
                        socket.close()
                        break
                    } catch (ste: SocketTimeoutException) {
                        // Port timed out, try next
                        try { socket.close() } catch (_: Exception) {}
                    } catch (e: Exception) {
                        // Connection refused or reset indicates host is alive!
                        val msg = e.message?.lowercase(Locale.ROOT) ?: ""
                        if (msg.contains("refused") || msg.contains("reset") || msg.contains("route")) {
                            isOnline = true
                            responseTime = (System.currentTimeMillis() - portStartTime).coerceAtLeast(1L)
                            try { socket.close() } catch (_: Exception) {}
                            break
                        }
                        try { socket.close() } catch (_: Exception) {}
                    }
                }
            }

            // Step 3: Hostname discovery
            var hostname = "Unknown Host"
            if (isOnline) {
                try {
                    val host = inetAddress.canonicalHostName
                    if (!host.isNullOrBlank() && host != ip) {
                        hostname = host
                    } else {
                        val simpleHost = inetAddress.hostName
                        if (!simpleHost.isNullOrBlank() && simpleHost != ip) {
                            hostname = simpleHost
                        }
                    }
                } catch (_: Exception) {}
            }

            // Step 4: MAC & Vendor lookup
            val mac = arpMap[ip] ?: "Tidak tersedia pada perangkat/versi Android ini"
            val vendor = OuiLookup.lookup(if (mac.contains(":")) mac else null, hostname)

            val now = System.currentTimeMillis()
            return DeviceModel(
                ip = ip,
                mac = mac,
                hostname = hostname,
                vendor = vendor,
                responseTimeMs = responseTime,
                isOnline = isOnline,
                firstSeen = now,
                lastSeen = now
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun readArpTable(): Map<String, String> {
        val arpMap = mutableMapOf<String, String>()
        try {
            val reader = BufferedReader(FileReader("/proc/net/arp"))
            reader.useLines { lines ->
                lines.drop(1).forEach { line ->
                    val tokens = line.split("\\s+".toRegex()).filter { it.isNotBlank() }
                    if (tokens.size >= 4) {
                        val ip = tokens[0]
                        val mac = tokens[3]
                        if (mac != "00:00:00:00:00:00" && mac.length == 17) {
                            arpMap[ip] = mac.uppercase(Locale.US)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Android 10+ restricts /proc/net/arp for privacy
        }
        return arpMap
    }
}

package com.example.network.scanner

import com.example.model.PortResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

sealed class PortScanEvent {
    data class Progress(val current: Int, val total: Int, val currentPort: Int) : PortScanEvent()
    data class PortFound(val result: PortResult) : PortScanEvent()
    data class Completed(val totalScanned: Int, val openCount: Int) : PortScanEvent()
    data class Error(val message: String) : PortScanEvent()
}

class PortScannerEngine {

    companion object {
        val COMMON_PORTS = listOf(
            21 to "FTP",
            22 to "SSH",
            23 to "Telnet",
            25 to "SMTP",
            53 to "DNS",
            80 to "HTTP",
            110 to "POP3",
            135 to "RPC",
            139 to "NetBIOS",
            143 to "IMAP",
            443 to "HTTPS",
            445 to "SMB",
            3306 to "MySQL",
            3389 to "RDP",
            5432 to "PostgreSQL",
            8080 to "HTTP-Alt"
        )

        private val SERVICE_MAP = (COMMON_PORTS + listOf(
            20 to "FTP-Data",
            69 to "TFTP",
            88 to "Kerberos",
            123 to "NTP",
            161 to "SNMP",
            162 to "SNMP-Trap",
            389 to "LDAP",
            636 to "LDAPS",
            993 to "IMAPS",
            995 to "POP3S",
            1433 to "MS-SQL",
            1521 to "Oracle",
            2049 to "NFS",
            5060 to "SIP",
            5900 to "VNC",
            6379 to "Redis",
            8443 to "HTTPS-Alt",
            9090 to "Web-Admin",
            27017 to "MongoDB"
        )).toMap()

        fun getServiceName(port: Int): String {
            return SERVICE_MAP[port] ?: "Port-$port"
        }

        fun parseCustomPorts(input: String): List<Int> {
            val ports = mutableSetOf<Int>()
            val tokens = input.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            for (token in tokens) {
                if (token.contains("-")) {
                    val rangeParts = token.split("-").map { it.trim() }
                    if (rangeParts.size == 2) {
                        val start = rangeParts[0].toIntOrNull()
                        val end = rangeParts[1].toIntOrNull()
                        if (start != null && end != null && start in 1..65535 && end in 1..65535) {
                            val rangeStart = minOf(start, end)
                            val rangeEnd = minOf(maxOf(start, end), rangeStart + 200) // cap range to prevent overload
                            for (p in rangeStart..rangeEnd) {
                                ports.add(p)
                            }
                        }
                    }
                } else {
                    val p = token.toIntOrNull()
                    if (p != null && p in 1..65535) {
                        ports.add(p)
                    }
                }
            }
            return ports.toList().sorted()
        }
    }

    fun scanPorts(
        targetIp: String,
        portsToScan: List<Int>,
        timeoutMs: Int = 400,
        concurrencyLimit: Int = 10
    ): Flow<PortScanEvent> = channelFlow {
        if (portsToScan.isEmpty()) {
            send(PortScanEvent.Error("Tidak ada port yang ditentukan untuk dipindai."))
            return@channelFlow
        }

        val total = portsToScan.size
        var current = 0
        var openCount = 0
        val semaphore = Semaphore(concurrencyLimit.coerceIn(2, 25))

        for (port in portsToScan) {
            if (!coroutineContext.isActive) break

            semaphore.withPermit {
                if (!coroutineContext.isActive) return@withPermit
                current++
                send(PortScanEvent.Progress(current, total, port))

                val result = withContext(Dispatchers.IO) {
                    checkPort(targetIp, port, timeoutMs)
                }

                if (result.isOpen) {
                    openCount++
                }
                send(PortScanEvent.PortFound(result))
            }
        }

        send(PortScanEvent.Completed(total, openCount))
        awaitClose { }
    }

    private fun checkPort(ip: String, port: Int, timeoutMs: Int): PortResult {
        val socket = Socket()
        val startTime = System.currentTimeMillis()
        var isOpen = false
        var responseTime = 0L

        try {
            socket.connect(InetSocketAddress(ip, port), timeoutMs)
            isOpen = true
            responseTime = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
            socket.close()
        } catch (_: SocketTimeoutException) {
            isOpen = false
            try { socket.close() } catch (_: Exception) {}
        } catch (_: Exception) {
            isOpen = false
            try { socket.close() } catch (_: Exception) {}
        }

        return PortResult(
            port = port,
            isOpen = isOpen,
            serviceName = getServiceName(port),
            responseTimeMs = responseTime
        )
    }
}

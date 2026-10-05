package com.example.network.scanner

import com.example.model.PingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Locale

class PingEngine {

    fun executePing(targetIp: String, packetCount: Int = 4, timeoutMs: Int = 1000): Flow<PingResult> = flow {
        var currentResult = PingResult(
            targetIp = targetIp,
            packetsTransmitted = 0,
            packetsReceived = 0,
            packetsLost = 0,
            minLatencyMs = 0L,
            maxLatencyMs = 0L,
            avgLatencyMs = 0L,
            logs = listOf("Memulai PING ke $targetIp dengan $packetCount paket..."),
            isPinging = true
        )
        emit(currentResult)

        val latencies = mutableListOf<Long>()
        val logs = currentResult.logs.toMutableList()

        for (seq in 1..packetCount) {
            val startTime = System.currentTimeMillis()
            var success = false
            var latency = 0L

            withContext(Dispatchers.IO) {
                try {
                    val addr = InetAddress.getByName(targetIp)
                    if (addr.isReachable(timeoutMs)) {
                        latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
                        success = true
                    } else {
                        // Fallback socket ping (port 80 or 443 or 53)
                        val socket = Socket()
                        val socketStart = System.currentTimeMillis()
                        try {
                            socket.connect(InetSocketAddress(targetIp, 80), timeoutMs)
                            latency = (System.currentTimeMillis() - socketStart).coerceAtLeast(1L)
                            success = true
                            socket.close()
                        } catch (e: Exception) {
                            val msg = e.message?.lowercase(Locale.ROOT) ?: ""
                            if (msg.contains("refused") || msg.contains("reset")) {
                                latency = (System.currentTimeMillis() - socketStart).coerceAtLeast(1L)
                                success = true
                            }
                            try { socket.close() } catch (_: Exception) {}
                        }
                    }
                } catch (e: Exception) {
                    success = false
                }
            }

            if (success) {
                latencies.add(latency)
                logs.add("Reply dari $targetIp: bytes=32 waktu=${latency}ms seq=$seq")
            } else {
                logs.add("Request timed out untuk seq=$seq")
            }

            val received = latencies.size
            val lost = seq - received
            val min = if (latencies.isNotEmpty()) latencies.minOrNull() ?: 0L else 0L
            val max = if (latencies.isNotEmpty()) latencies.maxOrNull() ?: 0L else 0L
            val avg = if (latencies.isNotEmpty()) latencies.average().toLong() else 0L

            currentResult = currentResult.copy(
                packetsTransmitted = seq,
                packetsReceived = received,
                packetsLost = lost,
                minLatencyMs = min,
                maxLatencyMs = max,
                avgLatencyMs = avg,
                logs = logs.toList(),
                isPinging = seq < packetCount
            )
            emit(currentResult)

            if (seq < packetCount) {
                delay(300)
            }
        }
    }
}

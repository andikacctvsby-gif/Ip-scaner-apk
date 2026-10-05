package com.example.utils

import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object IpUtils {

    private val IPV4_REGEX = Regex(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    )

    private val CIDR_REGEX = Regex(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)/(3[0-2]|[12]?[0-9])$"
    )

    fun isValidIpv4(ip: String): Boolean {
        val trimmed = ip.trim()
        if (!IPV4_REGEX.matches(trimmed)) return false
        val parts = trimmed.split(".")
        if (parts.size != 4) return false
        return parts.all {
            val num = it.toIntOrNull()
            num != null && num in 0..255
        }
    }

    fun isValidCidr(cidr: String): Boolean {
        val trimmed = cidr.trim()
        if (!CIDR_REGEX.matches(trimmed)) return false
        val parts = trimmed.split("/")
        if (parts.size != 2) return false
        val ip = parts[0]
        val prefix = parts[1].toIntOrNull() ?: return false
        return isValidIpv4(ip) && prefix in 16..30 // Safe range for local subnet scanning
    }

    fun ipToLong(ip: String): Long {
        val parts = ip.trim().split(".")
        if (parts.size != 4) return 0L
        var result = 0L
        for (i in 0..3) {
            val part = parts[i].toLongOrNull() ?: 0L
            result = (result shl 8) or (part and 0xFF)
        }
        return result
    }

    fun longToIp(ipLong: Long): String {
        return "${(ipLong shr 24) and 0xFF}.${(ipLong shr 16) and 0xFF}.${(ipLong shr 8) and 0xFF}.${ipLong and 0xFF}"
    }

    fun prefixLengthToSubnetMask(prefix: Int): String {
        val mask = if (prefix == 0) 0L else (-1L shl (32 - prefix)) and 0xFFFFFFFFL
        return longToIp(mask)
    }

    fun calculateSubnetRange(ip: String, prefixLength: Int): Triple<String, String, Int> {
        val validPrefix = prefixLength.coerceIn(16, 30)
        val ipLong = ipToLong(ip)
        val mask = if (validPrefix == 0) 0L else (-1L shl (32 - validPrefix)) and 0xFFFFFFFFL
        val networkLong = ipLong and mask
        val broadcastLong = networkLong or (mask.inv() and 0xFFFFFFFFL)

        // Usable hosts: network + 1 up to broadcast - 1
        val firstHostLong = networkLong + 1
        val lastHostLong = (broadcastLong - 1).coerceAtLeast(firstHostLong)
        val totalHosts = ((lastHostLong - firstHostLong) + 1).toInt().coerceAtLeast(1)

        return Triple(longToIp(firstHostLong), longToIp(lastHostLong), totalHosts)
    }

    fun calculateCidrRange(cidr: String): Triple<String, String, Int>? {
        if (!isValidCidr(cidr)) return null
        val parts = cidr.trim().split("/")
        val ip = parts[0]
        val prefix = parts[1].toIntOrNull() ?: 24
        return calculateSubnetRange(ip, prefix)
    }

    fun generateIpList(startIp: String, endIp: String, maxLimit: Int = 1024): List<String> {
        if (!isValidIpv4(startIp) || !isValidIpv4(endIp)) return emptyList()
        val startLong = ipToLong(startIp)
        val endLong = ipToLong(endIp)
        if (startLong > endLong) return emptyList()

        val count = (endLong - startLong + 1).coerceAtMost(maxLimit.toLong())
        val list = ArrayList<String>(count.toInt())
        for (i in 0 until count) {
            list.add(longToIp(startLong + i))
        }
        return list
    }

    fun generateCidrIpList(cidr: String, maxLimit: Int = 1024): List<String> {
        val range = calculateCidrRange(cidr) ?: return emptyList()
        return generateIpList(range.first, range.second, maxLimit)
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID"))
        return sdf.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDuration(seconds: Long): String {
        return if (seconds < 60) {
            "$seconds detik"
        } else {
            val mins = seconds / 60
            val remSecs = seconds % 60
            "$mins m $remSecs d"
        }
    }
}

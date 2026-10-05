package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import com.example.model.NetworkInfoModel
import com.example.utils.IpUtils
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

class NetworkManager(private val context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    fun getNetworkInfo(): NetworkInfoModel {
        var ipAddress = "0.0.0.0"
        var subnetMask = "255.255.255.0"
        var prefixLength = 24
        var interfaceName = "wlan0"
        var connectionType = "Tidak Ada Koneksi"
        var isConnected = false
        var ssid = "Tidak Terhubung"
        var gateway = "0.0.0.0"
        val dnsServers = mutableListOf<String>()

        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        val linkProps = activeNetwork?.let { connectivityManager.getLinkProperties(it) }

        if (capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            isConnected = true
            connectionType = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                else -> "Jaringan Lokal"
            }
        }

        // Get SSID
        if (connectionType == "Wi-Fi" && wifiManager != null) {
            try {
                val wifiInfo = wifiManager.connectionInfo
                val rawSsid = wifiInfo?.ssid?.replace("\"", "")
                ssid = if (rawSsid != null && rawSsid != "<unknown ssid>" && rawSsid.isNotBlank()) {
                    rawSsid
                } else {
                    "Wi-Fi Terhubung"
                }
            } catch (e: Exception) {
                ssid = "Wi-Fi Terhubung"
            }
        } else if (isConnected) {
            ssid = connectionType
        }

        // Get Gateway and DNS from LinkProperties
        if (linkProps != null) {
            try {
                // Gateway from routes
                for (route in linkProps.routes) {
                    if (route.isDefaultRoute && route.gateway != null) {
                        val gwStr = route.gateway?.hostAddress
                        if (gwStr != null && IpUtils.isValidIpv4(gwStr)) {
                            gateway = gwStr
                            break
                        }
                    }
                }

                // DNS servers
                for (dns in linkProps.dnsServers) {
                    val dnsStr = dns.hostAddress
                    if (dnsStr != null && IpUtils.isValidIpv4(dnsStr)) {
                        dnsServers.add(dnsStr)
                    }
                }

                if (linkProps.interfaceName != null) {
                    interfaceName = linkProps.interfaceName!!
                }
            } catch (e: Exception) {
                // fallback
            }
        }

        // Find primary IPv4 address and subnet prefix from NetworkInterface
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            var found = false

            // Prioritize matching interfaceName from linkProps, then wlan0/eth0, then any non-loopback
            val sortedInterfaces = interfaces.sortedWith(compareByDescending {
                if (it.name == interfaceName) 3
                else if (it.name.startsWith("wlan") || it.name.startsWith("eth")) 2
                else 1
            })

            for (networkInterface in sortedInterfaces) {
                if (networkInterface.isLoopback || !networkInterface.isUp) continue

                for (ia in networkInterface.interfaceAddresses) {
                    val addr = ia.address
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        ipAddress = addr.hostAddress ?: "0.0.0.0"
                        prefixLength = ia.networkPrefixLength.toInt()
                        if (prefixLength !in 16..30) {
                            prefixLength = 24
                        }
                        subnetMask = IpUtils.prefixLengthToSubnetMask(prefixLength)
                        interfaceName = networkInterface.name
                        found = true
                        break
                    }
                }
                if (found) break
            }
        } catch (e: Exception) {
            // fallback
        }

        // Fallback gateway if not discovered: assume x.x.x.1
        if (gateway == "0.0.0.0" && ipAddress != "0.0.0.0") {
            val parts = ipAddress.split(".")
            if (parts.size == 4) {
                gateway = "${parts[0]}.${parts[1]}.${parts[2]}.1"
            }
        }

        // Subnet CIDR and range calculation
        val (rangeStart, rangeEnd, totalHosts) = IpUtils.calculateSubnetRange(ipAddress, prefixLength)
        val networkIp = IpUtils.longToIp(
            IpUtils.ipToLong(ipAddress) and (if (prefixLength == 0) 0L else (-1L shl (32 - prefixLength)) and 0xFFFFFFFFL)
        )
        val cidr = "$networkIp/$prefixLength"

        return NetworkInfoModel(
            ssid = ssid,
            ipAddress = ipAddress,
            subnetMask = subnetMask,
            gateway = gateway,
            dnsServers = dnsServers,
            interfaceName = interfaceName,
            connectionType = connectionType,
            cidr = cidr,
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
            totalHosts = totalHosts,
            isConnected = isConnected && ipAddress != "0.0.0.0"
        )
    }
}

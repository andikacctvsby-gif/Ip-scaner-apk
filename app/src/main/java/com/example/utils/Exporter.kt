package com.example.utils

import android.content.Context
import android.content.Intent
import com.example.model.DeviceModel
import com.example.model.NetworkInfoModel
import org.json.JSONArray
import org.json.JSONObject

enum class ExportFormat(val extension: String, val mimeType: String, val displayName: String) {
    CSV("csv", "text/csv", "CSV Spreadsheet"),
    TXT("txt", "text/plain", "Text File (.txt)"),
    JSON("json", "application/json", "JSON Data")
}

object Exporter {

    fun generateCsv(devices: List<DeviceModel>): String {
        val sb = StringBuilder()
        sb.append("IP,Hostname,MAC,Status,Response,Vendor,NetworkInterface\n")
        devices.forEach { d ->
            val status = if (d.isOnline) "ONLINE" else "OFFLINE"
            val resp = "${d.responseTimeMs}ms"
            val safeHost = d.hostname.replace(",", " ")
            val safeVendor = d.vendor.replace(",", " ")
            sb.append("${d.ip},$safeHost,${d.mac},$status,$resp,$safeVendor,${d.networkInterface}\n")
        }
        return sb.toString()
    }

    fun generateTxt(networkInfo: NetworkInfoModel, devices: List<DeviceModel>, durationSecs: Long): String {
        val sb = StringBuilder()
        sb.append("=========================================\n")
        sb.append("           IP SCANNER PRO REPORT         \n")
        sb.append("=========================================\n")
        sb.append("Jaringan: ${networkInfo.ssid}\n")
        sb.append("Subnet CIDR: ${networkInfo.cidr}\n")
        sb.append("Gateway: ${networkInfo.gateway}\n")
        sb.append("Durasi Scan: ${durationSecs}s\n")
        sb.append("Total Perangkat: ${devices.size} (Online: ${devices.count { it.isOnline }})\n")
        sb.append("-----------------------------------------\n\n")

        devices.forEachIndexed { index, d ->
            val status = if (d.isOnline) "[ONLINE]" else "[OFFLINE]"
            sb.append("${index + 1}. $status ${d.ip}\n")
            sb.append("   Hostname : ${d.hostname}\n")
            sb.append("   MAC      : ${d.mac}\n")
            sb.append("   Vendor   : ${d.vendor}\n")
            sb.append("   Latency  : ${d.responseTimeMs} ms\n")
            if (d.openPorts.isNotEmpty()) {
                sb.append("   Open Ports: ${d.openPorts.joinToString(", ")}\n")
            }
            sb.append("\n")
        }
        sb.append("=========================================\n")
        return sb.toString()
    }

    fun generateJson(networkInfo: NetworkInfoModel, devices: List<DeviceModel>, durationSecs: Long): String {
        val root = JSONObject()
        root.put("network", networkInfo.ssid)
        root.put("cidr", networkInfo.cidr)
        root.put("gateway", networkInfo.gateway)
        root.put("scanDurationSeconds", durationSecs)
        root.put("totalDevices", devices.size)
        root.put("onlineDevices", devices.count { it.isOnline })

        val array = JSONArray()
        devices.forEach { d ->
            val obj = JSONObject()
            obj.put("ip", d.ip)
            obj.put("hostname", d.hostname)
            obj.put("mac", d.mac)
            obj.put("vendor", d.vendor)
            obj.put("isOnline", d.isOnline)
            obj.put("responseTimeMs", d.responseTimeMs)
            obj.put("networkInterface", d.networkInterface)
            obj.put("firstSeen", d.firstSeen)
            obj.put("lastSeen", d.lastSeen)
            val portsArray = JSONArray()
            d.openPorts.forEach { portsArray.put(it) }
            obj.put("openPorts", portsArray)
            array.put(obj)
        }
        root.put("devices", array)

        return root.toString(2)
    }

    fun shareExport(context: Context, content: String, format: ExportFormat, title: String = "Export Scan") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_SUBJECT, "IP Scanner Pro - $title")
            putExtra(Intent.EXTRA_TEXT, content)
        }
        val chooser = Intent.createChooser(intent, "Bagikan hasil scan ($title)")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}

package com.example.network

import java.util.Locale

object OuiLookup {

    private val OUI_MAP = mapOf(
        "00:50:56" to "VMware",
        "00:0C:29" to "VMware",
        "B8:27:EB" to "Raspberry Pi Foundation",
        "DC:A6:32" to "Raspberry Pi Trading",
        "E4:5F:01" to "Raspberry Pi Trading",
        "28:CD:C1" to "Raspberry Pi Trading",
        "24:0A:C4" to "Espressif Systems (ESP32/ESP8266)",
        "30:AE:A4" to "Espressif Systems",
        "84:CC:A8" to "Espressif Systems",
        "A4:CF:12" to "Espressif Systems",
        "DC:4F:22" to "Espressif Systems",
        "00:1A:11" to "Google Inc.",
        "3C:5A:B4" to "Google Inc.",
        "54:60:09" to "Google Inc.",
        "F4:F5:D8" to "Google Inc.",
        "00:17:88" to "Philips Lighting",
        "00:1F:F3" to "Apple, Inc.",
        "00:25:00" to "Apple, Inc.",
        "3C:D0:F8" to "Apple, Inc.",
        "AC:BC:32" to "Apple, Inc.",
        "F0:18:98" to "Apple, Inc.",
        "F4:0F:24" to "Apple, Inc.",
        "00:12:FB" to "Samsung Electronics",
        "00:15:99" to "Samsung Electronics",
        "00:26:37" to "Samsung Electronics",
        "50:85:69" to "Samsung Electronics",
        "BC:72:B1" to "Samsung Electronics",
        "E8:E5:D6" to "Samsung Electronics",
        "18:FE:34" to "Espressif Inc",
        "60:01:94" to "Espressif Inc",
        "00:1E:58" to "D-Link Corporation",
        "14:D6:4D" to "D-Link Corporation",
        "00:19:E0" to "TP-Link Technologies",
        "50:C7:BF" to "TP-Link Technologies",
        "70:4F:57" to "TP-Link Technologies",
        "E8:48:B8" to "TP-Link Technologies",
        "C0:06:C3" to "TP-Link Technologies",
        "00:18:E7" to "Cameo Communications",
        "00:24:D7" to "Cisco Systems",
        "00:08:E3" to "Cisco Systems",
        "CC:46:D6" to "Cisco Systems",
        "00:0C:43" to "Ralink Technology",
        "00:26:5A" to "D-Link System",
        "00:1A:2B" to "Ayecom Technology",
        "74:D4:35" to "Giga-Byte Technology",
        "00:1D:7E" to "Cisco-Linksys",
        "00:21:29" to "Cisco-Linksys",
        "00:22:6B" to "Cisco-Linksys",
        "00:1B:FC" to "ASUSTeK Computer",
        "04:D9:F5" to "ASUSTeK Computer",
        "AC:9E:17" to "ASUSTeK Computer",
        "00:1F:C6" to "ASUSTeK Computer",
        "00:24:8C" to "ASUSTeK Computer",
        "00:16:D3" to "Wistron NeWeb",
        "00:23:4D" to "Hon Hai Precision (Foxconn)",
        "00:26:B9" to "Dell Inc.",
        "78:2B:CB" to "Dell Inc.",
        "F8:DB:88" to "Dell Inc.",
        "00:1E:0B" to "HP Inc.",
        "3C:D9:2B" to "HP Inc.",
        "A0:D3:C1" to "HP Inc.",
        "00:21:6A" to "Intel Corporate",
        "34:13:E8" to "Intel Corporate",
        "48:51:B7" to "Intel Corporate",
        "00:26:08" to "Huawei Technologies",
        "08:19:A6" to "Huawei Technologies",
        "70:7B:E8" to "Huawei Technologies",
        "84:5B:12" to "Xiaomi Communications",
        "28:6C:07" to "Xiaomi Communications",
        "7C:49:EB" to "Xiaomi Communications",
        "64:09:80" to "Xiaomi Communications",
        "00:04:4B" to "NVIDIA Corporation",
        "00:13:E8" to "Intel Corporate",
        "00:1A:80" to "Sony Corporation",
        "F8:46:1C" to "Sony Interactive Entertainment",
        "00:50:F2" to "Microsoft Corporation",
        "70:66:55" to "Amazon Technologies",
        "FC:65:DE" to "Amazon Technologies",
        "CC:32:E5" to "MikroTik",
        "48:8F:5A" to "MikroTik",
        "00:0C:42" to "MikroTik"
    )

    fun lookup(mac: String?, hostname: String?): String {
        if (!mac.isNullOrBlank() && mac.length >= 8) {
            val cleanMac = mac.trim().uppercase(Locale.US).replace("-", ":")
            val prefix = if (cleanMac.length >= 8) cleanMac.substring(0, 8) else cleanMac
            val vendor = OUI_MAP[prefix]
            if (vendor != null) return vendor
        }

        // Fallback heuristic based on hostname
        if (!hostname.isNullOrBlank()) {
            val h = hostname.lowercase(Locale.ROOT)
            return when {
                h.contains("iphone") || h.contains("ipad") || h.contains("macbook") || h.contains("apple") -> "Apple"
                h.contains("galaxy") || h.contains("samsung") -> "Samsung"
                h.contains("xiaomi") || h.contains("redmi") || h.contains("poco") -> "Xiaomi"
                h.contains("huawei") || h.contains("honor") -> "Huawei"
                h.contains("pixel") || h.contains("nest") || h.contains("chromecast") -> "Google"
                h.contains("desktop-") || h.contains("laptop-") || h.contains("windows") -> "PC / Windows"
                h.contains("raspberry") || h.contains("raspi") -> "Raspberry Pi"
                h.contains("esp_") || h.contains("esp32") || h.contains("esp8266") || h.contains("tasmota") -> "Espressif IoT"
                h.contains("tplink") || h.contains("tp-link") -> "TP-Link"
                h.contains("mikrotik") || h.contains("routerboard") -> "MikroTik"
                h.contains("openwrt") -> "OpenWrt Router"
                h.contains("printer") || h.contains("epson") || h.contains("canon") || h.contains("hp-") -> "Printer"
                h.contains("synology") || h.contains("qnap") -> "NAS Storage"
                h.contains("android") -> "Android Device"
                else -> "Unknown"
            }
        }

        return "Unknown"
    }
}

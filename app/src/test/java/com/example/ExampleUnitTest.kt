package com.example

import com.example.network.scanner.PortScannerEngine
import com.example.utils.IpUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testIpValidation() {
        assertTrue(IpUtils.isValidIpv4("192.168.1.1"))
        assertTrue(IpUtils.isValidIpv4("10.0.0.1"))
        assertTrue(IpUtils.isValidIpv4("172.16.254.254"))
        assertFalse(IpUtils.isValidIpv4("192.168.1.256"))
        assertFalse(IpUtils.isValidIpv4("192.168.1"))
        assertFalse(IpUtils.isValidIpv4("abcd"))
    }

    @Test
    fun testCidrValidationAndCalculation() {
        assertTrue(IpUtils.isValidCidr("192.168.1.0/24"))
        assertTrue(IpUtils.isValidCidr("10.0.0.0/24"))
        assertFalse(IpUtils.isValidCidr("192.168.1.0/35"))

        val range = IpUtils.calculateCidrRange("192.168.1.0/24")
        assertNotNull(range)
        assertEquals("192.168.1.1", range!!.first)
        assertEquals("192.168.1.254", range.second)
        assertEquals(254, range.third)
    }

    @Test
    fun testIpGeneration() {
        val list = IpUtils.generateIpList("192.168.1.1", "192.168.1.5")
        assertEquals(5, list.size)
        assertEquals("192.168.1.1", list[0])
        assertEquals("192.168.1.5", list[4])
    }

    @Test
    fun testCustomPortParsing() {
        val ports = PortScannerEngine.parseCustomPorts("80, 443, 8080, 3000-3002")
        assertTrue(ports.contains(80))
        assertTrue(ports.contains(443))
        assertTrue(ports.contains(8080))
        assertTrue(ports.contains(3000))
        assertTrue(ports.contains(3001))
        assertTrue(ports.contains(3002))
    }
}

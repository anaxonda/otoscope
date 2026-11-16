package dev.rubec.otoscope.stream.jegoat

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JegoatLedControlTest {
    @Test fun `new connection starts at full brightness`() {
        val led = JegoatLedControl()
        assertEquals(100, led.brightness.value)
        assertTrue(led.enabled.value)
    }

    @Test fun `toggle restores the chosen dim level`() {
        val led = JegoatLedControl()
        led.setBrightness(37)
        led.setEnabled(false)
        assertEquals(0, led.brightness.value)
        assertFalse(led.enabled.value)
        led.setEnabled(true)
        assertEquals(37, led.brightness.value)
        assertTrue(led.enabled.value)
    }

    @Test fun `slider zero turns off and retains the last lit level`() {
        val led = JegoatLedControl()
        led.setBrightness(25)
        led.setBrightness(0)
        assertFalse(led.enabled.value)
        led.setEnabled(true)
        assertEquals(25, led.brightness.value)
        led.setEnabled(false)
        led.setBrightness(10)
        assertTrue(led.enabled.value)
        assertEquals(10, led.brightness.value)
    }

    @Test fun `brightness requests are clamped to the device range`() {
        val led = JegoatLedControl()
        led.setBrightness(-1)
        assertEquals(0, led.brightness.value)
        assertFalse(led.enabled.value)
        led.setBrightness(101)
        assertEquals(100, led.brightness.value)
        assertTrue(led.enabled.value)
    }

    @Test fun `all brightness levels encode a four byte little endian command`() {
        for (level in 0..100) {
            assertContentEquals(byteArrayOf(0x10, 0x03, level.toByte(), 0), ledBrightnessCommand(level))
        }
    }

    @Test fun `out of range values cannot escape into the wire command`() {
        assertContentEquals(byteArrayOf(0x10, 0x03, 0, 0), ledBrightnessCommand(Int.MIN_VALUE))
        assertContentEquals(byteArrayOf(0x10, 0x03, 100, 0), ledBrightnessCommand(Int.MAX_VALUE))
    }
}

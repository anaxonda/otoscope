package dev.rubec.otoscope.stream.jegoat

import dev.rubec.otoscope.stream.DimmableLedControl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** JEGOAT does not report LED state: these flows reflect the requested level. */
internal class JegoatLedControl : DimmableLedControl {
    private val requestedBrightness = MutableStateFlow(100)
    override val brightness: StateFlow<Int> = requestedBrightness.asStateFlow()

    private val requestedEnabled = MutableStateFlow(true)
    override val enabled: StateFlow<Boolean> = requestedEnabled.asStateFlow()

    private var lastNonzeroBrightness = 100

    override fun setBrightness(percent: Int) {
        val level = percent.coerceIn(0, 100)
        if (level > 0) lastNonzeroBrightness = level
        requestedBrightness.value = level
        requestedEnabled.value = level > 0
    }

    override fun setEnabled(on: Boolean) {
        setBrightness(if (on) lastNonzeroBrightness else 0)
    }
}

/** 0x10 0x03 followed by brightness (0..100), uint16 little-endian. */
internal fun ledBrightnessCommand(percent: Int): ByteArray =
    byteArrayOf(0x10, 0x03, percent.coerceIn(0, 100).toByte(), 0)

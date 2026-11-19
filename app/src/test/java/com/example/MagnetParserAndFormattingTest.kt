package com.example

import com.example.torrent.MagnetParser
import com.example.ui.components.formatBytes
import com.example.ui.components.formatCountdown
import com.example.ui.components.formatEta
import com.example.ui.components.formatLimitKbps
import com.example.ui.components.formatSpeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MagnetParserAndFormattingTest {

    @Test
    fun parseHexMagnetUri_extractsHashNameAndTrackers() {
        val uri = "magnet:?xt=urn:btih:DD8255ECDC7CA55FB0BBF81323D87062DB1F6D1C&dn=Big+Buck+Bunny&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337"
        val parsed = MagnetParser.parse(uri)
        assertNotNull(parsed)
        assertEquals("dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c", parsed!!.infoHash)
        assertEquals("Big Buck Bunny", parsed.displayName)
        assertEquals(1, parsed.trackers.size)
        assertEquals("udp://tracker.opentrackr.org:1337", parsed.trackers.first())
    }

    @Test
    fun parseBase32MagnetUri_convertsToHex() {
        val uri = "magnet:?xt=urn:btih:3WBFK3G4PSXFXMF37AJSHWDQMLNR63I4&dn=TestFile"
        val parsed = MagnetParser.parse(uri)
        assertNotNull(parsed)
        assertEquals(40, parsed!!.infoHash.length)
        assertEquals("TestFile", parsed.displayName)
    }

    @Test
    fun parseInvalidUri_returnsNull() {
        assertNull(MagnetParser.parse("https://example.com/file.torrent"))
        assertNull(MagnetParser.parse("magnet:?xt=urn:btih:short"))
        assertNull(MagnetParser.parse(""))
    }

    @Test
    fun formattingUtilities_produceExpectedStrings() {
        assertEquals("0 Б", formatBytes(0L))
        assertEquals("1.0 МБ", formatBytes(1024L * 1024L))
        assertEquals("0 КБ/с", formatSpeed(0L))
        assertEquals("2.00 МБ/с", formatSpeed(2L * 1024L * 1024L))
        assertEquals("Без лимита", formatLimitKbps(0))
        assertEquals("512 КБ/с", formatLimitKbps(512))
        assertEquals("2.0 МБ/с", formatLimitKbps(2048))
        assertEquals("05:00", formatCountdown(300))
        assertEquals("1м 30с", formatEta(90L))
    }
}

package com.example.torrent

import java.net.URLDecoder
import java.util.Locale

data class ParsedMagnet(
    val infoHash: String,
    val displayName: String?,
    val trackers: List<String>,
    val rawUri: String
)

object MagnetParser {

    fun parse(rawInput: String): ParsedMagnet? {
        val trimmed = rawInput.trim()
        if (!trimmed.startsWith("magnet:?", ignoreCase = true)) return null

        val queryPart = trimmed.substring(8) // after "magnet:?"
        if (queryPart.isBlank()) return null

        val params = LinkedHashMap<String, MutableList<String>>()
        for (pair in queryPart.split('&')) {
            if (pair.isEmpty()) continue
            val eqIdx = pair.indexOf('=')
            val rawKey = if (eqIdx >= 0) pair.substring(0, eqIdx) else pair
            val rawVal = if (eqIdx >= 0) pair.substring(eqIdx + 1) else ""
            val key = decodeComponent(rawKey).lowercase(Locale.US)
            val value = decodeComponent(rawVal)
            if (key.isNotEmpty()) {
                params.getOrPut(key) { mutableListOf() }.add(value)
            }
        }

        val xtValues = params["xt"].orEmpty()
        val btihRaw = xtValues
            .firstOrNull { it.startsWith("urn:btih:", ignoreCase = true) }
            ?.substringAfter("urn:btih:", "")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        val normalizedHash = when (btihRaw.length) {
            40 -> {
                if (btihRaw.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
                    btihRaw.lowercase(Locale.US)
                } else {
                    return null
                }
            }
            32 -> base32ToHex(btihRaw) ?: return null
            else -> return null
        }

        val dn = params["dn"]?.firstOrNull()?.takeIf { it.isNotBlank() }
        val trackers = params["tr"].orEmpty().filter { it.isNotBlank() }.distinct()

        return ParsedMagnet(
            infoHash = normalizedHash,
            displayName = dn,
            trackers = trackers,
            rawUri = trimmed
        )
    }

    private fun decodeComponent(value: String): String =
        runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)

    private fun base32ToHex(base32: String): String? {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val clean = base32.uppercase(Locale.US).replace("=", "")
        if (clean.length != 32) return null

        val bytes = ByteArray(20)
        var buffer = 0
        var bitsLeft = 0
        var count = 0

        for (ch in clean) {
            val valIdx = alphabet.indexOf(ch)
            if (valIdx < 0) return null
            buffer = (buffer shl 5) or valIdx
            bitsLeft += 5
            if (bitsLeft >= 8) {
                if (count < 20) {
                    bytes[count++] = ((buffer shr (bitsLeft - 8)) and 0xFF).toByte()
                }
                bitsLeft -= 8
            }
        }
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

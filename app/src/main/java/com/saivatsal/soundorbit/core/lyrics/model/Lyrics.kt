package com.saivatsal.soundorbit.core.lyrics.model

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

data class Lyrics(
    val trackId: String,
    val plainLyrics: String?,
    val syncedLyrics: List<LyricLine>,
    val isInstrumental: Boolean = false
) {
    val isSynced: Boolean get() = syncedLyrics.isNotEmpty()
    val hasLyrics: Boolean get() = !isInstrumental && (plainLyrics != null || syncedLyrics.isNotEmpty())

    fun activeLineIndex(currentPositionMs: Long): Int {
        if (syncedLyrics.isEmpty()) return -1
        // Binary search or linear scan for the closest lyric line at or before currentPositionMs
        var low = 0
        var high = syncedLyrics.size - 1
        var result = -1

        while (low <= high) {
            val mid = (low + high) ushr 1
            if (syncedLyrics[mid].timestampMs <= currentPositionMs) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }
}

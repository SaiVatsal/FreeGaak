package com.saivatsal.soundorbit.core.lyrics.parser

import com.saivatsal.soundorbit.core.lyrics.model.LyricLine

object LrcParser {

    private val TIME_TAG_REGEX = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{2,3}))?\]""")
    private val METADATA_OFFSET_REGEX = Regex("""\[offset:\s*([+-]?\d+)\s*\]""", RegexOption.IGNORE_CASE)

    fun parse(lrcContent: String?): List<LyricLine> {
        if (lrcContent.isNullOrBlank()) return emptyList()

        var globalOffsetMs = 0L
        val lines = lrcContent.lines()

        // Scan for offset tag first
        for (line in lines) {
            val offsetMatch = METADATA_OFFSET_REGEX.find(line.trim())
            if (offsetMatch != null) {
                globalOffsetMs = offsetMatch.groupValues[1].toLongOrNull() ?: 0L
            }
        }

        val parsedLines = mutableListOf<LyricLine>()

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank()) continue

            // Skip metadata tags like [ar:Artist], [ti:Title], [offset:123], etc.
            if (line.startsWith("[") && !TIME_TAG_REGEX.containsMatchIn(line)) {
                continue
            }

            val matches = TIME_TAG_REGEX.findAll(line).toList()
            if (matches.isEmpty()) continue

            // Extract the lyric text after all timestamp tags
            val lastMatch = matches.last()
            val textStartIndex = lastMatch.range.last + 1
            val text = if (textStartIndex < line.length) {
                line.substring(textStartIndex).trim()
            } else {
                ""
            }

            // A line can have multiple timestamp tags: [00:12.34][00:45.67] Same chorus
            for (match in matches) {
                val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                val fractionStr = match.groupValues.getOrNull(3).orEmpty()

                val millis = when (fractionStr.length) {
                    2 -> (fractionStr.toLongOrNull() ?: 0L) * 10L // Centiseconds to ms
                    3 -> fractionStr.toLongOrNull() ?: 0L        // Milliseconds
                    1 -> (fractionStr.toLongOrNull() ?: 0L) * 100L
                    else -> 0L
                }

                val totalTimestampMs = (minutes * 60 * 1000) + (seconds * 1000) + millis + globalOffsetMs
                parsedLines.add(LyricLine(timestampMs = totalTimestampMs.coerceAtLeast(0L), text = text))
            }
        }

        return parsedLines.sortedBy { it.timestampMs }
    }
}

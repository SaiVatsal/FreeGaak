package com.saivatsal.soundorbit

import org.junit.Test
import java.io.File
import org.junit.Assert.assertTrue

class ArchitectureTest {

    @Test
    fun coreNeverImportsFeature() {
        val projectDir = System.getProperty("user.dir") ?: "."
        val coreDir = listOf(
            File(projectDir, "src/main/java/com/saivatsal/soundorbit/core"),
            File(projectDir, "app/src/main/java/com/saivatsal/soundorbit/core")
        ).firstOrNull { it.exists() } ?: error("Core directory not found relative to $projectDir")

        val kotlinFiles = coreDir.walkTopDown().filter { it.extension == "kt" }.toList()
        assertTrue("Core files should not be empty", kotlinFiles.isNotEmpty())
        val violations = mutableListOf<String>()

        kotlinFiles.forEach { file ->
            file.useLines { lines ->
                lines.forEachIndexed { index, line ->
                    if (line.trim().startsWith("import com.saivatsal.soundorbit.feature.")) {
                        violations.add("${file.name}:${index + 1} - $line")
                    }
                }
            }
        }

        assertTrue(
            "Architecture violation: core must never import feature layer!\nViolations found:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    @Test
    fun noRawSecretsInCode() {
        val projectDir = System.getProperty("user.dir") ?: "."
        val srcDir = listOf(
            File(projectDir, "src/main/java"),
            File(projectDir, "app/src/main/java")
        ).firstOrNull { it.exists() } ?: error("Source directory not found relative to $projectDir")

        val kotlinFiles = srcDir.walkTopDown().filter { it.extension == "kt" }.toList()
        assertTrue("Source files should not be empty", kotlinFiles.isNotEmpty())
        val violations = mutableListOf<String>()

        val suspiciousPatterns = listOf(
            Regex("(?i)api[_-]?key\\s*=\\s*\"[a-zA-Z0-9_-]{10,}\""),
            Regex("(?i)secret\\s*=\\s*\"[a-zA-Z0-9_-]{10,}\""),
            Regex("(?i)bearer\\s+[a-zA-Z0-9._-]{20,}")
        )

        kotlinFiles.forEach { file ->
            file.useLines { lines ->
                lines.forEachIndexed { index, line ->
                    suspiciousPatterns.forEach { pattern ->
                        if (pattern.containsMatchIn(line) && !line.contains("BuildConfig")) {
                            violations.add("${file.name}:${index + 1}")
                        }
                    }
                }
            }
        }

        assertTrue(
            "Hardcoded secrets detected in source files:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }
}

package org.cangnova.cangjie.test.services.impl

import org.cangnova.cangjie.test.isTeamCityBuild
import org.cangnova.cangjie.test.services.AssertionsService
import org.cangnova.cangjie.utils.convertLineSeparators
import org.cangnova.cangjie.utils.rethrow
import org.cangnova.cangjie.utils.trimTrailingWhitespacesAndAddNewlineAtEOF
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.function.Executable
import org.opentest4j.AssertionFailedError
import org.opentest4j.FileInfo
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import org.junit.jupiter.api.Assertions as JUnit5PlatformAssertions

object JUnit5Assertions : AssertionsService() {
    override fun doesEqualToFile(expectedFile: File, actual: String, sanitizer: (String) -> String): Boolean {
        return doesEqualToFile(
            expectedFile,
            actual,
            sanitizer,
            fileNotFoundMessageTeamCity = { "Expected data file did not exist `$expectedFile`" },
            fileNotFoundMessageLocal = { "Expected data file did not exist. Generating: $expectedFile" }).first
    }

    private fun doesEqualToFile(
        expectedFile: File,
        actual: String,
        sanitizer: (String) -> String,
        fileNotFoundMessageTeamCity: (File) -> String,
        fileNotFoundMessageLocal: (File) -> String,
    ): Pair<Boolean, String> {
        try {
            val actualText = actual.trim { it <= ' ' }.convertLineSeparators().trimTrailingWhitespacesAndAddNewlineAtEOF()
            if (!expectedFile.exists()) {
                if (isTeamCityBuild) {
                    org.junit.jupiter.api.fail(fileNotFoundMessageTeamCity(expectedFile))
                } else {
                    expectedFile.parentFile.mkdirs()
                    expectedFile.writeText(actualText)
                    org.junit.jupiter.api.fail(fileNotFoundMessageLocal(expectedFile))
                }
            }
            val expected = expectedFile.readText().convertLineSeparators()
            val expectedText = expected.trim { it <= ' ' }.trimTrailingWhitespacesAndAddNewlineAtEOF()
            return Pair(sanitizer.invoke(expectedText) == sanitizer.invoke(actualText), expected)
        } catch (e: IOException) {
            throw rethrow(e)
        }
    }

    override fun assertEqualsToFile(expectedFile: File, actual: String, sanitizer: (String) -> String, message: () -> String) {
        val (equalsToFile, expected) = doesEqualToFile(
            expectedFile, actual, sanitizer,
            fileNotFoundMessageTeamCity = { "Expected data file did not exist `$expectedFile`" },
            fileNotFoundMessageLocal = { "Expected data file did not exist. Generating: $expectedFile" },
        )
        if (!equalsToFile) {
            val details = buildString {
                appendLine()
                appendLine("=====预期======")
                appendLine(expected)
                appendLine("=====得到======")
                appendLine(actual)
            }
            throw AssertionFailedError(
                "${message()}: ${expectedFile.name}$details",
                FileInfo(expectedFile.absolutePath, expected.toByteArray(StandardCharsets.UTF_8)),
                actual,
            )
        }
    }

    override fun assertEquals(expected: Any?, actual: Any?, message: (() -> String)?) {
        JUnit5PlatformAssertions.assertEquals(expected, actual, message)
    }

    override fun assertNotEquals(expected: Any?, actual: Any?, message: (() -> String)?) {
        JUnit5PlatformAssertions.assertNotEquals(expected, actual, message)
    }

    override fun assertTrue(value: Boolean, message: (() -> String)?) {
        JUnit5PlatformAssertions.assertTrue(value, message)
    }

    override fun assertFalse(value: Boolean, message: (() -> String)?) {
        JUnit5PlatformAssertions.assertFalse(value, message)
    }

    override fun failAll(exceptions: List<Throwable>) {
        exceptions.singleOrNull()?.let { throw it }
        JUnit5PlatformAssertions.assertAll(exceptions.sortedWith(AssertionFailedErrorFirst).map { Executable { throw it } })
    }

    override fun assertAll(conditions: List<() -> Unit>) {
        JUnit5PlatformAssertions.assertAll(conditions.map { Executable { it() } })
    }

    override fun assertNotNull(value: Any?, message: (() -> String)?) {
        JUnit5PlatformAssertions.assertNotNull(value, message)
    }

    override fun <T> assertSameElements(expected: Collection<T>, actual: Collection<T>, message: (() -> String)?) {
        JUnit5PlatformAssertions.assertIterableEquals(expected, actual, message)
    }

    override fun fail(message: () -> String): Nothing {
        org.junit.jupiter.api.fail(message)
    }

    override fun assumeFalse(value: Boolean, message: () -> String) {
        Assumptions.assumeFalse(value, message)
    }

    private object AssertionFailedErrorFirst : Comparator<Throwable> {
        override fun compare(o1: Throwable, o2: Throwable): Int {
            return when {
                o1 is AssertionFailedError && o2 is AssertionFailedError -> 0
                o1 is AssertionFailedError -> -1
                o2 is AssertionFailedError -> 1
                else -> 0
            }
        }
    }
}

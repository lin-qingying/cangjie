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

/**
 * 提供 `JUnit5Assertions` 单例，集中承载测试服务的共享状态、常量或默认行为。
 */
object JUnit5Assertions : AssertionsService() {
    /**
     * 执行 `doesEqualToFile` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    override fun doesEqualToFile(expectedFile: File, actual: String, sanitizer: (String) -> String): Boolean {
        return doesEqualToFile(
            expectedFile,
            actual,
            sanitizer,
            fileNotFoundMessageTeamCity = { "Expected data file did not exist `$expectedFile`" },
            fileNotFoundMessageLocal = { "Expected data file did not exist. Generating: $expectedFile" }).first
    }

    /**
     * 提供 `doesEqualToFile` 对应的测试服务流程，维持测试框架的阶段契约。
     */
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

    /**
     * 执行 `assertEqualsToFile` 对应的测试服务流程，维持测试框架的阶段契约。
     */
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
                "${message()}: ${expectedFile.name} $details",
                FileInfo(expectedFile.absolutePath, expected.toByteArray(StandardCharsets.UTF_8)),
                actual,
            )
        }
    }

    /**
     * 执行 `assertEquals` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    override fun assertEquals(expected: Any?, actual: Any?, message: (() -> String)?) {
        JUnit5PlatformAssertions.assertEquals(expected, actual, message)
    }

    /**
     * 执行 `assertNotEquals` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    override fun assertNotEquals(expected: Any?, actual: Any?, message: (() -> String)?) {
        JUnit5PlatformAssertions.assertNotEquals(expected, actual, message)
    }

    /**
     * 执行 `assertTrue` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    override fun assertTrue(value: Boolean, message: (() -> String)?) {
        JUnit5PlatformAssertions.assertTrue(value, message)
    }

    /**
     * 执行 `assertFalse` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    override fun assertFalse(value: Boolean, message: (() -> String)?) {
        JUnit5PlatformAssertions.assertFalse(value, message)
    }

    /**
     * 执行 `failAll` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    override fun failAll(exceptions: List<Throwable>) {
        exceptions.singleOrNull()?.let { throw it }
        JUnit5PlatformAssertions.assertAll(exceptions.sortedWith(AssertionFailedErrorFirst).map { Executable { throw it } })
    }

    /**
     * 执行 `assertAll` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    override fun assertAll(conditions: List<() -> Unit>) {
        JUnit5PlatformAssertions.assertAll(conditions.map { Executable { it() } })
    }

    /**
     * 执行 `assertNotNull` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    override fun assertNotNull(value: Any?, message: (() -> String)?) {
        JUnit5PlatformAssertions.assertNotNull(value, message)
    }

    /**
     * 执行 `assertSameElements` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    override fun <T> assertSameElements(expected: Collection<T>, actual: Collection<T>, message: (() -> String)?) {
        JUnit5PlatformAssertions.assertIterableEquals(expected, actual, message)
    }

    /**
     * 执行 `fail` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    override fun fail(message: () -> String): Nothing {
        org.junit.jupiter.api.fail(message)
    }

    /**
     * 执行 `assumeFalse` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    override fun assumeFalse(value: Boolean, message: () -> String) {
        Assumptions.assumeFalse(value, message)
    }

    /**
     * 提供 `AssertionFailedErrorFirst` 单例，集中承载测试服务的共享状态、常量或默认行为。
     */
    private object AssertionFailedErrorFirst : Comparator<Throwable> {
        /**
         * 执行 `compare` 对应的测试服务流程，维持测试框架的阶段契约。
         */
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

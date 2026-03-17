package org.cangnova.cangjie.test

import java.io.File
import java.nio.file.Path

val isTeamCityBuild: Boolean = System.getenv("TEAMCITY_VERSION") != null

/**
 * 断言基类
 *
 * 对应 Kotlin K2 的 Assertions
 */
abstract class Assertions {
    fun assertEqualsToFile(expectedFile: File, actual: String, sanitizer: (String) -> String = { it }) {
        assertEqualsToFile(expectedFile, actual, sanitizer) { "Actual data differs from file content" }
    }

    abstract fun doesEqualToFile(expectedFile: File, actual: String, sanitizer: (String) -> String = { it }): Boolean

    fun assertEqualsToFile(expectedFile: Path, actual: String, sanitizer: (String) -> String = { it }) {
        assertEqualsToFile(expectedFile.toFile(), actual, sanitizer)
    }

    abstract fun assertEqualsToFile(
        expectedFile: File,
        actual: String,
        sanitizer: (String) -> String = { it },
        message: (() -> String)
    )

    fun assertFileDoesntExist(file: File, errorMessage: () -> String) {
        if (file.exists()) {
            if (!isTeamCityBuild) {
                file.delete()
            }
            fail(errorMessage)
        }
    }

    abstract fun assertEquals(expected: Any?, actual: Any?, message: (() -> String)? = null)
    abstract fun assertNotEquals(expected: Any?, actual: Any?, message: (() -> String)? = null)
    abstract fun assertTrue(value: Boolean, message: (() -> String)? = null)
    abstract fun assertFalse(value: Boolean, message: (() -> String)? = null)
    abstract fun assertNotNull(value: Any?, message: (() -> String)? = null)
    abstract fun <T> assertSameElements(expected: Collection<T>, actual: Collection<T>, message: (() -> String)?)

    fun <T> assertContainsElements(collection: Collection<T>, vararg expected: T) {
        assertContainsElements(collection, expected.toList())
    }

    fun <T> assertContainsElements(collection: Collection<T>, expected: Collection<T>) {
        val copy = ArrayList(collection)
        copy.retainAll(expected)
        assertSameElements(copy, expected) { renderCollectionToString(collection) }
    }

    fun renderCollectionToString(collection: Iterable<*>): String {
        if (!collection.iterator().hasNext()) {
            return "<empty>"
        }

        return collection.joinToString("\n")
    }

    abstract fun failAll(exceptions: List<Throwable>)
    abstract fun assertAll(conditions: List<() -> Unit>)

    fun assertAll(vararg conditions: () -> Unit) {
        assertAll(conditions.toList())
    }

    abstract fun fail(message: () -> String): Nothing

    open fun assumeFalse(value: Boolean, message: () -> String) {
        assertFalse(value, message)
    }
}
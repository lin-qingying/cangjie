package org.cangnova.cangjie.test.services.impl

import org.cangnova.cangjie.test.services.AssertionsService
import java.io.File

open class DefaultAssertionsService : AssertionsService() {
    override fun doesEqualToFile(expectedFile: File, actual: String, sanitizer: (String) -> String): Boolean {
        if (!expectedFile.exists()) return false
        return sanitizer(expectedFile.readText()) == sanitizer(actual)
    }

    override fun assertEqualsToFile(expectedFile: File, actual: String, sanitizer: (String) -> String, message: () -> String) {
        val expected = expectedFile.takeIf { it.exists() }?.readText() ?: ""
        if (sanitizer(expected) != sanitizer(actual)) {
            throw AssertionError(message())
        }
    }

    override fun assertEquals(expected: Any?, actual: Any?, message: (() -> String)?) {
        if (expected != actual) throw AssertionError(message?.invoke() ?: "Expected <$expected>, actual <$actual>")
    }

    override fun assertNotEquals(expected: Any?, actual: Any?, message: (() -> String)?) {
        if (expected == actual) throw AssertionError(message?.invoke() ?: "Expected values to differ, both are <$actual>")
    }

    override fun assertTrue(value: Boolean, message: (() -> String)?) {
        if (!value) throw AssertionError(message?.invoke() ?: "Expected true")
    }

    override fun assertFalse(value: Boolean, message: (() -> String)?) {
        if (value) throw AssertionError(message?.invoke() ?: "Expected false")
    }

    override fun assertNotNull(value: Any?, message: (() -> String)?) {
        if (value == null) throw AssertionError(message?.invoke() ?: "Expected non-null")
    }

    override fun <T> assertSameElements(expected: Collection<T>, actual: Collection<T>, message: (() -> String)?) {
        if (expected.toSet() != actual.toSet()) {
            throw AssertionError(message?.invoke() ?: "Collections differ")
        }
    }

    override fun failAll(exceptions: List<Throwable>) {
        if (exceptions.isNotEmpty()) {
            throw AssertionError("There are ${exceptions.size} failures", exceptions.first())
        }
    }

    override fun assertAll(conditions: List<() -> Unit>) {
        val failures = mutableListOf<Throwable>()
        conditions.forEach {
            runCatching(it).exceptionOrNull()?.let(failures::add)
        }
        if (failures.isNotEmpty()) {
            throw AssertionError("There are ${failures.size} assertion failures", failures.first())
        }
    }

    override fun fail(message: () -> String): Nothing = throw AssertionError(message())
}

object JUnit5Assertions : DefaultAssertionsService()

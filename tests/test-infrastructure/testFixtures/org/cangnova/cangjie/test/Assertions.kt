package org.cangnova.cangjie.test

import java.io.File
import java.nio.file.Path

/**
 * 保存 `isTeamCityBuild`，供测试基础设施在测试执行期间读取或传递。
 */
val isTeamCityBuild: Boolean = System.getenv("TEAMCITY_VERSION") != null

/**
 * 断言基类
 *
 * 对应 Kotlin K2 的 Assertions
 */
abstract class Assertions {
    /**
     * 执行 `assertEqualsToFile` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    fun assertEqualsToFile(expectedFile: File, actual: String, sanitizer: (String) -> String = { it }) {
        assertEqualsToFile(expectedFile, actual, sanitizer) { "Actual data differs from file content" }
    }

    /**
     * 提供 `doesEqualToFile` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    abstract fun doesEqualToFile(expectedFile: File, actual: String, sanitizer: (String) -> String = { it }): Boolean

    /**
     * 执行 `assertEqualsToFile` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    fun assertEqualsToFile(expectedFile: Path, actual: String, sanitizer: (String) -> String = { it }) {
        assertEqualsToFile(expectedFile.toFile(), actual, sanitizer)
    }

    /**
     * 提供 `assertEqualsToFile` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    abstract fun assertEqualsToFile(
        expectedFile: File,
        actual: String,
        sanitizer: (String) -> String = { it },
        message: (() -> String)
    )

    /**
     * 执行 `assertFileDoesntExist` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    fun assertFileDoesntExist(file: File, errorMessage: () -> String) {
        if (file.exists()) {
            if (!isTeamCityBuild) {
                file.delete()
            }
            fail(errorMessage)
        }
    }

    /**
     * 提供 `assertEquals` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    abstract fun assertEquals(expected: Any?, actual: Any?, message: (() -> String)? = null)
    /**
     * 提供 `assertNotEquals` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    abstract fun assertNotEquals(expected: Any?, actual: Any?, message: (() -> String)? = null)
    /**
     * 提供 `assertTrue` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    abstract fun assertTrue(value: Boolean, message: (() -> String)? = null)
    /**
     * 提供 `assertFalse` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    abstract fun assertFalse(value: Boolean, message: (() -> String)? = null)
    /**
     * 提供 `assertNotNull` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    abstract fun assertNotNull(value: Any?, message: (() -> String)? = null)
    /**
     * 提供 `assertSameElements` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    abstract fun <T> assertSameElements(expected: Collection<T>, actual: Collection<T>, message: (() -> String)?)

    /**
     * 执行 `assertContainsElements` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    fun <T> assertContainsElements(collection: Collection<T>, vararg expected: T) {
        assertContainsElements(collection, expected.toList())
    }

    /**
     * 执行 `assertContainsElements` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    fun <T> assertContainsElements(collection: Collection<T>, expected: Collection<T>) {
        val copy = ArrayList(collection)
        copy.retainAll(expected)
        assertSameElements(copy, expected) { renderCollectionToString(collection) }
    }

    /**
     * 执行 `renderCollectionToString` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    fun renderCollectionToString(collection: Iterable<*>): String {
        if (!collection.iterator().hasNext()) {
            return "<empty>"
        }

        return collection.joinToString("\n")
    }

    /**
     * 提供 `failAll` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    abstract fun failAll(exceptions: List<Throwable>)
    /**
     * 提供 `assertAll` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    abstract fun assertAll(conditions: List<() -> Unit>)

    /**
     * 执行 `assertAll` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    fun assertAll(vararg conditions: () -> Unit) {
        assertAll(conditions.toList())
    }

    /**
     * 提供 `fail` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    abstract fun fail(message: () -> String): Nothing

    /**
     * 提供 `assumeFalse` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    open fun assumeFalse(value: Boolean, message: () -> String) {
        assertFalse(value, message)
    }
}

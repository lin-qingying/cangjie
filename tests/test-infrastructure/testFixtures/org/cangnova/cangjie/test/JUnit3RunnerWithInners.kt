package org.cangnova.cangjie.test

import junit.framework.TestCase
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.manipulation.Filter
import org.junit.runner.manipulation.Filterable
import org.junit.runner.manipulation.NoTestsRemainException
import org.junit.runner.manipulation.Sortable
import org.junit.runner.manipulation.Sorter
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunNotifier
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * 最小可用的 Runner：执行一个类及其所有嵌套类中的测试。
 *
 * 目前仓库内的 Kotlin 编译器风格测试主要满足以下约定，因此该实现足够使用：
 * - 测试类继承 [TestCase]
 * - 测试方法命名为 `test*`
 * - 测试分组通过嵌套（静态）类表达
 */
class JUnit3RunnerWithInners(private val klass: Class<*>) : Runner(), Filterable, Sortable {
    /**
     * 保存 `runners`，供测试基础设施在测试执行期间读取或传递。
     */
    private val runners: MutableList<Runner> = buildRunners(klass).toMutableList()

    /**
     * 执行 `getDescription` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    override fun getDescription(): Description {
        return Description.createSuiteDescription(klass).also { suite ->
            runners.forEach { suite.addChild(it.description) }
        }
    }

    /**
     * 执行 `run` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    override fun run(notifier: RunNotifier) {
        runners.forEach { it.run(notifier) }
    }

    /**
     * 执行 `filter` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    override fun filter(filter: Filter) {
        val it = runners.listIterator()
        while (it.hasNext()) {
            val runner = it.next()
            try {
                (runner as? Filterable)?.filter(filter)
            } catch (_: NoTestsRemainException) {
                it.remove()
            }
        }
        if (runners.isEmpty()) throw NoTestsRemainException()
    }

    /**
     * 执行 `sort` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    override fun sort(sorter: Sorter) {
        runners.forEach { (it as? Sortable)?.sort(sorter) }
    }

    /**
     * 提供 `buildRunners` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    private fun buildRunners(root: Class<*>): List<Runner> {
        val classes = buildList {
            add(root)
            collectNestedClassesRecursively(root, this)
        }
        return classes.mapNotNull(::createRunnerOrNull)
    }

    /**
     * 提供 `collectNestedClassesRecursively` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    private fun collectNestedClassesRecursively(klass: Class<*>, out: MutableList<Class<*>>) {
        for (nested in klass.declaredClasses) {
            if (nested.isSynthetic) continue
            out.add(nested)
            collectNestedClassesRecursively(nested, out)
        }
    }

    /**
     * 提供 `createRunnerOrNull` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    private fun createRunnerOrNull(klass: Class<*>): Runner? {
        if (TestCase::class.java.isAssignableFrom(klass)) {
            return JUnit38ClassRunner(klass)
        }
        val testLikeMethods = klass.declaredMethods
            .filter(::isLegacyTestMethod)
            .sortedBy { it.name }
        if (testLikeMethods.isEmpty()) return null
        return LegacyNameBasedRunner(klass, testLikeMethods)
    }

    /**
     * 提供 `isLegacyTestMethod` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    private fun isLegacyTestMethod(method: Method): Boolean {
        if (!method.name.startsWith("test")) return false
        if (method.parameterCount != 0) return false
        if (method.returnType != Void.TYPE) return false
        return Modifier.isPublic(method.modifiers)
    }
}

/**
 * 表示 `LegacyNameBasedRunner`，承载测试基础设施中的配置数据、测试产物或处理步骤。
 */
private class LegacyNameBasedRunner(
    /**
     * 保存 `klass`，供测试基础设施在测试执行期间读取或传递。
     */
    private val klass: Class<*>,
    methods: List<Method>,
) : Runner(), Filterable, Sortable {
    /**
     * 保存 `methods`，供测试基础设施在测试执行期间读取或传递。
     */
    private val methods: MutableList<Method> = methods.toMutableList()

    /**
     * 执行 `getDescription` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    override fun getDescription(): Description {
        return Description.createSuiteDescription(klass).also { suite ->
            methods.forEach { method ->
                suite.addChild(Description.createTestDescription(klass, method.name))
            }
        }
    }

    /**
     * 执行 `run` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    override fun run(notifier: RunNotifier) {
        for (method in methods) {
            val description = Description.createTestDescription(klass, method.name)
            notifier.fireTestStarted(description)
            try {
                val instance = klass.getDeclaredConstructor().newInstance()
                method.isAccessible = true
                method.invoke(instance)
                notifier.fireTestFinished(description)
            } catch (t: Throwable) {
                notifier.fireTestFailure(Failure(description, unwrapInvocationException(t)))
                notifier.fireTestFinished(description)
            }
        }
    }

    /**
     * 执行 `filter` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    override fun filter(filter: Filter) {
        val iterator = methods.listIterator()
        while (iterator.hasNext()) {
            val method = iterator.next()
            val description = Description.createTestDescription(klass, method.name)
            if (!filter.shouldRun(description)) {
                iterator.remove()
            }
        }
        if (methods.isEmpty()) throw NoTestsRemainException()
    }

    /**
     * 执行 `sort` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    override fun sort(sorter: Sorter) {
        methods.sortWith { left, right ->
            sorter.compare(
                Description.createTestDescription(klass, left.name),
                Description.createTestDescription(klass, right.name),
            )
        }
    }

    /**
     * 提供 `unwrapInvocationException` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    private fun unwrapInvocationException(t: Throwable): Throwable {
        return t.cause?.takeIf { t is java.lang.reflect.InvocationTargetException } ?: t
    }
}

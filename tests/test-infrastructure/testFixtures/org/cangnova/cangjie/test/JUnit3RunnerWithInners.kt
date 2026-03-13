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
import org.junit.runner.notification.RunNotifier

/**
 * 最小可用的 Runner：执行一个类及其所有嵌套类中的测试。
 *
 * 目前仓库内的 Kotlin 编译器风格测试主要满足以下约定，因此该实现足够使用：
 * - 测试类继承 [TestCase]
 * - 测试方法命名为 `test*`
 * - 测试分组通过嵌套（静态）类表达
 */
class JUnit3RunnerWithInners(private val klass: Class<*>) : Runner(), Filterable, Sortable {
    private val runners: MutableList<Runner> = buildRunners(klass).toMutableList()

    override fun getDescription(): Description {
        return Description.createSuiteDescription(klass).also { suite ->
            runners.forEach { suite.addChild(it.description) }
        }
    }

    override fun run(notifier: RunNotifier) {
        runners.forEach { it.run(notifier) }
    }

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

    override fun sort(sorter: Sorter) {
        runners.forEach { (it as? Sortable)?.sort(sorter) }
    }

    private fun buildRunners(root: Class<*>): List<Runner> {
        val classes = buildList {
            add(root)
            collectNestedClassesRecursively(root, this)
        }
        return classes.mapNotNull(::createRunnerOrNull)
    }

    private fun collectNestedClassesRecursively(klass: Class<*>, out: MutableList<Class<*>>) {
        for (nested in klass.declaredClasses) {
            if (nested.isSynthetic) continue
            out.add(nested)
            collectNestedClassesRecursively(nested, out)
        }
    }

    private fun createRunnerOrNull(klass: Class<*>): Runner? {
        if (TestCase::class.java.isAssignableFrom(klass)) {
            return JUnit38ClassRunner(klass)
        }
        return null
    }
}


package org.cangnova.cangjie.test

import junit.framework.AssertionFailedError
import junit.framework.TestCase
import junit.framework.TestSuite
import org.junit.Ignore
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.api.parallel.Resources
import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.manipulation.Filter
import org.junit.runner.manipulation.NoTestsRemainException
import org.junit.runner.manipulation.Sorter
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener
import org.junit.runner.notification.RunNotifier

/**
 * 公共 runner 的行为契约；这里显式模拟 Gradle 逐类发现，不模拟 Gradle 的 glob 选择器。
 * 合成 fixture 带 Ignore，避免 Vintage 自动发现其中故意失败的方法；手工构造公共 runner 时仍真实执行它们。
 */
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class JUnit3RunnerWithInnersContractTest {
    @Test
    fun gradleDiscoveredOuterInnerAndLeafEachExecuteExactlyOnce() = withMode(jps = false) {
        val result = runRunners(
            JUnit3RunnerWithInners(GradleOuter::class.java),
            JUnit3RunnerWithInners(GradleOuter.Inner::class.java),
            JUnit3RunnerWithInners(GradleOuter.Inner.Leaf::class.java),
        )
        assertSuccessfulCases(listOf(
            id(GradleOuter::class.java, "testOuter"),
            id(GradleOuter.Inner::class.java, "testInner"),
            id(GradleOuter.Inner.Leaf::class.java, "testLeaf"),
        ), result)
    }

    @Test
    fun junit3LifecycleAndFailuresAreReportedWithoutBypassingTestSuite() = withMode(jps = false) {
        val result = runRunners(JUnit3RunnerWithInners(LifecycleFixture::class.java))
        val passing = id(LifecycleFixture::class.java, "testPass")
        val failing = id(LifecycleFixture::class.java, "testFailure")
        val setupFailure = id(LifecycleFixture::class.java, "testSetupFailure")
        assertExactlyOnce(listOf(passing, failing, setupFailure), result.started)
        assertExactlyOnce(listOf(passing, failing, setupFailure), result.finished)
        assertEquals(listOf("setUp", "body", "tearDown"), result.events.filter { it.caseId == passing }.map { it.phase })
        assertEquals(listOf("setUp", "body", "tearDown"), result.events.filter { it.caseId == failing }.map { it.phase })
        assertEquals(listOf("setUp"), result.events.filter { it.caseId == setupFailure }.map { it.phase })
        assertEquals(2, result.failures.size)
        val failures = result.failures.associateBy { descriptionId(it.description) }
        assertSame(expectedTestFailure, failures.getValue(failing).exception)
        assertSame(expectedSetupFailure, failures.getValue(setupFailure).exception)
    }

    @Test
    fun classFilterSelectsOnlyTheAlreadyDiscoveredInnerClass() = withMode(jps = false) {
        val selectedClass = SelectionOuter.Inner::class.java
        val filter = classFilter(selectedClass)
        val outer = JUnit3RunnerWithInners(SelectionOuter::class.java)
        assertThrows(NoTestsRemainException::class.java) { outer.filter(filter) }
        val inner = JUnit3RunnerWithInners(selectedClass)
        inner.filter(filter)
        assertSuccessfulCases(listOf(id(selectedClass, "testAlpha"), id(selectedClass, "testBeta")), runRunners(inner))
    }

    @Test
    fun methodFilterDoesNotExecuteOtherMethodsOrSiblingClasses() = withMode(jps = false) {
        val selectedClass = SelectionOuter.Inner::class.java
        val filter = Filter.matchMethodDescription(Description.createTestDescription(selectedClass, "testBeta"))
        val inner = JUnit3RunnerWithInners(selectedClass)
        inner.filter(filter)
        assertSuccessfulCases(listOf(id(selectedClass, "testBeta")), runRunners(inner))
    }

    @Test
    fun sortingPreservesJUnit38TestSuiteExecutionContract() = withMode(jps = false) {
        val sorter = Sorter { first, second -> second.methodName.orEmpty().compareTo(first.methodName.orEmpty()) }
        // JUnit 4.13.2 的 JUnit38 仅对 Sortable Test 转发排序；TestSuite 自身不实现该接口。
        // 用独立 JUnit38 执行作为契约，不能给仓库 runner 发明 TestSuite 不支持的倒序语义。
        val junitRunner = JUnit38ClassRunner(TestSuite(SortingFixture::class.java))
        junitRunner.sort(sorter)
        val junitResult = runRunners(junitRunner)
        lifecycleEvents.clear()
        val runner = JUnit3RunnerWithInners(SortingFixture::class.java)
        runner.sort(sorter)
        val expected = listOf("testAlpha", "testBeta", "testGamma").map { id(SortingFixture::class.java, it) }
        val result = runRunners(runner)
        assertSuccessfulCases(expected, junitResult)
        assertSuccessfulCases(expected, result)
        assertEquals(junitResult.started, result.started)
        assertEquals(junitResult.finished, result.finished)
        assertEquals(junitResult.events, result.events)
    }

    @Test
    fun gradleEmptyParentEmitsNoFakeLeafEvents() = withMode(jps = false) {
        val parent = JUnit3RunnerWithInners(EmptyGradleOuter::class.java)
        parent.description
        val empty = runRunners(parent)
        assertTrue(empty.started.isEmpty())
        assertTrue(empty.finished.isEmpty())
        assertTrue(empty.failures.isEmpty())
        assertTrue(empty.events.isEmpty())
        val child = JUnit3RunnerWithInners(EmptyGradleOuter.Inner::class.java)
        assertSuccessfulCases(listOf(id(EmptyGradleOuter.Inner::class.java, "testInner")), runRunners(child))
    }

    @Test
    fun jpsOuterOnlyRequestIncludesInheritedAndNestedTestCases() = withMode(jps = true) {
        val result = runRunners(JUnit3RunnerWithInners(JpsOuterOnly::class.java))
        assertSuccessfulCases(listOf(
            id(JpsOuterOnly::class.java, "testInherited"),
            id(JpsOuterOnly.Inner::class.java, "testInner"),
            id(JpsOuterOnly.Inner.Leaf::class.java, "testLeaf"),
        ), result)
    }

    @Test
    fun jpsPackageDiscoveryDoesNotRepeatIndividuallyRequestedDescendants() = withMode(jps = true) {
        // 全部构造完成后才查询 description/run，复现 IDEA package discovery 的请求集合。
        val runners = listOf(
            JUnit3RunnerWithInners(JpsPackageOuter::class.java),
            JUnit3RunnerWithInners(JpsPackageOuter.Inner::class.java),
            JUnit3RunnerWithInners(JpsPackageOuter.Inner.Leaf::class.java),
        )
        runners.forEach { it.description }
        assertSuccessfulCases(listOf(
            id(JpsPackageOuter::class.java, "testOuter"),
            id(JpsPackageOuter.Inner::class.java, "testInner"),
            id(JpsPackageOuter.Inner.Leaf::class.java, "testLeaf"),
        ), runRunners(*runners.toTypedArray()))
    }

    @Test
    fun jpsIndividuallyRequestedChildLeavesEmptyParentAsContainer() = withMode(jps = true) {
        val parent = JUnit3RunnerWithInners(EmptyJpsOuter::class.java)
        val child = JUnit3RunnerWithInners(EmptyJpsOuter.Inner::class.java)
        assertEquals(Description.EMPTY, parent.description)
        assertSuccessfulCases(listOf(id(EmptyJpsOuter.Inner::class.java, "testInner")), runRunners(parent, child))
    }

    private fun classFilter(selected: Class<*>): Filter = object : Filter() {
        override fun shouldRun(description: Description): Boolean =
            if (description.isTest) description.className == selected.name else description.children.any(::shouldRun)

        override fun describe(): String = "Only discovered class " + selected.name
    }

    private fun runRunners(vararg runners: Runner): Observation {
        val started = mutableListOf<String>()
        val finished = mutableListOf<String>()
        val failures = mutableListOf<Failure>()
        val notifier = RunNotifier()
        notifier.addListener(object : RunListener() {
            override fun testStarted(description: Description) { started += descriptionId(description) }
            override fun testFinished(description: Description) { finished += descriptionId(description) }
            override fun testFailure(failure: Failure) { failures += failure }
        })
        runners.forEach { it.run(notifier) }
        return Observation(started, finished, failures, lifecycleEvents.toList())
    }

    private fun assertSuccessfulCases(expected: List<String>, result: Observation) {
        assertTrue(result.failures.isEmpty(), result.failures.joinToString { it.trace })
        assertExactlyOnce(expected, result.started)
        assertExactlyOnce(expected, result.finished)
        assertExactlyOnce(expected, result.events.filter { it.phase == "body" }.map { it.caseId })
        for (caseId in expected) {
            assertEquals(listOf("setUp", "body", "tearDown"), result.events.filter { it.caseId == caseId }.map { it.phase })
        }
    }

    private fun assertExactlyOnce(expected: List<String>, actual: List<String>) {
        assertEquals(expected.size, actual.size)
        assertEquals(expected.associateWith { 1 }, actual.groupingBy { it }.eachCount())
    }

    /** 三套 JPS fixture 各使用独立 Class 身份，避免静态 requestedRunners 在测试之间污染断言。 */
    private fun withMode(jps: Boolean, action: () -> Unit) = synchronized(modeLock) {
        val previous = System.getProperty("use.jps")
        lifecycleEvents.clear()
        System.setProperty("use.jps", jps.toString())
        try {
            action()
        } finally {
            if (previous == null) System.clearProperty("use.jps") else System.setProperty("use.jps", previous)
            lifecycleEvents.clear()
        }
    }

    private data class Event(val caseId: String, val phase: String)
    private data class Observation(
        val started: List<String>, val finished: List<String>, val failures: List<Failure>, val events: List<Event>,
    )

    abstract class TracedCase : TestCase() {
        override fun setUp() { record(this, "setUp") }
        override fun tearDown() { record(this, "tearDown") }
        protected fun executeBody() { record(this, "body") }
    }

    @Ignore("Synthetic runner fixture; executed explicitly by contract tests")
    class GradleOuter : TracedCase() {
        fun testOuter() { executeBody() }
        @Ignore("Synthetic runner fixture")
        class Inner : TracedCase() {
            fun testInner() { executeBody() }
            @Ignore("Synthetic runner fixture")
            class Leaf : TracedCase() { fun testLeaf() { executeBody() } }
        }
    }

    @Ignore("Synthetic fixture includes intentional failures; contract test checks their events")
    class LifecycleFixture : TracedCase() {
        override fun setUp() {
            super.setUp()
            if (name == "testSetupFailure") throw expectedSetupFailure
        }
        fun testPass() { executeBody() }
        fun testFailure() { executeBody(); throw expectedTestFailure }
        fun testSetupFailure() { executeBody() }
    }

    @Ignore("Synthetic runner fixture")
    class SelectionOuter : TracedCase() {
        fun testOuter() { executeBody() }
        @Ignore("Synthetic runner fixture")
        class Inner : TracedCase() {
            fun testAlpha() { executeBody() }
            fun testBeta() { executeBody() }
        }
    }

    @Ignore("Synthetic runner fixture")
    class SortingFixture : TracedCase() {
        fun testAlpha() { executeBody() }
        fun testBeta() { executeBody() }
        fun testGamma() { executeBody() }
    }

    @Ignore("Synthetic empty container")
    class EmptyGradleOuter : TracedCase() {
        @Ignore("Synthetic runner fixture")
        class Inner : TracedCase() { fun testInner() { executeBody() } }
    }

    abstract class InheritedCase : TracedCase() { fun testInherited() { executeBody() } }

    @Ignore("Synthetic JPS runner fixture")
    class JpsOuterOnly : InheritedCase() {
        @Ignore("Synthetic runner fixture")
        class Inner : TracedCase() {
            fun testInner() { executeBody() }
            @Ignore("Synthetic runner fixture")
            class Leaf : TracedCase() { fun testLeaf() { executeBody() } }
        }
    }

    @Ignore("Synthetic JPS package fixture")
    class JpsPackageOuter : TracedCase() {
        fun testOuter() { executeBody() }
        @Ignore("Synthetic runner fixture")
        class Inner : TracedCase() {
            fun testInner() { executeBody() }
            @Ignore("Synthetic runner fixture")
            class Leaf : TracedCase() { fun testLeaf() { executeBody() } }
        }
    }

    @Ignore("Synthetic empty JPS container")
    class EmptyJpsOuter : TracedCase() {
        @Ignore("Synthetic runner fixture")
        class Inner : TracedCase() { fun testInner() { executeBody() } }
    }

    companion object {
        private val modeLock = Any()
        private val lifecycleEvents = mutableListOf<Event>()
        private val expectedTestFailure = AssertionFailedError("intentional testcase failure")
        private val expectedSetupFailure = IllegalStateException("intentional setup failure")
        private fun id(type: Class<*>, method: String): String = type.name + "#" + method
        private fun descriptionId(description: Description): String = description.className + "#" + description.methodName
        private fun record(test: TestCase, phase: String) { lifecycleEvents += Event(id(test.javaClass, test.name), phase) }
    }
}

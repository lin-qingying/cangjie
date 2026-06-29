package org.cangnova.cangjie.analysis.api.impl.base.test.cases.sessions

import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.platform.modification.KotlinModificationEventKind
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryFallbackDependenciesModule
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.directives.ModificationEventDirectives
import org.cangnova.cangjie.analysis.test.framework.directives.publishWildcardModificationEventsByDirective
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * session invalidation 测试的 shared 基类。
 *
 * 该基类与 Kotlin `AbstractSessionInvalidationTest` 保持同一职责边界：
 * - 复用同一份 testData；
 * - 通过不同修改事件种类触发失效；
 * - 比较 invalidated / untouched session 的可见行为。
 */
abstract class AbstractSessionInvalidationTest<S> : AbstractAnalysisApiBasedTest() {
    /**
     * 当前失效测试额外注册的修改事件指令。
     *
     * testData 通过这些指令描述要向测试模块结构发布哪些 wildcard modification events。
     */
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + ModificationEventDirectives

    /**
     * 当前具体测试要发布的 modification event 种类。
     *
     * 子类通过覆盖该属性选择模块级、全局级、source 级或 code fragment 级失效路径。
     */
    protected abstract val modificationEventKind: KotlinModificationEventKind

    /**
     * 当前测试输出使用的 golden 子目录名。
     *
     * 不同 session 类型可以复用同一套 testData，但输出结果应分目录保存。
     */
    protected abstract val testOutputSubdirectoryName: String

    /**
     * 为指定测试模块获取要纳入失效检查的 session 列表。
     *
     * 具体 session 类型由子类决定，基类只依赖统一的 `TestSession` 包装模型。
     */
    protected abstract fun getSessions(cjTestModule: CjTestModule): List<TestSession<S>>

    /**
     * 判断指定失效 session 是否需要跳过有效性标记检查。
     *
     * 默认所有失效 session 都必须被标记为无效；特殊 session 类型可以覆盖该规则。
     */
    protected open fun shouldSkipValidityCheck(session: TestSession<S>): Boolean = false

    /**
     * 执行完整的 session invalidation 测试流程。
     *
     * 流程包括创建修改前 session、触发 fallback dependency session、发布事件、重新收集 session，
     * 并分别断言失效集合、失效标记和未触碰 session 仍有效。
     */
    override fun doTest(testServices: TestServices) {
        val testModules = testServices.cjTestModuleStructure.mainModules

        val sessionsBeforeModification = getAllSessions(testModules)
        ensureFallbackDependencySessionsExist(testModules)
        checkSessionValidityBeforeModification(sessionsBeforeModification, testServices)

        testServices.cjTestModuleStructure.publishWildcardModificationEventsByDirective(modificationEventKind)
        val sessionsAfterModification = getAllSessions(testModules)

        val invalidatedSessions = buildSet {
            addAll(sessionsBeforeModification)
            removeAll(sessionsAfterModification)
        }

        checkInvalidatedSessions(invalidatedSessions, testServices)
        checkSessionsMarkedInvalid(invalidatedSessions, testServices)

        val untouchedSessions = sessionsBeforeModification.intersect(sessionsAfterModification)
        checkUntouchedSessionValidity(untouchedSessions, testServices)
    }

    /**
     * 收集所有主测试模块对应的 session。
     *
     * 该 helper 把模块维度展开为单一列表，供修改事件前后做集合比较。
     */
    private fun getAllSessions(testModules: List<CjTestModule>): List<TestSession<S>> {
        return testModules.flatMap(::getSessions)
    }

    /**
     * fallback dependencies 模块不会作为普通测试模块物化出来，
     * 因此这里通过库模块上的一次符号查询显式触发依赖 session 创建，
     * 保证失效测试能观察到这条链路。
     */
    private fun ensureFallbackDependencySessionsExist(
        testModules: List<CjTestModule>,
    ) {
        testModules.forEach { testModule ->
            val useSiteModule = testModule.caModule
            if (useSiteModule.directRegularDependencies.none { it is CaLibraryFallbackDependenciesModule }) {
                return@forEach
            }

            analyze(useSiteModule) {
                getClassLikeSymbol(ClassId.topLevel(FqName.topLevel(Name.identifier("IDontExistAtAll"))))
            }
        }
    }

    /**
     * 将失效 session 集合写入当前事件对应的 golden 文件。
     *
     * 输出按描述去重并排序，保证结果稳定且不依赖 session 创建顺序。
     */
    private fun checkInvalidatedSessions(
        invalidatedSessions: Set<TestSession<S>>,
        testServices: TestServices,
    ) {
        val invalidatedDescriptions = invalidatedSessions
            .map { session -> session.description }
            .distinct()
            .sorted()

        val actual = buildString {
            appendLine("Invalidated sessions:")
            invalidatedDescriptions.forEach(::appendLine)
        }

        testServices.assertions.assertEqualsToTestOutputFile(
            actual = actual,
            extension = ".${modificationEventKind.name.lowercase()}.txt",
            subdirectoryName = testOutputSubdirectoryName,
        )
    }

    /**
     * 检查修改事件发布前所有 session 都处于有效状态。
     *
     * 这是后续失效断言的前置条件，避免用已经无效的 session 推导事件效果。
     */
    private fun checkSessionValidityBeforeModification(
        sessions: List<TestSession<S>>,
        testServices: TestServices,
    ) {
        sessions.forEach { session ->
            testServices.assertions.assertTrue(session.isValid) {
                "Session `${session.description}` should be valid before invalidation."
            }
        }
    }

    /**
     * 检查失效集合中的 session 在事件后被标记为无效。
     *
     * 子类可以通过 `shouldSkipValidityCheck` 跳过特殊模块或特殊 session 的有效性标记断言。
     */
    private fun checkSessionsMarkedInvalid(
        invalidatedSessions: Set<TestSession<S>>,
        testServices: TestServices,
    ) {
        invalidatedSessions.forEach { session ->
            if (shouldSkipValidityCheck(session)) return@forEach

            testServices.assertions.assertFalse(session.isValid) {
                "Invalidated session `${session.description}` should have been marked invalid."
            }
        }
    }

    /**
     * 检查未被当前 modification event 影响的 session 仍保持有效。
     *
     * 该断言用于发现失效范围过宽导致的过度 invalidation。
     */
    private fun checkUntouchedSessionValidity(
        sessions: Set<TestSession<S>>,
        testServices: TestServices,
    ) {
        sessions.forEach { session ->
            testServices.assertions.assertTrue(session.isValid) {
                "Untouched session `${session.description}` should still be valid."
            }
        }
    }

    /**
     * session invalidation 测试共享的输出目录常量。
     *
     * 生成器在排除旧输出目录时使用该列表。
     */
    companion object {
        /**
         * 所有 session invalidation 测试可能产生的 golden 输出目录名。
         */
        val TEST_OUTPUT_DIRECTORY_NAMES = listOf("analysisSession", "cfirSession")
    }
}

/**
 * session invalidation 测试使用的统一 session 包装模型。
 *
 * 该抽象类把测试模块、底层 session、有效性状态和可读描述统一成基类可处理的形态。
 */
abstract class TestSession<S> {
    /**
     * session 所属的仓颉测试模块。
     */
    abstract val cjTestModule: CjTestModule
    /**
     * 被测试的实际 session 对象。
     */
    abstract val underlyingSession: S
    /**
     * 当前 session 是否仍有效。
     */
    abstract val isValid: Boolean
    /**
     * 当前 session 在 golden 输出中的稳定描述。
     */
    abstract val description: String

    /**
     * 按底层 session 对象身份比较包装模型。
     *
     * 失效测试需要比较修改事件前后 session 是否为同一个底层对象。
     */
    override fun equals(other: Any?): Boolean =
        this === other || other is TestSession<*> && underlyingSession == other.underlyingSession

    /**
     * 返回底层 session 的 hash code。
     *
     * 该实现与 `equals` 保持一致，使 session 包装可以稳定参与集合差集计算。
     */
    override fun hashCode(): Int = underlyingSession?.hashCode() ?: 0
}

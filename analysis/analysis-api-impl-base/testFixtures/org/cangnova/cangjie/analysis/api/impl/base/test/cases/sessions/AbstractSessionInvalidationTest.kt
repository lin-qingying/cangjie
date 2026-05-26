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
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + ModificationEventDirectives

    protected abstract val modificationEventKind: KotlinModificationEventKind

    protected abstract val testOutputSubdirectoryName: String

    protected abstract fun getSessions(cjTestModule: CjTestModule): List<TestSession<S>>

    protected open fun shouldSkipValidityCheck(session: TestSession<S>): Boolean = false

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

    companion object {
        val TEST_OUTPUT_DIRECTORY_NAMES = listOf("analysisSession", "cfirSession")
    }
}

abstract class TestSession<S> {
    abstract val cjTestModule: CjTestModule
    abstract val underlyingSession: S
    abstract val isValid: Boolean
    abstract val description: String

    override fun equals(other: Any?): Boolean =
        this === other || other is TestSession<*> && underlyingSession == other.underlyingSession

    override fun hashCode(): Int = underlyingSession?.hashCode() ?: 0
}

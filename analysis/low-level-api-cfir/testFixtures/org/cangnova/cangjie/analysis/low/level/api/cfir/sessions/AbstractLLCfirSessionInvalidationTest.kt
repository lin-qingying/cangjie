@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import org.cangnova.cangjie.analysis.api.impl.base.test.cases.sessions.AbstractSessionInvalidationTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.sessions.TestSession
import org.cangnova.cangjie.analysis.api.platform.modification.KotlinModificationEventKind
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator

/**
 * 对齐 Kotlin `AbstractLLFirSessionInvalidationTest` 的 low-level session 失效测试。
 *
 * 这里覆盖的是 `LLCfirSessionCache` 持有的 session 实例，而不是上层 analysis session。
 */
abstract class AbstractLLCfirSessionInvalidationTest : AbstractSessionInvalidationTest<LLCfirSession>() {
    override val testOutputSubdirectoryName: String
        get() = "cfirSession"

    override fun getSessions(cjTestModule: CjTestModule): List<TestSession<LLCfirSession>> {
        val cache = LLCfirSessionCache.getInstance(cjTestModule.caModule.project)
        return cjTestModule.allCaModules.map { module ->
            LLCfirTestSession(
                cjTestModule = cjTestModule,
                underlyingSession = cache.getSession(module, preferBinary = module is CaLibraryModule),
            )
        }
    }

    override val configurator: AnalysisApiTestConfigurator =
        analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)
}

internal class LLCfirTestSession(
    override val cjTestModule: CjTestModule,
    override val underlyingSession: LLCfirSession,
) : TestSession<LLCfirSession>() {
    override val isValid: Boolean
        get() = underlyingSession.isValid

    override val description: String
        get() = buildString {
            append(underlyingSession.caModule)
            if (underlyingSession.caModule is CaLibraryModule) {
                append(" (binary session)")
            }
        }
}

abstract class AbstractModuleStateModificationLLCfirSessionInvalidationTest : AbstractLLCfirSessionInvalidationTest() {
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.MODULE_STATE_MODIFICATION
}

abstract class AbstractModuleOutOfBlockModificationLLCfirSessionInvalidationTest : AbstractLLCfirSessionInvalidationTest() {
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.MODULE_OUT_OF_BLOCK_MODIFICATION
}

abstract class AbstractGlobalModuleStateModificationLLCfirSessionInvalidationTest : AbstractLLCfirSessionInvalidationTest() {
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.GLOBAL_MODULE_STATE_MODIFICATION
}

abstract class AbstractGlobalSourceModuleStateModificationLLCfirSessionInvalidationTest : AbstractLLCfirSessionInvalidationTest() {
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.GLOBAL_SOURCE_MODULE_STATE_MODIFICATION
}

abstract class AbstractGlobalSourceOutOfBlockModificationLLCfirSessionInvalidationTest : AbstractLLCfirSessionInvalidationTest() {
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.GLOBAL_SOURCE_OUT_OF_BLOCK_MODIFICATION
}

abstract class AbstractCodeFragmentContextModificationLLCfirSessionInvalidationTest : AbstractLLCfirSessionInvalidationTest() {
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.CODE_FRAGMENT_CONTEXT_MODIFICATION
}

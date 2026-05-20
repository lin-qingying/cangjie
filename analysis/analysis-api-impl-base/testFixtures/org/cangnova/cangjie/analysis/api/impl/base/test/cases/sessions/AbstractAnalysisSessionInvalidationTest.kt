package org.cangnova.cangjie.analysis.api.impl.base.test.cases.sessions

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.lifetime.isValid
import org.cangnova.cangjie.analysis.api.platform.modification.KotlinModificationEventKind
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibrarySourceModule
import org.cangnova.cangjie.analysis.api.session.CaSessionProvider
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule

abstract class AbstractAnalysisSessionInvalidationTest : AbstractSessionInvalidationTest<CaSession>() {
    override val testOutputSubdirectoryName: String
        get() = "analysisSession"

    override fun getSessions(cjTestModule: CjTestModule): List<TestSession<CaSession>> {
        val sessionProvider = CaSessionProvider.getInstance(cjTestModule.caModule.project)
        return listOf(AnalysisTestSession(cjTestModule, sessionProvider.getAnalysisSession(cjTestModule.caModule)))
    }

    override fun shouldSkipValidityCheck(session: TestSession<CaSession>): Boolean {
        return when (modificationEventKind) {
            KotlinModificationEventKind.GLOBAL_SOURCE_MODULE_STATE_MODIFICATION,
            KotlinModificationEventKind.GLOBAL_SOURCE_OUT_OF_BLOCK_MODIFICATION -> {
                val useSiteModule = session.underlyingSession.useSiteModule
                useSiteModule is CaLibraryModule || useSiteModule is CaLibrarySourceModule
            }

            else -> false
        }
    }
}

internal class AnalysisTestSession(
    override val cjTestModule: CjTestModule,
    override val underlyingSession: CaSession,
) : TestSession<CaSession>() {
    override val isValid: Boolean
        get() = underlyingSession.isValid()

    override val description: String
        get() = buildString {
            append(underlyingSession.useSiteModule.moduleDescription)
            if (underlyingSession.useSiteModule is CaLibraryModule) {
                append(" (resolvable session)")
            }
        }
}

abstract class AbstractModuleStateModificationAnalysisSessionInvalidationTest : AbstractAnalysisSessionInvalidationTest() {
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.MODULE_STATE_MODIFICATION
}

abstract class AbstractModuleOutOfBlockModificationAnalysisSessionInvalidationTest : AbstractAnalysisSessionInvalidationTest() {
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.MODULE_OUT_OF_BLOCK_MODIFICATION
}

abstract class AbstractGlobalModuleStateModificationAnalysisSessionInvalidationTest : AbstractAnalysisSessionInvalidationTest() {
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.GLOBAL_MODULE_STATE_MODIFICATION
}

abstract class AbstractGlobalSourceModuleStateModificationAnalysisSessionInvalidationTest : AbstractAnalysisSessionInvalidationTest() {
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.GLOBAL_SOURCE_MODULE_STATE_MODIFICATION
}

abstract class AbstractGlobalSourceOutOfBlockModificationAnalysisSessionInvalidationTest : AbstractAnalysisSessionInvalidationTest() {
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.GLOBAL_SOURCE_OUT_OF_BLOCK_MODIFICATION
}

abstract class AbstractCodeFragmentContextModificationAnalysisSessionInvalidationTest : AbstractAnalysisSessionInvalidationTest() {
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.CODE_FRAGMENT_CONTEXT_MODIFICATION
}

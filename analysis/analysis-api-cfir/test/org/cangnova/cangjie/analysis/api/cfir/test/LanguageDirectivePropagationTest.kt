@file:OptIn(org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals::class)

package org.cangnova.cangjie.analysis.api.cfir.test

import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.test.configurators.CaCfirAnalysisApiTestConfiguratorFactory
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** 文件指令必须传到模块与实际 LL session；显式关闭与各模块的作用域同样保留。 */
class LanguageDirectivePropagationTest : AbstractAnalysisApiExecutionTest("analysis/analysis-api-cfir/testData/languageDirectivePropagation") {
    override val configurator = CaCfirAnalysisApiTestConfiguratorFactory.createConfigurator(
        AnalysisApiTestConfiguratorFactoryData(FrontendKind.Cfir, TestModuleKind.Source,
            AnalysisSessionMode.Normal, AnalysisApiMode.Ide),
    )

    @Test
    fun explicitEnable(mainFile: CjFile) {
        assertSettings(mainFile, LanguageFeature.State.ENABLED)
    }

    @Test
    fun explicitDisable(mainFile: CjFile) {
        assertSettings(mainFile, LanguageFeature.State.DISABLED)
    }

    @Test
    fun moduleIsolation(mainFile: CjFile, mainModule: CjTestModule, services: TestServices) {
        val modules = services.cjTestModuleStructure
        assertSame(modules.getModule("main"), mainModule)
        assertSettings(mainFile, LanguageFeature.State.DISABLED)
        val base = modules.getModule("base")
        assertSettings(base.psiFiles.filterIsInstance<CjFile>().single(), LanguageFeature.State.ENABLED)
    }

    private fun assertSettings(file: CjFile, state: LanguageFeature.State) {
        analyzeForTest(file) {
            val settings = (useSiteModule as CaSourceModule).languageVersionSettings
            assertEquals(LanguageVersion.CANGJIE_1_1_3, settings.languageVersion)
            assertEquals(state, settings.getCustomizedLanguageFeatures()[LanguageFeature.EffectHandlers])
            assertEquals(state, settings.getFeatureSupport(LanguageFeature.EffectHandlers))
            assertEquals(state == LanguageFeature.State.ENABLED, settings.supportsFeature(LanguageFeature.EffectHandlers))
            val actual = (this as CaCfirSession).resolutionFacade.useSiteCfirSession.languageVersionSettings
            assertEquals(settings.languageVersion, actual.languageVersion)
            assertEquals(state, actual.getCustomizedLanguageFeatures()[LanguageFeature.EffectHandlers])
            assertEquals(state, actual.getFeatureSupport(LanguageFeature.EffectHandlers))
        }
    }
}

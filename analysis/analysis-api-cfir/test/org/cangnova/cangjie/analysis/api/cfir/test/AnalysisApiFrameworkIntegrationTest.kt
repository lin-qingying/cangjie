package org.cangnova.cangjie.analysis.api.cfir.test

import org.cangnova.cangjie.analysis.api.CaTargetPlatform
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.CaCfirAnalysisApiTestConfiguratorFactory
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * 锁定 Analysis API 测试框架的模块级入口行为。
 */
class AnalysisApiFrameworkIntegrationTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/framework",
) {
    override val configurator: AnalysisApiTestConfigurator =
        CaCfirAnalysisApiTestConfiguratorFactory.createConfigurator(
            AnalysisApiTestConfiguratorFactoryData(
                frontend = FrontendKind.Cfir,
                moduleKind = TestModuleKind.Source,
                analysisSessionMode = AnalysisSessionMode.Normal,
                analysisApiMode = AnalysisApiMode.Standalone,
            ),
        )

    @Test
    fun moduleKindDirective(mainFile: CjFile, mainModule: CjTestModule) {
        assertEquals(TestModuleKind.NotUnderContentRoot, mainModule.moduleKind)
        assertEquals(CaTargetPlatform.STANDALONE, mainModule.caModule.targetPlatform)
        assertEquals("moduleKindDirective.cj", mainFile.name)
    }

    @Test
    fun selectMainModule(mainFile: CjFile, mainModule: CjTestModule) {
        assertEquals("app", mainModule.name)
        assertEquals("entry.cj", mainFile.name)
    }
}

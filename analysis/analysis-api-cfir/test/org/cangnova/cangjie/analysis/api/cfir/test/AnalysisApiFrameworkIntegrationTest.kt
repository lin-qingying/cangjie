package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
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
import org.cangnova.cangjie.psi.CangJieReferenceProvidersService
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * Analysis API 测试框架集成测试。
 *
 * 这组测试验证的是框架装配本身，而不是某个单独 API 的业务语义：
 * 1. configurator factory 能产出可用测试宿主；
 * 2. `.cjs` 脚本文件会被纳入模块图与 PSI 装配；
 * 3. MAIN_MODULE / MAIN_FILE_NAME / MODULE_KIND 等框架级指令能真实影响测试入口。
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
            )
        )

    @Test
    fun scriptReference(mainFile: CjFile, mainModule: CjTestModule) {
        assertEquals(TestModuleKind.ScriptSource, mainModule.moduleKind)
        assertEquals(CaTargetPlatform.STANDALONE, mainModule.caModule.targetPlatform)

        val referenceExpression = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
            .last { it.referencedName == "greet" }

        val references = CangJieReferenceProvidersService.getReferencesFromProviders(referenceExpression)
        val resolvedDeclaration = references.singleOrNull()?.resolve() as? CjNamedDeclaration

        assertNotNull(resolvedDeclaration, "脚本文件中的 simple-name 引用没有解析到声明。")
        assertEquals("greet", resolvedDeclaration?.name)
    }

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

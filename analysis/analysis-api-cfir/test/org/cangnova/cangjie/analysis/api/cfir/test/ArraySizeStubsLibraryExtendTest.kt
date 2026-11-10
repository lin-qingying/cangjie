package org.cangnova.cangjie.analysis.api.cfir.test

import org.cangnova.cangjie.analysis.api.cfir.test.configurators.CaCfirAnalysisApiTestConfiguratorFactory
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * STUBS 链路：库声明的 extend（`extend Box <: Sized`）提供的成员必须可见。
 *
 * 该用例覆盖 `LLCfirAbstractSessionFactory` 里 `createLibraryExtendProvider(...)` 分支：
 * library 依赖的 extend 只能从 `.cjo` 反序列化来源补齐（stub 摘要索引丢弃 extend）。
 */
class ArraySizeStubsLibraryExtendTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/diagnostics",
) {
    override val configurator = CaCfirAnalysisApiTestConfiguratorFactory.createConfigurator(
        AnalysisApiTestConfiguratorFactoryData(
            frontend = FrontendKind.Cfir,
            moduleKind = TestModuleKind.Source,
            analysisSessionMode = AnalysisSessionMode.Normal,
            analysisApiMode = AnalysisApiMode.Ide,
        ),
    )

    @Test
    fun arraySizeStubsLibraryExtend(mainFile: CjFile) {
        assertEquals(
            org.cangnova.cangjie.analysis.api.platform.CaDeserializedDeclarationsOrigin.STUBS,
            org.cangnova.cangjie.analysis.api.platform.CaPlatformSettings.getInstance(mainFile.project)
                .deserializedDeclarationsOrigin,
        )
        val (resolved, diagnostics) = analyzeForTest(mainFile) {
            val diagnostics = mainFile.collectDiagnostics(
                org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS,
            ).map { diagnostic -> diagnostic.factoryName }
            val sizeExpr = com.intellij.psi.util.PsiTreeUtil
                .findChildrenOfType(mainFile, org.cangnova.cangjie.psi.CjSimpleNameExpression::class.java)
                .firstOrNull { expression ->
                    expression.text == "size" && expression.parent?.text?.startsWith("box.size") == true
                }
            org.junit.jupiter.api.Assertions.assertNotNull(sizeExpr, "fixture 必须包含 `box.size` 访问")
            sizeExpr!!.resolveToSymbols().map { symbol -> symbol::class.simpleName ?: "?" } to diagnostics
        }
        assertTrue(diagnostics.isEmpty(), "库 extend 成员的访问不应产生诊断：$diagnostics")
        assertTrue(resolved.any { name -> name.contains("Property") }, "库 extend 的 size 必须解析为属性符号：$resolved")
    }
}

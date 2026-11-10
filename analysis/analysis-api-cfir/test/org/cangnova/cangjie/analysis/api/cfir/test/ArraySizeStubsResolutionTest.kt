package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.cfir.test.configurators.CaCfirAnalysisApiTestConfiguratorFactory
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * STUBS 链路：`Array<Byte>.size` 必须解析到属性符号。
 *
 * 使用 `CaCfirAnalysisApiTestConfiguratorFactory`（IndexingConfiguration=INDEX_STUBS），
 * 与 `AnalysisApiCfirDiagnosticsTest`（standalone，固定 BINARIES）互为对照。
 */
class ArraySizeStubsResolutionTest : AbstractAnalysisApiExecutionTest(
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
    fun arraySizeStubsResolution(mainFile: CjFile) {
        assertEquals(
            org.cangnova.cangjie.analysis.api.platform.CaDeserializedDeclarationsOrigin.STUBS,
            org.cangnova.cangjie.analysis.api.platform.CaPlatformSettings.getInstance(mainFile.project)
                .deserializedDeclarationsOrigin,
            "该用例必须运行在 STUBS 配置下",
        )
        val (resolved, diagnostics) = analyzeForTest(mainFile) {
            val diagnostics = mainFile.collectDiagnostics(
                org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS,
            ).map { diagnostic -> diagnostic.factoryName }
            // 精确选 `bytes.size`（extend 成员），避开 `asCollection.size`（接口自身声明）。
            val sizeExpr = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
                .firstOrNull { expression ->
                    expression.text == "size" && expression.parent?.text?.startsWith("bytes.size") == true
                }
            assertNotNull(sizeExpr, "fixture 必须包含 `bytes.size` 访问")
            // 符号对象不能逃出 analyze 生命周期，这里只带回稳定字符串。
            sizeExpr!!.resolveToSymbols().map { symbol -> (symbol::class.simpleName ?: "?") + "|psi=" + (symbol.psi != null) } to diagnostics
        }
        assertTrue(diagnostics.isEmpty(), "STUBS 下 Array/Collection 的 size 访问不应产生诊断：$diagnostics")
        assertTrue(resolved.isNotEmpty(), "`size` 必须解析到符号（STUBS 下 builtins/库 extend 必须可见）")
        assertTrue(
            resolved.any { name -> name.contains("Property") },
            "STUBS 下 `size` 必须解析为属性符号，实际：$resolved",
        )
        assertTrue(
            resolved.any { name -> name.contains("Property") && name.contains("psi=true") },
            "STUBS 下 extend 成员必须带非空 PSI（快速文档/导航依赖它），实际：$resolved",
        )

    }
}

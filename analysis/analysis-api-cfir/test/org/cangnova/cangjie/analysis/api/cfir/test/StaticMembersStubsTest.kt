package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.cfir.test.configurators.CaCfirAnalysisApiTestConfiguratorFactory
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.platform.CaDeserializedDeclarationsOrigin
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * STUBS 链路：库声明中的 static 函数、static 字段与 enum entry 必须可解析且不产生诊断。
 *
 * 对齐 binary 反序列化：static 成员没有 dispatch receiver，owner 必须显式记录在
 * `containingClassForStaticMemberAttr`；enum constructor 同理。
 */
class StaticMembersStubsTest : AbstractOriginDiagnosticsTest(
    "analysis/analysis-api-cfir/testData/diagnostics",
    CaDeserializedDeclarationsOrigin.STUBS,
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
    fun staticMembersStubs(mainFile: CjFile) {
        assertExpectedOrigin(mainFile)

        val (resolvedByName, diagnostics) = analyzeForTest(mainFile) {
            val diagnostics = mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
                .map { diagnostic -> diagnostic.factoryName }
            val members = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
                .filter { expression -> expression.text in setOf("bump", "total", "tag", "Red") }
                .groupBy(
                    keySelector = { expression -> expression.text },
                    valueTransform = { expression ->
                        expression.resolveToSymbols().map { symbol -> symbol::class.simpleName ?: "?" }
                    },
                )
            members to diagnostics
        }

        fun symbolsOf(name: String): List<String> = resolvedByName[name].orEmpty().flatten()

        assertTrue(diagnostics.isEmpty(), "STUBS 下库 static 成员与 enum entry 不应产生诊断：$diagnostics")
        assertTrue(
            symbolsOf("bump").any { it.contains("Function") },
            "`Counter.bump` 必须解析为函数符号：${symbolsOf("bump")}",
        )
        assertTrue(
            symbolsOf("total").any { it.contains("Field") || it.contains("Property") || it.contains("Variable") },
            "`Counter.total` 必须解析为字段/属性符号：${symbolsOf("total")}",
        )
        assertTrue(
            symbolsOf("tag").any { it.contains("Field") || it.contains("Property") || it.contains("Variable") },
            "`Counter.tag` 必须解析为字段/属性符号：${symbolsOf("tag")}",
        )
        assertTrue(
            symbolsOf("Red").any { it.contains("Enum") || it.contains("Class") },
            "`Color.Red` 必须解析为 enum entry 符号：${symbolsOf("Red")}",
        )
    }
}

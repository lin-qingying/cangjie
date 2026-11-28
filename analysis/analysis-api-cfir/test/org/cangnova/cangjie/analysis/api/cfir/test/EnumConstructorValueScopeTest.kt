package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.cfir.test.configurators.CaCfirAnalysisApiTestConfiguratorFactory
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjMatchExpression
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 枚举静态方法的 match 分支结果应在枚举值作用域中解析裸 constructor 名称。
 */
class EnumConstructorValueScopeTest : AbstractAnalysisApiExecutionTest(
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
    fun enumConstructorValueScope(mainFile: CjFile) {
        analyzeForTest(mainFile) {
            val matchExpression = PsiTreeUtil.findChildrenOfType(mainFile, CjMatchExpression::class.java).single()
            val stringReference = matchExpression.entries
                .asSequence()
                .mapNotNull { entry -> entry.expression }
                .flatMap { branchResult ->
                    PsiTreeUtil.findChildrenOfType(branchResult, CjSimpleNameExpression::class.java).asSequence()
                }
                .single { expression -> expression.referencedName == "String" }
            val resolvedSymbol = stringReference.resolveToSymbols().singleOrNull()

            assertTrue(
                resolvedSymbol is CaEnumConstructorSymbol,
                "The match branch result `String` must resolve to one enum constructor symbol; got $resolvedSymbol",
            )
            val enumConstructor = resolvedSymbol as CaEnumConstructorSymbol
            assertEquals(
                "ColumnType",
                enumConstructor.containingEnumClassId?.shortClassName?.asString(),
                "The `String` enum constructor must be owned by ColumnType",
            )
        }
    }
}

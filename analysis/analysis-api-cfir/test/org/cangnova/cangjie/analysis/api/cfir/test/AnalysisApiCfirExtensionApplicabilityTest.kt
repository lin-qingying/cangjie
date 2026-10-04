package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.completion.CaExtensionApplicabilityResult
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * extend 候选适用性检查回归。
 *
 * 验证适用性判定复用带约束的 compiler extend 匹配：目标类型匹配才能成立，
 * 约束不成立与目标不匹配必须区分，无法判定时返回未知而不是否定结论。
 *
 * PSI 到公开符号的转换必须在 `analyzeForTest` 块内完成：这些入口由会话组件提供，
 * 块外拿不到组件接收者。
 */
@OptIn(CaExperimentalApi::class)
class AnalysisApiCfirExtensionApplicabilityTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/components/extensionApplicability",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /** 完全实例化 extend 对其目标类型必须适用，并携带有效替换器。 */
    @Test
    fun applicableExtendForMatchedReceiver(mainFile: CjFile) {
        val extendPsi = mainFile.onlyExtend()
        val useSite = mainFile.useSiteOf("use")

        analyzeForTest(mainFile) {
            val receiver = mainFile.namedClass("Box").classSymbol.defaultType
            val result = checkExtensionApplicability(extendPsi.symbol, receiver, useSite)

            assertTrue(result.isApplicable, "目标类型匹配的 extend 必须适用，实际 $result")
            val applicable = result as CaExtensionApplicabilityResult.Applicable
            assertTrue(applicable.substitutor.token.isValid(), "适用结论必须携带有效的替换器")
        }
    }

    /** 泛型 extend 在接收者实际类型确定后仍然适用。 */
    @Test
    fun applicableGenericExtendAfterSubstitution(mainFile: CjFile) {
        val extendPsi = mainFile.onlyExtend()
        val useSite = mainFile.useSiteOf("use")

        analyzeForTest(mainFile) {
            val receiver = mainFile.namedClass("Holder").classSymbol.defaultType
            val result = checkExtensionApplicability(extendPsi.symbol, receiver, useSite)
            assertTrue(result.isApplicable, "泛型 extend 在实参确定后必须适用，实际 $result")
        }
    }

    /** 目标类型不匹配的 extend 必须给出确定的否定结论，而不是未知。 */
    @Test
    fun receiverMismatchForOtherExtend(mainFile: CjFile) {
        val extendPsi = mainFile.onlyExtend()
        val useSite = mainFile.useSiteOf("use")

        analyzeForTest(mainFile) {
            val receiver = mainFile.namedClass("Crate").classSymbol.defaultType
            val result = checkExtensionApplicability(extendPsi.symbol, receiver, useSite)
            assertEquals(
                CaExtensionApplicabilityResult.ReceiverMismatch::class,
                result::class,
                "目标类型不匹配时必须给出确定的否定结论，实际 $result",
            )
        }
    }

    /** 无法取得确定的 extend 符号时只能返回未知，且不得影响适用性判定本身。 */
    @Test
    fun unknownForUnresolvedReceiver(mainFile: CjFile) {
        val extendPsi = mainFile.onlyExtend()
        val boxPsi = mainFile.namedClass("Box")
        val useSite = mainFile.useSiteOf("use")

        analyzeForTest(mainFile) {
            val receiver = boxPsi.classSymbol.defaultType
            assertTrue(
                checkExtensionApplicability(boxPsi.classSymbol, receiver, useSite) is CaExtensionApplicabilityResult.Unknown,
                "非 extend 符号必须返回未知",
            )
            assertTrue(
                checkExtensionApplicability(extendPsi.symbol, receiver, useSite).isApplicable,
                "使用点存在其它未解析标识符不得影响 extend 适用性",
            )
        }
    }
}

/**
 * 取得夹具中唯一的 extend 声明。
 */
private fun CjFile.onlyExtend(): CjExtend =
    PsiTreeUtil.findChildrenOfType(this, CjExtend::class.java).single()

/**
 * 取得按名类声明。
 */
private fun CjFile.namedClass(name: String): CjClass =
    PsiTreeUtil.findChildrenOfType(this, CjClass::class.java).single { it.name == name }

/**
 * 取得指定函数体中的首个表达式作为使用点。
 */
private fun CjFile.useSiteOf(functionName: String): CjElement =
    PsiTreeUtil.findChildrenOfType(this, CjNamedFunction::class.java)
        .single { it.name == functionName }
        .bodyExpression
        ?: error("函数 `$functionName` 缺少函数体表达式")
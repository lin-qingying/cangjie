package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjValueArgument
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * 不完整调用的期望类型恢复回归。
 *
 * 恢复只使用本次解析已保留的候选：全部候选都给出同一确定类型时才返回该类型；
 * 候选缺失映射、类型未确定或候选之间不一致时必须返回 `null`，不得任取第一个候选。
 *
 * 这些调用在源码中即为未完成/不可解析状态，因此放在编辑态夹具而不是生成诊断夹具中。
 */
class AnalysisApiCfirExpectedTypeRecoveryTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/components/expectedTypeRecovery",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /** 唯一保留候选时应恢复出确定的参数类型。 */
    @Test
    fun recoversExpectedTypeFromSingleCandidate(mainFile: CjFile) {
        analyzeForTest(mainFile) {
            assertEquals(
                "completion/expectedtype/Alpha",
                mainFile.firstArgumentOf("recoversExpectedTypeFromSingleCandidate").expectedType?.toString(),
                "唯一候选时应从保留候选恢复出参数类型",
            )
        }
    }

    /** 候选之间参数类型不一致时不得给出确定期望类型。 */
    @Test
    fun ambiguousCandidatesYieldNoExpectedType(mainFile: CjFile) {
        analyzeForTest(mainFile) {
            assertNull(
                mainFile.firstArgumentOf("ambiguousCandidatesYieldNoExpectedType").expectedType,
                "两个重载的参数类型不同，不得任取其一作为期望类型",
            )
        }
    }

    /** 没有保留候选时不得伪造期望类型。 */
    @Test
    fun noCandidateYieldsNoExpectedType(mainFile: CjFile) {
        analyzeForTest(mainFile) {
            assertNull(
                mainFile.firstArgumentOf("noCandidateYieldsNoExpectedType").expectedType,
                "callee 不可解析时必须返回 null",
            )
        }
    }

    /** 完全没有任何实参表达式的调用不承诺恢复类型。 */
    @Test
    fun emptyArgumentYieldsNoExpectedType(mainFile: CjFile) {
        analyzeForTest(mainFile) {
            assertNull(
                mainFile.onlyCallOf("emptyArgumentYieldsNoExpectedType").expectedType,
                "没有任何实参表达式时不得恢复出期望类型",
            )
        }
    }
}

/**
 * 取得指定函数中的唯一调用表达式；该函数必须存在且只发起一次调用。
 */
private fun CjFile.onlyCallOf(functionName: String): CjCallExpression {
    val function = PsiTreeUtil.findChildrenOfType(this, CjNamedFunction::class.java)
        .single { it.name == functionName }
    return PsiTreeUtil.findChildrenOfType(function, CjCallExpression::class.java).single()
}

/**
 * 取得指定函数中的第一个实参表达式；函数必须存在且至少有一个实参。
 */
private fun CjFile.firstArgumentOf(functionName: String): CjExpression {
    val function = PsiTreeUtil.findChildrenOfType(this, CjNamedFunction::class.java)
        .single { it.name == functionName }
    val argument = PsiTreeUtil.findChildrenOfType(function, CjExpression::class.java)
        .firstOrNull { it.parent is CjValueArgument }
    return requireNotNull(argument) { "函数 `$functionName` 中缺少实参表达式" }
}
package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.resolution.CaFunctionValueCall
import org.cangnova.cangjie.analysis.api.resolution.successfulCallOrNull
import org.cangnova.cangjie.analysis.api.resolution.successfulFunctionCallOrNull
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AnalysisApiCfirFunctionValueCallTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/components/functionValueCallClassification",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    @OptIn(CaExperimentalApi::class)
    @Test
    fun functionValueCallClassification(mainFile: CjFile) {
        val calls = PsiTreeUtil.findChildrenOfType(mainFile, CjCallExpression::class.java)
        val functionValueCall = calls.single {
            it.calleeExpression?.text == "callback" && it.valueArgumentList?.arguments?.isNotEmpty() == true
        }
        val directFunctionCall = calls.single {
            it.calleeExpression?.text == "transform" && it.valueArgumentList?.arguments?.isNotEmpty() == true
        }

        analyzeForTest(mainFile) {
            val resolvedFunctionValueCall = requireNotNull(
                functionValueCall.resolveToCall()?.successfulCallOrNull<CaFunctionValueCall>(),
            ) {
                "A call through a function-typed variable must resolve to CaFunctionValueCall."
            }
            val resolvedDirectCall = requireNotNull(
                directFunctionCall.resolveToCall()?.successfulFunctionCallOrNull(),
            )
            assertTrue(resolvedFunctionValueCall.isFunctionValueCall)
            assertFalse(resolvedDirectCall.isFunctionValueCall)
        }
    }
}

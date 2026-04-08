package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.resolver

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiComponentTestDirectives
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedCallableName
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedExplicitReceiverType
import org.cangnova.cangjie.analysis.api.impl.base.test.targetCallText
import org.cangnova.cangjie.analysis.api.resolution.CaCallKind
import org.cangnova.cangjie.analysis.api.resolution.CaCallOrigin
import org.cangnova.cangjie.analysis.api.resolution.successfulFunctionCallOrNull
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

/**
 * `resolver.resolveToCall` 的抽象测试。
 *
 * 这里不把 `memberCallInfo` 写死成单个 JUnit 方法，而是让 testData 声明目标调用和期望的公开语义，
 * 由 generated tests 扫描目录统一展开。
 */
abstract class AbstractResolveCallTest : AbstractAnalysisApiComponentTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val memberCall = PsiTreeUtil.findChildrenOfType(mainFile, CjCallExpression::class.java)
            .single { call -> matchesTargetCall(call, directives.targetCallText) }
        val expectedArgumentTypes = directives[AnalysisApiComponentTestDirectives.EXPECTED_ARGUMENT_TYPE]

        analyzeForTest(memberCall) {
            val callInfo = memberCall.resolveToCall()
            val successfulCall = callInfo?.successfulFunctionCallOrNull()

            assertNotNull(callInfo, "成员调用应产生调用解析结果。")
            assertNotNull(successfulCall, "成员调用应成功解析为函数调用。")
            assertEquals(1, callInfo!!.calls.size, "当前成功调用主链应只暴露唯一调用。")
            assertEquals(CaCallKind.FUNCTION, successfulCall!!.kind)
            assertEquals(CaCallOrigin.REGULAR, successfulCall.origin)
            assertEquals(directives.expectedCallableName, successfulCall.calleeName?.asString())
            assertEquals(directives.expectedCallableName, successfulCall.target?.name?.asString())
            assertEquals(
                directives.expectedExplicitReceiverType,
                successfulCall.explicitReceiverType?.render()?.let(::normalizeTypeRendering),
            )
            assertEquals(
                expectedArgumentTypes,
                successfulCall.argumentTypes.map { it?.render()?.let(::normalizeTypeRendering) },
            )
            assertEquals(0, successfulCall.typeArgumentCount)
        }
    }

    /**
     * 调用表达式的 PSI 形状和源码文本并不总是同构。
     *
     * 例如成员调用 `counter.add(42)` 常常会被拆成带 selector 的 qualified 结构。
     * 测试需要稳定地从源码意图中选中目标调用，因此同时接受：
     * 1. 调用表达式自身文本
     * 2. callee 文本
     * 3. 调用所在父表达式文本
     */
    private fun matchesTargetCall(callExpression: CjCallExpression, expectedText: String): Boolean {
        return callExpression.text == expectedText ||
            callExpression.calleeExpression?.text == expectedText ||
            callExpression.parent?.text == expectedText
    }
}

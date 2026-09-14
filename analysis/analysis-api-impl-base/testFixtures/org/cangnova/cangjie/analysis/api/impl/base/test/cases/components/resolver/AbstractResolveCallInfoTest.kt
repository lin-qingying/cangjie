package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.resolver

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.targetCallText
import org.cangnova.cangjie.analysis.api.resolution.CaCall
import org.cangnova.cangjie.analysis.api.resolution.CaCallableMemberCall
import org.cangnova.cangjie.analysis.api.resolution.calls
import org.cangnova.cangjie.analysis.api.resolution.symbol
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * `resolver.resolveToCall` 的调用信息快照测试。
 *
 * 与 `AbstractResolveCallTest` 只断言成功调用不同，本测试对齐 Kotlin
 * `AbstractResolveCallInfoTest`：同时覆盖成功与错误（重载歧义、未解析）解析结果，
 * 渲染 callInfo 运行时类别、候选数量与候选符号。
 */
abstract class AbstractResolveCallInfoTest : AbstractAnalysisApiComponentTest() {
    /**
     * 执行单个调用的 callInfo 快照测试。
     *
     * 方法按 `TARGET_CALL` 定位调用，渲染：
     * 1. callInfo 运行时类别（成功/错误）；
     * 2. 对外可见的调用候选数量；
     * 3. 各候选的被调用符号摘要。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val callExpression = PsiTreeUtil.findChildrenOfType(mainFile, CjCallExpression::class.java)
            .single { call -> matchesTargetCall(call, directives.targetCallText) }

        val actual = analyzeForTest(callExpression) {
            val callInfo = callExpression.resolveToCall()
            buildString {
                appendLine("callInfoClass: ${callInfo?.let { it::class.simpleName } ?: "null"}")
                appendLine("callsSize: ${callInfo?.calls?.size ?: 0}")
                if (callInfo == null) {
                    append("NO_CALL")
                } else {
                    append(
                        callInfo.calls.joinToString(separator = "\n\n") { call ->
                            renderCallSymbol(call)
                        },
                    )
                }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual, extension = "call.txt")
    }

    /**
     * 渲染单个调用候选的被调用符号摘要。
     *
     * 非可调用成员调用（理论上的扩展点）显式输出运行时类别，保证 golden 不丢失信息。
     */
    context(session: CaSession)
    private fun renderCallSymbol(call: CaCall): String {
        return if (call is CaCallableMemberCall<*, *>) {
            renderSymbolForResolveTest(call.symbol)
        } else {
            "callClass: ${call::class.simpleName}"
        }
    }

    /**
     * 调用表达式的 PSI 形状和源码文本并不总是同构。
     *
     * 与 `AbstractResolveCallTest` 保持同一目标选择协议：接受调用自身、callee 与父表达式文本。
     */
    private fun matchesTargetCall(callExpression: CjCallExpression, expectedText: String): Boolean {
        return callExpression.text == expectedText ||
            callExpression.calleeExpression?.text == expectedText ||
            callExpression.parent?.text == expectedText
    }
}

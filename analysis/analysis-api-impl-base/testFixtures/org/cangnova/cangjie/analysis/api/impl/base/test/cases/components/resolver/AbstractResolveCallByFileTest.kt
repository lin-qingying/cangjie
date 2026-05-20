package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.resolver

import org.cangnova.cangjie.analysis.api.resolution.successfulFunctionCallOrNull
import org.cangnova.cangjie.analysis.api.resolution.calls
import org.cangnova.cangjie.analysis.api.resolution.symbol
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.psiUtil.collectDescendantsOfType
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * `allByPsi` 文件级调用解析测试。
 *
 * 对齐 Kotlin `AbstractResolveCallByFileTest` 的文件级批量验证职责，
 * 但只覆盖仓颉当前公开 API 已明确支持的 `CjCallExpression`。
 */
abstract class AbstractResolveCallByFileTest : AbstractResolveCallTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val calls = mainFile.collectDescendantsOfType<CjCallExpression>()

        val actual = buildString {
            calls.forEachIndexed { index, callExpression ->
                appendLine("${callExpression::class.simpleName}: ${callExpression.text}")
                append(
                    analyzeForTest(callExpression) {
                        val callInfo = callExpression.resolveToCall()
                        val successfulCall = callInfo?.successfulFunctionCallOrNull()

                        buildString {
                            appendLine("callInfoClass: ${callInfo?.javaClass?.simpleName ?: "null"}")
                            appendLine("callsSize: ${callInfo?.calls?.size ?: 0}")
                            appendLine("dispatchReceiverType: ${renderTypeForResolveTest(successfulCall?.dispatchReceiver?.type)}")
                            appendLine(
                                "argumentTypes: ${
                                    successfulCall?.valueArgumentMapping?.keys?.map { argument ->
                                        renderTypeForResolveTest(argument.expressionType)
                                    } ?: emptyList()
                                }",
                            )
                            append(renderSymbolForResolveTest(successfulCall?.symbol))
                        }
                    }.prependIndent("  "),
                )

                if (index != calls.lastIndex) {
                    appendLine()
                    appendLine()
                }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual, extension = "call.txt")
    }
}

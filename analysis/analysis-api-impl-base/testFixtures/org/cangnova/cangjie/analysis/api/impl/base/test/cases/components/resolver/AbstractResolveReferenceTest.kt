package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.resolver

import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.idea.references.mainReference
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * `resolver` 的引用解析测试。
 *
 * 对齐 Kotlin `AbstractResolveReferenceTest` 的职责边界：
 * - `singleByPsi` 通过 `<caret>` 锁定引用入口；
 * - 只验证仓颉当前公开 API 已支持的 `CjReferenceExpression -> resolveToSymbols()` 路径；
 * - 不发明 `KtReference.tryResolveSymbols()` 对应的仓颉兼容层。
 */
abstract class AbstractResolveReferenceTest : AbstractAnalysisApiComponentTest() {
    /**
     * 执行单个引用表达式的解析测试。
     *
     * 方法通过 caret 定位引用表达式，渲染主引用信息和 `resolveToSymbols()` 输出。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val referenceExpression = testServices.expressionMarkerProvider
            .getBottommostElementOfTypeAtCaret<CjReferenceExpression>(mainFile)

        val actual = renderReference(referenceExpression)
        testServices.assertions.assertEqualsToTestOutputFile(actual, extension = "reference.txt")
    }

    /**
     * 渲染单个引用表达式的公开解析结果。
     *
     * 输出包含引用实现类、表达式文本、按名解析信息和恢复到的公开 symbol 列表。
     */
    protected fun renderReference(referenceExpression: CjReferenceExpression): String = analyzeForTest(referenceExpression) {
        val reference = referenceExpression.mainReference
        buildString {
            appendLine("referenceClass: ${reference?.javaClass?.simpleName ?: "null"}")
            appendLine("expression: ${referenceExpression.text}")
            appendLine(
                "resolvesByNames: ${
                    reference?.resolvesByNames?.map { it.asString() } ?: emptyList()
                }",
            )

            val resolvedSymbols = referenceExpression.resolveToSymbols().toList()
            if (resolvedSymbols.isEmpty()) {
                append("resolvedSymbols: []")
            } else {
                appendLine("resolvedSymbols:")
                resolvedSymbols.forEachIndexed { index, symbol ->
                    appendLine("  [$index]")
                    append(
                        renderSymbolForResolveTest(symbol).prependIndent("    "),
                    )
                    if (index != resolvedSymbols.lastIndex) {
                        appendLine()
                    }
                }
            }
        }
    }
}

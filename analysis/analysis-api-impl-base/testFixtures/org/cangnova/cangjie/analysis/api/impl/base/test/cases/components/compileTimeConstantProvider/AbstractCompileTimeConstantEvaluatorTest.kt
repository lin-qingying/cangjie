package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.compileTimeConstantProvider

import org.cangnova.cangjie.analysis.api.evaluation.CaCollectionCompileTimeValue
import org.cangnova.cangjie.analysis.api.evaluation.CaCompileTimeValue
import org.cangnova.cangjie.analysis.api.evaluation.CaScalarCompileTimeValue
import org.cangnova.cangjie.analysis.api.evaluation.CaTupleCompileTimeValue
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjValueArgument
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * `compileTimeConstantProvider.evaluate` 的抽象测试。
 *
 * 这里只观察当前公开 Analysis API 已经暴露的稳定值模型：
 * - 标量常量
 * - tuple 常量
 * - collection 常量
 * - 渲染文本与递归结构
 */
abstract class AbstractCompileTimeConstantEvaluatorTest : AbstractAnalysisApiComponentTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val actual = copyAwareAnalyzeForTest(mainFile) { contextFile ->
            val targetElement = testServices.expressionMarkerProvider.getBottommostElementOfTypeByDirective(contextFile, mainModule.testModule)
            val expression = when (targetElement) {
                is CjExpression -> targetElement
                is CjValueArgument -> targetElement.getArgumentExpression()
                else -> null
            } ?: error("Unsupported constant-evaluation target: ${targetElement::class.simpleName}")

            val compileTimeValue = expression.evaluate()
            buildString {
                appendLine("expression: ${expression.text}")
                if (compileTimeValue == null) {
                    appendLine("constant: NOT_EVALUATED")
                } else {
                    appendLine("constant:")
                    append(renderCompileTimeValue(compileTimeValue))
                }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }

    private fun renderCompileTimeValue(
        value: CaCompileTimeValue,
        indent: Int = 2,
    ): String = buildString {
        when (value) {
            is CaScalarCompileTimeValue -> {
                appendLine("${" ".repeat(indent)}kind: SCALAR(${value.kind})")
                appendLine("${" ".repeat(indent)}renderedText: ${value.renderedText}")
            }

            is CaTupleCompileTimeValue -> {
                appendLine("${" ".repeat(indent)}kind: TUPLE")
                appendLine("${" ".repeat(indent)}renderedText: ${value.renderedText}")
                value.elements.forEachIndexed { index, element ->
                    appendLine("${" ".repeat(indent)}element[$index]:")
                    append(renderCompileTimeValue(element, indent + 2))
                }
            }

            is CaCollectionCompileTimeValue -> {
                appendLine("${" ".repeat(indent)}kind: COLLECTION")
                appendLine("${" ".repeat(indent)}renderedText: ${value.renderedText}")
                value.elements.forEachIndexed { index, element ->
                    appendLine("${" ".repeat(indent)}element[$index]:")
                    append(renderCompileTimeValue(element, indent + 2))
                }
            }

            else -> {
                appendLine("${" ".repeat(indent)}kind: ${value::class.qualifiedName}")
                appendLine("${" ".repeat(indent)}renderedText: ${value.renderedText}")
            }
        }
    }
}

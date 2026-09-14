package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.symbolInformationProvider

import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * 函数级修饰符语义（operator / static / mutating）公开 API 的抽象测试。
 *
 * 对齐 Kotlin `AbstractIsOperatorTest`：caret 定位函数声明，恢复 `CaFunctionSymbol`，
 * 渲染 `isOperator` 并附带 `isStatic` / `isMutating`，覆盖操作符函数、普通函数与
 * extend 成员操作符场景。
 */
abstract class AbstractIsOperatorTest : AbstractAnalysisApiComponentTest() {
    /**
     * 执行函数修饰符语义测试。
     *
     * 方法从 caret 处定位函数声明，输出符号运行时类别与三类修饰符标志；
     * 非 callable 符号显式输出 `symbolClass`，保证 golden 不丢失符号形态信息。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val declaration = testServices.expressionMarkerProvider
            .getBottommostElementOfTypeAtCaret<CjNamedFunction>(mainFile)

        val actual = copyAwareAnalyzeForTest(declaration) { contextDeclaration ->
            val symbol = contextDeclaration.symbol
            val functionSymbol = symbol as? CaFunctionSymbol
            buildString {
                appendLine("CjNamedFunction: ${contextDeclaration.name}")
                appendLine("symbolClass: ${symbol::class.simpleName}")
                if (functionSymbol != null) {
                    appendLine("isOperator: ${functionSymbol.isOperator}")
                    appendLine("isStatic: ${functionSymbol.isStatic}")
                    appendLine("isMutating: ${functionSymbol.isMutating}")
                }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}

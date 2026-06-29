package org.cangnova.cangjie.analysis.api.impl.base.test.cases.symbols

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices

/**
 * caret 标记处单个 PSI 元素到 symbol 的抽象测试。
 *
 * 测试允许选择声明或文件，并验证对应 symbol 的渲染与 pointer 恢复。
 */
abstract class AbstractSingleSymbolByPsiTest : AbstractSymbolTest() {
    /**
     * 收集 caret 指定元素对应的单个 symbol。
     *
     * 选择元素为声明时返回 `declaration.symbol`，选择文件时返回 `file.symbol`。
     */
    override fun CaSession.collectSymbols(cjFile: CjFile, testServices: TestServices): SymbolsData {
        val module = testServices.cjTestModuleStructure.requireModuleByFile(cjFile)
        val selected = testServices.expressionMarkerProvider.getBottommostElementOfTypeByDirective(
            cjFile,
            module.testModule,
            defaultType = CjDeclaration::class,
        )

        val symbol = when (selected) {
            is CjDeclaration -> selected.symbol
            is CjFile -> selected.symbol
            else -> error("Selected element type should be a declaration or a file: ${selected::class.simpleName}")
        }

        return SymbolsData(listOf(symbol))
    }
}

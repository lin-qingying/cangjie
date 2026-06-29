package org.cangnova.cangjie.analysis.api.impl.base.test.cases.symbols

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.psiUtil.collectDescendantsOfType
import org.cangnova.cangjie.test.services.TestServices

/**
 * 文件内所有可成符号声明到 symbol 的抽象测试。
 *
 * 测试遍历 PSI 声明树，过滤当前 public symbol API 支持的声明形态，并追加 file symbol。
 */
abstract class AbstractSymbolByPsiTest : AbstractSymbolTest() {
    /**
     * 从文件 PSI 中收集所有可创建的声明 symbol 与文件 symbol。
     *
     * 该集合用于批量验证 symbol 渲染、pointer 创建和 pointer 恢复行为。
     */
    override fun CaSession.collectSymbols(cjFile: CjFile, testServices: TestServices): SymbolsData {
        val declarationSymbols = cjFile
            .collectDescendantsOfType<CjDeclaration> { it.isValidForSymbolCreation }
            .map { declaration -> declaration.symbol }

        return SymbolsData(declarationSymbols + cjFile.symbol)
    }
}

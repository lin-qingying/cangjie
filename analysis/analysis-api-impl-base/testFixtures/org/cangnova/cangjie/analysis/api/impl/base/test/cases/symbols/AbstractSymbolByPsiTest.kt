package org.cangnova.cangjie.analysis.api.impl.base.test.cases.symbols

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.psiUtil.collectDescendantsOfType
import org.cangnova.cangjie.test.services.TestServices

abstract class AbstractSymbolByPsiTest : AbstractSymbolTest() {
    override fun CaSession.collectSymbols(cjFile: CjFile, testServices: TestServices): SymbolsData {
        val declarationSymbols = cjFile
            .collectDescendantsOfType<CjDeclaration> { it.isValidForSymbolCreation }
            .map { declaration -> declaration.symbol }

        return SymbolsData(declarationSymbols + cjFile.symbol)
    }
}

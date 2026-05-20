package org.cangnova.cangjie.analysis.api.impl.base.test.cases.symbols

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices

abstract class AbstractSingleSymbolByPsiTest : AbstractSymbolTest() {
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

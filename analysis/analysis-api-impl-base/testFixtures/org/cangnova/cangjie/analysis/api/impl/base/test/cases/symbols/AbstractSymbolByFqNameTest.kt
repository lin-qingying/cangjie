package org.cangnova.cangjie.analysis.api.impl.base.test.cases.symbols

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices

abstract class AbstractSymbolByFqNameTest : AbstractSymbolTest() {
    override fun CaSession.collectSymbols(cjFile: CjFile, testServices: TestServices): SymbolsData {
        val module = testServices.cjTestModuleStructure.requireModuleByFile(cjFile)
        val directives = directivesForMainFile(cjFile, module)
        return SymbolsData(
            buildList {
                directives[SymbolTestDirectives.TARGET_CLASS_FQ_NAME].mapTo(this) { className ->
                    getClassLikeSymbol(ClassId.topLevel(FqName.fromString(className)))
                        ?: error("Class-like symbol '$className' was not found")
                }
                directives[SymbolTestDirectives.TARGET_CALLABLE_FQ_NAME].flatMapTo(this) { callableName ->
                    val fqName = FqName.fromString(callableName)
                    getTopLevelCallableSymbols(fqName.parent(), Name.identifier(fqName.shortName().asString()))
                }
            },
        )
    }
}

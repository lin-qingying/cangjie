package org.cangnova.cangjie.analysis.api.impl.base.test.cases.types

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices

abstract class AbstractTypeByDeclarationReturnTypeTest : AbstractTypeTest() {
    override fun getType(
        analysisSession: CaSession,
        cjFile: CjFile,
        module: CjTestModule,
        testServices: TestServices,
    ) = with(analysisSession) {
        val declaration = testServices.expressionMarkerProvider
            .getBottommostElementOfTypeAtCaret<CjCallableDeclaration>(cjFile)
        declaration.returnType ?: error("Callable `${declaration.text}` does not expose a return type.")
    }
}

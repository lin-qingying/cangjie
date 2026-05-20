package org.cangnova.cangjie.analysis.api.impl.base.test.cases.annotations

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

abstract class AbstractAnalysisApiAnnotationsOnDeclarationsTest : AbstractAnalysisApiComponentTest() {
    open fun renderAnnotations(analysisSession: CaSession, annotations: CaAnnotationList): String {
        return TestAnnotationRenderer.renderAnnotations(analysisSession, annotations)
    }

    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val declaration = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<CjDeclaration>(mainFile)
        val actual = copyAwareAnalyzeForTest(declaration) { contextDeclaration ->
            val declarationSymbol = contextDeclaration.symbol as? CaDeclarationSymbol
                ?: error("Declaration `${contextDeclaration.text}` does not resolve to a declaration symbol.")
            buildString {
                appendLine("${CjDeclaration::class.simpleName}: ${contextDeclaration::class.simpleName} ${contextDeclaration.name}")
                append(renderAnnotations(useSiteSession, declarationSymbol.annotations))
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}

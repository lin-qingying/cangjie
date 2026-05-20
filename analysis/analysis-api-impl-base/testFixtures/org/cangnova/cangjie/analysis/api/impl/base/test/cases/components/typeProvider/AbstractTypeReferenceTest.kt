package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.typeProvider

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjTypeReference
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * `typeProvider.typeReference` 的抽象测试。
 */
abstract class AbstractTypeReferenceTest : AbstractAnalysisApiComponentTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val typeReference = testServices.expressionMarkerProvider
            .getBottommostElementOfTypeAtCaret<CjTypeReference>(mainFile)

        val actual = analyzeForTest(typeReference) {
            val owner = PsiTreeUtil.getParentOfType(typeReference, CjCallableDeclaration::class.java)
            val ownerSymbol = owner?.symbol as? CaCallableSymbol
            buildString {
                appendLine("CjTypeReference: ${typeReference.getTypeText()}")
                appendLine(
                    "CaType: ${
                        ownerSymbol?.returnType
                            ?.render(CaTypeRendererForSource.WITH_QUALIFIED_NAMES)
                            ?.let(::normalizeTypeRendering)
                    }",
                )
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}

package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.typeProvider

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * `typeProvider.defaultType` 的抽象测试。
 */
abstract class AbstractDefaultTypeTest : AbstractAnalysisApiComponentTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val classDeclaration = testServices.expressionMarkerProvider
            .getTopmostSelectedElementOfTypeByDirectiveOrNull(mainFile, mainModule, CjClass::class)
            as? CjClass
            ?: PsiTreeUtil.findChildrenOfType(mainFile, CjClass::class.java).single()

        val actual = analyzeForTest(classDeclaration) {
            val symbol = getClassLikeSymbol(classDeclaration.getClassId()!!) as CaClassLikeSymbol
            val defaultType = symbol.defaultType
            buildString {
                appendLine("class: ${classDeclaration.name}")
                appendLine("defaultType: ${defaultType.render(CaTypeRendererForSource.WITH_QUALIFIED_NAMES).let(::normalizeTypeRendering)}")
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}

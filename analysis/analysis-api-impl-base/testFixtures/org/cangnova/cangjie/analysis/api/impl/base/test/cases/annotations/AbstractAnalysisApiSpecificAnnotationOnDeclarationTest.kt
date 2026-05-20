package org.cangnova.cangjie.analysis.api.impl.base.test.cases.annotations

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.directives.model.singleValue
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

abstract class AbstractAnalysisApiSpecificAnnotationOnDeclarationTest : AbstractAnalysisApiComponentTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + Directives

    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val classIdString = directives.singleValue(Directives.CLASS_ID)

        val actual = copyAwareAnalyzeForTest(mainFile) { contextFile ->
            val declaration = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<CjDeclaration>(contextFile)
            val declarationSymbol = declaration.symbol as? org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
                ?: error("Declaration `${declaration.text}` does not resolve to a declaration symbol.")
            val annotationList = declarationSymbol.annotations
            val classId = ClassId.fromString(classIdString)

            fun renderAnnotation(annotation: CaAnnotation): String = buildString {
                appendLine("${CjDeclaration::class.simpleName}: ${declaration::class.simpleName} ${declaration.name}")
                append(TestAnnotationRenderer.renderSingleAnnotation(annotation))
            }

            testServices.assertions.assertTrue(classId in annotationList) {
                "ClassId $classId is not found in the annotation list."
            }

            val directAccess = renderAnnotation(annotationList[classId].single())
            val resolvedAccess = renderAnnotation(annotationList.single { annotation -> annotation.classId == classId })
            testServices.assertions.assertEquals(resolvedAccess, directAccess) {
                "Result before and after resolving the target annotation differs."
            }

            resolvedAccess
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }

    private object Directives : SimpleDirectivesContainer() {
        val CLASS_ID by stringDirective("当前测试期望命中的注解 ClassId。", applicability = DirectiveApplicability.File)
    }
}

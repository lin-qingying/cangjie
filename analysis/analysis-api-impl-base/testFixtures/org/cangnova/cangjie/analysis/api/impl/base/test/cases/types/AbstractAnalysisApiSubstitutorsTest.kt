package org.cangnova.cangjie.analysis.api.impl.base.test.cases.types

import org.cangnova.cangjie.analysis.api.components.buildSubstitutor
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.targetClassName
import org.cangnova.cangjie.analysis.api.impl.base.test.targetFunctionName
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * public `CaSubstitutor` 的类型替换抽象测试。
 */
abstract class AbstractAnalysisApiSubstitutorsTest : AbstractAnalysisApiComponentTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val actual = analyzeForTest(mainFile) {
            val replacementClass = org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiTypeTestSupport
                .resolveClassSymbol(mainModule, directives.targetClassName)
            val callableSymbol = getTopLevelCallableSymbols(
                mainFile.packageFqName,
                Name.identifier(directives.targetFunctionName),
            ).singleOrNull() as? CaFunctionSymbol
                ?: error("Cannot resolve function `${directives.targetFunctionName}` for substitutor test.")

            val returnType = callableSymbol.returnType
            val substitutor = buildSubstitutor {
                substitution(
                    typeParameter = callableSymbol.typeParameters.single(),
                    type = replacementClass.defaultType,
                )
            }
            val substituted = substitutor.substitute(returnType)
            val substitutedOrNull = substitutor.substituteOrNull(returnType)

            buildString {
                appendLine("originalType: ${normalizeTypeRendering(returnType.render(CaTypeRendererForSource.WITH_QUALIFIED_NAMES))}")
                appendLine("substitutor.substitute: ${normalizeTypeRendering(substituted.render(CaTypeRendererForSource.WITH_QUALIFIED_NAMES))}")
                appendLine(
                    "substitutor.substituteOrNull: ${
                        substitutedOrNull
                            ?.render(CaTypeRendererForSource.WITH_QUALIFIED_NAMES)
                            ?.let(::normalizeTypeRendering)
                    }",
                )
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}

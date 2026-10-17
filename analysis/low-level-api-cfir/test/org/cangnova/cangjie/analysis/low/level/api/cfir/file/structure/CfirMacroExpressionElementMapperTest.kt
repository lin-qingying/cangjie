package org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.resolveToCfirSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.CfirElementsRecorder
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.DeclarationStructureElement
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.LLEagerElementMapper
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.applyMacroSemanticSnapshotMappings
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildErrorExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.psi.CjConstantExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjPsiFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/** 验证 expression macro alias 在 eager body 映射中重绑定到当前解析树。 */
class CfirMacroExpressionElementMapperTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/macroElementMapper",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun currentMacroReplacementIsMappedForEagerBodyAnalysis(mainFile: CjFile) {
        val facade = mainFile.getResolutionFacadeForTest()
        val macroFile = CjPsiFactory(mainFile.project).createFile(
            "macroAliases.cj",
            mainFile.text.replace("= 42", "= @MakeInt64()"),
        )

        val eagerPsi = function(mainFile, "eagerTarget")
        val eagerLiteralPsi = literal(eagerPsi)
        val eagerMacroPsi = macro(function(macroFile, "eagerTarget"))
        val eagerDeclaration = eagerPsi.resolveToCfirSymbol(facade, CfirResolvePhase.BODY_RESOLVE)
            .cfir as CfirNamedFunction
        val eagerStaleReplacement = unresolvedReplacement(eagerDeclaration, eagerLiteralPsi)
        val resolvedMappings = CfirElementsRecorder.recordElementsFrom(
            eagerDeclaration,
            DeclarationStructureElement.Recorder,
        )
        val constructionCarrier = buildErrorExpression {
            source = eagerStaleReplacement.source
            diagnostic = ConeSimpleDiagnostic("construction-only macro carrier")
        }
        val physicalMappings = resolvedMappings.toMutableMap().apply {
            put(eagerMacroPsi, constructionCarrier)
        }
        applyMacroSemanticSnapshotMappings(
            resolvedMappings = physicalMappings,
            additionalMappings = mapOf(eagerMacroPsi to eagerStaleReplacement),
            session = eagerDeclaration.moduleData.session,
        )
        assertSame(
            resolvedMappings[eagerLiteralPsi],
            physicalMappings[eagerMacroPsi],
            "A macro snapshot replacement must override a construction-only carrier already recorded for its PSI.",
        )
        assertFalse(physicalMappings[eagerMacroPsi] is CfirErrorExpression)

        val eagerMapper = LLEagerElementMapper(
            eagerDeclaration,
            additionalMappings = mapOf(eagerMacroPsi to eagerStaleReplacement),
        )

        val eagerMapped = eagerMapper(eagerMacroPsi) as CfirExpression
        assertNotSame(eagerStaleReplacement, eagerMapped)
        assertEquals("42", (eagerMapped as CfirLiteralExpression).value.toString())
        assertEquals(ConePrimitiveType.INT64, eagerMapped.coneTypeOrNull)
        assertSame(eagerMapper(eagerLiteralPsi), eagerMapped)
    }

    private fun function(file: CjFile, name: String): CjNamedFunction =
        file.declarations.filterIsInstance<CjNamedFunction>().single { it.name == name }

    private fun literal(function: CjNamedFunction): CjConstantExpression =
        PsiTreeUtil.findChildrenOfType(function, CjConstantExpression::class.java).single { it.text == "42" }

    private fun macro(function: CjNamedFunction): CjMacroExpression =
        PsiTreeUtil.findChildrenOfType(function, CjMacroExpression::class.java).single()

    private fun unresolvedReplacement(
        function: CfirNamedFunction,
        sourcePsi: CjConstantExpression,
    ): CfirLiteralExpression {
        val rawExpression = CfirElementsRecorder.recordElementsFrom(
            function,
            DeclarationStructureElement.Recorder,
        )[sourcePsi] as CfirLiteralExpression

        return buildLiteralExpression {
            source = rawExpression.source
            kind = rawExpression.kind
            value = rawExpression.value
        }
    }
}

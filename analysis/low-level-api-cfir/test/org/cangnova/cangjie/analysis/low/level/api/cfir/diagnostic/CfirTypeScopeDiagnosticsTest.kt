package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostic

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.collectDiagnosticsForFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.type
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** 验证按声明惰性解析时保留官方类体名称遮蔽。 */
class CfirTypeScopeDiagnosticsTest : AbstractAnalysisApiExecutionTest("analysis/low-level-api-cfir/testData/diagnostics/typeScope") {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun classHeaderBeforeBody(mainFile: CjFile) {
        val diagnostics = mainFile.collectDiagnosticsForFile(mainFile.getResolutionFacadeForTest(),
            DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
        assertEquals(emptyList<String>(), diagnostics.map { it.factoryName })
    }

    @Test
    fun nestedFunctionTypeShadow(mainFile: CjFile) {
        val diagnostics = mainFile.collectDiagnosticsForFile(mainFile.getResolutionFacadeForTest(),
            DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
        val notTypes = diagnostics.filter { it.factoryName == "CFIR_NOT_A_TYPE" }
        assertEquals(1, notTypes.size, diagnostics.joinToString { it.factoryName })
        assertEquals(mainFile.text.indexOf("x: Bound") + "x: ".length,
            notTypes.single().textRanges.single().startOffset)
    }

    @Test
    fun redeclaredConstructorOwners(mainFile: CjFile) {
        val file = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
        val classes = file.declarations.filterIsInstance<CfirClass>()
        assertEquals(2, classes.size)
        for (owner in classes) {
            val constructor = owner.declarations.filterIsInstance<CfirConstructor>().single()
            constructor.lazyResolveToPhase(CfirResolvePhase.TYPES)
            val type = constructor.returnTypeRef.coneTypeOrNull as ConeLookupTagBasedType
            assertEquals(owner.typeParameters.single().symbol.constructType(), type.typeArguments.single().type)
        }
    }

    @Test
    fun declaredMemberTypeShadow(mainFile: CjFile) {
        val diagnostics = mainFile.collectDiagnosticsForFile(mainFile.getResolutionFacadeForTest(),
            DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
        val notTypes = diagnostics.filter { it.factoryName == "CFIR_NOT_A_TYPE" }
        assertEquals(4, notTypes.size, diagnostics.joinToString { it.factoryName })
        val text = mainFile.text
        val expectedOffsets = listOf("WrappedJob: Job", "s: Schedule", "w: Job", "j: Job").map { fragment ->
            text.indexOf(fragment) + fragment.indexOf(": ") + 2
        }.sorted()
        assertEquals(expectedOffsets, notTypes.flatMap { it.textRanges }.map { it.startOffset }.sorted())
    }
}

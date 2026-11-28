package org.cangnova.cangjie.analysis.low.level.api.cfir.resolve

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.collectDiagnosticsForFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.cjmpMatchFacts
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.session.CfirCjmpCandidateDiagnostic
import org.cangnova.cangjie.cfir.session.CfirCjmpFamilyAggregate
import org.cangnova.cangjie.cfir.session.CfirCjmpFamilySnapshot
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.cjmpFamilyAggregate
import org.cangnova.cangjie.cfir.session.isCjmpSpecificCompilationEnabled
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMismatchKind
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 从真实 LL enum variant 入口请求配对，父 enum 和其它 variant 不由测试提前解析。
 * 官方 Empty 虽以 VarDecl 编码仍是 enum TryBind；非穷尽只免失配诊断，不制造成功绑定。
 */
class CfirCjmpEnumMatchingTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/cjmpEnumMatching",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)
    override val additionalDirectives = super.additionalDirectives + CfirDiagnosticsDirectives

    @Test
    fun genericVariantsResolveFromVariant(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val fixture = prepare(mainFile, mainModule, testServices)
        val commonVariants = fixture.common.declarations.filterIsInstance<CfirEnumConstructor>().associateBy { it.name.asString() }
        val specificVariants = fixture.specific.declarations.filterIsInstance<CfirEnumConstructor>().associateBy { it.name.asString() }
        assertEquals(setOf("Empty", "Value", "Pair"), commonVariants.keys)
        assertEquals(commonVariants.keys, specificVariants.keys)
        assertEquals(0, specificVariants.getValue("Empty").valueParameters.size)
        assertEquals(1, specificVariants.getValue("Value").valueParameters.size)
        assertEquals(2, specificVariants.getValue("Pair").valueParameters.size)
        val parentMapping = fixture.common.typeParameters.zip(fixture.specific.typeParameters).toMap()
        assertEquals(1, parentMapping.size)

        // 先请求最复杂的 payload；不先推进 E，也不使用 Empty 为其它 variant 预热。
        for (name in listOf("Pair", "Empty", "Value")) {
            val specific = specificVariants.getValue(name)
            specific.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
            assertSame(
                commonVariants.getValue(name), fixture.family.commonFor(specific),
                "$name receives its enclosing enum's CJMP result; roles=${fixture.specific.status.isCommon}/${fixture.specific.status.isSpecific}/${specific.status.isCommon}/${specific.status.isSpecific}; parent=${fixture.family.commonFor(fixture.specific)}, " +
                    "mismatch=${fixture.family.mismatchKindsFor(specific)}, mapping=${fixture.family.typeParameterMappingFor(specific)}, " +
                    "commonPayload=${commonVariants.getValue(name).valueParameters.map { it.returnTypeRef }}, " +
                    "specificPayload=${specific.valueParameters.map { it.returnTypeRef }}, " +
                    "plans=${specific.cjmpMatchFacts?.plans}",
            )
            assertTrue(fixture.family.hasResolutionResult(specific), "$name has a completed matching result")
            assertEquals(parentMapping, fixture.family.typeParameterMappingFor(specific), "$name carries the outer T to U mapping")
            assertEquals(emptyList<CjmpMismatchKind>(), fixture.family.mismatchKindsFor(specific), name)
            assertEquals(listOf(specific), fixture.family.specificBindingsFor(commonVariants.getValue(name)), name)
        }
        assertSame(fixture.common, fixture.family.commonFor(fixture.specific))
        assertEquals(parentMapping, fixture.family.typeParameterMappingFor(fixture.specific))
        assertUnrelatedBodyIsLazy(fixture)

        val firstResults = specificVariants.mapValues { snapshot(it.value, fixture.family) }
        repeat(2) {
            for (specific in specificVariants.values.toList().asReversed()) {
                specific.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
            }
        }
        assertEquals(firstResults, specificVariants.mapValues { snapshot(it.value, fixture.family) })
        assertUnrelatedBodyIsLazy(fixture)
    }

    @Test
    fun nonExhaustiveExtraIsProcessedButUnmatched(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val fixture = prepare(mainFile, mainModule, testServices)
        assertTrue(fixture.common.isNonExhaustive)
        val extra = fixture.specific.declarations.filterIsInstance<CfirEnumConstructor>().single { it.name.asString() == "Extra" }
        extra.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        assertSame(fixture.common, fixture.family.commonFor(fixture.specific))
        assertTrue(fixture.family.hasResolutionResult(extra), "a suppressed NOT_MATCHED still requires a processed result")
        assertNull(fixture.family.commonFor(extra), "non-exhaustiveness does not invent a counterpart")
        assertTrue(fixture.family.commonCounterpartsFor(extra).isEmpty())
        assertTrue(fixture.family.isUnmatched(extra))
        assertUnrelatedBodyIsLazy(fixture)

        val firstResult = snapshot(extra, fixture.family)
        extra.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        assertEquals(firstResult, snapshot(extra, fixture.family))
        val diagnostics = mainFile.collectDiagnosticsForFile(mainFile.getResolutionFacadeForTest(), DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
        assertEquals(emptyList<String>(), diagnostics.map { it.factoryName }.sorted())
        assertNull(fixture.family.commonFor(extra))
        assertTrue(fixture.family.hasResolutionResult(extra))
        assertUnrelatedBodyIsLazy(fixture)
    }

    @Test
    fun concretePayloadMismatchIsNotReturnCovariance(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val fixture = prepare(mainFile, mainModule, testServices)
        val common = fixture.common.declarations.filterIsInstance<CfirEnumConstructor>().single()
        val specific = fixture.specific.declarations.filterIsInstance<CfirEnumConstructor>().single()
        assertEquals(common.name, specific.name)
        assertEquals(1, common.valueParameters.size)
        assertEquals(1, specific.valueParameters.size)
        specific.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        assertSame(fixture.common, fixture.family.commonFor(fixture.specific))
        assertTrue(fixture.family.hasResolutionResult(specific), "payload incompatibility must be recorded")
        assertNull(fixture.family.commonFor(specific), "same enum return type does not make Int64 and Bool payloads compatible")
        assertTrue(fixture.family.isUnmatched(specific))
        assertTrue(fixture.family.specificBindingsFor(common).isEmpty())
        val firstResult = snapshot(specific, fixture.family)
        specific.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        assertEquals(firstResult, snapshot(specific, fixture.family))
        assertUnrelatedBodyIsLazy(fixture)

        val diagnostics = mainFile.collectDiagnosticsForFile(mainFile.getResolutionFacadeForTest(), DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
        val names = diagnostics.map { it.factoryName.removePrefix("CFIR_") }
        assertTrue("NOT_MATCHED" in names, "concrete payload incompatibility is an enum matching failure: $names")
        assertFalse("RETURN_TYPE_INCOMPATIBLE" in names, "enum payload matching is not function return covariance")
        assertNull(fixture.family.commonFor(specific))
        assertUnrelatedBodyIsLazy(fixture)
    }

    /** 只构建 raw 文件并检查模块模式，沿用现有 LL 基座；准备阶段不推进任何声明相位。 */
    private fun prepare(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices): EnumFixture {
        val modules = testServices.cjTestModuleStructure.mainModules
        assertEquals(2, modules.size)
        for (module in modules) {
            val file = module.cjFiles.first()
            val raw = file.getOrBuildCfirFile(file.getResolutionFacadeForTest())
            assertEquals(
                module.testModule.directives[CfirDiagnosticsDirectives.CJMP_MODE].single(),
                raw.moduleData.session.cjmpSettings.mode,
                "${module.name}: session 必须消费模块配置的 CJMP 模式",
            )
        }
        val commonPsi = modules.single { it !== mainModule }.cjFiles.single()
        val common = commonPsi.getOrBuildCfirFile(commonPsi.getResolutionFacadeForTest())
            .declarations.filterIsInstance<CfirEnum>().single()
        val specificFile = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
        val specific = specificFile.declarations.filterIsInstance<CfirEnum>().single()
        val unrelatedPsi = mainModule.cjFiles.single { it !== mainFile }
        val unrelated = unrelatedPsi.getOrBuildCfirFile(unrelatedPsi.getResolutionFacadeForTest())
            .declarations.filterIsInstance<CfirNamedFunction>().single()
        assertTrue(specificFile.moduleData.session.isCjmpSpecificCompilationEnabled())
        assertNull(specific.cjmpMatchFacts)
        assertTrue(common.resolvePhase < CfirResolvePhase.CJMP_MATCHING)
        assertTrue(specific.resolvePhase < CfirResolvePhase.CJMP_MATCHING)
        for (variant in specific.declarations.filterIsInstance<CfirEnumConstructor>()) {
            assertTrue(variant.resolvePhase < CfirResolvePhase.CJMP_MATCHING)
            assertNull(variant.cjmpMatchFacts)
        }
        return EnumFixture(common, specific, unrelated, specificFile.moduleData.session.cjmpFamilyAggregate)
            .also(::assertUnrelatedBodyIsLazy)
    }

    private fun assertUnrelatedBodyIsLazy(fixture: EnumFixture) {
        assertTrue(fixture.unrelated.body is CfirLazyBlock, "enum matching must not analyze an unrelated named function body")
        assertTrue(fixture.unrelated.resolvePhase < CfirResolvePhase.BODY_RESOLVE)
    }

    private fun snapshot(variant: CfirEnumConstructor, familySnapshot: CfirCjmpFamilySnapshot): MatchSnapshot {
        val common = familySnapshot.commonFor(variant)
        return MatchSnapshot(
            common,
            familySnapshot.typeParameterMappingFor(variant).toMap(),
            familySnapshot.mismatchKindsFor(variant),
            familySnapshot.mismatchCandidatesFor(variant),
            familySnapshot.candidateDiagnosticsFor(variant),
            common?.let(familySnapshot::specificBindingsFor).orEmpty(),
            familySnapshot.hasResolutionResult(variant),
        )
    }

    private data class EnumFixture(
        val common: CfirEnum,
        val specific: CfirEnum,
        val unrelated: CfirNamedFunction,
        private val familyAggregate: CfirCjmpFamilyAggregate,
    ) {
        val family: CfirCjmpFamilySnapshot get() = familyAggregate.matchingGroupFor(specific)
    }

    private data class MatchSnapshot(
        val common: CfirDeclaration?,
        val typeParameterMapping: Map<CfirTypeParameter, CfirTypeParameter>,
        val mismatchKinds: List<CjmpMismatchKind>,
        val mismatchCandidates: List<CfirDeclaration>,
        val candidateDiagnostics: List<CfirCjmpCandidateDiagnostic>,
        val reverseBindings: List<CfirDeclaration>,
        val hasResolutionResult: Boolean,
    )
}

package org.cangnova.cangjie.analysis.low.level.api.cfir.resolve

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.collectDiagnosticsForFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.cjmpMatchFacts
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.session.CfirCjmpCandidateDiagnostic
import org.cangnova.cangjie.cfir.session.defaultCfirCjmpCommonSideFacts
import org.cangnova.cangjie.cfir.session.CfirCjmpFamilyAggregate
import org.cangnova.cangjie.cfir.session.CfirCjmpFamilySnapshot
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.cjmpFamilyAggregate
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMismatchKind
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 官方 mixed_body 探针：common 的普通非 static 函数可被 specific enum variant 实现。
 * 配对身份与最终调用身份分别断言，防止零诊断时仍调用返回 A 的 common 默认函数。
 */
class CfirCjmpMixedEnumCandidateTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/cjmpMixedEnumCandidate",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)
    override val additionalDirectives = super.additionalDirectives + CfirDiagnosticsDirectives

    @Test
    fun variantMatchesCommonFunction(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val modules = testServices.cjTestModuleStructure.mainModules
        assertEquals(2, modules.size)
        // 检查模块工厂传入的编译模式，不解析父 enum 或 common 函数。
        for (module in modules) {
            val psi = module.cjFiles.first()
            val raw = psi.getOrBuildCfirFile(psi.getResolutionFacadeForTest())
            assertEquals(
                module.testModule.directives[CfirDiagnosticsDirectives.CJMP_MODE].single(),
                raw.moduleData.session.cjmpSettings.mode,
                "${module.name}: session 必须消费模块配置的 CJMP 模式",
            )
        }
        val commonPsi = modules.single { it !== mainModule }.cjFiles.single()
        val commonEnum = commonPsi.getOrBuildCfirFile(commonPsi.getResolutionFacadeForTest())
            .declarations.filterIsInstance<CfirEnum>().single()
        val commonFunction = commonEnum.declarations.filterIsInstance<CfirNamedFunction>().single()
        assertEquals("Value", commonFunction.name.asString())
        assertFalse(commonFunction.status.isStatic, "the successful official fixture has no static modifier")
        assertTrue(commonFunction.status.isCommon)
        val specificPsi = mainModule.cjFiles.single { it !== mainFile }
        val specificFile = specificPsi.getOrBuildCfirFile(specificPsi.getResolutionFacadeForTest())
        val specificEnum = specificFile.declarations.filterIsInstance<CfirEnum>().single()
        val specificVariant = specificEnum.declarations.filterIsInstance<CfirEnumConstructor>()
            .single { it.name.asString() == "Value" }
        val callerFacade = mainFile.getResolutionFacadeForTest()
        val callerFile = mainFile.getOrBuildCfirFile(callerFacade)
        val caller = callerFile.declarations.filterIsInstance<CfirNamedFunction>().single()
        val familyAggregate = specificFile.moduleData.session.cjmpFamilyAggregate

        assertNull(specificEnum.cjmpMatchFacts)
        assertTrue(commonEnum.resolvePhase < CfirResolvePhase.CJMP_MATCHING)
        assertTrue(commonFunction.resolvePhase < CfirResolvePhase.CJMP_MATCHING)
        assertTrue(specificEnum.resolvePhase < CfirResolvePhase.CJMP_MATCHING)
        assertTrue(specificVariant.resolvePhase < CfirResolvePhase.CJMP_MATCHING)
        assertTrue(commonFunction.body is CfirLazyBlock)
        assertTrue(caller.body is CfirLazyBlock)

        // 第一个语义请求直接落在 variant，不能先把 common 变成函数调用目标来促成配对。
        specificVariant.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        val family = familyAggregate.matchingGroupFor(specificVariant)
        assertSame(commonEnum, family.commonFor(specificEnum))
        assertTrue(family.hasResolutionResult(specificVariant))
        assertSame(commonFunction, family.commonFor(specificVariant), "enum variant must match the ordinary common function")
        assertEquals(listOf(specificVariant), family.specificBindingsFor(commonFunction))
        assertTrue(commonFunction.body is CfirLazyBlock, "signature matching must not resolve the common implementation")
        assertTrue(caller.body is CfirLazyBlock, "matching must not prewarm the caller")

        val diagnostics = mainFile.collectDiagnosticsForFile(callerFacade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
        assertEquals(emptyList<String>(), diagnostics.map { it.factoryName }.sorted())
        val calls = mutableListOf<CfirFunctionCall>()
        requireNotNull(caller.body).accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) = element.acceptChildren(this)
            override fun visitFunctionCall(functionCall: CfirFunctionCall) {
                calls += functionCall
                functionCall.acceptChildren(this)
            }
        })
        val call = calls.single()
        val target = (call.calleeReference as CfirResolvedNamedReference).resolvedSymbol
        assertSame(specificVariant.symbol, target, "E.Value(1) must construct the specific variant")
        assertNotSame(commonFunction.symbol, target, "the common default implementation would incorrectly return A")
        assertTrue(commonFunction.body is CfirLazyBlock)

        specificVariant.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        val repeated = mainFile.collectDiagnosticsForFile(callerFacade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
        assertEquals(emptyList<String>(), repeated.map { it.factoryName }.sorted())
        assertSame(commonFunction, family.commonFor(specificVariant))
        assertEquals(listOf(specificVariant), family.specificBindingsFor(commonFunction))
        assertSame(specificVariant.symbol, (call.calleeReference as CfirResolvedNamedReference).resolvedSymbol)
        assertTrue(commonFunction.body is CfirLazyBlock)
    }

    @Test
    fun functionMatchesCommonVariant(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val fixture = prepare(mainModule, testServices)
        val common = fixture.common.declarations.filterIsInstance<CfirEnumConstructor>()
            .single { it.name.asString() == "Value" }
        val specific = fixture.specific.declarations.filterIsInstance<CfirNamedFunction>().single()
        val caller = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
            .declarations.filterIsInstance<CfirNamedFunction>().single()
        assertFalse(specific.status.isStatic)
        assertTrue(specific.body is CfirLazyBlock)
        assertTrue(caller.body is CfirLazyBlock)

        // 反向第一次请求仍落在真实 specific 函数；不先解析 common variant 或调用者。
        specific.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        assertSame(common, fixture.family.commonFor(specific)) {
            "first CJMP request must bind the real function; " +
                    "parent=${fixture.family.commonFor(fixture.specific)}, " +
                    "phases=${fixture.specific.resolvePhase}/${specific.resolvePhase}/${common.resolvePhase}, " +
                    "roles=${fixture.specific.status}/${specific.status}/${common.status}, " +
                    "result=${fixture.family.hasResolutionResult(specific)}, " +
                    "unmatched=${fixture.family.isUnmatched(specific)}, " +
                    "kinds=${fixture.family.mismatchKindsFor(specific)}, " +
                    "candidates=${fixture.family.mismatchCandidatesFor(specific)}, " +
                    "events=${fixture.family.candidateDiagnosticsFor(specific)}, " +
                    "returns=${specific.returnTypeRef}/${common.returnTypeRef}, " +
                    "parameters=${specific.valueParameters.map { it.returnTypeRef }}/" +
                    "${common.valueParameters.map { it.returnTypeRef }}, " +
                    "sessions=${specific.moduleData.session === fixture.specific.moduleData.session}"
        }
        assertEquals(listOf(specific), fixture.family.specificBindingsFor(common))
        assertTrue(specific.body is CfirLazyBlock)
        assertTrue(caller.body is CfirLazyBlock)
        assertUnrelatedBodyIsLazy(fixture)

        assertEquals(emptyList<String>(), diagnosticNames(mainFile))
        val call = functionCalls(caller).single()
        assertSame(specific.symbol, (call.calleeReference as CfirResolvedNamedReference).resolvedSymbol)
        assertNotSame(common.symbol, (call.calleeReference as CfirResolvedNamedReference).resolvedSymbol)
        // 官方 reverse 的 E.Value 是非法实例成员访问；真实合法调用是 e.Value(1)。
        assertStableMatch(specific, fixture.family)
        assertEquals(emptyList<String>(), diagnosticNames(mainFile))
        assertSame(specific.symbol, (call.calleeReference as CfirResolvedNamedReference).resolvedSymbol)
        assertUnrelatedBodyIsLazy(fixture)
    }

    @Test
    fun nonExhaustiveNamedVariant(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        assertNonExhaustiveNamedMismatch(mainFile, mainModule, testServices, commonRequiresSpecific = true)
    }

    @Test
    fun nonExhaustiveDefaultVariant(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        assertNonExhaustiveNamedMismatch(mainFile, mainModule, testServices, commonRequiresSpecific = false)
    }

    @Test
    fun nonExhaustiveReverseNamedDefault(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        assertNonExhaustiveNamedMismatch(mainFile, mainModule, testServices, commonRequiresSpecific = true)
    }

    @Test
    fun nonExhaustiveUnmatchedFunction(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val fixture = prepare(mainModule, testServices)
        val specific = fixture.specific.declarations.filterIsInstance<CfirNamedFunction>().single()
        assertTrue(fixture.common.isNonExhaustive)
        assertTrue(specific.body is CfirLazyBlock)
        specific.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        assertTrue(fixture.family.hasResolutionResult(specific))
        assertNull(fixture.family.commonFor(specific), "non-exhaustiveness must not manufacture a binding")
        assertTrue(specific.body is CfirLazyBlock)
        assertEquals(emptyList<String>(), diagnosticNames(mainFile), "ordinary specific functions share the outer-enum exemption")
        assertStableMatch(specific, fixture.family)
        assertUnrelatedBodyIsLazy(fixture)
    }

    @Test
    fun mixedModifier(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val fixture = prepare(mainModule, testServices)
        val common = fixture.common.declarations.filterIsInstance<CfirNamedFunction>().single()
        val specific = fixture.specific.declarations.filterIsInstance<CfirEnumConstructor>()
            .single { it.name.asString() == "Value" }
        specific.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        assertSame(common, fixture.family.commonFor(specific), "static mismatch must not reject a mixed callable candidate")
        assertEquals(listOf(specific), fixture.family.specificBindingsFor(common))
        assertEquals(listOf("SPECIFIC_HAS_DIFFERENT_MODIFIER"), diagnosticNames(mainFile))
        assertStableMatch(specific, fixture.family)
        assertSame(common, fixture.family.commonFor(specific))
        assertUnrelatedBodyIsLazy(fixture)
    }

    @Test
    fun duplicateVariantsKind(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val fixture = prepare(mainModule, testServices)
        val common = fixture.common.declarations.filterIsInstance<CfirEnumConstructor>().single()
        val variants = fixture.specific.declarations.filterIsInstance<CfirEnumConstructor>()
        assertEquals(2, variants.size)
        // 先请求第二项；组求值按声明顺序得出占用事实，但只发布当前锁定声明的结果。
        variants.last().lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        assertNull(variants.first().cjmpMatchFacts, "family preparation must not publish an untargeted sibling root")
        assertTrue(variants.first().resolvePhase < CfirResolvePhase.CJMP_MATCHING)
        val family = fixture.family
        assertSame(common, family.commonFor(variants.first()))
        assertNull(family.commonFor(variants.last()))
        assertEquals(listOf(CjmpMismatchKind.SECOND_BINDING), family.mismatchKindsFor(variants.last()))
        val occupied = family.candidateDiagnosticsFor(variants.last())
            .filterIsInstance<CfirCjmpCandidateDiagnostic.Occupied>().single()
        assertSame(common, occupied.commonCandidate)
        assertSame(variants.first(), occupied.previousSpecific)
        assertTrue("NOT_MATCHED" in diagnosticNames(mainFile), "the exhaustive duplicate payload remains unmatched")
        assertSame(common, family.commonFor(variants.first()))
        assertEquals(listOf(variants.last()), defaultCfirCjmpCommonSideFacts.occupiedSpecificDeclarations(common, family))
        assertStableMatch(variants.last(), family)
        assertUnrelatedBodyIsLazy(fixture)
    }

    @Test
    fun mixedDuplicateKind(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val fixture = prepare(mainModule, testServices)
        val common = fixture.common.declarations.filterIsInstance<CfirEnumConstructor>()
            .single { it.name.asString() == "Value" }
        val variant = fixture.specific.declarations.filterIsInstance<CfirEnumConstructor>()
            .single { it.name.asString() == "Value" }
        val function = fixture.specific.declarations.filterIsInstance<CfirNamedFunction>().single()
        function.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        assertNull(variant.cjmpMatchFacts, "family preparation must not publish an untargeted variant root")
        assertTrue(variant.resolvePhase < CfirResolvePhase.CJMP_MATCHING)
        val family = fixture.family
        assertSame(common, family.commonFor(variant))
        assertNull(family.commonFor(function))
        assertTrue(function.body is CfirLazyBlock)
        val occupied = family.candidateDiagnosticsFor(function)
            .filterIsInstance<CfirCjmpCandidateDiagnostic.Occupied>().single()
        assertSame(common, occupied.commonCandidate)
        assertSame(variant, occupied.previousSpecific)
        diagnosticNames(mainFile)
        assertSame(common, family.commonFor(variant))
        assertEquals(listOf(function), defaultCfirCjmpCommonSideFacts.occupiedSpecificDeclarations(common, family),
            "the occupied mixed FUNC event must retain its specific declaration")
        assertStableMatch(function, family)
        assertUnrelatedBodyIsLazy(fixture)
    }

    /** 参数失败先重放，非穷尽只抑制 specific NOT_MATCHED；common 要求由独立事实判定。 */
    private fun assertNonExhaustiveNamedMismatch(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
        commonRequiresSpecific: Boolean,
    ) {
        val fixture = prepare(mainModule, testServices)
        assertTrue(fixture.common.isNonExhaustive)
        val specific = fixture.specific.declarations.filterIsInstance<CfirCallableDeclaration>()
            .single { it.symbol.name.asString() == "Value" }
        val common = fixture.common.declarations.filterIsInstance<CfirCallableDeclaration>()
            .single { it.symbol.name.asString() == "Value" }
        specific.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        assertTrue(fixture.family.hasResolutionResult(specific))
        assertNull(fixture.family.commonFor(specific))
        val mismatch = fixture.family.candidateDiagnosticsFor(specific)
            .filterIsInstance<CfirCjmpCandidateDiagnostic.ParameterMismatch>().single().mismatch
        assertEquals(CjmpMismatchKind.PARAMETER_NAMES, mismatch.kind)
        assertEquals(0, mismatch.parameterIndex)
        assertUnrelatedBodyIsLazy(fixture)
        val commonFile = testServices.cjTestModuleStructure.mainModules.single { it !== mainModule }.cjFiles.single()
        val callerFacade = mainFile.getResolutionFacadeForTest()
        val specificDiagnostics = mainFile.collectDiagnosticsForFile(
            callerFacade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS,
        )
        val commonDiagnostics = commonFile.collectDiagnosticsForFile(
            callerFacade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS,
        )
        val specificNames = specificDiagnostics.map { it.factoryName.removePrefix("CFIR_") }.sorted()
        val commonNames = commonDiagnostics.map { it.factoryName.removePrefix("CFIR_") }.sorted()
        assertTrue(specificDiagnostics.all { it.psiElement.containingFile !== commonFile })
        assertTrue(commonDiagnostics.all { it.psiElement.containingFile === commonFile })
        val diagnostics = specificDiagnostics + commonDiagnostics
        assertEquals(listOf("SPECIFIC_HAS_DIFFERENT_PARAMETER"), specificNames)
        assertFalse("NOT_MATCHED" in specificNames, "outer non-exhaustiveness exempts only the specific unmatched diagnostic")
        assertEquals(if (commonRequiresSpecific) listOf("NOT_MATCHED") else emptyList<String>(), commonNames)
        val names = diagnostics.map { it.factoryName.removePrefix("CFIR_") }.sorted()
        assertEquals((specificNames + commonNames).sorted(), names, "no diagnostic may be omitted by file partitioning")
        // 文件诊断包含 common 方向检查产生的跨文件锚点，两侧结论还须与家族事实一致。
        val family = fixture.family
        assertEquals(commonRequiresSpecific, defaultCfirCjmpCommonSideFacts.mustReportNotMatched(common, fixture.common, family))
        assertStableMatch(specific, family)
        assertEquals(specificNames, diagnosticNames(mainFile))
        assertUnrelatedBodyIsLazy(fixture)
    }

    /** raw 构建与模块模式检查不推进 BODY，也不先请求父 enum 的配对。 */
    private fun prepare(mainModule: CjTestModule, testServices: TestServices): MixedFixture {
        val modules = testServices.cjTestModuleStructure.mainModules
        assertEquals(2, modules.size)
        for (module in modules) {
            val psi = module.cjFiles.first()
            val raw = psi.getOrBuildCfirFile(psi.getResolutionFacadeForTest())
            assertEquals(
                module.testModule.directives[CfirDiagnosticsDirectives.CJMP_MODE].single(),
                raw.moduleData.session.cjmpSettings.mode,
                "${module.name}: session 必须消费模块配置的 CJMP 模式",
            )
        }
        val commonPsi = modules.single { it !== mainModule }.cjFiles.single()
        val common = commonPsi.getOrBuildCfirFile(commonPsi.getResolutionFacadeForTest())
            .declarations.filterIsInstance<CfirEnum>().single()
        val specificPsi = mainModule.cjFiles.single { it.name == "specific.cj" }
        val specificFile = specificPsi.getOrBuildCfirFile(specificPsi.getResolutionFacadeForTest())
        val specific = specificFile.declarations.filterIsInstance<CfirEnum>().single()
        val unrelatedPsi = mainModule.cjFiles.single { it.name == "unrelated.cj" }
        val unrelated = unrelatedPsi.getOrBuildCfirFile(unrelatedPsi.getResolutionFacadeForTest())
            .declarations.filterIsInstance<CfirNamedFunction>().single()
        val familyAggregate = specificFile.moduleData.session.cjmpFamilyAggregate
        assertNull(specific.cjmpMatchFacts)
        assertTrue(common.resolvePhase < CfirResolvePhase.CJMP_MATCHING)
        assertTrue(specific.resolvePhase < CfirResolvePhase.CJMP_MATCHING)
        specific.declarations.filterIsInstance<CfirCallableDeclaration>().forEach {
            assertTrue(it.resolvePhase < CfirResolvePhase.CJMP_MATCHING)
        }
        return MixedFixture(common, specific, unrelated, familyAggregate).also(::assertUnrelatedBodyIsLazy)
    }

    private fun diagnosticNames(file: CjFile): List<String> = file.collectDiagnosticsForFile(
        file.getResolutionFacadeForTest(), DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS,
    ).map { it.factoryName.removePrefix("CFIR_") }.sorted()

    private fun functionCalls(function: CfirNamedFunction): List<CfirFunctionCall> = buildList {
        requireNotNull(function.body).accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) = element.acceptChildren(this)
            override fun visitFunctionCall(functionCall: CfirFunctionCall) {
                add(functionCall)
                functionCall.acceptChildren(this)
            }
        })
    }

    private fun assertStableMatch(specific: CfirDeclaration, family: CfirCjmpFamilySnapshot) {
        val common = family.commonFor(specific)
        val diagnostics = family.candidateDiagnosticsFor(specific)
        val mismatches = family.mismatchKindsFor(specific)
        repeat(2) { specific.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING) }
        assertSame(common, family.commonFor(specific))
        assertEquals(diagnostics, family.candidateDiagnosticsFor(specific))
        assertEquals(mismatches, family.mismatchKindsFor(specific))
        assertTrue(family.hasResolutionResult(specific))
    }

    private fun assertUnrelatedBodyIsLazy(fixture: MixedFixture) {
        assertTrue(fixture.unrelated.body is CfirLazyBlock)
        assertTrue(fixture.unrelated.resolvePhase < CfirResolvePhase.BODY_RESOLVE)
    }

    private data class MixedFixture(
        val common: CfirEnum,
        val specific: CfirEnum,
        val unrelated: CfirNamedFunction,
        private val familyAggregate: CfirCjmpFamilyAggregate,
    ) {
        val family: CfirCjmpFamilySnapshot get() = familyAggregate.matchingGroupFor(specific)
    }
}

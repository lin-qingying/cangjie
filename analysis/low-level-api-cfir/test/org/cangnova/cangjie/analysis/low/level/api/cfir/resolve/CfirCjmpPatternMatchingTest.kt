package org.cangnova.cangjie.analysis.low.level.api.cfir.resolve

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirResolvedDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.cjmpMatchFacts
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.patterns.CfirTuplePattern
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.session.CfirCjmpFamilyAggregate
import org.cangnova.cangjie.cfir.session.CfirCjmpFamilySnapshot
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.resolve.providers.cfirProvider
import org.cangnova.cangjie.cfir.session.cjmpFamilyAggregate
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 从真实 PSI 的绑定符号首次请求 CJMP，验证声明归属与官方 tuple 逐项停止规则。
 * root 是声明容器，成功项必须映射原始绑定实体，不能合成一个 common 对位替代整个 tuple。
 */
class CfirCjmpPatternMatchingTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/cjmpPatternMatching",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)
    override val additionalDirectives = super.additionalDirectives + CfirDiagnosticsDirectives

    @Test
    fun ordinaryBindingFirst(mainModule: CjTestModule, testServices: TestServices) {
        val fixture = prepare(mainModule, testServices)
        assertTrue(fixture.specific.pattern is CfirBindingPattern)
        val binding = fixture.binding("value")
        val rootInitializer = fixture.specific.initializer
        val bindingInitializer = binding.initializer
        binding.lazyResolveToPhase(CfirResolvePhase.TYPES)
        assertTrue(fixture.specific.returnTypeRef is CfirResolvedTypeRef)
        assertTrue(binding.returnTypeRef is CfirResolvedTypeRef)
        assertEquals(ConePrimitiveType.INT64, fixture.specific.returnTypeRef.coneTypeOrNull)
        assertEquals(fixture.specific.returnTypeRef.coneTypeOrNull, binding.returnTypeRef.coneTypeOrNull)
        assertSame(binding, fixture.binding("value"))
        assertSame(fixture.specific, binding.moduleData.session.cfirProvider.getCfirPatternVariableForBinding(binding.symbol))
        assertSame(rootInitializer, fixture.specific.initializer, "header resolution must preserve the lazy initializer")
        assertSame(bindingInitializer, binding.initializer)
        assertTrue(fixture.specific.resolvePhase < CfirResolvePhase.BODY_RESOLVE)
        assertTrue(fixture.unrelated.body is CfirLazyBlock)
        assertNull(fixture.specific.cjmpMatchFacts)
        binding.lazyResolveToPhase(CfirResolvePhase.STATUS)
        assertTrue(fixture.specific.status is CfirResolvedDeclarationStatus)
        for (owned in fixture.specific.pattern.bindingVariables()) {
            assertTrue(owned.status is CfirResolvedDeclarationStatus)
        }
        assertSame(rootInitializer, fixture.specific.initializer)
        assertNull(fixture.specific.cjmpMatchFacts)
        binding.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        assertSame(fixture.specific, binding.moduleData.session.cfirProvider.getCfirPatternVariableForBinding(binding.symbol))
        assertBound(fixture, "value")
        assertTrue(fixture.family.hasResolutionResult(fixture.specific))
        assertFalse(fixture.family.isUnmatched(fixture.specific))
        assertTrue(fixture.specific.resolvePhase >= CfirResolvePhase.CJMP_MATCHING)
        assertNull(fixture.family.commonFor(fixture.specific), "ordinary source root also publishes only its true binding")
        assertStable(fixture)
    }

    @Test
    fun tupleBindingFirst(mainModule: CjTestModule, testServices: TestServices) {
        val fixture = prepare(mainModule, testServices)
        assertTrue(fixture.specific.pattern is CfirTuplePattern)
        // 第二项先请求，仍须一次处理真正 root 的完整直接元素，而不是只处理该项。
        fixture.binding("b").lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        assertBound(fixture, "a")
        assertBound(fixture, "b")
        assertTrue(fixture.family.hasResolutionResult(fixture.specific))
        assertFalse(fixture.family.isUnmatched(fixture.specific))
        assertNull(fixture.family.commonFor(fixture.specific), "tuple has no single common declaration counterpart")
        assertStable(fixture)
    }

    @Test
    fun explicitTupleBindingTypesFirst(mainModule: CjTestModule, testServices: TestServices) {
        val fixture = prepare(mainModule, testServices)
        val originalBindings = fixture.specific.pattern.bindingVariables()
        val initializer = fixture.specific.initializer
        fixture.binding("b").lazyResolveToPhase(CfirResolvePhase.TYPES)
        assertTrue(fixture.specific.returnTypeRef is CfirResolvedTypeRef)
        for ((index, binding) in fixture.specific.pattern.bindingVariables().withIndex()) {
            assertSame(originalBindings[index], binding)
            assertTrue(binding.returnTypeRef is CfirResolvedTypeRef)
            assertEquals(ConePrimitiveType.INT64, binding.returnTypeRef.coneTypeOrNull)
            assertSame(fixture.specific, binding.moduleData.session.cfirProvider.getCfirPatternVariableForBinding(binding.symbol))
        }
        assertSame(initializer, fixture.specific.initializer)
        assertTrue(fixture.specific.resolvePhase < CfirResolvePhase.BODY_RESOLVE)
        assertTrue(fixture.unrelated.body is CfirLazyBlock)
        assertNull(fixture.specific.cjmpMatchFacts)
        fixture.binding("b").lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        assertBound(fixture, "a")
        assertBound(fixture, "b")
        assertStable(fixture)
    }

    @Test
    fun ordinaryEnumBindingImplicit(mainModule: CjTestModule) {
        val psi = mainModule.cjFiles.single()
        val file = psi.getOrBuildCfirFile(psi.getResolutionFacadeForTest())
        val root = file.declarations.filterIsInstance<CfirPatternVariable>().single()
        val binding = root.pattern.bindingVariables().single()
        val unrelated = file.declarations.filterIsInstance<CfirNamedFunction>().single()
        assertTrue(root.resolvePhase < CfirResolvePhase.IMPLICIT_TYPES)
        assertTrue(unrelated.body is CfirLazyBlock)
        // 显式 enum root 与省略类型的 child 并存，不能只检查 root 的 isTypeImplicit 后早退。
        binding.lazyResolveToPhase(CfirResolvePhase.IMPLICIT_TYPES)
        assertSame(root, binding.moduleData.session.cfirProvider.getCfirPatternVariableForBinding(binding.symbol))
        assertSame(binding, root.pattern.bindingVariables().single())
        assertTrue(root.returnTypeRef is CfirResolvedTypeRef)
        assertTrue(binding.returnTypeRef is CfirResolvedTypeRef)
        assertEquals(ConePrimitiveType.INT64, binding.returnTypeRef.coneTypeOrNull)
        assertTrue(binding.resolvePhase >= CfirResolvePhase.IMPLICIT_TYPES)
        assertTrue(unrelated.body is CfirLazyBlock)
        assertTrue(unrelated.resolvePhase < CfirResolvePhase.BODY_RESOLVE)
        binding.lazyResolveToPhase(CfirResolvePhase.IMPLICIT_TYPES)
        assertEquals(ConePrimitiveType.INT64, binding.returnTypeRef.coneTypeOrNull)
    }

    @Test
    fun failureContinues(mainModule: CjTestModule, testServices: TestServices) {
        val fixture = prepare(mainModule, testServices)
        fixture.binding("missing").lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        assertTrue(fixture.family.hasResolutionResult(fixture.binding("missing")))
        assertTrue(fixture.family.isUnmatched(fixture.binding("missing")))
        assertNull(fixture.family.commonFor(fixture.binding("missing")))
        assertBound(fixture, "b")
        assertFailedRoot(fixture)
        assertStable(fixture)
    }

    @Test
    fun nestedStops(mainModule: CjTestModule, testServices: TestServices) {
        val fixture = prepare(mainModule, testServices)
        // 被停止的内层绑定也能请求其真实 root，但不能让匹配器扁平化继续。
        fixture.binding("c").lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        assertBound(fixture, "a")
        assertStopped(fixture, "b")
        assertStopped(fixture, "c")
        assertFailedRoot(fixture)
        assertStable(fixture)
    }

    @Test
    fun wildcardStops(mainModule: CjTestModule, testServices: TestServices) {
        val fixture = prepare(mainModule, testServices)
        fixture.binding("b").lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        assertBound(fixture, "a")
        assertStopped(fixture, "b")
        assertFailedRoot(fixture)
        assertStable(fixture)
    }

    private fun assertFailedRoot(fixture: PatternFixture) {
        assertTrue(fixture.family.hasResolutionResult(fixture.specific))
        assertTrue(fixture.family.isUnmatched(fixture.specific))
        assertNull(fixture.family.commonFor(fixture.specific))
    }

    private fun assertBound(fixture: PatternFixture, name: String) {
        val binding = fixture.binding(name)
        val common = fixture.commonBindings.getValue(name)
        assertTrue(fixture.family.hasResolutionResult(binding))
        assertSame(common, fixture.family.commonFor(binding))
        assertEquals(listOf(binding), fixture.family.specificBindingsFor(common))
        assertTrue(binding.resolvePhase >= CfirResolvePhase.CJMP_MATCHING)
    }

    private fun assertStopped(fixture: PatternFixture, name: String) {
        val binding = fixture.binding(name)
        assertTrue(binding.resolvePhase >= CfirResolvePhase.CJMP_MATCHING, "phase tracks the containing declaration")
        assertFalse(fixture.family.hasResolutionResult(binding), "a stopped element has no candidate visit")
        assertNull(fixture.family.commonFor(binding))
        assertEquals(emptyList<CfirPatternBindingVariable>(), fixture.family.specificBindingsFor(fixture.commonBindings.getValue(name)))
    }

    /** 只构建 raw 文件并检查模块模式，不预热 common、root、绑定或无关函数。 */
    private fun prepare(mainModule: CjTestModule, testServices: TestServices): PatternFixture {
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
        val commonFile = commonPsi.getOrBuildCfirFile(commonPsi.getResolutionFacadeForTest())
        val commonRoots = commonFile.declarations.filterIsInstance<CfirPatternVariable>()
        val commonBindings = commonRoots.flatMap { it.pattern.bindingVariables() }.associateBy { it.name.asString() }
        assertTrue(commonBindings.isNotEmpty())
        val specificPsi = mainModule.cjFiles.single { it.name == "specific.cj" }
        val specificFile = specificPsi.getOrBuildCfirFile(specificPsi.getResolutionFacadeForTest())
        val specific = specificFile.declarations.filterIsInstance<CfirPatternVariable>().single()
        val unrelatedPsi = mainModule.cjFiles.single { it.name == "unrelated.cj" }
        val unrelated = unrelatedPsi.getOrBuildCfirFile(unrelatedPsi.getResolutionFacadeForTest())
            .declarations.filterIsInstance<CfirNamedFunction>().single()
        val familyAggregate = specificFile.moduleData.session.cjmpFamilyAggregate
        assertNull(specific.cjmpMatchFacts)
        assertTrue(specific.resolvePhase < CfirResolvePhase.CJMP_MATCHING)
        for (root in commonRoots) assertTrue(root.resolvePhase < CfirResolvePhase.CJMP_MATCHING)
        for (binding in specific.pattern.bindingVariables()) {
            assertTrue(binding.resolvePhase < CfirResolvePhase.CJMP_MATCHING)
            assertSame(specific, binding.moduleData.session.cfirProvider.getCfirPatternVariableForBinding(binding.symbol))
        }
        assertTrue(unrelated.body is CfirLazyBlock)
        return PatternFixture(specific, commonBindings, unrelated, familyAggregate)
    }

    /** 重复 root 和绑定请求不新增事件、不改变部分成功，也不解析无关函数体。 */
    private fun assertStable(fixture: PatternFixture) {
        val bindings = fixture.specific.pattern.bindingVariables()
        val mappings = bindings.map { fixture.family.commonFor(it) }
        val results = bindings.map { fixture.family.hasResolutionResult(it) }
        val events = bindings.map { fixture.family.candidateDiagnosticsFor(it) }
        fixture.specific.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        for (binding in bindings) binding.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        assertEquals(mappings, bindings.map { fixture.family.commonFor(it) })
        assertEquals(results, bindings.map { fixture.family.hasResolutionResult(it) })
        assertEquals(events, bindings.map { fixture.family.candidateDiagnosticsFor(it) })
        val snapshot = fixture.family
        assertNull(snapshot.commonFor(fixture.specific))
        for ((index, binding) in bindings.withIndex()) {
            assertTrue(snapshot.specificDeclarations.any { it === binding }, "snapshot must retain the original child declaration")
            assertSame(mappings[index], snapshot.commonFor(binding), "snapshot must project the true binding mapping")
            assertEquals(results[index], snapshot.hasResolutionResult(binding))
        }
        assertTrue(fixture.unrelated.body is CfirLazyBlock)
        assertTrue(fixture.unrelated.resolvePhase < CfirResolvePhase.BODY_RESOLVE)
    }

    private data class PatternFixture(
        val specific: CfirPatternVariable,
        val commonBindings: Map<String, CfirPatternBindingVariable>,
        val unrelated: CfirNamedFunction,
        private val familyAggregate: CfirCjmpFamilyAggregate,
    ) {
        val family: CfirCjmpFamilySnapshot get() = familyAggregate.matchingGroupFor(specific)
        fun binding(name: String): CfirPatternBindingVariable = specific.pattern.bindingVariables().single { it.name.asString() == name }
    }
}

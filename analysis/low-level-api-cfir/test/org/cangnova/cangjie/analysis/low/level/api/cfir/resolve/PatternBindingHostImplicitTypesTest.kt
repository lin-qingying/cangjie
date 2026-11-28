package org.cangnova.cangjie.analysis.low.level.api.cfir.resolve

import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.CfirLazyBodiesCalculator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.cfir.CfirElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchBranch
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirPatternTreeNode
import org.cangnova.cangjie.cfir.patterns.CfirPatternTreeOwner
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.isString
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjMatchEntry
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjVarOrEnumPattern
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * 验证局部真实绑定的 leaf 请求沿包含声明 BODY_RESOLVE 路径完成解析。
 *
 * 每次参数化调用创建独立环境，准备阶段仅重建 raw 函数体，不执行声明或表达式解析。
 * 错误 selector 下全部 active binding 写入原错误类型，是本项目的错误收敛契约；
 * 不将官方遇到错误后的短路行为解释为官方逐叶完成了错误类型写入。
 */
class PatternBindingHostImplicitTypesTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/patternBindingHostImplicitTypes",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    /** 局部 owner 有显式嵌套 tuple 类型时，仍通过真实 leaf 首查推进包含函数。 */
    @ParameterizedTest(name = "{0} first")
    @ValueSource(strings = ["number", "flag", "text"])
    fun localExplicitOwner(firstBinding: String, mainFile: CjFile) {
        val function = buildRawFunctionBody(mainFile)
        val owner = function.body!!.statements.filterIsInstance<CfirPatternVariable>().single()
        val rawBindings = owner.pattern.bindingVariables()
        val target = rawBindings.single { it.name.asString() == firstBinding }

        assertTrue(owner.isLocal)
        assertEquals(CfirResolvePhase.RAW_CFIR, owner.resolvePhase)
        assertFalse(owner.returnTypeRef is CfirImplicitTypeRef, "raw local owner 必须保留显式类型注解。")
        assertFalse(owner.returnTypeRef is CfirResolvedTypeRef, "准备阶段不得预先解析局部 owner 类型。")
        assertEquals(listOf("number", "flag", "text"), rawBindings.map { it.name.asString() })
        assertRawBindings(owner, owner.pattern, rawBindings)

        target.lazyResolveToPhase(CfirResolvePhase.IMPLICIT_TYPES)

        assertTrue(function.resolvePhase >= CfirResolvePhase.BODY_RESOLVE, "局部 leaf 首查必须解析包含函数。")
        assertTrue(target.resolvePhase >= CfirResolvePhase.IMPLICIT_TYPES)
        assertTrue(owner.resolvePhase >= CfirResolvePhase.BODY_RESOLVE)
        assertSame(owner, function.body!!.statements.filterIsInstance<CfirPatternVariable>().single())
        assertTrue(owner.returnTypeRef is CfirResolvedTypeRef)
        assertTrue((owner.returnTypeRef as CfirResolvedTypeRef).coneType is ConeTupleType)

        val bindings = assertBindingIdentity(owner, owner.pattern, rawBindings)
        assertEquals(ConePrimitiveType.INT64, resolvedBindingType(bindings[0]))
        assertEquals(ConePrimitiveType.BOOLEAN, resolvedBindingType(bindings[1]))
        assertTrue(resolvedBindingType(bindings[2]).isString, "显式嵌套分量必须解析为 String。")
    }

    /** 成功的嵌套 enum payload 每叶冷首查；包含函数的 BODY_RESOLVE 只能由该 leaf 路径触发。 */
    @OptIn(LLCfirInternals::class)
    @ParameterizedTest(name = "{0} cold first")
    @ValueSource(strings = ["serial", "flag", "tail"])
    fun matchEnumPayloadCold(firstMarker: String, mainFile: CjFile, services: TestServices) {
        val markers = listOf("serial", "flag", "tail")
        val canonical = markers.map {
            services.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<CjVarOrEnumPattern>(mainFile, it)
        }
        val selectedPsi = services.expressionMarkerProvider
            .getBottommostElementOfTypeAtCaret<CjVarOrEnumPattern>(mainFile, firstMarker)
        val functionPsi = requireNotNull(PsiTreeUtil.getParentOfType(selectedPsi, CjNamedFunction::class.java))
        val cfirFile = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
        val function = cfirFile.declarations.filterIsInstance<CfirNamedFunction>()
            .single { it.source?.psi === functionPsi }
        assertEquals(CfirResolvePhase.RAW_CFIR, function.resolvePhase)
        CfirLazyBodiesCalculator.calculateBodies(CfirDesignation(listOf(cfirFile), function))
        assertEquals(CfirResolvePhase.RAW_CFIR, function.resolvePhase, "仅重建 RAW body，不预先解析函数。")
        assertSame(functionPsi, function.source?.psi)
        assertTrue(function.body != null)
        assertFalse(function.body is CfirLazyBlock)
        val body = requireNotNull(function.body)
        val matches = mutableListOf<CfirMatchExpression>()
        body.accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (element is CfirMatchExpression) matches += element
                element.acceptChildren(this)
            }
        })
        val match = matches.single()
        val target = collectEnumImplicitBindings(body).single { it.source?.psi === selectedPsi }
        val owner = target.patternTreeOwner as CfirMatchBranch
        val rawPattern = owner.pattern
        val rawBindings = rawPattern.bindingVariables()
        val enum = cfirFile.declarations.filterIsInstance<CfirEnum>().single()

        assertSame(owner, match.branches.single { it === owner })
        assertSame(PsiTreeUtil.getParentOfType(selectedPsi, CjMatchEntry::class.java), owner.source?.psi)
        assertSame(selectedPsi, target.source?.psi)
        assertSame(target, rawBindings[markers.indexOf(firstMarker)])
        assertEquals(3, rawBindings.size)
        assertRawBindings(owner, rawPattern, rawBindings)
        rawBindings.forEachIndexed { index, binding -> assertSame(canonical[index], binding.source?.psi) }
        match.branches.forEach { branch -> assertPatternTreeOwner(branch, branch.pattern) }

        assertEnumLeafSymbols(rawBindings)
        recordEnumImplicitState("BEFORE", firstMarker, owner, target, rawBindings, function)
        try {
            target.lazyResolveToPhase(CfirResolvePhase.IMPLICIT_TYPES)
        } finally {
            recordEnumImplicitState("AFTER_REQUEST", firstMarker, owner, target, rawBindings, function)
        }

        assertTrue(target.resolvePhase >= CfirResolvePhase.IMPLICIT_TYPES)
        assertTrue(function.resolvePhase >= CfirResolvePhase.BODY_RESOLVE, "局部首查由生产路径解析包含函数。")
        assertSame(functionPsi, function.source?.psi)
        assertSame(body, function.body)
        assertSame(owner, match.branches.single { it === owner })
        val resolvedPattern = owner.pattern
        assertEnumResolvedRoot(owner, rawPattern, resolvedPattern, rawBindings)
        val bindings = assertBindingIdentity(owner, resolvedPattern, rawBindings)
        assertEnumLeafSymbols(bindings)
        assertEquals(ConePrimitiveType.INT64, resolvedBindingType(bindings[0]))
        assertEquals(ConePrimitiveType.BOOLEAN, resolvedBindingType(bindings[1]))
        assertEquals(ConePrimitiveType.INT64, resolvedBindingType(bindings[2]))
        bindings.forEachIndexed { index, binding -> assertSame(canonical[index], binding.source?.psi) }
        assertEnumImplicitType(requireNotNull(requireNotNull(match.subject).coneTypeOrNull), enum)
        assertEquals(3, match.branches.flatMap { it.pattern.bindingVariables() }.size,
            "Empty 是 constructor，不能留下第四个 active binder。")
        match.branches.forEach { branch -> assertPatternTreeOwner(branch, branch.pattern) }
        val completedTypes = bindings.map { it.returnTypeRef }
        val completedPhases = bindings.map { it.resolvePhase }

        target.lazyResolveToPhase(CfirResolvePhase.IMPLICIT_TYPES)
        val stable = assertBindingIdentity(owner, resolvedPattern, rawBindings)
        stable.forEachIndexed { index, binding ->
            assertSame(completedTypes[index], binding.returnTypeRef)
            assertEquals(completedPhases[index], binding.resolvePhase)
            assertSame(canonical[index], binding.source?.psi)
            resolvedBindingType(binding)
        }
        assertEnumLeafSymbols(stable)
        recordEnumImplicitState("REPEAT", firstMarker, owner, target, rawBindings, function)
    }

    /** selector 错误时，嵌套 tuple 的每个 active leaf 都必须携带原错误且不能停留 implicit。 */
    @ParameterizedTest(name = "{0} first")
    @ValueSource(strings = ["number", "flag", "text"])
    fun matchErrorNestedTuple(firstBinding: String, mainFile: CjFile) {
        assertMatchSelectorError(mainFile, firstBinding, listOf("number", "flag", "text"))
    }

    /** selector 错误的 enum payload 也须逐叶收敛，不能在 match early return 后遗漏 binder。 */
    @ParameterizedTest(name = "{0} first")
    @ValueSource(strings = ["payload", "flag"])
    fun matchErrorEnumPayload(firstBinding: String, mainFile: CjFile) {
        assertMatchSelectorError(mainFile, firstBinding, listOf("payload", "flag"))
    }

    /**
     * 定位 raw match 宿主，再直接请求某个叶子；函数和 selector 的解析均由生产 leaf 路径触发。
     * 诊断身份与 selector 原错误比较，避免将 match 本体的重复诊断包装当成叶子的错误来源。
     */
    private fun assertMatchSelectorError(mainFile: CjFile, firstBinding: String, expectedNames: List<String>) {
        val function = buildRawFunctionBody(mainFile)
        val match = function.body!!.statements.filterIsInstance<CfirMatchExpression>().single()
        val owner = match.branches.single { branch -> branch.pattern.bindingVariables().isNotEmpty() }
        val rawBindings = owner.pattern.bindingVariables()
        val target = rawBindings.single { it.name.asString() == firstBinding }

        assertEquals(expectedNames, rawBindings.map { it.name.asString() })
        assertRawBindings(owner, owner.pattern, rawBindings)
        match.branches.forEach { branch -> assertPatternTreeOwner(branch, branch.pattern) }

        target.lazyResolveToPhase(CfirResolvePhase.IMPLICIT_TYPES)

        assertTrue(function.resolvePhase >= CfirResolvePhase.BODY_RESOLVE, "match leaf 首查必须解析包含函数。")
        assertTrue(target.resolvePhase >= CfirResolvePhase.IMPLICIT_TYPES)
        assertSame(match, function.body!!.statements.filterIsInstance<CfirMatchExpression>().single())
        assertSame(owner, match.branches.single { branch -> branch.pattern.bindingVariables().isNotEmpty() })
        val subjectType = match.subject!!.coneTypeOrNull
        assertTrue(subjectType is ConeErrorType, "fixture 的 selector 必须产生原始错误类型。")
        val selectorError = subjectType as ConeErrorType
        val bindings = assertBindingIdentity(owner, owner.pattern, rawBindings)
        bindings.forEach { binding ->
            val type = resolvedBindingType(binding)
            assertTrue(type is ConeErrorType, "错误 selector 的 active leaf 必须收敛为 ConeErrorType：" + binding.name)
            assertSame(selectorError.diagnostic, (type as ConeErrorType).diagnostic, "叶子必须传播 selector 的原诊断。")
        }
        assertEquals(expectedNames, match.branches.flatMap { it.pattern.bindingVariables() }.map { it.name.asString() })
        match.branches.forEach { branch -> assertPatternTreeOwner(branch, branch.pattern) }
    }

    /** 仅按包含函数的 designation 重建 raw body，并以 phase 检查禁止准备阶段的语义预热。 */
    @OptIn(LLCfirInternals::class)
    private fun buildRawFunctionBody(mainFile: CjFile): CfirNamedFunction {
        val cfirFile = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
        val function = cfirFile.declarations.filterIsInstance<CfirNamedFunction>().single()
        assertEquals(CfirResolvePhase.RAW_CFIR, function.resolvePhase)
        CfirLazyBodiesCalculator.calculateBodies(CfirDesignation(listOf(cfirFile), function))
        assertEquals(CfirResolvePhase.RAW_CFIR, function.resolvePhase, "raw body 重建不得推进函数解析阶段。")
        assertTrue(function.body != null)
        assertFalse(function.body is CfirLazyBlock, "必须取得可遍历的 raw 函数体。")
        return function
    }

    /** 首查前的真实叶子必须仍在 RAW_CFIR 且为 implicit，并拥有正确宿主。 */
    private fun assertRawBindings(
        owner: CfirPatternTreeOwner,
        pattern: CfirPattern,
        bindings: List<CfirPatternBindingVariable>,
    ) {
        assertPatternTreeOwner(owner, pattern)
        bindings.forEach { binding ->
            assertTrue(binding.isLocal)
            assertEquals(CfirResolvePhase.RAW_CFIR, binding.resolvePhase)
            assertTrue(binding.returnTypeRef is CfirImplicitTypeRef, "必须直接首查尚未解析的 local leaf：" + binding.name)
            assertSame(owner, binding.patternTreeOwner)
        }
    }

    /** 宿主、模式子树及真实 binder 的身份在包含函数 BODY_RESOLVE 后必须保持一致。 */
    private fun assertBindingIdentity(
        owner: CfirPatternTreeOwner,
        pattern: CfirPattern,
        rawBindings: List<CfirPatternBindingVariable>,
    ): List<CfirPatternBindingVariable> {
        val bindings = pattern.bindingVariables()
        assertEquals(rawBindings.map { it.name.asString() }, bindings.map { it.name.asString() })
        assertPatternTreeOwner(owner, pattern)
        bindings.forEachIndexed { index, binding ->
            assertSame(rawBindings[index], binding, "BODY_RESOLVE 不得替换真实 binder 身份。")
            assertSame(owner, binding.patternTreeOwner)
        }
        return bindings
    }

    /** 全部 active leaf 都须收敛；尤其禁止发布 BODY_RESOLVE 阶段的 implicit 叶子。 */
    private fun resolvedBindingType(binding: CfirPatternBindingVariable): ConeCangJieType {
        val typeRef = binding.returnTypeRef
        assertFalse(
            binding.resolvePhase >= CfirResolvePhase.BODY_RESOLVE && typeRef is CfirImplicitTypeRef,
            "不能发布 BODY_RESOLVE + implicit 的 active binder：" + binding.name,
        )
        assertTrue(typeRef is CfirResolvedTypeRef, "active leaf 必须完成类型解析：" + binding.name)
        return (typeRef as CfirResolvedTypeRef).coneType
    }

    /** 每一层模式节点的反向链接都必须指向正向拥有该模式根的同一宿主。 */
    private fun assertPatternTreeOwner(owner: CfirPatternTreeOwner, pattern: CfirPattern) {
        assertSame(pattern, owner.patternTreeRoot())
        pattern.accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (element is CfirPatternTreeNode) {
                    assertSame(owner, element.patternTreeOwner)
                }
                element.acceptChildren(this)
            }
        })
    }
}

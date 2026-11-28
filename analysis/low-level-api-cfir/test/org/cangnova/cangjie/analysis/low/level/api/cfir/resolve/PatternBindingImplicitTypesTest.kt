package org.cangnova.cangjie.analysis.low.level.api.cfir.resolve

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.TargetSourceTestSupport
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.patterns.CfirPatternTreeNode
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhaseRecursively
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.isString
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

/**
 * 验证真实绑定从 raw CFIR 直接请求 IMPLICIT_TYPES 时的 owner 与逐叶类型契约。
 *
 * 每次参数化调用由执行基类创建独立测试环境，使每个叶子都成为首次语义请求的目标。
 * 官方 TypeCheckPattern.cpp:480 要求当前层 tuple 形状和元数完全匹配，再按位置递归，
 * 首个失败之前的成功分量保留类型。确定失败后的未完成叶子写入 ConeErrorType，
 * 属于本项目要求的错误收敛契约，不代表官方编译器逐叶写入错误类型。
 */
class PatternBindingImplicitTypesTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/patternBindingImplicitTypes",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    /** 每个顶层 tuple 叶子首查时，都必须解析出自己的分量类型并收敛整个 owner。 */
    @ParameterizedTest(name = "{0} first")
    @ValueSource(strings = ["number", "flag", "text"])
    fun globalTuple(firstBinding: String, mainFile: CjFile) {
        assertGlobalBindings(
            mainFile,
            firstBinding,
            listOf(mapOf("number" to ExpectedType.INT64, "flag" to ExpectedType.BOOL, "text" to ExpectedType.STRING)),
        )
    }

    /** 每个成功 enum payload 叶子都在独立环境中从 RAW 首查，不能由兄弟查询预热。 */
    @ParameterizedTest(name = "{0} cold first")
    @ValueSource(strings = ["serial", "flag", "tail"])
    fun globalEnumPayloadCold(firstMarker: String, mainFile: CjFile, services: TestServices) {
        val markers = listOf("serial", "flag", "tail")
        val cfirFile = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
        val sources = TargetSourceTestSupport(cfirFile, mainFile)
        val canonical = markers.map {
            val origin = services.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<CjBindingPattern>(mainFile, it)
            sources.targetElement(origin) as CjBindingPattern
        }
        val selectedPsi = canonical[markers.indexOf(firstMarker)]
        val target = collectEnumImplicitBindings(cfirFile).single { it.source?.psi === selectedPsi }
        val owner = target.patternTreeOwner as CfirPatternVariable
        val rawPattern = owner.pattern
        val rawBindings = rawPattern.bindingVariables()
        val enum = cfirFile.declarations.filterIsInstance<CfirEnum>().single()
        val expected = mapOf("serial" to ExpectedType.INT64, "flag" to ExpectedType.BOOL, "tail" to ExpectedType.INT64)

        assertSame(owner, cfirFile.declarations.filterIsInstance<CfirPatternVariable>().single())
        assertSame(PsiTreeUtil.getParentOfType(selectedPsi, CjPatternVariable::class.java), owner.source?.psi)
        assertSame(selectedPsi, target.source?.psi)
        assertSame(target, rawBindings[markers.indexOf(firstMarker)])
        assertEquals(CfirResolvePhase.RAW_CFIR, owner.resolvePhase)
        assertTrue(owner.returnTypeRef is CfirImplicitTypeRef)
        assertTrue(!owner.isLocal && rawBindings.none { it.isLocal })
        assertEquals(3, rawBindings.size)
        assertPatternTreeOwner(owner)
        rawBindings.forEachIndexed { index, binding ->
            assertEquals(CfirResolvePhase.RAW_CFIR, binding.resolvePhase)
            assertTrue(binding.returnTypeRef is CfirImplicitTypeRef)
            assertSame(canonical[index], binding.source?.psi)
            assertSame(owner, binding.patternTreeOwner)
        }

        assertEnumLeafSymbols(rawBindings)
        recordEnumImplicitState("BEFORE", firstMarker, owner, target, rawBindings)
        try {
            target.lazyResolveToPhase(CfirResolvePhase.IMPLICIT_TYPES)
        } finally {
            // 即使生产路径抛异常，也保留实际 source、owner、leaf phase/type 的失败现场。
            recordEnumImplicitState("AFTER_REQUEST", firstMarker, owner, target, rawBindings)
        }

        assertTrue(target.resolvePhase >= CfirResolvePhase.IMPLICIT_TYPES)
        assertTrue(owner.resolvePhase >= CfirResolvePhase.IMPLICIT_TYPES)
        assertTrue(rawBindings.all { it.resolvePhase < CfirResolvePhase.BODY_RESOLVE })
        assertEnumResolvedRoot(owner, rawPattern, owner.pattern, rawBindings)
        assertConvergedBindings(owner, expected, rawBindings)
        assertEnumLeafSymbols(rawBindings)
        rawBindings.forEachIndexed { index, binding -> assertSame(canonical[index], binding.source?.psi) }
        assertTrue(owner.returnTypeRef is CfirResolvedTypeRef)
        assertEnumImplicitType((owner.returnTypeRef as CfirResolvedTypeRef).coneType, enum)
        val completedTypes = rawBindings.map { it.returnTypeRef }
        val completedPhases = rawBindings.map { it.resolvePhase }

        target.lazyResolveToPhase(CfirResolvePhase.IMPLICIT_TYPES)
        assertConvergedBindings(owner, expected, rawBindings)
        rawBindings.forEachIndexed { index, binding ->
            assertSame(completedTypes[index], binding.returnTypeRef)
            assertEquals(completedPhases[index], binding.resolvePhase)
            assertSame(canonical[index], binding.source?.psi)
        }
        assertEnumLeafSymbols(rawBindings)
        recordEnumImplicitState("REPEAT", firstMarker, owner, target, rawBindings)
    }

    /** 整体 owner/file 请求必须在各 leaf 自身的声明锁下完成尚未查询的全局绑定。 */
    @ParameterizedTest(name = "file={0}, {1} first")
    @CsvSource(
        "false, number", "false, flag", "false, text",
        "true, number", "true, flag", "true, text",
    )
    fun globalTupleRecursively(recursiveFile: Boolean, firstBinding: String, mainFile: CjFile) {
        assertGlobalBindings(
            mainFile,
            firstBinding,
            listOf(mapOf("number" to ExpectedType.INT64, "flag" to ExpectedType.BOOL, "text" to ExpectedType.STRING)),
            recursiveTarget = if (recursiveFile) RecursiveTarget.FILE else RecursiveTarget.OWNER,
        )
    }

    /** 嵌套层级必须按位置投影，最深叶子和外层兄弟均可独立发起首次请求。 */
    @ParameterizedTest(name = "{0} first")
    @ValueSource(strings = ["number", "flag", "text", "tailNumber"])
    fun globalNestedTuple(firstBinding: String, mainFile: CjFile) {
        assertGlobalBindings(
            mainFile,
            firstBinding,
            listOf(
                mapOf(
                    "number" to ExpectedType.INT64,
                    "flag" to ExpectedType.BOOL,
                    "text" to ExpectedType.STRING,
                    "tailNumber" to ExpectedType.INT64,
                ),
            ),
        )
    }

    /** owner 的显式类型已在 TYPES 解析时，仍须处理其 implicit 叶子。 */
    @ParameterizedTest(name = "{0} first")
    @ValueSource(strings = ["number", "flag", "text"])
    fun globalExplicitOwner(firstBinding: String, mainFile: CjFile) {
        assertGlobalBindings(
            mainFile,
            firstBinding,
            listOf(mapOf("number" to ExpectedType.INT64, "flag" to ExpectedType.BOOL, "text" to ExpectedType.STRING)),
            explicitOwner = true,
        )
    }

    /** 非 tuple、当前层元数不等和 initializer 错误均不得留下 implicit 叶子。 */
    @ParameterizedTest(name = "{0} first")
    @ValueSource(
        strings = [
            "nonTupleNumber", "nonTupleFlag",
            "shortNumber", "shortFlag", "shortText",
            "longNumber", "longFlag",
            "initializerNumber", "initializerFlag",
        ],
    )
    fun globalWrongShape(firstBinding: String, mainFile: CjFile) {
        assertGlobalBindings(
            mainFile,
            firstBinding,
            listOf(
                mapOf("nonTupleNumber" to ExpectedType.ERROR, "nonTupleFlag" to ExpectedType.ERROR),
                mapOf(
                    "shortNumber" to ExpectedType.ERROR,
                    "shortFlag" to ExpectedType.ERROR,
                    "shortText" to ExpectedType.ERROR,
                ),
                mapOf("longNumber" to ExpectedType.ERROR, "longFlag" to ExpectedType.ERROR),
                mapOf("initializerNumber" to ExpectedType.ERROR, "initializerFlag" to ExpectedType.ERROR),
            ),
        )
    }

    /** 嵌套层失败只将未完成绑定收敛为 error，保留此前成功兄弟的 Int64 和 String。 */
    @ParameterizedTest(name = "{0} first")
    @ValueSource(
        strings = [
            "shapeNumber", "shapeText", "shapeFlag", "shapeTail",
            "arityNumber", "arityText", "arityFlag", "arityTail",
        ],
    )
    fun globalNestedWrongShape(firstBinding: String, mainFile: CjFile) {
        assertGlobalBindings(
            mainFile,
            firstBinding,
            listOf(
                mapOf(
                    "shapeNumber" to ExpectedType.INT64,
                    "shapeText" to ExpectedType.STRING,
                    "shapeFlag" to ExpectedType.ERROR,
                    "shapeTail" to ExpectedType.ERROR,
                ),
                mapOf(
                    "arityNumber" to ExpectedType.INT64,
                    "arityText" to ExpectedType.STRING,
                    "arityFlag" to ExpectedType.ERROR,
                    "arityTail" to ExpectedType.ERROR,
                ),
            ),
        )
    }

    /**
     * 只从 raw 文件的模式树取实际叶子；首次解析请求直接发送给选中的绑定。
     * 显式 owner 用例仅额外推进到 TYPES，以建立 owner 已解析而叶子仍 implicit 的前置条件。
     * 此处仅覆盖非局部声明：owner 的正文解析不得替叶子发布其独立 LL 目标的阶段。
     */
    private fun assertGlobalBindings(
        mainFile: CjFile,
        firstBinding: String,
        expectedOwners: List<Map<String, ExpectedType>>,
        explicitOwner: Boolean = false,
        recursiveTarget: RecursiveTarget? = null,
    ) {
        val expected = expectedOwners.single { firstBinding in it }
        val cfirFile = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
        val owner = cfirFile.declarations.filterIsInstance<CfirPatternVariable>().single { declaration ->
            declaration.pattern.bindingVariables().any { it.name.asString() == firstBinding }
        }
        val rawBindings = owner.pattern.bindingVariables()
        val target = rawBindings.single { it.name.asString() == firstBinding }

        assertTrue(!owner.isLocal && rawBindings.none { it.isLocal }, "声明锁阶段契约仅适用于非局部绑定。")
        assertEquals(CfirResolvePhase.RAW_CFIR, owner.resolvePhase, "准备文件不得预先解析 owner。")
        assertEquals(expected.keys.toList(), rawBindings.map { it.name.asString() })
        assertPatternTreeOwner(owner)
        rawBindings.forEach { binding ->
            assertEquals(CfirResolvePhase.RAW_CFIR, binding.resolvePhase)
            assertTrue(binding.returnTypeRef is CfirImplicitTypeRef, "叶子必须从 implicit 类型开始：" + binding.name)
        }

        if (explicitOwner) {
            owner.lazyResolveToPhase(CfirResolvePhase.TYPES)
            assertEquals(CfirResolvePhase.TYPES, owner.resolvePhase)
            assertTrue(owner.returnTypeRef is CfirResolvedTypeRef, "显式 owner 类型必须已解析。")
            assertTrue((owner.returnTypeRef as CfirResolvedTypeRef).coneType is ConeTupleType)
            rawBindings.forEach { binding ->
                assertTrue(binding.returnTypeRef is CfirImplicitTypeRef, "TYPES 后叶子仍须是 implicit：" + binding.name)
                assertTrue(binding.resolvePhase < CfirResolvePhase.IMPLICIT_TYPES)
            }
        } else {
            assertTrue(owner.returnTypeRef is CfirImplicitTypeRef)
        }
        val ownerTypeBeforeQuery = owner.returnTypeRef

        target.lazyResolveToPhase(CfirResolvePhase.IMPLICIT_TYPES)

        assertTrue(target.resolvePhase >= CfirResolvePhase.IMPLICIT_TYPES)
        assertTrue(owner.resolvePhase >= CfirResolvePhase.IMPLICIT_TYPES, "leaf 请求必须推进真实 owner。")
        if (explicitOwner) {
            assertSame(ownerTypeBeforeQuery, owner.returnTypeRef, "逐叶解析须保留已有显式 owner 类型。")
        }
        assertConvergedBindings(owner, expected, rawBindings)

        // 首查后立即记录，包括尚未独立查询的兄弟；各非局部声明按 Kotlin LL 的自身锁发布阶段。
        val phasesBeforeOwnerBody = rawBindings.associateWith { it.resolvePhase }
        assertTrue(phasesBeforeOwnerBody.values.all { it < CfirResolvePhase.BODY_RESOLVE })
        owner.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
        assertEquals(CfirResolvePhase.BODY_RESOLVE, owner.resolvePhase)
        rawBindings.forEach { binding ->
            assertEquals(
                phasesBeforeOwnerBody.getValue(binding),
                binding.resolvePhase,
                "owner BODY_RESOLVE 不得推进独立 leaf 目标：" + binding.name,
            )
        }
        assertConvergedBindings(owner, expected, rawBindings)

        if (recursiveTarget != null) {
            val recursiveDeclaration: CfirElementWithResolveState = when (recursiveTarget) {
                RecursiveTarget.FILE -> cfirFile
                RecursiveTarget.OWNER -> owner
            }
            recursiveDeclaration.lazyResolveToPhaseRecursively(CfirResolvePhase.BODY_RESOLVE)
            rawBindings.forEach { binding ->
                assertEquals(CfirResolvePhase.BODY_RESOLVE, binding.resolvePhase, "递归目标必须完成真实叶子：" + binding.name)
            }
            assertConvergedBindings(owner, expected, rawBindings)
            return
        }

        // 让参数化选中的叶子也首查正文；每次仅当前目标发布 BODY_RESOLVE，兄弟阶段保持原值。
        val expectedPhases = phasesBeforeOwnerBody.toMutableMap()
        val bodyTargets = listOf(target) + rawBindings.filter { it !== target }
        bodyTargets.forEach { binding ->
            binding.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
            expectedPhases[binding] = CfirResolvePhase.BODY_RESOLVE
            rawBindings.forEach { leaf ->
                assertEquals(
                    expectedPhases.getValue(leaf),
                    leaf.resolvePhase,
                    "逐叶 BODY_RESOLVE 必须由各自 LL 目标完成：" + leaf.name,
                )
            }
            assertConvergedBindings(owner, expected, rawBindings)
        }

        // 正文已逐叶完成后再重复类型请求，身份、类型和已发布的正文阶段都不得改变。
        rawBindings.forEach { binding -> binding.lazyResolveToPhase(CfirResolvePhase.IMPLICIT_TYPES) }
        assertConvergedBindings(owner, expected, rawBindings)
        assertTrue(rawBindings.all { it.resolvePhase == CfirResolvePhase.BODY_RESOLVE })
    }

    /** 正向模式树与每个节点的唯一反向 owner 必须指向同一个真实容器。 */
    private fun assertPatternTreeOwner(owner: CfirPatternVariable) {
        assertSame(owner.pattern, owner.patternTreeRoot())
        owner.pattern.accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (element is CfirPatternTreeNode) {
                    assertSame(owner, element.patternTreeOwner, "模式树节点不得串到其他 owner。")
                }
                element.acceptChildren(this)
            }
        })
    }

    /** 全部 active binding 都必须解析完毕，并保留首次查询之前的真实声明身份。 */
    private fun assertConvergedBindings(
        owner: CfirPatternVariable,
        expected: Map<String, ExpectedType>,
        rawBindings: List<CfirPatternBindingVariable>,
    ) {
        val bindings = owner.pattern.bindingVariables()
        assertEquals(expected.keys.toList(), bindings.map { it.name.asString() })
        assertEquals(rawBindings.size, bindings.size)
        assertPatternTreeOwner(owner)
        bindings.forEachIndexed { index, binding ->
            assertSame(rawBindings[index], binding, "类型解析不得替换真实 binder。")
            assertSame(owner, binding.patternTreeOwner)
            val typeRef = binding.returnTypeRef
            assertTrue(typeRef is CfirResolvedTypeRef, "active 叶子必须是 resolved 类型：" + binding.name)
            val type = (typeRef as CfirResolvedTypeRef).coneType
            when (expected.getValue(binding.name.asString())) {
                ExpectedType.INT64 -> assertEquals(ConePrimitiveType.INT64, type, binding.name.asString())
                ExpectedType.BOOL -> assertEquals(ConePrimitiveType.BOOLEAN, type, binding.name.asString())
                ExpectedType.STRING -> assertTrue(type.isString, "叶子必须为 String：" + binding.name)
                ExpectedType.ERROR -> assertTrue(type is ConeErrorType, "确定失败叶子必须为 ConeErrorType：" + binding.name)
            }
        }
    }

    private enum class RecursiveTarget { FILE, OWNER }

    /** 正常分量类型来自官方位置投影；ERROR 表达本项目额外要求的失败收敛契约。 */
    private enum class ExpectedType {
        INT64,
        BOOL,
        STRING,
        ERROR,
    }
}

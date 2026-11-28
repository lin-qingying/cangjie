package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.common.CfirBinaryDependenciesModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.copy.*
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.deserialization.SingleModuleDataProvider
import org.cangnova.cangjie.cfir.entrypoint.configuration.createForCfirFrontend
import org.cangnova.cangjie.cfir.entrypoint.session.CfirDefaultSessionFactory
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.patterns.CfirEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.builder.buildBindingPattern
import org.cangnova.cangjie.cfir.patterns.builder.buildEnumPattern
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzer
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzerContext
import org.cangnova.cangjie.cfir.resolve.body.SnapshotCfirMapper
import org.cangnova.cangjie.cfir.resolve.dfa.CfirLocalVariableAssignmentAnalyzer
import org.cangnova.cangjie.cfir.resolve.dfa.createDfaSourceVisitor
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.*
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.source.CfirSourceCallKind
import org.cangnova.cangjie.cfir.source.CfirSourceForInKind
import org.cangnova.cangjie.cfir.source.CfirSourceOperator
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import java.util.IdentityHashMap
import org.cangnova.cangjie.source.CjBinarySourceElement
import kotlin.test.*

/**
 * G2 的真实 owned-copy 事务及公开阶段操作数合同；不是 G3 full replay 验收。
 * native 14276740 的 ForInExpr.cpp:462–632 保存槽与 Some/P 关系；
 * Kotlin 3c7a5041 的 ControlFlowGraphCopier 保留 fir，由 DFA context 的公开 snapshot 消费。
 * 手工 CFIR 的字段形状参考 NativeForExecutionControlFlowTest，不作为 native CJO roundtrip 正例。
 */
class CfirNativeForProjectionTest {
    private val bodyLibraryModule = CfirBinaryDependenciesModuleData(Name.special("<native-for-projection-dependencies>"))
    private val module = CfirSourceModuleData(
        name = Name.identifier("native-for-projection"), dependencies = listOf(bodyLibraryModule), refinementDependencies = emptyList(),
        targetPlatform = CangJiePlatforms.defaultCangJiePlatform, platform = CfirPlatform.DEFAULT,
    )
    private val intType = ConePrimitiveType(PrimitiveTypeKind.INT64)
    private val boolType = ConePrimitiveType(PrimitiveTypeKind.BOOLEAN)
    private val unitType = ConePrimitiveType(PrimitiveTypeKind.UNIT)
    private val byteType = ConePrimitiveType(PrimitiveTypeKind.UINT8)
    private val stringType = ConeClassLikeType(ConeClassLikeLookupTagImpl(StdlibClassIds.String))
    private val optionType = ConeEnumType(ConeClassLikeLookupTagImpl(StdlibClassIds.Option), listOf(intType))
    private val cursorId = ClassId(FqName("nativefor"), Name.identifier("Cursor"))
    private val cursorType = ConeClassLikeType(ConeClassLikeLookupTagImpl(cursorId))
    private val typeRemapper = CfirTypeRefRemapper { type, _ -> type }
    private val attributes = object : CfirOwnedAttributeRemapper {
        override fun collectOwnedChildren(key: String, value: Any, visit: (CfirElement) -> Unit) {
            error("This fixture has no extension attribute: $key")
        }
        override fun copyAttribute(key: String, value: Any, context: CfirOwnedGraphCopyContext): Any =
            error("This fixture has no extension attribute: $key")
    }

    @Test
    fun declarationOnlyIteratorCopyKeepsBorrowedReferenceAndUsesFreshConstructor() {
        val fixture = iteratorFixture()
        val copy = transaction(listOf(fixture.constructor))
        val fresh = copy.getCopied(fixture.constructor)
        assertNotSame(fixture.constructor, fresh)
        assertSame(fresh.symbol, copy.getCopiedSymbol(fixture.constructor.symbol))
        assertSame(fresh, fresh.symbol.cfir)
        assertBorrowedHost(copy, fixture.loop, listOf(fixture.next, fixture.reference, fixture.iterator, fixture.selector))
        assertIteratorStages(fixture, copy)
        assertSame(fixture.constructor.symbol, fixture.reference.resolvedSymbol)
        assertSlots(fixture.loop, listOf(fixture.iterator, fixture.selector))
    }

    @Test
    fun symbolOnlyIteratorTransactionRetainsExternalRebindingWithoutOwningEitherDeclaration() {
        val fixture = iteratorFixture()
        // 两份实际绑定的 Option.Some 声明，同 callable id/concrete kind；不是按临时名字猜 target。
        val external = someConstructor()
        val copy = transaction(emptyList(), mapOf(fixture.constructor.symbol to external.symbol))
        assertTrue(copy.roots.isEmpty())
        var count = 0
        copy.forEachCopied { _, _ -> count++ }
        assertEquals(0, count)
        assertSame(external.symbol, copy.getCopiedSymbol(fixture.constructor.symbol))
        assertBorrowedHost(copy, fixture.loop, listOf(fixture.constructor, external, fixture.reference, fixture.next))
        assertIteratorStages(fixture, copy)
        assertSame(fixture.constructor, fixture.reference.resolvedSymbol.cfir)
        assertSame(external, external.symbol.cfir)
        assertSlots(fixture.loop, listOf(fixture.iterator, fixture.selector))
    }

    @Test
    fun iteratorChildRootsAndWholeLoopPreserveActualResultReferenceAndSomeBinding() {
        for (selection in 0..3) {
            val fixture = iteratorFixture()
            val roots: List<CfirElement> = when (selection) {
                0 -> listOf(fixture.next, fixture.next)
                1 -> listOf(fixture.reference, fixture.constructor, fixture.next)
                2 -> listOf(fixture.loop)
                else -> listOf(fixture.loop, fixture.constructor)
            }
            val copy = transaction(roots)
            if (selection < 2) assertBorrowedHost(copy, fixture.loop, listOf(fixture.selector, fixture.iterator))
            if (selection == 0) assertSame(copy.roots[0], copy.roots[1])
            val mapper = projection(copy)
            val some = assertIs<CfirEnumPattern>(mapper.mapElement(fixture.loop).pattern)
            if (selection >= 2) {
                assertNotSame(fixture.loop, mapper.mapElement(fixture.loop))
                assertSame(copy.getCopied(fixture.selector), assertIs<CfirBlock>(mapper.mapElement(fixture.loop).iterable).statements[1])
                assertSame(copy.getCopied(fixture.next), copy.getCopied(fixture.selector).selector)
                assertTrue(copy.getCopied(fixture.selector).cases.isEmpty())
                assertSame(copy.getCopied(fixture.binding), assertIs<CfirBindingPattern>(some.arguments.single()).bindingVariable)
            }
            if (selection == 0 || selection == 2) {
                assertSame(fixture.constructor.symbol, copy.getCopiedSymbol(fixture.constructor.symbol))
            }
            assertIteratorStages(fixture, copy)
            assertSame(fixture.reference, assertIs<CfirNamedAccessExpression>(fixture.some.constructor).calleeReference)
            assertSame(fixture.constructor.symbol, fixture.reference.resolvedSymbol)
            assertSame(fixture.next, fixture.selector.selector)
            assertSlots(fixture.loop, listOf(fixture.iterator, fixture.selector))
        }
    }

    @Test
    fun rangeChildOnlyRootsPreserveHostSlotsAndSeparateEffectiveOperands() {
        for (selection in 0..4) {
            val fixture = rangeFixture()
            val initializer = checkNotNull(fixture.binding.initializer)
            val roots: List<CfirElement> = when (selection) {
                0 -> listOf(fixture.index, fixture.bound)
                1 -> listOf(fixture.update)
                2 -> listOf(fixture.comparison)
                3 -> listOf(initializer, initializer)
                else -> listOf(fixture.index, fixture.update, fixture.comparison, initializer)
            }
            val copy = transaction(roots)
            assertBorrowedHost(copy, fixture.loop, listOf(fixture.binding))
            val mapper = projection(copy)
            assertSame(mapper.mapElement(fixture.index).symbol, copy.getCopiedSymbol(fixture.index.symbol))
            for (expression in listOf(fixture.update.lValue, fixture.comparison.left, initializer)) {
                val mapped = mapper.mapElement(expression)
                val expected = if (mapped === expression) fixture.index.symbol else copy.getCopiedSymbol(fixture.index.symbol)
                assertSame(expected, reference(mapped).resolvedSymbol)
                assertSame(copy.getCopiedSymbol(fixture.index.symbol), mapper.mapSymbol(reference(expression).resolvedSymbol))
            }
            val right = mapper.mapElement(fixture.comparison.right)
            assertSame(if (right === fixture.comparison.right) fixture.bound.symbol else copy.getCopiedSymbol(fixture.bound.symbol),
                reference(right).resolvedSymbol)
            // declaration-only 时保存表达式中的 borrowed 引用仍指向 old；有效写目标独立查询。
            if (selection == 0) {
                assertSame(fixture.index.symbol, reference(fixture.update.lValue).resolvedSymbol)
                assertSame(copy.getCopied(fixture.index), copy.getCopiedSymbol(reference(fixture.update.lValue).resolvedSymbol).cfir)
            }
            if (selection == 3) assertSame(copy.roots[0], copy.roots[1])
            val harness = Harness(mapper.mapElement(fixture.loop))
            harness.begin(listOf(mapper.mapElement(fixture.index), mapper.mapElement(fixture.bound)))
            harness.emit(mapper.mapElement(fixture.comparison))
            harness.builder.exitForInLoopInitialCheck(harness.loop)
            harness.builder.enterForInLoopCondition(harness.loop)
            val check = harness.builder.exitForInLoopRangeCheck(mapper.mapElement(fixture.comparison),
                mapper.mapElement(fixture.index), mapper.mapElement(fixture.bound))
            harness.bindAndBody(mapper.mapElement(fixture.binding), mapper.mapElement(initializer))
            harness.builder.enterForInLoopIteration(harness.loop)
            val update = harness.emit(mapper.mapElement(fixture.update))
            val graphs = harness.snapshotAndFinish()
            for (graph in graphs) {
                val saved = graph.nodes.filterIsInstance<ForInLoopRangeCheckNode>().single()
                assertSame(check.fir, saved.fir)
                assertSame(mapper.mapElement(fixture.index), saved.index)
                assertSame(mapper.mapElement(fixture.bound), saved.bound)
                assertSame(update.fir, graph.nodes.filterIsInstance<VariableAssignmentNode>().single().fir)
                assertSourceElements(saved, listOf(saved.fir, saved.index, saved.bound))
            }
            assertSame(fixture.index.symbol, reference(fixture.update.lValue).resolvedSymbol)
            assertSame(fixture.index.symbol, reference(initializer).resolvedSymbol)
            assertSlots(fixture.loop, listOf(fixture.index, fixture.bound, fixture.update, fixture.comparison))
            assertSame(fixture.binding, assertIs<CfirBindingPattern>(fixture.loop.pattern).bindingVariable)
            assertSame(CfirSourceOperator.ADD_ASSIGN, fixture.update.operation)
            assertTrue(fixture.update.isCompound)
        }
    }

    @Test
    fun stringChildOnlyRootsKeepImplicitCheckAndWriteOnActualMappedIndex() {
        for (selection in 0..2) {
            val fixture = stringFixture()
            val roots: List<CfirElement> = when (selection) {
                0 -> listOf(fixture.index, fixture.bound)
                1 -> listOf(fixture.elementRead, fixture.elementRead)
                else -> listOf(fixture.index, fixture.temporary, fixture.bound, fixture.elementRead)
            }
            val copy = transaction(roots)
            assertBorrowedHost(copy, fixture.loop, listOf(fixture.binding))
            val mapper = projection(copy)
            val harness = Harness(fixture.loop)
            harness.begin(listOf(fixture.index, fixture.temporary, fixture.bound).map { mapper.mapElement(it) })
            harness.builder.exitForInLoopStringCheck(fixture.loop, mapper.mapElement(fixture.index), mapper.mapElement(fixture.bound))
            harness.builder.exitForInLoopInitialCheck(fixture.loop)
            harness.builder.enterForInLoopCondition(fixture.loop)
            harness.builder.exitForInLoopStringCheck(fixture.loop, mapper.mapElement(fixture.index), mapper.mapElement(fixture.bound))
            harness.bindAndBody(fixture.binding, mapper.mapElement(fixture.elementRead))
            harness.builder.enterForInLoopIteration(fixture.loop)
            harness.builder.exitForInLoopStringUpdate(fixture.loop, mapper.mapElement(fixture.index))
            for (graph in harness.snapshotAndFinish()) {
                val checks = graph.nodes.filterIsInstance<ForInLoopStringCheckNode>()
                assertEquals(2, checks.size)
                checks.forEach { check ->
                    assertSame(fixture.loop, check.fir)
                    assertSame(mapper.mapElement(fixture.index), check.index)
                    assertSame(mapper.mapElement(fixture.bound), check.bound)
                    assertSourceElements(check, listOf(fixture.loop, check.index, check.bound))
                }
                val update = graph.nodes.filterIsInstance<ForInLoopStringUpdateNode>().single()
                assertSame(fixture.loop, update.fir)
                assertSame(mapper.mapElement(fixture.index), update.index)
                assertSourceElements(update, listOf(fixture.loop, update.index))
                assertTrue(graph.nodes.any { it is CallExpressionNode && it.fir === mapper.mapElement(fixture.elementRead) })
                assertFalse(graph.nodes.any { it is VariableAssignmentNode })
            }
            assertSame(mapper.mapElement(fixture.index).symbol, copy.getCopiedSymbol(fixture.index.symbol))
            assertSame(fixture.elementRead, fixture.binding.initializer)
            assertSame(fixture.index.symbol, reference(checkNotNull(fixture.elementRead.arguments.single().value)).resolvedSymbol)
            assertSlots(fixture.loop, listOf(fixture.index, fixture.temporary, fixture.bound))
            assertSame(fixture.binding, assertIs<CfirBindingPattern>(fixture.loop.pattern).bindingVariable)
        }
    }

    /** 真正的 mini 赋值事实由公开 analyzer 接收有效目标及原 RHS 类型；不读取私有 Assignment 表。 */
    @Test
    fun borrowedRangeAssignmentRecordsRhsTypeForCopiedDeclarationAndSurvivesSnapshot() {
        val fixture = rangeFixture()
        val copy = transaction(listOf(fixture.index))
        val mapper = projection(copy)
        val harness = Harness(fixture.loop)
        val assignmentAnalyzer = CfirLocalVariableAssignmentAnalyzer(mapper)
        harness.context.variableAssignmentAnalyzer = assignmentAnalyzer
        assignmentAnalyzer.enterFunction(harness.function)
        assignmentAnalyzer.enterLoop(fixture.loop)
        val configuration = CompilerConfiguration.createForCfirFrontend()
        // 普通源码会话经实际 Library 依赖取得 shared provider，声明仍属于原 module。
        val factory = CfirDefaultSessionFactory()
        val shared = factory.createSharedLibrarySession(module.name, emptyList(), configuration.languageVersionSettings)
        factory.createLibrarySession(
            shared, SingleModuleDataProvider(bodyLibraryModule), emptyList(), configuration.languageVersionSettings,
        )
        val sourceSession = factory.createSourceSession(module, emptyList(), configuration)
        val holder = object : SessionHolder { override val session = sourceSession }
        val analyzer = CfirDataFlowAnalyzer(holder, BodyResolveContext(ReturnTypeCalculatorForFullBodyResolve.Default, harness.context))
        val types = mutableListOf<ConeCangJieType>()
        val declarations = mutableListOf<CfirElement>()
        fun collectTypes(context: CfirDataFlowAnalyzerContext) {
            types.clear()
            declarations.clear()
            context.variableAssignmentAnalyzer.acceptSourceChildren(createDfaSourceVisitor({ declarations += it }, { types += it }, { true }))
        }
        collectTypes(harness.context)
        assertTrue(types.isEmpty())
        assertTrue(declarations.any { it === copy.getCopied(fixture.index) })
        assertFalse(declarations.any { it === fixture.index })
        val effectiveTarget = assertIs<CfirCallableDeclaration>(copy.getCopiedSymbol(reference(fixture.update.lValue).resolvedSymbol).cfir)
        analyzer.recordAssignment(fixture.update, effectiveTarget)
        collectTypes(harness.context)
        assertTrue(types.any { it === fixture.update.rValue.coneTypeOrNull })
        val snapshot = harness.context.createSnapshot(mapper, smartCastPosition = null)
        collectTypes(snapshot.context)
        assertTrue(types.any { it === fixture.update.rValue.coneTypeOrNull })
        assertTrue(declarations.any { it === effectiveTarget })
        assertFalse(declarations.any { it === fixture.index })
        assertSame(fixture.index.symbol, reference(fixture.update.lValue).resolvedSymbol)
        assertSame(fixture.update, assertIs<CfirBlock>(fixture.loop.iterable).statements[2])
        assignmentAnalyzer.exitLoop()
        assignmentAnalyzer.exitFunction()
    }

    private fun transaction(roots: List<CfirElement>, symbols: Map<CfirBasedSymbol<*>, CfirBasedSymbol<*>> = emptyMap()): CfirOwnedGraphCopyResult {
        val copy = deepCopyOwnedGraphs(roots, typeRemapper, attributes, symbolRebindings = symbols)
        assertEquals(roots.size, copy.roots.size)
        roots.forEachIndexed { index, root ->
            assertSame(copy.getCopied(root), copy.roots[index])
            assertNotSame(root, copy.roots[index])
            assertEquals(root::class, copy.roots[index]::class)
        }
        return copy
    }

    /** 测试输入适配器只消费事务公开结果，不调用/反射 private replay mapper。 */
    private fun projection(copy: CfirOwnedGraphCopyResult): SnapshotCfirMapper {
        val elements = IdentityHashMap<CfirElement, CfirElement>()
        copy.forEachCopied { old, fresh -> elements[old] = fresh }
        return object : SnapshotCfirMapper {
            @Suppress("UNCHECKED_CAST")
            override fun <T : CfirElement> mapElement(element: T): T = (elements[element] ?: element) as T
            override fun <T : CfirBasedSymbol<*>> mapSymbol(symbol: T): T = copy.getCopiedSymbol(symbol)
        }
    }

    private fun assertBorrowedHost(copy: CfirOwnedGraphCopyResult, loop: CfirForInExpression, children: List<CfirElement>) {
        val forbidden = listOf(loop, loop.iterable, loop.pattern, loop.body) + children
        copy.forEachCopied { old, _ -> assertFalse(forbidden.any { it === old }, "Copy expanded the selected child domain") }
        val mapper = projection(copy)
        forbidden.forEach { assertSame(it, mapper.mapElement(it)) }
    }

    private fun assertIteratorStages(fixture: IteratorFixture, copy: CfirOwnedGraphCopyResult) {
        val mapper = projection(copy)
        val loop = mapper.mapElement(fixture.loop)
        val result = mapper.mapElement(fixture.next)
        val reference = mapper.mapElement(fixture.reference)
        val target = copy.getCopiedSymbol(fixture.reference.resolvedSymbol)
        val harness = Harness(loop, fixture.ownerSymbol, listOf(fixture.sequence))
        harness.begin(listOf(mapper.mapElement(fixture.iterator)))
        harness.builder.enterForInLoopIteration(loop)
        harness.emit(result)
        val check = harness.builder.exitForInLoopIteratorCheck(loop, result, reference, target)
        harness.bindAndBody(mapper.mapElement(fixture.binding))
        val graphs = harness.snapshotAndFinish()
        val checks = graphs.map { it.nodes.filterIsInstance<ForInLoopIteratorCheckNode>().single() }
        assertSame(check, checks[0])
        assertNotSame(checks[0], checks[1])
        checks.forEach { saved ->
            assertSame(loop, saved.fir)
            assertSame(result, saved.resultExpression)
            assertSame(reference, saved.constructorReference)
            assertSame(target, saved.constructorSymbol)
            assertSourceElements(saved, listOf(loop, result, reference, target.cfir))
        }
        assertSame(if (reference === fixture.reference) fixture.constructor.symbol else target, reference.resolvedSymbol)
    }

    private fun assertSourceElements(node: CFGNode<*>, expected: List<CfirElement>) {
        val actual = mutableListOf<CfirElement>()
        node.acceptSourceChildren(createDfaSourceVisitor({ actual += it }, {}, { true }))
        assertEquals(expected.size, actual.size)
        expected.forEach { element -> assertEquals(1, actual.count { it === element }) }
    }

    private fun assertSlots(loop: CfirForInExpression, expected: List<CfirStatement>) {
        val actual = assertIs<CfirBlock>(loop.iterable).statements
        assertEquals(expected.size, actual.size)
        expected.zip(actual).forEach { (element, saved) -> assertSame(element, saved) }
    }

    /** 发出实际 CFIR 的表达式事件，在活动函数图中调用公开 snapshot，再分别完成源图与副本。 */
    private inner class Harness(
        val loop: CfirForInExpression,
        symbol: CfirNamedFunctionSymbol = CfirNamedFunctionSymbol(CallableId(FqName("nativefor"), Name.identifier("executeProjection"))),
        parameters: List<CfirValueParameter> = emptyList(),
    ) {
        val context = CfirDataFlowAnalyzerContext()
        val builder get() = context.graphBuilder
        private val outer = buildBlock { statements += loop; coneTypeOrNull = unitType }
        val function = function(symbol.callableId, unitType, outer, functionSymbol = symbol, parameters = parameters)
        init {
            builder.enterFunction(function)
            parameters.forEach { parameter ->
                assertSame(function.symbol, parameter.containingDeclarationSymbol)
                builder.enterValueParameter(parameter)
                builder.exitValueParameter(parameter, defaultGraph = null)
            }
            builder.enterBlock(outer)
        }
        fun begin(declarations: List<CfirVariable>) {
            builder.enterForInLoop(loop)
            declarations.forEach { variable ->
                builder.enterVariableDeclaration(variable)
                emit(checkNotNull(variable.initializer))
                builder.exitVariableDeclaration(variable)
            }
        }
        fun bindAndBody(binding: CfirVariable, initializer: CfirExpression? = binding.initializer) {
            builder.exitForInLoopIteration(loop)
            builder.enterVariableDeclaration(binding)
            initializer?.let(::emit)
            builder.exitVariableDeclaration(binding)
            builder.exitForInLoopGuard(loop)
            builder.enterBlock(loop.body)
            loop.body.statements.forEach { emit(assertIs<CfirExpression>(it)) }
            builder.exitBlock(loop.body)
            builder.exitForInLoopBody(loop)
        }
        fun emit(expression: CfirExpression): CFGNode<*> = when (expression) {
            is CfirLiteralExpression -> builder.exitLiteralExpression(expression)
            is CfirNamedAccessExpression -> {
                expression.explicitReceiver?.let(::emit)
                builder.exitQualifiedAccessExpression(expression)
            }
            is CfirCallExpression -> {
                emit(expression.base)
                expression.arguments.forEach { emit(checkNotNull(it.value)) }
                builder.exitCallExpression(expression)
            }
            is CfirBinaryExpression -> {
                emit(expression.left); emit(expression.right); builder.exitBinaryExpression(expression)
            }
            is CfirAssignment -> {
                emit(expression.lValue); emit(expression.rValue); builder.exitVariableAssignment(expression)
            }
            else -> error("Fixture has no explicit event for ${expression::class}")
        }
        fun snapshotAndFinish(): List<ControlFlowGraph> {
            val sourceGraph = checkNotNull(builder.currentGraphOrNull)
            val snapshot = context.createSnapshot(identityMapper, smartCastPosition = null)
            val copiedGraph = snapshot.graphMapping.getValue(sourceGraph)
            assertNotSame(sourceGraph, copiedGraph)
            fun finish(builder: ControlFlowGraphBuilder): ControlFlowGraph {
                builder.exitForInLoop(loop)
                builder.exitBlock(outer)
                return builder.exitFunction(function).second
            }
            val source = finish(builder)
            val copied = finish(snapshot.context.graphBuilder)
            assertSame(sourceGraph, source)
            assertSame(copiedGraph, copied)
            assertEquals(source.nodes.size, copied.nodes.size)
            source.nodes.zip(copied.nodes).forEach { (old, fresh) ->
                assertNotSame(old, fresh)
                assertSame(old.fir, fresh.fir)
                assertEquals(old::class, fresh::class)
            }
            return listOf(source, copied)
        }
    }

    private val identityMapper = object : SnapshotCfirMapper {
        override fun <T : CfirElement> mapElement(element: T): T = element
        override fun <T : CfirBasedSymbol<*>> mapSymbol(symbol: T): T = symbol
    }

    private data class RangeFixture(val loop: CfirForInExpression, val index: CfirPatternBindingVariable,
        val bound: CfirPatternBindingVariable, val update: CfirAssignment, val comparison: CfirBinaryExpression,
        val binding: CfirPatternBindingVariable)
    private data class StringFixture(val loop: CfirForInExpression, val index: CfirPatternBindingVariable,
        val temporary: CfirPatternBindingVariable, val bound: CfirPatternBindingVariable,
        val elementRead: CfirCallExpression, val binding: CfirPatternBindingVariable)
    private data class IteratorFixture(val loop: CfirForInExpression, val iterator: CfirPatternBindingVariable,
        val selector: CfirNativeMatchExpression, val next: CfirCallExpression, val some: CfirEnumPattern,
        val reference: CfirResolvedNamedReference, val constructor: CfirEnumConstructor,
        val binding: CfirPatternBindingVariable, val ownerSymbol: CfirNamedFunctionSymbol, val sequence: CfirValueParameter)

    private fun rangeFixture(): RangeFixture {
        val index = variable("index", intType, integer(0), true)
        val bound = variable("stop", intType, integer(3), false)
        val update = buildAssignment {
            coneTypeOrNull = unitType; lValue = access(index); rValue = integer(1)
            operation = CfirSourceOperator.ADD_ASSIGN; isCompound = true
        }
        val comparison = buildBinaryExpression {
            coneTypeOrNull = boolType; left = access(index); right = access(bound); operation = CfirSourceOperator.LESS
        }
        val binding = variable("element", intType, access(index), false)
        val storage = buildBlock { statements += listOf(index, bound, update, comparison); coneTypeOrNull = unitType }
        return RangeFixture(loop(CfirSourceForInKind.RANGE, storage, bindingPattern(binding)), index, bound, update, comparison, binding)
    }

    private fun stringFixture(): StringFixture {
        val index = variable("index", intType, integer(0), true)
        val text = buildLiteralExpression { kind = CfirLiteralKind.STRING; value = "ab"; coneTypeOrNull = stringType }
        val temporary = variable("text", stringType, text, false)
        val getter = function(CallableId(StdlibClassIds.String, Name.identifier("size")), intType)
        val bound = variable("end", intType, call(getter, access(temporary)), false)
        val indexGetter = function(CallableId(StdlibClassIds.String, Name.identifier("[]")), byteType, parameterTypes = listOf(intType))
        val read = call(indexGetter, access(temporary), listOf(access(index)))
        val binding = variable("element", byteType, read, false)
        val storage = buildBlock { statements += listOf(index, temporary, bound); coneTypeOrNull = unitType }
        return StringFixture(loop(CfirSourceForInKind.STRING, storage, bindingPattern(binding)), index, temporary, bound, read, binding)
    }

    private fun iteratorFixture(): IteratorFixture {
        val sequenceType = ConeClassLikeType(ConeClassLikeLookupTagImpl(StdlibClassIds.Iterable), listOf(intType), isInterface = true)
        val owner = CfirNamedFunctionSymbol(CallableId(FqName("nativefor"), Name.identifier("sequenceOwner")))
        val sequence = parameter("sequence", sequenceType, owner)
        val iteratorFunction = function(CallableId(StdlibClassIds.Iterable, Name.identifier("iterator")), cursorType)
        val iterator = variable("cursor", cursorType, call(iteratorFunction, access(sequence)), false)
        val nextFunction = function(CallableId(cursorId, Name.identifier("next")), optionType)
        val next = call(nextFunction, access(iterator))
        val selector = buildNativeMatchExpression { this.selector = next; coneTypeOrNull = optionType }
        val binding = variable("element", intType, null, false)
        val constructor = someConstructor()
        val reference = buildResolvedNamedReference { name = constructor.name; resolvedSymbol = constructor.symbol }
        val some = buildEnumPattern {
            this.constructor = buildNamedAccessExpression {
                calleeReference = reference; coneTypeOrNull = ConeFunctionType(listOf(intType), optionType)
            }
            arguments += bindingPattern(binding); semanticTypeRef = typeRef(optionType)
        }
        val storage = buildBlock { statements += listOf(iterator, selector); coneTypeOrNull = unitType }
        return IteratorFixture(loop(CfirSourceForInKind.ITERATOR, storage, some), iterator, selector, next, some, reference,
            constructor, binding, owner, sequence)
    }

    private fun someConstructor(): CfirEnumConstructor {
        val id = CallableId(StdlibClassIds.Option, Name.identifier("Some"))
        val symbol = CfirEnumConstructorSymbol(id)
        return buildEnumConstructor {
            source = source(id.callableName.asString())
            moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE; origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY; isLocal = false; status = CfirDeclarationStatusImpl()
            name = id.callableName; this.symbol = symbol; returnTypeRef = typeRef(optionType)
            valueParameters += parameter("payload", intType, symbol)
        }
    }

    private fun loop(kind: CfirSourceForInKind, storage: CfirBlock, pattern: CfirPattern): CfirForInExpression = buildForInExpression {
        coneTypeOrNull = unitType; iterationKind = kind; iterable = storage; this.pattern = pattern
        body = buildBlock {
            statements += buildLiteralExpression { this.kind = CfirLiteralKind.UNIT; value = Unit; coneTypeOrNull = unitType }
            coneTypeOrNull = unitType
        }
    }

    private fun variable(name: String, type: ConeCangJieType, initializer: CfirExpression?, mutable: Boolean): CfirPatternBindingVariable =
        buildPatternBindingVariable {
            source = source(name)
            moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE; origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY; isLocal = true; status = CfirDeclarationStatusImpl()
            this.name = Name.identifier(name); symbol = CfirPatternBindingSymbol(CallableId(this.name))
            returnTypeRef = typeRef(type); this.initializer = initializer; isVar = mutable
        }

    private fun bindingPattern(binding: CfirPatternBindingVariable): CfirBindingPattern = buildBindingPattern {
        name = binding.name; bindingVariable = binding; semanticTypeRef = binding.returnTypeRef; typeRef = binding.returnTypeRef
    }
    private fun reference(expression: CfirExpression): CfirResolvedNamedReference =
        assertIs<CfirResolvedNamedReference>(assertIs<CfirNamedAccessExpression>(expression).calleeReference)
    private fun access(variable: CfirVariable): CfirNamedAccessExpression = buildNamedAccessExpression {
        coneTypeOrNull = assertIs<CfirResolvedTypeRef>(variable.returnTypeRef).coneType
        calleeReference = buildResolvedNamedReference { name = variable.symbol.name; resolvedSymbol = variable.symbol }
    }
    private fun function(id: CallableId, result: ConeCangJieType, body: CfirBlock? = null,
        parameterTypes: List<ConeCangJieType> = emptyList(),
        functionSymbol: CfirNamedFunctionSymbol = CfirNamedFunctionSymbol(id),
        parameters: List<CfirValueParameter> = emptyList()): CfirNamedFunction {
        return buildNamedFunction {
            source = source(id.callableName.asString())
            moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE; origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY; isLocal = false; status = CfirDeclarationStatusImpl()
            name = id.callableName; symbol = functionSymbol; returnTypeRef = typeRef(result); this.body = body; isMut = false
            valueParameters += parameters
            parameterTypes.forEachIndexed { index, type -> valueParameters += parameter("arg$index", type, functionSymbol) }
        }
    }
    private fun parameter(name: String, type: ConeCangJieType, owner: CfirBasedSymbol<*>): CfirValueParameter = buildValueParameter {
        source = source(name)
        moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE; origin = CfirDeclarationOrigin.Source
        attributes = CfirDeclarationAttributes.EMPTY; isLocal = true; isNamed = false
        this.name = Name.identifier(name); symbol = CfirValueParameterSymbol(CallableId(this.name))
        containingDeclarationSymbol = owner; returnTypeRef = typeRef(type)
    }
    private fun call(function: CfirNamedFunction, receiver: CfirNamedAccessExpression,
        values: List<CfirExpression> = emptyList()): CfirCallExpression = buildCallExpression {
        val result = assertIs<CfirResolvedTypeRef>(function.returnTypeRef).coneType
        coneTypeOrNull = result
        base = buildNamedAccessExpression {
            calleeReference = buildResolvedNamedReference { name = function.name; resolvedSymbol = function.symbol }
            explicitReceiver = receiver
            coneTypeOrNull = ConeFunctionType(function.valueParameters.map { assertIs<CfirResolvedTypeRef>(it.returnTypeRef).coneType }, result)
        }
        arguments += values.map { expression -> buildFunctionArgument {
            value = expression; isDefaultValue = false; withInout = false; coneTypeOrNull = expression.coneTypeOrNull
        } }
        callKind = CfirSourceCallKind.DECLARED; hasSideEffect = true
    }
    private fun typeRef(type: ConeCangJieType): CfirResolvedTypeRef = buildResolvedTypeRef { coneType = type }
    private fun source(name: String) = object : CjBinarySourceElement(name, null, name) {}
    private fun integer(value: Int): CfirLiteralExpression = buildLiteralExpression {
        kind = CfirLiteralKind.INT; this.value = value.toLong(); coneTypeOrNull = intType
    }
}

package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.CfirFunctionTarget
import org.cangnova.cangjie.cfir.CfirLoopTarget
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.common.CfirBinaryDependenciesModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
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
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticCollector
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.body.CfirBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.*
import org.cangnova.cangjie.cfir.resolve.transformers.CfirImportResolveTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.cangnova.cangjie.cfir.source.CfirSourceCallKind
import org.cangnova.cangjie.cfir.source.CfirSourceForInKind
import org.cangnova.cangjie.cfir.source.CfirSourceOperator
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.source.CjBinarySourceElement
import kotlin.test.*

/**
 * 固定 native 14276740ffec1262b81b6b5236b1d840af270b37 的保留 For 执行合同。
 * ForInExpr.cpp:462–632 保存实际槽；TranslateForInExpr.cpp:656–858 确定首检查、绑定与 latch。
 * 用同一批实际载荷验证 CFG 接边和正常 BODY 的事件顺序。
 */
class CfirNativeForExecutionControlFlowTest {
    private val bodyLibraryModule = CfirBinaryDependenciesModuleData(Name.special("<native-for-dependencies>"))
    private val module = CfirSourceModuleData(
        name = Name.identifier("native-for-execution-cfg"),
        dependencies = listOf(bodyLibraryModule),
        refinementDependencies = emptyList(),
        targetPlatform = CangJiePlatforms.defaultCangJiePlatform,
        platform = CfirPlatform.DEFAULT,
    )
    private val intType = ConePrimitiveType(PrimitiveTypeKind.INT64)
    private val boolType = ConePrimitiveType(PrimitiveTypeKind.BOOLEAN)
    private val unitType = ConePrimitiveType(PrimitiveTypeKind.UNIT)
    private val byteType = ConePrimitiveType(PrimitiveTypeKind.UINT8)
    private val stringType = ConeClassLikeType(ConeClassLikeLookupTagImpl(StdlibClassIds.String))
    private val optionType = ConeEnumType(ConeClassLikeLookupTagImpl(StdlibClassIds.Option), listOf(intType))
    private val cursorId = ClassId(FqName("nativefor"), Name.identifier("Cursor"))
    private val cursorType = ConeClassLikeType(ConeClassLikeLookupTagImpl(cursorId))

    @Test
    fun rangeFirstCheckAndInternalConditionPrecedeStoredUpdate() {
        for (operation in listOf(CfirSourceOperator.LESS, CfirSourceOperator.LESS_EQUAL,
            CfirSourceOperator.GREATER, CfirSourceOperator.GREATER_EQUAL)) {
            val fixture = rangeFixture(operation, null, Completion.NORMAL)
            val run = buildRange(fixture)
            assertRangeStorage(fixture)
            assertOrdered(run.graph, run.initializers + listOf(run.firstCheck, run.initialCheckExit,
                run.condition, run.repeatedCheck, run.success, run.binding, run.bodyEnter, run.bodyExit,
                run.iteration, run.update))
            assertSame(fixture.comparison, run.firstCheck.fir)
            assertSame(fixture.comparison, run.repeatedCheck.fir)
            assertNotSame(run.firstCheck, run.repeatedCheck)
            val repeatedCheck = assertIs<ForInLoopRangeCheckNode>(run.repeatedCheck)
            assertSame(fixture.index, repeatedCheck.index)
            assertSame(fixture.stop, repeatedCheck.bound)
            assertEquals(1, run.graph.nodes.count { it is BinaryExpressionNode && it.fir === fixture.comparison })
            assertSame(fixture.update, run.update.fir)
            assertSame(operation, fixture.comparison.operation)
            val bindingRead = run.graph.nodes.single { it.fir === fixture.input.binding.initializer }
            assertOrdered(run.graph, listOf(run.success, run.binding, bindingRead, run.bodyEnter))
            assertForward(run.bodyExit, run.iteration)
            assertBack(run.update, run.condition)
            assertNoLivePath(run.iteration, run.firstCheck)
            assertExhaustion(run.initialCheckExit, run)
            assertExhaustion(run.success, run)
            assertInitializersOutsideRepeat(run)
        }
    }

    /** std.env.exit 的真实返回类型来自固定 runtime env.cj:154，测试只构图，不执行退出。 */
    @Test
    fun nothingReturningCompletedCallsCannotReachRangeLatch() {
        for (raw in listOf(false, true)) {
            val fixture = rangeFixture(CfirSourceOperator.LESS, null, Completion.NORMAL)
            val exit = function(CallableId(FqName("std.env"), Name.identifier("exit")), ConePrimitiveType.NOTHING,
                parameterTypes = listOf(intType))
            val expression: CfirExpression = if (raw) buildFunctionCall {
                coneTypeOrNull = ConePrimitiveType.NOTHING
                calleeReference = buildResolvedNamedReference { name = exit.name; resolvedSymbol = exit.symbol }
                argumentList = buildArgumentList { arguments += integer(1) }
            } else buildCallExpression {
                coneTypeOrNull = ConePrimitiveType.NOTHING
                base = functionAccess(exit)
                arguments += buildFunctionArgument { value = integer(1); isDefaultValue = false; withInout = false; coneTypeOrNull = intType }
                callKind = CfirSourceCallKind.DECLARED
                hasSideEffect = true
            }
            fixture.input.loop.body.replaceStatements(listOf(expression))
            fixture.input.loop.body.replaceConeTypeOrNull(ConePrimitiveType.NOTHING)
            val run = buildRange(fixture)
            val call = if (raw) {
                run.graph.nodes.filterIsInstance<FunctionCallExitNode>().single { it.fir === expression }
            } else {
                run.graph.nodes.filterIsInstance<CallExpressionNode>().single { it.fir === expression }
            }
            assertFalse(hasLivePath(call, run.iteration))
            assertTrue(run.bodyExit.isDead)
            assertTrue(run.iteration.isDead)
            assertRangeStorage(fixture)
        }
    }

    @Test
    fun nothingReturningShortCircuitOperandKeepsOnlyItsLegalBypass() {
        for (left in listOf(false, true)) {
            val fixture = rangeFixture(CfirSourceOperator.LESS, null, Completion.NORMAL)
            val exit = function(CallableId(FqName("std.env"), Name.identifier("exit")), ConePrimitiveType.NOTHING,
                parameterTypes = listOf(intType))
            val terminating = buildCallExpression {
                coneTypeOrNull = ConePrimitiveType.NOTHING
                base = functionAccess(exit)
                arguments += buildFunctionArgument { value = integer(1); isDefaultValue = false; withInout = false; coneTypeOrNull = intType }
                callKind = CfirSourceCallKind.DECLARED
                hasSideEffect = true
            }
            val leftOperand = buildLiteralExpression { kind = CfirLiteralKind.BOOLEAN; value = left; coneTypeOrNull = boolType }
            val boolean = buildBinaryExpression {
                coneTypeOrNull = boolType
                this.left = leftOperand
                right = terminating
                operation = CfirSourceOperator.LOGICAL_AND
            }
            fixture.input.loop.body.replaceStatements(listOf(boolean))
            fixture.input.loop.body.replaceConeTypeOrNull(boolType)
            val run = buildRange(fixture)
            val call = run.graph.nodes.single { it.fir === terminating }
            val entry = run.graph.nodes.filterIsInstance<BooleanOperatorEnterNode>().single()
            assertFalse(hasLivePath(call, run.iteration))
            assertEquals(!left, hasLivePath(entry, run.iteration))
            assertEquals(left, run.iteration.isDead)
        }
    }

    @Test
    fun rangeGuardFalseAndContinueReachReservedLatchForward() {
        val filtered = buildRange(rangeFixture(CfirSourceOperator.LESS, false, Completion.NORMAL))
        assertForward(checkNotNull(filtered.guardExit), filtered.iteration)
        assertFalse(filtered.guardExit.edgeTo(filtered.iteration).kind.isDead)
        assertTrue(filtered.bodyEnter.isDead)
        assertFalse(filtered.iteration.isDead)
        assertLivePath(filtered.iteration, filtered.update)
        val continued = buildRange(rangeFixture(CfirSourceOperator.LESS, null, Completion.CONTINUE))
        assertForward(checkNotNull(continued.jump), continued.iteration)
        assertFalse(continued.jump.edgeTo(continued.iteration).kind.isDead)
        assertBack(continued.update, continued.condition)
        assertLivePath(continued.jump, continued.update)
    }

    @Test
    fun rangeBreakAndReturnSkipStoredUpdate() {
        for (completion in listOf(Completion.BREAK, Completion.RETURN)) {
            val fixture = rangeFixture(CfirSourceOperator.LESS, true, completion)
            val run = buildRange(fixture)
            assertTerminatingJump(fixture.input, run)
            assertNoLivePath(checkNotNull(run.jump), run.update)
            assertNoLivePath(run.jump, run.condition)
            assertTrue(run.update.isDead)
            assertTrue(checkNotNull(run.guardExit).edgeTo(run.iteration).kind.isDead)
        }
    }

    @Test
    fun stringImplicitChecksAndUpdateUseOnlyStoredIndexAndEnd() {
        val fixture = stringFixture(null, Completion.NORMAL)
        val run = buildString(fixture)
        assertStringStorage(fixture)
        val checks = run.graph.nodes.filterIsInstance<ForInLoopStringCheckNode>()
        assertEquals(2, checks.size)
        checks.forEach { check ->
            assertSame(fixture.input.loop, check.fir)
            assertSame(fixture.index, check.index)
            assertSame(fixture.end, check.bound)
        }
        val update = assertIs<ForInLoopStringUpdateNode>(run.update)
        assertSame(fixture.input.loop, update.fir)
        assertSame(fixture.index, update.index)
        assertEquals(1, run.graph.nodes.filterIsInstance<ForInLoopStringUpdateNode>().size)
        assertOrdered(run.graph, run.initializers + listOf(run.firstCheck, run.initialCheckExit,
            run.condition, run.repeatedCheck, run.success, run.binding, run.bodyEnter, run.bodyExit,
            run.iteration, run.update))
        val initializer = checkNotNull(fixture.input.binding.initializer)
        val bindingRead = run.graph.nodes.single { it.fir === initializer }
        assertOrdered(run.graph, listOf(run.success, run.binding, bindingRead, run.bodyEnter))
        assertSame(fixture.elementRead, initializer)
        assertEquals(2, run.graph.nodes.filterIsInstance<CallExpressionNode>().size)
        assertForward(run.bodyExit, run.iteration)
        assertBack(run.update, run.condition)
        assertInitializersOutsideRepeat(run)
        assertNoLivePath(run.iteration, run.firstCheck)
        assertExhaustion(run.initialCheckExit, run)
        assertExhaustion(run.success, run)
    }

    @Test
    fun stringGuardFalseAndContinuePerformImplicitUpdateBeforeCheckingAgain() {
        val filtered = buildString(stringFixture(false, Completion.NORMAL))
        assertForward(checkNotNull(filtered.guardExit), filtered.iteration)
        assertFalse(filtered.guardExit.edgeTo(filtered.iteration).kind.isDead)
        assertTrue(filtered.bodyEnter.isDead)
        assertFalse(filtered.iteration.isDead)
        assertLivePath(filtered.iteration, filtered.update)
        assertBack(filtered.update, filtered.condition)
        val continued = buildString(stringFixture(null, Completion.CONTINUE))
        assertForward(checkNotNull(continued.jump), continued.iteration)
        assertLivePath(continued.jump, continued.update)
        assertBack(continued.update, continued.condition)
    }

    @Test
    fun stringBreakAndReturnDoNotUpdateIndexOrRepeatSizeGetter() {
        for (completion in listOf(Completion.BREAK, Completion.RETURN)) {
            val fixture = stringFixture(null, completion)
            val run = buildString(fixture)
            assertTerminatingJump(fixture.input, run)
            assertNoLivePath(checkNotNull(run.jump), run.update)
            assertNoLivePath(run.jump, run.condition)
            assertTrue(run.update.isDead)
            assertEquals(1, run.graph.nodes.count { it.fir === fixture.sizeCall })
            assertInitializersOutsideRepeat(run)
        }
    }

    @Test
    fun iteratorRepeatsStoredNextAndExhaustionBypassesOriginalPattern() {
        val fixture = iteratorFixture(null, Completion.NORMAL)
        val run = buildIterator(fixture)
        assertIteratorStorage(fixture)
        assertOrdered(run.graph, run.initializers + listOf(run.iteration, run.next, run.check,
            run.success, run.binding, run.bodyEnter, run.bodyExit))
        assertSame(fixture.next, run.next.fir)
        assertSame(fixture.next, run.check.resultExpression)
        assertSame(fixture.constructorReference, run.check.constructorReference)
        assertSame(fixture.input.loop, run.check.fir)
        assertEquals(1, run.graph.nodes.count { it.fir === fixture.next })
        assertEquals(2, run.graph.nodes.filterIsInstance<CallExpressionNode>().size)
        assertForward(run.next, run.check)
        assertForward(run.check, run.success)
        assertFalse(run.graph.nodes.any { it.fir === fixture.selectorOwner })
        assertFalse(run.graph.nodes.any { it.fir === fixture.somePattern.constructor })
        assertNull(fixture.input.binding.initializer)
        assertBack(run.bodyExit, run.iteration)
        assertLivePath(run.iteration, run.next)
        assertExhaustion(run.success, run)
        assertInitializersOutsideRepeat(run)
    }

    @Test
    fun iteratorGuardFalseAndContinueRepeatNextWithoutReinitializingCursor() {
        val filtered = buildIterator(iteratorFixture(false, Completion.NORMAL))
        assertBack(checkNotNull(filtered.guardExit), filtered.iteration)
        assertFalse(filtered.guardExit.edgeTo(filtered.iteration).kind.isDead)
        assertTrue(filtered.bodyEnter.isDead)
        assertLivePath(filtered.guardExit, filtered.next)
        assertInitializersOutsideRepeat(filtered)
        val continued = buildIterator(iteratorFixture(null, Completion.CONTINUE))
        assertBack(checkNotNull(continued.jump), continued.iteration)
        assertLivePath(continued.jump, continued.next)
        assertInitializersOutsideRepeat(continued)
    }

    @Test
    fun iteratorBreakAndReturnDoNotCallNextAgain() {
        for (completion in listOf(Completion.BREAK, Completion.RETURN)) {
            val fixture = iteratorFixture(true, completion)
            val run = buildIterator(fixture)
            assertTerminatingJump(fixture.input, run)
            assertNoLivePath(checkNotNull(run.jump), run.next)
            assertNoLivePath(run.jump, run.iteration)
            assertTrue(checkNotNull(run.guardExit).edgeTo(run.iteration).kind.isDead)
            assertEquals(1, run.graph.nodes.count { it.fir === fixture.iterator.initializer })
            assertEquals(1, run.graph.nodes.count { it.fir === fixture.next })
        }
    }

    @Test
    fun bodyResolveRangeEvaluatesInitializersAndBindsBeforeGuard() {
        val fixture = rangeFixture(CfirSourceOperator.LESS, true, Completion.NORMAL)
        val graph = resolveBody(fixture.input)
        val initializers = listOf(fixture.index, fixture.stop).map { variable ->
            graph.nodes.filterIsInstance<VariableDeclarationExitNode>().single { it.fir === variable }
        }
        val first = graph.nodes.filterIsInstance<BinaryExpressionNode>().single { it.fir === fixture.comparison }
        val condition = graph.nodes.filterIsInstance<ForInLoopConditionEnterNode>().single()
        val repeated = graph.nodes.filterIsInstance<ForInLoopRangeCheckNode>().single()
        val update = graph.nodes.filterIsInstance<VariableAssignmentNode>().single { it.fir === fixture.update }
        assertSame(fixture.index, repeated.index)
        assertSame(fixture.stop, repeated.bound)
        assertOrdered(graph, initializers + listOf(first, condition, repeated, update))
        assertBack(update, condition)
        val read = assertIs<QualifiedAccessNode>(assertBodyBindingBeforeGuard(graph, fixture.input))
        assertReference(fixture.index, read.fir)
    }

    @Test
    fun bodyResolveStringEvaluatesStoredElementCallBeforeGuard() {
        val fixture = stringFixture(true, Completion.NORMAL)
        val graph = resolveBody(fixture.input)
        val checks = graph.nodes.filterIsInstance<ForInLoopStringCheckNode>()
        assertEquals(2, checks.size)
        checks.forEach {
            assertSame(fixture.index, it.index)
            assertSame(fixture.end, it.bound)
        }
        assertSame(fixture.elementRead, assertBodyBindingBeforeGuard(graph, fixture.input).fir)
    }

    @Test
    fun bodyResolveIteratorBindsOriginalPatternAfterCheckingStoredNext() {
        val fixture = iteratorFixture(true, Completion.NORMAL)
        val graph = resolveBody(fixture.input)
        val next = graph.nodes.filterIsInstance<CallExpressionNode>().single { it.fir === fixture.next }
        val check = graph.nodes.filterIsInstance<ForInLoopIteratorCheckNode>().single()
        val success = graph.nodes.filterIsInstance<ForInLoopIterationExitNode>().single()
        val binding = graph.nodes.filterIsInstance<VariableDeclarationExitNode>().single { it.fir === fixture.input.binding }
        val guard = graph.nodes.filterIsInstance<ForInLoopGuardExitNode>().single()
        assertSame(fixture.next, check.resultExpression)
        assertSame(fixture.constructorReference, check.constructorReference)
        assertNull(fixture.input.binding.initializer)
        assertOrdered(graph, listOf(next, check, success, binding, guard))
        assertLivePath(success, binding)
    }

    private fun assertBodyBindingBeforeGuard(graph: ControlFlowGraph, input: Input): CFGNode<*> {
        val initializer = assertNotNull(input.binding.initializer)
        val success = graph.nodes.filterIsInstance<ForInLoopIterationExitNode>().single { it.fir === input.loop }
        val bindingEnter = graph.nodes.filterIsInstance<VariableDeclarationEnterNode>().single { it.fir === input.binding }
        val initializerNode = graph.nodes.single { it.fir === initializer }
        val bindingExit = graph.nodes.filterIsInstance<VariableDeclarationExitNode>().single { it.fir === input.binding }
        val guard = graph.nodes.filterIsInstance<LiteralExpressionNode>().single { it.fir === input.loop.patternGuard }
        assertOrdered(graph, listOf(success, bindingEnter, initializerNode, bindingExit, guard))
        return initializerNode
    }

    private fun resolveBody(input: Input): ControlFlowGraph {
        val configuration = CompilerConfiguration.createForCfirFrontend()
        val factory = CfirDefaultSessionFactory()
        val shared = factory.createSharedLibrarySession(module.name, emptyList(), configuration.languageVersionSettings)
        factory.createLibrarySession(
            shared, SingleModuleDataProvider(bodyLibraryModule), emptyList(), configuration.languageVersionSettings,
        )
        val session = factory.createSourceSession(module, emptyList(), configuration)
        val function = function(
            input.functionSymbol.callableId, unitType, buildBlock { statements += input.loop },
            functionSymbol = input.functionSymbol, parameters = input.parameters,
            phase = CfirResolvePhase.IMPLICIT_TYPES,
        )
        input.returnTarget.bind(function)
        val file = buildFile {
            source = source("nativeFor.cj")
            moduleData = module
            resolvePhase = CfirResolvePhase.IMPLICIT_TYPES
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            name = "nativeFor.cj"
            symbol = CfirFileSymbol()
            packageDirective = buildPackageDirective {
                packageFqName = FqName("nativefor")
                isMacroPackage = false
            }
            declarations += function
        }
        CfirImportResolveTransformer(session, CfirDiagnosticCollector()).recordImportBindingsForFile(file)
        CfirBodyResolveTransformer(session, ScopeSession(), ReturnTypeCalculatorForFullBodyResolve.Default)
            .transformFile(file, ResolutionMode.ContextIndependent)
        val graph = assertIs<CfirControlFlowGraphReferenceImpl>(function.controlFlowGraphReference).controlFlowGraph
        assertSame(function, graph.declaration)
        assertSame(input.loop, function.body?.statements?.single())
        return graph
    }

    private enum class Completion { NORMAL, CONTINUE, BREAK, RETURN }

    /** 每个载荷保存原 P、正文和借用 target；构图过程不能重新制造这些身份。 */
    private data class Input(
        val loop: CfirForInExpression,
        val binding: CfirPatternBindingVariable,
        val originalPattern: CfirBindingPattern,
        val target: CfirLoopTarget,
        val returnTarget: CfirFunctionTarget,
        val completion: Completion,
        val functionSymbol: CfirNamedFunctionSymbol,
        val parameters: List<CfirValueParameter>,
    )

    private data class RangeFixture(
        val input: Input,
        val storage: CfirBlock,
        val index: CfirPatternBindingVariable,
        val stop: CfirPatternBindingVariable,
        val update: CfirAssignment,
        val comparison: CfirBinaryExpression,
    )

    private data class StringFixture(
        val input: Input,
        val storage: CfirBlock,
        val index: CfirPatternBindingVariable,
        val temporary: CfirPatternBindingVariable,
        val end: CfirPatternBindingVariable,
        val sizeCall: CfirCallExpression,
        val elementRead: CfirCallExpression,
    )

    private data class IteratorFixture(
        val input: Input,
        val storage: CfirBlock,
        val iterator: CfirPatternBindingVariable,
        val selectorOwner: CfirNativeMatchExpression,
        val next: CfirCallExpression,
        val somePattern: CfirEnumPattern,
        val constructor: CfirEnumConstructor,
        val constructorReference: CfirResolvedNamedReference,
    )

    private open class Run(
        val graph: ControlFlowGraph,
        val function: CfirNamedFunction,
        val functionExit: FunctionExitNode,
        val initializers: List<VariableDeclarationExitNode>,
        val iteration: ForInLoopIterationEnterNode,
        val success: ForInLoopIterationExitNode,
        val binding: VariableDeclarationEnterNode,
        val guardExit: ForInLoopGuardExitNode?,
        val bodyEnter: LoopBlockEnterNode,
        val bodyExit: LoopBlockExitNode,
        val loopExit: LoopExitNode,
        val jump: JumpNode?,
    )

    private class IndexedRun(
        graph: ControlFlowGraph, function: CfirNamedFunction, functionExit: FunctionExitNode,
        initializers: List<VariableDeclarationExitNode>, iteration: ForInLoopIterationEnterNode,
        success: ForInLoopIterationExitNode, binding: VariableDeclarationEnterNode,
        guardExit: ForInLoopGuardExitNode?, bodyEnter: LoopBlockEnterNode, bodyExit: LoopBlockExitNode,
        loopExit: LoopExitNode, jump: JumpNode?,
        val firstCheck: CFGNode<*>,
        val initialCheckExit: ForInLoopInitialCheckExitNode,
        val condition: ForInLoopConditionEnterNode,
        val repeatedCheck: CFGNode<*>,
        val update: CFGNode<*>,
    ) : Run(graph, function, functionExit, initializers, iteration, success, binding,
        guardExit, bodyEnter, bodyExit, loopExit, jump)

    private class IteratorRun(
        graph: ControlFlowGraph, function: CfirNamedFunction, functionExit: FunctionExitNode,
        initializers: List<VariableDeclarationExitNode>, iteration: ForInLoopIterationEnterNode,
        success: ForInLoopIterationExitNode, binding: VariableDeclarationEnterNode,
        guardExit: ForInLoopGuardExitNode?, bodyEnter: LoopBlockEnterNode, bodyExit: LoopBlockExitNode,
        loopExit: LoopExitNode, jump: JumpNode?,
        val next: CallExpressionNode,
        val check: ForInLoopIteratorCheckNode,
    ) : Run(graph, function, functionExit, initializers, iteration, success, binding,
        guardExit, bodyEnter, bodyExit, loopExit, jump)

    private fun rangeFixture(operation: CfirSourceOperator, guard: Boolean?, completion: Completion): RangeFixture {
        val descending = operation == CfirSourceOperator.GREATER || operation == CfirSourceOperator.GREATER_EQUAL
        val index = variable("index", intType, integer(if (descending) 3 else 0), true)
        val stop = variable("stop", intType, integer(if (descending) 0 else 3), false)
        val update = buildAssignment {
            source = source("stored-range-update")
            coneTypeOrNull = unitType
            lValue = access(index)
            rValue = integer(if (descending) -1 else 1)
            this.operation = CfirSourceOperator.ADD_ASSIGN
            isCompound = true
        }
        val comparison = buildBinaryExpression {
            source = source("stored-range-comparison")
            coneTypeOrNull = boolType
            left = access(index)
            right = access(stop)
            this.operation = operation
        }
        val storage = buildBlock { statements += listOf(index, stop, update, comparison); coneTypeOrNull = boolType }
        val binding = variable("element", intType, access(index), false)
        return RangeFixture(input(CfirSourceForInKind.RANGE, storage, binding, guard, completion),
            storage, index, stop, update, comparison)
    }

    private fun stringFixture(guard: Boolean?, completion: Completion): StringFixture {
        val index = variable("index", intType, integer(0), true)
        val text = buildLiteralExpression { kind = CfirLiteralKind.STRING; value = "ab"; coneTypeOrNull = stringType }
        val temporary = variable("text", stringType, text, false)
        val sizeGetter = function(CallableId(StdlibClassIds.String, Name.identifier("size")), intType)
        val sizeCall = call(sizeGetter, access(temporary))
        val end = variable("end", intType, sizeCall, false)
        val indexGetter = function(CallableId(StdlibClassIds.String, Name.identifier("[]")), byteType,
            parameterTypes = listOf(intType))
        val elementRead = call(indexGetter, access(temporary), listOf(access(index)))
        val binding = variable("element", byteType, elementRead, false)
        val storage = buildBlock { statements += listOf(index, temporary, end); coneTypeOrNull = unitType }
        return StringFixture(input(CfirSourceForInKind.STRING, storage, binding, guard, completion),
            storage, index, temporary, end, sizeCall, elementRead)
    }

    private fun iteratorFixture(guard: Boolean?, completion: Completion): IteratorFixture {
        val sequenceType = ConeClassLikeType(ConeClassLikeLookupTagImpl(StdlibClassIds.Iterable), listOf(intType), isInterface = true)
        val functionSymbol = CfirNamedFunctionSymbol(CallableId(FqName("nativefor"), Name.identifier("execute")))
        val sequence = parameter("sequence", sequenceType, functionSymbol)
        val iteratorFunction = function(CallableId(StdlibClassIds.Iterable, Name.identifier("iterator")), cursorType)
        val iterator = variable("cursor", cursorType, call(iteratorFunction, access(sequence)), false)
        val nextFunction = function(CallableId(cursorId, Name.identifier("next")), optionType)
        val next = call(nextFunction, access(iterator))
        val selectorOwner = buildNativeMatchExpression { selector = next; coneTypeOrNull = optionType }
        val binding = variable("element", intType, null, false)
        val originalPattern = bindingPattern(binding)
        val someId = CallableId(StdlibClassIds.Option, Name.identifier("Some"))
        val someSymbol = CfirEnumConstructorSymbol(someId)
        val constructor = buildEnumConstructor {
            source = source("Some")
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            status = CfirDeclarationStatusImpl()
            name = someId.callableName
            symbol = someSymbol
            returnTypeRef = typeRef(optionType)
            valueParameters += parameter("payload", intType, someSymbol)
        }
        val constructorReference = buildResolvedNamedReference { name = constructor.name; resolvedSymbol = constructor.symbol }
        val somePattern = buildEnumPattern {
            this.constructor = buildNamedAccessExpression {
                calleeReference = constructorReference
                coneTypeOrNull = ConeFunctionType(listOf(intType), optionType)
            }
            arguments += originalPattern
            semanticTypeRef = typeRef(optionType)
        }
        val storage = buildBlock { statements += listOf(iterator, selectorOwner); coneTypeOrNull = optionType }
        return IteratorFixture(input(CfirSourceForInKind.ITERATOR, storage, binding, guard, completion,
            originalPattern, somePattern, functionSymbol, listOf(sequence)),
            storage, iterator, selectorOwner, next, somePattern, constructor, constructorReference)
    }

    private fun input(kind: CfirSourceForInKind, storage: CfirBlock, binding: CfirPatternBindingVariable,
        guard: Boolean?, completion: Completion, original: CfirBindingPattern = bindingPattern(binding),
        pattern: CfirPattern = original,
        functionSymbol: CfirNamedFunctionSymbol = CfirNamedFunctionSymbol(CallableId(FqName("nativefor"), Name.identifier("execute"))),
        parameters: List<CfirValueParameter> = emptyList()): Input {
        val target = CfirLoopTarget()
        val returnTarget = CfirFunctionTarget(isLambda = false)
        val bodyExpression = when (completion) {
            Completion.NORMAL -> unitLiteral()
            Completion.CONTINUE -> buildContinueExpression { this.target = target; coneTypeOrNull = ConePrimitiveType.NOTHING }
            Completion.BREAK -> buildBreakExpression { this.target = target; coneTypeOrNull = ConePrimitiveType.NOTHING }
            Completion.RETURN -> buildReturnExpression {
                this.target = returnTarget; result = unitLiteral(); coneTypeOrNull = ConePrimitiveType.NOTHING
            }
        }
        val loop = buildForInExpression {
            source = source("native-for")
            coneTypeOrNull = unitType
            iterationKind = kind
            iterable = storage
            this.pattern = pattern
            patternGuard = guard?.let { boolean(it) }
            body = buildBlock { statements += bodyExpression; coneTypeOrNull = bodyExpression.coneTypeOrNull }
        }
        target.bind(loop)
        return Input(loop, binding, original, target, returnTarget, completion, functionSymbol, parameters)
    }

    /** 首检查与 latch 的调用顺序直接来自 native facts，不从待验证实现生成期待。 */
    private fun buildRange(fixture: RangeFixture): IndexedRun = buildIndexed(fixture.input,
        listOf(fixture.index, fixture.stop),
        check = { emit(fixture.comparison) }, update = { emit(fixture.update) },
        repeatCheck = { builder.exitForInLoopRangeCheck(fixture.comparison, fixture.index, fixture.stop) })

    private fun buildString(fixture: StringFixture): IndexedRun = buildIndexed(fixture.input,
        listOf(fixture.index, fixture.temporary, fixture.end),
        check = { builder.exitForInLoopStringCheck(input.loop, fixture.index, fixture.end) },
        update = { builder.exitForInLoopStringUpdate(input.loop, fixture.index) })

    private fun buildIndexed(input: Input, declarations: List<CfirVariable>,
        check: Harness.() -> CFGNode<*>, update: Harness.() -> CFGNode<*>,
        repeatCheck: Harness.() -> CFGNode<*> = check): IndexedRun {
        val harness = Harness(input)
        val builder = harness.builder
        val (_, reservedLatch) = builder.enterForInLoop(input.loop)
        assertFalse(reservedLatch.isEntered)
        val initializers = declarations.map(harness::declare)
        val first = harness.check()
        val initialExit = builder.exitForInLoopInitialCheck(input.loop)
        val condition = builder.enterForInLoopCondition(input.loop)
        val repeatedCheck = harness.repeatCheck()
        val success = builder.exitForInLoopIteration(input.loop)
        val binding = harness.bindPattern()
        input.loop.patternGuard?.let(harness::emit)
        val bodyEnter = builder.exitForInLoopGuard(input.loop).second
        val guardExit = if (input.loop.patternGuard == null) null else assertIs<ForInLoopGuardExitNode>(bodyEnter.previousNodes.single())
        val jump = harness.body()
        val bodyExit = builder.exitForInLoopBody(input.loop)
        // guard/continue 必须已接到尚未进入的同一个 latch，不能在后面新建目标。
        assertFalse(reservedLatch.isEntered)
        guardExit?.let { assertForward(it, reservedLatch) }
        if (input.completion == Completion.CONTINUE) {
            assertContinueIdentity(input, checkNotNull(jump))
            assertForward(jump, reservedLatch)
        }
        val enteredLatch = builder.enterForInLoopIteration(input.loop)
        assertSame(reservedLatch, enteredLatch)
        assertTrue(enteredLatch.isEntered)
        val updateNode = harness.update()
        val (repeat, savedBodyExit, loopExit) = builder.exitForInLoop(input.loop)
        assertSame(reservedLatch, repeat)
        assertSame(bodyExit, savedBodyExit)
        val (functionExit, graph) = harness.finish()
        return IndexedRun(graph, harness.function, functionExit, initializers, repeat, success, binding,
            guardExit, bodyEnter, bodyExit, loopExit, jump, first, initialExit, condition, repeatedCheck, updateNode)
    }

    private fun buildIterator(fixture: IteratorFixture): IteratorRun {
        val harness = Harness(fixture.input)
        val builder = harness.builder
        val (_, reservedIteration) = builder.enterForInLoop(fixture.input.loop)
        assertFalse(reservedIteration.isEntered)
        val initialization = harness.declare(fixture.iterator)
        val iteration = builder.enterForInLoopIteration(fixture.input.loop)
        assertSame(reservedIteration, iteration)
        val next = assertIs<CallExpressionNode>(harness.emit(fixture.next))
        val check = builder.exitForInLoopIteratorCheck(fixture.input.loop, fixture.next, fixture.constructorReference, fixture.constructorReference.resolvedSymbol)
        val success = builder.exitForInLoopIteration(fixture.input.loop)
        val binding = harness.bindPattern()
        fixture.input.loop.patternGuard?.let(harness::emit)
        val bodyEnter = builder.exitForInLoopGuard(fixture.input.loop).second
        val guardExit = if (fixture.input.loop.patternGuard == null) null else assertIs<ForInLoopGuardExitNode>(bodyEnter.previousNodes.single())
        val jump = harness.body()
        if (fixture.input.completion == Completion.CONTINUE) assertContinueIdentity(fixture.input, checkNotNull(jump))
        val bodyExit = builder.exitForInLoopBody(fixture.input.loop)
        val (repeat, savedBodyExit, loopExit) = builder.exitForInLoop(fixture.input.loop)
        assertSame(iteration, repeat)
        assertSame(bodyExit, savedBodyExit)
        val (functionExit, graph) = harness.finish()
        return IteratorRun(graph, harness.function, functionExit, listOf(initialization), repeat, success, binding,
            guardExit, bodyEnter, bodyExit, loopExit, jump, next, check)
    }

    /** 仅发出这些手工载荷确实拥有的表达式事件；不构造隐式阶段的替代 AST。 */
    private inner class Harness(val input: Input) {
        val builder = ControlFlowGraphBuilder()
        private val outerBody = buildBlock { statements += input.loop; coneTypeOrNull = unitType }
        val function = function(input.functionSymbol.callableId, unitType, outerBody,
            functionSymbol = input.functionSymbol, parameters = input.parameters)

        init {
            input.returnTarget.bind(function)
            builder.enterFunction(function)
            input.parameters.forEach { parameter ->
                assertSame(function.symbol, parameter.containingDeclarationSymbol)
                builder.enterValueParameter(parameter)
                builder.exitValueParameter(parameter, defaultGraph = null)
            }
            builder.enterBlock(outerBody)
        }

        fun declare(variable: CfirVariable): VariableDeclarationExitNode {
            builder.enterVariableDeclaration(variable)
            variable.initializer?.let(::emit)
            return builder.exitVariableDeclaration(variable)
        }

        fun bindPattern(): VariableDeclarationEnterNode {
            val enter = builder.enterVariableDeclaration(input.binding)
            input.binding.initializer?.let(::emit)
            builder.exitVariableDeclaration(input.binding)
            return enter
        }

        fun body(): JumpNode? {
            builder.enterBlock(input.loop.body)
            val expression = input.loop.body.statements.single() as CfirExpression
            val jump = if (expression is CfirJump<*>) {
                builder.enterJump(expression)
                if (expression is CfirReturnExpression) emit(expression.result)
                builder.exitJump(expression)
            } else {
                emit(expression)
                null
            }
            builder.exitBlock(input.loop.body)
            return jump
        }

        fun emit(expression: CfirExpression): CFGNode<*> = when (expression) {
            is CfirNamedAccessExpression -> {
                expression.explicitReceiver?.let(::emit)
                if (expression.dispatchReceiver !== expression.explicitReceiver) expression.dispatchReceiver?.let(::emit)
                builder.exitQualifiedAccessExpression(expression)
            }
            is CfirLiteralExpression -> builder.exitLiteralExpression(expression)
            is CfirCallExpression -> {
                emit(expression.base)
                expression.arguments.forEach { emit(checkNotNull(it.value)) }
                builder.exitCallExpression(expression)
            }
            is CfirFunctionCall -> {
                // 与固定 Kotlin 3c7a5041 的正常调用顺序一致：参数阶段先于 callee 阶段。
                builder.enterCallArguments(expression, expression.argumentList.arguments.mapNotNull {
                    it.unwrapAnonymousFunctionExpression()?.anonymousFunction
                })
                expression.explicitReceiver?.let(::emit)
                builder.exitCallExplicitReceiver()
                expression.argumentList.arguments.forEach(::emit)
                builder.exitCallArguments()
                builder.enterFunctionCall(expression)
                builder.exitFunctionCall(expression, callCompleted = true).value
            }
            is CfirBinaryExpression -> {
                if (expression.operation == CfirSourceOperator.LOGICAL_AND || expression.operation == CfirSourceOperator.LOGICAL_OR) {
                    builder.enterBooleanOperatorExpression(expression)
                    emit(expression.left)
                    builder.exitLeftBooleanOperatorExpressionArgument(expression)
                    emit(expression.right)
                    builder.exitBooleanOperatorExpression(expression)
                } else {
                    emit(expression.left)
                    emit(expression.right)
                    builder.exitBinaryExpression(expression)
                }
            }
            is CfirAssignment -> {
                emit(expression.lValue)
                emit(expression.rValue)
                builder.exitVariableAssignment(expression)
            }
            else -> error("Fixture contains an expression without an explicit native execution event: ${expression::class}")
        }

        fun finish(): Pair<FunctionExitNode, ControlFlowGraph> {
            builder.exitBlock(outerBody)
            return builder.exitFunction(function).also { (exit, graph) ->
                assertSame(function, graph.declaration)
                assertSame(exit, graph.exitNode)
                assertEquals(graph.nodeCount, graph.nodes.size)
                assertEquals(graph.nodes.size, graph.nodes.map { it.id }.toSet().size)
                assertSame(input.loop, graph.nodes.filterIsInstance<LoopEnterNode>().single().fir)
                assertSame(input.loop, graph.nodes.filterIsInstance<LoopBlockEnterNode>().single().fir)
                assertFalse(graph.nodes.any { it is LoopConditionEnterNode || it is LoopConditionExitNode })
            }
        }
    }

    private fun assertRangeStorage(fixture: RangeFixture) {
        assertSlots(fixture.storage, listOf(fixture.index, fixture.stop, fixture.update, fixture.comparison))
        assertSame(fixture.storage, fixture.input.loop.iterable)
        assertSame(fixture.input.originalPattern, fixture.input.loop.pattern)
        assertSame(fixture.input.binding, fixture.input.originalPattern.bindingVariable)
        assertReference(fixture.index, fixture.update.lValue)
        assertReference(fixture.index, fixture.comparison.left)
        assertReference(fixture.stop, fixture.comparison.right)
        assertReference(fixture.index, checkNotNull(fixture.input.binding.initializer))
        assertNotSame(fixture.update.lValue, fixture.comparison.left)
        assertNotSame(fixture.comparison.left, fixture.input.binding.initializer)
        assertSame(CfirSourceOperator.ADD_ASSIGN, fixture.update.operation)
        assertTrue(fixture.update.isCompound)
    }

    private fun assertStringStorage(fixture: StringFixture) {
        assertSlots(fixture.storage, listOf(fixture.index, fixture.temporary, fixture.end))
        assertSame(fixture.storage, fixture.input.loop.iterable)
        assertSame(fixture.input.originalPattern, fixture.input.loop.pattern)
        assertSame(fixture.input.binding, fixture.input.originalPattern.bindingVariable)
        assertSame(fixture.sizeCall, fixture.end.initializer)
        assertReference(fixture.temporary, assertIs<CfirNamedAccessExpression>(fixture.sizeCall.base).explicitReceiver!!)
        assertReference(fixture.temporary, assertIs<CfirNamedAccessExpression>(fixture.elementRead.base).explicitReceiver!!)
        assertReference(fixture.index, checkNotNull(fixture.elementRead.arguments.single().value))
        assertSame(fixture.elementRead, fixture.input.binding.initializer)
    }

    private fun assertIteratorStorage(fixture: IteratorFixture) {
        assertSlots(fixture.storage, listOf(fixture.iterator, fixture.selectorOwner))
        assertSame(fixture.storage, fixture.input.loop.iterable)
        assertTrue(fixture.selectorOwner.cases.isEmpty())
        assertSame(fixture.next, fixture.selectorOwner.selector)
        assertSame(optionType, fixture.selectorOwner.coneTypeOrNull)
        assertSame(fixture.somePattern, fixture.input.loop.pattern)
        assertSame(fixture.input.originalPattern, fixture.somePattern.arguments.single())
        assertSame(fixture.input.binding, fixture.input.originalPattern.bindingVariable)
        val someReference = assertIs<CfirResolvedNamedReference>(assertIs<CfirNamedAccessExpression>(fixture.somePattern.constructor).calleeReference)
        assertSame(fixture.constructorReference, someReference)
        assertSame(fixture.constructor.symbol, someReference.resolvedSymbol)
        assertSame(fixture.constructor, someReference.resolvedSymbol.cfir)
        assertReference(fixture.iterator, assertIs<CfirNamedAccessExpression>(fixture.next.base).explicitReceiver!!)
        val iteratorCall = assertIs<CfirCallExpression>(fixture.iterator.initializer)
        assertReference(fixture.input.parameters.single(), assertIs<CfirNamedAccessExpression>(iteratorCall.base).explicitReceiver!!)
        assertNull(fixture.input.binding.initializer)
    }

    private fun assertSlots(block: CfirBlock, expected: List<CfirStatement>) {
        assertEquals(expected.size, block.statements.size)
        expected.zip(block.statements).forEach { (original, stored) -> assertSame(original, stored) }
    }

    private fun assertReference(variable: CfirVariable, expression: CfirExpression) {
        val reference = assertIs<CfirResolvedNamedReference>(assertIs<CfirNamedAccessExpression>(expression).calleeReference)
        assertSame(variable.symbol, reference.resolvedSymbol)
        assertSame(variable, reference.resolvedSymbol.cfir)
    }

    private fun assertInitializersOutsideRepeat(run: Run) {
        run.initializers.forEach { initializer ->
            assertEquals(1, run.graph.nodes.filterIsInstance<VariableDeclarationExitNode>().count { it.fir === initializer.fir })
            assertFalse(initializer.previousNodes.any { it.edgeTo(initializer).kind.isBack })
            assertNoLivePath(run.iteration, initializer)
            initializer.fir.initializer?.let { expression ->
                val node = run.graph.nodes.single { it.fir === expression }
                assertNoLivePath(run.iteration, node)
            }
        }
    }

    private fun assertExhaustion(check: CFGNode<*>, run: Run) {
        assertForward(check, run.loopExit)
        assertFalse(check.edgeTo(run.loopExit).kind.isDead)
        // 选择耗尽出口的分支后，不能再经过 P、guard、body 或任何重复操作。
        assertNoLivePath(run.loopExit, run.binding)
        run.guardExit?.let { assertNoLivePath(run.loopExit, it) }
        assertNoLivePath(run.loopExit, run.bodyEnter)
        assertNoLivePath(run.loopExit, run.iteration)
    }

    private fun assertTerminatingJump(input: Input, run: Run) {
        val jump = checkNotNull(run.jump)
        assertSame(input.loop.body.statements.single(), jump.fir)
        val destination = when (input.completion) {
            Completion.BREAK -> {
                assertSame(input.target, assertIs<CfirBreakExpression>(jump.fir).target)
                assertSame(input.loop, input.target.labeledElement)
                run.loopExit
            }
            Completion.RETURN -> {
                assertSame(input.returnTarget, assertIs<CfirReturnExpression>(jump.fir).target)
                assertSame(run.function, input.returnTarget.labeledElement)
                run.functionExit
            }
            else -> error("This assertion requires a terminating native path")
        }
        assertForward(jump, destination)
        assertFalse(jump.edgeTo(destination).kind.isDead)
    }

    private fun assertContinueIdentity(input: Input, jump: JumpNode) {
        val expression = assertIs<CfirContinueExpression>(jump.fir)
        assertSame(input.loop.body.statements.single(), expression)
        assertSame(input.target, expression.target)
        assertSame(input.loop, input.target.labeledElement)
    }

    private fun assertOrdered(graph: ControlFlowGraph, expected: List<CFGNode<*>>) {
        val positions = expected.map { node -> graph.nodes.indexOfFirst { it === node }.also { assertTrue(it >= 0) } }
        positions.zipWithNext().forEach { (before, after) -> assertTrue(before < after, "Native stage order: $positions") }
    }

    private fun assertForward(from: CFGNode<*>, to: CFGNode<*>) {
        assertTrue(from.followingNodes.any { it === to }, "Missing native forward edge ${from.id} -> ${to.id}")
        assertFalse(from.edgeTo(to).kind.isBack)
    }

    private fun assertBack(from: CFGNode<*>, to: CFGNode<*>) {
        assertTrue(from.followingNodes.any { it === to }, "Missing native repeat edge ${from.id} -> ${to.id}")
        assertTrue(from.edgeTo(to).kind.isBack)
    }

    private fun assertLivePath(from: CFGNode<*>, to: CFGNode<*>) = assertTrue(hasLivePath(from, to))
    private fun assertNoLivePath(from: CFGNode<*>, to: CFGNode<*>) = assertFalse(hasLivePath(from, to),
        "Native path must bypass node ${to.id}, starting at ${from.id}")

    private fun hasLivePath(from: CFGNode<*>, to: CFGNode<*>): Boolean {
        val pending = ArrayDeque<CFGNode<*>>()
        val visited = mutableSetOf<CFGNode<*>>()
        pending.add(from)
        while (pending.isNotEmpty()) {
            val node = pending.removeFirst()
            if (node === to) return true
            if (!visited.add(node)) continue
            node.followingNodes.filter { !node.edgeTo(it).kind.isDead }.forEach(pending::addLast)
        }
        return false
    }

    private fun variable(name: String, type: ConeCangJieType, initializer: CfirExpression?, mutable: Boolean): CfirPatternBindingVariable =
        buildPatternBindingVariable {
            source = source(name)
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            status = CfirDeclarationStatusImpl()
            this.name = Name.identifier(name)
            symbol = CfirPatternBindingSymbol(CallableId(this.name))
            returnTypeRef = typeRef(type)
            this.initializer = initializer
            isVar = mutable
        }

    private fun bindingPattern(binding: CfirPatternBindingVariable): CfirBindingPattern = buildBindingPattern {
        name = binding.name
        bindingVariable = binding
        semanticTypeRef = binding.returnTypeRef
        typeRef = binding.returnTypeRef
    }

    private fun access(variable: CfirVariable): CfirNamedAccessExpression = buildNamedAccessExpression {
        source = source("read-${variable.symbol.name}")
        coneTypeOrNull = assertIs<CfirResolvedTypeRef>(variable.returnTypeRef).coneType
        calleeReference = buildResolvedNamedReference { name = variable.symbol.name; resolvedSymbol = variable.symbol }
    }

    private fun function(id: CallableId, resultType: ConeCangJieType, body: CfirBlock? = null,
        parameterTypes: List<ConeCangJieType> = emptyList(),
        functionSymbol: CfirNamedFunctionSymbol = CfirNamedFunctionSymbol(id),
        parameters: List<CfirValueParameter> = emptyList(),
        phase: CfirResolvePhase = CfirResolvePhase.BODY_RESOLVE): CfirNamedFunction {
        return buildNamedFunction {
            source = source(id.callableName.asString())
            moduleData = module
            resolvePhase = phase
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            status = CfirDeclarationStatusImpl()
            name = id.callableName
            symbol = functionSymbol
            returnTypeRef = typeRef(resultType)
            this.body = body
            isMut = false
            valueParameters += parameters
            parameterTypes.forEachIndexed { index, type -> valueParameters += parameter("arg$index", type, functionSymbol) }
        }
    }

    private fun parameter(name: String, type: ConeCangJieType,
        owner: CfirBasedSymbol<*>): CfirValueParameter = buildValueParameter {
        source = source(name)
        moduleData = module
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Source
        attributes = CfirDeclarationAttributes.EMPTY
        isLocal = true
        isNamed = false
        this.name = Name.identifier(name)
        symbol = CfirValueParameterSymbol(CallableId(this.name))
        containingDeclarationSymbol = owner
        returnTypeRef = typeRef(type)
    }

    private fun call(function: CfirNamedFunction, receiver: CfirNamedAccessExpression,
        values: List<CfirExpression> = emptyList()): CfirCallExpression = buildCallExpression {
        source = source("call-${function.name}")
        coneTypeOrNull = assertIs<CfirResolvedTypeRef>(function.returnTypeRef).coneType
        base = buildNamedAccessExpression {
            calleeReference = buildResolvedNamedReference { name = function.name; resolvedSymbol = function.symbol }
            explicitReceiver = receiver
            coneTypeOrNull = ConeFunctionType(function.valueParameters.map { assertIs<CfirResolvedTypeRef>(it.returnTypeRef).coneType },
                assertIs<CfirResolvedTypeRef>(function.returnTypeRef).coneType)
        }
        arguments += values.map { expression -> buildFunctionArgument {
            value = expression; isDefaultValue = false; withInout = false; coneTypeOrNull = expression.coneTypeOrNull
        } }
        callKind = CfirSourceCallKind.DECLARED
        hasSideEffect = true
    }

    private fun typeRef(type: ConeCangJieType): CfirResolvedTypeRef = buildResolvedTypeRef { coneType = type }
    /** 声明头来自固定 std.env.exit 签名，引用始终保留真实绑定 symbol。 */
    private fun functionAccess(function: CfirNamedFunction): CfirNamedAccessExpression = buildNamedAccessExpression {
        calleeReference = buildResolvedNamedReference { name = function.name; resolvedSymbol = function.symbol }
        coneTypeOrNull = ConeFunctionType(function.valueParameters.map { assertIs<CfirResolvedTypeRef>(it.returnTypeRef).coneType },
            assertIs<CfirResolvedTypeRef>(function.returnTypeRef).coneType)
    }
    private fun integer(value: Int): CfirLiteralExpression = buildLiteralExpression {
        kind = CfirLiteralKind.INT; this.value = value.toLong(); coneTypeOrNull = intType
    }
    private fun boolean(value: Boolean): CfirLiteralExpression = buildLiteralExpression {
        kind = CfirLiteralKind.BOOLEAN; this.value = value; coneTypeOrNull = boolType
    }
    private fun unitLiteral(): CfirLiteralExpression = buildLiteralExpression { kind = CfirLiteralKind.UNIT; value = Unit; coneTypeOrNull = unitType }
    private fun source(name: String) = object : CjBinarySourceElement(name, null, name) {}
}

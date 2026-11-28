package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.common.CfirBinaryDependenciesModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.copy.CfirOwnedAttributeRemapper
import org.cangnova.cangjie.cfir.copy.CfirOwnedGraphCopyContext
import org.cangnova.cangjie.cfir.copy.CfirTypeRefRemapper
import org.cangnova.cangjie.cfir.copy.deepCopyOwnedGraphs
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.utils.constructedClassTypeParameterRefs
import org.cangnova.cangjie.cfir.deserialization.SingleModuleDataProvider
import org.cangnova.cangjie.cfir.entrypoint.configuration.createForCfirFrontend
import org.cangnova.cangjie.cfir.entrypoint.session.CfirDefaultSessionFactory
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.patterns.builder.buildBindingPattern
import org.cangnova.cangjie.cfir.patterns.builder.buildEnumPattern
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzer
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzerContext
import org.cangnova.cangjie.cfir.resolve.body.CfirResolvedGraphAnalysisRebuilder
import org.cangnova.cangjie.cfir.resolve.body.SnapshotCfirMapper
import org.cangnova.cangjie.cfir.resolve.dfa.Flow
import org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl
import org.cangnova.cangjie.cfir.resolve.dfa.LogicSystem
import org.cangnova.cangjie.cfir.resolve.dfa.PersistentFlow
import org.cangnova.cangjie.cfir.resolve.dfa.RealVariable
import org.cangnova.cangjie.cfir.resolve.dfa.CfirResolvedGraphOwnerContext
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.*
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportSourcePayload
import org.cangnova.cangjie.cfir.resolve.providers.captureExportSourcePayload
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.resolve.transformers.CfirStatusResolver
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.source.CfirSourceCallKind
import org.cangnova.cangjie.cfir.source.CfirSourceForInKind
import org.cangnova.cangjie.cfir.source.CfirSourceOperator
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.cangnova.cangjie.test.testFramework.CjPlatformLiteFixture
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.*

/**
 * F6 的实际解析游标和 F7 的阶段能力合同。节点及 Flow 只由公开 analyzer 产生。
 * 固定 K 3c7a5041 / N 14276740；control-only 期望描述当前未闭 transfer，不能永久排除合法事件。
 */
class CfirDataFlowPositionLifecycleTest : SessionHolder {
    // 复用正规 CoreApplication/PSI 环境；组合 fixture 保持本类 kotlin.test 断言的返回值契约。
    private val platformFixture = object : CjPlatformLiteFixture() {
        fun start() = setUp()
        fun stop() = tearDown()
    }
    @BeforeEach fun setUpPlatform() = platformFixture.start()
    @AfterEach fun tearDownPlatform() = platformFixture.stop()

    private val library = CfirBinaryDependenciesModuleData(Name.special("<position-lifecycle-dependencies>"))
    private val module = CfirSourceModuleData(
        Name.identifier("position-lifecycle"), listOf(library), emptyList(),
        CangJiePlatforms.defaultCangJiePlatform, CfirPlatform.DEFAULT,
    )
    private val intType = ConePrimitiveType.INT64
    private val boolType = ConePrimitiveType.BOOLEAN
    private val unitType = ConePrimitiveType.UNIT
    private val optionType = ConeEnumType(ConeClassLikeLookupTagImpl(StdlibClassIds.Option), listOf(intType))
    private val functionType = ConeFunctionType(emptyList(), unitType)
    // 在 @BeforeEach 建立 application 后才装配会话，SDK/CJD 不得由测试对象构造提前触发。
    override val session: CfirSession by lazy { createSession() }
    private val logic by lazy { object : LogicSystem(session.typeContext) {} }

    @Test
    fun successfulSpeculationRestoresExplicitReceiverPositionImmediately() = checkIsolation {
        assertTrue(analyzer.withIsolatedContext {
            assertNull(analyzer.currentSmartCastPosition)
            emitFunction(speculative)
            assertNotSame(originalBuilder, dfa.graphBuilder)
            true
        })
    }

    @Test
    fun failedSpeculationRestoresExplicitReceiverPositionImmediately() = checkIsolation {
        assertFalse(analyzer.withIsolatedContext {
            assertNull(analyzer.currentSmartCastPosition)
            emitFunction(speculative)
            assertNotSame(originalBuilder, dfa.graphBuilder)
            return@withIsolatedContext false
        })
    }

    @Test
    fun exceptionalSpeculationRestoresExplicitReceiverPositionImmediately() = checkIsolation {
        val failure = IllegalStateException("speculative body failure")
        val caught = assertFailsWith<IllegalStateException> {
            analyzer.withIsolatedContext {
                assertNull(analyzer.currentSmartCastPosition)
                emitFunction(speculative)
                assertNotSame(originalBuilder, dfa.graphBuilder)
                throw failure
            }
        }
        assertSame(failure, caught)
    }

    @Test
    @OptIn(CfgInternals::class)
    fun snapshotRestoresExactReceiverCursorSharesFrozenFlowsAndForksIndependently() {
        val fixture = ReceiverCallFixture()
        val sourceWrite = fixture.sourceWrite
        val copiedWrite = fixture.copiedWrite
        val driver = Driver(fixture.owner, fixture.actors)
        driver.run {
            fixture.enterArguments(this)
            val saved = assertIs<PersistentFlow>(analyzer.currentSmartCastPosition)
            val originalGraph = dfa.graphBuilder.currentGraph
            val originals = originalGraph.orderNodes(isComplete = false)
            val argumentExit = assertIs<FunctionCallArgumentsExitNode>(dfa.graphBuilder.lastNode)
            val argumentsFlow = argumentExit.flow
            val real = known(saved, fixture.value)
            val snapshot = analyzer.captureDataFlowAnalyzerContext(identityMapper)
            val copiedGraph = snapshot.graphMapping.getValue(originalGraph)
            assertNotSame(originalGraph, copiedGraph)
            assertSame(saved, snapshot.smartCastPosition)
            assertSame(copiedGraph, snapshot.context.graphBuilder.currentGraph)
            assertSame(argumentsFlow, snapshot.context.graphBuilder.lastNode.flow)
            assertNotSame(saved, snapshot.context.graphBuilder.lastNode.flow)
            val copiedNodes = copiedGraph.orderNodes(isComplete = false)
            assertEquals(originals.size, copiedNodes.size)
            originals.zip(copiedNodes).forEach { (original, copied) ->
                assertNotSame(original, copied)
                assertSame(original.fir, copied.fir)
                assertEquals(original::class, copied::class)
                assertEquals(original.dataFlowAnalysisState, copied.dataFlowAnalysisState)
                assertEquals(original.flowInitialized, copied.flowInitialized)
                if (original.flowInitialized) assertSame(original.flow, copied.flow)
                assertSame(copiedGraph, copied.owner)
            }
            val copiedExit = assertIs<FunctionCallArgumentsExitNode>(snapshot.context.graphBuilder.lastNode)
            assertNotSame(argumentExit.explicitReceiverExitNode, copiedExit.explicitReceiverExitNode)
            assertSame(saved, copiedExit.explicitReceiverExitNode.flow)
            assertSame(copiedGraph, copiedExit.explicitReceiverExitNode.owner)

            finishCall(fixture.call)
            emit(sourceWrite)
            val sourceBranch = dfa.graphBuilder.lastNode.flow
            assertFalse(logic.isSameValueIn(argumentsFlow, sourceBranch, real))
            assertEquals(setOf(boolType), sourceBranch.getTypeStatement(real)?.upperTypes)
            assertEquals(setOf(boolType), argumentsFlow.getTypeStatement(real)?.upperTypes)

            analyzer.restoreDataFlowAnalyzerContext(snapshot)
            // 恢复后立即读取，不能先添加节点让 mergeIncomingFlow 自动修正游标。
            assertSame(copiedGraph, dfa.graphBuilder.currentGraph)
            assertSame(saved, analyzer.currentSmartCastPosition)
            assertNotSame(dfa.graphBuilder.lastNode.flow, analyzer.currentSmartCastPosition)
            assertSame(real, known(assertNotNull(analyzer.currentSmartCastPosition), fixture.value))
            assertEquals(setOf(intType), saved.getTypeStatement(real)?.upperTypes)
            assertFalse(logic.isSameValueIn(saved, dfa.graphBuilder.lastNode.flow, real))

            finishCall(fixture.call)
            emit(sourceWrite)
            emit(copiedWrite)
            val copiedBranch = dfa.graphBuilder.lastNode.flow
            assertFalse(logic.isSameValueIn(argumentsFlow, copiedBranch, real))
            assertEquals(setOf(intType), copiedBranch.getTypeStatement(real)?.upperTypes)
            assertEquals(setOf(boolType), sourceBranch.getTypeStatement(real)?.upperTypes)
            assertEquals(setOf(boolType), argumentsFlow.getTypeStatement(real)?.upperTypes)
            assertEquals(setOf(intType), saved.getTypeStatement(real)?.upperTypes)
            assertSame(argumentsFlow, sourceBranch.lowestCommonAncestor(copiedBranch))
            assertNotSame(originalGraph.orderNodes(isComplete = false).single { it.fir === sourceWrite },
                copiedGraph.orderNodes(isComplete = false).single { it.fir === sourceWrite })
            assertFalse(originalGraph.orderNodes(isComplete = false).any { it.fir === copiedWrite })
            emit(fixture.owner.body!!.statements.last())
        }
    }

    @Test
    fun passingThrowingClosureToIgnoringFunctionKeepsCallAndOuterSuccessorLive() {
        val actors = Actors()
        val symbol = functionSymbol("passClosure")
        val exceptionType = ConeClassLikeType(ConeClassLikeLookupTagImpl(StdlibClassIds.Exception))
        val exception = parameter("exception", exceptionType, symbol)
        val receiver = variable("receiver", actors.type, rawCall(actors.constructor), false)
        val value = variable("captured", ConeAnyType, integer(0), true)
        val seed = assignment(value, integer(1))
        val innerWrite = assignment(value, boolean(true))
        val throwing = buildThrowExpression {
            source = source("throw-exception"); this.exception = access(exception)
            coneTypeOrNull = ConePrimitiveType.NOTHING
        }
        val body = block(innerWrite, throwing).also { it.replaceConeTypeOrNull(ConePrimitiveType.NOTHING) }
        val anonymous = buildAnonymousFunction {
            source = source("throwing-closure"); moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true; status = CfirDeclarationStatusImpl(Visibilities.Local)
            this.symbol = CfirAnonymousFunctionSymbol(); returnTypeRef = typeRef(unitType)
            typeRef = typeRef(functionType); this.body = body; hasExplicitParameterList = true; isLambda = true
        }
        val expression = buildAnonymousFunctionExpression {
            source = source("closure-value"); anonymousFunction = anonymous; isTrailingLambda = false
        }
        val call = rawCall(actors.ignore, access(receiver), listOf(expression))
        val tail = access(value)
        val owner = function(symbol, block(receiver, value, seed, call, tail, unitLiteral()), listOf(exception))
        val driver = Driver(owner, actors)
        val graph = driver.run { owner.body!!.statements.forEach(::emit) }
        val before = graph.nodes.filterIsInstance<VariableAssignmentNode>().single { it.fir === seed }.flow
        val real = known(before, value)
        val callExit = graph.nodes.filterIsInstance<FunctionCallExitNode>().single { it.fir === call }
        val after = graph.nodes.filterIsInstance<QualifiedAccessNode>().single { it.fir === tail }
        assertFalse(callExit.isDead)
        assertFalse(after.isDead)
        assertFalse(graph.exitNode.isDead)
        assertTrue(logic.isSameValueIn(before, after.flow, real))
        assertEquals(setOf(intType), after.flow.getTypeStatement(real)?.upperTypes)
        assertFalse(graph.nodes.any { it.fir === innerWrite || it.fir === throwing })
        val child = assertIs<CfirControlFlowGraphReferenceImpl>(anonymous.controlFlowGraphReference).controlFlowGraph
        assertSame(anonymous, child.declaration)
        assertTrue(child.nodes.any { it.fir === throwing })
        val writeFlow = child.nodes.filterIsInstance<VariableAssignmentNode>().single { it.fir === innerWrite }.flow
        assertFalse(logic.isSameValueIn(before, writeFlow, real))
        assertEquals(setOf(boolType), writeFlow.getTypeStatement(real)?.upperTypes)
        assertFlowsCompleted(child)
        assertFlowsCompleted(graph)
        assertEquals(DataFlowAnalysisState.CONTROL_FLOW_READY, callExit.dataFlowAnalysisState)
        assertEquals(DataFlowAnalysisState.CONTROL_FLOW_READY, after.dataFlowAnalysisState)
    }

    @Test
    fun unfinishedTupleNativeIteratorSourceCallAndOptionCastKeepSuccessorsControlReady() {
        checkTupleReadiness()
        checkNativeIteratorReadiness()
        checkSourceCallReadiness()
        checkOptionCastReadiness()
    }

    @Test
    fun ordinarySequencePublishesSemanticReadyWithRealValueVersionChange() {
        val value = variable("sequenceValue", ConeAnyType, integer(0), true)
        val seed = assignment(value, integer(1))
        val write = assignment(value, boolean(true))
        val tail = access(value)
        val owner = function(functionSymbol("sequence"), block(value, seed, write, tail, unitLiteral()))
        val graph = Driver(owner).run { owner.body!!.statements.forEach(::emit) }
        val before = graph.nodes.filterIsInstance<VariableAssignmentNode>().single { it.fir === seed }.flow
        val after = graph.nodes.filterIsInstance<QualifiedAccessNode>().single { it.fir === tail }.flow
        val real = known(before, value)
        assertFalse(logic.isSameValueIn(before, after, real))
        assertEquals(setOf(intType), before.getTypeStatement(real)?.upperTypes)
        assertEquals(setOf(boolType), after.getTypeStatement(real)?.upperTypes)
        assertFlowsCompleted(graph)
        graph.nodes.forEach { assertEquals(DataFlowAnalysisState.SEMANTICS_READY, it.dataFlowAnalysisState, "$it") }
    }

    @Test
    fun directLambdaUsesNaturalDeferredBodyBetweenCallEnterAndExit() =
        checkDeferredLambda(LambdaArgumentKind.DIRECT)

    @Test
    fun namedLambdaUsesSameNaturalDeferredBodyAndKeepsWrapperIdentity() =
        checkDeferredLambda(LambdaArgumentKind.NAMED)

    /** N ChkInoutFuncArg 拒绝临时 lambda；这里只验证真实 CFIR 包装模型的调度，不是合法源码正例。 */
    @Test
    fun inoutLambdaModelUsesSameDeferredEventsWithoutClaimingSemaLegality() =
        checkDeferredLambda(LambdaArgumentKind.INOUT_MODEL)

    @Test
    fun namedOptionalChainScalarKeepsOriginalChainEntryAndControlReadySuccessors() {
        val fixture = OptionalScalarFixture()
        val graph = fixture.driver.run { function.body!!.statements.forEach(::emit) }
        val enter = graph.nodes.filterIsInstance<EnterOptionalChainNode>().single { it.fir === fixture.chain }
        val exit = graph.nodes.filterIsInstance<ExitOptionalChainNode>().single { it.fir === fixture.chain }
        val scalarExit = graph.nodes.filterIsInstance<FunctionCallExitNode>().single { it.fir === fixture.scalar }
        val wrapper = graph.nodes.filterIsInstance<WrappedExpressionNode>().single { it.fir === fixture.argument }
        val arguments = graph.nodes.filterIsInstance<FunctionCallArgumentsExitNode>().single { it.fir === fixture.call }
        val callExit = graph.nodes.filterIsInstance<FunctionCallExitNode>().single { it.fir === fixture.call }
        val tail = graph.nodes.filterIsInstance<QualifiedAccessNode>().single { it.fir === fixture.tail }
        assertSame(fixture.chain.expression, assertIs<CfirExportSourcePayload.OptionalChain>(fixture.chain.captureExportSourcePayload()).expression)
        assertSame(fixture.optionRead, assertIs<CfirExportSourcePayload.Optional>(fixture.optional.captureExportSourcePayload()).expression)
        assertSame(fixture.chain, assertIs<CfirExportSourcePayload.NamedArgument>(fixture.argument.captureExportSourcePayload()).expression)
        assertSame(fixture.optional, fixture.scalar.explicitReceiver)
        assertSame(fixture.actors.scalar.symbol, assertIs<CfirResolvedNamedReference>(fixture.scalar.calleeReference).resolvedSymbol)
        assertSame(fixture.actors.acceptNamedOption.symbol, assertIs<CfirResolvedNamedReference>(fixture.call.calleeReference).resolvedSymbol)
        assertSame(fixture.chain.source, enter.fir.source)
        assertSame(fixture.chain.source, exit.fir.source)
        assertSame(fixture.argument.source, wrapper.fir.source)
        assertSame(optionType, fixture.chain.coneTypeOrNull)
        assertSame(optionType, fixture.argument.coneTypeOrNull)
        assertSame(intType, fixture.scalar.coneTypeOrNull)
        assertNotEquals(fixture.scalar.coneTypeOrNull, fixture.chain.coneTypeOrNull)
        assertTrue(scalarExit in exit.previousNodes)
        assertTrue(exit in wrapper.previousNodes)
        assertTrue(wrapper in arguments.previousNodes)
        assertFalse(graph.nodes.any { it is SplitPostponedLambdasNode || it is AnonymousFunctionCaptureNode ||
            it is AnonymousFunctionExpressionNode || it is PostponedLambdaExitNode })
        listOf(enter, scalarExit, exit, wrapper, arguments, callExit, tail, graph.exitNode).forEach {
            assertSame(graph, it.owner)
            assertFalse(it.isDead)
            assertPending(it)
        }
        assertFlowsCompleted(graph)
        fixture.driver.assertOwnedRebuildAbortedAtSemanticTransferGate()
    }

    @Test
    fun namedOptionalChainOfOptionScalarRetainsNestedOptionWithoutFlattening() {
        val fixture = OptionalScalarFixture(nestedOptionResult = true)
        val chainSource = fixture.chain.source
        val graph = fixture.driver.run { function.body!!.statements.forEach(::emit) }
        val enter = graph.nodes.filterIsInstance<EnterOptionalChainNode>().single { it.fir === fixture.chain }
        val exit = graph.nodes.filterIsInstance<ExitOptionalChainNode>().single { it.fir === fixture.chain }
        val wrapper = graph.nodes.filterIsInstance<WrappedExpressionNode>().single { it.fir === fixture.argument }
        assertSame(optionType, fixture.scalar.coneTypeOrNull)
        val outerType = assertIs<ConeEnumType>(fixture.chain.coneTypeOrNull)
        assertSame(optionType, outerType.typeArguments.single())
        assertSame(outerType, fixture.argument.coneTypeOrNull)
        assertSame(outerType, assertIs<CfirResolvedTypeRef>(fixture.target.valueParameters.single().returnTypeRef).coneType)
        assertNotEquals(optionType, outerType)
        assertSame(fixture.chain, fixture.argument.expression)
        assertSame(fixture.scalar, fixture.chain.expression)
        assertSame(fixture.optional, fixture.scalar.explicitReceiver)
        assertSame(chainSource, enter.fir.source)
        assertSame(chainSource, exit.fir.source)
        assertTrue(exit in wrapper.previousNodes)
        assertFalse(graph.nodes.any { it is SplitPostponedLambdasNode || it is AnonymousFunctionCaptureNode })
        listOf(enter, exit, wrapper, graph.exitNode).forEach(::assertPending)
        assertFlowsCompleted(graph)
    }

    @Test
    fun namedLambdaOwnedRebuildAbortsInsteadOfPublishingDriverControlProducts() {
        val fixture = LambdaFixture(LambdaArgumentKind.NAMED)
        fixture.driver.run { function.body!!.statements.forEach(::emit) }
        fixture.driver.assertOwnedRebuildAbortedAtSemanticTransferGate()
    }

    private enum class LambdaArgumentKind { DIRECT, NAMED, INOUT_MODEL }

    /** 观察点只读取实际 analyzer 产品；不安装图、合流或改变 readiness。 */
    private enum class CallStage { ARGUMENTS_EXIT, CALL_ENTER, DEFERRED_BODY_EXIT, CALL_EXIT }

    private inner class LambdaFixture(kind: LambdaArgumentKind) {
        val actors = Actors(withWrapperMembers = true)
        private val symbol = functionSymbol("wrapperClosure${kind.name}")
        private val exception = parameter("exception", ConeClassLikeType(ConeClassLikeLookupTagImpl(StdlibClassIds.Exception)), symbol)
        val receiver = variable("wrapperReceiver", actors.type, rawCall(actors.constructor), false)
        val value = variable("wrapperCaptured", ConeAnyType, integer(0), true)
        val seed = assignment(value, integer(1))
        val innerWrite = assignment(value, boolean(true))
        private val wrapperExceptionValue = access(exception)
        val throwing = buildThrowExpression {
            source = source("wrapper-throw"); this.exception = wrapperExceptionValue; coneTypeOrNull = ConePrimitiveType.NOTHING
        }
        val anonymous = buildAnonymousFunction {
            source = source("wrapper-closure"); moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true; status = CfirDeclarationStatusImpl(Visibilities.Local)
            this.symbol = CfirAnonymousFunctionSymbol(); returnTypeRef = typeRef(unitType)
            typeRef = typeRef(functionType); body = block(innerWrite, throwing).also { it.replaceConeTypeOrNull(ConePrimitiveType.NOTHING) }
            hasExplicitParameterList = true; isLambda = true
        }
        val expression = buildAnonymousFunctionExpression {
            source = source("wrapper-closure-value"); anonymousFunction = anonymous; isTrailingLambda = false
        }
        val argument: CfirExpression = when (kind) {
            LambdaArgumentKind.DIRECT -> expression
            LambdaArgumentKind.NAMED -> namedArgument(expression, functionType)
            LambdaArgumentKind.INOUT_MODEL -> buildInoutArgumentExpression {
                source = source("inout-lambda-model-only"); this.expression = this@LambdaFixture.expression; coneTypeOrNull = functionType
            }
        }
        val target = if (kind == LambdaArgumentKind.NAMED) actors.ignoreNamed else actors.ignore
        val call = rawCall(target, access(receiver), listOf(argument))
        val tail = access(value)
        val owner = function(symbol, block(receiver, value, seed, call, tail, unitLiteral()), listOf(exception))
        val driver = Driver(owner, actors)
    }

    @OptIn(CfgInternals::class)
    private fun checkDeferredLambda(kind: LambdaArgumentKind) {
        val fixture = LambdaFixture(kind)
        val stages = mutableListOf<CallStage>()
        var outer: ControlFlowGraph? = null
        var split: SplitPostponedLambdasNode? = null
        var arguments: FunctionCallArgumentsExitNode? = null
        var entered: FunctionCallEnterNode? = null
        val graph = fixture.driver.run {
            emit(fixture.receiver); emit(fixture.value); emit(fixture.seed)
            emitCall(fixture.call) { stage ->
                stages += stage
                val current = dfa.graphBuilder.currentGraph
                val nodes = current.orderNodes(isComplete = false)
                if (stage == CallStage.ARGUMENTS_EXIT) {
                    outer = current
                    arguments = assertIs<FunctionCallArgumentsExitNode>(dfa.graphBuilder.lastNode)
                    split = nodes.filterIsInstance<SplitPostponedLambdasNode>().single { it.fir === fixture.call }
                    assertSame(fixture.anonymous, assertNotNull(split).lambdas.single())
                    val capture = nodes.filterIsInstance<AnonymousFunctionCaptureNode>().single { it.fir === fixture.expression }
                    assertSame(fixture.expression.source, capture.fir.source)
                    assertTrue(assertNotNull(split) in assertNotNull(arguments).previousNodes)
                    assertSame(assertNotNull(arguments).explicitReceiverExitNode.flow, analyzer.currentSmartCastPosition)
                    assertNull(fixture.anonymous.controlFlowGraphReference)
                    assertFalse(nodes.any { it is FunctionCallEnterNode && it.fir === fixture.call })
                    if (fixture.argument is CfirWrappedExpression) {
                        val wrapper = nodes.filterIsInstance<WrappedExpressionNode>().single { it.fir === fixture.argument }
                        assertSame(fixture.argument.source, wrapper.fir.source)
                        assertTrue(capture in wrapper.previousNodes)
                        assertTrue(wrapper in assertNotNull(split).previousNodes)
                    } else {
                        assertTrue(capture in assertNotNull(split).previousNodes)
                    }
                } else {
                    assertSame(outer, current)
                    if (stage == CallStage.CALL_ENTER) {
                        entered = assertIs<FunctionCallEnterNode>(dfa.graphBuilder.lastNode)
                        assertSame(fixture.call, assertNotNull(entered).fir)
                        assertTrue(assertNotNull(arguments) in assertNotNull(entered).previousNodes)
                        assertNull(fixture.anonymous.controlFlowGraphReference)
                    } else {
                        val child = assertIs<CfirControlFlowGraphReferenceImpl>(fixture.anonymous.controlFlowGraphReference).controlFlowGraph
                        assertSame(fixture.anonymous, child.declaration)
                        assertSame(fixture.anonymous.source, child.enterNode.fir.source)
                        assertTrue(assertNotNull(split) in child.enterNode.previousNodes)
                        assertTrue(child.nodes.any { it.fir === fixture.innerWrite })
                        assertTrue(child.nodes.any { it.fir === fixture.throwing })
                        assertFlowsCompleted(child)
                        if (stage == CallStage.DEFERRED_BODY_EXIT) {
                            assertSame(assertNotNull(entered), dfa.graphBuilder.lastNode)
                            assertFalse(nodes.any { it is FunctionCallExitNode && it.fir === fixture.call })
                        } else {
                            val exit = assertIs<FunctionCallExitNode>(dfa.graphBuilder.lastNode)
                            assertSame(fixture.call, exit.fir)
                            assertPending(exit)
                        }
                    }
                }
            }
            emit(fixture.tail); emit(function.body!!.statements.last())
        }
        assertEquals(CallStage.entries.toList(), stages)
        assertSame(outer, graph)
        assertSame(fixture.target.symbol, assertIs<CfirResolvedNamedReference>(fixture.call.calleeReference).resolvedSymbol)
        assertSame(fixture.actors.type, fixture.target.dispatchReceiverType)
        if (kind == LambdaArgumentKind.NAMED) {
            val parameter = fixture.target.valueParameters.single()
            val named = assertIs<CfirNamedArgumentExpression>(fixture.argument)
            assertTrue(parameter.isNamed)
            assertSame(fixture.target.symbol, parameter.containingDeclarationSymbol)
            assertEquals(parameter.name, named.argumentName)
            assertSame(fixture.expression, named.expression)
        }
        val before = graph.nodes.filterIsInstance<VariableAssignmentNode>().single { it.fir === fixture.seed }.flow
        val real = known(before, fixture.value)
        val after = graph.nodes.filterIsInstance<QualifiedAccessNode>().single { it.fir === fixture.tail }
        assertTrue(logic.isSameValueIn(before, after.flow, real))
        assertEquals(setOf(intType), after.flow.getTypeStatement(real)?.upperTypes)
        assertFalse(graph.nodes.any { it.fir === fixture.innerWrite || it.fir === fixture.throwing })
        val child = assertIs<CfirControlFlowGraphReferenceImpl>(fixture.anonymous.controlFlowGraphReference).controlFlowGraph
        val write = child.nodes.filterIsInstance<VariableAssignmentNode>().single { it.fir === fixture.innerWrite }
        assertSame(fixture.innerWrite.source, write.fir.source)
        assertFalse(logic.isSameValueIn(before, write.flow, real))
        assertEquals(setOf(boolType), write.flow.getTypeStatement(real)?.upperTypes)
        listOf(after, graph.exitNode).forEach { assertFalse(it.isDead); assertPending(it) }
        assertFalse(graph.nodes.filterIsInstance<FunctionCallExitNode>().single { it.fir === fixture.call }.isDead)
        assertFlowsCompleted(graph)
    }

    /** 对应 maybeCursor?.scalar() 的实际 typed 结构；Option 结果和 scalar 值的类型分别保留。 */
    private inner class OptionalScalarFixture(nestedOptionResult: Boolean = false) {
        val actors = Actors(withWrapperMembers = true)
        private val result = if (nestedOptionResult) actors.nestedOptionType else optionType
        private val cursorOption = ConeEnumType(ConeClassLikeLookupTagImpl(StdlibClassIds.Option), listOf(actors.type))
        private val receiver = variable("optionalScalarReceiver", actors.type, rawCall(actors.constructor), false)
        private val maybe = variable("maybeCursor", cursorOption, rawCall(actors.some, values = listOf(access(receiver)),
            resultType = cursorOption, typeValues = listOf(actors.type)), false)
        val optionRead = access(maybe)
        val optional = buildOptionalExpression {
            source = source("maybeCursor?"); expression = optionRead; coneTypeOrNull = actors.type
        }
        val scalar = rawCall(if (nestedOptionResult) actors.scalarOption else actors.scalar, optional)
        val chain = buildOptionalChainExpression {
            source = source("maybeCursor?.${assertIs<CfirResolvedNamedReference>(scalar.calleeReference).name}()")
            expression = scalar; coneTypeOrNull = result
        }
        val argument = namedArgument(chain, result)
        val target = if (nestedOptionResult) actors.acceptNamedNestedOption else actors.acceptNamedOption
        val call = rawCall(target, access(receiver), listOf(argument))
        val tail = access(receiver)
        private val owner = function(functionSymbol("namedOptionalScalar"), block(receiver, maybe, call, tail, unitLiteral()))
        val driver = Driver(owner, actors)
    }

    private fun namedArgument(value: CfirExpression, type: ConeCangJieType): CfirNamedArgumentExpression = buildNamedArgumentExpression {
        source = source("argument:wrapped-value"); expression = value; argumentName = Name.identifier("argument")
        nameSource = source("argument:"); coneTypeOrNull = type
    }

    /** receiver 在本次实例上解析；实参块写另一个外层 var，产生两个不同版本的查询位置。 */
    private inner class ReceiverCallFixture {
        val actors = Actors()
        val receiver = variable("receiver", actors.type, rawCall(actors.constructor), false)
        val value = variable("argumentWrittenValue", ConeAnyType, integer(0), true)
        val seed = assignment(value, integer(1))
        val write = assignment(value, boolean(true))
        val argument = block(write, integer(7)).also { it.replaceConeTypeOrNull(intType) }
        val receiverRead = access(receiver)
        val call = rawCall(actors.accept, receiverRead, listOf(argument))
        val sourceWrite = assignment(value, boolean(false))
        val copiedWrite = assignment(value, integer(3))
        val owner = function(functionSymbol("receiverPosition"), block(receiver, value, seed, call, sourceWrite, copiedWrite, unitLiteral()))

        fun enterArguments(driver: Driver) = with(driver) {
            emit(receiver); emit(value); emit(seed)
            analyzer.enterCallArguments(call, call.argumentList.arguments)
            emit(receiverRead)
            val receiverExit = dfa.graphBuilder.lastNode
            analyzer.exitCallExplicitReceiver()
            emit(argument)
            analyzer.exitCallArguments()
            val argumentExit = assertIs<FunctionCallArgumentsExitNode>(dfa.graphBuilder.lastNode)
            assertSame(receiverExit, argumentExit.explicitReceiverExitNode)
            assertSame(receiverExit.flow, analyzer.currentSmartCastPosition)
            assertNotSame(argumentExit.flow, analyzer.currentSmartCastPosition)
            val real = known(receiverExit.flow, value)
            assertEquals(setOf(intType), receiverExit.flow.getTypeStatement(real)?.upperTypes)
            assertEquals(setOf(boolType), argumentExit.flow.getTypeStatement(real)?.upperTypes)
            assertFalse(logic.isSameValueIn(receiverExit.flow, argumentExit.flow, real))
            assertSame(receiver.symbol, known(receiverExit.flow, receiver).symbol)
            assertSame(actors.accept.symbol, assertIs<CfirResolvedNamedReference>(call.calleeReference).resolvedSymbol)
            assertEquals(actors.klass.symbol.classId, actors.accept.symbol.callableId.classId)
            assertSame(actors.type, actors.accept.dispatchReceiverType)
            assertSame(receiverRead, call.dispatchReceiver)
            assertNotSame(receiver.symbol, value.symbol)
        }
    }

    private inner class IsolationScope(val driver: Driver, val speculative: CfirNamedFunction) {
        val analyzer get() = driver.analyzer
        val dfa get() = driver.dfa
        val originalBuilder = dfa.graphBuilder
        fun emitFunction(function: CfirFunction) = driver.emitFunction(function)
    }

    private fun checkIsolation(speculate: IsolationScope.() -> Unit) {
        val fixture = ReceiverCallFixture()
        val temporary = variable("speculativeValue", ConeAnyType, integer(0), true)
        val speculative = function(functionSymbol("speculative"), block(temporary, assignment(temporary, boolean(true)), unitLiteral()))
        val driver = Driver(fixture.owner, fixture.actors, listOf(speculative))
        driver.run {
            fixture.enterArguments(this)
            val saved = assertIs<PersistentFlow>(analyzer.currentSmartCastPosition)
            val builder = dfa.graphBuilder
            val graph = builder.currentGraph
            val last = builder.lastNode
            val counter = dfa.assignmentCounter
            val real = known(saved, fixture.value)
            IsolationScope(this, speculative).speculate()
            // 三种退出都在这里立即查 Flow；没有中间节点、手动 reset 或字段修改。
            assertSame(builder, dfa.graphBuilder)
            assertSame(graph, dfa.graphBuilder.currentGraph)
            assertSame(last, dfa.graphBuilder.lastNode)
            assertEquals(counter, dfa.assignmentCounter)
            assertSame(saved, analyzer.currentSmartCastPosition)
            assertNotSame(last.flow, analyzer.currentSmartCastPosition)
            val restored = assertNotNull(analyzer.currentSmartCastPosition)
            assertSame(real, known(restored, fixture.value))
            assertEquals(setOf(intType), restored.getTypeStatement(real)?.upperTypes)
            assertEquals(setOf(boolType), last.flow.getTypeStatement(real)?.upperTypes)
            assertFalse(logic.isSameValueIn(restored, last.flow, real))
            assertNull(restored.getVariableIfKnown(access(temporary)))
            finishCall(fixture.call)
            emit(fixture.sourceWrite); emit(fixture.copiedWrite); emit(fixture.owner.body!!.statements.last())
        }
    }

    private fun checkTupleReadiness() {
        val left = variable("left", intType, integer(0), true)
        val right = variable("right", intType, integer(1), true)
        fun tuple(vararg values: CfirExpression) = buildTupleLiteral {
            source = source("tuple"); elements += values; coneTypeOrNull = ConeTupleType(listOf(intType, intType))
        }
        val write = buildAssignment {
            source = source("tuple-write"); lValue = tuple(access(left), access(right))
            rValue = tuple(integer(2), integer(3)); operation = CfirSourceOperator.ASSIGN
            isCompound = false; coneTypeOrNull = unitType
        }
        val tail = integer(4)
        val owner = function(functionSymbol("tupleReadiness"), block(left, right, write, tail).also {
            it.replaceConeTypeOrNull(intType)
        }, result = intType)
        val graph = Driver(owner).run {
            emit(left); emit(right); emit(write)
            assertPending(dfa.graphBuilder.lastNode)
            emit(tail)
            assertPending(dfa.graphBuilder.lastNode)
        }
        assertFlowsCompleted(graph)
        assertPending(graph.exitNode)
    }

    private fun checkNativeIteratorReadiness() {
        val actors = Actors()
        val iterator = variable("cursor", actors.type, rawCall(actors.constructor), false)
        val next = rawCall(actors.next, access(iterator))
        val selector = buildNativeMatchExpression {
            source = source("stored-next-selector"); this.selector = next; coneTypeOrNull = optionType
        }
        val binding = variable("element", intType, null, false)
        val reference = buildResolvedNamedReference {
            source = actors.some.source; name = actors.some.name; resolvedSymbol = actors.some.symbol
        }
        val pattern = buildEnumPattern {
            source = source("stored-Some-pattern"); semanticTypeRef = typeRef(optionType)
            constructor = buildNamedAccessExpression {
                source = actors.some.source; calleeReference = reference
                coneTypeOrNull = ConeFunctionType(listOf(intType), optionType)
            }
            arguments += buildBindingPattern {
                source = binding.source; name = binding.name; bindingVariable = binding
                typeRef = binding.returnTypeRef; semanticTypeRef = binding.returnTypeRef
            }
        }
        val bodyValue = unitLiteral()
        val loop = buildForInExpression {
            source = source("native-iterator"); iterationKind = CfirSourceForInKind.ITERATOR
            iterable = block(iterator, selector); this.pattern = pattern; body = block(bodyValue); coneTypeOrNull = unitType
        }
        val tail = integer(9)
        val owner = function(functionSymbol("iteratorReadiness"), block(loop, tail).also {
            it.replaceConeTypeOrNull(intType)
        }, result = intType)
        val graph = Driver(owner, actors).run {
            analyzer.enterForInLoop(loop)
            emit(iterator)
            analyzer.enterForInLoopIteration(loop)
            emit(next)
            analyzer.exitForInLoopIteratorCheck(loop, next, reference, actors.some.symbol)
            val check = assertIs<ForInLoopIteratorCheckNode>(dfa.graphBuilder.lastNode)
            assertSame(next, check.resultExpression)
            assertSame(reference, check.constructorReference)
            assertSame(actors.some.symbol, check.constructorSymbol)
            assertPending(check)
            analyzer.exitForInLoopIteration(loop)
            emit(binding)
            analyzer.exitForInLoopGuard(loop)
            emit(loop.body)
            assertPending(dfa.graphBuilder.lastNode)
            analyzer.exitForInLoopBody(loop)
            analyzer.exitForInLoop(loop)
            emit(tail)
            assertPending(dfa.graphBuilder.lastNode)
        }
        assertSame(next, selector.selector)
        assertNull(binding.initializer)
        assertSame(actors.option, actors.coreFile.declarations.single())
        assertTrue(actors.option.declarations.any { it === actors.some })
        assertSame(actors.option.symbol, actors.optionParameter.containingDeclarationSymbol)
        assertSame(actors.some.symbol, actors.some.valueParameters.single().containingDeclarationSymbol)
        assertEquals(1, graph.nodes.count { it is FunctionCallExitNode && it.fir === next })
        assertFlowsCompleted(graph)
        assertPending(graph.exitNode)
    }

    private fun checkSourceCallReadiness() {
        val actors = Actors()
        val receiver = variable("sourceReceiver", actors.type, rawCall(actors.constructor), false)
        val call = buildCallExpression {
            source = source("source-member-call"); coneTypeOrNull = unitType
            base = buildNamedAccessExpression {
                source = actors.accept.source; explicitReceiver = access(receiver)
                dispatchReceiver = explicitReceiver
                calleeReference = buildResolvedNamedReference { name = actors.accept.name; resolvedSymbol = actors.accept.symbol }
                coneTypeOrNull = ConeFunctionType(listOf(intType), unitType)
            }
            arguments += buildFunctionArgument {
                source = source("source-argument"); value = integer(7); coneTypeOrNull = intType
                isDefaultValue = false; withInout = false
            }
            callKind = CfirSourceCallKind.DECLARED; hasSideEffect = true
        }
        val tail = integer(8)
        val owner = function(functionSymbol("sourceCallReadiness"), block(receiver, call, tail).also {
            it.replaceConeTypeOrNull(intType)
        }, result = intType)
        val graph = Driver(owner, actors).run {
            emit(receiver); emit(call)
            assertPending(assertIs<CallExpressionNode>(dfa.graphBuilder.lastNode))
            emit(tail)
            assertPending(dfa.graphBuilder.lastNode)
        }
        assertFlowsCompleted(graph)
        assertPending(graph.exitNode)
    }

    private fun checkOptionCastReadiness() {
        val value = variable("castValue", ConeAnyType, integer(0), true)
        val seed = assignment(value, integer(1))
        // N SynAsExpr：结果是 Option<Int64>，不能给原 operand 写入 Kotlin AS 的成功收窄。
        val cast = buildTypeOperator {
            source = source("option-as"); operation = CfirTypeOperationKind.AS
            argument = access(value); typeRef = typeRef(intType); coneTypeOrNull = optionType
        }
        val tail = integer(2)
        val owner = function(functionSymbol("optionCastReadiness"), block(value, seed, cast, tail).also {
            it.replaceConeTypeOrNull(intType)
        }, result = intType)
        val graph = Driver(owner).run {
            emit(value); emit(seed); emit(cast)
            assertPending(dfa.graphBuilder.lastNode)
            emit(tail)
            assertPending(dfa.graphBuilder.lastNode)
        }
        assertSame(optionType, cast.coneTypeOrNull)
        assertFlowsCompleted(graph)
        assertPending(graph.exitNode)
    }

    /** 真实 actor：成员声明具有实际 class owner/dispatch 类型，receiver 由该类构造调用产生。 */
    private inner class Actors(private val withWrapperMembers: Boolean = false) {
        private val classId = ClassId(FqName("positionlifecycle"), Name.identifier("Cursor"))
        private val classSymbol = CfirClassSymbol(classId)
        val type = ConeClassLikeType(ConeClassLikeLookupTagWithFixedSymbol(classId, classSymbol))
        val constructor = buildConstructor {
            source = source("Cursor-init"); moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false; status = CfirDeclarationStatusImpl(Visibilities.Public)
            symbol = CfirConstructorSymbol(CallableId(classId, Name.special("<init>")))
            returnTypeRef = typeRef(type); body = block(unitLiteral())
        }
        val accept = member("accept", intType, unitType, block(unitLiteral()))
        val ignore = member("ignore", functionType, unitType, block(unitLiteral()))
        val ignoreNamed by lazy { namedMember("ignoreNamed", functionType) }
        val acceptNamedOption by lazy { namedMember("acceptNamedOption", optionType) }
        val scalar by lazy { member("scalar", null, intType, block(integer(7)).also { it.replaceConeTypeOrNull(intType) }) }
        val nestedOptionType = ConeEnumType(ConeClassLikeLookupTagImpl(StdlibClassIds.Option), listOf(optionType))
        val acceptNamedNestedOption by lazy { namedMember("acceptNamedNestedOption", nestedOptionType) }
        val scalarOption by lazy { member("scalarOption", null, optionType, block(rawCall(some, values = listOf(integer(7)),
            resultType = optionType, typeValues = listOf(intType))).also { it.replaceConeTypeOrNull(optionType) }) }
        private val optionSymbol = CfirEnumSymbol(StdlibClassIds.Option)
        val optionParameter = buildTypeParameter {
            source = source("Option-T"); moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY
            name = Name.identifier("T"); symbol = CfirTypeParameterSymbol(); containingDeclarationSymbol = optionSymbol
            bounds += typeRef(ConeAnyType)
        }
        private val payloadType = ConeTypeParameterTypeImpl(optionParameter.symbol.toLookupTag())
        private val genericOptionType = ConeEnumType(
            ConeClassLikeLookupTagWithFixedSymbol(StdlibClassIds.Option, optionSymbol), listOf(payloadType),
        )
        val some = buildEnumConstructor {
            source = source("std.core.Option.Some"); moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false; status = CfirDeclarationStatusImpl(Visibilities.Public)
            name = Name.identifier("Some"); symbol = CfirEnumConstructorSymbol(CallableId(StdlibClassIds.Option, name))
            typeParameters += constructedClassTypeParameterRefs(listOf(optionParameter.symbol))
            returnTypeRef = typeRef(genericOptionType); valueParameters += parameter("payload", payloadType, symbol)
        }
        private val none = buildEnumConstructor {
            source = source("std.core.Option.None"); moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false; status = CfirDeclarationStatusImpl(Visibilities.Public)
            name = Name.identifier("None"); symbol = CfirEnumConstructorSymbol(CallableId(StdlibClassIds.Option, name))
            typeParameters += constructedClassTypeParameterRefs(listOf(optionParameter.symbol))
            returnTypeRef = typeRef(genericOptionType)
        }
        val option = buildEnum {
            source = source("Option"); moduleData = module; resolvePhase = CfirResolvePhase.IMPORTS
            origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY
            status = CfirDeclarationStatusImpl(Visibilities.Public); scopeProvider = CfirCangJieScopeProvider()
            name = Name.identifier("Option"); symbol = optionSymbol; typeParameters += optionParameter
            declarations += listOf(some, none); isRefEnum = false; isNonExhaustive = false
        }
        val coreFile = buildFile {
            source = source("position-option.cj"); moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY
            name = "position-option.cj"; symbol = CfirFileSymbol()
            packageDirective = buildPackageDirective { packageFqName = StdlibClassIds.Option.packageFqName; isMacroPackage = false }
            declarations += option
        }
        val next = member("next", null, optionType, block(rawCall(some, values = listOf(integer(1)),
            resultType = optionType, typeValues = listOf(intType))).also {
            it.replaceConeTypeOrNull(optionType)
        }).also { (it.status as CfirDeclarationStatusImpl).isOverride = true }
        val klass = buildClass {
            source = source("Cursor"); moduleData = module; resolvePhase = CfirResolvePhase.IMPORTS
            origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY
            status = CfirDeclarationStatusImpl(Visibilities.Public); scopeProvider = CfirCangJieScopeProvider()
            name = classId.shortClassName; symbol = classSymbol
            superTypeRefs += typeRef(ConeClassLikeType(ConeClassLikeLookupTagImpl(
                ClassId(StdlibClassIds.Option.packageFqName, Name.identifier("Iterator")),
            ), listOf(intType)))
            declarations += listOf(constructor, accept, ignore, next)
            if (withWrapperMembers) declarations += listOf(ignoreNamed, acceptNamedOption, scalar, acceptNamedNestedOption, scalarOption)
        }

        /** 新正例的命名实参有真实 isNamed 形参和 class owner；不改变旧 actor 的参数合同。 */
        private fun namedMember(name: String, argument: ConeCangJieType): CfirNamedFunction {
            val symbol = CfirNamedFunctionSymbol(CallableId(classId, Name.identifier(name)))
            val named = buildValueParameter {
                source = source("argument!"); moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY
                isLocal = true; isNamed = true; this.name = Name.identifier("argument")
                this.symbol = CfirValueParameterSymbol(CallableId(this.name))
                containingDeclarationSymbol = symbol; returnTypeRef = typeRef(argument)
            }
            return function(symbol, block(unitLiteral()), listOf(named), receiverType = type)
        }

        private fun member(name: String, argument: ConeCangJieType?, result: ConeCangJieType, body: CfirBlock): CfirNamedFunction {
            val symbol = CfirNamedFunctionSymbol(CallableId(classId, Name.identifier(name)))
            return function(symbol, body, if (argument == null) emptyList() else listOf(parameter("argument", argument, symbol)),
                result, receiverType = type)
        }
    }

    /** 只发真实 typed 语句的公开事件；不设置 flow/state，不访问私有变量表。 */
    private inner class Driver(
        val function: CfirNamedFunction,
        private val actors: Actors? = null,
        additional: List<CfirDeclaration> = emptyList(),
    ) {
        val dfa = CfirDataFlowAnalyzerContext()
        private val context = BodyResolveContext(ReturnTypeCalculatorForFullBodyResolve.Default, dfa)
        val analyzer = CfirDataFlowAnalyzer(this@CfirDataFlowPositionLifecycleTest, context)
        private val file = buildFile {
            source = source("position-lifecycle.cj"); moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY
            name = "position-lifecycle.cj"; symbol = CfirFileSymbol()
            packageDirective = buildPackageDirective { packageFqName = FqName("positionlifecycle"); isMacroPackage = false }
            actors?.let { declarations += it.klass }
            declarations += function; declarations += additional
        }
        private val sourceFiles = listOfNotNull(actors?.coreFile, file)

        init {
            val scopeSession = ScopeSession()
            if (actors != null) {
                val iteratorId = ClassId(StdlibClassIds.Option.packageFqName, Name.identifier("Iterator"))
                val iterator = assertIs<CfirClassSymbol>(session.symbolProvider.getClassLikeSymbolByClassId(iteratorId))
                assertTrue(iterator.isBound)
                val originalParentType = assertIs<ConeClassLikeType>(actors.klass.superTypeRefs.single().coneTypeOrNull)
                assertFalse(originalParentType.isInterface)
                assertSame(iterator, originalParentType.toSymbol(session))
                assertSame(intType, originalParentType.typeArguments.single())
            }
            sourceFiles.forEach { completeCjoSourceFixtureClassLikeHeaders(session, it.declarations, scopeSession, it) }
            if (actors != null) {
                // SDK的Iterator<T>是abstract class；Cursor保留该真实直接父类，Object不另造直接边。
                val provider = assertNotNull(session.directSupertypeProviderOrNull)
                val cursorProduct = assertNotNull(provider.getCompletedSupertypeProduct(actors.klass.symbol))
                val optionProduct = assertNotNull(provider.getCompletedSupertypeProduct(actors.option.symbol))
                assertSame(actors.klass.symbol, cursorProduct.ownerSymbol)
                assertSame(actors.option.symbol, optionProduct.ownerSymbol)
                assertSame(actors.klass, cursorProduct.ownerSymbol.cfir)
                assertSame(actors.option, optionProduct.ownerSymbol.cfir)
                assertSame(session, actors.klass.moduleData.session)
                assertTrue(actors.klass.origin.fromSource && actors.option.origin.fromSource)
                assertFalse(cursorProduct.isInReferenceCycle)
                assertFalse(optionProduct.isInReferenceCycle)
                val iteratorId = ClassId(StdlibClassIds.Option.packageFqName, Name.identifier("Iterator"))
                val iterator = assertIs<CfirClassSymbol>(session.symbolProvider.getClassLikeSymbolByClassId(iteratorId))
                val iteratorEdge = cursorProduct.superTypes.single { it.resolvedClassSymbol === iterator }
                val iteratorType = assertIs<ConeClassLikeType>(iteratorEdge.typeRef.coneTypeOrNull)
                assertFalse(iteratorType.isInterface)
                assertSame(intType, iteratorType.typeArguments.single())
            }
            // 所有状态准备都发生在第一次公开 DFA 事件之前；不在重建失败断言中补 header。
            val statusResolver = CfirStatusResolver(session, scopeSession)
            var containingClass: CfirClassLikeDeclaration? = null
            val statusVisitor = object : org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid() {
                override fun visitElement(element: CfirElement) {
                    if (element is CfirMemberDeclaration) {
                        element.replaceStatus(statusResolver.resolveStatus(element, containingClass, null,
                            element is CfirCallableDeclaration && element.isLocal))
                    }
                    val previous = containingClass
                    if (element is CfirClassLikeDeclaration) containingClass = element
                    try { element.acceptChildren(this) } finally { containingClass = previous }
                }
            }
            sourceFiles.forEach { it.accept(statusVisitor) }
        }

        fun run(events: Driver.() -> Unit): ControlFlowGraph = context.withFile(file) {
            analyzer.enterFile(file, buildGraph = true)
            context.withContainer(function) {
                analyzer.enterFunction(function)
                function.valueParameters.forEach { parameter ->
                    assertSame(function.symbol, parameter.containingDeclarationSymbol)
                    analyzer.enterValueParameter(parameter); analyzer.exitValueParameter(parameter)
                }
                val body = assertNotNull(function.body)
                analyzer.enterBlock(body)
                events()
                analyzer.exitBlock(body)
                function.replaceControlFlowGraphReference(assertNotNull(analyzer.exitFunction(function)))
            }
            val fileGraph = assertNotNull(analyzer.exitFile())
            assertFlowsCompleted(fileGraph)
            assertNull(analyzer.currentSmartCastPosition)
            assertIs<CfirControlFlowGraphReferenceImpl>(function.controlFlowGraphReference).controlFlowGraph
        }

        fun emitFunction(owner: CfirFunction) = context.withContainer(owner) {
            analyzer.enterFunction(owner)
            owner.valueParameters.forEach { parameter ->
                assertSame(owner.symbol, parameter.containingDeclarationSymbol)
                analyzer.enterValueParameter(parameter); analyzer.exitValueParameter(parameter)
            }
            emit(assertNotNull(owner.body))
            owner.replaceControlFlowGraphReference(assertNotNull(analyzer.exitFunction(owner)))
        }

        fun finishCall(call: CfirFunctionCall) {
            analyzer.enterFunctionCall(call)
            analyzer.exitFunctionCall(call, callCompleted = true)
        }

        /** 原公开调用事件序列；可选观察器仅用于检查各阶段已经产生的节点/正文图身份。 */
        fun emitCall(call: CfirFunctionCall, observe: (CallStage) -> Unit = {}) {
            val deferred = mutableListOf<CfirAnonymousFunction>()
            analyzer.enterCallArguments(call, call.argumentList.arguments)
            call.explicitReceiver?.let(::emit); analyzer.exitCallExplicitReceiver()
            call.argumentList.arguments.forEach { emitCallArgument(it, deferred) }
            analyzer.exitCallArguments()
            observe(CallStage.ARGUMENTS_EXIT)
            analyzer.enterFunctionCall(call)
            observe(CallStage.CALL_ENTER)
            deferred.forEach(::emitFunction)
            observe(CallStage.DEFERRED_BODY_EXIT)
            analyzer.exitFunctionCall(call, callCompleted = true)
            observe(CallStage.CALL_EXIT)
        }

        /** 正式复制/重建必须命中实际未闭 transfer 门；任意更早异常不能被记作通过。 */
        fun assertOwnedRebuildAbortedAtSemanticTransferGate() {
            val originalGraph = function.controlFlowGraphReference
            val attributes = object : CfirOwnedAttributeRemapper {
                override fun collectOwnedChildren(key: String, value: Any, visit: (CfirElement) -> Unit) {
                    error("Wrapper fixture has no extension attribute: $key")
                }
                override fun copyAttribute(key: String, value: Any, context: CfirOwnedGraphCopyContext): Any =
                    error("Wrapper fixture has no extension attribute: $key")
            }
            val copy = deepCopyOwnedGraphs(listOf(function), CfirTypeRefRemapper { type, _ -> type }, attributes)
            val fresh = copy.getCopied(function)
            assertNotSame(function, fresh)
            assertSame(function.source, fresh.source)
            assertNull(fresh.controlFlowGraphReference)
            assertTrue(copy.resolvedDeclarationsRequiringAnalysisRebuild.isNotEmpty())
            copy.resolvedDeclarationsRequiringAnalysisRebuild.forEach {
                assertEquals(CfirOwnedGraphAnalysisState.PENDING, it.ownedGraphAnalysisState)
            }
            val failure = assertFailsWith<IllegalStateException> {
                CfirResolvedGraphAnalysisRebuilder.rebuild(copy, CfirResolvedGraphOwnerContext(sourceFiles))
            }
            assertEquals("Full owned analysis requires completed semantic transfer and DFA flow for every actual node", failure.message)
            copy.resolvedDeclarationsRequiringAnalysisRebuild.forEach {
                assertEquals(CfirOwnedGraphAnalysisState.ABORTED, it.ownedGraphAnalysisState)
                assertNull(it.ownedGraphAnalysisReadiness)
            }
            assertNull(fresh.controlFlowGraphReference)
            assertSame(originalGraph, function.controlFlowGraphReference)
            assertFailsWith<IllegalStateException> { copy.requireAnalysisReady() }
        }

        private fun emitCallArgument(argument: CfirExpression, deferred: MutableList<CfirAnonymousFunction>) {
            when (argument) {
                is CfirAnonymousFunctionExpression -> {
                    analyzer.enterAnonymousFunctionExpression(argument)
                    deferred += argument.anonymousFunction
                }
                is CfirNamedArgumentExpression, is CfirInoutArgumentExpression -> {
                    check(argument is CfirWrappedExpression)
                    emitCallArgument(argument.expression, deferred)
                    analyzer.exitWrappedExpression(argument)
                }
                else -> emit(argument)
            }
        }

        fun emit(statement: CfirStatement) {
            when (statement) {
                is CfirBlock -> {
                    analyzer.enterBlock(statement); statement.statements.forEach(::emit); analyzer.exitBlock(statement)
                }
                is CfirVariable -> {
                    analyzer.enterVariableDeclaration(statement); statement.initializer?.let(::emit)
                    analyzer.exitVariableDeclaration(statement)
                }
                is CfirLiteralExpression -> analyzer.exitLiteralExpression(statement)
                is CfirNamedAccessExpression -> {
                    statement.explicitReceiver?.let(::emit); analyzer.exitQualifiedAccessExpression(statement)
                }
                is CfirAssignment -> {
                    emit(statement.lValue); emit(statement.rValue); analyzer.recordAssignment(statement)
                    analyzer.exitVariableAssignment(statement)
                }
                is CfirTupleLiteral -> statement.elements.forEach(::emit)
                is CfirFunctionCall -> emitCall(statement)
                is CfirOptionalChainExpression -> {
                    analyzer.enterOptionalChain(statement); emit(statement.expression); analyzer.exitOptionalChain(statement)
                }
                is CfirOptionalExpression -> emit(statement.expression)
                is CfirNamedArgumentExpression, is CfirInoutArgumentExpression -> {
                    check(statement is CfirWrappedExpression)
                    emit(statement.expression); analyzer.exitWrappedExpression(statement)
                }
                is CfirCallExpression -> {
                    emit(statement.base)
                    statement.arguments.forEach { emit(assertNotNull(it.value)) }
                    analyzer.exitCallExpression(statement)
                }
                is CfirAnonymousFunctionExpression -> {
                    analyzer.enterAnonymousFunctionExpression(statement)
                    val saved = analyzer.currentSmartCastPosition
                    emitFunction(statement.anonymousFunction)
                    assertSame(saved, analyzer.currentSmartCastPosition)
                }
                is CfirThrowExpression -> { emit(statement.exception); analyzer.exitThrowException(statement) }
                is CfirTypeOperator -> { emit(statement.argument); analyzer.exitTypeOperatorCall(statement) }
                else -> error("No public position lifecycle event for ${statement::class}")
            }
        }
    }

    private val identityMapper = object : SnapshotCfirMapper {
        override fun <T : CfirElement> mapElement(element: T): T = element
        override fun <T : CfirBasedSymbol<*>> mapSymbol(symbol: T): T = symbol
    }

    private fun known(flow: Flow, variable: CfirVariable): RealVariable =
        assertIs<RealVariable>(flow.getVariableIfKnown(access(variable)))

    private fun assertPending(node: CFGNode<*>) {
        assertTrue(node.flowInitialized, "Missing control product: $node")
        assertIs<PersistentFlow>(node.flow)
        assertEquals(DataFlowAnalysisState.CONTROL_FLOW_READY, node.dataFlowAnalysisState, "$node")
    }

    private fun assertFlowsCompleted(graph: ControlFlowGraph) {
        assertTrue(graph.nodes.isNotEmpty())
        graph.nodes.forEach { node ->
            assertTrue(node.flowInitialized, "Missing analyzer flow: $node")
            assertIs<PersistentFlow>(node.flow)
            node.alternateFlowPaths.forEach { assertNotNull(node.getAlternateFlow(it)) }
        }
    }

    private fun createSession(): CfirSession {
        val configuration = CompilerConfiguration.createForCfirFrontend()
        val factory = CfirDefaultSessionFactory()
        val sdkRoot = java.io.File(assertNotNull(javaClass.classLoader.getResource("cjo-sdk/windows_x86_64_cjnative")).toURI())
        check(sdkRoot.isDirectory) { "The real CJO SDK fixture must be available as a classpath directory" }
        // 通过正常 CJO 依赖装配取得 Iterator/Object，原 Cursor/Option 仍属于这个 Source session。
        val context = CfirDefaultSessionFactory.Context(
            cjoManager = CjoManager(CjoSearchPath(envProvider = { name ->
                if (name == "CANGJIE_STDLIB_MODULE") sdkRoot.absolutePath else null
            }), CjoFormatVersionPolicy.Compilation(configuration.languageVersionSettings)),
        )
        val shared = factory.createSharedLibrarySession(module.name, emptyList(), configuration.languageVersionSettings, context)
        factory.createLibrarySession(shared, SingleModuleDataProvider(library), emptyList(), configuration.languageVersionSettings, context)
        return factory.createSourceSession(module, emptyList(), configuration, context)
    }

    private fun functionSymbol(name: String) = CfirNamedFunctionSymbol(CallableId(FqName("positionlifecycle"), Name.identifier(name)))

    private fun function(symbol: CfirNamedFunctionSymbol, body: CfirBlock, parameters: List<CfirValueParameter> = emptyList(),
        result: ConeCangJieType = unitType, receiverType: ConeSimpleCangJieType? = null): CfirNamedFunction = buildNamedFunction {
        source = source(symbol.name.asString()); moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY
        isLocal = false; status = CfirDeclarationStatusImpl(Visibilities.Public)
        name = symbol.name; this.symbol = symbol; returnTypeRef = typeRef(result); this.body = body
        valueParameters += parameters; isMut = false; dispatchReceiverType = receiverType
    }

    private fun parameter(name: String, type: ConeCangJieType, owner: CfirBasedSymbol<*>): CfirValueParameter = buildValueParameter {
        source = source(name); moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY
        isLocal = true; isNamed = false; this.name = Name.identifier(name); symbol = CfirValueParameterSymbol(CallableId(this.name))
        containingDeclarationSymbol = owner; returnTypeRef = typeRef(type)
    }

    private fun variable(name: String, type: ConeCangJieType, initializer: CfirExpression?, mutable: Boolean): CfirPatternBindingVariable =
        buildPatternBindingVariable {
            source = source(name); moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true; status = CfirDeclarationStatusImpl(Visibilities.Local)
            this.name = Name.identifier(name); symbol = CfirPatternBindingSymbol(CallableId(this.name))
            returnTypeRef = typeRef(type); this.initializer = initializer; isVar = mutable; isTypeImplicit = false
        }

    private fun access(variable: CfirVariable): CfirNamedAccessExpression = buildNamedAccessExpression {
        source = source("read-${variable.symbol.name}"); coneTypeOrNull = assertIs<CfirResolvedTypeRef>(variable.returnTypeRef).coneType
        calleeReference = buildResolvedNamedReference { source = variable.source; name = variable.symbol.name; resolvedSymbol = variable.symbol }
    }

    private fun rawCall(function: CfirCallableDeclaration, receiver: CfirExpression? = null, values: List<CfirExpression> = emptyList(),
        resultType: ConeCangJieType = assertIs<CfirResolvedTypeRef>(function.returnTypeRef).coneType,
        typeValues: List<ConeCangJieType> = emptyList()): CfirFunctionCall =
        buildFunctionCall {
            source = source("call-${function.symbol.name}"); coneTypeOrNull = resultType
            calleeReference = buildResolvedNamedReference { source = function.source; name = function.symbol.name; resolvedSymbol = function.symbol }
            explicitReceiver = receiver; dispatchReceiver = receiver
            argumentList = buildArgumentList { source = source("arguments"); arguments += values }
            typeArguments += typeValues.map(::typeRef)
            origin = CfirFunctionCallOrigin.Regular; sourceCallHasSideEffect = true
        }

    private fun assignment(variable: CfirVariable, value: CfirExpression): CfirAssignment = buildAssignment {
        source = source("write-${variable.symbol.name}"); lValue = access(variable); rValue = value
        operation = CfirSourceOperator.ASSIGN; isCompound = false; coneTypeOrNull = unitType
    }

    private fun block(vararg values: CfirStatement): CfirBlock = buildBlock {
        source = source("block"); statements += values; coneTypeOrNull = unitType
    }
    private fun integer(value: Int): CfirLiteralExpression = buildLiteralExpression {
        source = source("int-$value"); kind = CfirLiteralKind.INT; this.value = value.toLong(); coneTypeOrNull = intType
    }
    private fun boolean(value: Boolean): CfirLiteralExpression = buildLiteralExpression {
        source = source("bool-$value"); kind = CfirLiteralKind.BOOLEAN; this.value = value; coneTypeOrNull = boolType
    }
    private fun unitLiteral(): CfirLiteralExpression = buildLiteralExpression {
        source = source("unit"); kind = CfirLiteralKind.UNIT; value = Unit; coneTypeOrNull = unitType
    }
    private fun typeRef(type: ConeCangJieType): CfirResolvedTypeRef = buildResolvedTypeRef { source = source("type"); coneType = type }
    private fun source(name: String) = object : CjBinarySourceElement(name, null, name) {}
}

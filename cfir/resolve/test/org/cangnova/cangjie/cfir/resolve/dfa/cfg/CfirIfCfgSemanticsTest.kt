@file:OptIn(CfgInternals::class, org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve.dfa.cfg

import java.util.Collections
import java.util.IdentityHashMap
import org.cangnova.cangjie.cfir.CfirFunctionTarget
import org.cangnova.cangjie.cfir.CfirLoopTarget
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.patterns.builder.buildCatchPattern
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl
import org.cangnova.cangjie.cfir.symbols.CfirAnonymousFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance

/** 只调用正式 if Builder；不补边、不复活 dead 节点、不改旧 exceptional/RGT oracle。 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CfirIfCfgSemanticsTest {
    private lateinit var sourceElements: CfirCfgSourceElements

    @BeforeAll
    fun createDeclarationSourceElements() { sourceElements = CfirCfgSourceElements() }

    @AfterAll
    fun releaseDeclarationSourceElements() { sourceElements.close() }

    @Test
    fun thenThrowWithoutElseKeepsTheRealFalseInputLive() {
        val f = Fixture(); val e = f.control()
        f.start(); val condition = f.begin(e)
        val thrown = f.b.exitThrowExceptionNode(f.thrown())
        val thenExit = f.endThen(e)
        val exit = f.b.exitIfExpression(e, true)
        val tail = f.emit(f.call())
        f.finish()
        assertTrue(thenExit.isDead)
        assertFalse(exit.isUnion)
        assertFalse(exit.isDead)
        assertSame(IfBranchFalse, condition.edgeTo(exit).label)
        assertFalse(condition.edgeTo(exit).kind.isDead)
        assertTrue(reachable(condition, tail))
        assertFalse(reachable(thrown, tail, normalOnly = true))
    }

    @Test
    fun thenReturnWithoutElseKeepsTheFollowingStatementLive() {
        val f = Fixture(); val e = f.control(); val statement = f.returnTo(f.owner)
        f.start(); val condition = f.begin(e)
        f.b.enterJump(statement); val jump = f.b.exitJump(statement)
        f.endThen(e); val exit = f.b.exitIfExpression(e, true)
        val tail = f.emit(f.call()); val graph = f.finish()
        assertFalse(exit.isDead)
        assertFalse(tail.isDead)
        assertTrue(reachable(condition, tail))
        assertFalse(reachable(jump, tail, normalOnly = true))
        assertTrue(ControlFlowGraphCompletionTraversal(graph).returnPredecessors().contains(jump))
    }

    @Test
    fun bothReturnBranchesMakeOnlyTheNormalContinuationDead() {
        val f = Fixture(); val e = f.control(buildBlock {})
        val left = f.returnTo(f.owner); val right = f.returnTo(f.owner)
        e.thenBranch.replaceStatements(listOf(left)); (e.elseBranch as CfirBlock).replaceStatements(listOf(right))
        f.start(); f.begin(e)
        f.b.enterJump(left); val l = f.b.exitJump(left); f.endThen(e)
        f.beginElse(e); f.b.enterJump(right); val r = f.b.exitJump(right); f.endElse(e)
        val exit = f.b.exitIfExpression(e, true); val tail = f.emit(f.call()); val graph = f.finish()
        assertTrue(exit.isDead)
        assertTrue(tail.isDead)
        assertFalse(reachable(graph.enterNode, tail))
        assertEquals(identitySet<CFGNode<*>>().apply { add(l); add(r) },
            ControlFlowGraphCompletionTraversal(graph).returnPredecessors())
        assertFalse(graph.exitNode.isDead)
    }

    @Test
    fun elseStartsAtTheConditionAndNeverAtTheThenTail() {
        val f = Fixture(); val e = f.control(buildBlock {})
        f.start(); val condition = f.begin(e)
        val left = f.emit(f.call()); val leftExit = f.endThen(e)
        val elseEnter = f.beginElse(e)
        val right = f.emit(f.call()); f.endElse(e)
        val exit = f.b.exitIfExpression(e, true); f.finish()
        assertSame(condition, elseEnter.previousNodes.single())
        assertSame(IfBranchFalse, condition.edgeTo(elseEnter).label)
        assertFalse(reachable(leftExit, right))
        assertFalse(reachable(left, right))
        assertFalse(exit.isDead)
    }

    @Test
    fun elseIfWithoutLastElseKeepsBothRealExitFrames() {
        val f = Fixture(); val inner = f.control(); val outer = f.control(inner)
        f.start(); f.begin(outer); f.b.exitThrowExceptionNode(f.thrown()); f.endThen(outer)
        f.b.enterIfElseBranch(outer); f.begin(inner)
        f.b.exitThrowExceptionNode(f.thrown()); f.endThen(inner)
        val innerExit = f.b.exitIfExpression(inner, true)
        f.b.exitIfBranchResult(outer, false)
        val outerExit = f.b.exitIfExpression(outer, true)
        val tail = f.emit(f.call()); val graph = f.finish()
        assertSame(inner, outer.elseBranch)
        assertFalse(innerExit.isDead); assertFalse(outerExit.isDead); assertFalse(tail.isDead)
        assertEquals(2, graph.nodes.filterIsInstance<IfExitNode>().size)
        assertTrue(reachable(innerExit, tail))
    }

    @Test
    fun literalBooleanBranchesKeepTheirActualTrueAndFalseLabels() {
        for (selected in listOf(true, false)) {
            val f = Fixture(); val e = f.control(buildBlock {}, condition = f.boolean(selected))
            f.start(); f.b.enterIfExpression(e); f.b.exitLiteralExpression(e.condition as CfirLiteralExpression)
            val (condition, thenEnter) = f.b.exitIfCondition(e); f.b.enterBlock(e.thenBranch)
            val left = f.emit(f.call()); f.endThen(e)
            val elseEnter = f.beginElse(e); val right = f.emit(f.call()); f.endElse(e)
            val exit = f.b.exitIfExpression(e, true); f.finish()
            assertSame(IfBranchTrue, condition.edgeTo(thenEnter).label)
            assertSame(IfBranchFalse, condition.edgeTo(elseEnter).label)
            assertEquals(!selected, left.isDead)
            assertEquals(selected, right.isDead)
            assertFalse(exit.isDead)
        }
    }

    @Test
    fun conditionalThrowHasNormalAndCaughtCompletionThroughItsRealFinally() {
        val f = Fixture(); val e = f.control()
        val caught = buildCatch {
            pattern = buildCatchPattern { isWildcard = true; typeRefs.add(f.exceptionType.toCfirResolvedTypeRef()); resolvedTypeRef = f.exceptionType.toCfirResolvedTypeRef() }
            body = buildBlock {}
        }
        val t = buildTryExpression { tryBlock = buildBlock { statements.add(e) }; catches.add(caught); finallyBlock = buildBlock {} }
        f.start(); f.b.enterTryExpression(t); f.b.enterBlock(t.tryBlock)
        f.begin(e); val thrown = f.b.exitThrowExceptionNode(f.thrown()); f.endThen(e)
        f.b.exitIfExpression(e, true); val normal = f.emit(f.call())
        f.b.exitBlock(t.tryBlock); f.b.exitTryMainBlock()
        val catchEnter = f.b.enterCatchClause(caught); f.b.refineExceptionDispatch(catchEnter) { a, b -> a == b }
        f.b.enterBlock(caught.body); f.b.exitBlock(caught.body); val catchExit = f.b.exitCatchClause(caught)
        val finallyEnter = f.finallyBody(t)
        f.b.exitTryExpression(true); val tail = f.emit(f.call()); f.finish()
        assertFalse(normal.isDead)
        assertTrue(reachable(thrown, catchEnter))
        assertTrue(reachable(normal, finallyEnter))
        assertTrue(reachable(catchExit, finallyEnter))
        assertFalse(reachable(normal, tail, blocked = setOf(finallyEnter)))
        assertFalse(reachable(catchExit, tail, blocked = setOf(finallyEnter)))
    }

    @Test
    fun finallyConditionalReturnPreservesTheOldFalseCompletionAndNewTrueReturn() {
        val f = Fixture(); val old = f.returnTo(f.owner); val replacement = f.returnTo(f.owner); val e = f.control()
        e.thenBranch.replaceStatements(listOf(replacement))
        val t = buildTryExpression { tryBlock = buildBlock { statements.add(old) }; finallyBlock = buildBlock { statements.add(e) } }
        f.start(); f.b.enterTryExpression(t); f.b.enterBlock(t.tryBlock)
        f.b.enterJump(old); val oldJump = f.b.exitJump(old)
        f.b.exitBlock(t.tryBlock); f.b.exitTryMainBlock()
        f.finallyBody(t) {
            f.begin(e); f.b.enterJump(replacement); f.b.exitJump(replacement); f.endThen(e); f.b.exitIfExpression(e, true)
        }
        f.b.exitTryExpression(true); val graph = f.finish()
        val returns = ControlFlowGraphCompletionTraversal(graph).returnPredecessors()
        assertTrue(returns.contains(oldJump))
        val newJump = graph.nodes.filterIsInstance<JumpNode>().single { it.fir === replacement }
        assertTrue(returns.contains(newJump))
        val values = f.b.returnExpressionsOfFunction(f.owner)!!
        assertTrue(values.any { it === old.result })
        assertTrue(values.any { it === replacement.result })
        assertSame(f.owner, replacement.target.labeledElement)
    }

    @Test
    fun activeNestedSnapshotMapsEachIfExitAndSavedFalseInput() {
        val f = Fixture(); val outer = f.control(); val inner = f.control()
        outer.thenBranch.replaceStatements(listOf(inner))
        f.start(); val outerCondition = f.begin(outer); val innerCondition = f.begin(inner)
        val before = f.b.currentGraph; val copier = ControlFlowGraphCopier()
        val copy = f.b.createSnapshot(copier)
        val copiedOuter = copier[outerCondition]; val copiedInner = copier[innerCondition]
        copier.finish(); f.b = copy
        f.b.exitThrowExceptionNode(f.thrown()); f.endThen(inner)
        val innerExit = f.b.exitIfExpression(inner, true); f.endThen(outer)
        val outerExit = f.b.exitIfExpression(outer, true)
        val tail = f.emit(f.call()); val graph = f.finish()
        assertNotSame(before, graph)
        assertSame(graph, copiedOuter.owner); assertSame(graph, copiedInner.owner)
        assertSame(IfBranchFalse, copiedInner.edgeTo(innerExit).label)
        assertSame(IfBranchFalse, copiedOuter.edgeTo(outerExit).label)
        assertFalse(innerExit.isDead); assertFalse(outerExit.isDead); assertFalse(tail.isDead)
        assertTrue(graph.nodes.none { it.owner === before })
        assertEquals(2, graph.nodes.filterIsInstance<IfExitNode>().size)
    }

    @Test
    fun continueInsideIfKeepsLoopBackEdgeAndFalseBodyContinuation() {
        val f = Fixture(); val e = f.control()
        val loop = buildLoopExpression { condition = f.call(ConePrimitiveType.BOOLEAN); body = buildBlock { statements.add(e) }; isDoWhile = false; coneTypeOrNull = ConePrimitiveType.UNIT }
        val statement = buildContinueExpression { target = CfirLoopTarget().also { it.bind(loop) }; coneTypeOrNull = ConePrimitiveType.NOTHING }
        f.start(); f.b.enterWhileLoop(loop); f.emit(loop.condition as CfirFunctionCall)
        val (_, block) = f.b.exitWhileLoopCondition(loop); f.b.enterBlock(loop.body)
        f.begin(e); val jump = f.b.exitJump(statement); f.endThen(e)
        val exit = f.b.exitIfExpression(e, true); val tail = f.emit(f.call())
        f.b.exitBlock(loop.body); val (condition, _, _) = f.b.exitWhileLoop(loop); f.finish()
        assertTrue(jump.edgeTo(condition).kind.isBack)
        assertFalse(jump.edgeTo(condition).kind.isDead)
        assertFalse(exit.isDead); assertFalse(tail.isDead)
        assertTrue(reachable(block, tail))
    }

    @Test
    fun delayedThenNothingCannotKillANoElseFalseInput() {
        val f = Fixture(); val e = f.control(); val call = f.lateNothingCall()
        f.start(); val condition = f.begin(e)
        f.emit(call, false); f.endThen(e)
        call.replaceConeTypeOrNull(ConePrimitiveType.NOTHING)
        val exit = f.b.exitIfExpression(e, false); val tail = f.emit(f.call()); f.finish()
        assertFalse(condition.edgeTo(exit).kind.isDead)
        assertSame(IfBranchFalse, condition.edgeTo(exit).label)
        assertFalse(exit.isDead); assertFalse(tail.isDead)
        assertTrue(reachable(condition, tail))
    }

    @Test
    fun delayedNothingInBothBranchesKillsTheJoinAndPreservesExceptionEdges() {
        val f = Fixture(); val e = f.control(buildBlock {}); val left = f.lateNothingCall(); val right = f.lateNothingCall()
        f.start(); f.begin(e); val l = f.emit(left, false); f.endThen(e)
        f.beginElse(e); val r = f.emit(right, false); f.endElse(e)
        left.replaceConeTypeOrNull(ConePrimitiveType.NOTHING); right.replaceConeTypeOrNull(ConePrimitiveType.NOTHING)
        val exit = f.b.exitIfExpression(e, false); val tail = f.emit(f.call()); val graph = f.finish()
        assertTrue(exit.isDead); assertTrue(tail.isDead)
        assertFalse(reachable(graph.enterNode, tail))
        for (node in listOf(l, r)) {
            val transfer = node.followingNodes.filterIsInstance<ExceptionTransferNode>().single()
            assertFalse(node.edgeTo(transfer).kind.isDead)
            assertSame(UncaughtExceptionPath, node.edgeTo(transfer).label)
            assertTrue(node.followingNodes.filterIsInstance<StubNode>().single().isDead)
        }
    }

    @Test
    fun delayedConditionNothingKillsBothSuccessfulBranchesAndTheFalseShortcut() {
        val f = Fixture(); val conditionCall = f.lateNothingCall()
        val e = f.control(buildBlock {}, condition = conditionCall)
        f.start(); f.b.enterIfExpression(e); val callNode = f.emit(conditionCall, false)
        f.b.exitIfCondition(e); f.b.enterBlock(e.thenBranch); val left = f.emit(f.call()); f.endThen(e)
        f.beginElse(e); val right = f.emit(f.call()); f.endElse(e)
        conditionCall.replaceConeTypeOrNull(ConePrimitiveType.NOTHING)
        val exit = f.b.exitIfExpression(e, false); val tail = f.emit(f.call()); val graph = f.finish()
        assertTrue(left.isDead); assertTrue(right.isDead); assertTrue(exit.isDead); assertTrue(tail.isDead)
        assertTrue(reachable(callNode, graph.exceptionExitNode!!))
    }

    @Test
    fun actualBranchLambdaCallsMergeOrLiftAtTheIfPolicyBoundary() {
        for (completed in listOf(false, true)) {
            val f = Fixture(); val left = f.lambdaCarrier(); val right = f.lambdaCarrier()
            val e = f.control(buildBlock { statements.add(right) }, f.functionType)
            e.thenBranch.replaceStatements(listOf(left))
            val outer = f.call(ConePrimitiveType.UNIT, listOf(e))
            f.start(); f.b.enterCallArguments(outer, emptyList()); f.begin(e)
            val l = f.emitLambdaCarrier(left); f.endThen(e)
            f.beginElse(e); val r = f.emitLambdaCarrier(right); f.endElse(e)
            val exit = f.b.exitIfExpression(e, completed)
            f.b.exitCallArguments(); f.b.enterFunctionCall(outer); f.b.exitFunctionCall(outer, true)
            val graph = f.finish()
            val merges = graph.nodes.filterIsInstance<MergePostponedLambdaExitsNode>().filter { it.fir === e }
            if (completed) {
                assertTrue(merges.isEmpty())
                for (node in listOf(l, r)) assertSame(PostponedPath, node.edgeTo(exit).label)
            } else {
                val merge = merges.single()
                assertSame(e, merge.fir)
                for (node in listOf(l, r)) assertEquals(EdgeKind.DfgForward, node.edgeTo(merge).kind)
                assertFalse(merge.isDead)
            }
            assertFalse(exit.isDead)
        }
    }

    @Test
    fun snapshotRestoresBranchPostponedExitsIntoTheSameEnclosingCallLayer() {
        val f = Fixture(); val left = f.lambdaCarrier(); val right = f.lambdaCarrier()
        val e = f.control(buildBlock { statements.add(right) }, f.functionType); e.thenBranch.replaceStatements(listOf(left))
        val outer = f.call(ConePrimitiveType.UNIT, listOf(e))
        f.start(); f.b.enterCallArguments(outer, emptyList()); f.begin(e)
        val leftExit = f.emitLambdaCarrier(left); f.endThen(e)
        val original = f.b.currentGraph; val copier = ControlFlowGraphCopier()
        val copy = f.b.createSnapshot(copier); val copiedLeft = copier[leftExit]
        copier.finish(); f.b = copy
        f.beginElse(e); val rightExit = f.emitLambdaCarrier(right); f.endElse(e)
        val exit = f.b.exitIfExpression(e, false)
        f.b.exitCallArguments(); f.b.enterFunctionCall(outer); f.b.exitFunctionCall(outer, true)
        val graph = f.finish(); val merge = graph.nodes.filterIsInstance<MergePostponedLambdaExitsNode>().single { it.fir === e }
        assertNotSame(original, graph)
        assertSame(graph, copiedLeft.owner); assertSame(graph, rightExit.owner)
        assertEquals(EdgeKind.DfgForward, copiedLeft.edgeTo(merge).kind)
        assertEquals(EdgeKind.DfgForward, rightExit.edgeTo(merge).kind)
        assertFalse(exit.isDead)
    }

    @Test
    fun returnOfIfResultMovesItsRealPostponedLayerToTheOwningLambda() {
        val f = Fixture(); val root = f.lambda(f.functionType); val left = f.lambdaCarrier(); val right = f.lambdaCarrier()
        val e = f.control(buildBlock { statements.add(right) }, f.functionType); e.thenBranch.replaceStatements(listOf(left))
        val statement = f.returnTo(root, e); root.body!!.replaceStatements(listOf(statement))
        val rootExpression = f.lambdaExpression(root); val outer = f.call(ConePrimitiveType.UNIT, listOf(rootExpression))
        f.start(); f.b.enterCallArguments(outer, listOf(root)); f.b.enterAnonymousFunctionExpression(rootExpression)
        f.b.enterAnonymousFunction(root); f.b.enterBlock(root.body!!)
        // 与真实 transformer 相同：return 层先进入，result 内的 if/branch calls 才产生 postponed exits。
        f.b.enterJump(statement); f.begin(e); f.emitLambdaCarrier(left); f.endThen(e)
        f.beginElse(e); f.emitLambdaCarrier(right); f.endElse(e); f.b.exitIfExpression(e, false)
        f.b.exitJump(statement); f.b.exitBlock(root.body!!)
        val rootGraph = f.b.exitAnonymousFunction(root).third
        root.replaceControlFlowGraphReference(CfirControlFlowGraphReferenceImpl(rootGraph))
        f.b.exitCallArguments(); f.b.enterFunctionCall(outer); val outerExit = f.b.exitFunctionCall(outer, true); f.finish()
        val merge = rootGraph.nodes.filterIsInstance<MergePostponedLambdaExitsNode>().single { it.fir === e }
        assertEquals(EdgeKind.DfgForward, merge.edgeTo(outerExit).kind)
        assertSame(PostponedPath, merge.edgeTo(outerExit).label)
        val info = f.b.returnExpressionsOfAnonymousFunction(root)!!.single()
        assertSame(e, info.expression); assertSame(statement, info.containingStatement)
        assertSame(root, statement.target.labeledElement)
    }

    private inner class Fixture {
        val session = CallResolutionTestFixtures.newTestSession()
        var b = ControlFlowGraphBuilder()
        val body = buildBlock { coneTypeOrNull = ConePrimitiveType.UNIT }
        val owner = CallResolutionTestFixtures.buildFunctionSymbol(session, "owner", ConePrimitiveType.UNIT).cfir.also { it.replaceBody(body) }
        val exceptionType = ConeClassLikeType(ConeClassLikeLookupTagImpl(ClassId.topLevel(FqName("std.core.Exception"))))
        val functionType = ConeFunctionType(emptyList(), ConePrimitiveType.UNIT)
        private var serial = 0
        fun call(type: ConeCangJieType = ConePrimitiveType.UNIT, arguments: List<CfirExpression> = emptyList()): CfirFunctionCall {
            val name = "call" + serial++
            val symbol = CallResolutionTestFixtures.buildFunctionSymbol(session, name, type,
                parameterTypes = arguments.map { it.coneTypeOrNull!! })
            return buildFunctionCall {
                coneTypeOrNull = type
                calleeReference = buildResolvedNamedReference { this.name = symbol.cfir.name; resolvedSymbol = symbol }
                argumentList = buildArgumentList { this.arguments.addAll(arguments) }
            }
        }
        fun lateNothingCall(): CfirFunctionCall = call(ConePrimitiveType.NOTHING).also { it.replaceConeTypeOrNull(ConePrimitiveType.UNIT) }
        fun control(elseBranch: CfirExpression? = null, type: ConeCangJieType = ConePrimitiveType.UNIT,
            condition: CfirExpression = call(ConePrimitiveType.BOOLEAN)): CfirIfExpression = buildIfExpression {
            this.condition = condition; thenBranch = buildBlock { coneTypeOrNull = type }
            this.elseBranch = elseBranch; coneTypeOrNull = type
        }
        fun boolean(value: Boolean) = buildLiteralExpression { kind = CfirLiteralKind.BOOLEAN; this.value = value; coneTypeOrNull = ConePrimitiveType.BOOLEAN }
        fun thrown() = buildThrowExpression { exception = call(exceptionType); coneTypeOrNull = ConePrimitiveType.NOTHING }
        fun returnTo(function: CfirFunction, result: CfirExpression = buildBlock { coneTypeOrNull = ConePrimitiveType.UNIT }) = buildReturnExpression {
            target = CfirFunctionTarget(isLambda = function is CfirAnonymousFunction).also { it.bind(function) }
            this.result = result; coneTypeOrNull = ConePrimitiveType.NOTHING
        }
        fun start() { b.enterFunction(owner); b.enterBlock(body) }
        fun finish(): ControlFlowGraph {
            b.exitBlock(body)
            return b.exitFunction(owner).second.also { owner.replaceControlFlowGraphReference(CfirControlFlowGraphReferenceImpl(it)) }
        }
        fun emit(call: CfirFunctionCall, completed: Boolean = true): FunctionCallExitNode {
            b.enterCallArguments(call, emptyList()); b.exitCallArguments(); b.enterFunctionCall(call)
            return b.exitFunctionCall(call, completed)
        }
        fun begin(e: CfirIfExpression): IfConditionExitNode {
            b.enterIfExpression(e); emit(e.condition as CfirFunctionCall)
            val (condition, _) = b.exitIfCondition(e); b.enterBlock(e.thenBranch); return condition
        }
        fun endThen(e: CfirIfExpression): IfBranchResultExitNode { b.exitBlock(e.thenBranch); return b.exitIfBranchResult(e, true) }
        fun beginElse(e: CfirIfExpression): IfBranchResultEnterNode =
            b.enterIfElseBranch(e).also { b.enterBlock(e.elseBranch as CfirBlock) }
        fun endElse(e: CfirIfExpression): IfBranchResultExitNode { b.exitBlock(e.elseBranch as CfirBlock); return b.exitIfBranchResult(e, false) }
        fun finallyBody(t: CfirTryExpression, action: () -> Unit = {}): FinallyBlockEnterNode {
            val enter = b.enterFinallyBlock(); b.enterBlock(t.finallyBlock!!)
            action(); b.exitBlock(t.finallyBlock!!); b.exitFinallyBlock(); return enter
        }
        fun lambda(type: ConeCangJieType = ConePrimitiveType.UNIT): CfirAnonymousFunction {
            val parsed = sourceElements.lambda(returnsFunction = type is ConeFunctionType)
            return buildAnonymousFunction {
                source = parsed.declaration
                moduleData = session.moduleData; resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY
                isLocal = true; status = CfirDeclarationStatusImpl()
                returnTypeRef = type.toCfirResolvedTypeRef(); typeRef = ConeFunctionType(emptyList(), type).toCfirResolvedTypeRef()
                symbol = CfirAnonymousFunctionSymbol(); hasExplicitParameterList = true; isLambda = true
                body = buildBlock { source = parsed.body; coneTypeOrNull = type }
            }
        }
        fun lambdaExpression(lambda: CfirAnonymousFunction): CfirAnonymousFunctionExpression = buildAnonymousFunctionExpression {
            source = kotlin.checkNotNull(lambda.source)
            anonymousFunction = lambda; isTrailingLambda = false
        }
        fun lambdaCarrier(): CfirFunctionCall = call(functionType, listOf(lambdaExpression(lambda())))
        fun emitLambdaCarrier(call: CfirFunctionCall): PostponedLambdaExitNode {
            val expression = call.argumentList.arguments.single() as CfirAnonymousFunctionExpression
            val lambda = expression.anonymousFunction
            b.enterCallArguments(call, listOf(lambda)); b.enterAnonymousFunctionExpression(expression)
            b.enterAnonymousFunction(lambda); b.enterBlock(lambda.body!!); b.exitBlock(lambda.body!!)
            val (_, postponed, graph) = b.exitAnonymousFunction(lambda)
            lambda.replaceControlFlowGraphReference(CfirControlFlowGraphReferenceImpl(graph))
            b.exitCallArguments(); b.enterFunctionCall(call); b.exitFunctionCall(call, false)
            return kotlin.checkNotNull(postponed)
        }
    }

    private fun reachable(start: CFGNode<*>, target: CFGNode<*>, normalOnly: Boolean = false, blocked: Set<CFGNode<*>> = emptySet()): Boolean {
        val seen = identitySet<CFGNode<*>>(); val queue = ArrayDeque<CFGNode<*>>(); queue.add(start)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node in blocked || !seen.add(node)) continue
            if (node === target) return true
            for (next in node.followingNodes) {
                val edge = node.edgeTo(next)
                if (edge.kind.usedInCfa && !edge.kind.isDead && (!normalOnly || edge.label === NormalPath)) queue.add(next)
            }
        }
        return false
    }
    private fun <T : Any> identitySet(): MutableSet<T> = Collections.newSetFromMap(IdentityHashMap<T, Boolean>())
}

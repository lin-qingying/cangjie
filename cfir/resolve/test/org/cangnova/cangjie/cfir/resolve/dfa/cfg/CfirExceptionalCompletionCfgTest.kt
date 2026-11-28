@file:OptIn(CfgInternals::class, org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve.dfa.cfg

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildCodeFragment
import org.cangnova.cangjie.cfir.declarations.builder.buildFieldVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.declarations.builder.buildResourceVariable
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.patterns.builder.buildCatchPattern
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance

/** 官方已验证的异常控制合同；直接使用正式 builder/AST，禁止替换既有 RGT 冷入口。 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CfirExceptionalCompletionCfgTest {
    private lateinit var sourceElements: CfirCfgSourceElements

    @BeforeAll
    fun createDeclarationSourceElements() { sourceElements = CfirCfgSourceElements() }

    @AfterAll
    fun releaseDeclarationSourceElements() { sourceElements.close() }

    @Test
    fun typedCatchChoosesFirstMatchingUnionAndKeepsTheSequentialStubDead() {
        val f = Fixture()
        val first = f.catch(f.otherType)
        val union = f.catch(f.otherType, f.exceptionType)
        val shadowed = f.catch(f.exceptionType)
        val t = f.tryExpression(first, union, shadowed)
        f.start()
        f.builder.enterTryExpression(t)
        f.builder.enterBlock(t.tryBlock)
        val thrown = f.builder.exitThrowExceptionNode(f.thrown)
        val unreachable = f.call()
        f.builder.exitBlock(t.tryBlock)
        f.builder.exitTryMainBlock()
        val entries = t.catches.map { f.catchBody(it) }
        f.builder.exitTryExpression(true)
        val graph = f.finish()
        assertFalse(reachable(thrown, entries[0]))
        assertTrue(reachable(thrown, entries[1]))
        assertFalse(reachable(thrown, entries[2]))
        assertFalse(reachable(thrown, unreachable, normalOnly = true))
        assertTrue(unreachable.isDead)
        assertTrue(thrown.followingNodes.filterIsInstance<StubNode>().single().isDead)
        assertTrue(graph.nodes.filterIsInstance<ExceptionDispatchNode>().any { it.acceptedTypes.size == 2 })
    }

    @Test
    fun catchRecoveryMustPassThroughItsRealFinallyBeforeTheTailCall() {
        val f = Fixture()
        val t = f.tryExpression(f.catch(f.exceptionType), finallyBlock = buildBlock {})
        f.start()
        f.builder.enterTryExpression(t)
        f.builder.enterBlock(t.tryBlock)
        val thrown = f.builder.exitThrowExceptionNode(f.thrown)
        f.builder.exitBlock(t.tryBlock)
        f.builder.exitTryMainBlock()
        val caught = f.catchBody(t.catches.single())
        val finallyEnter = f.finallyBody(t)
        f.builder.exitTryExpression(true)
        val tail = f.call()
        f.finish()
        assertTrue(reachable(thrown, caught))
        assertTrue(reachable(caught, tail))
        assertFalse(reachable(caught, tail, blocked = setOf(finallyEnter)))
        assertSame(t, finallyEnter.fir)
    }

    @Test
    fun unmatchedExceptionRunsNestedFinallyAndRethrowsIntoOuterCatch() {
        val f = Fixture()
        val outer = f.tryExpression(f.catch(f.exceptionType))
        val inner = f.tryExpression(finallyBlock = buildBlock {})
        f.start()
        f.builder.enterTryExpression(outer)
        f.builder.enterBlock(outer.tryBlock)
        f.builder.enterTryExpression(inner)
        f.builder.enterBlock(inner.tryBlock)
        val thrown = f.builder.exitThrowExceptionNode(f.thrown)
        f.builder.exitBlock(inner.tryBlock)
        f.builder.exitTryMainBlock()
        val finallyEnter = f.finallyBody(inner)
        f.builder.exitTryExpression(true)
        f.builder.exitBlock(outer.tryBlock)
        f.builder.exitTryMainBlock()
        val caught = f.catchBody(outer.catches.single())
        f.builder.exitTryExpression(true)
        f.finish()
        assertTrue(reachable(thrown, caught))
        assertFalse(reachable(thrown, caught, blocked = setOf(finallyEnter)))
    }

    @Test
    fun throwFromCatchSkipsSiblingCatches() {
        val f = Fixture()
        val outer = f.tryExpression(f.catch(f.exceptionType))
        val inner = f.tryExpression(f.catch(f.exceptionType), f.catch(f.exceptionType))
        f.start()
        f.builder.enterTryExpression(outer)
        f.builder.enterBlock(outer.tryBlock)
        f.builder.enterTryExpression(inner)
        f.builder.enterBlock(inner.tryBlock)
        f.builder.exitThrowExceptionNode(f.thrown)
        f.builder.exitBlock(inner.tryBlock)
        f.builder.exitTryMainBlock()
        var rethrow: ThrowExceptionNode? = null
        f.catchBody(inner.catches[0]) { rethrow = f.builder.exitThrowExceptionNode(f.throwExpression()) }
        val sibling = f.catchBody(inner.catches[1])
        f.builder.exitTryExpression(true)
        f.builder.exitBlock(outer.tryBlock)
        f.builder.exitTryMainBlock()
        val target = f.catchBody(outer.catches.single())
        f.builder.exitTryExpression(true)
        f.finish()
        assertTrue(reachable(checkNotNull(rethrow), target))
        assertFalse(reachable(checkNotNull(rethrow), sibling))
    }

    @Test
    fun finallyThrowReplacesPendingExceptionAndDoesNotResumeTheOldOne() {
        val f = Fixture()
        val outer = f.tryExpression(f.catch(f.exceptionType))
        val inner = f.tryExpression(finallyBlock = buildBlock {})
        f.start()
        f.builder.enterTryExpression(outer)
        f.builder.enterBlock(outer.tryBlock)
        f.builder.enterTryExpression(inner)
        f.builder.enterBlock(inner.tryBlock)
        f.builder.exitThrowExceptionNode(f.thrown)
        f.builder.exitBlock(inner.tryBlock)
        f.builder.exitTryMainBlock()
        var replacement: ThrowExceptionNode? = null
        f.finallyBody(inner) { replacement = f.builder.exitThrowExceptionNode(f.throwExpression()) }
        f.builder.exitTryExpression(true)
        f.builder.exitBlock(outer.tryBlock)
        f.builder.exitTryMainBlock()
        val target = f.catchBody(outer.catches.single())
        f.builder.exitTryExpression(true)
        f.finish()
        val paths = completionPaths(checkNotNull(replacement), target,
            listOf(ControlFlowGraphCompletionPath.PendingFinally(
                innerGraphFinally(checkNotNull(replacement)),
                innerGraphFinally(checkNotNull(replacement)).previousNodes.map { it.edgeTo(innerGraphFinally(checkNotNull(replacement))).label }
                    .filterIsInstance<ExceptionTransferNode>().first(),
            )))
        assertTrue(paths.isNotEmpty())
        assertTrue(paths.all { it.pending.isEmpty() })
    }

    @Test
    fun ordinaryLambdaHasAnExceptionalExitAndDoesNotReachTheDefinitionCatch() {
        val f = Fixture()
        val t = f.tryExpression(f.catch(f.exceptionType))
        f.start()
        f.builder.enterTryExpression(t)
        f.builder.enterBlock(t.tryBlock)
        val lambda = f.lambda()
        f.builder.enterAnonymousFunction(lambda)
        f.builder.enterBlock(checkNotNull(lambda.body))
        val thrown = f.builder.exitThrowExceptionNode(f.thrown)
        f.builder.exitBlock(checkNotNull(lambda.body))
        val lambdaGraph = f.builder.exitAnonymousFunction(lambda).third
        f.builder.exitBlock(t.tryBlock)
        f.builder.exitTryMainBlock()
        val definitionCatch = f.catchBody(t.catches.single())
        f.builder.exitTryExpression(true)
        f.finish()
        assertTrue(reachable(thrown, checkNotNull(lambdaGraph.exceptionExitNode)))
        assertFalse(reachable(thrown, definitionCatch))
        assertTrue(lambdaGraph.exitNode.followingNodes.any { it === lambdaGraph.enterNode && lambdaGraph.exitNode.edgeTo(it).kind.isBack })
    }

    @Test
    fun delayedClosureFailureIsCaughtAtItsActualCall() {
        val f = Fixture()
        val t = f.tryExpression(f.catch(f.exceptionType))
        f.start()
        f.builder.enterTryExpression(t)
        f.builder.enterBlock(t.tryBlock)
        val invocation = f.call()
        f.builder.exitBlock(t.tryBlock)
        f.builder.exitTryMainBlock()
        val dynamicCatch = f.catchBody(t.catches.single())
        f.builder.exitTryExpression(true)
        val graph = f.finish()
        assertTrue(reachable(invocation, dynamicCatch))
        assertEquals(1, graph.nodes.filterIsInstance<FunctionCallExitNode>().count { it.fir === invocation.fir })
    }

    @Test
    fun initializerFailureClosesOnlySuccessfullyInitializedResourcesBeforeCatch() {
        val f = Fixture()
        val r1 = f.resource("first")
        val r2 = f.resource("second")
        val t = f.tryExpression(f.catch(f.exceptionType), finallyBlock = buildBlock {})
        t.replaceResources(listOf(r1, r2))
        f.start()
        f.builder.enterTryExpression(t)
        f.builder.enterVariableDeclaration(r1)
        f.call()
        f.builder.exitVariableDeclaration(r1)
        f.builder.enterVariableDeclaration(r2)
        val failed = f.builder.exitThrowExceptionNode(f.thrown)
        f.builder.exitVariableDeclaration(r2)
        f.builder.enterBlock(t.tryBlock)
        f.builder.exitBlock(t.tryBlock)
        f.builder.exitTryMainBlock()
        val caught = f.catchBody(t.catches.single())
        f.finallyBody(t)
        f.builder.exitTryExpression(true)
        val graph = f.finish()
        val cleanups = graph.nodes.filterIsInstance<ResourceCleanupNode>().filter { reachable(failed, it) }
        assertTrue(cleanups.any { it.fir === r1 })
        assertFalse(cleanups.any { it.fir === r2 })
        assertTrue(reachable(failed, caught))
        assertFalse(reachable(failed, caught, blocked = cleanups.toSet()))
    }

    @Test
    fun deadThrowDoesNotMakeItsCatchLive() {
        val f = Fixture()
        val t = f.tryExpression(f.catch(f.exceptionType))
        f.start()
        f.builder.exitThrowExceptionNode(f.thrown)
        f.builder.enterTryExpression(t)
        f.builder.enterBlock(t.tryBlock)
        val deadThrow = f.builder.exitThrowExceptionNode(f.throwExpression())
        f.builder.exitBlock(t.tryBlock)
        f.builder.exitTryMainBlock()
        val caught = f.catchBody(t.catches.single())
        f.builder.exitTryExpression(true)
        f.finish()
        assertTrue(deadThrow.isDead)
        assertTrue(caught.isDead)
        assertFalse(reachable(deadThrow, caught))
    }

    @Test
    fun activeSnapshotMapsDispatchAndPendingFinallyLabelsWithoutImportingOriginalNodes() {
        val f = Fixture()
        val t = f.tryExpression(f.catch(f.otherType), finallyBlock = buildBlock {})
        f.start()
        f.builder.enterTryExpression(t)
        f.builder.enterBlock(t.tryBlock)
        f.builder.exitThrowExceptionNode(f.thrown)
        f.builder.exitBlock(t.tryBlock)
        f.builder.exitTryMainBlock()
        val originalGraph = f.builder.currentGraph
        val copier = ControlFlowGraphCopier()
        val resumed = f.builder.createSnapshot(copier)
        copier.finish()
        assertNotSame(originalGraph, resumed.currentGraph)
        f.builder = resumed
        f.catchBody(t.catches.single())
        f.finallyBody(t)
        f.builder.exitTryExpression(true)
        val graph = f.finish()
        assertTrue(graph.nodes.none { it.owner === originalGraph })
        graph.nodes.forEach { node ->
            node.followingNodes.forEach { next ->
                val label = node.edgeTo(next).label
                if (label is CFGNode<*>) assertSame(graph, label.owner)
            }
        }
        assertTrue(graph.nodes.filterIsInstance<ExceptionDispatchNode>().all {
            it.transfer.owner === graph && it.precedingCatches.all { catch -> catch.owner === graph }
        })
    }

    @Test
    fun wildcardHasAResolvedExceptionTestAndDoesNotInventABinding() {
        val f = Fixture()
        val clause = f.catch()
        val t = f.tryExpression(clause)
        f.start()
        f.builder.enterTryExpression(t)
        f.builder.enterBlock(t.tryBlock)
        val thrown = f.builder.exitThrowExceptionNode(f.thrown)
        f.builder.exitBlock(t.tryBlock)
        f.builder.exitTryMainBlock()
        val caught = f.catchBody(clause)
        f.builder.exitTryExpression(true)
        val graph = f.finish()
        assertTrue(reachable(thrown, caught))
        assertNull(clause.pattern.bindingVariable)
        assertEquals(listOf(f.baseExceptionType), graph.nodes.filterIsInstance<ExceptionDispatchNode>().single().acceptedTypes)
    }

    @Test
    fun nothingCallsKeepTheirExceptionalBranchAtImmediateAndDelayedCompletion() {
        for (completed in listOf(true, false)) {
            val f = Fixture()
            val t = f.tryExpression(f.catch(f.exceptionType))
            f.start()
            f.builder.enterTryExpression(t)
            f.builder.enterBlock(t.tryBlock)
            val call = f.call(ConePrimitiveType.NOTHING, completed)
            val following = f.call()
            f.builder.exitBlock(t.tryBlock)
            f.builder.exitTryMainBlock()
            val caught = f.catchBody(t.catches.single())
            f.builder.exitTryExpression(true)
            val graph = f.finish()
            assertTrue(reachable(call, caught))
            assertFalse(reachable(call, following, normalOnly = true))
            assertTrue(following.isDead)
            assertEquals(1, graph.nodes.filterIsInstance<FunctionCallExitNode>().count { it.fir === call.fir })
        }
    }

    @Test
    fun explicitCalledInPlaceContractPropagatesToEnclosingCatch() {
        val f = Fixture()
        val t = f.tryExpression(f.catch(f.exceptionType))
        f.start()
        f.builder.enterTryExpression(t)
        f.builder.enterBlock(t.tryBlock)
        val lambda = f.lambda()
        f.builder.enterAnonymousFunction(lambda, calledInPlace = true)
        f.builder.enterBlock(checkNotNull(lambda.body))
        val thrown = f.builder.exitThrowExceptionNode(f.thrown)
        f.builder.exitBlock(checkNotNull(lambda.body))
        f.builder.exitAnonymousFunction(lambda)
        f.builder.exitBlock(t.tryBlock)
        f.builder.exitTryMainBlock()
        val caught = f.catchBody(t.catches.single())
        f.builder.exitTryExpression(true)
        f.finish()
        assertTrue(reachable(thrown, caught))
    }

    @Test
    fun returnAcrossTwoFinallyBlocksRetainsItsActualReturnArgument() {
        val f = Fixture()
        val lambda = f.lambda()
        val outer = f.tryExpression(finallyBlock = buildBlock {})
        val inner = f.tryExpression(finallyBlock = buildBlock {})
        val returnedValue = buildBlock { coneTypeOrNull = f.classType("Box2") }
        val target = org.cangnova.cangjie.cfir.CfirFunctionTarget(isLambda = true).also { it.bind(lambda) }
        val statement = buildReturnExpression {
            this.target = target
            result = returnedValue
            coneTypeOrNull = ConePrimitiveType.NOTHING
        }
        inner.tryBlock.replaceStatements(listOf(statement))
        outer.tryBlock.replaceStatements(listOf(inner))
        checkNotNull(lambda.body).replaceStatements(listOf(outer))
        f.builder.enterAnonymousFunction(lambda)
        f.builder.enterBlock(checkNotNull(lambda.body))
        f.builder.enterTryExpression(outer)
        f.builder.enterBlock(outer.tryBlock)
        f.builder.enterTryExpression(inner)
        f.builder.enterBlock(inner.tryBlock)
        f.builder.enterJump(statement)
        val returned = f.builder.exitJump(statement)
        f.builder.exitBlock(inner.tryBlock)
        f.builder.exitTryMainBlock()
        val innerFinally = f.finallyBody(inner)
        f.builder.exitTryExpression(true)
        f.builder.exitBlock(outer.tryBlock)
        f.builder.exitTryMainBlock()
        val outerFinally = f.finallyBody(outer)
        f.builder.exitTryExpression(true)
        f.builder.exitBlock(checkNotNull(lambda.body))
        val graph = f.builder.exitAnonymousFunction(lambda).third
        lambda.replaceControlFlowGraphReference(org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl(graph))
        val returns = checkNotNull(f.builder.returnExpressionsOfAnonymousFunction(lambda))
        assertSame(returnedValue, returns.single().expression)
        assertTrue(reachable(returned, graph.exitNode))
        assertFalse(reachable(returned, graph.exitNode, blocked = setOf(innerFinally)))
        assertFalse(reachable(returned, graph.exitNode, blocked = setOf(outerFinally)))
        assertTrue(completionPaths(returned, graph.exitNode).isNotEmpty())
    }

    @Test
    fun exceptionalCompletionCannotUseTheNormalExitOfASharedFinally() {
        val f = Fixture()
        val t = f.tryExpression(finallyBlock = buildBlock {})
        f.start()
        f.builder.enterTryExpression(t)
        f.builder.enterBlock(t.tryBlock)
        val call = f.call()
        val exceptional = call.followingNodes.filterIsInstance<ExceptionTransferNode>().single()
        f.builder.exitBlock(t.tryBlock)
        f.builder.exitTryMainBlock()
        f.finallyBody(t)
        f.builder.exitTryExpression(true)
        val normalTail = f.call()
        val graph = f.finish()
        assertTrue(reachable(call, normalTail, normalOnly = true))
        assertTrue(completionPaths(exceptional, checkNotNull(graph.exceptionExitNode)).isNotEmpty())
        assertTrue(completionPaths(exceptional, normalTail).isEmpty())
    }

    @Test
    fun snapshotMapsPendingCompletionAndFinallyFrameToTheCopiedGraph() {
        val f = Fixture()
        val t = f.tryExpression(finallyBlock = buildBlock {})
        f.start()
        f.builder.enterTryExpression(t)
        f.builder.enterBlock(t.tryBlock)
        f.builder.exitThrowExceptionNode(f.thrown)
        f.builder.exitBlock(t.tryBlock)
        f.builder.exitTryMainBlock()
        val entry = f.builder.enterFinallyBlock()
        val label = entry.previousNodes.map { it.edgeTo(entry).label }.filterIsInstance<ExceptionTransferNode>().single()
        val path = ControlFlowGraphCompletionPath(entry,
            listOf(ControlFlowGraphCompletionPath.PendingFinally(entry, label)))
        val copier = ControlFlowGraphCopier()
        val builderCopy = f.builder.createSnapshot(copier)
        val copiedPath = path.createSnapshot(copier)
        copier.finish()
        assertSame(builderCopy.currentGraph, copiedPath.node.owner)
        assertSame(builderCopy.currentGraph, copiedPath.pending.single().enter.owner)
        assertSame(builderCopy.currentGraph, (copiedPath.pending.single().completion as ExceptionTransferNode).owner)
        assertNotSame(path.pending.single().completion, copiedPath.pending.single().completion)
    }

    @Test
    fun finallyThrowRemovesTheSuppressedReturnFromProductionReturnCollection() {
        val f = Fixture()
        val lambda = f.lambda()
        val t = f.tryExpression(finallyBlock = buildBlock {})
        val target = org.cangnova.cangjie.cfir.CfirFunctionTarget(isLambda = true).also { it.bind(lambda) }
        val statement = buildReturnExpression {
            this.target = target
            result = buildBlock { coneTypeOrNull = f.classType("SuppressedBox") }
            coneTypeOrNull = ConePrimitiveType.NOTHING
        }
        t.tryBlock.replaceStatements(listOf(statement))
        checkNotNull(lambda.body).replaceStatements(listOf(t))
        f.builder.enterAnonymousFunction(lambda)
        f.builder.enterBlock(checkNotNull(lambda.body))
        f.builder.enterTryExpression(t)
        f.builder.enterBlock(t.tryBlock)
        f.builder.enterJump(statement)
        f.builder.exitJump(statement)
        f.builder.exitBlock(t.tryBlock)
        f.builder.exitTryMainBlock()
        f.finallyBody(t) { f.builder.exitThrowExceptionNode(f.thrown) }
        f.builder.exitTryExpression(true)
        f.builder.exitBlock(checkNotNull(lambda.body))
        val graph = f.builder.exitAnonymousFunction(lambda).third
        lambda.replaceControlFlowGraphReference(org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl(graph))
        assertTrue(checkNotNull(f.builder.returnExpressionsOfAnonymousFunction(lambda)).isEmpty())
        assertTrue(checkNotNull(f.builder.returnExpressionsOfFunction(lambda)).isEmpty())
    }

    @Test
    fun finallyReturnWithTheSameTargetSuppliesOnlyItsActualReplacementArgument() {
        val f = Fixture()
        val lambda = f.lambda()
        val t = f.tryExpression(finallyBlock = buildBlock {})
        val target = org.cangnova.cangjie.cfir.CfirFunctionTarget(isLambda = true).also { it.bind(lambda) }
        val oldValue = buildBlock { coneTypeOrNull = f.classType("SuppressedBox") }
        val newValue = buildBlock { coneTypeOrNull = f.classType("ReplacementBox") }
        val oldReturn = buildReturnExpression {
            this.target = target; result = oldValue; coneTypeOrNull = ConePrimitiveType.NOTHING
        }
        val newReturn = buildReturnExpression {
            this.target = target; result = newValue; coneTypeOrNull = ConePrimitiveType.NOTHING
        }
        t.tryBlock.replaceStatements(listOf(oldReturn))
        checkNotNull(t.finallyBlock).replaceStatements(listOf(newReturn))
        checkNotNull(lambda.body).replaceStatements(listOf(t))
        f.builder.enterAnonymousFunction(lambda)
        f.builder.enterBlock(checkNotNull(lambda.body))
        f.builder.enterTryExpression(t)
        f.builder.enterBlock(t.tryBlock)
        f.builder.enterJump(oldReturn)
        f.builder.exitJump(oldReturn)
        f.builder.exitBlock(t.tryBlock)
        f.builder.exitTryMainBlock()
        f.finallyBody(t) { f.builder.enterJump(newReturn); f.builder.exitJump(newReturn) }
        f.builder.exitTryExpression(true)
        f.builder.exitBlock(checkNotNull(lambda.body))
        val graph = f.builder.exitAnonymousFunction(lambda).third
        lambda.replaceControlFlowGraphReference(org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl(graph))
        val arguments = checkNotNull(f.builder.returnExpressionsOfAnonymousFunction(lambda))
        assertSame(newValue, arguments.single().expression)
        assertSame(newReturn, arguments.single().containingStatement)
        assertSame(newValue, checkNotNull(f.builder.returnExpressionsOfFunction(lambda)).single())
    }

    @Test
    fun productionReturnCollectionPreservesTheImplicitTailAfterCatchRecovery() {
        val f = Fixture()
        val lambda = f.lambda()
        val t = f.tryExpression(f.catch(f.exceptionType), finallyBlock = buildBlock {})
        f.builder.enterAnonymousFunction(lambda)
        f.builder.enterBlock(checkNotNull(lambda.body))
        f.builder.enterTryExpression(t)
        f.builder.enterBlock(t.tryBlock)
        f.builder.exitThrowExceptionNode(f.thrown)
        f.builder.exitBlock(t.tryBlock)
        f.builder.exitTryMainBlock()
        f.catchBody(t.catches.single())
        f.finallyBody(t)
        f.builder.exitTryExpression(true)
        val tail = f.call(f.classType("Box2")).fir
        checkNotNull(lambda.body).replaceStatements(listOf(t, tail))
        f.builder.exitBlock(checkNotNull(lambda.body))
        val graph = f.builder.exitAnonymousFunction(lambda).third
        lambda.replaceControlFlowGraphReference(org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl(graph))
        val arguments = checkNotNull(f.builder.returnExpressionsOfAnonymousFunction(lambda))
        assertSame(tail, arguments.single().expression)
        assertSame(lambda.body, arguments.single().containingStatement)
        assertFalse(arguments.single().isExplicit)
        assertEquals(1, graph.nodes.filterIsInstance<FunctionCallExitNode>().count { it.fir === tail })
    }

    @Test
    fun pendingThrowKeepsItsTypeAndIdentityAcrossReverseResourceCleanup() {
        val f = Fixture()
        val first = f.resource("first")
        val second = f.resource("second")
        val t = f.tryExpression(f.catch(f.otherType), f.catch(f.exceptionType))
        t.replaceResources(listOf(first, second))
        f.start()
        f.builder.enterTryExpression(t)
        for (resource in t.resources) {
            f.builder.enterVariableDeclaration(resource)
            f.builder.exitVariableDeclaration(resource)
        }
        f.builder.enterBlock(t.tryBlock)
        val thrown = f.builder.exitThrowExceptionNode(f.thrown)
        f.builder.exitBlock(t.tryBlock)
        f.builder.exitTryMainBlock()
        val sibling = f.catchBody(t.catches[0])
        val original = f.catchBody(t.catches[1])
        f.builder.exitTryExpression(true)
        val graph = f.finish()
        val cleanups = graph.nodes.filterIsInstance<ResourceCleanupNode>().filter { reachable(thrown, it) }
        assertEquals(listOf(second, first), cleanups.map { it.fir })
        assertTrue(reachable(cleanups[0], cleanups[1]))
        assertFalse(reachable(cleanups[1], cleanups[0]))
        assertTrue(reachable(thrown, original))
        assertFalse(reachable(thrown, sibling))
        val dispatches = graph.nodes.filterIsInstance<ExceptionDispatchNode>().filter { reachable(thrown, it) }
        assertTrue(dispatches.isNotEmpty())
        assertTrue(dispatches.all { it.transfer.sourceNode === thrown && it.transfer.exceptionType == f.exceptionType })
    }

    @Test
    fun firstCloseFailureRetainsItsSourceWhileRemainingResourcesAreClosed() {
        val f = Fixture()
        val first = f.resource("first")
        val second = f.resource("second")
        val t = f.tryExpression(f.catch(f.baseExceptionType))
        t.replaceResources(listOf(first, second))
        f.start()
        f.builder.enterTryExpression(t)
        for (resource in t.resources) {
            f.builder.enterVariableDeclaration(resource)
            f.builder.exitVariableDeclaration(resource)
        }
        f.builder.enterBlock(t.tryBlock)
        f.builder.exitBlock(t.tryBlock)
        val bodyExit = f.builder.exitTryMainBlock()
        val caught = f.catchBody(t.catches.single())
        f.builder.exitTryExpression(true)
        val graph = f.finish()
        val firstCleanup = bodyExit.followingNodes.filterIsInstance<ResourceCleanupNode>().single()
        assertSame(second, firstCleanup.fir)
        val failure = firstCleanup.followingNodes.filterIsInstance<ExceptionTransferNode>().single()
        val remaining = graph.nodes.filterIsInstance<ResourceCleanupNode>().filter { reachable(failure, it) }
        assertEquals(listOf(first), remaining.map { it.fir })
        assertTrue(reachable(failure, caught))
        val dispatches = graph.nodes.filterIsInstance<ExceptionDispatchNode>().filter { reachable(failure, it) }
        assertTrue(dispatches.all { it.transfer.sourceNode === firstCleanup })
    }

    @Test
    fun graphQueueCompletesTerminalNothingBeforeEachGraphExitIsAppended() {
        for (kind in listOf("function", "lambda", "field", "default", "fragment")) {
            val f = Fixture()
            val lambda = f.lambda()
            when (kind) {
                "function" -> f.builder.enterFunction(f.owner)
                "lambda" -> f.builder.enterAnonymousFunction(lambda)
                "field" -> f.builder.enterFieldInitializer(f.field())
                "default" -> f.builder.enterDefaultArguments(f.parameter())
                "fragment" -> f.builder.enterCodeFragment(f.fragment())
            }
            val call = f.call(completed = false)
            call.fir.replaceConeTypeOrNull(ConePrimitiveType.NOTHING)
            val graph = when (kind) {
                "function" -> f.builder.exitFunction(f.owner).second
                "lambda" -> f.builder.exitAnonymousFunction(lambda).third
                "field" -> f.builder.exitFieldInitializer().second
                "default" -> f.builder.exitDefaultArguments().second
                else -> f.builder.exitCodeFragment().second
            }
            assertTrue(graph.exitNode.isDead, kind)
            assertFalse(reachable(call, graph.exitNode, normalOnly = true), kind)
            assertTrue(reachable(call, checkNotNull(graph.exceptionExitNode)), kind)
            val stub = call.followingNodes.filterIsInstance<StubNode>().single()
            assertTrue(stub.isDead)
            assertSame(stub, graph.exitNode.previousNodes.single())
            assertTrue(call.edgeTo(stub).kind.isDead)
            assertEquals(1, graph.nodes.filterIsInstance<FunctionCallExitNode>().count { it.fir === call.fir })
        }
    }

    @Test
    fun snapshotKeepsNestedGraphQueuesAndTheirTerminalStubsIndependent() {
        val f = Fixture()
        f.builder.enterFunction(f.owner)
        val parentCall = f.call(completed = false)
        val child = f.lambda()
        f.builder.enterAnonymousFunction(child)
        val childCall = f.call(completed = false)
        val originalChildGraph = f.builder.currentGraph
        val copier = ControlFlowGraphCopier()
        val resumed = f.builder.createSnapshot(copier)
        val copiedParentCall = copier[parentCall]
        val copiedChildCall = copier[childCall]
        copier.finish()
        f.builder = resumed
        childCall.fir.replaceConeTypeOrNull(ConePrimitiveType.NOTHING)
        val childGraph = f.builder.exitAnonymousFunction(child).third
        assertNotSame(originalChildGraph, childGraph)
        assertFalse(reachable(copiedChildCall, childGraph.exitNode, normalOnly = true))
        assertTrue(reachable(copiedChildCall, checkNotNull(childGraph.exceptionExitNode)))
        assertTrue(childGraph.exitNode.followingNodes.any { it === childGraph.enterNode && childGraph.exitNode.edgeTo(it).kind.isBack })
        assertTrue(copiedParentCall.followingNodes.none { it is StubNode })
        parentCall.fir.replaceConeTypeOrNull(ConePrimitiveType.NOTHING)
        val parentGraph = f.builder.exitFunction(f.owner).second
        assertFalse(reachable(copiedParentCall, parentGraph.exitNode, normalOnly = true))
        assertTrue(reachable(copiedParentCall, checkNotNull(parentGraph.exceptionExitNode)))
        assertTrue(parentGraph.nodes.none { it === parentCall || it === childCall })
    }

    @Test
    fun independentCalledInPlaceAnalysisDoesNotConsumeItsParentsFinallyFrame() {
        val f = Fixture()
        val t = f.tryExpression(finallyBlock = buildBlock {})
        val child = f.lambda()
        f.start()
        f.builder.enterTryExpression(t)
        f.builder.enterBlock(t.tryBlock)
        f.builder.exitBlock(t.tryBlock)
        f.builder.exitTryMainBlock()
        var childGraph: ControlFlowGraph? = null
        f.finallyBody(t) {
            f.builder.enterAnonymousFunction(child, calledInPlace = true)
            f.builder.enterBlock(checkNotNull(child.body))
            f.builder.exitThrowExceptionNode(f.thrown)
            f.builder.exitBlock(checkNotNull(child.body))
            childGraph = f.builder.exitAnonymousFunction(child).third
        }
        f.builder.exitTryExpression(true)
        f.finish()
        val graph = checkNotNull(childGraph)
        child.replaceControlFlowGraphReference(org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl(graph))
        val paths = ControlFlowGraphCompletionTraversal(graph).collect(Unit, { _, state -> state }, { _, _ -> Unit }).keys
        assertTrue(paths.all { it.node.owner === graph && it.pending.isEmpty() })
        assertTrue(checkNotNull(f.builder.returnExpressionsOfAnonymousFunction(child)).isEmpty())
    }

    @Test
    fun calledInPlaceLocalReturnIsConsumedBeforeTheParentsImplicitTail() {
        val f = Fixture()
        val child = f.lambda()
        val parentGraph = ControlFlowGraph(f.owner, "parent", ControlFlowGraph.Kind.Function)
        val childGraph = ControlFlowGraph(child, "child", ControlFlowGraph.Kind.AnonymousFunctionCalledInPlace)
        val enter = FunctionEnterNode(parentGraph, f.owner, 1)
        val exit = FunctionExitNode(parentGraph, f.owner, 1)
        parentGraph.enterNode = enter; parentGraph.exitNode = exit
        val childEnter = FunctionEnterNode(childGraph, child, 2)
        val childExit = FunctionExitNode(childGraph, child, 2)
        childGraph.enterNode = childEnter; childGraph.exitNode = childExit
        val target = org.cangnova.cangjie.cfir.CfirFunctionTarget(isLambda = true).also { it.bind(child) }
        val statement = buildReturnExpression {
            this.target = target
            result = buildBlock { coneTypeOrNull = ConePrimitiveType.UNIT }
            coneTypeOrNull = ConePrimitiveType.NOTHING
        }
        val jump = JumpNode(childGraph, statement, 2)
        val tailValue = buildBlock { coneTypeOrNull = f.classType("Box2") }
        f.body.replaceStatements(listOf(tailValue))
        val tail = BlockExitNode(parentGraph, f.body, 1)
        fun edge(a: CFGNode<*>, b: CFGNode<*>, kind: EdgeKind = EdgeKind.Forward) {
            CFGNode.addEdge(a, b, kind, propagateDeadness = false)
        }
        edge(enter, childEnter)
        // 结构排序仍保留父图顺序死边；实际成功流必须来自 called-in-place 子图返回。
        edge(enter, tail, EdgeKind.DeadForward)
        edge(childEnter, jump)
        edge(jump, childExit)
        edge(childExit, tail)
        edge(childExit, childEnter, EdgeKind.CfgBackward)
        edge(tail, exit)
        childGraph.complete(); parentGraph.complete()
        val traversal = ControlFlowGraphCompletionTraversal(parentGraph)
        val paths = traversal.collect(Unit, { _, state -> state }, { _, _ -> Unit }).keys
        assertTrue(paths.any { it.node === childEnter })
        assertTrue(paths.filter { it.node === tail }.all { it.abruptJump == null })
        assertEquals(setOf(tail), traversal.returnPredecessors())
        f.owner.replaceControlFlowGraphReference(org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl(parentGraph))
        assertSame(tailValue, checkNotNull(f.builder.returnExpressionsOfFunction(f.owner)).single())
        assertTrue(childExit.edgeTo(childEnter).kind.isBack)
    }

    @Test
    fun sameTargetReturnInInnerFinallyReplacesTheValueThroughOuterFinally() {
        val f = Fixture()
        val lambda = f.lambda()
        val outer = f.tryExpression(finallyBlock = buildBlock {})
        val inner = f.tryExpression(finallyBlock = buildBlock {})
        val target = org.cangnova.cangjie.cfir.CfirFunctionTarget(isLambda = true).also { it.bind(lambda) }
        val oldValue = buildBlock { coneTypeOrNull = f.classType("SuppressedBox") }
        val newValue = buildBlock { coneTypeOrNull = f.classType("ReplacementBox") }
        val oldReturn = buildReturnExpression { this.target = target; result = oldValue; coneTypeOrNull = ConePrimitiveType.NOTHING }
        val newReturn = buildReturnExpression { this.target = target; result = newValue; coneTypeOrNull = ConePrimitiveType.NOTHING }
        inner.tryBlock.replaceStatements(listOf(oldReturn))
        checkNotNull(inner.finallyBlock).replaceStatements(listOf(newReturn))
        outer.tryBlock.replaceStatements(listOf(inner))
        checkNotNull(lambda.body).replaceStatements(listOf(outer))
        f.builder.enterAnonymousFunction(lambda)
        f.builder.enterBlock(checkNotNull(lambda.body))
        f.builder.enterTryExpression(outer)
        f.builder.enterBlock(outer.tryBlock)
        f.builder.enterTryExpression(inner)
        f.builder.enterBlock(inner.tryBlock)
        f.builder.enterJump(oldReturn); f.builder.exitJump(oldReturn)
        f.builder.exitBlock(inner.tryBlock); f.builder.exitTryMainBlock()
        f.finallyBody(inner) { f.builder.enterJump(newReturn); f.builder.exitJump(newReturn) }
        f.builder.exitTryExpression(true)
        f.builder.exitBlock(outer.tryBlock); f.builder.exitTryMainBlock()
        f.finallyBody(outer); f.builder.exitTryExpression(true)
        f.builder.exitBlock(checkNotNull(lambda.body))
        val graph = f.builder.exitAnonymousFunction(lambda).third
        lambda.replaceControlFlowGraphReference(org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl(graph))
        val info = checkNotNull(f.builder.returnExpressionsOfAnonymousFunction(lambda)).single()
        assertSame(newValue, info.expression)
        assertSame(newReturn, info.containingStatement)
        assertSame(newValue, checkNotNull(f.builder.returnExpressionsOfFunction(lambda)).single())
    }

    @Test
    fun originalJvmEntryPointSignaturesRemainAvailable() {
        assertNotNull(ControlFlowGraphBuilder::class.java.getDeclaredMethod("enterCatchClause", CfirCatch::class.java))
        assertNotNull(ControlFlowGraphBuilder::class.java.getDeclaredMethod("enterAnonymousFunction", CfirAnonymousFunction::class.java))
        assertNotNull(org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzer::class.java
            .getDeclaredMethod("enterFunction", CfirFunction::class.java))
    }

    @Test
    fun exceptionExitWithOnlyDeadTransfersIsDeadWhenItsGraphCompletes() {
        val f = Fixture()
        val target = org.cangnova.cangjie.cfir.CfirFunctionTarget(isLambda = false).also { it.bind(f.owner) }
        val statement = buildReturnExpression {
            this.target = target
            result = buildBlock { coneTypeOrNull = ConePrimitiveType.UNIT }
            coneTypeOrNull = ConePrimitiveType.NOTHING
        }
        f.body.replaceStatements(listOf(statement, f.thrown))
        f.start()
        f.builder.enterJump(statement)
        f.builder.exitJump(statement)
        val deadThrow = f.builder.exitThrowExceptionNode(f.thrown)
        val graph = f.finish()
        val exceptionExit = checkNotNull(graph.exceptionExitNode)
        assertTrue(deadThrow.isDead)
        assertTrue(exceptionExit.previousNodes.isNotEmpty())
        assertTrue(exceptionExit.previousNodes.all { exceptionExit.edgeFrom(it).kind.isDead })
        assertTrue(exceptionExit.isDead)
        assertFalse(graph.exitNode.isDead)
        assertFalse(reachable(graph.enterNode, exceptionExit))
    }

    @Test
    fun anEarlyDeadCatchExceptionCannotPoisonALaterLiveExceptionExitInput() {
        val f = Fixture()
        val t = f.tryExpression(f.catch(f.otherType), f.catch(f.exceptionType))
        f.start()
        f.builder.enterTryExpression(t)
        f.builder.enterBlock(t.tryBlock)
        f.builder.exitThrowExceptionNode(f.thrown)
        f.builder.exitBlock(t.tryBlock)
        f.builder.exitTryMainBlock()
        var deadThrow: ThrowExceptionNode? = null
        val deadCatch = f.catchBody(t.catches[0]) { deadThrow = f.builder.exitThrowExceptionNode(f.throwExpression()) }
        val exitBeforeLiveInput = checkNotNull(f.builder.currentGraph.exceptionExitNode)
        var liveThrow: ThrowExceptionNode? = null
        f.catchBody(t.catches[1]) { liveThrow = f.builder.exitThrowExceptionNode(f.throwExpression()) }
        f.builder.exitTryExpression(true)
        val graph = f.finish()
        val exceptionExit = checkNotNull(graph.exceptionExitNode)
        assertSame(exitBeforeLiveInput, exceptionExit)
        assertTrue(deadCatch.isDead)
        assertTrue(checkNotNull(deadThrow).isDead)
        assertFalse(checkNotNull(liveThrow).isDead)
        assertTrue(exceptionExit.previousNodes.any { exceptionExit.edgeFrom(it).kind.isDead })
        assertTrue(exceptionExit.previousNodes.any { !exceptionExit.edgeFrom(it).kind.isDead && exceptionExit.edgeFrom(it).kind.usedInCfa })
        assertFalse(exceptionExit.isDead)
        assertTrue(reachable(checkNotNull(liveThrow), exceptionExit))
        assertTrue(reachable(graph.enterNode, exceptionExit))
    }

    @Test
    fun calledInPlaceUncaughtThrowKeepsEveryExecutionBoundaryStructurallyComplete() {
        for (kind in listOf("function", "lambda", "field", "default", "fragment")) {
            val f = Fixture()
            val enclosingLambda = f.lambda()
            when (kind) {
                "function" -> f.builder.enterFunction(f.owner)
                "lambda" -> f.builder.enterAnonymousFunction(enclosingLambda)
                "field" -> f.builder.enterFieldInitializer(f.field())
                "default" -> f.builder.enterDefaultArguments(f.parameter())
                "fragment" -> f.builder.enterCodeFragment(f.fragment())
            }
            val activeBoundary = f.builder.currentGraph
            val child = f.lambda()
            f.builder.enterAnonymousFunction(child, calledInPlace = true)
            f.builder.enterBlock(checkNotNull(child.body))
            val thrown = f.builder.exitThrowExceptionNode(f.thrown)
            f.builder.exitBlock(checkNotNull(child.body))
            val childGraph = f.builder.exitAnonymousFunction(child).third
            val boundary = when (kind) {
                "function" -> f.builder.exitFunction(f.owner).second
                "lambda" -> f.builder.exitAnonymousFunction(enclosingLambda).third
                "field" -> f.builder.exitFieldInitializer().second
                "default" -> f.builder.exitDefaultArguments().second
                else -> f.builder.exitCodeFragment().second
            }
            val exceptionalExit = checkNotNull(boundary.exceptionExitNode)
            assertSame(activeBoundary, boundary, kind)
            assertSame(boundary, exceptionalExit.owner, kind)
            assertNull(childGraph.exceptionExitNode, kind)
            assertEquals(boundary.nodeCount, boundary.nodes.size, kind)
            assertEquals(childGraph.nodeCount, childGraph.nodes.size, kind)
            assertTrue(boundary.nodes.all { it.owner === boundary }, kind)
            assertTrue(childGraph.nodes.none { it === exceptionalExit }, kind)
            val localInput = exceptionalExit.previousNodes.filter { it.owner === boundary }.single()
            assertSame(boundary.enterNode, localInput, kind)
            assertFalse(localInput.edgeTo(exceptionalExit).kind.usedInCfa, kind)
            assertTrue(localInput.edgeTo(exceptionalExit).kind.isDead, kind)
            assertFalse(localInput.edgeTo(exceptionalExit).kind.usedInDfa, kind)
            assertSame(UncaughtExceptionPath, localInput.edgeTo(exceptionalExit).label, kind)
            val transfer = exceptionalExit.previousNodes.filterIsInstance<ExceptionTransferNode>().single()
            assertSame(childGraph, transfer.owner, kind)
            assertSame(thrown, transfer.sourceNode, kind)
            assertTrue(reachable(thrown, exceptionalExit), kind)
            assertFalse(exceptionalExit.isDead, kind)
            assertTrue(ControlFlowGraphCompletionPath(boundary.enterNode).successors().none {
                it.node === exceptionalExit
            }, kind)
            assertTrue(childGraph.exitNode.followingNodes.any {
                it === childGraph.enterNode && childGraph.exitNode.edgeTo(it).kind.isBack
            }, kind)
        }
    }

    @Test
    fun calledInPlaceDeadThrowCannotMakeItsBoundaryExceptionExitLive() {
        val f = Fixture()
        val child = f.lambda()
        val target = org.cangnova.cangjie.cfir.CfirFunctionTarget(isLambda = true).also { it.bind(child) }
        val statement = buildReturnExpression {
            this.target = target
            result = buildBlock { coneTypeOrNull = ConePrimitiveType.UNIT }
            coneTypeOrNull = ConePrimitiveType.NOTHING
        }
        checkNotNull(child.body).replaceStatements(listOf(statement, f.thrown))
        f.start()
        f.builder.enterAnonymousFunction(child, calledInPlace = true)
        f.builder.enterBlock(checkNotNull(child.body))
        f.builder.enterJump(statement)
        f.builder.exitJump(statement)
        val deadThrow = f.builder.exitThrowExceptionNode(f.thrown)
        f.builder.exitBlock(checkNotNull(child.body))
        val childGraph = f.builder.exitAnonymousFunction(child).third
        val boundary = f.finish()
        val exceptionalExit = checkNotNull(boundary.exceptionExitNode)
        assertTrue(deadThrow.isDead)
        assertFalse(childGraph.exitNode.isDead)
        assertTrue(exceptionalExit.previousNodes.any { it.owner === childGraph && exceptionalExit.edgeFrom(it).kind.usedInCfa })
        assertTrue(exceptionalExit.previousNodes.filter { exceptionalExit.edgeFrom(it).kind.usedInCfa }.all {
            exceptionalExit.edgeFrom(it).kind.isDead
        })
        assertTrue(exceptionalExit.previousNodes.all { exceptionalExit.edgeFrom(it).kind.isDead })
        assertTrue(exceptionalExit.isDead)
        assertFalse(boundary.exitNode.isDead)
        assertFalse(reachable(deadThrow, exceptionalExit))
        assertEquals(boundary.nodeCount, boundary.nodes.size)
        assertTrue(ControlFlowGraphCompletionPath(boundary.enterNode).successors().none {
            it.node === exceptionalExit
        })
    }

    @Test
    fun activeSnapshotKeepsCalledInPlaceExceptionExitAndExternalFinallyFrameInTheirRealOwners() {
        val f = Fixture()
        val t = f.tryExpression(finallyBlock = buildBlock {})
        val child = f.lambda()
        checkNotNull(child.body).replaceStatements(listOf(f.thrown))
        f.start()
        val originalBoundary = f.builder.currentGraph
        f.builder.enterTryExpression(t)
        f.builder.enterBlock(t.tryBlock)
        f.builder.exitBlock(t.tryBlock)
        f.builder.exitTryMainBlock()
        val originalFinally = f.builder.enterFinallyBlock()
        f.builder.enterBlock(checkNotNull(t.finallyBlock))
        f.builder.enterAnonymousFunction(child, calledInPlace = true)
        val originalChildGraph = f.builder.currentGraph
        f.builder.enterBlock(checkNotNull(child.body))
        val originalThrow = f.builder.exitThrowExceptionNode(f.thrown)
        val originalExceptionalExit = checkNotNull(originalBoundary.exceptionExitNode)
        val copier = ControlFlowGraphCopier()
        val resumed = f.builder.createSnapshot(copier)
        val copiedBoundary = copier[originalBoundary]
        val copiedFinally = copier[originalFinally]
        val copiedThrow = copier[originalThrow]
        val copiedExceptionalExit = copier[originalExceptionalExit]
        copier.finish()
        f.builder = resumed
        f.builder.exitBlock(checkNotNull(child.body))
        val childGraph = f.builder.exitAnonymousFunction(child).third
        f.builder.exitBlock(checkNotNull(t.finallyBlock))
        f.builder.exitFinallyBlock()
        f.builder.exitTryExpression(true)
        val boundary = f.finish()
        assertSame(copiedBoundary, boundary)
        assertSame(copiedExceptionalExit, boundary.exceptionExitNode)
        assertSame(boundary, copiedExceptionalExit.owner)
        assertNotSame(originalBoundary, boundary)
        assertNotSame(originalChildGraph, childGraph)
        assertEquals(boundary.nodeCount, boundary.nodes.size)
        assertEquals(childGraph.nodeCount, childGraph.nodes.size)
        assertFalse(boundary.enterNode.edgeTo(copiedExceptionalExit).kind.usedInCfa)
        assertTrue(boundary.enterNode.edgeTo(copiedExceptionalExit).kind.isDead)
        assertFalse(boundary.enterNode.edgeTo(copiedExceptionalExit).kind.usedInDfa)
        assertTrue(reachable(copiedThrow, copiedExceptionalExit))
        assertTrue(childGraph.nodes.any { node ->
            node.followingNodes.any { node.edgeTo(it).label === copiedFinally }
        })
        for (node in boundary.nodes + childGraph.nodes) {
            for (related in node.previousNodes + node.followingNodes) {
                assertTrue(related.owner === boundary || related.owner === childGraph)
            }
            for (next in node.followingNodes) {
                val label = node.edgeTo(next).label
                if (label is CFGNode<*>) assertTrue(label.owner === boundary || label.owner === childGraph)
            }
            if (node is ExceptionTransferNode) assertSame(copiedThrow, node.sourceNode)
        }
        child.replaceControlFlowGraphReference(org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl(childGraph))
        val paths = ControlFlowGraphCompletionTraversal(childGraph).collect(Unit, { _, state -> state }, { _, _ -> Unit }).keys
        assertTrue(paths.all { it.node.owner === childGraph && it.pending.isEmpty() })
        assertTrue(checkNotNull(f.builder.returnExpressionsOfAnonymousFunction(child)).isEmpty())
    }

    @Test
    fun calledInPlacePostponedArgumentRetainsItsActualRuntimeEntryAndUncaughtTransfer() {
        val f = Fixture()
        val functionType = ConeFunctionType(emptyList(), ConePrimitiveType.UNIT)
        val child = f.lambda().also {
            it.replaceTypeRef(functionType.toCfirResolvedTypeRef())
            checkNotNull(it.body).replaceStatements(listOf(f.thrown))
        }
        val expression = buildAnonymousFunctionExpression {
            source = checkNotNull(child.source)
            anonymousFunction = child
            isTrailingLambda = false
        }
        val symbol = CallResolutionTestFixtures.buildFunctionSymbol(
            f.session, "invokeInPlace", ConePrimitiveType.UNIT, parameterTypes = listOf(functionType),
        )
        val call = buildFunctionCall {
            coneTypeOrNull = ConePrimitiveType.UNIT
            calleeReference = buildResolvedNamedReference { name = symbol.cfir.name; resolvedSymbol = symbol }
            argumentList = buildArgumentList { arguments.add(expression) }
        }
        f.body.replaceStatements(listOf(call))
        f.start()
        f.builder.enterCallArguments(call, listOf(child))
        f.builder.enterAnonymousFunctionExpression(expression)
        val childEnter = f.builder.enterAnonymousFunction(child, calledInPlace = true)
        f.builder.enterBlock(checkNotNull(child.body))
        val thrown = f.builder.exitThrowExceptionNode(f.thrown)
        f.builder.exitBlock(checkNotNull(child.body))
        val (childExit, postponedExit, childGraph) = f.builder.exitAnonymousFunction(child)
        child.replaceControlFlowGraphReference(org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl(childGraph))
        f.builder.exitCallArguments()
        f.builder.enterFunctionCall(call)
        val callExit = f.builder.exitFunctionCall(call, true)
        val boundary = f.finish()
        val exceptionalExit = checkNotNull(boundary.exceptionExitNode)
        val paths = ControlFlowGraphCompletionTraversal(boundary).collect(Unit, { _, state -> state }, { _, _ -> Unit }).keys
        assertTrue(paths.any { it.node === childEnter })
        assertTrue(paths.any { it.node === thrown })
        assertTrue(paths.any { it.node === exceptionalExit })
        assertTrue(reachable(thrown, exceptionalExit))
        assertFalse(reachable(thrown, boundary.exitNode, normalOnly = true))
        assertNull(childGraph.exceptionExitNode)
        assertNotNull(postponedExit)
        assertTrue(childExit.followingNodes.any { it === postponedExit })
        assertSame(call, callExit.fir)
        assertEquals(1, boundary.nodes.filterIsInstance<FunctionCallExitNode>().count { it.fir === call })
        assertEquals(boundary.nodeCount, boundary.nodes.size)
        assertTrue(boundary.enterNode.edgeTo(exceptionalExit).kind.isDead)
        assertFalse(boundary.enterNode.edgeTo(exceptionalExit).kind.usedInDfa)
        assertTrue(ControlFlowGraphCompletionPath(boundary.enterNode).successors().none {
            it.node === exceptionalExit
        })
        assertTrue(ControlFlowGraphCompletionPath(boundary.enterNode).successors(includeDead = true).none {
            it.node === exceptionalExit
        })
    }

    private inner class Fixture {
        val session = CallResolutionTestFixtures.newTestSession()
        var builder = ControlFlowGraphBuilder()
        // 两个合法异常类在受控类型树中互为兄弟，均继承 std.core.Exception。
        val baseExceptionType = ConeClassLikeType(ConeClassLikeLookupTagImpl(ClassId.topLevel(FqName("std.core.Exception"))))
        val exceptionType = classType("LeafException")
        val otherType = classType("SiblingException")
        val body = buildBlock {}
        val owner = CallResolutionTestFixtures.buildFunctionSymbol(session, "owner", ConePrimitiveType.UNIT).cfir.also { it.replaceBody(body) }
        private val exceptionProducer = CallResolutionTestFixtures.buildFunctionSymbol(session, "produceException", exceptionType)
        val thrown = throwExpression()
        fun classType(name: String) = ConeClassLikeType(ConeClassLikeLookupTagImpl(ClassId.topLevel(FqName("test." + name))))
        fun throwExpression() = buildThrowExpression {
            exception = buildFunctionCall {
                coneTypeOrNull = exceptionType
                calleeReference = buildResolvedNamedReference {
                    name = exceptionProducer.cfir.name
                    resolvedSymbol = exceptionProducer
                }
            }
            coneTypeOrNull = ConePrimitiveType.NOTHING
        }
        fun catch(vararg types: ConeCangJieType): CfirCatch = buildCatch {
            pattern = buildCatchPattern {
                isWildcard = types.isEmpty()
                typeRefs += types.map { it.toCfirResolvedTypeRef() }
                resolvedTypeRef = (if (types.size == 1) types.single() else baseExceptionType).toCfirResolvedTypeRef()
            }
            body = buildBlock {}
        }
        fun tryExpression(vararg clauses: CfirCatch, finallyBlock: CfirBlock? = null): CfirTryExpression = buildTryExpression {
            tryBlock = buildBlock {}
            catches += clauses
            this.finallyBlock = finallyBlock
        }
        fun start() { builder.enterFunction(owner); builder.enterBlock(body) }
        fun finish(): ControlFlowGraph { builder.exitBlock(body); return builder.exitFunction(owner).second }
        fun call(returnType: ConeCangJieType = ConePrimitiveType.UNIT, completed: Boolean = true): FunctionCallExitNode {
            val symbol = CallResolutionTestFixtures.buildFunctionSymbol(session, "invoke", returnType)
            val call = buildFunctionCall {
                coneTypeOrNull = returnType
                calleeReference = buildResolvedNamedReference { name = Name.identifier("invoke"); resolvedSymbol = symbol }
            }
            builder.enterCallArguments(call, emptyList())
            builder.exitCallArguments()
            builder.enterFunctionCall(call)
            return builder.exitFunctionCall(call, completed)
        }
        fun catchBody(clause: CfirCatch, action: () -> Unit = {}): CatchClauseEnterNode {
            val entry = builder.enterCatchClause(clause)
            builder.refineExceptionDispatch(entry) { a, b ->
                a == b || b == baseExceptionType && (a == exceptionType || a == otherType)
            }
            builder.enterBlock(clause.body)
            action()
            builder.exitBlock(clause.body)
            builder.exitCatchClause(clause)
            return entry
        }
        fun finallyBody(t: CfirTryExpression, action: () -> Unit = {}): FinallyBlockEnterNode {
            val enter = builder.enterFinallyBlock()
            builder.enterBlock(checkNotNull(t.finallyBlock))
            action()
            builder.exitBlock(checkNotNull(t.finallyBlock))
            builder.exitFinallyBlock()
            return enter
        }
        fun lambda(): CfirAnonymousFunction {
            val parsed = sourceElements.lambda()
            return buildAnonymousFunction {
                source = parsed.declaration
                moduleData = session.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = true
                status = CfirDeclarationStatusImpl()
                returnTypeRef = ConePrimitiveType.UNIT.toCfirResolvedTypeRef()
                typeRef = ConePrimitiveType.UNIT.toCfirResolvedTypeRef()
                symbol = CfirAnonymousFunctionSymbol()
                hasExplicitParameterList = true
                isLambda = true
                body = buildBlock { source = parsed.body }
            }
        }
        fun field(): CfirFieldVariable = buildFieldVariable {
            source = sourceElements.field()
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            status = CfirDeclarationStatusImpl()
            isVar = false
            returnTypeRef = ConePrimitiveType.UNIT.toCfirResolvedTypeRef()
            name = Name.identifier("field")
            symbol = CfirFieldVariableSymbol(CallableId(FqName("test"), name))
        }
        fun parameter(): CfirValueParameter = buildValueParameter {
            source = sourceElements.parameter()
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            isNamed = false
            returnTypeRef = ConePrimitiveType.UNIT.toCfirResolvedTypeRef()
            name = Name.identifier("parameter")
            symbol = CfirValueParameterSymbol(CallableId(name))
            containingDeclarationSymbol = owner.symbol
        }
        fun fragment(): CfirCodeFragment {
            val parsed = sourceElements.fragment()
            return buildCodeFragment {
                source = parsed.declaration
                moduleData = session.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                symbol = CfirCodeFragmentSymbol()
                block = buildBlock { source = parsed.body }
            }
        }
        fun resource(resourceName: String): CfirResourceVariable {
            val parsed = sourceElements.resource(resourceName)
            return buildResourceVariable {
                source = parsed.declaration
                moduleData = session.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = true
                status = CfirDeclarationStatusImpl()
                initializer = buildBlock { source = parsed.initializer; coneTypeOrNull = classType("Resource") }
                isTypeImplicit = false
                isVar = false
                symbol = CfirResourceVariableSymbol(CallableId(Name.identifier(resourceName)))
                returnTypeRef = classType("Resource").toCfirResolvedTypeRef()
                name = Name.identifier(resourceName)
            }
        }
    }

    private fun reachable(
        start: CFGNode<*>, target: CFGNode<*>, normalOnly: Boolean = false, blocked: Set<CFGNode<*>> = emptySet(),
    ): Boolean {
        val queue = ArrayDeque<CFGNode<*>>()
        val seen = mutableSetOf<CFGNode<*>>()
        queue.add(start)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node in blocked || !seen.add(node)) continue
            if (node === target) return true
            node.followingNodes.filterTo(queue) {
                val edge = node.edgeTo(it)
                edge.kind.usedInCfa && !edge.kind.isDead && (!normalOnly || edge.label === NormalPath)
            }
        }
        return false
    }

    private fun completionPaths(
        start: CFGNode<*>, target: CFGNode<*>, pending: List<ControlFlowGraphCompletionPath.PendingFinally> = emptyList(),
    ): List<ControlFlowGraphCompletionPath> {
        val queue = ArrayDeque<ControlFlowGraphCompletionPath>()
        val seen = mutableSetOf<ControlFlowGraphCompletionPath>()
        queue.add(ControlFlowGraphCompletionPath(start, pending))
        while (queue.isNotEmpty()) {
            val path = queue.removeFirst()
            if (!seen.add(path)) continue
            if (path.node !== target) queue.addAll(path.successors())
        }
        return seen.filter { it.node === target }
    }

    private fun innerGraphFinally(node: CFGNode<*>): FinallyBlockEnterNode =
        node.owner.nodes.filterIsInstance<FinallyBlockEnterNode>().single()
}

@file:OptIn(org.cangnova.cangjie.cfir.resolve.dfa.cfg.CfgInternals::class)

package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildFunctionCall
import org.cangnova.cangjie.cfir.expressions.builder.buildFunctionCallCopy
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.CfirResolutionSnapshot
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.CFGNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraph
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.RevisableFunctionCallNode
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** 正式 AST 的类型重放、完整回滚与实际 CFG 索引必须遵循同一身份代次。 */
class CfirFormalCallCfgRollbackTest {
    @Test
    fun typeReplayKeepsTheCurrentAstAndCallNodeIdentity() {
        val f = Fixture()
        val before = CfirResolutionSnapshot.capture(f.owner)
        f.replace(f.replacement)
        before.restoreTypeResolutionState()
        assertSame(f.replacement, f.body.statements.single())
        assertTrue(f.callNodes().all { it.fir === f.replacement })
        val next = buildFunctionCallCopy(f.replacement) {}
        f.builder.updateFunctionCallNodes(f.replacement, next, checkNotNull(f.argumentsEnter).owner)
        assertTrue(f.callNodes().all { it.fir === next })
    }

    @Test
    fun withdrawnAliasCannotTargetTheRestoredCallAsAnIdempotentUpdate() {
        val f = Fixture()
        val before = CfirResolutionSnapshot.capture(f.owner)
        f.replace(f.replacement)
        before.restore()
        assertThrows(IllegalStateException::class.java) {
            f.builder.updateFunctionCallNodes(f.replacement, f.original, checkNotNull(f.argumentsEnter).owner)
        }
        assertTrue(f.callNodes().all { it.fir === f.original })
        assertSame(f.original, f.body.statements.single())
    }

    @Test
    fun fullRollbackRemovesNewAliasKeysAndSelectedReplayRestoresItsExactKeys() {
        val f = Fixture()
        val before = CfirResolutionSnapshot.capture(f.owner)
        f.replace(f.replacement)
        val selected = CfirResolutionSnapshot.capture(f.owner)
        val next = buildFunctionCallCopy(f.replacement) {}
        f.builder.updateFunctionCallNodes(f.replacement, next, checkNotNull(f.argumentsEnter).owner)
        f.body.replaceStatements(listOf(next))
        before.restore()
        assertEquals("", f.builder.describeFunctionCallNodes(f.replacement))
        assertEquals("", f.builder.describeFunctionCallNodes(next))
        selected.restore()
        assertTrue(f.callNodes().all { it.fir === f.replacement })
        assertSame(f.replacement, f.body.statements.single())
        assertNotEquals("", f.builder.describeFunctionCallNodes(f.replacement))
        assertEquals("", f.builder.describeFunctionCallNodes(next))
        assertThrows(IllegalStateException::class.java) {
            f.builder.updateFunctionCallNodes(next, f.replacement, checkNotNull(f.argumentsEnter).owner)
        }
        f.builder.updateFunctionCallNodes(f.replacement, next, checkNotNull(f.argumentsEnter).owner)
        assertTrue(f.callNodes().all { it.fir === next })
    }

    @Test
    fun argumentExitCreatedBeforeAttachmentIsPartOfTheActualReplacement() {
        val f = Fixture(finish = false)
        f.builder.updateFunctionCallNodes(f.original, f.replacement, checkNotNull(f.argumentsEnter).owner)
        val (split, exit) = f.builder.exitCallArguments()
        assertNull(split)
        assertSame(f.replacement, checkNotNull(exit).fir)
        assertSame(f.replacement, checkNotNull(f.argumentsEnter).fir)
        f.finishAfterArguments(f.replacement)
        assertEquals(4, f.callNodes().size)
        assertTrue(f.callNodes().all { it.fir === f.replacement })
    }

    @Test
    fun resetRevokesBothRegisteredAndWithdrawnIndexState() {
        val f = Fixture()
        val before = CfirResolutionSnapshot.capture(f.owner)
        f.replace(f.replacement)
        before.restore()
        f.builder.reset()
        assertEquals("", f.builder.describeFunctionCallNodes(f.original))
        assertEquals("", f.builder.describeFunctionCallNodes(f.replacement))
        val next = buildFunctionCallCopy(f.original) {}
        assertThrows(IllegalStateException::class.java) {
            f.builder.updateFunctionCallNodes(f.original, next, checkNotNull(f.argumentsEnter).owner)
        }
        assertTrue(f.callNodes().all { it.fir === f.original })
    }

    @Test
    fun resetContextRejectsCfgWritesWhileAstMementoKeepsItsActualGraphOwner() {
        val f = Fixture()
        val before = CfirResolutionSnapshot.capture(f.owner)
        f.replace(f.replacement)
        f.builder.reset()
        assertThrows(IllegalStateException::class.java) {
            f.builder.updateFunctionCallNodes(f.original, f.replacement, f.graph)
        }
        assertSame(f.replacement, f.body.statements.single())
        assertTrue(f.callNodes().all { it.fir === f.replacement })
        before.restore()
        assertSame(f.original, f.body.statements.single())
        assertTrue(f.callNodes().all { it.fir === f.original })
        assertEquals("", f.builder.describeFunctionCallNodes(f.original))
    }

    @Test
    fun anotherBuilderCannotAdoptAParentNodeWithoutCopyingIt() {
        val f = Fixture()
        val other = org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraphBuilder()
        assertThrows(IllegalStateException::class.java) {
            other.registerFunctionCallNode(checkNotNull(f.argumentsEnter))
        }
        assertEquals("", other.describeFunctionCallNodes(f.original))
        f.replace(f.replacement)
        assertTrue(f.callNodes().all { it.fir === f.replacement })
    }

    @Test
    fun copiedLiveAndWithdrawnAliasesAndNodeMementosAreIndependent() {
        val f = Fixture()
        val before = CfirResolutionSnapshot.capture(f.owner)
        f.replace(f.replacement)
        before.restore()
        val context = CfirDataFlowAnalyzerContext(f.builder)
        val mapper = object : SnapshotCfirMapper {
            override fun <T : org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol<*>> mapSymbol(symbol: T): T = symbol
            override fun <T : org.cangnova.cangjie.cfir.CfirElement> mapElement(element: T): T = element
        }
        val snapshot = context.createSnapshot(mapper)
        val copy = checkNotNull(snapshot.graphMapping[f.graph])
        val copiedNodes = copy.nodes.filterIsInstance<RevisableFunctionCallNode>()
        val copyStates = copiedNodes.map { it.captureFirState() }
        assertEquals(4, copiedNodes.size)
        assertThrows(IllegalStateException::class.java) {
            snapshot.context.graphBuilder.updateFunctionCallNodes(f.replacement, f.original, copy)
        }
        snapshot.context.graphBuilder.updateFunctionCallNodes(f.original, f.replacement, copy)
        assertTrue(copiedNodes.all { it.fir === f.replacement })
        assertTrue(f.callNodes().all { it.fir === f.original })
        before.restore()
        assertTrue(copiedNodes.all { it.fir === f.replacement })
        copyStates.forEach { it.restore() }
        assertTrue(copiedNodes.all { it.fir === f.original })
        assertEquals("", snapshot.context.graphBuilder.describeFunctionCallNodes(f.replacement))
        assertThrows(IllegalStateException::class.java) {
            snapshot.context.graphBuilder.updateFunctionCallNodes(f.replacement, f.original, copy)
        }
    }

    @Test
    fun freshSnapshotAfterContextResetKeepsGraphFactsWithoutReauthorizingContext() {
        val f = Fixture()
        f.builder.reset()
        val afterReset = CfirResolutionSnapshot.capture(f.owner)
        f.callNodes().forEach {
            (it as RevisableFunctionCallNode).setResolvedFunctionCall(f.original, f.replacement)
        }
        f.body.replaceStatements(listOf(f.replacement))
        afterReset.restore()
        assertSame(f.original, f.body.statements.single())
        assertTrue(f.callNodes().all { it.fir === f.original })
        assertEquals("", f.builder.describeFunctionCallNodes(f.original))
        assertEquals("", f.builder.describeFunctionCallNodes(f.replacement))
    }

    @Test
    fun dfaCopyKeepsAnUnattachedArgumentExitAndReceiverMappingIndependent() {
        val f = Fixture(finish = false)
        val context = CfirDataFlowAnalyzerContext(f.builder)
        val mapper = object : SnapshotCfirMapper {
            override fun <T : org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol<*>> mapSymbol(symbol: T): T = symbol
            override fun <T : org.cangnova.cangjie.cfir.CfirElement> mapElement(element: T): T = element
        }
        val snapshot = context.createSnapshot(mapper)
        val copy = checkNotNull(snapshot.graphMapping[checkNotNull(f.argumentsEnter).owner])
        val next = buildFunctionCallCopy(f.original) {}
        f.builder.updateFunctionCallNodes(f.original, f.replacement, checkNotNull(f.argumentsEnter).owner)
        snapshot.context.graphBuilder.updateFunctionCallNodes(f.original, next, copy)
        val copiedExit = checkNotNull(snapshot.context.graphBuilder.exitCallArguments().second)
        val originalExit = checkNotNull(f.builder.exitCallArguments().second)
        assertSame(next, copiedExit.fir)
        assertSame(next, copiedExit.explicitReceiverExitNode.fir)
        assertSame(copy, copiedExit.owner)
        assertSame(copy, copiedExit.explicitReceiverExitNode.owner)
        assertNotSame(f.argumentsEnter, copiedExit.explicitReceiverExitNode)
        assertSame(f.replacement, originalExit.fir)
        assertSame(f.replacement, checkNotNull(f.argumentsEnter).fir)
    }

    private class Fixture(finish: Boolean = true) {
        val session = CallResolutionTestFixtures.newTestSession()
        val builder = org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraphBuilder()
        private val target = CallResolutionTestFixtures.buildFunctionSymbol(session, "make", ConePrimitiveType.INT32)
        val original = buildFunctionCall {
            source = CallResolutionTestFixtures.buildTypedExpression(ConePrimitiveType.INT32).source
            coneTypeOrNull = ConePrimitiveType.INT32
            calleeReference = buildResolvedNamedReference {
                name = Name.identifier("make")
                resolvedSymbol = target
            }
        }
        val replacement = buildFunctionCallCopy(original) {}
        val body = buildBlock { statements += original }
        val owner = CallResolutionTestFixtures.buildFunctionSymbol(session, "owner", ConePrimitiveType.INT32).cfir
        lateinit var graph: ControlFlowGraph
        val argumentsEnter: org.cangnova.cangjie.cfir.resolve.dfa.cfg.FunctionCallArgumentsEnterNode?
        init {
            owner.replaceBody(body)
            builder.enterFunction(owner)
            builder.enterBlock(body)
            argumentsEnter = builder.enterCallArguments(original, emptyList())
            if (finish) {
                builder.exitCallArguments()
                finishAfterArguments(original)
            }
        }
        fun finishAfterArguments(call: CfirFunctionCall) {
            builder.enterFunctionCall(call)
            builder.exitFunctionCall(call, callCompleted = true)
            builder.exitBlock(body)
            graph = builder.exitFunction(owner).second
            owner.replaceControlFlowGraphReference(CfirControlFlowGraphReferenceImpl(graph))
        }
        fun replace(call: CfirFunctionCall) {
            builder.updateFunctionCallNodes(original, call, checkNotNull(argumentsEnter).owner)
            body.replaceStatements(listOf(call))
        }
        fun callNodes(): List<CFGNode<*>> = graph.nodes.filter { it is RevisableFunctionCallNode }
    }
}

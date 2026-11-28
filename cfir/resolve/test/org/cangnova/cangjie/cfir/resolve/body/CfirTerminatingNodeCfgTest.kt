@file:OptIn(
    org.cangnova.cangjie.cfir.resolve.dfa.cfg.CfgInternals::class,
    org.cangnova.cangjie.cfir.CfirImplementationDetail::class,
)

package org.cangnova.cangjie.cfir.resolve.body

import java.util.Collections
import java.util.IdentityHashMap
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildFunctionCall
import org.cangnova.cangjie.cfir.expressions.builder.buildThrowExpression
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.CFGNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraphBuilder
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.FunctionCallExitNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.NormalPath
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.StubNode
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** 官方 throw 的顺序终止合同；使用真实 builder/node/edge，不改变验收过滤规则。 */
class CfirTerminatingNodeCfgTest {
    @Test
    fun throwCreatesADeadSequentialContinuation() {
        val f = Fixture()
        f.builder.enterFunction(f.owner)
        f.builder.enterBlock(f.body)
        val thrown = f.builder.exitThrowExceptionNode(f.thrown)
        val stub = thrown.followingNodes.filterIsInstance<StubNode>().single()
        assertSame(f.builder.currentGraph, stub.owner)
        assertTrue(stub.isDead)
        assertTrue(thrown.edgeTo(stub).kind.isDead, edgeDescription(thrown, stub))
        assertSame(f.thrown, thrown.fir)
    }

    @Test
    fun followingBodyCallStaysOnDeadFlowAfterUnconditionalThrow() {
        val f = Fixture()
        f.builder.enterFunction(f.owner)
        f.builder.enterBlock(f.body)
        val thrown = f.builder.exitThrowExceptionNode(f.thrown)
        val following = f.appendCall()
        f.builder.exitBlock(f.body)
        val graph = f.builder.exitFunction(f.owner).second
        assertSame(f.following, following.fir)
        assertSame(f.owner, graph.declaration)
        assertTrue(graph.nodes.any { it === thrown })
        assertTrue(graph.nodes.any { it === following })
        assertFalse(reachable(thrown, following), graph.nodes.joinToString("\n") { node ->
            node.followingNodes.joinToString("\n") { next -> edgeDescription(node, next) }
        })
        assertTrue(following.isDead)
    }

    @Test
    fun aDeadContinuationRemainsDeadAcrossTheOwningBlockExit() {
        val f = Fixture()
        f.builder.enterFunction(f.owner)
        f.builder.enterBlock(f.body)
        val thrown = f.builder.exitThrowExceptionNode(f.thrown)
        val stub = thrown.followingNodes.filterIsInstance<StubNode>().single()
        val blockExit = f.builder.exitBlock(f.body)
        assertSame(f.body, blockExit.fir)
        assertSame(stub.owner, blockExit.owner)
        assertTrue(blockExit.isDead, edgeDescription(stub, blockExit))
        assertTrue(stub.edgeTo(blockExit).kind.isDead)
    }

    @Test
    fun anUntouchedLiveBodyCallRetainsItsFormalOwnerAndLivePath() {
        val f = Fixture()
        f.body.replaceStatements(listOf(f.following))
        f.builder.enterFunction(f.owner)
        f.builder.enterBlock(f.body)
        val following = f.appendCall()
        f.builder.exitBlock(f.body)
        val graph = f.builder.exitFunction(f.owner).second
        assertSame(f.owner, graph.declaration)
        assertSame(f.following, following.fir)
        assertTrue(reachable(graph.enterNode, following))
        assertTrue(graph.nodes.none { it.isDead })
    }

    private class Fixture {
        private val session = CallResolutionTestFixtures.newTestSession()
        val builder = ControlFlowGraphBuilder()
        private val exceptionType = ConeClassLikeType(
            lookupTag = ConeClassLikeLookupTagImpl(ClassId.topLevel(FqName("std.core.Exception"))),
        )
        private val exceptionProducer = CallResolutionTestFixtures.buildFunctionSymbol(session, "produceException", exceptionType)
        private val followingSymbol = CallResolutionTestFixtures.buildFunctionSymbol(session, "following", ConePrimitiveType.UNIT)
        private val exception = buildFunctionCall {
            coneTypeOrNull = exceptionType
            calleeReference = buildResolvedNamedReference {
                name = Name.identifier("produceException")
                resolvedSymbol = exceptionProducer
            }
        }
        val thrown = buildThrowExpression {
            exception = this@Fixture.exception
            coneTypeOrNull = ConePrimitiveType.NOTHING
        }
        val following = buildFunctionCall {
            coneTypeOrNull = ConePrimitiveType.UNIT
            calleeReference = buildResolvedNamedReference {
                name = Name.identifier("following")
                resolvedSymbol = followingSymbol
            }
        }
        val body = buildBlock { statements += thrown; statements += following }
        val owner = CallResolutionTestFixtures.buildFunctionSymbol(session, "owner", ConePrimitiveType.UNIT).cfir.also {
            it.replaceBody(body)
        }
        fun appendCall(): FunctionCallExitNode {
            builder.enterCallArguments(following, emptyList())
            builder.exitCallArguments()
            builder.enterFunctionCall(following)
            return builder.exitFunctionCall(following, callCompleted = true).value
        }
    }

    private fun reachable(start: CFGNode<*>, target: CFGNode<*>): Boolean {
        val seen = Collections.newSetFromMap(IdentityHashMap<CFGNode<*>, Boolean>())
        val queue = ArrayDeque<CFGNode<*>>()
        queue += start
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (!seen.add(node)) continue
            if (node === target) return true
            for (next in node.followingNodes) {
                val edge = node.edgeTo(next)
                if (edge.kind.usedInCfa && !edge.kind.isDead && edge.label === NormalPath) queue += next
            }
        }
        return false
    }

    private fun edgeDescription(from: CFGNode<*>, to: CFGNode<*>): String =
        "${from.javaClass.simpleName}@${System.identityHashCode(from).toString(16)}(dead=${from.isDead}) -> " +
            "${to.javaClass.simpleName}@${System.identityHashCode(to).toString(16)}(dead=${to.isDead}) " +
            "kind=${from.edgeTo(to).kind} normal=${from.edgeTo(to).label === NormalPath}"
}

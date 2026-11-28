@file:OptIn(org.cangnova.cangjie.cfir.resolve.dfa.cfg.CfgInternals::class,
    org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.analysis.collectors.components

import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.builder.buildConstPattern
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.*
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * 直接调用生产常量 CFA；只构造语义层真实节点，不引入 checkers → resolve 依赖。
 * 若 consumer 回到普通节点 BFS，第一项将错误报告 finally 后根本不会执行的模式。
 */
class CfirExceptionalCompletionConsumerTest {
    @Test
    fun uncaughtExceptionCannotContributeToTheNormalAfterFinallyDiagnostic() {
        val f = Fixture(boolean(true))
        val unreachable = CfirControlFlowConstAnalysis().collectUnreachablePatterns(f.graph)
        assertFalse(f.tailRejectedPattern in unreachable)
    }

    @Test
    fun normalCompletionStillReportsTheActuallyRejectedTailPattern() {
        val f = Fixture(boolean(false))
        val unreachable = CfirControlFlowConstAnalysis().collectUnreachablePatterns(f.graph)
        assertTrue(f.tailRejectedPattern in unreachable)
    }

    @Test
    fun differentCompletionsInSharedFinallyDoNotDuplicatePatternDiagnostics() {
        val f = Fixture(buildFunctionCall {
            coneTypeOrNull = ConePrimitiveType.BOOLEAN
            calleeReference = buildNamedReference { name = Name.identifier("unknownBool") }
        })
        val unreachable = CfirControlFlowConstAnalysis().collectUnreachablePatterns(f.graph)
        assertTrue(f.tailRejectedPattern in unreachable)
        assertEquals(1, unreachable.count { it === f.finallyRejectedPattern })
    }

    @Test
    fun aLegalBackEdgeInsideFinallyKeepsItsPendingCompletion() {
        val f = Fixture(boolean(true), finallyBackEdge = true)
        val unreachable = CfirControlFlowConstAnalysis().collectUnreachablePatterns(f.graph)
        assertFalse(f.tailRejectedPattern in unreachable)
        assertTrue(f.finallyRejectedPattern in unreachable)
        assertTrue(f.backEdgeSource.followingNodes.any { f.backEdgeSource.edgeTo(it).kind.isBack })
    }

    private class Fixture(selector: CfirExpression, finallyBackEdge: Boolean = false) {
        val graph = ControlFlowGraph(null, "real-const-consumer", ControlFlowGraph.Kind.CodeFragment)
        val tailRejectedPattern: CfirPattern
        val finallyRejectedPattern: CfirPattern
        val backEdgeSource: CFGNode<*>

        init {
            val enter = LiteralExpressionNode(graph, boolean(true), 0)
            val exit = LiteralExpressionNode(graph, boolean(false), 0)
            graph.enterNode = enter
            graph.exitNode = exit

            val thrownExpression = buildThrowExpression {
                exception = buildFunctionCall {
                    coneTypeOrNull = ConeClassLikeType(ConeClassLikeLookupTagImpl(ClassId.topLevel(FqName("std.core.Exception"))))
                    calleeReference = buildNamedReference { name = Name.identifier("Exception") }
                }
                coneTypeOrNull = ConePrimitiveType.NOTHING
            }
            val thrown = ThrowExceptionNode(graph, thrownExpression, 1)
            val t = buildTryExpression {
                tryBlock = buildBlock { statements.add(thrownExpression) }
                finallyBlock = buildBlock {}
            }
            val finallyEnter = FinallyBlockEnterNode(graph, t, 1)
            val finallyExit = FinallyBlockExitNode(graph, t, finallyEnter, 1)
            val resumed = ExceptionTransferNode(graph, thrown, 1)
            val uncaught = ExceptionExitNode(graph, thrownExpression, 0)
            graph.exceptionExitNode = uncaught
            val normalInput = LiteralExpressionNode(graph, boolean(false), 1)

            val chooseThrow = decision(selector, boolean(true))
            val rootFailure = LiteralExpressionNode(graph, boolean(false), 1)
            edge(enter, chooseThrow.first)
            edge(chooseThrow.first, thrown, MatchBranchSuccess)
            edge(chooseThrow.first, rootFailure, MatchBranchFailure)
            edge(rootFailure, normalInput)
            edge(thrown, finallyEnter, resumed)
            edge(normalInput, finallyEnter)
            edge(finallyExit, resumed, resumed)
            edge(resumed, uncaught, UncaughtExceptionPath)

            val finallyDecision = decision(boolean(true), boolean(false))
            finallyRejectedPattern = finallyDecision.second
            val finallySuccess = LiteralExpressionNode(graph, boolean(true), 1)
            val finallyFailure = LiteralExpressionNode(graph, boolean(false), 1)
            edge(finallyEnter, finallyDecision.first)
            edge(finallyDecision.first, finallySuccess, MatchBranchSuccess)
            edge(finallyDecision.first, finallyFailure, MatchBranchFailure)
            edge(finallySuccess, finallyExit)
            edge(finallyFailure, finallyExit)
            backEdgeSource = finallyFailure
            if (finallyBackEdge) CFGNode.addEdge(finallyFailure, finallyDecision.first,
                EdgeKind.CfgBackward, propagateDeadness = false)

            val tailDecision = decision(boolean(true), boolean(false))
            tailRejectedPattern = tailDecision.second
            val tailSuccess = LiteralExpressionNode(graph, boolean(true), 0)
            val tailFailure = LiteralExpressionNode(graph, boolean(false), 0)
            edge(finallyExit, tailDecision.first)
            edge(tailDecision.first, tailSuccess, MatchBranchSuccess)
            edge(tailDecision.first, tailFailure, MatchBranchFailure)
            edge(tailSuccess, exit)
            edge(tailFailure, exit)
            graph.complete()
        }

        private fun decision(subject: CfirExpression, expected: CfirExpression): Pair<MatchPatternDecisionNode, CfirPattern> {
            val pattern = buildConstPattern { expression = expected }
            val branch = buildMatchBranch { this.pattern = pattern; body = buildBlock {} }
            val match = buildMatchExpression { this.subject = subject; branches.add(branch) }
            return MatchPatternDecisionNode(graph, branch, pattern, null, emptyList(), pattern, match, 1) to pattern
        }

        private fun edge(from: CFGNode<*>, to: CFGNode<*>, label: EdgeLabel = NormalPath) {
            CFGNode.addEdge(from, to, EdgeKind.Forward, propagateDeadness = false, label = label)
        }
    }

    companion object {
        private fun boolean(value: Boolean): CfirLiteralExpression = buildLiteralExpression {
            kind = CfirLiteralKind.BOOLEAN
            this.value = value
            coneTypeOrNull = ConePrimitiveType.BOOLEAN
        }
    }
}

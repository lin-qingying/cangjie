@file:OptIn(
    org.cangnova.cangjie.cfir.CfirImplementationDetail::class,
    org.cangnova.cangjie.cfir.declarations.ResolveStateAccess::class,
    org.cangnova.cangjie.cfir.resolve.dfa.cfg.CfgInternals::class,
    org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals::class,
)

package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import java.util.Collections
import java.util.IdentityHashMap
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.impl.base.test.configurators.CaAnalysisApiDecompiledTestServiceRegistrar
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.CfirLazyBodiesCalculator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.*
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

/** 真正源码冷入口；由生产 resolver 调用真实 if builder，不在测试中补边或改 deadness。 */
@Timeout(60)
class IfCfgSourceSemanticsTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/ifCfgSemantics",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator
    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> =
        listOf(CaAnalysisApiDecompiledTestServiceRegistrar)

    @Test
    fun thenThrowWithoutElseKeepsFalseContinuation(file: CjFile, services: TestServices) = resolved(file) { v ->
        val control = v.ifAt("guard")
        val tail = v.callAt("tail")
        val thrown = v.throwAt("throw")
        assertFalse(v.exit(control).isDead)
        assertFalse(tail.isDead)
        assertTrue(reachable(v.graph.enterNode, tail))
        assertFalse(reachable(thrown, tail, normalOnly = true))
        assertTrue(thrown.followingNodes.filterIsInstance<StubNode>().single().isDead)
    }

    @Test
    fun twoReturnBranchesLeaveFollowingStatementDead(file: CjFile, services: TestServices) = resolved(file) { v ->
        val control = v.ifAt("guard")
        val tail = v.callAt("deadTail")
        assertTrue(v.exit(control).isDead)
        assertTrue(tail.isDead)
        assertFalse(reachable(v.graph.enterNode, tail))
        val returns = ControlFlowGraphCompletionTraversal(v.graph).returnPredecessors()
        assertEquals(identitySet<CFGNode<*>>().apply {
            add(v.returnAt("thenReturn")); add(v.returnAt("elseReturn"))
        }, returns)
        assertFalse(v.graph.exitNode.isDead)
    }

    @Test
    fun elseIfWithoutEndingElseKeepsNestedFalseContinuation(file: CjFile, services: TestServices) = resolved(file) { v ->
        val outer = v.ifAt("outerIf")
        val inner = v.ifAt("innerIf")
        assertSame(inner, outer.elseBranch)
        assertFalse(v.exit(outer).isDead)
        assertFalse(v.exit(inner).isDead)
        val tail = v.callAt("tail")
        assertFalse(tail.isDead)
        assertTrue(reachable(v.graph.enterNode, tail))
        assertFalse(reachable(v.throwAt("outerThrow"), tail, normalOnly = true))
        assertFalse(reachable(v.throwAt("innerThrow"), tail, normalOnly = true))
    }

    @Test
    fun conditionalThrowAndCatchBothPassThroughFinally(file: CjFile, services: TestServices) = resolved(file) { v ->
        val t = v.elements.filterIsInstance<CfirTryExpression>().single()
        val caught = v.nodes.filterIsInstance<CatchClauseEnterNode>().single { it.fir === t.catches.single() }
        val catchExit = v.nodes.filterIsInstance<CatchClauseExitNode>().single { it.fir === t.catches.single() }
        val finallyEnter = v.nodes.filterIsInstance<FinallyBlockEnterNode>().single { it.fir === t }
        val finallyExit = v.nodes.filterIsInstance<FinallyBlockExitNode>().single { it.fir === t }
        val normal = v.callAt("normal")
        val tail = v.callAt("tail")
        assertFalse(normal.isDead)
        assertTrue(reachable(v.throwAt("throw"), caught))
        assertTrue(reachable(caught, catchExit))
        assertTrue(reachable(normal, finallyEnter))
        assertTrue(reachable(catchExit, finallyEnter))
        assertTrue(reachable(finallyExit, tail))
        assertFalse(reachable(normal, tail, blocked = setOf(finallyEnter)))
        assertFalse(reachable(catchExit, tail, blocked = setOf(finallyEnter)))
        assertFalse(reachable(v.throwAt("throw"), normal, normalOnly = true))
    }

    @Test
    fun conditionalReturnInFinallyKeepsOriginalAndReplacementIdentity(file: CjFile, services: TestServices) = resolved(file) { v ->
        val oldReturn = v.returnAt("oldReturn")
        val newReturn = v.returnAt("newReturn")
        val control = v.ifAt("guard")
        assertFalse(v.exit(control).isDead)
        val actual = ControlFlowGraphCompletionTraversal(v.graph).returnPredecessors()
        assertEquals(identitySet<CFGNode<*>>().apply { add(oldReturn); add(newReturn) }, actual)
        assertSame(v.function, (oldReturn.fir as CfirReturnExpression).target.labeledElement)
        assertSame(v.function, (newReturn.fir as CfirReturnExpression).target.labeledElement)
    }

    @Test
    fun letPatternThenBindingIsDistinctFromElseBinding(file: CjFile, services: TestServices) = resolved(file) { v ->
        val control = v.ifAt("guard")
        assertTrue(control.condition is CfirLetPatternExpression)
        val thenUse = v.variableUseAt("thenUse")
        val elseUse = v.variableUseAt("elseUse")
        val thenSymbol = (thenUse.calleeReference as CfirResolvedNamedReference).resolvedSymbol
        val elseSymbol = (elseUse.calleeReference as CfirResolvedNamedReference).resolvedSymbol
        assertNotSame(thenSymbol, elseSymbol)
        assertEquals(ConePrimitiveType.INT64, (thenSymbol.cfir as CfirVariable).returnTypeRef.coneType)
        assertEquals(ConePrimitiveType.INT64, (elseSymbol.cfir as CfirVariable).returnTypeRef.coneType)
        assertFalse(v.callAt("thenCall").isDead)
        assertFalse(v.callAt("elseCall").isDead)
    }

    @Test
    fun branchLambdasCompleteInActualGenericParentCall(file: CjFile, services: TestServices) = resolved(file) { v ->
        val control = v.ifAt("guard")
        val selected = v.callAt("selectCall")
        val lambdas = v.elements.filterIsInstance<CfirAnonymousFunction>().toList()
        assertEquals(2, lambdas.size)
        lambdas.forEach {
            assertEquals(ConePrimitiveType.INT64, it.returnTypeRef.coneType)
            val graph = (it.controlFlowGraphReference as CfirControlFlowGraphReferenceImpl).controlFlowGraph
            assertSame(it, graph.declaration)
        }
        val ifType = control.coneTypeOrNull as ConeFunctionType
        assertTrue(ifType.parameterTypes.isEmpty())
        assertEquals(ConePrimitiveType.INT64, ifType.returnType)
        assertEquals(ConePrimitiveType.INT64, selected.fir.coneTypeOrNull)
        assertFalse(v.exit(control).isDead)
        assertFalse(selected.isDead)
        assertTrue(reachable(v.graph.enterNode, selected))
    }


    @Test
    fun branchLambdaCallsLiftThroughIfIntoTheirActualGenericParent(file: CjFile, services: TestServices) = resolved(file) { v ->
        val control = v.ifAt("guard")
        val selected = v.callAt("consumeCall")
        val left = v.callAt("leftCarrier")
        val right = v.callAt("rightCarrier")
        for (call in listOf(left, right)) {
            val type = call.fir.coneTypeOrNull as ConeFunctionType
            assertEquals(ConePrimitiveType.INT64, type.returnType)
            assertFalse(call.isDead)
        }
        assertEquals(2, v.elements.filterIsInstance<CfirAnonymousFunction>().size)
        assertEquals(ConePrimitiveType.INT64, (control.coneTypeOrNull as ConeFunctionType).returnType)
        assertEquals(ConePrimitiveType.INT64, selected.fir.coneTypeOrNull)
        assertTrue(reachable(v.graph.enterNode, selected))
        assertFalse(v.exit(control).isDead)
    }

    /** 文件先展开 RAW，再由真实 BODY_RESOLVE 冷入口完成；不构造替身 CFG。 */
    private fun resolved(file: CjFile, check: (View) -> Unit) {
        analyzeForTest(file) {
            with(this as CaCfirSession) {
                val raw = file.getOrBuildCfirFile(resolutionFacade)
                assertEquals(CfirResolvePhase.RAW_CFIR, raw.resolvePhase)
                CfirLazyBodiesCalculator.calculateAllLazyExpressionsInFile(raw)
                val function = raw.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "tested" }
                val before = collect(function).filterIsInstance<CfirIfExpression>()
                    .associateBy { kotlin.checkNotNull(it.source?.psi) }
                assertTrue(before.isNotEmpty())
                function.symbol.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
                val elements = collect(function)
                elements.filterIsInstance<CfirIfExpression>().forEach { assertSame(before[it.source?.psi], it) }
                assertEquals(before.size, elements.filterIsInstance<CfirIfExpression>().size)
                val graph = (function.controlFlowGraphReference as CfirControlFlowGraphReferenceImpl).controlFlowGraph
                assertSame(function, graph.declaration)
                val nodes = identitySet<CFGNode<*>>()
                elements.filterIsInstance<CfirFunction>().forEach {
                    val owned = (it.controlFlowGraphReference as CfirControlFlowGraphReferenceImpl).controlFlowGraph
                    assertSame(it, owned.declaration)
                    nodes.addAll(owned.nodes)
                }
                val view = View(file, function, elements, graph, nodes)
                elements.filterIsInstance<CfirIfExpression>().forEach {
                    assertSame(it, view.enter(it).fir)
                    assertSame(view.enter(it).owner, view.exit(it).owner)
                }
                check(view)
            }
        }
    }

    private class View(
        val file: CjFile,
        val function: CfirNamedFunction,
        val elements: Set<CfirElement>,
        val graph: ControlFlowGraph,
        val nodes: Set<CFGNode<*>>,
    ) {
        private inline fun <reified T : PsiElement> psi(marker: String): T {
            val token = "/*@" + marker + "*/"
            val offset = file.text.indexOf(token)
            require(offset >= 0 && file.text.indexOf(token, offset + token.length) < 0) { "non-unique marker: " + marker }
            return kotlin.checkNotNull(PsiTreeUtil.getParentOfType(
                file.findElementAt(offset + token.length), T::class.java, false,
            ))
        }
        fun ifAt(marker: String): CfirIfExpression {
            val target = psi<CjIfExpression>(marker)
            return elements.filterIsInstance<CfirIfExpression>().single { it.source?.psi === target }
        }
        fun enter(expression: CfirIfExpression): IfEnterNode =
            nodes.filterIsInstance<IfEnterNode>().single { it.fir === expression }
        fun exit(expression: CfirIfExpression): IfExitNode =
            nodes.filterIsInstance<IfExitNode>().single { it.fir === expression }
        fun callAt(marker: String): FunctionCallExitNode {
            val target = psi<CjCallExpression>(marker)
            val call = elements.filterIsInstance<CfirFunctionCall>().single { it.source?.psi === target }
            return nodes.filterIsInstance<FunctionCallExitNode>().single { it.fir === call }
        }
        fun throwAt(marker: String): ThrowExceptionNode {
            val target = psi<CjThrowExpression>(marker)
            val statement = elements.filterIsInstance<CfirThrowExpression>().single { it.source?.psi === target }
            return nodes.filterIsInstance<ThrowExceptionNode>().single { it.fir === statement }
        }
        fun returnAt(marker: String): JumpNode {
            val target = psi<CjReturnExpression>(marker)
            val statement = elements.filterIsInstance<CfirReturnExpression>().single { it.source?.psi === target }
            return nodes.filterIsInstance<JumpNode>().single { it.fir === statement }
        }
        fun variableUseAt(marker: String): CfirQualifiedAccessExpression {
            val target = psi<CjSimpleNameExpression>(marker)
            return elements.filterIsInstance<CfirQualifiedAccessExpression>().single { it.source?.psi === target }
        }
    }

    private fun collect(root: CfirElement): Set<CfirElement> {
        val result = identitySet<CfirElement>()
        root.accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (result.add(element)) element.acceptChildren(this)
            }
        })
        return result
    }

    private fun reachable(
        start: CFGNode<*>, target: CFGNode<*>, normalOnly: Boolean = false, blocked: Set<CFGNode<*>> = emptySet(),
    ): Boolean {
        val seen = identitySet<CFGNode<*>>()
        val queue = ArrayDeque<CFGNode<*>>()
        queue.add(start)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node in blocked || !seen.add(node)) continue
            if (node === target) return true
            for (next in node.followingNodes) {
                val edge = node.edgeTo(next)
                if (!edge.kind.usedInCfa || edge.kind.isDead || normalOnly && edge.label !== NormalPath) continue
                queue.add(next)
            }
        }
        return false
    }

    private fun <T : Any> identitySet(): MutableSet<T> =
        Collections.newSetFromMap(IdentityHashMap<T, Boolean>())
}

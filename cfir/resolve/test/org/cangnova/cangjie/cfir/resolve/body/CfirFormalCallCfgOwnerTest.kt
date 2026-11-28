@file:OptIn(org.cangnova.cangjie.cfir.resolve.dfa.cfg.CfgInternals::class, org.cangnova.cangjie.util.PrivateForInline::class)

package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildCodeFragment
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.CfirResolutionSnapshot
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.candidate.*
import org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.*
import org.cangnova.cangjie.cfir.resolve.inference.inferenceComponents
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.symbols.CfirCodeFragmentSymbol
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** 生产 builder/copier/writer 的 call 身份协议；不按 source 文本或名字查找 CFG node。 */
class CfirFormalCallCfgOwnerTest {
    @Test
    fun completedGraphCallReplacementKeepsNodesEdgesAndActualFormalFir() {
        val f = Fixture()
        val nodes = f.graph.nodes.toList()
        val edges = nodes.map { it.followingNodes.toList() }
        f.builder.updateFunctionCallNodes(f.original, f.replacement, f.graph)
        f.fragment.block.replaceStatements(listOf(f.replacement))
        assertEquals(nodes, f.graph.nodes)
        assertEquals(edges, nodes.map { it.followingNodes.toList() })
        assertTrue(f.callNodes().all { it.fir === f.replacement })
        assertEquals(4, f.callNodes().size)
        assertTrue(f.callNodes().all { it.owner === f.graph })
    }

    @Test
    fun postponedSplitAndMergeKeepTheirTopologyAndSubgraphs() {
        val f = Fixture()
        val builder = ControlFlowGraphBuilder()
        val owner = CallResolutionTestFixtures.buildFunctionSymbol(f.session, "postponedOwner", ConePrimitiveType.INT32).cfir
        owner.replaceBody(buildBlock { statements += f.original })
        builder.enterFunction(owner)
        val graph = builder.currentGraph
        val split = builder.createSplitPostponedLambdasNode(f.original, emptyList())
        val merge = builder.createMergePostponedLambdaExitsNode(f.original)
        CFGNode.addEdge(graph.enterNode, split, EdgeKind.Forward, propagateDeadness = false)
        CFGNode.addEdge(split, merge, EdgeKind.Forward, propagateDeadness = false)
        CFGNode.addEdge(merge, graph.exitNode, EdgeKind.Forward, propagateDeadness = false)
        graph.complete()
        builder.updateFunctionCallNodes(f.original, f.replacement, graph)
        assertSame(f.replacement, split.fir)
        assertSame(f.replacement, merge.fir)
        assertTrue(split.subGraphs.isEmpty())
        assertSame(graph, split.owner)
        assertSame(graph, merge.owner)
        assertTrue(f.callNodes().all { it.fir === f.original })

    }

    @Test
    fun sameSourceAndSameCalleeDoNotAuthorizeAnotherCallIdentity() {
        val f = Fixture()
        val other = buildFunctionCallCopy(f.original) {}
        f.builder.updateFunctionCallNodes(other, f.replacement, f.graph)
        assertTrue(f.callNodes().all { it.fir === f.original })
    }

    @Test
    fun astRollbackRestoresActualFirCellsAndRejectsRevokedReplacement() {
        val f = Fixture()
        val before = CfirResolutionSnapshot.capture(f.owner)
        f.builder.updateFunctionCallNodes(f.original, f.replacement, f.graph)
        f.fragment.block.replaceStatements(listOf(f.replacement))
        before.restore()
        assertTrue(f.callNodes().all { it.fir === f.original })
        assertSame(f.original, f.fragment.block.statements.single())
        val third = buildFunctionCallCopy(f.replacement) {}
        assertThrows(IllegalStateException::class.java) { f.builder.updateFunctionCallNodes(f.replacement, third, f.graph) }
        f.builder.updateFunctionCallNodes(f.original, third, f.graph)
        assertTrue(f.callNodes().all { it.fir === third })
    }

    @Test
    fun selectedAstSnapshotRestoresTheSelectedFirGeneration() {
        val f = Fixture()
        val before = CfirResolutionSnapshot.capture(f.owner)
        f.builder.updateFunctionCallNodes(f.original, f.replacement, f.graph)
        f.fragment.block.replaceStatements(listOf(f.replacement))
        val selected = CfirResolutionSnapshot.capture(f.owner)
        before.restore()
        selected.restore()
        assertTrue(f.callNodes().all { it.fir === f.replacement })
        assertSame(f.replacement, f.fragment.block.statements.single())
        f.builder.updateFunctionCallNodes(f.original, f.replacement, f.graph)
    }

    @Test
    fun dfaSnapshotHasIndependentNodesAndRestorableCallIndex() {
        val f = Fixture()
        val mapper = object : SnapshotCfirMapper {
            override fun <T : org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol<*>> mapSymbol(symbol: T): T = symbol
            override fun <T : org.cangnova.cangjie.cfir.CfirElement> mapElement(element: T): T = element
        }
        val snapshot = f.context.dataFlowAnalyzerContext.createSnapshot(mapper)
        val copy = checkNotNull(snapshot.graphMapping[f.graph])
        f.builder.updateFunctionCallNodes(f.original, f.replacement, f.graph)
        assertTrue(copy.nodes.filterIsInstance<RevisableFunctionCallNode>().all { it.fir === f.original })
        f.context.dataFlowAnalyzerContext.resetFrom(snapshot.context)
        f.context.dataFlowAnalyzerContext.graphBuilder.updateFunctionCallNodes(f.original, f.replacement, copy)
        assertTrue(copy.nodes.filterIsInstance<RevisableFunctionCallNode>().all { it.fir === f.replacement })
        assertNotSame(f.graph.nodes.first(), copy.nodes.first())
    }

    @Test
    fun isolatedProbeCannotReviseTheFormalParentGraph() {
        val f = Fixture()
        f.context.dataFlowAnalyzerContext.withIsolatedContext {
            f.context.dataFlowAnalyzerContext.graphBuilder.updateFunctionCallNodes(f.original, f.replacement, f.graph)
        }
        assertTrue(f.callNodes().all { it.fir === f.original })
        assertSame(f.builder, f.context.dataFlowAnalyzerContext.graphBuilder)
    }

    @Test
    fun actualResultsWriterCommitsArgumentAndCfgTogether() {
        val f = Fixture()
        val outer = CallResolutionTestFixtures.buildFunctionSymbol(f.session, "accept", ConePrimitiveType.INT32,
            parameterTypes = listOf(ConePrimitiveType.INT32))
        val atom = checkNotNull(ConeResolutionAtom.createRawAtom(f.original))
        val template = CallResolutionTestFixtures.buildCallInfo(f.session, "accept", listOf(f.original))
        val call = buildFunctionCall {
            source = f.original.source
            calleeReference = buildResolvedNamedReference { name = Name.identifier("accept"); resolvedSymbol = outer }
            argumentList = buildArgumentList { arguments += f.original }
        }
        val info = CallInfo(callSite = call, callKind = CallKind.Function, name = Name.identifier("accept"),
            explicitReceiver = null, arguments = listOf(f.original), isUsedAsGetClassReceiver = false, typeArguments = emptyList(),
            session = f.session, containingFile = template.containingFile, containingDeclarations = listOf(f.owner),
            resolutionMode = ResolutionMode.ContextIndependent)
        val candidate = Candidate(symbol = outer, dispatchReceiver = null, givenExtensionReceiver = null,
            explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
            constraintSystemFactory = f.session.inferenceComponents.constraintSystemFactory,
            baseSystem = f.session.inferenceComponents.createConstraintSystem().asReadOnlyStorage(),
            callInfo = info, originScope = null, bodyResolveContext = f.context).apply {
            initializeSubstitutorAndVariables(ConeSubstitutor.Empty, emptyList())
            initializeArgumentMapping(listOf(atom), linkedMapOf(atom to outer.cfir.valueParameters.single()))
            setUpdatedArgumentFromContextSensitiveResolution(f.original, f.replacement)
        }
        f.fragment.block.replaceStatements(listOf(call))
        call.replaceCalleeReference(CfirNamedReferenceWithCandidate(call.source, info.name, candidate))
        val transformer = CfirBodyResolveTransformer(f.session, ScopeSession(), outerBodyResolveContext = f.context)
        call.transformSingle(transformer.components.callCompleter.createCompletionResultsWriter(
            ConeSubstitutor.Empty, completionSystem = candidate.system, completionRoot = call, completionCandidate = candidate), null)
        assertSame(f.replacement, call.argumentList.arguments.single())
        assertTrue(f.callNodes().all { it.fir === call.argumentList.arguments.single() })
    }

    private class Fixture {
        val session = CallResolutionTestFixtures.newTestSession()
        val context = BodyResolveContext(ReturnTypeCalculator.Default, CfirDataFlowAnalyzerContext())
        val builder = context.dataFlowAnalyzerContext.graphBuilder
        private val function = CallResolutionTestFixtures.buildFunctionSymbol(session, "make", ConePrimitiveType.INT32)
        val original = buildFunctionCall {
            source = CallResolutionTestFixtures.buildTypedExpression(ConePrimitiveType.INT32).source
            coneTypeOrNull = ConePrimitiveType.INT32
            calleeReference = buildResolvedNamedReference { name = Name.identifier("make"); resolvedSymbol = function }
        }
        val replacement = buildFunctionCallCopy(original) {}
        val fragment = buildCodeFragment {
            source = original.source
            moduleData = session.moduleData
            origin = CfirDeclarationOrigin.Source
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            attributes = CfirDeclarationAttributes.EMPTY
            symbol = CfirCodeFragmentSymbol()
            block = buildBlock { statements += original }
        }
        val owner = CallResolutionTestFixtures.buildFunctionSymbol(session, "owner", ConePrimitiveType.INT32).cfir
        val graph: ControlFlowGraph
        init {
            owner.replaceBody(fragment.block)
            context.file = CallResolutionTestFixtures.buildCallInfo(session, "make").containingFile
            builder.enterFunction(owner)
            builder.enterBlock(fragment.block)
            builder.enterCallArguments(original, emptyList())
            builder.exitCallArguments()
            builder.enterFunctionCall(original)
            builder.exitFunctionCall(original, callCompleted = true)
            builder.exitBlock(fragment.block)
            graph = builder.exitFunction(owner).second
            owner.replaceControlFlowGraphReference(CfirControlFlowGraphReferenceImpl(graph))
        }
        fun callNodes(): List<CFGNode<*>> = graph.nodes.filter { it is RevisableFunctionCallNode }
    }
}

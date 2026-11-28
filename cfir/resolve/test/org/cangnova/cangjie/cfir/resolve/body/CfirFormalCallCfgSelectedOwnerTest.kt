@file:OptIn(org.cangnova.cangjie.cfir.resolve.dfa.cfg.CfgInternals::class, org.cangnova.cangjie.util.PrivateForInline::class)

package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunctionCopy
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.CfirResolutionSnapshot
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.candidate.*
import org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraph
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraphBuilder
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.RevisableFunctionCallNode
import org.cangnova.cangjie.cfir.resolve.inference.inferenceComponents
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** 真实 selected snapshot 的采用入口、probe 隔离和跨 owner 的 graph copier 协议。 */
class CfirFormalCallCfgSelectedOwnerTest {
    @Test
    fun writerUsesTheActualSelectedFormalGraphOwnerAfterIsolatedContextReturns() {
        val f = Fixture()
        val nodes = f.graph.nodes.toList()
        val edges = nodes.map { it.followingNodes.toList() }
        f.write(f.call)
        assertSame(f.call, f.body.statements.single())
        assertSame(f.replacement, f.call.argumentList.arguments.single())
        assertFalse(f.graph.nodes.filterIsInstance<RevisableFunctionCallNode>().any { it.fir === f.original })
        assertEquals(4, f.graph.nodes.filterIsInstance<RevisableFunctionCallNode>().count { it.fir === f.replacement })
        assertEquals(nodes, f.graph.nodes)
        assertEquals(edges, nodes.map { it.followingNodes.toList() })
        assertSame(f.original.source, f.replacement.source)
    }

    @Test
    fun isolatedWriterDoesNotBorrowThePublishedParentGraphForItsPrivateCall() {
        val f = Fixture()
        val probe = buildFunctionCallCopy(f.call) {
            argumentList = buildArgumentList { arguments += f.original }
        }
        f.context.dataFlowAnalyzerContext.withIsolatedContext {
            f.write(probe)
            assertSame(f.replacement, probe.argumentList.arguments.single())
            assertEquals("", f.context.dataFlowAnalyzerContext.graphBuilder.describeFunctionCallNodes(f.original))
        }
        assertSame(f.original, f.call.argumentList.arguments.single())
        assertEquals(4, f.graph.nodes.filterIsInstance<RevisableFunctionCallNode>().count { it.fir === f.original })
        assertEquals("", f.parentBuilder.describeFunctionCallNodes(f.replacement))
    }

    @Test
    fun copierPreservesTheAdoptedGraphIndexEvenWithoutParentFactoryNodes() {
        val f = Fixture()
        val mapper = object : SnapshotCfirMapper {
            override fun <T : org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol<*>> mapSymbol(symbol: T): T = symbol
            override fun <T : org.cangnova.cangjie.cfir.CfirElement> mapElement(element: T): T = element
        }
        val snapshot = f.context.dataFlowAnalyzerContext.createSnapshot(mapper)
        val copy = checkNotNull(snapshot.graphMapping[f.graph])
        f.owner.replaceControlFlowGraphReference(CfirControlFlowGraphReferenceImpl(copy))
        f.context.dataFlowAnalyzerContext.resetFrom(snapshot.context)
        f.write(f.call)
        assertSame(f.replacement, f.call.argumentList.arguments.single())
        assertEquals(4, copy.nodes.filterIsInstance<RevisableFunctionCallNode>().count { it.fir === f.replacement })
        assertEquals(4, f.graph.nodes.filterIsInstance<RevisableFunctionCallNode>().count { it.fir === f.original })
        assertNotSame(f.graph.nodes.first(), copy.nodes.first())
    }

    private class Fixture {
        val session = CallResolutionTestFixtures.newTestSession()
        val context = BodyResolveContext(ReturnTypeCalculator.Default, CfirDataFlowAnalyzerContext())
        val parentBuilder = context.dataFlowAnalyzerContext.graphBuilder
        private val source = CallResolutionTestFixtures.buildTypedExpression(ConePrimitiveType.INT32).source
        private val makeTemplate = CallResolutionTestFixtures.buildFunctionSymbol(session, "make", ConePrimitiveType.INT32)
        private val make = buildNamedFunctionCopy(makeTemplate.cfir) {
            source = this@Fixture.source
            origin = CfirDeclarationOrigin.Source
            symbol = CfirNamedFunctionSymbol(makeTemplate.callableId)
        }.symbol
        private val accept = CallResolutionTestFixtures.buildFunctionSymbol(session, "accept", ConePrimitiveType.INT32,
            parameterTypes = listOf(ConePrimitiveType.INT32))
        val original = buildFunctionCall {
            source = this@Fixture.source
            coneTypeOrNull = ConePrimitiveType.INT32
            calleeReference = buildResolvedNamedReference { name = Name.identifier("make"); resolvedSymbol = make }
        }
        val replacement = buildFunctionCallCopy(original) {}
        val call = buildFunctionCall {
            source = original.source
            coneTypeOrNull = ConePrimitiveType.INT32
            calleeReference = buildResolvedNamedReference { name = Name.identifier("accept"); resolvedSymbol = accept }
            argumentList = buildArgumentList { arguments += original }
        }
        private val ownerTemplate = CallResolutionTestFixtures.buildFunctionSymbol(session, "owner", ConePrimitiveType.INT32)
        val owner = buildNamedFunctionCopy(ownerTemplate.cfir) {
            source = original.source
            origin = CfirDeclarationOrigin.Source
            symbol = CfirNamedFunctionSymbol(ownerTemplate.callableId)
        }
        val body = buildBlock { statements += call }
        private val template = CallResolutionTestFixtures.buildCallInfo(session, "accept", listOf(original))
        lateinit var graph: ControlFlowGraph
        init {
            owner.replaceBody(body)
            assertEquals(CfirDeclarationOrigin.Source, owner.origin)
            assertEquals(CfirDeclarationOrigin.Source, make.cfir.origin)
            context.file = template.containingFile
            val before = CfirResolutionSnapshot.capture(owner)
            lateinit var selected: CfirResolutionSnapshot
            lateinit var selectedBuilder: ControlFlowGraphBuilder
            context.dataFlowAnalyzerContext.withIsolatedContext {
                selectedBuilder = context.dataFlowAnalyzerContext.graphBuilder
                val b = selectedBuilder
                b.enterFunction(owner)
                b.enterBlock(body)
                b.enterCallArguments(call, emptyList())
                b.enterCallArguments(original, emptyList())
                b.exitCallArguments()
                b.enterFunctionCall(original)
                b.exitFunctionCall(original, callCompleted = true)
                b.exitCallArguments()
                b.enterFunctionCall(call)
                b.exitFunctionCall(call, callCompleted = true)
                b.exitBlock(body)
                graph = b.exitFunction(owner).second
                owner.replaceControlFlowGraphReference(CfirControlFlowGraphReferenceImpl(graph))
                selected = CfirResolutionSnapshot.capture(owner)
            }
            before.restore()
            assertEquals("", parentBuilder.describeFunctionCallNodes(original))
            selected.restoreForSelectedCandidate(null, null, context.dataFlowAnalyzerContext)
            assertSame(parentBuilder, context.dataFlowAnalyzerContext.graphBuilder)
            assertNotSame(parentBuilder, selectedBuilder)
            assertEquals(8, graph.nodes.filterIsInstance<RevisableFunctionCallNode>().size)
            assertEquals(selectedBuilder.describeFunctionCallNodes(original), parentBuilder.describeFunctionCallNodes(original))
            assertNotEquals("", parentBuilder.describeFunctionCallNodes(original))
        }

        fun write(root: CfirFunctionCall) {
            val atom = checkNotNull(ConeResolutionAtom.createRawAtom(original))
            val info = CallInfo(callSite = root, callKind = CallKind.Function, name = Name.identifier("accept"),
                explicitReceiver = null, arguments = listOf(original), isUsedAsGetClassReceiver = false,
                typeArguments = emptyList(), session = session, containingFile = template.containingFile,
                containingDeclarations = listOf(owner), resolutionMode = ResolutionMode.ContextIndependent)
            val candidate = Candidate(symbol = accept, dispatchReceiver = null, givenExtensionReceiver = null,
                explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                constraintSystemFactory = session.inferenceComponents.constraintSystemFactory,
                baseSystem = session.inferenceComponents.createConstraintSystem().asReadOnlyStorage(),
                callInfo = info, originScope = null, bodyResolveContext = context).apply {
                initializeSubstitutorAndVariables(ConeSubstitutor.Empty, emptyList())
                initializeArgumentMapping(listOf(atom), linkedMapOf(atom to accept.cfir.valueParameters.single()))
                setUpdatedArgumentFromContextSensitiveResolution(original, replacement)
            }
            root.replaceCalleeReference(CfirNamedReferenceWithCandidate(root.source, info.name, candidate))
            val transformer = CfirBodyResolveTransformer(session, ScopeSession(), outerBodyResolveContext = context)
            root.transformSingle(transformer.components.callCompleter.createCompletionResultsWriter(
                ConeSubstitutor.Empty, completionSystem = candidate.system, completionRoot = root,
                completionCandidate = candidate), null)
        }
    }
}

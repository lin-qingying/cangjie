@file:OptIn(org.cangnova.cangjie.cfir.resolve.dfa.cfg.CfgInternals::class, org.cangnova.cangjie.util.PrivateForInline::class)

package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirFunctionTarget
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.candidate.*
import org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraph
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.RevisableFunctionCallNode
import org.cangnova.cangjie.cfir.resolve.inference.ExpectedArgumentType
import org.cangnova.cangjie.cfir.resolve.inference.inferenceComponents
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.symbols.CfirAnonymousFunctionSymbol
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** 只运行实际 return adoption 私有接缝的负例，禁止靠后续 body 遍历偶然补上 AST/CFG。 */
class CfirFormalCallCfgReturnAdoptionTest {
    @Test
    fun implicitReturnAdoptionUpdatesItsActualBlockSlotAndExistingCfgWithoutBodyTraversal() {
        val f = Fixture(explicit = false)
        val nodes = f.graph.nodes.toList()
        val edges = nodes.map { it.followingNodes.toList() }
        val adopted = f.adoptOnly()
        assertSame(f.replacement, adopted.single().expression)
        assertSame(f.replacement, f.body.statements.single())
        assertSame(f.body, adopted.single().containingStatement)
        assertFalse(adopted.single().isExplicit)
        assertTrue(f.callNodes().all { it.fir === f.replacement })
        assertEquals(nodes, f.graph.nodes)
        assertEquals(edges, nodes.map { it.followingNodes.toList() })
    }

    @Test
    fun explicitReturnAdoptionKeepsActualReturnAndTargetOwnerAndUpdatesExistingCfg() {
        val f = Fixture(explicit = true)
        val originalReturn = checkNotNull(f.returnStatement)
        val adopted = f.adoptOnly()
        assertSame(f.replacement, adopted.single().expression)
        assertSame(originalReturn, f.body.statements.single())
        assertSame(f.replacement, originalReturn.result)
        assertSame(originalReturn, adopted.single().containingStatement)
        assertTrue(adopted.single().isExplicit)
        assertSame(f.lambda, originalReturn.target.labeledElement)
        assertTrue(f.callNodes().all { it.fir === f.replacement })
    }

    @Test
    fun actualResultsWriterCompletesImplicitLambdaReturnAndCfgTogether() {
        val f = Fixture(explicit = false)
        f.outer.transformSingle(f.writer, null)
        val result = (f.body.statements.single() as? CfirReturnExpression)?.result ?: f.body.statements.single()
        assertSame(f.replacement, result)
        assertTrue(f.callNodes().all { it.fir === f.replacement })
        assertSame(f.lambda, f.expression.anonymousFunction)
    }

    @Test
    fun actualResultsWriterCompletesExplicitLambdaReturnAndCfgTogether() {
        val f = Fixture(explicit = true)
        val statement = checkNotNull(f.returnStatement)
        f.outer.transformSingle(f.writer, null)
        assertSame(statement, f.body.statements.single())
        assertSame(f.replacement, statement.result)
        assertSame(f.lambda, statement.target.labeledElement)
        assertTrue(f.callNodes().all { it.fir === f.replacement })
    }

    private class Fixture(explicit: Boolean) {
        val session = CallResolutionTestFixtures.newTestSession()
        val context = BodyResolveContext(ReturnTypeCalculator.Default, CfirDataFlowAnalyzerContext())
        private val actualSource = CallResolutionTestFixtures.buildTypedExpression(ConePrimitiveType.INT32).source
        private val make = CallResolutionTestFixtures.buildFunctionSymbol(session, "make", ConePrimitiveType.INT32)
        val original = buildFunctionCall {
            source = actualSource
            coneTypeOrNull = ConePrimitiveType.INT32
            calleeReference = buildResolvedNamedReference { name = Name.identifier("make"); resolvedSymbol = make }
        }
        val replacement = buildFunctionCallCopy(original) {}
        val body = buildBlock { source = actualSource }
        private val functionType = ConeFunctionType(emptyList(), ConePrimitiveType.INT32)
        val lambda = buildAnonymousFunction {
            source = actualSource
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            deprecationsProvider = EmptyDeprecationsProvider
            status = CfirDeclarationStatusImpl()
            returnTypeRef = buildResolvedTypeRef { coneType = ConePrimitiveType.INT32 }
            typeRef = buildResolvedTypeRef { coneType = functionType }
            symbol = CfirAnonymousFunctionSymbol()
            hasExplicitParameterList = false
            isLambda = true
            body = this@Fixture.body
        }
        val returnStatement = if (explicit) buildReturnExpression {
            source = actualSource
            coneTypeOrNull = ConePrimitiveType.NOTHING
            target = CfirFunctionTarget(null, isLambda = true).also { it.bind(lambda) }
            result = original
        } else null
        val expression = buildAnonymousFunctionExpression { source = actualSource; anonymousFunction = lambda; isTrailingLambda = false }
        private val accept = CallResolutionTestFixtures.buildFunctionSymbol(session, "acceptLambda", ConePrimitiveType.INT32,
            parameterTypes = listOf(functionType))
        val outer = buildFunctionCall {
            source = actualSource
            coneTypeOrNull = ConePrimitiveType.INT32
            argumentList = buildArgumentList { arguments += expression }
            calleeReference = buildResolvedNamedReference { name = Name.identifier("acceptLambda"); resolvedSymbol = accept }
        }
        private val parent = CallResolutionTestFixtures.buildFunctionSymbol(session, "parent", ConePrimitiveType.INT32).cfir
        private val template = CallResolutionTestFixtures.buildCallInfo(session, "acceptLambda", listOf(expression))
        val transformer: CfirBodyResolveTransformer
        val graph: ControlFlowGraph
        val writer: org.cangnova.cangjie.cfir.resolve.inference.CfirCallCompletionResultsWriterTransformer
        init {
            body.replaceStatements(listOf(returnStatement ?: original))
            context.file = template.containingFile
            val b = context.dataFlowAnalyzerContext.graphBuilder
            val parentBody = buildBlock { source = actualSource; statements += outer }
            parent.replaceBody(parentBody)
            b.enterFunction(parent)
            b.enterBlock(parentBody)
            b.enterCallArguments(outer, listOf(lambda))
            b.enterAnonymousFunctionExpression(expression)
            b.enterAnonymousFunction(lambda)
            b.enterBlock(body)
            b.enterCallArguments(original, emptyList())
            b.exitCallArguments()
            b.enterFunctionCall(original)
            b.exitFunctionCall(original, callCompleted = true)
            returnStatement?.let(b::exitJump)
            b.exitBlock(body)
            graph = b.exitFunction(lambda).second
            lambda.replaceControlFlowGraphReference(CfirControlFlowGraphReferenceImpl(graph))
            val atom = checkNotNull(ConeResolutionAtom.createRawAtom(expression))
            val info = CallInfo(callSite = outer, callKind = CallKind.Function, name = Name.identifier("acceptLambda"),
                explicitReceiver = null, arguments = listOf(expression), isUsedAsGetClassReceiver = false,
                typeArguments = emptyList(), session = session, containingFile = template.containingFile,
                containingDeclarations = listOf(parent), resolutionMode = ResolutionMode.ContextIndependent)
            val candidate = Candidate(symbol = accept, dispatchReceiver = null, givenExtensionReceiver = null,
                explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                constraintSystemFactory = session.inferenceComponents.constraintSystemFactory,
                baseSystem = session.inferenceComponents.createConstraintSystem().asReadOnlyStorage(),
                callInfo = info, originScope = null, bodyResolveContext = context).apply {
                initializeSubstitutorAndVariables(ConeSubstitutor.Empty, emptyList())
                initializeArgumentMapping(listOf(atom), linkedMapOf(atom to accept.cfir.valueParameters.single()))
                setUpdatedArgumentFromContextSensitiveResolution(original, replacement)
            }
            outer.replaceCalleeReference(CfirNamedReferenceWithCandidate(outer.source, info.name, candidate))
            transformer = CfirBodyResolveTransformer(session, ScopeSession(), outerBodyResolveContext = context)
            writer = transformer.components.callCompleter.createCompletionResultsWriter(
                ConeSubstitutor.Empty, completionSystem = candidate.system, completionRoot = outer,
                completionCandidate = candidate)
        }

        /** 反射仅进入现有私有生产接缝及其实际 owner 栈，不增设测试 API/tracing，不运行 body 遍历。 */
        @Suppress("UNCHECKED_CAST")
        fun adoptOnly(): Collection<CfirDataFlowAnalyzer.CfirAnonymousFunctionReturnExpressionInfo> {
            val info = transformer.components.dataFlowAnalyzer.returnExpressionsOfAnonymousFunction(lambda)
            assertEquals(1, info.size)
            assertSame(original, info.single().expression)
            val data = ExpectedArgumentType.ArgumentsMap(
                map = mapOf(expression to functionType),
                lambdasReturnTypes = mapOf(lambda to ConePrimitiveType.INT32),
                forErrorReference = false,
                argumentMappingFailed = false,
                argumentReplacements = mapOf(original to replacement),
            )
            val adopt = writer.javaClass.getDeclaredMethod("replacePostponedAtomsInReturnExpressions",
                Collection::class.java, ExpectedArgumentType::class.java).apply { isAccessible = true }
            val owner = writer.javaClass.getDeclaredMethod("withCallCfgOwner",
                CfirControlFlowGraphOwner::class.java, Function0::class.java).apply { isAccessible = true }
            return owner.invoke(writer, lambda, { adopt.invoke(writer, info, data) }) as
                Collection<CfirDataFlowAnalyzer.CfirAnonymousFunctionReturnExpressionInfo>
        }
        fun callNodes() = graph.nodes.filterIsInstance<RevisableFunctionCallNode>()
    }
}

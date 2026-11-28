@file:OptIn(
    org.cangnova.cangjie.cfir.CfirImplementationDetail::class,
    org.cangnova.cangjie.cfir.resolve.dfa.cfg.CfgInternals::class,
)

package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.builder.buildCodeFragment
import org.cangnova.cangjie.cfir.declarations.builder.buildFile
import org.cangnova.cangjie.cfir.declarations.builder.buildPackageDirective
import org.cangnova.cangjie.cfir.declarations.builder.buildPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirTypeOperationKind
import org.cangnova.cangjie.cfir.expressions.builder.buildAssignment
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildTryExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildTupleLiteral
import org.cangnova.cangjie.cfir.expressions.builder.buildTypeOperator
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.resolve.dfa.FlowPath
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraph
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.DataFlowAnalysisState
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.FinallyBlockExitNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.NormalPath
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.symbols.CfirCodeFragmentSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/**
 * 只读消费真实 analyzer 产品：测试不写 flow、能力状态或发布证明。
 * 当前未闭合事件完善后，应以其真实 transfer 行为替换相应 control-only 期望。
 */
private object ReadOnlyIdentityMapper : SnapshotCfirMapper {
    override fun <T : CfirBasedSymbol<*>> mapSymbol(symbol: T): T = symbol
    override fun <T : CfirElement> mapElement(element: T): T = element
}

class CfirDataFlowReadOnlyCapabilityTest {
    @Test
    fun `future exit remains uninitialized while completed scalar nodes are semantic ready`() {
        val fixture = Fixture()
        val value = fixture.integer(1)
        val graph = fixture.analyze(listOf(value)) { analyzer, context ->
            assertEquals(DataFlowAnalysisState.UNINITIALIZED, context.graphBuilder.currentGraph.exitNode.dataFlowAnalysisState)
            assertFalse(context.graphBuilder.currentGraph.exitNode.flowInitialized)
            analyzer.exitLiteralExpression(value)
        }
        graph.nodes.forEach {
            assertEquals(DataFlowAnalysisState.SEMANTICS_READY, it.dataFlowAnalysisState)
            assertNotNull(it.flow)
        }
    }

    @Test
    fun `pending Option cast propagates control capability into later scalar nodes`() {
        val fixture = Fixture()
        val operand = fixture.integer(1)
        // N IsAsExprs.cpp:44–50：as 的完成类型是 Option<T>，不是 T。
        val cast = buildTypeOperator {
            operation = CfirTypeOperationKind.AS
            argument = operand
            typeRef = buildResolvedTypeRef { coneType = ConePrimitiveType.INT64 }
            coneTypeOrNull = ConeEnumType(ConeClassLikeLookupTagImpl(StdlibClassIds.Option), listOf(ConePrimitiveType.INT64))
        }
        val after = fixture.integer(2)
        val graph = fixture.analyze(listOf(cast, after)) { analyzer, context ->
            analyzer.exitLiteralExpression(operand)
            analyzer.exitTypeOperatorCall(cast)
            val castNode = context.graphBuilder.lastNode
            assertNotNull(castNode.flow)
            assertEquals(DataFlowAnalysisState.CONTROL_FLOW_READY, castNode.dataFlowAnalysisState)
            analyzer.exitLiteralExpression(after)
            assertEquals(DataFlowAnalysisState.CONTROL_FLOW_READY, context.graphBuilder.lastNode.dataFlowAnalysisState)
        }
        assertEquals(DataFlowAnalysisState.CONTROL_FLOW_READY, graph.exitNode.dataFlowAnalysisState)
    }

    @Test
    fun `snapshot preserves pending capability and the actual immutable cursor`() {
        val fixture = Fixture()
        val operand = fixture.integer(1)
        val cast = buildTypeOperator {
            operation = CfirTypeOperationKind.AS
            argument = operand
            typeRef = buildResolvedTypeRef { coneType = ConePrimitiveType.INT64 }
            coneTypeOrNull = ConeEnumType(ConeClassLikeLookupTagImpl(StdlibClassIds.Option), listOf(ConePrimitiveType.INT64))
        }
        fixture.analyze(listOf(cast)) { analyzer, context ->
            analyzer.exitLiteralExpression(operand)
            analyzer.exitTypeOperatorCall(cast)
            val original = context.graphBuilder.lastNode
            val snapshot = analyzer.captureDataFlowAnalyzerContext(ReadOnlyIdentityMapper)
            val copied = snapshot.context.graphBuilder.lastNode
            assertSame(original.flow, copied.flow)
            assertSame(analyzer.currentSmartCastPosition, snapshot.smartCastPosition)
            assertEquals(DataFlowAnalysisState.CONTROL_FLOW_READY, copied.dataFlowAnalysisState)
            assertEquals(DataFlowAnalysisState.UNINITIALIZED, snapshot.context.graphBuilder.currentGraph.exitNode.dataFlowAnalysisState)
            assertEquals(DataFlowAnalysisState.CONTROL_FLOW_READY, original.dataFlowAnalysisState)
        }
    }

    @Test
    fun `tuple write without value projection cannot be promoted by following literals`() {
        val fixture = Fixture()
        val left = fixture.variable("left", 1)
        val right = fixture.variable("right", 2)
        val first = fixture.integer(3)
        val second = fixture.integer(4)
        val tupleType = ConeTupleType(listOf(ConePrimitiveType.INT64, ConePrimitiveType.INT64))
        val assignment = buildAssignment {
            lValue = buildTupleLiteral {
                elements += listOf(fixture.access(left), fixture.access(right))
                coneTypeOrNull = tupleType
            }
            rValue = buildTupleLiteral {
                elements += listOf(first, second)
                coneTypeOrNull = tupleType
            }
            coneTypeOrNull = fixture.session.builtinTypes.unitType
        }
        val after = fixture.integer(5)
        val graph = fixture.analyze(listOf(left, right, assignment, after)) { analyzer, context ->
            for (variable in listOf(left, right)) {
                analyzer.enterVariableDeclaration(variable)
                analyzer.exitLiteralExpression(checkNotNull(variable.initializer) as org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression)
                analyzer.exitVariableDeclaration(variable)
            }
            analyzer.exitLiteralExpression(first)
            analyzer.exitLiteralExpression(second)
            analyzer.exitVariableAssignment(assignment)
            assertEquals(DataFlowAnalysisState.CONTROL_FLOW_READY, context.graphBuilder.lastNode.dataFlowAnalysisState)
            analyzer.exitLiteralExpression(after)
            assertEquals(DataFlowAnalysisState.CONTROL_FLOW_READY, context.graphBuilder.lastNode.dataFlowAnalysisState)
        }
        assertEquals(DataFlowAnalysisState.CONTROL_FLOW_READY, graph.exitNode.dataFlowAnalysisState)
    }

    @Test
    fun `completed finally retains its required normal alternate product`() {
        val fixture = Fixture()
        val mainValue = fixture.integer(1)
        val cleanup = buildLiteralExpression {
            kind = CfirLiteralKind.UNIT
            value = Unit
            coneTypeOrNull = fixture.session.builtinTypes.unitType
        }
        val attempt = buildTryExpression {
            tryBlock = buildBlock { statements += mainValue; coneTypeOrNull = ConePrimitiveType.INT64 }
            finallyBlock = buildBlock { statements += cleanup; coneTypeOrNull = fixture.session.builtinTypes.unitType }
            coneTypeOrNull = ConePrimitiveType.INT64
        }
        val graph = fixture.analyze(listOf(attempt)) { analyzer, _ ->
            analyzer.enterTryExpression(attempt)
            analyzer.enterBlock(attempt.tryBlock)
            analyzer.exitLiteralExpression(mainValue)
            analyzer.exitBlock(attempt.tryBlock)
            analyzer.exitTryMainBlock()
            analyzer.enterFinallyBlock()
            val finallyBlock = checkNotNull(attempt.finallyBlock)
            analyzer.enterBlock(finallyBlock)
            analyzer.exitLiteralExpression(cleanup)
            analyzer.exitBlock(finallyBlock)
            analyzer.exitFinallyBlock()
            analyzer.exitTryExpression(callCompleted = true)
        }
        val exit = graph.nodes.filterIsInstance<FinallyBlockExitNode>().single()
        val normal = FlowPath.CfgEdge(NormalPath, attempt)
        assertEquals(DataFlowAnalysisState.SEMANTICS_READY, exit.dataFlowAnalysisState)
        assertNotNull(exit.getAlternateFlow(normal))
        assertNotNull(graph.exitNode.flow)
    }

    /** 复用模块的正式 typed session；这里只组织真实节点与事件，不模拟 merge 或 capability。 */
    private class Fixture {
        val session = CallResolutionTestFixtures.newTestSession()
            .apply { register(org.cangnova.cangjie.cfir.session.CfirBuiltinTypes::class, org.cangnova.cangjie.cfir.session.CfirBuiltinTypes()) }

        fun integer(value: Long) = buildLiteralExpression {
            kind = CfirLiteralKind.INT
            this.value = value
            coneTypeOrNull = ConePrimitiveType.INT64
        }

        fun variable(text: String, value: Long): CfirPatternBindingVariable = buildPatternBindingVariable {
            source = binarySource(text)
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            status = CfirDeclarationStatusImpl()
            isLocal = true
            isVar = true
            name = Name.identifier(text)
            symbol = CfirPatternBindingSymbol(CallableId(name))
            returnTypeRef = buildResolvedTypeRef { coneType = ConePrimitiveType.INT64 }
            ownedInitializer = integer(value)
        }

        fun access(variable: CfirPatternBindingVariable) = buildNamedAccessExpression {
            calleeReference = buildResolvedNamedReference { name = variable.name; resolvedSymbol = variable.symbol }
            coneTypeOrNull = ConePrimitiveType.INT64
        }

        fun analyze(
            statements: List<CfirStatement>,
            events: (CfirDataFlowAnalyzer, CfirDataFlowAnalyzerContext) -> Unit,
        ): ControlFlowGraph {
            val block = buildBlock {
                source = binarySource("block")
                this.statements += statements
                coneTypeOrNull = checkNotNull((statements.last() as CfirExpression).coneTypeOrNull)
            }
            val fragment = buildCodeFragment {
                source = binarySource("readOnlyCapability.cj")
                moduleData = session.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                symbol = CfirCodeFragmentSymbol()
                this.block = block
            }
            val file = buildFile {
                source = binarySource("readOnlyCapability.cj")
                moduleData = session.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                symbol = CfirFileSymbol()
                name = "readOnlyCapability.cj"
                packageDirective = buildPackageDirective { packageFqName = FqName.ROOT; isMacroPackage = false }
                declarations += fragment
            }
            val dataFlowContext = CfirDataFlowAnalyzerContext()
            val bodyContext = BodyResolveContext(ReturnTypeCalculatorForFullBodyResolve.Default, dataFlowContext)
            val analyzer = CfirDataFlowAnalyzer(object : SessionHolder { override val session = this@Fixture.session }, bodyContext)
            return bodyContext.withFile(file) {
                bodyContext.withContainer(fragment) {
                    analyzer.enterCodeFragment(fragment)
                    analyzer.enterBlock(block)
                    events(analyzer, dataFlowContext)
                    analyzer.exitBlock(block)
                    analyzer.exitCodeFragment(fragment)
                }
            }
        }
    }
}

private fun binarySource(name: String) = object : CjBinarySourceElement(name, null, name) {}

@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildCodeFragment
import org.cangnova.cangjie.cfir.declarations.builder.buildFile
import org.cangnova.cangjie.cfir.declarations.builder.buildPackageDirective
import org.cangnova.cangjie.cfir.declarations.builder.buildPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.resolve.dfa.LogicSystem
import org.cangnova.cangjie.cfir.resolve.dfa.RealVariable
import org.cangnova.cangjie.cfir.resolve.dfa.typeEq
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.symbols.CfirCodeFragmentSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [CfirDataFlowAnalyzerContext] 快照、重置和引用接管行为测试。
 */
class CfirDataFlowAnalyzerContextTest {
    /**
     * 验证快照会复制 builder 状态并保留 assignment 计数器。
     */
    @Test
    fun `createSnapshot copies builder state and keeps assignment counter`() {
        val context = CfirDataFlowAnalyzerContext()
        val fixture = buildCodeFragmentWithLiteral()
        analyzeFragment(context, fixture)

        assertEquals(0, context.newAssignmentIndex())
        assertEquals(1, context.newAssignmentIndex())

        val snapshot = context.createSnapshot(IdentitySnapshotCfirMapper, context.graphBuilder.lastNode.flow)

        assertFalse(snapshot.graphMapping.isEmpty())
        assertNotSame(context.graphBuilder, snapshot.context.graphBuilder)
        assertNotSame(context.variableAssignmentAnalyzer, snapshot.context.variableAssignmentAnalyzer)
        val originalGraph = context.graphBuilder.currentGraph
        val copiedGraph = snapshot.graphMapping.getValue(originalGraph)
        assertNotSame(originalGraph, copiedGraph)
        val originalNode = context.graphBuilder.lastNode
        val copiedNode = snapshot.context.graphBuilder.lastNode
        assertNotSame(originalNode, copiedNode)
        assertSame(copiedGraph, copiedNode.owner)
        assertSame(originalNode.flow, copiedNode.flow)
        val real = fixture.realVariable()
        val registered = copiedNode.flow.getVariableIfKnown(real)
        assertNotNull(registered)
        assertSame(fixture.variable.symbol, registered?.symbol)
        assertEquals(fixture.intType, registered?.originalType)
        assertSame(originalNode.flow.getVariableIfKnown(real), registered)
        assertSame(originalGraph.enterNode.flow,
            originalGraph.enterNode.flow.lowestCommonAncestor(copiedNode.flow))
        // 固定 K：节点复制共享 immutable Flow；fork 中的新赋值不得污染任一原快照。
        val fork = copiedNode.flow.fork()
        val peer = originalNode.flow.fork()
        val logic = object : LogicSystem(fixture.session.typeContext) {}
        assertEquals(2, snapshot.context.newAssignmentIndex())
        val assignmentIndex = snapshot.context.newAssignmentIndex()
        assertEquals(3, assignmentIndex)
        logic.recordNewAssignment(fork, real, assignmentIndex)
        logic.addTypeStatement(fork, real typeEq fixture.intType)
        assertEquals(setOf(fixture.intType), fork.getTypeStatement(real)?.upperTypes)
        assertFalse(logic.isSameValueIn(copiedNode.flow, fork, real))
        assertTrue(logic.isSameValueIn(originalNode.flow, peer, real))
        assertNull(originalNode.flow.getTypeStatement(real))
        assertNull(copiedNode.flow.getTypeStatement(real))
        assertNull(peer.getTypeStatement(real))
        assertEquals(2, context.newAssignmentIndex())
    }

    /**
     * 验证 reset 不会清零 assignment 计数器。
     */
    @Test
    fun `reset does not zero assignment counter`() {
        val context = CfirDataFlowAnalyzerContext()
        assertEquals(0, context.newAssignmentIndex())

        context.reset()

        assertEquals(1, context.newAssignmentIndex())
    }

    /**
     * 验证 resetFrom 会接管源上下文中的共享引用。
     */
    @Test
    fun `resetFrom takes over source references`() {
        val source = CfirDataFlowAnalyzerContext()
        val target = CfirDataFlowAnalyzerContext()
        val fixture = buildCodeFragmentWithLiteral()
        analyzeFragment(source, fixture)
        source.newAssignmentIndex()

        target.resetFrom(source)

        assertSame(source.graphBuilder, target.graphBuilder)
        assertSame(source.variableAssignmentAnalyzer, target.variableAssignmentAnalyzer)
        assertSame(source.graphBuilder.lastNode, target.graphBuilder.lastNode)
        assertSame(source.graphBuilder.lastNode.flow, target.graphBuilder.lastNode.flow)
        val real = fixture.realVariable()
        assertSame(source.graphBuilder.lastNode.flow.getVariableIfKnown(real),
            target.graphBuilder.lastNode.flow.getVariableIfKnown(real))
        assertSame(fixture.variable.symbol, target.graphBuilder.lastNode.flow.getVariableIfKnown(real)?.symbol)
        assertEquals(fixture.intType, target.graphBuilder.lastNode.flow.getVariableIfKnown(real)?.originalType)
        assertEquals(1, target.newAssignmentIndex())
    }

    /**
     * 构造保留原 literal 的 typed code fragment，并登记一个真实局部值供 Flow 快照查询。
     */
    private fun buildCodeFragmentWithLiteral(): FragmentFixture {
        val session = CallResolutionTestFixtures.newTestSession()
        session.register(org.cangnova.cangjie.cfir.session.CfirBuiltinTypes::class, org.cangnova.cangjie.cfir.session.CfirBuiltinTypes())
        val intType = ConePrimitiveType(PrimitiveTypeKind.INT64)
        val literal = buildLiteralExpression {
            source = TestBinarySourceElement("literal")
            kind = CfirLiteralKind.INT
            value = 1L
            coneTypeOrNull = intType
        }
        val variable = buildPatternBindingVariable {
            source = TestBinarySourceElement("value")
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            status = CfirDeclarationStatusImpl()
            isLocal = true
            name = Name.identifier("value")
            symbol = CfirPatternBindingSymbol(CallableId(name))
            returnTypeRef = buildResolvedTypeRef { coneType = intType }
            isVar = true
            ownedInitializer = buildLiteralExpression {
                source = TestBinarySourceElement("initializer")
                kind = CfirLiteralKind.INT; value = 2L; coneTypeOrNull = intType
            }
        }
        val symbol = CfirCodeFragmentSymbol()
        val fragment = buildCodeFragment {
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            source = TestBinarySourceElement("test code fragment")
            this.symbol = symbol
            block = buildBlock {
                source = TestBinarySourceElement("block")
                statements += listOf(variable, literal)
                coneTypeOrNull = intType
            }
        }
        symbol.bind(fragment)
        val file = buildFile {
            source = TestBinarySourceElement("fragment.cj")
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            name = "fragment.cj"
            this.symbol = CfirFileSymbol()
            packageDirective = buildPackageDirective { packageFqName = FqName.ROOT; isMacroPackage = false }
            declarations += fragment
        }
        return FragmentFixture(session, file, fragment, literal, variable, intType)
    }

    /** 使用 resolve.test 已有正式 fixture 的 typed 组件，所有节点由真实 analyzer 完成流。 */
    private fun analyzeFragment(context: CfirDataFlowAnalyzerContext, fixture: FragmentFixture) {
        val holder = object : SessionHolder { override val session = fixture.session }
        val bodyContext = BodyResolveContext(ReturnTypeCalculatorForFullBodyResolve.Default, context)
        val analyzer = CfirDataFlowAnalyzer(holder, bodyContext)
        bodyContext.withFile(fixture.file) {
            bodyContext.withContainer(fixture.fragment) {
                analyzer.enterCodeFragment(fixture.fragment)
                analyzer.enterBlock(fixture.fragment.block)
                analyzer.enterVariableDeclaration(fixture.variable)
                analyzer.exitLiteralExpression(checkNotNull(fixture.variable.initializer) as CfirLiteralExpression)
                analyzer.exitVariableDeclaration(fixture.variable)
                analyzer.exitLiteralExpression(fixture.literal)
                analyzer.exitBlock(fixture.fragment.block)
            }
        }
        assertSame(context.graphBuilder.lastNode.flow, analyzer.currentSmartCastPosition)
    }

    private data class FragmentFixture(
        val session: CallResolutionTestFixtures.TestSession,
        val file: org.cangnova.cangjie.cfir.declarations.CfirFile,
        val fragment: org.cangnova.cangjie.cfir.declarations.CfirCodeFragment,
        val literal: CfirLiteralExpression,
        val variable: CfirPatternBindingVariable,
        val intType: ConePrimitiveType,
    ) {
        fun realVariable(): RealVariable = RealVariable(variable.symbol, false, null, null, intType)
    }

    /**
     * 带稳定 debug identity 的二进制 source element。
     */
    private class TestBinarySourceElement(identity: String) : CjBinarySourceElement(
        debugText = identity,
        binaryFilePath = null,
        stableIdentity = identity,
    )

    /**
     * 不改变 symbol 与 element 身份的快照 mapper。
     */
    private object IdentitySnapshotCfirMapper : SnapshotCfirMapper {
        /**
         * 原样返回传入 symbol。
         */
        override fun <T : org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol<*>> mapSymbol(symbol: T): T = symbol

        /**
         * 原样返回传入 element。
         */
        override fun <T : org.cangnova.cangjie.cfir.CfirElement> mapElement(element: T): T = element
    }
}

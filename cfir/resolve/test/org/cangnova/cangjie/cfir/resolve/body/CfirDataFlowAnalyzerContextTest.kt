@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.builder.buildCodeFragment
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.symbols.CfirCodeFragmentSymbol
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
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
        val (fragment, literal) = buildCodeFragmentWithLiteral()

        context.graphBuilder.enterCodeFragment(fragment)
        context.variableAssignmentAnalyzer.enterCodeFragment(fragment)
        context.graphBuilder.enterBlock(fragment.block)
        context.graphBuilder.exitLiteralExpression(literal)
        context.graphBuilder.exitBlock(fragment.block)

        assertEquals(0, context.newAssignmentIndex())
        assertEquals(1, context.newAssignmentIndex())

        val snapshot = context.createSnapshot(IdentitySnapshotCfirMapper)

        assertFalse(snapshot.graphMapping.isEmpty())
        assertNotSame(context.graphBuilder, snapshot.context.graphBuilder)
        assertNotSame(context.variableStorage, snapshot.context.variableStorage)
        assertEquals(2, snapshot.context.newAssignmentIndex())
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
        val (fragment, literal) = buildCodeFragmentWithLiteral()

        source.graphBuilder.enterCodeFragment(fragment)
        source.variableAssignmentAnalyzer.enterCodeFragment(fragment)
        source.graphBuilder.enterBlock(fragment.block)
        source.graphBuilder.exitLiteralExpression(literal)
        source.graphBuilder.exitBlock(fragment.block)
        source.newAssignmentIndex()

        target.resetFrom(source)

        assertSame(source.graphBuilder, target.graphBuilder)
        assertSame(source.variableAssignmentAnalyzer, target.variableAssignmentAnalyzer)
        assertSame(source.variableStorage, target.variableStorage)
        assertEquals(1, target.newAssignmentIndex())
    }

    /**
     * 构造包含单个 literal 的 code fragment，用于 data-flow 图构建测试。
     */
    private fun buildCodeFragmentWithLiteral(): Pair<org.cangnova.cangjie.cfir.declarations.CfirCodeFragment, CfirLiteralExpression> {
        val session = CallResolutionTestFixtures.newTestSession()
        val literal = buildLiteralExpression {
            kind = CfirLiteralKind.INT
            value = 1
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
                statements += literal
            }
        }
        symbol.bind(fragment)
        return fragment to literal
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

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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class CfirDataFlowAnalyzerContextTest {
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

    @Test
    fun `reset does not zero assignment counter`() {
        val context = CfirDataFlowAnalyzerContext()
        assertEquals(0, context.newAssignmentIndex())

        context.reset()

        assertEquals(1, context.newAssignmentIndex())
    }

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

    private fun buildCodeFragmentWithLiteral(): Pair<org.cangnova.cangjie.cfir.declarations.CfirCodeFragment, CfirLiteralExpression> {
        val literal = buildLiteralExpression {
            kind = CfirLiteralKind.INT
            value = 1
        }
        val symbol = CfirCodeFragmentSymbol()
        val fragment = buildCodeFragment {
            moduleData = CallResolutionTestFixtures.TEST_MODULE_DATA
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            this.symbol = symbol
            block = buildBlock {
                statements += literal
            }
        }
        symbol.bind(fragment)
        return fragment to literal
    }

    private object IdentitySnapshotCfirMapper : SnapshotCfirMapper {
        override fun <T : org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol<*>> mapSymbol(symbol: T): T = symbol

        override fun <T : org.cangnova.cangjie.cfir.CfirElement> mapElement(element: T): T = element
    }
}

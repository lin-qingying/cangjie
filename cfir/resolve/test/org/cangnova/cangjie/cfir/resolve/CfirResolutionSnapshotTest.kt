package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildErrorExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/** 快照恢复真实可变类型状态，不向错误表达式的派生类型 getter 写回。 */
class CfirResolutionSnapshotTest {
    @Test
    fun `restore keeps a fixed error diagnostic and restores sibling types`() {
        val diagnostic = ConeSimpleDiagnostic("unexpanded macro")
        val error = buildErrorExpression { this.diagnostic = diagnostic }
        val literal = buildLiteralExpression {
            kind = CfirLiteralKind.INT
            value = 1
            coneTypeOrNull = ConePrimitiveType.INT32
        }
        val block = buildBlock { statements.addAll(listOf(error, literal)) }
        val snapshot = CfirResolutionSnapshot.capture(block)

        repeat(2) {
            literal.replaceConeTypeOrNull(ConePrimitiveType.INT64)
            snapshot.restore()
            assertEquals(ConePrimitiveType.INT32, literal.coneTypeOrNull)
            val restoredDiagnostic = (error.coneTypeOrNull as ConeErrorType).diagnostic as ConeUnreportedDuplicateDiagnostic
            assertSame(diagnostic, restoredDiagnostic.original)
        }
    }

    @Test
    fun `restore visits the expression supplying an error wrapper type`() {
        val literal = buildLiteralExpression {
            kind = CfirLiteralKind.INT
            value = 1
            coneTypeOrNull = ConePrimitiveType.INT32
        }
        val diagnostic = ConeSimpleDiagnostic("erroneous wrapper")
        val error = buildErrorExpression {
            this.diagnostic = diagnostic
            expression = literal
        }
        val snapshot = CfirResolutionSnapshot.capture(error)
        literal.replaceConeTypeOrNull(ConePrimitiveType.INT64)

        snapshot.restore()

        assertEquals(ConePrimitiveType.INT32, error.coneTypeOrNull)
        assertSame(literal, error.expression)
        assertSame(diagnostic, error.diagnostic)
    }
}

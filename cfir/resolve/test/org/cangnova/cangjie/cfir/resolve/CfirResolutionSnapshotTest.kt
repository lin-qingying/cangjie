package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostic.ConeInvalidCatchTypeError
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildErrorExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.patterns.builder.buildCatchPattern
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** 快照恢复真实可变类型状态，不向错误表达式的派生类型 getter 写回。 */
class CfirResolutionSnapshotTest {
    /** 候选试跑检查了 catch 后，回滚必须恢复原类型引用和“尚未检查”的整体状态。 */
    @Test
    fun `restore discards catch pattern typing from a failed candidate`() {
        val originalType = ConePrimitiveType.INT64.toCfirResolvedTypeRef()
        val pattern = buildCatchPattern {
            isWildcard = true
            typeRefs += originalType
        }
        val snapshot = CfirResolutionSnapshot.capture(pattern)
        repeat(2) {
            val errorType = ConeErrorType(ConeInvalidCatchTypeError).toCfirResolvedTypeRef()
            pattern.replaceTypeRefs(listOf(errorType))
            pattern.replaceResolvedTypeRef(errorType)
            snapshot.restore()
            assertNull(pattern.resolvedTypeRef)
            assertEquals(1, pattern.typeRefs.size)
            assertSame(originalType, pattern.typeRefs.single())
        }
    }

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

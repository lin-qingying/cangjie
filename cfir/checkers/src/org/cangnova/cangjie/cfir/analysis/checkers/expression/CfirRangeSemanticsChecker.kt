package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.checkTypeMismatch
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirRangeExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.IdealTypeResolver
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * 对齐官方 `CheckRangeElements`：
 * - start/stop 按推断或 target element type 做逐项检查；
 * - step 必须是 `Int64`；
 * - 常量 step 不能是 0，诊断落在 step 表达式本身。
 */
object CfirRangeSemanticsChecker : CfirBasicExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: org.cangnova.cangjie.cfir.expressions.CfirStatement) {
        val rangeExpression = expression as? CfirRangeExpression ?: return
        val elementType = rangeExpression.expectedElementTypeFromParentCall(context)
            ?: rangeExpression.coneTypeOrNull.rangeElementTypeOrNull()
            ?: inferRangeElementType(rangeExpression)

        checkEndpoint(rangeExpression.start, elementType)
        checkEndpoint(rangeExpression.end, elementType)
        checkStep(rangeExpression.step, ConePrimitiveType.INT64)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkEndpoint(expression: CfirExpression, expectedType: ConeCangJieType) {
        val source = expression.source as? AbstractCjSourceElement ?: return
        val actualType = expression.coneTypeOrNull ?: return
        if (actualType is ConeErrorType || expectedType is ConeErrorType) return

        checkTypeMismatch(
            expectedType = expectedType,
            actualType = actualType,
            source = source,
            preferredSpecializedSource = source,
            diagnosticFactory = CfirErrors.TYPE_MISMATCH,
        )
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkStep(stepExpression: CfirExpression?, expectedType: ConeCangJieType) {
        val stepExpression = stepExpression ?: return
        val source = stepExpression.source as? AbstractCjSourceElement ?: return
        val actualType = stepExpression.coneTypeOrNull ?: return
        if (actualType is ConeErrorType || expectedType is ConeErrorType) return

        checkTypeMismatch(
            expectedType = expectedType,
            actualType = actualType,
            source = source,
            preferredSpecializedSource = source,
            diagnosticFactory = CfirErrors.TYPE_MISMATCH,
        )

        if (actualType is ConeErrorType) return
        val parsed = CfirIntConstantEvalUtils.parseSignedIntExpression(stepExpression) ?: return
        if (parsed.value.signum() == 0) {
            reporter.reportOn(source, CfirErrors.RANGE_STEP_CANNOT_BE_ZERO)
        }
    }

    private fun inferRangeElementType(rangeExpression: CfirRangeExpression): ConeCangJieType {
        val startType = rangeExpression.start.coneTypeOrNull
        val useStartType = rangeExpression.start !is CfirLiteralExpression || rangeExpression.end is CfirLiteralExpression
        if (startType != null && startType !is ConeErrorType && !startType.isNothing && useStartType) {
            return normalizeRangeElementType(startType)
        }

        val endType = rangeExpression.end.coneTypeOrNull
        if (endType != null && endType !is ConeErrorType && !endType.isNothing) {
            return normalizeRangeElementType(endType)
        }

        if (startType != null && startType !is ConeErrorType) {
            return normalizeRangeElementType(startType)
        }

        return ConePrimitiveType.INT64
    }

    private fun normalizeRangeElementType(type: ConeCangJieType): ConeCangJieType {
        val normalized = IdealTypeResolver.resolveIfIdeal(type, null)
        return if (normalized is ConePrimitiveType && normalized.kind == PrimitiveTypeKind.IDEAL_INT) {
            ConePrimitiveType.INT64
        } else {
            normalized
        }
    }

}

private fun CfirRangeExpression.expectedElementTypeFromParentCall(context: CheckerContext): ConeCangJieType? {
    val parentCall = context.callsOrAssignments.asReversed().firstOrNull() as? CfirFunctionCall ?: return null
    val argumentIndex = parentCall.argumentList.arguments.indexOfFirst { it === this }
    if (argumentIndex < 0) return null

    val appliedReference = parentCall.calleeReference as? CfirResolvedAppliedCallableReference ?: return null
    return appliedReference.substitutedParameterTypes.getOrNull(argumentIndex)?.rangeElementTypeOrNull()
}

private fun ConeCangJieType?.rangeElementTypeOrNull(): ConeCangJieType? = when (this) {
    is ConeClassLikeType -> if (classId == StdlibClassIds.Range) typeArguments.singleOrNull()?.type else null
    is ConeStructType -> if (classId == StdlibClassIds.Range) typeArguments.singleOrNull()?.type else null
    is ConeTypeAliasType -> expandedType.rangeElementTypeOrNull()
    else -> null
}

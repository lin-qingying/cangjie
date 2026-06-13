package org.cangnova.cangjie.cfir.analysis.checkers.expression

import java.math.BigInteger
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.constants.CfirIntConstantEvalUtils
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * 复合赋值的内建语义检查。
 *
 * 官方 Sema 在 `AssignExpr` 层处理 `COMPOUND_ASSIGN_EXPR_MAP`：
 * 先按左值类型约束右值，再对 `<<=` / `>>=` 做移位计数检查。
 * 当前 CFIR 尚未引入独立的 AugmentedAssignment 节点，因此这里识别 raw builder
 * 生成的 `lValue = lValue <op> rValue` 承载形态，补齐同一层语义。
 */
object CfirCompoundAssignmentSemanticsChecker : CfirAssignmentChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirAssignment) {
        val call = expression.compoundAssignmentCall() ?: return
        val operatorName = call.operatorName() ?: return
        val leftType = expression.lValue.coneTypeOrNull as? ConePrimitiveType ?: return
        val leftKind = leftType.kind

        val allowedKinds = allowedCompoundAssignmentKinds(operatorName) ?: return
        if (leftKind !in allowedKinds) {
            val source = expression.lValue.source as? AbstractCjSourceElement ?: return
            reporter.reportOn(
                source,
                CfirErrors.TYPE_INCOMPATIBLE,
                "compound assignment expression",
            )
            return
        }

        val rightExpression = call.argumentList.arguments.singleOrNull()
        if (checkRightLiteralRange(rightExpression, leftType)) return
        if (operatorName == OperatorNameConventions.LEFT_SHIFT || operatorName == OperatorNameConventions.RIGHT_SHIFT) {
            checkShiftCount(rightExpression, leftKind)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkRightLiteralRange(rightExpression: CfirExpression?, targetType: ConePrimitiveType): Boolean {
        val parsed = rightExpression?.let(CfirIntConstantEvalUtils::parseSignedIntExpression) ?: return false
        if (parsed.explicitSuffix != null) return false

        val range = CfirIntConstantEvalUtils.rangeForLiteralTargetType(targetType) ?: return false
        if (range.contains(parsed.value)) return false

        val source = rightExpression.source as? AbstractCjSourceElement ?: return false
        reporter.reportOn(
            source,
            CfirErrors.LITERAL_NUMERIC_OVERFLOW,
            parsed.originalText,
            targetType,
        )
        return true
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkShiftCount(rightExpression: CfirExpression?, leftKind: PrimitiveTypeKind) {
        val parsed = rightExpression?.let(CfirIntConstantEvalUtils::parseSignedIntExpression) ?: return
        val source = rightExpression.source as? AbstractCjSourceElement ?: return

        if (parsed.value < BigInteger.ZERO) {
            reporter.reportOn(source, CfirErrors.CONST_EVAL_NEGATIVE_SHIFT_COUNT)
            return
        }

        val bitWidth = CfirIntConstantEvalUtils.bitWidthForIntegerType(ConePrimitiveType(leftKind)) ?: return
        if (parsed.value >= BigInteger.valueOf(bitWidth.toLong())) {
            reporter.reportOn(source, CfirErrors.CONST_EVAL_SHIFT_COUNT_OVERFLOW)
        }
    }
}

internal fun CfirAssignment.compoundAssignmentCall(): CfirFunctionCall? {
    val call = rValue as? CfirFunctionCall ?: return null
    val assignmentSource = source ?: return null
    val callSource = call.source ?: return null
    if (assignmentSource != callSource) return null
    if (call.operatorName() !in COMPOUND_ASSIGNMENT_OPERATOR_NAMES) return null
    return call
}

internal fun CfirFunctionCall.isInvalidPrimitiveCompoundAssignmentCall(context: CheckerContext): Boolean {
    val operatorName = operatorName() ?: return false
    val assignment = primitiveCompoundAssignment(context) ?: return false
    val leftKind = (assignment.lValue.coneTypeOrNull as? ConePrimitiveType)?.kind ?: return false
    val allowedKinds = allowedCompoundAssignmentKinds(operatorName) ?: return false
    return leftKind !in allowedKinds
}

/**
 * 当前 raw CFIR 用普通函数调用承载复合赋值右侧的 desugared operator call。
 * 赋值语义由 [CfirCompoundAssignmentSemanticsChecker] 统一处理，函数调用检查器据此避免重复上报。
 */
internal fun CfirFunctionCall.isPrimitiveCompoundAssignmentCall(context: CheckerContext): Boolean {
    val operatorName = operatorName() ?: return false
    primitiveCompoundAssignment(context) ?: return false
    return allowedCompoundAssignmentKinds(operatorName) != null
}

private fun CfirFunctionCall.primitiveCompoundAssignment(context: CheckerContext): CfirAssignment? {
    val assignment = context.callsOrAssignments.asReversed()
        .filterIsInstance<CfirAssignment>()
        .firstOrNull { it.rValue === this && it.compoundAssignmentCall() === this }
        ?: return null
    if (assignment.lValue.coneTypeOrNull !is ConePrimitiveType) return null
    return assignment
}

private val COMPOUND_ASSIGNMENT_OPERATOR_NAMES: Set<Name> = setOf(
    OperatorNameConventions.PLUS,
    OperatorNameConventions.MINUS,
    OperatorNameConventions.TIMES,
    OperatorNameConventions.DIV,
    OperatorNameConventions.REM,
    OperatorNameConventions.EXPONENTIATION,
    OperatorNameConventions.ANDAND,
    OperatorNameConventions.OROR,
    OperatorNameConventions.AND,
    OperatorNameConventions.OR,
    OperatorNameConventions.XOR,
    OperatorNameConventions.LEFT_SHIFT,
    OperatorNameConventions.RIGHT_SHIFT,
)

private val INTEGER_COMPOUND_ASSIGNMENT_KINDS: Set<PrimitiveTypeKind> =
    PrimitiveTypeKind.entries.filterTo(linkedSetOf()) { it.isInteger }

private val NUMERIC_COMPOUND_ASSIGNMENT_KINDS: Set<PrimitiveTypeKind> =
    PrimitiveTypeKind.entries.filterTo(linkedSetOf()) { it.isNumeric }

private fun allowedCompoundAssignmentKinds(operatorName: Name): Set<PrimitiveTypeKind>? = when (operatorName) {
    OperatorNameConventions.PLUS,
    OperatorNameConventions.MINUS,
    OperatorNameConventions.TIMES,
    OperatorNameConventions.DIV,
    OperatorNameConventions.EXPONENTIATION,
    -> NUMERIC_COMPOUND_ASSIGNMENT_KINDS

    OperatorNameConventions.REM,
    OperatorNameConventions.AND,
    OperatorNameConventions.OR,
    OperatorNameConventions.XOR,
    OperatorNameConventions.LEFT_SHIFT,
    OperatorNameConventions.RIGHT_SHIFT,
    -> INTEGER_COMPOUND_ASSIGNMENT_KINDS

    OperatorNameConventions.ANDAND,
    OperatorNameConventions.OROR,
    -> setOf(PrimitiveTypeKind.BOOLEAN)

    else -> null
}

private fun CfirFunctionCall.operatorName(): Name? {
    return when (val reference = calleeReference) {
        is CfirResolvedNamedReference -> reference.name
        is CfirNamedReference -> reference.name
        else -> null
    }
}

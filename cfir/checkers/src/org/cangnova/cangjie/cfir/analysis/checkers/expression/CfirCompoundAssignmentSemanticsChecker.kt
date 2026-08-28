package org.cangnova.cangjie.cfir.analysis.checkers.expression

import java.math.BigInteger
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.firstCharacterDiagnosticSource
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirRangeExpression
import org.cangnova.cangjie.cfir.expressions.CfirSubscriptExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.constants.CfirIntConstantEvalUtils
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.arrayElementType
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 复合赋值的内建语义检查。
 *
 * 官方 Sema 在 `AssignExpr` 层处理 `COMPOUND_ASSIGN_EXPR_MAP`：
 * 先按左值类型约束右值，再对 `<<=` / `>>=` 做移位计数检查。
 * raw CFIR 使用独立的 [org.cangnova.cangjie.cfir.expressions.CfirAugmentedAssignment]
 * 保留复合赋值语法；body resolve 解糖为带 [CfirAssignment.augmentedOperation] 来源信息的
 * 普通赋值后，本 checker 在 AssignExpr 层完成复合赋值专有的语义诊断。
 */
object CfirCompoundAssignmentSemanticsChecker : CfirAssignmentChecker() {
    /**
     * 检查复合赋值 desugar 后的内建运算语义。
     *
     * 该入口只消费 body resolve 为复合赋值保留来源信息的普通赋值；再基于左值原始类型判断
     * operator 是否可用，并对字面量范围和移位计数执行官方约束。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirAssignment) {
        val call = expression.compoundAssignmentCall() ?: return
        val operatorName = expression.augmentedOperation ?: return
        if (expression.lValue.isBuiltinArrayRangeSubscript(context)) return
        val leftType = expression.lValue.coneTypeOrNull
        if (leftType !is ConePrimitiveType) {
            // 重载解析失败时，operator call 自己拥有 INVALID_BINARY_OPERATOR 等语义诊断；
            // AssignExpr 只能在调用成功、但返回值不能写回左值时报告 TYPE_INCOMPATIBLE。
            if (call.coneTypeOrNull is ConeErrorType) return
            if (!expression.hasApplicableNonPrimitiveCompoundOperation(context)) {
                reportCompoundAssignmentTypeIncompatible(expression)
            }
            return
        }
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

    /**
     * 复合赋值必须在其专有 precheck 层报告类型不兼容，而不能把解糖后 operator 的结果
     * 当作普通赋值 RHS 产生 `TYPE_MISMATCH`。官方定位在赋值左侧的首字符。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportCompoundAssignmentTypeIncompatible(expression: CfirAssignment) {
        val source = expression.source as? AbstractCjSourceElement ?: return
        reporter.reportOn(
            source.firstCharacterDiagnosticSource(),
            CfirErrors.TYPE_INCOMPATIBLE,
            "compound assignment expression",
        )
    }

    /**
     * 检查复合赋值右侧无后缀整数字面量是否超出左值类型范围。
     *
     * 返回 `true` 表示已报告溢出诊断，调用方应停止后续移位计数检查，避免同一右值产生重复诊断。
     */
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

    /**
     * 检查复合移位赋值的常量移位计数。
     *
     * 负数移位和大于等于左操作数位宽的移位分别对应官方常量求值诊断。
     */
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

/**
 * Array 的 Range 下标复合赋值由 subscript set 语义负责。
 *
 * 官方 AssignExpr 在判断下标可赋值性失败后直接报告
 * `CANNOT_ASSIGN_TO_SUBSCRIPT`，不会再把 `Array<T> += Array<T>` 作为普通复合运算
 * 报告 `TYPE_INCOMPATIBLE`。该判断只描述内建 Array 的 Range 形态，用户自定义下标
 * 仍继续经过普通 operator-overload 复合赋值检查。
 */
private fun CfirExpression.isBuiltinArrayRangeSubscript(context: CheckerContext): Boolean {
    val subscript = this as? CfirSubscriptExpression ?: return false
    if (subscript.indices.none { it is CfirRangeExpression }) return false
    return subscript.receiver.coneTypeOrNull
        ?.fullyExpandedType(context.session)
        ?.arrayElementType != null
}

/**
 * 识别 body resolve 后带有复合赋值来源的 operator call。
 *
 * body resolve 只为 [org.cangnova.cangjie.cfir.expressions.CfirAugmentedAssignment] 生成带
 * [CfirAssignment.augmentedOperation] 的普通赋值；该 provenance 是复合赋值语义的唯一来源。
 */
internal fun CfirAssignment.compoundAssignmentCall(): CfirFunctionCall? {
    val operation = augmentedOperation ?: return null
    val call = rValue as? CfirFunctionCall ?: return null
    if (operation !in COMPOUND_ASSIGNMENT_OPERATOR_NAMES) return null
    return call
}

/**
 * 判断非内建复合赋值的 operator-overload 解糖是否已完整成功。
 *
 * 官方在 `InferAssignExprCheckCaseOverloading` 中只有当 `lhs.op(rhs)` 已成功解析、但其结果
 * 不能写回 lhs 时，才由 AssignExpr 报告 `TYPE_INCOMPATIBLE`。调用本身解析失败时，必须保留
 * operator call 的 `INVALID_BINARY_OPERATOR` 等根诊断，不能改写成赋值不兼容。
 */
private fun CfirAssignment.hasApplicableNonPrimitiveCompoundOperation(context: CheckerContext): Boolean {
    val resultType = compoundAssignmentCall()?.coneTypeOrNull ?: return false
    if (resultType is ConeErrorType) return false
    val leftType = lValue.coneTypeOrNull ?: return false
    return AbstractTypeChecker.isSubtypeOf(context.session.typeContext, resultType, leftType) == true
}

/**
 * 判断函数调用是否是非法 primitive 复合赋值调用。
 *
 * 该函数供函数调用检查器跳过重复类型诊断时使用：若所属赋值左值的 primitive kind 不在
 * 该 operator 的允许集合内，则由复合赋值检查器统一报告。
 */
internal fun CfirFunctionCall.isInvalidPrimitiveCompoundAssignmentCall(context: CheckerContext): Boolean {
    val operatorName = operatorName() ?: return false
    val assignment = compoundAssignment(context) ?: return false
    val leftKind = (assignment.lValue.coneTypeOrNull as? ConePrimitiveType)?.kind ?: return false
    val allowedKinds = allowedCompoundAssignmentKinds(operatorName) ?: return false
    return leftKind !in allowedKinds
}

/**
 * 判断当前 operator 调用是否属于应由 AssignExpr 统一报告的非法复合赋值。
 *
 * 内建左值由 operator 类型表判定；非内建左值只有完整重载结果可回写时才合法。该判定供
 * error collector 抑制解糖调用的底层错误，保证用户只看到官方 AssignExpr 语义的
 * `TYPE_INCOMPATIBLE`。
 */
internal fun CfirFunctionCall.isInvalidCompoundAssignmentCall(context: CheckerContext): Boolean {
    val assignment = compoundAssignment(context) ?: return false
    val leftType = assignment.lValue.coneTypeOrNull
    return when (leftType) {
        is ConePrimitiveType -> {
            val operatorName = operatorName() ?: return false
            operatorName in COMPOUND_ASSIGNMENT_OPERATOR_NAMES &&
                leftType.kind !in (allowedCompoundAssignmentKinds(operatorName) ?: return false)
        }

        null -> false
        else -> {
            val resultType = assignment.compoundAssignmentCall()?.coneTypeOrNull ?: return false
            resultType !is ConeErrorType && !assignment.hasApplicableNonPrimitiveCompoundOperation(context)
        }
    }
}

/**
 * body resolve 后的函数调用承载复合赋值的 operator call。
 * 赋值语义由 [CfirCompoundAssignmentSemanticsChecker] 统一处理，函数调用检查器据此避免重复上报。
 */
internal fun CfirFunctionCall.isPrimitiveCompoundAssignmentCall(context: CheckerContext): Boolean {
    val operatorName = operatorName() ?: return false
    compoundAssignment(context) ?: return false
    return allowedCompoundAssignmentKinds(operatorName) != null
}

/**
 * 取得当前函数调用所属的 primitive 复合赋值表达式。
 *
 * 该匹配依赖检查上下文中的调用/赋值栈，确保只把当前调用作为赋值右值时才返回对应赋值。
 */
private fun CfirFunctionCall.compoundAssignment(context: CheckerContext): CfirAssignment? {
    val assignment = context.callsOrAssignments.asReversed()
        .filterIsInstance<CfirAssignment>()
        .firstOrNull { it.rValue === this && it.compoundAssignmentCall() === this }
        ?: return null
    return assignment
}

/**
 * 复合赋值可使用的 operator 名称集合。
 */
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

/**
 * 整数类复合赋值运算允许的 primitive kind 集合。
 */
private val INTEGER_COMPOUND_ASSIGNMENT_KINDS: Set<PrimitiveTypeKind> =
    PrimitiveTypeKind.entries.filterTo(linkedSetOf()) { it.isInteger }

/**
 * 数值类复合赋值运算允许的 primitive kind 集合。
 */
private val NUMERIC_COMPOUND_ASSIGNMENT_KINDS: Set<PrimitiveTypeKind> =
    PrimitiveTypeKind.entries.filterTo(linkedSetOf()) { it.isNumeric }

/**
 * 根据 operator 名称返回可参与该复合赋值的左值 primitive kind 集合。
 *
 * 返回 `null` 表示该调用不是当前检查器负责的复合赋值运算。
 */
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

/**
 * 从函数调用引用中提取 operator 名称。
 *
 * 已解析引用和未完全解析的命名引用都保留了调用名，其他引用形态不参与复合赋值识别。
 */
private fun CfirFunctionCall.operatorName(): Name? {
    return when (val reference = calleeReference) {
        is CfirResolvedNamedReference -> reference.name
        is CfirNamedReference -> reference.name
        else -> null
    }
}

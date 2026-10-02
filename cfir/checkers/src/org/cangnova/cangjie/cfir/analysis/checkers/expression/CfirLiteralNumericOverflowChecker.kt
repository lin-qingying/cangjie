package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors

import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirAssignmentTypeMismatchPrimaryDiagnostic
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.constants.CfirIntConstantEvalUtils
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.containsErrorType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions

/**
 * 整数字面量范围检查器。
 *
 * 符号随字面量的 value 一起进入 CFIR（官方 `ParseNegativeLiteral`、Kotlin psi2fir 折叠），
 * 因此这里只按最终值选有符号或无符号值域，不再回扫源码文本：`-Int64.MIN_VALUE` 是合法的 Int64 下界，
 * 而不能先把内部正数字面量单独报溢出。
 */
object CfirLiteralNumericOverflowChecker : CfirLiteralExpressionChecker() {
    /**
     * 检查整数字面量是否超出显式后缀、上下文目标类型或默认 Int64 范围。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirLiteralExpression) {
        if (context.isAssignmentSpecificLiteralConversion(expression)) return
        if (context.hasTypeConversionOverflow(expression.source)) return

        val source = expression.source as? AbstractCjSourceElement ?: return
        val parsed = CfirIntConstantEvalUtils.parseIntLiteral(expression) ?: return
        // 符号已随字面量进入 value：负值用有符号值域，正值用无符号值域，不再回扫源码文本判断符号。
        val signed = parsed.value.signum() < 0
        val suffixType = expression.explicitIntegerRangeType(parsed.explicitSuffix)
        // 显式声明的目标类型对正负字面量同样有效：`let c: Int64 = 9223372036854775808`
        // 官方按 Int64 报溢出（`ChkLitConstExprRange` 先取 `lce.ty` 再判范围），
        // 正字面量不能因为不带源码负号就跳过显式标注。
        // 未显式标注时该 helper 返回 null，落到字面量自身的 cone 类型。
        val initializerType = context.expectedInitializerTypeFor(source) as? ConePrimitiveType
        val targetType: ConePrimitiveType = suffixType
            ?: context.binaryDivisionOperandTargetTypeFor(source, isSigned = signed)
            ?: initializerType
            ?: (expression.coneTypeOrNull as? ConePrimitiveType)
            ?: ConePrimitiveType.INT64
        val range = suffixType?.let(CfirIntConstantEvalUtils::rangeForLiteralTargetType)
            ?: CfirIntConstantEvalUtils.rangeForIntLiteralValueTargetType(targetType, parsed.value)
            ?: return

        if (!range.contains(parsed.value)) {
            reporter.reportOn(
                source,
                CfirErrors.LITERAL_NUMERIC_OVERFLOW,
                parsed.originalText,
                targetType,
            )
        }
    }
}

/**
 * assignment expected-type owner 已将该直接字面量分类为 CANNOT_CONVERT_LITERAL 时，
 * 数值范围 checker 不再追加独立的 LITERAL_NUMERIC_OVERFLOW；两者描述的是同一次
 * AssignExpr/LitConstExpr 失败，官方只保留前者。
 */
private fun CheckerContext.isAssignmentSpecificLiteralConversion(
    expression: CfirLiteralExpression,
): Boolean {
    return containingElements.asReversed()
        .filterIsInstance<CfirAssignment>()
        .any { assignment ->
            assignment.rValue === expression &&
                    assignment.typeMismatchOutcome?.primaryDiagnostic is
                    CfirAssignmentTypeMismatchPrimaryDiagnostic.CannotConvertLiteral
        }
}

/** 后缀先确定源码类型，成功的 Option 数值定型则以实际内层类型检查范围。 */
private fun CfirExpression.explicitIntegerRangeType(suffix: String?): ConePrimitiveType? {
    val sourceType = CfirIntConstantEvalUtils.coneTypeForExplicitSuffix(suffix) ?: return null
    val actual = coneTypeOrNull as? ConePrimitiveType
    return if (actual != null && actual.kind.isInteger && !actual.kind.isIdeal) actual else sourceType
}

/**
 * 查找包含当前字面量的显式声明初始化目标类型。
 *
 * 官方 `ChkLitConstExprRange`（`CalcConstExpr.cpp:88-112`）只按字面量自身定型后的类型判范围：
 * 初始化器的显式类型经由类型推断链下传，链上任一表达式解析失败时该下传即在此断开。
 * 因此初始化器内部存在已失败表达式时不返回声明类型，操作数字面量退回自身类型定型，
 * 由失败表达式的根诊断独占该表达式，官方只报 `sema_invalid_binary_expr` 一条。
 */
private fun CheckerContext.expectedInitializerTypeFor(source: AbstractCjSourceElement): ConeCangJieType? {
    for (symbol in containingDeclarations.asReversed()) {
        val declaration = (symbol as? CfirVariableSymbol<*>)?.cfir ?: continue
        val initializerSource = declaration.initializer?.source as? AbstractCjSourceElement ?: continue
        if (!initializerSource.contains(source)) continue
        if (hasErroneousEnclosingExpression(initializerSource)) return null
        return declaration.returnTypeRef.explicitConeTypeOrNull()
    }
    return null
}

/**
 * 判断字面量与初始化器之间是否隔着已失败的表达式。
 *
 * 对齐官方 InvalidTy 语义：错误类型阻断后续普通类型规则，操作数不再沿失败的表达式继承
 * 外层声明的期望类型。范围限定在初始化器源码之内，避免声明之上的错误宿主（外层调用失败
 * 的实参里的局部声明）被误判成"初始化器已失败"。
 */
private fun CheckerContext.hasErroneousEnclosingExpression(
    initializer: AbstractCjSourceElement,
): Boolean = containingElements.asReversed().any { element ->
    val expression = element as? CfirExpression ?: return@any false
    val elementSource = expression.source as? AbstractCjSourceElement ?: return@any false
    elementSource.startOffset >= initializer.startOffset &&
            initializer.endOffset <= elementSource.endOffset &&
            expression.coneTypeOrNull?.containsErrorType() == true
}

/**
 * 返回显式类型引用的 cone 类型。
 */
private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.explicitConeTypeOrNull(): ConeCangJieType? =
    if (source != null) coneTypeOrNull else null

/**
 * 判断可空源码范围是否包含指定源码范围。
 */
private fun AbstractCjSourceElement?.contains(source: AbstractCjSourceElement): Boolean {
    val container = this ?: return false
    return container.startOffset <= source.startOffset && source.endOffset <= container.endOffset
}

/**
 * 在除法或取余表达式中推导操作数字面量的目标类型。
 *
 * 右操作数固定按 Int64 检查；左操作数只有在右操作数超出 Int64 且当前 literal 是带符号场景时，
 * 才按 UInt64 特殊语义处理。
 */
private fun CheckerContext.binaryDivisionOperandTargetTypeFor(
    source: AbstractCjSourceElement,
    isSigned: Boolean,
): ConePrimitiveType? {
    val binaryCall = containingElements.asReversed()
        .filterIsInstance<CfirFunctionCall>()
        .firstOrNull { call ->
            val operatorName = call.extractOperatorName()
            (operatorName == OperatorNameConventions.DIV || operatorName == OperatorNameConventions.REM) &&
                    call.source.contains(source)
        } ?: return null

    val rightOperand = binaryCall.argumentList.arguments.singleOrNull() ?: return null
    if (rightOperand.source.contains(source)) {
        return ConePrimitiveType.INT64
    }

    val leftOperand = binaryCall.explicitReceiver ?: return null
    if (!leftOperand.source.contains(source) || !isSigned) return null

    val rightValue = CfirIntConstantEvalUtils.parseSignedIntExpression(rightOperand)?.value ?: return null
    val int64Range = CfirIntConstantEvalUtils.rangeForLiteralTargetType(ConePrimitiveType.INT64) ?: return null
    return if (!int64Range.contains(rightValue)) ConePrimitiveType.UINT64 else null
}

/**
 * 从函数调用引用中提取 operator 名称。
 */
private fun CfirFunctionCall.extractOperatorName(): Name? {
    val reference = calleeReference
    return when (reference) {
        is CfirResolvedNamedReference -> reference.name
        is CfirNamedReference -> reference.name
        else -> null
    }
}

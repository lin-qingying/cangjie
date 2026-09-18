package org.cangnova.cangjie.cfir.resolve.match

import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirWrappedExpression
import org.cangnova.cangjie.cfir.patterns.CfirConstPattern
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.resolve.constants.CfirIntConstantEvalUtils
import org.cangnova.cangjie.cfir.resolve.constants.explicitNumericLiteralType
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.source.text
import org.cangnova.cangjie.type.AbstractTypeChecker

/** 常量模式的类型检查结论，由模式解析、绑定发布和诊断共同消费。 */
data class CfirPatternLiteralTypeResolution(
    val actualType: ConeCangJieType,
    val expectedType: ConeCangJieType,
    val failure: Failure? = null,
) {
    sealed interface Failure {
        /** 普通字面量 Check 已拒绝该目标类型。 */
        data class CannotConvert(val description: String) : Failure
        /** Check 可以装箱，但常量模式还要求字面量和 selector 精确同型。 */
        data object TypeMismatch : Failure
        /** 数值范围诊断仍由共享 literal overflow checker 报告。 */
        data object OutOfRange : Failure
        /** 表达式已经携带根错误，不追加模式诊断。 */
        data object AlreadyReported : Failure
    }
}

/** 一元负号属于数值字面量语义；包装节点不改变模式字面量身份。 */
fun CfirConstPattern.constantPatternLiteral(): CfirLiteralExpression? = expression.patternLiteral()

/**
 * 提取常量模式中的字面量表达式。
 *
 * 包装节点透明穿透；一元 `+` / `-` 调用若只包裹一个 INT/FLOAT 字面量，
 * 视为负号/正号字面量语义，返回内层字面量本身。
 */
private fun CfirExpression.patternLiteral(): CfirLiteralExpression? = when (this) {
    is CfirWrappedExpression -> expression.patternLiteral()
    is CfirLiteralExpression -> this
    is CfirFunctionCall -> {
        val name = (calleeReference as? CfirNamedReference)?.name
        if (argumentList.arguments.isEmpty() &&
            (name == OperatorNameConventions.UNARY_MINUS || name == OperatorNameConventions.UNARY_PLUS)
        ) {
            (explicitReceiver as? CfirLiteralExpression)?.takeIf {
                it.kind == CfirLiteralKind.INT || it.kind == CfirLiteralKind.FLOAT
            }
        } else null
    }
    else -> null
}

/**
 * 在首次解析字面量/operator 前选择其自身的目标类型。
 *
 * ChkLitConstExpr 允许为 Option 的内层数值定型，但不把字面量本身改写成 Option；
 * 常量模式随后单独执行精确类型检查。直接数值目标还必须保留显式后缀的类型。
 */
fun CfirConstPattern.constantPatternLiteralExpectedType(
    expectedType: ConeCangJieType?,
    session: CfirSession,
): ConeCangJieType? {
    val literal = constantPatternLiteral() ?: return null
    val target = expectedType?.fullyExpandedType(session)?.let(IdealTypeResolver::resolveIfIdeal)
    val explicitType = literal.explicitNumericLiteralType()
    val defaultType = literal.defaultPatternLiteralType(explicitType)
    if (target == null || target.contains { it is ConeErrorType || it is ConeTypeVariableType }) return explicitType

    if (literal.kind == CfirLiteralKind.STRING) return literal.characterPatternTarget(target)
    if (literal.isNumericPatternKindOf(target)) return explicitType ?: target

    var innerTarget = target.optionElementType
    while (innerTarget != null) {
        val inner = innerTarget.fullyExpandedType(session)
        if (literal.isNumericPatternKindOf(inner)) return inner
        innerTarget = inner.optionElementType
    }
    return defaultType
}

/** 对齐官方 ChkConstPattern：先执行字面量 Check，再拒绝仅通过装箱成立的类型关系。 */
fun CfirConstPattern.resolveLiteralPatternType(
    expectedType: ConeCangJieType?,
    session: CfirSession,
): CfirPatternLiteralTypeResolution? {
    val literal = constantPatternLiteral() ?: return null
    val target = expectedType?.fullyExpandedType(session)?.let(IdealTypeResolver::resolveIfIdeal) ?: return null
    // 函数返回位或泛型实参仍有根错误时，不能把它改写成新的模式错误，丢失推断失败来源。
    if (target.contains { it is ConeErrorType || it is ConeTypeVariableType }) return null
    val expressionType = expression.coneTypeOrNull
    if (expressionType is ConeErrorType) {
        return CfirPatternLiteralTypeResolution(expressionType, target, CfirPatternLiteralTypeResolution.Failure.AlreadyReported)
    }
    val explicitType = literal.explicitNumericLiteralType()
    val actualType = constantPatternLiteralExpectedType(target, session) ?: expressionType ?: return null
    val failure = when {
        literal.isNumericPatternKindOf(target) && explicitType != null && explicitType != target ->
            CfirPatternLiteralTypeResolution.Failure.CannotConvert(
                literal.source?.text?.toString() ?: literal.kind.literalDescription(),
            )
        AbstractTypeChecker.equalTypes(session.typeContext, actualType, target) -> {
            val integer = CfirIntConstantEvalUtils.parseSignedIntExpression(expression)
            val range = CfirIntConstantEvalUtils.rangeForLiteralTargetType(actualType)
            if (integer != null && range != null && !range.contains(integer.value)) {
                CfirPatternLiteralTypeResolution.Failure.OutOfRange
            } else null
        }
        literal.kind == CfirLiteralKind.STRING || literal.kind == CfirLiteralKind.UNIT ->
            CfirPatternLiteralTypeResolution.Failure.TypeMismatch
        literal.defaultPatternLiteralType(explicitType)?.isLiteralBoxableTo(target, session) == true ||
                target == ConeAnyType || target.classIdOrPrimitiveClassId == StdlibClassIds.Any ||
                (literal.kind != CfirLiteralKind.RUNE && target.classIdOrPrimitiveClassId == StdlibClassIds.CType) ->
            CfirPatternLiteralTypeResolution.Failure.TypeMismatch
        else -> CfirPatternLiteralTypeResolution.Failure.CannotConvert(literal.kind.literalDescription())
    }
    return CfirPatternLiteralTypeResolution(actualType, target, failure)
}

/** IsLitBoxableType 允许同类数值适配、普通子类型关系以及任意层数的 Option。 */
private fun ConeCangJieType.isLiteralBoxableTo(target: ConeCangJieType, session: CfirSession): Boolean {
    if (isIntegerType && target.isIntegerType || isFloatType && target.isFloatType) return true
    if (AbstractTypeChecker.isSubtypeOf(session.typeContext, this, target) == true) return true
    val inner = target.optionElementType?.fullyExpandedType(session) ?: return false
    return isLiteralBoxableTo(inner, session)
}

/** 判断该数值字面量的种类是否与 [target] 的数值族匹配（整数对整数、浮点对浮点）。 */
private fun CfirLiteralExpression.isNumericPatternKindOf(target: ConeCangJieType): Boolean = when (kind) {
    CfirLiteralKind.INT, CfirLiteralKind.BYTE -> target.isIntegerType
    CfirLiteralKind.FLOAT -> target.isFloatType
    else -> false
}

/**
 * 返回字面量在无外部目标时的默认模式类型。
 *
 * 有显式后缀（或 byte 语法）时优先使用其定型结果；否则按官方缺省规则：
 * 整数 → Int64、浮点 → Float64，布尔 / Rune / Unit 直接取对应 primitive。
 */
private fun CfirLiteralExpression.defaultPatternLiteralType(explicitType: ConePrimitiveType?): ConeCangJieType? =
    explicitType ?: when (kind) {
        CfirLiteralKind.INT -> ConePrimitiveType.INT64
        CfirLiteralKind.FLOAT -> ConePrimitiveType.FLOAT64
        CfirLiteralKind.BOOLEAN -> ConePrimitiveType.BOOLEAN
        CfirLiteralKind.RUNE -> ConePrimitiveType.RUNE
        CfirLiteralKind.UNIT -> ConePrimitiveType.UNIT
        else -> null
    }

/** 字符串模式保留 STRING kind；官方只改目标类型，不改变 Sema 常量比较的种类。 */
private fun CfirLiteralExpression.characterPatternTarget(target: ConeCangJieType): ConeCangJieType? {
    val string = value as? String ?: return null
    if (string.codePointCount(0, string.length) != 1) return null
    return when {
        target.isRune -> target
        target == ConePrimitiveType.UINT8 && string.codePointAt(0) <= 255 -> target
        else -> null
    }
}

/** 返回该字面量种类在诊断文本中的英文描述，与官方 CannotConvert 消息用词保持一致。 */
private fun CfirLiteralKind.literalDescription(): String = when (this) {
    CfirLiteralKind.INT, CfirLiteralKind.BYTE -> "integer"
    CfirLiteralKind.FLOAT -> "floating-point"
    CfirLiteralKind.BOOLEAN -> "boolean"
    CfirLiteralKind.RUNE -> "character"
    else -> name.lowercase()
}

package org.cangnova.cangjie.cfir.analysis.diagnostics

import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.InternalDiagnosticFactoryMethod
import org.cangnova.cangjie.cfir.expressions.CfirArrayLiteral
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirWrappedExpression
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.isBoolean
import org.cangnova.cangjie.cfir.types.isFloatType
import org.cangnova.cangjie.cfir.types.isIntegerType
import org.cangnova.cangjie.cfir.types.isRune
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjSourceElement

/**
 * 专门承接“通用 type mismatch 之下还能继续细分的语义”。
 *
 * 这层故意既不直接做 checker，也不直接绑在 cone 映射上，
 * 因为同一条细分规则可能同时被：
 * - declaration / expression checker 直接使用；
 * - coneDiagnostic -> CfirDiagnostic 映射复用。
 *
 * 这样可以避免 VArray 这类类型系统专门语义在多个入口重复实现。
 */
internal sealed interface CfirSpecificTypeMismatch {
    /** 字面量不能按目标类型转换。 */
    data class CannotConvertLiteral(
        /** 官方诊断中的字面量类别文本。 */
        val literalDescription: String,
        /** 目标类型。 */
        val expectedType: ConeCangJieType,
    ) : CfirSpecificTypeMismatch
    /** VArray 数组字面量长度与目标类型长度不一致。 */
    data class VArraySizeMismatch(
        /** 目标 VArray 的元素类型。 */
        val elementType: ConeCangJieType,
        /** 期望类型中的 VArray 长度。 */
        val expectedSize: Long,
        /** 数组字面量中的元素数量。 */
        val actualSize: Long,
    ) : CfirSpecificTypeMismatch
}

/** 在通用 type mismatch 中识别由目标类型和表达式语法共同决定的 CFIR 专属细分诊断。 */
internal fun classifySpecificTypeMismatch(
    expectedType: ConeCangJieType,
    actualType: ConeCangJieType,
    expression: CfirExpression?,
    session: CfirSession,
): CfirSpecificTypeMismatch? {
    if (expression is CfirLiteralExpression && expression.kind == CfirLiteralKind.RUNE && !expectedType.isRune) {
        return CfirSpecificTypeMismatch.CannotConvertLiteral(
            literalDescription = "character",
            expectedType = expectedType,
        )
    }
    literalConversionMismatch(expectedType, expression, session)?.let { return it }
    val arrayLiteral = expression?.unwrapWrappedExpression() as? CfirArrayLiteral ?: return null
    val expectedVArray = expectedType.fullyExpandedType(session) as? ConeVArrayType ?: return null
    val actualSize = arrayLiteral.elements.size.toLong()
    if (expectedVArray.size == actualSize) return null

    return CfirSpecificTypeMismatch.VArraySizeMismatch(
        elementType = expectedVArray.elementType,
        expectedSize = expectedVArray.size,
        actualSize = actualSize,
    )
}

/**
 * VArray 长度诊断只认可数组字面量语法；括号等 CFIR 包装不改变该语义事实。
 */
private tailrec fun CfirExpression.unwrapWrappedExpression(): CfirExpression = when (this) {
    is CfirWrappedExpression -> expression.unwrapWrappedExpression()
    else -> this
}

/**
 * 识别数值/布尔字面量落入官方 `ChkLitConstExprOf*` 的“无法转换”分支。
 *
 * 官方在目标类型不是该字面量的合法子类型、且不可装箱、也不是 `Any`/`CType` 时报告
 * `sema_cannot_convert_literal`（`Sema/TypeCheckExpr/LitConstExpr.cpp`）。这里只覆盖
 * 标量基础类型目标——装箱与 `Any`/`CType` 逃逸路径在这些目标上不成立，因此结论与官方一致。
 */
internal fun literalConversionMismatch(
    expectedType: ConeCangJieType,
    expression: CfirExpression?,
    session: CfirSession,
): CfirSpecificTypeMismatch.CannotConvertLiteral? {
    val literal = expression?.unwrapWrappedExpression() as? CfirLiteralExpression ?: return null
    val target = expectedType.fullyExpandedType(session)
    if (!target.isIntegerType && !target.isFloatType && !target.isBoolean && !target.isRune) return null
    val description = when (literal.kind) {
        CfirLiteralKind.INT -> "integer".takeIf { !target.isIntegerType }
        CfirLiteralKind.FLOAT -> "floating-point".takeIf { !target.isFloatType }
        CfirLiteralKind.BOOLEAN -> "boolean".takeIf { !target.isBoolean }
        else -> null
    } ?: return null
    return CfirSpecificTypeMismatch.CannotConvertLiteral(
        literalDescription = description,
        expectedType = expectedType,
    )
}

/** 直接构造字面量转换失败诊断；不属于该语义时返回空。 */
internal fun literalConversionDiagnostic(
    source: AbstractCjSourceElement?,
    expectedType: ConeCangJieType,
    expression: CfirExpression?,
    session: CfirSession,
): CjDiagnostic? {
    val diagnosticSource = source as? CjSourceElement ?: return null
    val mismatch = literalConversionMismatch(expectedType, expression, session) ?: return null
    return createCannotConvertLiteralDiagnostic(diagnosticSource, mismatch, session)
}

/** 根据细分 type mismatch 创建更精确的 CFIR 诊断；没有细分语义时返回空。 */
internal fun specificTypeMismatchDiagnostic(
    source: AbstractCjSourceElement?,
    expectedType: ConeCangJieType,
    actualType: ConeCangJieType,
    expression: CfirExpression? = null,
    session: CfirSession,
): CjDiagnostic? {
    val diagnosticSource = source as? CjSourceElement ?: return null
    val mismatch = classifySpecificTypeMismatch(expectedType, actualType, expression, session) ?: return null
    return mismatch.toDiagnostic(diagnosticSource, session)
}

/** 将结构化细分 mismatch 转换为项目诊断。 */
private fun CfirSpecificTypeMismatch.toDiagnostic(
    source: CjSourceElement,
    session: CfirSession,
): CjDiagnostic? {
    return when (this) {
        is CfirSpecificTypeMismatch.CannotConvertLiteral -> createCannotConvertLiteralDiagnostic(
            source = source,
            mismatch = this,
            session = session,
        )
        is CfirSpecificTypeMismatch.VArraySizeMismatch -> createVArraySizeMismatchDiagnostic(
            source = source,
            mismatch = this,
            session = session,
        )
    }
}

/** 创建目标类型驱动的字面量转换失败诊断。 */
@OptIn(InternalDiagnosticFactoryMethod::class)
private fun createCannotConvertLiteralDiagnostic(
    source: CjSourceElement,
    mismatch: CfirSpecificTypeMismatch.CannotConvertLiteral,
    session: CfirSession,
): CjDiagnostic? {
    return CfirErrors.CANNOT_CONVERT_LITERAL.on(
        source,
        mismatch.literalDescription,
        mismatch.expectedType,
        null,
        diagnosticContext(session),
    )
}

/** 创建 VArray 长度不匹配的专用诊断。 */
@OptIn(InternalDiagnosticFactoryMethod::class)
private fun createVArraySizeMismatchDiagnostic(
    source: CjSourceElement,
    mismatch: CfirSpecificTypeMismatch.VArraySizeMismatch,
    session: CfirSession,
): CjDiagnostic? {
    return CfirErrors.VARRAY_SIZE_MISMATCH.on(
        source,
        mismatch.expectedSize,
        mismatch.actualSize,
        mismatch.elementType,
        null,
        diagnosticContext(session),
    )
}

/** 为内部构造诊断提供最小 diagnostic context。 */
private fun diagnosticContext(session: CfirSession): DiagnosticContext {
    return object : DiagnosticContext {
        /** 诊断工厂渲染所需的语言版本设置。 */
        override val languageVersionSettings = session.languageVersionSettings
        /** 该内部 context 不绑定具体文件路径。 */
        override val containingFilePath: String? = null
        /** 内部构造阶段不应用 suppress 规则。 */
        override fun isDiagnosticSuppressed(diagnostic: CjDiagnostic): Boolean = false
    }
}

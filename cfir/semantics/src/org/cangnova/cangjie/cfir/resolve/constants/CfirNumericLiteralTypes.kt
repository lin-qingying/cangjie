package org.cangnova.cangjie.cfir.resolve.constants

import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.optionElementType

/**
 * 浮点字面量的显式类型后缀模式。
 *
 * 对齐官方 `f16` / `f32` / `f64` 后缀语法；匹配时忽略大小写，
 * 命中后字面量在综合阶段即定型为对应 primitive，不再保留 ideal 语义。
 */
private val floatLiteralSuffix = Regex("(?i)(f16|f32|f64)$")

/**
 * 官方 GetNumLitTypeKind 的显式数值类型：后缀和 byte 语法在综合阶段就确定类型，
 * 只有没有显式类型的数值字面量才保留 ideal。普通表达式与常量模式共用此入口。
 */
fun CfirLiteralExpression.explicitNumericLiteralType(): ConePrimitiveType? = when (kind) {
    CfirLiteralKind.INT -> CfirIntConstantEvalUtils.coneTypeForExplicitSuffix(
        CfirIntConstantEvalUtils.parseIntLiteral(this)?.explicitSuffix,
    )
    CfirLiteralKind.BYTE -> ConePrimitiveType.UINT8
    CfirLiteralKind.FLOAT -> when (floatLiteralSuffix.find(value as? String ?: "")?.value?.lowercase()) {
        "f16" -> ConePrimitiveType.FLOAT16
        "f32" -> ConePrimitiveType.FLOAT32
        "f64" -> ConePrimitiveType.FLOAT64
        else -> null
    }
    else -> null
}

/**
 * 官方 GetInnerNumericType：Option 装箱检查可以将带后缀字面量按内层数值类型定型。
 * 直接 primitive 目标不走这条规则，仍须检查其类型与显式后缀相同。
 */
fun CfirLiteralExpression.numericLiteralBoxTargetType(
    expectedType: ConeCangJieType,
    session: CfirSession,
): ConePrimitiveType? {
    var inner = expectedType.fullyExpandedType(session).optionElementType ?: return null
    while (true) {
        inner = inner.fullyExpandedType(session)
        val next = inner.optionElementType ?: break
        inner = next
    }
    val primitive = inner as? ConePrimitiveType ?: return null
    return primitive.takeIf {
        when (kind) {
            CfirLiteralKind.INT, CfirLiteralKind.BYTE -> it.kind.isInteger
            CfirLiteralKind.FLOAT -> it.kind.isFloat
            else -> false
        }
    }
}

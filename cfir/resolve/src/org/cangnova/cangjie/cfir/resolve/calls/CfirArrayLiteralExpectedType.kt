package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.expressions.CfirArrayLiteral
import org.cangnova.cangjie.cfir.resolve.arrayLiteralTypeForSupertypeTarget
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.IdealTypeResolver
import org.cangnova.cangjie.cfir.types.arrayLiteralElementType
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 按元素目标类型检查数组字面量可构造的数组类型，不修改候选共享的表达式。
 *
 * 官方 ChkArrayLit 对每个元素执行目标类型检查，成功后才确定 Array/VArray 类型。
 * 这与已构造数组值的名义子类型检查不同：`[1]` 可构造 Array<I>，Array<Int64> 变量
 * 仍保持不变性。普通实参、变参选择和最终写回共用此判定，嵌套字面量逐层传播目标。
 */
internal fun CfirArrayLiteral.contextualArrayLiteralTypeOrNull(
    expectedType: ConeCangJieType,
    session: CfirSession,
): ConeCangJieType? {
    val expandedExpectedType = expectedType.fullyExpandedType(session)
    val arrayType = if (expandedExpectedType.arrayLiteralElementType != null) {
        expandedExpectedType
    } else {
        expandedExpectedType.arrayLiteralTypeForSupertypeTarget(session) ?: return null
    }
    val elementExpectedType = arrayType.arrayLiteralElementType ?: return null
    if (arrayType is ConeVArrayType && arrayType.size != elements.size.toLong()) return null

    for (element in elements) {
        val contextualElementType = (element as? CfirArrayLiteral)
            ?.contextualArrayLiteralTypeOrNull(elementExpectedType, session)
        val elementType = contextualElementType ?: element.coneTypeOrNull ?: return null
        val actualType = IdealTypeResolver.resolveIfIdeal(elementType, elementExpectedType)
        if (actualType !is ConeErrorType && elementExpectedType !is ConeErrorType &&
            !AbstractTypeChecker.equalTypes(session.typeContext, actualType, elementExpectedType) &&
            !AbstractTypeChecker.isSubtypeOf(session.typeContext, actualType, elementExpectedType)
        ) return null
    }
    return arrayType
}

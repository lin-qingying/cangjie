package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePlaceholderType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConeQuestType
import org.cangnova.cangjie.cfir.types.ConeStubType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.contains
import org.cangnova.cangjie.cfir.types.optionElementType
import org.cangnova.cangjie.cfir.types.type

/**
 * 官方 ChkPointerExpr 只从去掉 Option 外壳后、携带有效类型实参的目标中推断 pointee。
 * 无类型实参的接口目标不能通过条件 extend 的 where 约束反向推断 CPointer<T>；
 * 已显式实例化的指针仍按普通表达式规则检查目标兼容性。
 */
internal fun Candidate.shouldUseExpectedTypeForBuiltinPointerConstructor(
    expectedType: ConeCangJieType,
    session: CfirSession,
): Boolean {
    val declaration = symbol.takeIf { it.isBound }?.cfir ?: return true
    if (declaration.origin != CfirDeclarationOrigin.Synthetic.BuiltinPointerConstructor) return true
    if (callInfo.hasExplicitTypeArguments) return true
    return expectedType.builtinPointerExpectedTypeArgument(session) != null
}

/**
 * 取得官方 ChkPointerExpr 用于 pointee 推断的目标类型实参。
 * 参数检查前和 completion 必须使用同一个去 Option 投影，避免合法的可选指针目标失去约束。
 */
internal fun ConeCangJieType.builtinPointerExpectedTypeArgument(session: CfirSession): ConeCangJieType? {
    var target = fullyExpandedType(session)
    while (true) {
        val element = target.optionElementType ?: break
        target = element.fullyExpandedType(session)
    }
    val targetArgument = when (target) {
        // CFIR 的指针把类型实参存于 pointeeType，官方 PointerTy 则使用 typeArgs[0]。
        is ConePointerType -> target.pointeeType
        else -> target.typeArguments.firstOrNull()?.type ?: return null
    }
    return targetArgument.takeUnless { it.contains { component -> component is ConeErrorType } }
}

/**
 * 判断类型树中是否仍含不能用于确定性 expected-type 兼容分类的分量。
 *
 * 该判定同时供早期 candidate stage 与 precollected refinement 使用，保证二者不会
 * 对泛型、推断变量和错误恢复类型作出不同的淘汰结论。
 */
internal fun ConeCangJieType.hasUncertainExpectedTypeCompatibilityShape(): Boolean = contains { type ->
    when (type) {
        is ConeTypeParameterType,
        is ConeTypeVariableType,
        is ConeErrorType,
        is ConeStubType,
        is ConePlaceholderType,
        is ConeQuestType,
        -> true

        else -> false
    }
}

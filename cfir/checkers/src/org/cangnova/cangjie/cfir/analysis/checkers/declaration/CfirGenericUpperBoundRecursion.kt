package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRefsOwner
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.declaredUpperBoundConeTypeOrNull
import org.cangnova.cangjie.cfir.types.declaredUpperBoundRefsAfterTypeResolve
import org.cangnova.cangjie.cfir.types.type

/**
 * 描述官方 `ValidRecursiveConstraintCheck` 阶段找到的第一个泛型上界递归根因。
 */
internal sealed class CfirGenericUpperBoundRecursionIssue {
    abstract val parameter: CfirTypeParameter

    /**
     * 上界链里出现泛型参数环，对应 `sema_generic_param_directly_recursive`。
     */
    data class Direct(
        override val parameter: CfirTypeParameter,
        val recursiveWith: CfirTypeParameterSymbol,
    ) : CfirGenericUpperBoundRecursionIssue()

    /**
     * 非 class/interface 上界的类型实参递归引用当前参数，对应
     * `sema_generic_param_exist_in_class_irrelevant_upperbound_recursively`。
     */
    data class ClassIrrelevant(
        override val parameter: CfirTypeParameter,
        val upperBound: ConeCangJieType,
    ) : CfirGenericUpperBoundRecursionIssue()
}

/**
 * 按官方 `PreCheck.cpp::ValidRecursiveConstraintCheck` 顺序寻找声明级第一个递归上界根因。
 *
 * 官方在找到根因后立即停止本 generic 声明的上界 sanity check，因此后续
 * class/interface 上界合法性检查也必须跳过，避免重复或级联诊断。
 */
context(session: CfirSession)
internal fun CfirTypeParameter.findFirstGenericUpperBoundRecursionIssueInOwner(): CfirGenericUpperBoundRecursionIssue? {
    val owner = containingDeclarationSymbol.cfir as? CfirTypeParameterRefsOwner ?: return null
    return owner.findFirstGenericUpperBoundRecursionIssue()
}

context(session: CfirSession)
private fun CfirTypeParameterRefsOwner.findFirstGenericUpperBoundRecursionIssue(): CfirGenericUpperBoundRecursionIssue? {
    for (typeParameter in typeParameters) {
        val parameter = typeParameter as? CfirTypeParameter ?: continue
        for (boundType in parameter.symbol.declaredUpperBoundTypes()) {
            val boundTypeParameter = boundType.typeParameterSymbolOrNull()
            if (boundTypeParameter != null && boundTypeParameter.hasDirectRecursiveUpperBound()) {
                return CfirGenericUpperBoundRecursionIssue.Direct(parameter, boundTypeParameter)
            }
            if (!boundType.isClassLikeUpperBound() &&
                boundType.containsCurrentGenericParameterInUpperBounds(parameter.symbol)
            ) {
                return CfirGenericUpperBoundRecursionIssue.ClassIrrelevant(parameter, boundType)
            }
        }
    }
    return null
}

/**
 * 官方直接递归判断只沿泛型参数上界边递归；一旦任一泛型参数在该链路里重复出现即为直接递归。
 */
private fun CfirTypeParameterSymbol.hasDirectRecursiveUpperBound(
    visited: MutableSet<CfirTypeParameterSymbol> = linkedSetOf(),
): Boolean {
    if (!visited.add(this)) return true
    for (boundType in declaredUpperBoundTypes()) {
        val boundSymbol = boundType.typeParameterSymbolOrNull() ?: continue
        if (boundSymbol.hasDirectRecursiveUpperBound(visited)) return true
    }
    return false
}

/**
 * 对齐官方 `IsGenericParamExistInUpperBounds`：只在非 class/interface 上界中，
 * 沿类型实参和同一声明的泛型参数上界继续搜索当前参数。
 */
private fun ConeCangJieType.containsCurrentGenericParameterInUpperBounds(
    currentParameter: CfirTypeParameterSymbol,
): Boolean {
    val visited = linkedSetOf<ConeCangJieType>()
    val queue = ArrayDeque<ConeCangJieType>()
    queue += this

    while (queue.isNotEmpty()) {
        val currentType = queue.removeFirst()
        if (!visited.add(currentType)) continue
        if (currentType.typeParameterSymbolOrNull() == currentParameter) return true

        for (argument in currentType.typeArguments) {
            val argumentType = argument.type ?: continue
            val argumentSymbol = argumentType.typeParameterSymbolOrNull()
            if (argumentSymbol == null) {
                queue += argumentType
                continue
            }
            if (argumentSymbol == currentParameter) return true
            if (argumentSymbol.containingDeclarationSymbol != currentParameter.containingDeclarationSymbol) continue
            argumentSymbol.declaredUpperBoundTypes().forEach { queue += it }
        }
    }

    return false
}

private fun CfirTypeParameterSymbol.declaredUpperBoundTypes(): List<ConeCangJieType> =
    toLookupTag()
        .declaredUpperBoundRefsAfterTypeResolve()
        .mapNotNull { it.declaredUpperBoundConeTypeOrNull() }
        .filterNot { it is ConeErrorType }

context(session: CfirSession)
private fun ConeCangJieType.isClassLikeUpperBound(): Boolean =
    fullyExpandedType(session) is ConeClassLikeType

private fun ConeCangJieType.typeParameterSymbolOrNull(): CfirTypeParameterSymbol? =
    (this as? ConeTypeParameterType)
        ?.lookupTag
        ?.let { it as? ConeTypeParameterLookupTag }
        ?.typeParameterSymbol

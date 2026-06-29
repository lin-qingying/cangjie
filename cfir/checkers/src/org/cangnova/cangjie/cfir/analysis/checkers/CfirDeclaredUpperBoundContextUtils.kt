package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRefsOwner
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.CfirFunctionTypeRef
import org.cangnova.cangjie.cfir.types.CfirTupleTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.ConeAnyType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.declaredUpperBoundRefsAfterTypeResolve
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.name.Name

/**
 * 返回当前 checker context 下可安全消费的声明上界类型。
 *
 * 批处理 LLT 中局部函数头部不一定能通过 symbol lazy resolve 推进到 TYPES；
 * 对 raw `U <: T` 这类约束，需要从当前可见的类型参数声明栈恢复 `T` 的符号身份。
 */
context(context: CheckerContext)
internal fun CfirTypeParameter.declaredUpperBoundTypesInCurrentContext(): List<ConeCangJieType> =
    symbol.toLookupTag()
        .declaredUpperBoundRefsAfterTypeResolve()
        .mapNotNull { it.declaredUpperBoundConeTypeInCurrentContextOrNull() }

/**
 * 判断类型参数声明侧是否已经有非法上界诊断根因。
 */
context(context: CheckerContext)
internal fun CfirTypeParameter.hasInvalidDeclaredUpperBoundsInCurrentContext(): Boolean =
    declaredUpperBoundTypesInCurrentContext()
        .filterNot { it is ConeErrorType }
        .any { !it.isLegalDeclaredUpperBoundInCurrentContext() }

/**
 * 判断声明 owner 是否包含非法类型参数上界。
 */
context(context: CheckerContext)
internal fun CfirTypeParameterRefsOwner.hasInvalidDeclaredUpperBoundsInCurrentContext(): Boolean =
    typeParameters
        .map { it.symbol.cfir }
        .any { it.hasInvalidDeclaredUpperBoundsInCurrentContext() }

/**
 * 判断表达式类型是否是带非法声明上界的类型参数。
 */
context(context: CheckerContext)
internal fun ConeCangJieType.isTypeParameterWithInvalidDeclaredUpperBoundsInCurrentContext(): Boolean {
    val typeParameterType = this as? ConeTypeParameterType ?: return false
    return typeParameterType.lookupTag.typeParameterSymbol.cfir.hasInvalidDeclaredUpperBoundsInCurrentContext()
}

/**
 * 在当前声明栈下提取声明上界的 cone type。
 */
context(context: CheckerContext)
private fun CfirTypeRef.declaredUpperBoundConeTypeInCurrentContextOrNull(): ConeCangJieType? {
    coneTypeOrNull?.let { return it }
    return when (this) {
        is CfirFunctionTypeRef -> {
            val parameterTypes = parameterTypeRefs.map { it.declaredUpperBoundConeTypeInCurrentContextOrNull() }
            val returnType = returnTypeRef.declaredUpperBoundConeTypeInCurrentContextOrNull()
            if (parameterTypes.any { it == null } || returnType == null) {
                null
            } else {
                ConeFunctionType(
                    parameterTypes = parameterTypes.filterNotNull(),
                    returnType = returnType,
                )
            }
        }
        is CfirTupleTypeRef -> {
            val elementTypes = elementTypeRefs.map { it.declaredUpperBoundConeTypeInCurrentContextOrNull() }
            if (elementTypes.any { it == null }) {
                null
            } else {
                ConeTupleType(elementTypes.filterNotNull())
            }
        }
        is CfirUserTypeRef -> visibleTypeParameterSymbolOrNull()?.constructType()
        else -> null
    }
}

/**
 * 将单段 raw user type-ref 映射到当前声明栈中可见的类型参数。
 */
context(context: CheckerContext)
private fun CfirUserTypeRef.visibleTypeParameterSymbolOrNull(): CfirTypeParameterSymbol? {
    if (qualifier.size != 1) return null
    val part = qualifier.single()
    if (part.typeArguments.isNotEmpty()) return null
    return context.findVisibleTypeParameterSymbol(part.name)
}

/**
 * 从内到外查找当前可见的类型参数符号。
 */
private fun CheckerContext.findVisibleTypeParameterSymbol(name: Name): CfirTypeParameterSymbol? {
    for (declaration in containingDeclarations.asReversed()) {
        val owner = declaration as? CfirTypeParameterRefsOwner ?: continue
        owner.typeParameters.firstOrNull { it.symbol.name == name }?.let { return it.symbol }
    }
    return null
}

/**
 * 判断单个声明上界是否满足官方 class/interface 上界准入规则。
 */
context(context: CheckerContext)
private fun ConeCangJieType.isLegalDeclaredUpperBoundInCurrentContext(): Boolean {
    val expandedType = fullyExpandedType(context.session)
    if (expandedType === ConeAnyType) return true

    val classId = expandedType.classIdOrPrimitiveClassId
    if (classId == StdlibClassIds.Any || CfirExtendSemantics.isCType(classId)) return true

    return expandedType is ConeClassLikeType
}

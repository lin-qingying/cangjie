package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.typeAwareSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * 根据成员声明的 owner 与 use-site receiver 类型生成 owner 类型参数替换。
 *
 * 官方 Cangjie 在 `CheckGenericDeclInstantiation` 中先通过
 * `GenerateTypeMappingForBaseExpr` 收集成员访问 base expression 的泛型映射，
 * 再合并 callable 自身的显式类型实参映射。CFIR 的调用解析和诊断检查都必须
 * 复用这一入口，避免候选排序与最终诊断看到不同的上界。
 */
fun createCallableOwnerUseSiteSubstitutionMap(
    session: CfirSession,
    callableSymbol: CfirCallableSymbol<*>?,
    receiverType: ConeCangJieType?,
): Map<TypeConstructorMarker, ConeCangJieType> {
    if (callableSymbol == null || receiverType == null) return emptyMap()
    val ownerClassId = session.cfirProvider.getContainingClass(callableSymbol)?.classId
        ?: return emptyMap()
    val concreteOwnerType = findConcreteOwnerType(session, receiverType, ownerClassId)
        ?: return emptyMap()
    val ownerDeclaration = session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId)?.cfir
        ?: session.cfirProvider.getCfirClassifierByFqName(ownerClassId)
        ?: return emptyMap()
    return createClassLikeOwnerSubstitutionMap(ownerDeclaration, concreteOwnerType)
}

fun createCallableOwnerUseSiteSubstitutor(
    session: CfirSession,
    callableSymbol: CfirCallableSymbol<*>?,
    receiverType: ConeCangJieType?,
): ConeSubstitutor {
    val substitutions = createCallableOwnerUseSiteSubstitutionMap(session, callableSymbol, receiverType)
    return substitutions.takeIf { it.isNotEmpty() }?.let(::CfirTypeSubstitutorByMap)
        ?: ConeSubstitutor.Empty
}

private fun findConcreteOwnerType(
    session: CfirSession,
    receiverType: ConeCangJieType,
    ownerClassId: ClassId,
): ConeCangJieType? {
    val rootType = receiverType.fullyExpandedType(session)
    if (rootType.classIdOrPrimitiveClassId == ownerClassId) return rootType

    val queue = ArrayDeque<ConeCangJieType>()
    val visited = linkedSetOf<ConeCangJieType>()
    queue += rootType

    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        if (!visited.add(current)) continue
        if (current.classIdOrPrimitiveClassId == ownerClassId) return current
        queue.addAll(session.typeAwareSupertypeProviderOrNull?.getDirectSupertypes(current).orEmpty())
    }

    return null
}

private fun createClassLikeOwnerSubstitutionMap(
    declaration: CfirClassLikeDeclaration,
    concreteOwnerType: ConeCangJieType,
): Map<TypeConstructorMarker, ConeCangJieType> {
    if (concreteOwnerType !is ConeLookupTagBasedType) return emptyMap()
    if (declaration.typeParameters.isEmpty()) return emptyMap()
    if (declaration.typeParameters.size != concreteOwnerType.typeArguments.size) return emptyMap()

    return declaration.typeParameters
        .zip(concreteOwnerType.typeArguments)
        .associate { (typeParameter, argument) ->
            typeParameter.symbol.toLookupTag() to argument.type
        }
}

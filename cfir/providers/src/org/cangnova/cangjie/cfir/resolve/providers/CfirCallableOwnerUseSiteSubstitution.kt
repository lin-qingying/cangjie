package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRefsOwner
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.typeAwareSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.expandedClassIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
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
    val originalCallableSymbol = callableSymbol.unwrapSubstitutionOverrides()
    val ownerExtend = originalCallableSymbol.getContainingExtend()
    if (ownerExtend != null) {
        return createExtendOwnerSubstitutionMap(session, ownerExtend, receiverType)
    }

    val ownerClassId = originalCallableSymbol.getContainingClass()?.classId
        ?: enumConstructorOwnerClassId(originalCallableSymbol, receiverType, session)
        ?: return emptyMap()
    val concreteOwnerType = findConcreteOwnerType(session, receiverType, ownerClassId)
        ?: return emptyMap()
    val ownerSymbol = session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId)
    val ownerDeclaration = ownerSymbol?.cfir
        ?: session.cfirProvider.getCfirClassifierByFqName(ownerClassId)
        ?: return emptyMap()
    if (ownerSymbol != null && concreteOwnerType.isBareOrDeclarationSelfTypeOf(ownerSymbol)) {
        return emptyMap()
    }
    return createClassLikeOwnerSubstitutionMap(ownerDeclaration, concreteOwnerType)
}

/**
 * 裸 classifier qualifier 会被构造成声明自身类型 `A<T>`，用于把 owner 类型参数交给调用推断。
 *
 * 这种类型不是用户显式提供的 use-site 实参，不能在 provider 层提前替换成已知实参；
 * 否则 static 成员调用会在 fresh-variable 阶段之前失去可推断的 owner 类型参数。
 */
fun ConeCangJieType.isBareOrDeclarationSelfTypeOf(ownerSymbol: CfirClassLikeSymbol<*>): Boolean {
    val ownerType = this as? ConeLookupTagBasedType ?: return false
    if (ownerType.classIdOrPrimitiveClassId != ownerSymbol.classId) return false
    if (ownerType.typeArguments.isEmpty()) return true

    val ownerTypeParameters = (ownerSymbol.cfir as? CfirTypeParameterRefsOwner)?.typeParameters.orEmpty()
    if (ownerTypeParameters.isEmpty()) return false
    if (ownerType.typeArguments.size != ownerTypeParameters.size) return false

    return ownerType.typeArguments.zip(ownerTypeParameters).all { (argument, typeParameter) ->
        val argumentType = argument.type as? ConeTypeParameterType ?: return@all false
        argumentType.lookupTag.typeParameterSymbol == typeParameter.symbol
    }
}

/**
 * 为 extend 成员构造 owner 类型参数到 use-site 实参的替换表。
 */
private fun createExtendOwnerSubstitutionMap(
    session: CfirSession,
    ownerExtend: org.cangnova.cangjie.cfir.declarations.CfirExtend,
    receiverType: ConeCangJieType,
): Map<TypeConstructorMarker, ConeCangJieType> {
    val substitution = findExtendDeclarationSubstitution(session, ownerExtend, receiverType)
        ?: return emptyMap()
    if (ownerExtend.typeParameters.isEmpty()) return emptyMap()

    return ownerExtend.typeParameters.mapNotNull { typeParameter ->
        val key = typeParameter.symbol.toLookupTag()
        val value = substitution.substitutor.substituteOrNull(typeParameter.symbol.constructType())
            ?: return@mapNotNull null
        key to value
    }.toMap()
}

/**
 * 当 callable 是 enum constructor 时，从 receiver 类型恢复 owner enum 的 [ClassId]。
 */
private fun enumConstructorOwnerClassId(
    callableSymbol: CfirCallableSymbol<*>,
    receiverType: ConeCangJieType,
    session: CfirSession,
): ClassId? {
    if (callableSymbol !is CfirEnumConstructorSymbol && callableSymbol.cfir !is CfirEnumConstructor) return null
    return receiverType.fullyExpandedType(session).expandedClassIdOrPrimitiveClassId
}

/**
 * 创建 callable owner use-site 替换器。
 */
fun createCallableOwnerUseSiteSubstitutor(
    session: CfirSession,
    callableSymbol: CfirCallableSymbol<*>?,
    receiverType: ConeCangJieType?,
): ConeSubstitutor {
    val substitutions = createCallableOwnerUseSiteSubstitutionMap(session, callableSymbol, receiverType)
    return substitutions.takeIf { it.isNotEmpty() }?.let(::CfirTypeSubstitutorByMap)
        ?: ConeSubstitutor.Empty
}

/**
 * 在 receiver 类型及其直接父类型链中寻找实际声明 [ownerClassId] 的具体类型。
 */
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

/**
 * 为 class-like owner 构造类型参数到具体 owner 类型实参的替换表。
 */
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

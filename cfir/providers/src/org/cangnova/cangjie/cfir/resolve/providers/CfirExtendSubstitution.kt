package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.typeAwareSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * extend 目标类型与 use-site 接收者类型匹配后的替换结果。
 *
 * 这是 providers 层的共享语义入口：use-site substitution scope 与调用解析
 * receiver 检查必须使用同一套 extend 目标匹配规则，避免签名替换和候选适用性分叉。
 */
data class CfirExtendDeclarationSubstitution(
    val substitutor: ConeSubstitutor,
    val substitutedReceiverType: ConeCangJieType,
)

/**
 * 在接收者类型及其直接父类型链上匹配 extend 目标类型。
 *
 * 与 `CfirClassSubstitutionScope` 原有语义一致：extend 的所有类型参数都必须能从
 * 目标类型模式中被接收者约束，否则该 extend 对当前 use-site 不成立。
 */
fun findExtendDeclarationSubstitution(
    session: CfirSession,
    extend: CfirExtend,
    concreteReceiverType: ConeCangJieType,
): CfirExtendDeclarationSubstitution? {
    val queue = ArrayDeque<ConeCangJieType>()
    val visited = linkedSetOf<ConeCangJieType>()
    queue += concreteReceiverType

    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        if (!visited.add(current)) continue

        createExtendDeclarationSubstitution(extend, current)?.let { return it }

        queue.addAll(session.typeAwareSupertypeProviderOrNull?.getDirectSupertypes(current).orEmpty())
    }

    return null
}

private fun createExtendDeclarationSubstitution(
    extend: CfirExtend,
    concreteReceiverType: ConeCangJieType,
): CfirExtendDeclarationSubstitution? {
    val targetPattern = extend.extendedTypeRef.coneTypeOrNull ?: return null
    val substitutions = linkedMapOf<TypeConstructorMarker, ConeCangJieType>()
    val extendTypeParameterConstructors = extend.typeParameters.mapTo(linkedSetOf<TypeConstructorMarker>()) {
        it.symbol.toLookupTag()
    }

    if (!matchExtendTargetType(
            pattern = targetPattern,
            actual = concreteReceiverType,
            extendTypeParameterConstructors = extendTypeParameterConstructors,
            substitutions = substitutions,
        )
    ) {
        return null
    }
    if (extendTypeParameterConstructors.any { it !in substitutions }) {
        return null
    }

    val substitutor = substitutions.takeIf { it.isNotEmpty() }?.let(::CfirTypeSubstitutorByMap)
        ?: ConeSubstitutor.Empty
    return CfirExtendDeclarationSubstitution(
        substitutor = substitutor,
        substitutedReceiverType = substitutor.substituteOrSelf(targetPattern),
    )
}

private fun matchExtendTargetType(
    pattern: ConeCangJieType,
    actual: ConeCangJieType,
    extendTypeParameterConstructors: Set<TypeConstructorMarker>,
    substitutions: MutableMap<TypeConstructorMarker, ConeCangJieType>,
): Boolean {
    return when (pattern) {
        is ConeTypeParameterType -> {
            val typeParameterConstructor = pattern.lookupTag
            if (typeParameterConstructor !in extendTypeParameterConstructors) {
                pattern == actual
            } else {
                val existing = substitutions[typeParameterConstructor]
                existing == null || existing == actual
            }.also { matches ->
                if (matches) {
                    substitutions.putIfAbsent(typeParameterConstructor, actual)
                }
            }
        }

        is ConePrimitiveType -> actual is ConePrimitiveType && pattern.kind == actual.kind

        is ConeLookupTagBasedType -> {
            val actualClassifier = actual as? ConeLookupTagBasedType ?: return false
            if (pattern.classIdOrPrimitiveClassId != actualClassifier.classIdOrPrimitiveClassId) return false
            if (pattern.typeArguments.size != actualClassifier.typeArguments.size) return false

            pattern.typeArguments.indices.all { index ->
                matchExtendTargetType(
                    pattern = pattern.typeArguments[index].type,
                    actual = actualClassifier.typeArguments[index].type,
                    extendTypeParameterConstructors = extendTypeParameterConstructors,
                    substitutions = substitutions,
                )
            }
        }

        else -> pattern == actual
    }
}

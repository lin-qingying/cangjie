package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.typeAwareSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.collectUpperBounds
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.type.AbstractTypeChecker
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

        val targetPattern = extend.extendedTypeRef.coneTypeOrNull ?: return null
        createExtendDeclarationSubstitution(session, extend, targetPattern, current)?.let { return it }

        queue.addAll(session.typeAwareSupertypeProviderOrNull?.getDirectSupertypes(current).orEmpty())
    }

    return null
}

/**
 * 在单个已确定 receiver 类型上匹配 extend 目标，并校验 extend 自身泛型约束。
 *
 * 官方编译器在成员访问候选过滤中通过 `CheckGenericDeclInstantiation` 删除不满足
 * extend 约束的目标；这里是 CFIR providers 层的等价入口，供类型感知父类型、
 * use-site member scope 和调用解析共享。
 */
fun createExtendDeclarationSubstitution(
    session: CfirSession,
    extend: CfirExtend,
    targetPattern: ConeCangJieType,
    concreteReceiverType: ConeCangJieType,
): CfirExtendDeclarationSubstitution? {
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
    if (!extend.satisfiesGenericConstraints(session, substitutor)) {
        return null
    }
    return CfirExtendDeclarationSubstitution(
        substitutor = substitutor,
        substitutedReceiverType = substitutor.substituteOrSelf(targetPattern),
    )
}

/**
 * 校验 extend 声明类型参数在当前 use-site 实例化后是否满足声明侧 upper bounds。
 *
 * 如果实际实参仍是类型参数，按官方 `CheckGenericDeclInstantiation` 的 generic 分支，
 * 允许它的任一已知上界满足目标上界。
 */
private fun CfirExtend.satisfiesGenericConstraints(
    session: CfirSession,
    substitutor: ConeSubstitutor,
): Boolean {
    for (typeParameter in typeParameters) {
        val typeParameterSymbol = typeParameter.symbol
        typeParameterSymbol.lazyResolveToPhase(CfirResolvePhase.TYPES)

        val actualType = substitutor.substituteOrSelf(typeParameterSymbol.constructType())
        for (bound in typeParameterSymbol.resolvedBounds) {
            val upperBound = substitutor.substituteOrSelf(bound.coneType)
            if (upperBound is ConeErrorType || actualType is ConeErrorType) return false
            if (!actualType.satisfiesUpperBound(session, upperBound, substitutor)) {
                return false
            }
        }
    }
    return true
}

private fun ConeCangJieType.satisfiesUpperBound(
    session: CfirSession,
    upperBound: ConeCangJieType,
    substitutor: ConeSubstitutor,
): Boolean {
    if (AbstractTypeChecker.isSubtypeOf(session.typeContext, this, upperBound)) {
        return true
    }

    val typeParameterType = this as? ConeTypeParameterType ?: return false
    return typeParameterType.collectUpperBounds(session.typeContext).any { actualUpperBound ->
        val substitutedUpperBound = substitutor.substituteOrSelf(actualUpperBound)
        substitutedUpperBound !is ConeErrorType &&
                AbstractTypeChecker.isSubtypeOf(session.typeContext, substitutedUpperBound, upperBound)
    }
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

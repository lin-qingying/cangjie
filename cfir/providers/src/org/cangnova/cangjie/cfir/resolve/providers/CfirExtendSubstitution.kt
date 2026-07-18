package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.typeAwareSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeCStringType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.abbreviatedType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.collectUpperBounds
import org.cangnova.cangjie.cfir.types.idealExtendLookupTypes
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * extend 目标类型与 use-site 接收者类型匹配后的替换结果。
 *
 * 这是 providers 层的共享语义入口：use-site substitution scope 与调用解析
 * receiver 检查必须使用同一套 extend 目标匹配规则，避免签名替换和候选适用性分叉。
 *
 * @property substitutor 从 extend 类型参数到 use-site 实参的替换器。
 * @property substitutedReceiverType 应用替换后的 receiver 类型。
 */
data class CfirExtendDeclarationSubstitution(
    /**
     * 从 extend 声明类型参数到当前 use-site receiver 实参的替换器。
     */
    val substitutor: ConeSubstitutor,
    /**
     * 将替换器应用到 extend 目标模式后得到的实际 receiver 类型。
     */
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
    return createExtendDeclarationSubstitution(
        session = session,
        extend = extend,
        targetPattern = targetPattern,
        concreteReceiverType = concreteReceiverType,
        checkGenericConstraints = true,
    )
}

/**
 * 只完成 extend 目标类型匹配，不过滤 where 约束。
 *
 * 调用解析和普通父类型查询必须使用 [createExtendDeclarationSubstitution]；
 * 这个入口只供约束系统把 `extend<T> Option<T> <: I where T <: I`
 * 派生成 `T <: I` 这样的初始约束，不能作为成员可见性或子类型成功的判据。
 */
fun createExtendDeclarationSubstitutionForConstraintDerivation(
    session: CfirSession,
    extend: CfirExtend,
    targetPattern: ConeCangJieType,
    concreteReceiverType: ConeCangJieType,
): CfirExtendDeclarationSubstitution? {
    return createExtendDeclarationSubstitution(
        session = session,
        extend = extend,
        targetPattern = targetPattern,
        concreteReceiverType = concreteReceiverType,
        checkGenericConstraints = false,
    )
}

/**
 * 内部实现：匹配 extend 目标类型，并按调用方要求决定是否校验 where/upper-bound 约束。
 */
private fun createExtendDeclarationSubstitution(
    session: CfirSession,
    extend: CfirExtend,
    targetPattern: ConeCangJieType,
    concreteReceiverType: ConeCangJieType,
    checkGenericConstraints: Boolean,
): CfirExtendDeclarationSubstitution? {
    val semanticTargetPattern = targetPattern.abbreviatedType ?: targetPattern
    val semanticReceiverType = if (semanticTargetPattern is ConeTypeAliasType) {
        concreteReceiverType
    } else {
        concreteReceiverType.fullyExpandedType(session)
    }
    val substitutions = linkedMapOf<TypeConstructorMarker, ConeCangJieType>()
    val extendTypeParameterConstructors = extend.typeParameters.mapTo(linkedSetOf<TypeConstructorMarker>()) {
        it.symbol.toLookupTag()
    }

    if (!matchExtendTargetType(
            pattern = semanticTargetPattern,
            actual = semanticReceiverType,
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
    if (checkGenericConstraints && !extend.matchesOfficialInstantiationShape(session, concreteReceiverType, substitutor)) {
        return null
    }
    if (checkGenericConstraints && !extend.satisfiesGenericConstraints(session, substitutor)) {
        return null
    }
    return CfirExtendDeclarationSubstitution(
        substitutor = substitutor,
        substitutedReceiverType = substitutor.substituteOrSelf(semanticTargetPattern),
    )
}

/**
 * 对齐官方 `TypeManager::CheckGenericDeclInstantiation` 的 extend 实例化过滤。
 *
 * 结构匹配只负责从 `extend<T> A<T>` 这类目标模式提取替换；真正的 use-site
 * 适用性还必须保证 extend 声明泛型逐个对应接收者展开后的顶层类型实参。
 * 因此 `extend<T> A<Box<T>>` 或 typealias 展开成更多顶层实参时，不能仅靠
 * 嵌套结构匹配把 `T` 绑定到内层类型后误认为该 extend 成立。
 */
private fun CfirExtend.matchesOfficialInstantiationShape(
    session: CfirSession,
    concreteReceiverType: ConeCangJieType,
    substitutor: ConeSubstitutor,
): Boolean {
    val expandedReceiverType = concreteReceiverType.fullyExpandedType(session) as? ConeLookupTagBasedType
        ?: return true
    val receiverArguments = expandedReceiverType.typeArguments.map { it.type }
    if (receiverArguments.isEmpty()) return true
    if (typeParameters.size != receiverArguments.size) return false

    return typeParameters.zip(receiverArguments).all { (typeParameter, receiverArgument) ->
        substitutor.substituteOrSelf(typeParameter.symbol.constructType()) == receiverArgument
    }
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

/**
 * 判断当前类型是否满足给定 upper bound。
 *
 * 对类型参数实参，会继续检查其已解析上界是否可以满足目标上界。
 */
private fun ConeCangJieType.satisfiesUpperBound(
    session: CfirSession,
    upperBound: ConeCangJieType,
    substitutor: ConeSubstitutor,
): Boolean {
    if (AbstractTypeChecker.isSubtypeOfWithoutOptionBoxing(session.typeContext, this, upperBound)) {
        return true
    }

    if (this is ConeTypeVariableType) {
        /*
         * fresh inference type variable 还不是最终实例化类型。
         * 这里必须先暴露 extend 父类型，让约束系统从 expected type、显式类型实参和实参类型共同求解；
         * 最终写回后的 concrete/type-parameter 类型仍会在同一入口用真实上界重新过滤。
         */
        return true
    }

    val typeParameterType = this as? ConeTypeParameterType ?: return false
    return typeParameterType.collectUpperBounds(session.typeContext).any { actualUpperBound ->
        val substitutedUpperBound = substitutor.substituteOrSelf(actualUpperBound)
        substitutedUpperBound !is ConeErrorType &&
                AbstractTypeChecker.isSubtypeOfWithoutOptionBoxing(
                    session.typeContext,
                    substitutedUpperBound,
                    upperBound,
                )
    }
}

/**
 * 递归匹配 extend 目标类型模式与实际 receiver 类型。
 *
 * 匹配成功时会把 extend 类型参数构造器写入 [substitutions]。
 */
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

        is ConePrimitiveType -> {
            actual is ConePrimitiveType && pattern.kind == actual.kind ||
                    actual.idealExtendLookupTypes.any { it.kind == pattern.kind }
        }

        is ConePointerType -> {
            val actualPointer = actual as? ConePointerType ?: return false
            matchExtendTargetType(
                pattern = pattern.pointeeType,
                actual = actualPointer.pointeeType,
                extendTypeParameterConstructors = extendTypeParameterConstructors,
                substitutions = substitutions,
            )
        }

        is ConeCStringType -> actual is ConeCStringType

        is ConeTypeAliasType -> {
            val actualAlias = actual as? ConeTypeAliasType ?: return false
            if (pattern.classId != actualAlias.classId) return false
            if (pattern.typeArguments.size != actualAlias.typeArguments.size) return false

            pattern.typeArguments.indices.all { index ->
                matchExtendTargetType(
                    pattern = pattern.typeArguments[index].type,
                    actual = actualAlias.typeArguments[index].type,
                    extendTypeParameterConstructors = extendTypeParameterConstructors,
                    substitutions = substitutions,
                )
            }
        }

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

package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.fullyExpandedTypeUsingAbbreviation
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
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.collectUpperBounds
import org.cangnova.cangjie.cfir.types.idealExtendLookupTypes
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.cfir.types.withoutAbbreviation
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
    /*
     * 官方 extend map 以 typealias 展开后的真实类型参与适用性匹配；alias 身份只属于
     * 声明/渲染元数据，不能成为另一套 receiver 等价关系。递归 matcher 会对每一层
     * 再做同样展开，使嵌套类型实参中的 alias 也遵守这一规则。
     */
    val semanticTargetPattern = targetPattern.semanticExtendMatchType(session)
    val semanticReceiverType = concreteReceiverType.semanticExtendMatchType(session)
    val substitutions = linkedMapOf<TypeConstructorMarker, ConeCangJieType>()
    val extendTypeParameterConstructors = extend.typeParameters.mapTo(linkedSetOf<TypeConstructorMarker>()) {
        it.symbol.toLookupTag()
    }

    if (!matchExtendTargetType(
            session = session,
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
    if (checkGenericConstraints && !extend.satisfiesGenericConstraints(session, substitutor)) {
        return null
    }
    return CfirExtendDeclarationSubstitution(
        substitutor = substitutor,
        substitutedReceiverType = substitutor.substituteOrSelf(semanticTargetPattern),
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
    /*
     * 官方 CheckGenericDeclInstantiation 使用普通 IsSubtype，允许 Option boxing；
     * extend where/upper-bound 不能另行收紧为“禁止装箱”的子类型关系。
     */
    if (AbstractTypeChecker.isSubtypeOf(session.typeContext, this, upperBound)) {
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
                AbstractTypeChecker.isSubtypeOf(
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
    session: CfirSession,
    pattern: ConeCangJieType,
    actual: ConeCangJieType,
    extendTypeParameterConstructors: Set<TypeConstructorMarker>,
    substitutions: MutableMap<TypeConstructorMarker, ConeCangJieType>,
): Boolean {
    val semanticPattern = pattern.semanticExtendMatchType(session)
    val semanticActual = actual.semanticExtendMatchType(session)
    return when (semanticPattern) {
        is ConeTypeParameterType -> {
            val typeParameterConstructor = semanticPattern.lookupTag
            if (typeParameterConstructor !in extendTypeParameterConstructors) {
                semanticActual is ConeTypeParameterType && semanticPattern.lookupTag == semanticActual.lookupTag
            } else {
                val existing = substitutions[typeParameterConstructor]
                existing == null || existing == semanticActual
            }.also { matches ->
                if (matches) {
                    substitutions.putIfAbsent(typeParameterConstructor, semanticActual)
                }
            }
        }

        is ConePrimitiveType -> {
            semanticActual is ConePrimitiveType && semanticPattern.kind == semanticActual.kind ||
                    semanticActual.idealExtendLookupTypes.any { it.kind == semanticPattern.kind }
        }

        is ConePointerType -> {
            val actualPointer = semanticActual as? ConePointerType ?: return false
            matchExtendTargetType(
                session = session,
                pattern = semanticPattern.pointeeType,
                actual = actualPointer.pointeeType,
                extendTypeParameterConstructors = extendTypeParameterConstructors,
                substitutions = substitutions,
            )
        }

        is ConeCStringType -> semanticActual is ConeCStringType

        is ConeTypeAliasType -> {
            // 已解析 alias 必须在 semanticExtendMatchType 中展开；未展开时不能按别名身份伪造适用性。
            false
        }

        is ConeLookupTagBasedType -> {
            val actualClassifier = semanticActual as? ConeLookupTagBasedType ?: return false
            if (semanticPattern.classIdOrPrimitiveClassId != actualClassifier.classIdOrPrimitiveClassId) return false
            if (semanticPattern.typeArguments.size != actualClassifier.typeArguments.size) return false

            semanticPattern.typeArguments.indices.all { index ->
                matchExtendTargetType(
                    session = session,
                    pattern = semanticPattern.typeArguments[index].type,
                    actual = actualClassifier.typeArguments[index].type,
                    extendTypeParameterConstructors = extendTypeParameterConstructors,
                    substitutions = substitutions,
                )
            }
        }

        is ConeTupleType -> {
            val actualTuple = semanticActual as? ConeTupleType ?: return false
            semanticPattern.elementTypes.matchExtendTypeList(
                session = session,
                actualTypes = actualTuple.elementTypes,
                extendTypeParameterConstructors = extendTypeParameterConstructors,
                substitutions = substitutions,
            )
        }

        is ConeFunctionType -> {
            val actualFunction = semanticActual as? ConeFunctionType ?: return false
            if (semanticPattern.isCFunc != actualFunction.isCFunc ||
                semanticPattern.isClosureType != actualFunction.isClosureType ||
                semanticPattern.hasVariableLenArg != actualFunction.hasVariableLenArg
            ) {
                return false
            }
            semanticPattern.parameterTypes.matchExtendTypeList(
                session = session,
                actualTypes = actualFunction.parameterTypes,
                extendTypeParameterConstructors = extendTypeParameterConstructors,
                substitutions = substitutions,
            ) && matchExtendTargetType(
                session = session,
                pattern = semanticPattern.returnType,
                actual = actualFunction.returnType,
                extendTypeParameterConstructors = extendTypeParameterConstructors,
                substitutions = substitutions,
            )
        }

        is ConeVArrayType -> {
            val actualArray = semanticActual as? ConeVArrayType ?: return false
            semanticPattern.size == actualArray.size && matchExtendTargetType(
                session = session,
                pattern = semanticPattern.elementType,
                actual = actualArray.elementType,
                extendTypeParameterConstructors = extendTypeParameterConstructors,
                substitutions = substitutions,
            )
        }

        else -> semanticPattern == semanticActual
    }
}

/**
 * 返回 extend 目标匹配使用的真实类型视图。
 *
 * 每一层递归都执行该转换，保证 `A<Alias<T>>` 与 `A<Real<Int64>>` 也按展开后的
 * 类型结构生成同一份类型参数映射；缩写属性继续保留在原始 type ref 中供 IDE 使用。
 */
private fun ConeCangJieType.semanticExtendMatchType(session: CfirSession): ConeCangJieType =
    fullyExpandedTypeUsingAbbreviation(session)

/** 逐位置递归匹配同一类型构造器的子类型列表。 */
private fun List<ConeCangJieType>.matchExtendTypeList(
    session: CfirSession,
    actualTypes: List<ConeCangJieType>,
    extendTypeParameterConstructors: Set<TypeConstructorMarker>,
    substitutions: MutableMap<TypeConstructorMarker, ConeCangJieType>,
): Boolean {
    if (size != actualTypes.size) return false
    return indices.all { index ->
        matchExtendTargetType(
            session = session,
            pattern = this[index],
            actual = actualTypes[index],
            extendTypeParameterConstructors = extendTypeParameterConstructors,
            substitutions = substitutions,
        )
    }
}

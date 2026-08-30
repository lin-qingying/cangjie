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
import org.cangnova.cangjie.cfir.types.forEachType
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
 * 判断 extend 接口边在官方赋值/子类型谓词语义下是否成立。
 *
 * 官方 `TypeManager::HasExtendInterfaceTyHelper` 为谓词路径构建映射时，
 * 在查询类型的原始拼写层取 `extendedType` 的**顶层实参**，且只对其中
 * "直接的类型参数"（`DynamicCast<TyVar*>` 命中）建立代换；
 * 嵌套实参（如 `extend<X> A<B<X>>` 中的 `B<X>`）不会展开。等价判据为：
 * 接口类型中引用到的 extend 类型形参必须全部能由 direct 映射覆盖——
 * 覆盖不全的边在官方路径上保持含 TyVar 的形状，永远无法与具体目标匹配。于是：
 *
 * - `extend<X> A<X> <: I<X>` 与 `A<Int64>`：映射 `{X|->Int64}` 覆盖 `I<X>` 中的 `X`，边成立；
 * - `extend<X> A<B<X>> <: I<Int64>`：接口实参是常量，无需覆盖，边成立；
 * - `extend<X> A<B<X>> <: I<X>`：映射为空，`I<X>` 中的 `X` 无法覆盖，边不成立（cjc 报 mismatched types）；
 * - `extend<X, Y> A<X, B<Y>> <: I<X>`：`X` 直接映射覆盖，`Y` 未被接口引用，边成立；
 * - `extend<Y> C<Y> <: I<Y>` 与 `C<Int64>`（`type C<X> = GennericClassA<Array<X>, Array<X>>`）：
 *   官方在 alias 拼写层匹配，`C<Y>` 的顶层实参 `Y` 是 TyVar，映射 `{Y|->Int64}` 覆盖 `I<Y>`，边成立。
 *   若先展开 typealias 再取顶层实参，`Y` 会沉入 `Array<Y>` 而被误判，因此 [rawReceiverType]
 *   必须携带查询入口的原始拼写。
 *
 * 成员查找、可达性遍历等结构化语义（官方 `GetAllExtendInterfaceTyHelper`）不受本判据影响。
 *
 * @param rawReceiverType 查询入口的原始拼写类型（未做 typealias 展开）。
 *   superclass 传播链上的中间类型没有独立的用户拼写，传 `null`，
 *   此时按语义展开层判定——该层与官方"无 alias 拼写可保留"的行为一致。
 */
fun isExtendSuperTypeRefPredicateVisible(
    session: CfirSession,
    extend: CfirExtend,
    targetPattern: ConeCangJieType,
    concreteReceiverType: ConeCangJieType,
    superTypeRefType: ConeCangJieType,
    rawReceiverType: ConeCangJieType? = null,
): Boolean {
    val extendTypeParameterConstructors = extend.typeParameters.mapTo(linkedSetOf<TypeConstructorMarker>()) {
        it.symbol.toLookupTag()
    }
    if (extendTypeParameterConstructors.isEmpty()) return true

    // 官方等价判据：direct-only 映射必须覆盖接口类型中引用到的全部 extend 类型形参。
    // 覆盖完整的边等价于官方 `GetInstantiatedTy` 产出的可实例化边；覆盖不全的边
    // 在官方路径上保持含 TyVar 的形状，永远无法与具体目标类型匹配。
    val referencedExtendParameters = mutableSetOf<TypeConstructorMarker>()
    superTypeRefType.forEachType { type ->
        if (type is ConeTypeParameterType && type.lookupTag in extendTypeParameterConstructors) {
            referencedExtendParameters += type.lookupTag
        }
    }
    if (referencedExtendParameters.isEmpty()) return true

    // alias 拼写层 direct 映射：官方 `GetTypeArgs(ty)` 保留 typealias 引用的原始实参，
    // `C<Y>` 对 `C<Int64>` 在这一层才是 `Y` 的 direct 位置。构造器不一致说明
    // 查询侧拼写已被展开或改写，官方在 size/匹配检查下同样无法建立映射，
    // 此时回退到语义展开层的顶层判定。
    val rawPatternArguments = targetPattern.extendPredicateTypeArguments()
    val rawReceiverArguments = rawReceiverType?.extendPredicateTypeArguments()
    val sameRawConstructor = rawReceiverType != null &&
            rawPatternArguments.size == rawReceiverArguments?.size &&
            targetPattern.hasSameExtendPredicateConstructor(rawReceiverType)

    val directSubstitutions = linkedMapOf<TypeConstructorMarker, ConeCangJieType>()
    if (sameRawConstructor) {
        rawPatternArguments.forEachIndexed { index, patternType ->
            if (patternType is ConeTypeParameterType && patternType.lookupTag in extendTypeParameterConstructors) {
                rawReceiverArguments?.getOrNull(index)?.let { receiverArgument ->
                    directSubstitutions.putIfAbsent(patternType.lookupTag, receiverArgument)
                }
            }
        }
    } else {
        val semanticPattern = targetPattern.semanticExtendMatchType(session)
        val semanticReceiver = concreteReceiverType.semanticExtendMatchType(session)
        val patternArguments = semanticPattern.extendPredicateTypeArguments()
        val receiverArguments = semanticReceiver.extendPredicateTypeArguments()
        patternArguments.forEachIndexed { index, patternArgument ->
            val patternType = patternArgument
            if (patternType is ConeTypeParameterType && patternType.lookupTag in extendTypeParameterConstructors) {
                receiverArguments.getOrNull(index)?.let { receiverArgument ->
                    directSubstitutions.putIfAbsent(patternType.lookupTag, receiverArgument)
                }
            }
        }
    }

    return referencedExtendParameters.all { it in directSubstitutions }
}

/**
 * 取得官方 `TypeManager::GetTypeArgs` 对当前类型的直接类型实参。
 *
 * `ConePointerType` 的通用 [ConeCangJieType.typeArguments] 为空，但官方
 * `PointerTy` 仍把 pointee 放在 `typeArgs[0]`；函数与元组同样需要暴露其
 * 结构性实参，才能让 extend 的顶层 direct mapping 与官方保持一致。
 */
private fun ConeCangJieType.extendPredicateTypeArguments(): List<ConeCangJieType> = when (this) {
    is ConeFunctionType -> parameterTypes + returnType
    is ConeTupleType -> elementTypes
    is ConePointerType -> listOf(pointeeType)
    else -> typeArguments.map { it.type }
}

/** 判断两个类型是否具有同一个 extend direct-mapping 构造器。 */
private fun ConeCangJieType.hasSameExtendPredicateConstructor(other: ConeCangJieType): Boolean = when (this) {
    is ConeFunctionType -> other is ConeFunctionType &&
            isCFunc == other.isCFunc &&
            isClosureType == other.isClosureType &&
            hasVariableLenArg == other.hasVariableLenArg &&
            parameterTypes.size == other.parameterTypes.size
    is ConeTupleType -> other is ConeTupleType && elementTypes.size == other.elementTypes.size
    is ConePointerType -> other is ConePointerType
    is ConeVArrayType -> other is ConeVArrayType && size == other.size
    is ConeLookupTagBasedType -> other is ConeLookupTagBasedType &&
            classIdOrPrimitiveClassId == other.classIdOrPrimitiveClassId
    is ConeTypeAliasType -> other is ConeTypeAliasType && classId == other.classId
    else -> this::class == other::class
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

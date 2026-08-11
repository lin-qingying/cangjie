package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.copyWithNewSource
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.resolve.calls.CommonSuperTypeCalculator
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.supertypes
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withCfirEntry

/**
 * 复制 resolved type ref，并替换 source 与 cone type。
 *
 * 如果原 type ref 是错误 type ref，会保留诊断和部分解析 type ref 信息。
 */
fun CfirResolvedTypeRef.withReplacedSourceAndType(newSource: CjSourceElement?, newType: ConeCangJieType): CfirResolvedTypeRef {
    val originalPartiallyResolvedTypeRef = (this as? CfirErrorTypeRef)
        ?.partiallyResolvedTypeRef
        ?.let { typeRef ->
            if (newSource != null) {
                typeRef.copyWithNewSource(newSource)
            } else {
                typeRef
            }
        }

    return when {
        newType is ConeErrorType -> {
            buildErrorTypeRef {
                source = newSource
                coneType = newType
                annotations += this@withReplacedSourceAndType.annotations
                diagnostic = newType.diagnostic
                partiallyResolvedTypeRef = originalPartiallyResolvedTypeRef
            }
        }
        this is CfirErrorTypeRef -> {
            buildErrorTypeRef {
                source = newSource
                coneType = newType
                annotations += this@withReplacedSourceAndType.annotations
                diagnostic = this@withReplacedSourceAndType.diagnostic
                delegatedTypeRef = this@withReplacedSourceAndType.delegatedTypeRef
                partiallyResolvedTypeRef = originalPartiallyResolvedTypeRef
            }
        }
        else -> {
            buildResolvedTypeRef {
                source = newSource
                coneType = newType
                annotations += this@withReplacedSourceAndType.annotations
                delegatedTypeRef = this@withReplacedSourceAndType.delegatedTypeRef
            }
        }
    }
}

/**
 * 收集类型参数类型的所有有效上界。
 */
fun ConeTypeParameterType.collectUpperBounds(typeContext: ConeTypeContext): Set<ConeCangJieType> {
    val upperBounds = linkedSetOf<ConeCangJieType>()
    val seen = linkedSetOf<ConeCangJieType>()

    fun collect(type: ConeCangJieType) {
        if (!seen.add(type)) return

        when (type) {
            is ConeErrorType -> return
            is ConeTypeParameterType -> {
                type.lookupTag.collectUpperBoundsTo(::collect)
            }
            is ConeTypeVariableType -> {
                val originalTypeParameter = type.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag ?: return
                originalTypeParameter.collectUpperBoundsTo(::collect)
            }
            is ConeIntersectionType -> type.intersectedTypes.forEach(::collect)
            else -> upperBounds += type
        }
    }

    collect(this)
    return upperBounds
}

/**
 * operator 解析使用的操作数结构化分类。
 *
 * 类型参数的 primitive 上界虽然在声明侧非法，仍必须保留其“直接 primitive 上界”事实：
 * 该事实可参与理想字面量签名选择和逻辑运算子类型检查，但不能把类型参数伪装成普通
 * primitive 值接收者并开放成员 scope。
 */
sealed interface ConeOperatorOperandClassification {
    /** 操作数未经近似的原始类型。 */
    val type: ConeCangJieType

    /** 可直接进入内建 operator 表的 primitive 操作数。 */
    data class Primitive(
        override val type: ConeCangJieType,
        val kind: PrimitiveTypeKind,
    ) : ConeOperatorOperandClassification

    /** 声明了直接 primitive 上界的类型参数或其 fresh type variable。 */
    data class PrimitiveUpperBoundTypeParameter(
        override val type: ConeCangJieType,
        val primitiveUpperBounds: Set<ConePrimitiveType>,
        val hasInvalidDeclaredUpperBounds: Boolean,
    ) : ConeOperatorOperandClassification

    /** 已存在的根错误，后续 operator 只能传播，不能重新分类。 */
    data class Error(
        override val type: ConeErrorType,
    ) : ConeOperatorOperandClassification

    /** 不属于 primitive operator 特殊语义的普通类型。 */
    data class Other(
        override val type: ConeCangJieType,
    ) : ConeOperatorOperandClassification
}

/**
 * 按声明类型和直接上界分类 operator 操作数，不执行成员查找或候选解析。
 */
fun ConeCangJieType.classifyOperatorOperand(session: CfirSession): ConeOperatorOperandClassification {
    if (this is ConeErrorType) return ConeOperatorOperandClassification.Error(this)

    BuiltinPrimitiveOperators.primitiveOperandKind(this)?.let { kind ->
        return ConeOperatorOperandClassification.Primitive(this, kind)
    }

    val lookupTag = when (this) {
        is ConeTypeParameterType -> lookupTag
        is ConeTypeVariableType -> typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag
        else -> null
    } ?: return ConeOperatorOperandClassification.Other(this)

    val primitiveUpperBounds = lookupTag.declaredUpperBoundRefsAfterTypeResolve()
        .mapNotNull { boundRef -> boundRef.declaredUpperBoundConeTypeOrNull() }
        .map { boundType -> boundType.fullyExpandedType(session) }
        .mapNotNull(BuiltinPrimitiveOperators::normalizePrimitiveOperand)
        .toSet()
    if (primitiveUpperBounds.isEmpty()) return ConeOperatorOperandClassification.Other(this)

    return ConeOperatorOperandClassification.PrimitiveUpperBoundTypeParameter(
        type = this,
        primitiveUpperBounds = primitiveUpperBounds,
        hasInvalidDeclaredUpperBounds = lookupTag.hasInvalidDeclaredUpperBounds(session),
    )
}

/**
 * 判断类型参数声明侧 upper bounds 是否已经违反 class/interface 上界规则。
 *
 * 官方 `GenericsTy::isUpperBoundLegal` 会阻断基于非法上界的成员查找，避免在根因
 * 上界错误之后继续报告访问/调用级联错误。CFIR 不存储该标志，因此在类型系统层
 * 从已解析 bounds 派生相同语义，供 receiver scope、static qualifier 和调用解析共享。
 */
fun ConeTypeParameterType.hasInvalidDeclaredUpperBounds(session: CfirSession): Boolean =
    lookupTag.hasInvalidDeclaredUpperBounds(session)

/**
 * 判断当前类型是否是带非法声明上界的类型参数或其 fresh type variable。
 */
fun ConeCangJieType.isTypeParameterWithInvalidDeclaredUpperBounds(session: CfirSession): Boolean = when (this) {
    is ConeTypeParameterType -> hasInvalidDeclaredUpperBounds(session)
    is ConeTypeVariableType ->
        (typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag)
            ?.hasInvalidDeclaredUpperBounds(session) == true
    else -> false
}

/**
 * 判断 lookup tag 对应的类型参数是否具有非法声明上界。
 */
fun ConeTypeParameterLookupTag.hasInvalidDeclaredUpperBounds(session: CfirSession): Boolean {
    val boundRefs = declaredUpperBoundRefsAfterTypeResolve()
    if (boundRefs.any { it.isDefinitelyIllegalDeclaredUpperBound(session) }) return true

    val bounds = boundRefs
        .mapNotNull { it.declaredUpperBoundConeTypeOrNull() }
        .filterNot { it is ConeErrorType }
        .distinct()
    if (bounds.isEmpty()) return false

    if (bounds.any { !it.isLegalDeclaredUpperBound(session) }) return true

    val classBounds = bounds.mapNotNull { it.declaredClassUpperBoundOrNull(session) }
    return classBounds.size > 1 && !classBounds.areInSingleInheritanceChain(session)
}

/**
 * 返回类型参数在 TYPES 阶段后的声明上界原始列表。
 *
 * 非法 function/tuple 上界可能仍保持 raw type-ref 形态；这里不能走 [CfirTypeParameterSymbol.resolvedBounds]
 * 的强制已解析视图，否则会把官方上界诊断前置成内部异常。
 */
fun ConeTypeParameterLookupTag.declaredUpperBoundRefsAfterTypeResolve(): List<CfirTypeRef> {
    typeParameterSymbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
    return typeParameterSymbol.cfir.bounds
}

/**
 * 从声明上界 type-ref 提取可用于规则判断和诊断渲染的 cone type。
 */
fun CfirTypeRef.declaredUpperBoundConeTypeOrNull(): ConeCangJieType? {
    coneTypeOrNull?.let { return it }
    return when (this) {
        is CfirFunctionTypeRef -> {
            val parameterTypes = parameterTypeRefs.map { it.declaredUpperBoundConeTypeOrNull() }
            val returnType = returnTypeRef.declaredUpperBoundConeTypeOrNull()
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
            val elementTypes = elementTypeRefs.map { it.declaredUpperBoundConeTypeOrNull() }
            if (elementTypes.any { it == null }) {
                null
            } else {
                ConeTupleType(elementTypes.filterNotNull())
            }
        }
        else -> null
    }
}

/**
 * 语法形态已经确定不可能是 class/interface 上界的 raw type-ref。
 */
private fun CfirTypeRef.isDefinitelyIllegalDeclaredUpperBound(session: CfirSession): Boolean {
    declaredUpperBoundConeTypeOrNull()?.let { return !it.isLegalDeclaredUpperBound(session) }
    return when (this) {
        is CfirFunctionTypeRef,
        is CfirTupleTypeRef -> true
        else -> false
    }
}

/**
 * 判断单个声明上界是否满足官方 class/interface 上界准入规则。
 */
/**
 * 判断单个声明上界是否满足仓颉 class/interface 上界准入规则。
 *
 * 该判定同时供声明检查、调用约束初始化和 use-site 上界诊断使用；非法声明上界
 * 只能由声明侧 checker 报告，不能作为普通泛型约束继续参与后续推断。
 */
fun ConeCangJieType.isLegalDeclaredUpperBound(session: CfirSession): Boolean {
    val expandedType = fullyExpandedType(session)
    if (expandedType === ConeAnyType) return true

    val classId = expandedType.classIdOrPrimitiveClassId
    if (classId == StdlibClassIds.Any || CfirCTypeSemantics.isCTypeClassId(classId)) return true

    return expandedType is ConeClassLikeType
}

/**
 * 提取需要参与“多个 class 上界必须在同一继承链”规则的 class 上界。
 */
private fun ConeCangJieType.declaredClassUpperBoundOrNull(session: CfirSession): ConeCangJieType? {
    val expandedType = fullyExpandedType(session)
    if (expandedType !is ConeClassLikeType || expandedType.isInterface) return null
    if (expandedType.classId == StdlibClassIds.Any || CfirCTypeSemantics.isCTypeClassId(expandedType.classId)) return null
    return expandedType
}

/**
 * 判断 class 上界集合是否位于同一继承链。
 */
private fun List<ConeCangJieType>.areInSingleInheritanceChain(session: CfirSession): Boolean {
    for (leftIndex in indices) {
        for (rightIndex in leftIndex + 1 until size) {
            val left = this[leftIndex]
            val right = this[rightIndex]
            if (!AbstractTypeChecker.isSubtypeOf(session.typeContext, left, right) &&
                !AbstractTypeChecker.isSubtypeOf(session.typeContext, right, left)
            ) {
                return false
            }
        }
    }
    return true
}

/**
 * 判断当前类型或其父类型链中是否存在指定 [classId]。
 */
fun ConeCangJieType.hasSupertypeWithGivenClassId(classId: org.cangnova.cangjie.name.ClassId, typeContext: ConeTypeContext): Boolean {
    val seen = linkedSetOf<ConeCangJieType>()

    fun visit(type: ConeCangJieType): Boolean {
        if (!seen.add(type)) return false
        if (type.classIdOrPrimitiveClassId == classId) return true

        return when (type) {
            is ConeErrorType -> false
            is ConeTypeParameterType -> type.collectUpperBounds(typeContext).any(::visit)
            is ConeTypeVariableType -> {
                val originalTypeParameter = type.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag ?: return false
                originalTypeParameter.collectUpperBounds().any(::visit)
            }
            is ConeIntersectionType -> type.intersectedTypes.any(::visit)
            else -> {
                val constructor = (type as? ConeRigidType)?.getConstructor() ?: return false
                val directSupertypes = with(typeContext) {
                    val unsubstitutedSupertypes = constructor.supertypes().filterIsInstance<ConeCangJieType>()
                    val inferenceContext = this as? ConeInferenceContext
                    val substitutor = inferenceContext?.createSubstitutorForSuperTypes(type)
                    if (substitutor == null || inferenceContext == null) {
                        unsubstitutedSupertypes
                    } else {
                        unsubstitutedSupertypes.map { supertype ->
                            with(inferenceContext) {
                                substitutor.safeSubstitute(supertype) as ConeCangJieType
                            }
                        }
                    }
                }
                directSupertypes.any(::visit)
            }
        }
    }

    return visit(this)
}

/**
 * 解析类型参数 lookup tag 的直接上界。
 */
private fun ConeTypeParameterLookupTag.collectUpperBounds(): List<ConeCangJieType> {
    return declaredUpperBoundRefsAfterTypeResolve()
        .mapNotNull { it.declaredUpperBoundConeTypeOrNull() }
        .filterNot { it is ConeErrorType }
}

/**
 * 将类型参数上界逐个传给 [collect]。
 */
private inline fun ConeTypeParameterLookupTag.collectUpperBoundsTo(collect: (ConeCangJieType) -> Unit) {
    collectUpperBounds().forEach(collect)
}

/**
 * 计算一组类型的公共父类型；空列表返回 `null`。
 */
fun ConeInferenceContext.commonSuperTypeOrNull(types: List<ConeCangJieType>): ConeCangJieType? {
    return when (types.size) {
        0 -> null
        1 -> types.first()
        else -> with(CommonSuperTypeCalculator) {
            commonSuperType(types).asCone()
        }
    }
}

/**
 * 返回替换顶层 attributes 后的新类型。
 */
@Suppress("UNCHECKED_CAST")
fun <T : ConeCangJieType> T.withAttributes(attributes: ConeAttributes): T {
    if (this.attributes == attributes) {
        return this
    }

    return when (this) {
        is ConeClassLikeType -> ConeClassLikeType(lookupTag, typeArguments, attributes, isInterface, isThisType)
        is ConeStructType -> ConeStructType(lookupTag, typeArguments, attributes)
        is ConeEnumType -> ConeEnumType(lookupTag, typeArguments, attributes, isRefEnum)
        is ConePrimitiveType -> ConePrimitiveType(kind, attributes)
        is ConeCStringType -> ConeCStringType(attributes)
        is ConeTypeParameterType -> ConeTypeParameterTypeImpl(lookupTag, attributes)
        is ConeFunctionType -> ConeFunctionType(parameterTypes, returnType, isCFunc, isClosureType, hasVariableLenArg, attributes)
        is ConeTupleType -> ConeTupleType(elementTypes, attributes)
        is ConeVArrayType -> ConeVArrayType(elementType, size, attributes)
        is ConePointerType -> ConePointerType(pointeeType, attributes)
        is ConeIntersectionType -> ConeIntersectionType(
            intersectedTypes = intersectedTypes,
            upperBoundForApproximation = upperBoundForApproximation,
            attributes = attributes,
        )
        is ConeUnionType -> ConeUnionType(unionTypes, attributes)
        is ConeTypeAliasType -> ConeTypeAliasType(classId, expandedType, typeArguments, attributes)
        is ConeErrorType -> ConeErrorType(diagnostic, isUninferredParameter, delegatedType, typeArguments, attributes)
        is ConeQuestType -> ConeQuestType(attributes)
        is ConeTypeVariableType -> ConeTypeVariableType(typeConstructor, attributes)
        is ConePlaceholderType -> ConePlaceholderType(debugName, attributes)
        else -> this
    } as T
}

/**
 * 给当前类型添加顶层 typealias abbreviation 属性。
 */
fun <T : ConeCangJieType> T.withAbbreviation(attribute: AbbreviatedTypeAttribute): T {
    val clearedAttributes = attributes.abbreviatedType?.let(attributes::remove) ?: attributes
    return withAttributes(clearedAttributes.add(attribute))
}

/**
 * 移除当前类型顶层携带的 typealias 缩写视图。
 *
 * CFIR 内部展开类型会保留 [AbbreviatedTypeAttribute] 供引用、渲染和调试读取；
 * Analysis API 暴露 `fullyExpandedType` 时需要取得真正的展开视图，因此通过该 helper
 * 在保持其他类型属性不变的前提下只删除 abbreviation attribute。
 */
fun <T : ConeCangJieType> T.withoutAbbreviation(): T {
    val clearedAttributes = attributes.abbreviatedType?.let(attributes::remove) ?: return this
    return withAttributes(clearedAttributes)
}

/**
 * 返回替换类型实参后的新类型。
 */
fun <T : ConeCangJieType> T.withArguments(arguments: List<ConeTypeProjection>): T {
    if (typeArguments == arguments) {
        return this
    }

    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is ConeClassLikeType -> ConeClassLikeType(lookupTag, arguments, attributes, isInterface, isThisType)
        is ConeStructType -> ConeStructType(lookupTag, arguments, attributes)
        is ConeEnumType -> ConeEnumType(lookupTag, arguments, attributes, isRefEnum)
        is ConeFunctionType -> {
            val parameterTypes = arguments.dropLast(1).map { it.type }
            val returnType = arguments.lastOrNull()?.type ?: return this
            ConeFunctionType(parameterTypes, returnType, isCFunc, isClosureType, hasVariableLenArg, attributes)
        }
        is ConeTupleType -> ConeTupleType(arguments.map { it.type }, attributes)
        is ConeVArrayType -> ConeVArrayType(arguments.firstOrNull()?.type ?: return this, size, attributes)
        is ConePointerType -> ConePointerType(arguments.firstOrNull()?.type ?: return this, attributes)
        is ConeIntersectionType -> ConeIntersectionType(
            intersectedTypes = arguments.map { it.type },
            upperBoundForApproximation = upperBoundForApproximation,
            attributes = attributes,
        )
        is ConeUnionType -> ConeUnionType(arguments.map { it.type }.toSet(), attributes)
        is ConeTypeAliasType -> ConeTypeAliasType(classId, expandedType, arguments, attributes)
        is ConeErrorType -> ConeErrorType(diagnostic, isUninferredParameter, delegatedType, arguments, attributes)
        else -> errorWithAttachment("Not supported: ${this::class}") {
            withCfirEntry("type", this@withArguments)
        }
    } as T
}

/**
 * 按 [replacement] 批量替换类型实参。
 */
inline fun <T : ConeCangJieType> T.withArguments(
    replacement: (ConeTypeProjection) -> ConeTypeProjection,
): T {
    if (typeArguments.isEmpty()) return this
    val newArguments = ArrayList<ConeTypeProjection>(typeArguments.size)
    var changed = false
    for (argument in typeArguments) {
        val replaced = replacement(argument)
        newArguments += replaced
        if (replaced !== argument) {
            changed = true
        }
    }
    return if (!changed) this else withArguments(newArguments)
}

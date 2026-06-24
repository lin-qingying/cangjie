package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.copyWithNewSource
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.resolve.calls.CommonSuperTypeCalculator
import org.cangnova.cangjie.source.CjSourceElement
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
    typeParameterSymbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
    return typeParameterSymbol.resolvedBounds.map { it.coneType }
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

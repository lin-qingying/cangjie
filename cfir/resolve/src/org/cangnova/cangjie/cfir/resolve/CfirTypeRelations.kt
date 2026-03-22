package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.types.ConeArrayType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeFuncType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeQuestType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTypeContext
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeUnionType

enum class CfirTypeRelationResult {
    IDENTICAL,
    SUBTYPE,
    COMPATIBLE_WITH_CONVERSION,
    COMPATIBLE_WITH_QUEST_FALLBACK,
    CONSTRAINABLE_BY_VARIABLES,
    INCOMPATIBLE,
}

class CfirTypeRelations(
    private val typeContext: ConeTypeContext,
) {
    fun compare(source: ConeCangJieType, target: ConeCangJieType): CfirTypeRelationResult {
        if (source == target) return CfirTypeRelationResult.IDENTICAL

        if (source is ConeQuestType || target is ConeQuestType) {
            return CfirTypeRelationResult.COMPATIBLE_WITH_QUEST_FALLBACK
        }

        if (shouldConstrainByVariables(source, target)) {
            return CfirTypeRelationResult.CONSTRAINABLE_BY_VARIABLES
        }

        if (isIdealNumericConversion(source, target)) {
            return CfirTypeRelationResult.COMPATIBLE_WITH_CONVERSION
        }

        if (isSubtypeByContext(source, target)) {
            return CfirTypeRelationResult.SUBTYPE
        }

        return CfirTypeRelationResult.INCOMPATIBLE
    }

    fun isSubtype(source: ConeCangJieType, target: ConeCangJieType): Boolean {
        return compare(source, target) in setOf(
            CfirTypeRelationResult.IDENTICAL,
            CfirTypeRelationResult.SUBTYPE,
            CfirTypeRelationResult.COMPATIBLE_WITH_CONVERSION,
            CfirTypeRelationResult.COMPATIBLE_WITH_QUEST_FALLBACK,
            CfirTypeRelationResult.CONSTRAINABLE_BY_VARIABLES,
        )
    }

    fun isIdentical(source: ConeCangJieType, target: ConeCangJieType): Boolean {
        return compare(source, target) == CfirTypeRelationResult.IDENTICAL
    }

    fun isCompatible(source: ConeCangJieType, target: ConeCangJieType): Boolean {
        return compare(source, target) != CfirTypeRelationResult.INCOMPATIBLE
    }

    fun canUseQuestFallback(source: ConeCangJieType, target: ConeCangJieType): Boolean {
        return compare(source, target) == CfirTypeRelationResult.COMPATIBLE_WITH_QUEST_FALLBACK
    }

    fun isConstrainableByVariables(source: ConeCangJieType, target: ConeCangJieType): Boolean {
        return compare(source, target) == CfirTypeRelationResult.CONSTRAINABLE_BY_VARIABLES
    }

    private fun shouldConstrainByVariables(source: ConeCangJieType, target: ConeCangJieType): Boolean {
        return isPlaceholderTypeVariable(source) || isPlaceholderTypeVariable(target)
    }

    private fun isPlaceholderTypeVariable(type: ConeCangJieType): Boolean {
        return type is ConeTypeParameterType && type.isPlaceholder
    }

    private fun isIdealNumericConversion(source: ConeCangJieType, target: ConeCangJieType): Boolean {
        if (source !is ConePrimitiveType || target !is ConePrimitiveType) return false

        return when (source.kind) {
            org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.IDEAL_INT ->
                target.kind in setOf(
                    org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.INT8,
                    org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.INT16,
                    org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.INT32,
                    org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.INT64,
                    org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.UINT8,
                    org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.UINT16,
                    org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.UINT32,
                    org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.UINT64,
                )

            org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.IDEAL_FLOAT ->
                target.kind in setOf(
                    org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.FLOAT16,
                    org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.FLOAT32,
                    org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.FLOAT64,
                )

            else -> false
        }
    }

    private fun isSubtypeByContext(source: ConeCangJieType, target: ConeCangJieType): Boolean {
        // TypeAlias 透明展开
        val unwrappedSource = unwrapTypeAlias(source)
        val unwrappedTarget = unwrapTypeAlias(target)
        if (unwrappedSource !== source || unwrappedTarget !== target) {
            return isSubtypeByContext(unwrappedSource, unwrappedTarget)
        }

        if (typeContext.isSameTypeConstructor(source, target)) return true

        // 交集类型：A <: B & C iff A <: B AND A <: C
        if (target is ConeIntersectionType) {
            return target.intersectedTypes.all { isCompatibleNotIncompatible(source, it) }
        }

        // 交集类型：A & B <: C iff A <: C OR B <: C
        if (source is ConeIntersectionType) {
            return source.intersectedTypes.any { isCompatibleNotIncompatible(it, target) }
        }

        // 联合类型：A <: B | C iff A <: B OR A <: C
        if (target is ConeUnionType) {
            return target.unionTypes.any { isCompatibleNotIncompatible(source, it) }
        }

        // 联合类型：A | B <: C iff A <: C AND B <: C
        if (source is ConeUnionType) {
            return source.unionTypes.all { isCompatibleNotIncompatible(it, target) }
        }

        return when {
            source is ConeClassLikeType || source is ConeStructType || source is ConeEnumType ->
                typeContext.supertypes(source).any { compare(it, target) != CfirTypeRelationResult.INCOMPATIBLE }

            source is ConeFuncType && target is ConeFuncType ->
                source.parameterTypes.size == target.parameterTypes.size &&
                    source.parameterTypes.zip(target.parameterTypes).all { (s, t) -> compare(t, s) != CfirTypeRelationResult.INCOMPATIBLE } &&
                    compare(source.returnType, target.returnType) != CfirTypeRelationResult.INCOMPATIBLE

            source is ConeTupleType && target is ConeTupleType ->
                source.elementTypes.size == target.elementTypes.size &&
                    source.elementTypes.zip(target.elementTypes).all { (s, t) -> compare(s, t) != CfirTypeRelationResult.INCOMPATIBLE }

            // Array 不变性
            source is ConeArrayType && target is ConeArrayType ->
                source.dims == target.dims && compare(source.elementType, target.elementType) == CfirTypeRelationResult.IDENTICAL

            // Pointer 不变性
            source is ConePointerType && target is ConePointerType ->
                compare(source.pointeeType, target.pointeeType) == CfirTypeRelationResult.IDENTICAL

            else -> false
        }
    }

    private fun isCompatibleNotIncompatible(source: ConeCangJieType, target: ConeCangJieType): Boolean {
        return compare(source, target) != CfirTypeRelationResult.INCOMPATIBLE
    }

    private fun unwrapTypeAlias(type: ConeCangJieType): ConeCangJieType {
        return if (type is ConeTypeAliasType) type.expandedType ?: type else type
    }
}

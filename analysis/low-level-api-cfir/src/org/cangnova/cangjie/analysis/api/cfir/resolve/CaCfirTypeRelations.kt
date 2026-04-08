package org.cangnova.cangjie.analysis.api.cfir.resolve

import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.types.ConeAnyType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeCStringType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFuncType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConeNumericWidening
import org.cangnova.cangjie.cfir.types.ConePlaceholderType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeQuestType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.IdealTypeResolver

/**
 * `analysis` 层自持的 low-level 类型关系引擎。
 *
 * 这里不再依赖 `providers` 模块暴露的 `typeContext` 扩展，而是显式基于：
 * 1. 结构化 Cone 类型族；
 * 2. 当前 use-site 视角下的 direct-supertype 提供器；
 * 3. 仓颉自己的数值拓宽与理想字面量规则；
 * 构建 `analysis` 可稳定复用的类型相等与子类型判断。
 *
 * 这样 `analysis-api-cfir` 可以继续暴露公开类型关系协议，
 * 同时不把上游 `providers` 的实现细节拖进 `analysis` 边界。
 */
internal class CaCfirTypeRelations(
    private val directSuperTypes: (ConeCangJieType) -> List<ConeCangJieType>,
) {
    fun areTypesEqual(left: ConeCangJieType, right: ConeCangJieType): Boolean {
        return structuralEquals(left, right, mutableSetOf())
    }

    fun isSubTypeOf(subType: ConeCangJieType, superType: ConeCangJieType): Boolean {
        return isSubTypeOf(subType, superType, mutableSetOf())
    }

    private fun structuralEquals(
        left: ConeCangJieType,
        right: ConeCangJieType,
        visited: MutableSet<Pair<ConeCangJieType, ConeCangJieType>>,
    ): Boolean {
        if (left === right || left == right) return true
        if (!visited.add(left to right)) return true

        val normalizedLeft = normalizeIdeal(left, right)
        val normalizedRight = normalizeIdeal(right, left)
        if (normalizedLeft !== left || normalizedRight !== right) {
            return structuralEquals(normalizedLeft, normalizedRight, visited)
        }

        return when {
            left is ConeErrorType && right is ConeErrorType ->
                structuralEquals(left.delegatedType ?: return false, right.delegatedType ?: return false, visited)

            left is ConePrimitiveType && right is ConePrimitiveType ->
                left.kind == right.kind

            left is ConeFuncType && right is ConeFuncType ->
                left.isCFunc == right.isCFunc &&
                    left.isClosureType == right.isClosureType &&
                    left.hasVariableLenArg == right.hasVariableLenArg &&
                    left.parameterTypes.size == right.parameterTypes.size &&
                    left.parameterTypes.zip(right.parameterTypes).all { (l, r) -> structuralEquals(l, r, visited) } &&
                    structuralEquals(left.returnType, right.returnType, visited)

            left is ConeTupleType && right is ConeTupleType ->
                left.elementTypes.size == right.elementTypes.size &&
                    left.elementTypes.zip(right.elementTypes).all { (l, r) -> structuralEquals(l, r, visited) }

            left is ConeIntersectionType && right is ConeIntersectionType ->
                left.intersectedTypes.size == right.intersectedTypes.size &&
                    left.intersectedTypes.zip(right.intersectedTypes).all { (l, r) -> structuralEquals(l, r, visited) }

            left is ConeUnionType && right is ConeUnionType ->
                left.unionTypes.size == right.unionTypes.size &&
                    left.unionTypes.all { leftAlternative ->
                        right.unionTypes.any { rightAlternative -> structuralEquals(leftAlternative, rightAlternative, visited) }
                    }

            left is ConePointerType && right is ConePointerType ->
                structuralEquals(left.pointeeType, right.pointeeType, visited)

            left is ConeCStringType && right is ConeCStringType -> true

            left is ConeVArrayType && right is ConeVArrayType ->
                left.size == right.size && structuralEquals(left.elementType, right.elementType, visited)

            left is ConeQuestType && right is ConeQuestType -> true

            left is ConePlaceholderType && right is ConePlaceholderType ->
                left === right

            left is ConeTypeVariableType && right is ConeTypeVariableType ->
                left.typeConstructor == right.typeConstructor

            left is ConeTypeParameterType && right is ConeTypeParameterType ->
                left.lookupTag == right.lookupTag

            isNominalType(left) && isNominalType(right) ->
                left::class == right::class &&
                    nominalTypeId(left) == nominalTypeId(right) &&
                    sameTypeArguments(left.typeArguments, right.typeArguments, visited)

            else -> false
        }
    }

    private fun isSubTypeOf(
        subType: ConeCangJieType,
        superType: ConeCangJieType,
        visited: MutableSet<Pair<ConeCangJieType, ConeCangJieType>>,
    ): Boolean {
        if (structuralEquals(subType, superType, mutableSetOf())) return true
        if (!visited.add(subType to superType)) return false

        val normalizedSub = normalizeIdeal(subType, superType)
        val normalizedSuper = normalizeIdeal(superType, subType)
        if (normalizedSub !== subType || normalizedSuper !== superType) {
            return isSubTypeOf(normalizedSub, normalizedSuper, visited)
        }

        if (normalizedSub is ConePrimitiveType && normalizedSub.isNothing) return true
        if (normalizedSuper === ConeAnyType) return normalizedSub !is ConeErrorType

        val delegatedSubType = (normalizedSub as? ConeErrorType)?.delegatedType
        val delegatedSuperType = (normalizedSuper as? ConeErrorType)?.delegatedType

        return when {
            delegatedSubType != null ->
                isSubTypeOf(delegatedSubType, normalizedSuper, visited)

            delegatedSuperType != null ->
                isSubTypeOf(normalizedSub, delegatedSuperType, visited)

            normalizedSuper is ConeUnionType ->
                normalizedSuper.unionTypes.any { alternative -> isSubTypeOf(normalizedSub, alternative, visited) }

            normalizedSub is ConeUnionType ->
                normalizedSub.unionTypes.all { alternative -> isSubTypeOf(alternative, normalizedSuper, visited) }

            normalizedSuper is ConeIntersectionType ->
                normalizedSuper.intersectedTypes.all { conjunct -> isSubTypeOf(normalizedSub, conjunct, visited) }

            normalizedSub is ConeIntersectionType ->
                normalizedSub.intersectedTypes.any { conjunct -> isSubTypeOf(conjunct, normalizedSuper, visited) }

            normalizedSub is ConeFuncType && normalizedSuper is ConeFuncType ->
                isFunctionSubtype(normalizedSub, normalizedSuper, visited)

            normalizedSub is ConeTupleType && normalizedSuper is ConeTupleType ->
                normalizedSub.elementTypes.size == normalizedSuper.elementTypes.size &&
                    normalizedSub.elementTypes.zip(normalizedSuper.elementTypes)
                        .all { (subElement, superElement) -> isSubTypeOf(subElement, superElement, visited) }

            normalizedSub is ConePointerType && normalizedSuper is ConePointerType ->
                structuralEquals(normalizedSub.pointeeType, normalizedSuper.pointeeType, mutableSetOf())

            normalizedSub is ConeVArrayType && normalizedSuper is ConeVArrayType ->
                normalizedSub.size == normalizedSuper.size &&
                    structuralEquals(normalizedSub.elementType, normalizedSuper.elementType, mutableSetOf())

            normalizedSub is ConeCStringType && normalizedSuper is ConeCStringType -> true

            normalizedSub is ConeQuestType || normalizedSuper is ConeQuestType -> false

            normalizedSub is ConePlaceholderType || normalizedSuper is ConePlaceholderType -> false

            normalizedSub is ConeTypeVariableType || normalizedSuper is ConeTypeVariableType -> false

            normalizedSub is ConeTypeParameterType ->
                directSuperTypes(normalizedSub).any { upperBound -> isSubTypeOf(upperBound, normalizedSuper, visited) }

            normalizedSub is ConePrimitiveType && normalizedSuper is ConePrimitiveType ->
                isPrimitiveSubtype(normalizedSub, normalizedSuper)

            isNominalType(normalizedSub) && isNominalType(normalizedSuper) ->
                isNominalSubtype(normalizedSub, normalizedSuper, visited)

            else -> false
        }
    }

    private fun isFunctionSubtype(
        subType: ConeFuncType,
        superType: ConeFuncType,
        visited: MutableSet<Pair<ConeCangJieType, ConeCangJieType>>,
    ): Boolean {
        if (subType.isCFunc != superType.isCFunc) return false
        if (subType.isClosureType != superType.isClosureType) return false
        if (subType.hasVariableLenArg != superType.hasVariableLenArg) return false
        if (subType.parameterTypes.size != superType.parameterTypes.size) return false

        val parametersCompatible = subType.parameterTypes.zip(superType.parameterTypes)
            .all { (subParameter, superParameter) ->
                isSubTypeOf(superParameter, subParameter, visited)
            }
        if (!parametersCompatible) return false

        return isSubTypeOf(subType.returnType, superType.returnType, visited)
    }

    private fun isPrimitiveSubtype(
        subType: ConePrimitiveType,
        superType: ConePrimitiveType,
    ): Boolean {
        if (subType.kind == superType.kind) return true
        if (subType.kind.isIdeal || superType.kind.isIdeal) {
            val normalizedSub = normalizeIdeal(subType, superType)
            val normalizedSuper = normalizeIdeal(superType, subType)
            if (normalizedSub is ConePrimitiveType && normalizedSuper is ConePrimitiveType) {
                return isPrimitiveSubtype(normalizedSub, normalizedSuper)
            }
        }

        return ConeNumericWidening.isWideningAllowed(subType.kind, superType.kind)
    }

    private fun isNominalSubtype(
        subType: ConeCangJieType,
        superType: ConeCangJieType,
        visited: MutableSet<Pair<ConeCangJieType, ConeCangJieType>>,
    ): Boolean {
        if (subType::class == superType::class &&
            nominalTypeId(subType) == nominalTypeId(superType) &&
            sameTypeArguments(subType.typeArguments, superType.typeArguments, mutableSetOf())
        ) {
            return true
        }

        return directSuperTypes(subType).any { directSuperType ->
            isSubTypeOf(directSuperType, superType, visited)
        }
    }

    private fun sameTypeArguments(
        left: List<ConeTypeProjection>,
        right: List<ConeTypeProjection>,
        visited: MutableSet<Pair<ConeCangJieType, ConeCangJieType>>,
    ): Boolean {
        if (left.size != right.size) return false
        return left.zip(right).all { (leftProjection, rightProjection) ->
            structuralEquals(leftProjection.type, rightProjection.type, visited)
        }
    }

    private fun normalizeIdeal(type: ConeCangJieType, targetType: ConeCangJieType): ConeCangJieType {
        return IdealTypeResolver.resolveIfIdeal(type, targetType)
    }

    private fun isNominalType(type: ConeCangJieType): Boolean {
        return type is ConeClassLikeType ||
            type is ConeStructType ||
            type is ConeEnumType ||
            type is ConeTypeAliasType ||
            type is ConePrimitiveType
    }

    private fun nominalTypeId(type: ConeCangJieType): Any {
        return when (type) {
            is ConeClassLikeType -> type.classId
            is ConeStructType -> type.classId
            is ConeEnumType -> type.classId to type.isRefEnum
            is ConeTypeAliasType -> type.classId
            is ConePrimitiveType -> type.kind
            else -> error("只允许对名义类型读取语义标识：${type::class.simpleName}")
        }
    }
}

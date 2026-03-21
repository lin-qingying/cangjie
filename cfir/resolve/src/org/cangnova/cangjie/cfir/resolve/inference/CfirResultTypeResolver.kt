package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.types.ConeArrayType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeFlexibleType
import org.cangnova.cangjie.cfir.types.ConeFuncType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeSubtypeChecker
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind

internal class CfirResultTypeResolver(
    private val storage: CfirConstraintStorage,
    private val subtypeChecker: ConeSubtypeChecker?,
) {
    fun hasConflictingBounds(variable: CfirTypeVariable): Boolean {
        val checker = subtypeChecker ?: return false
        val substitutor = storage.buildCurrentSubstitutor()
        val unresolvedNames = storage.typeVariables.filterNot { it.isFixed }.mapTo(mutableSetOf()) { it.name }
        val selfName = variable.name

        val lowers = variable.lowerBounds
            .map { substitutor.substituteOrSelf(it) }
            .filterNot { containsUnresolvedTypeVariable(it, unresolvedNames, selfName) }
        val uppers = variable.upperBounds
            .map { substitutor.substituteOrSelf(it) }
            .filterNot { containsUnresolvedTypeVariable(it, unresolvedNames, selfName) }
        if (lowers.isEmpty() || uppers.isEmpty()) return false
        return lowers.any { lower -> uppers.any { upper -> !checker.isSubtypeOf(lower, upper) } }
    }

    fun computeResultType(variable: CfirTypeVariable): ConeCangJieType? {
        val substitutor = storage.buildCurrentSubstitutor()
        val unresolvedNames = storage.typeVariables.filterNot { it.isFixed }.mapTo(mutableSetOf()) { it.name }
        val selfName = variable.name

        val lowers = variable.lowerBounds
            .map { materializeIdealType(substitutor.substituteOrSelf(it)) }
            .filterNot { containsUnresolvedTypeVariable(it, unresolvedNames, selfName) }
            .distinct()
        val uppers = variable.upperBounds
            .map { materializeIdealType(substitutor.substituteOrSelf(it)) }
            .filterNot { containsUnresolvedTypeVariable(it, unresolvedNames, selfName) }
            .distinct()

        val lowerCandidate = combineLowerBounds(lowers)
        val upperCandidate = combineUpperBounds(uppers)

        if (lowerCandidate != null && isCompatibleCandidate(lowerCandidate, lowers, uppers)) return lowerCandidate
        if (upperCandidate != null && isCompatibleCandidate(upperCandidate, lowers, uppers)) return upperCandidate

        return when {
            lowerCandidate != null -> lowerCandidate
            upperCandidate != null -> upperCandidate
            else -> null
        }
    }

    private fun combineLowerBounds(lowers: List<ConeCangJieType>): ConeCangJieType? {
        if (lowers.isEmpty()) return null
        if (lowers.size == 1) return lowers.single()

        val checker = subtypeChecker
        if (checker != null) {
            val allCommonSupertypes = lowers.filter { candidate ->
                lowers.all { lower -> checker.isSubtypeOf(lower, candidate) }
            }
            if (allCommonSupertypes.isNotEmpty()) {
                return allCommonSupertypes.firstOrNull { candidate ->
                    allCommonSupertypes.none { other -> other != candidate && checker.isSubtypeOf(other, candidate) }
                } ?: allCommonSupertypes.first()
            }
        }

        // Cangjie inference is greedy here: when no common lower candidate can be
        // formed, keep the first concrete lower bound instead of widening to union.
        return lowers.firstOrNull()
    }

    private fun combineUpperBounds(uppers: List<ConeCangJieType>): ConeCangJieType? {
        if (uppers.isEmpty()) return null
        if (uppers.size == 1) return uppers.single()

        val checker = subtypeChecker
        if (checker != null) {
            val allCommonSubtypes = uppers.filter { candidate ->
                uppers.all { upper -> checker.isSubtypeOf(candidate, upper) }
            }
            if (allCommonSubtypes.isNotEmpty()) {
                return allCommonSubtypes.firstOrNull { candidate ->
                    allCommonSubtypes.none { other -> other != candidate && checker.isSubtypeOf(candidate, other) }
                } ?: allCommonSubtypes.first()
            }
        }

        // Do not force intersection fallback for uppers; leave it unresolved and
        // let lower-bound information drive fixing when possible.
        return null
    }

    private fun isCompatibleCandidate(
        candidate: ConeCangJieType,
        lowers: List<ConeCangJieType>,
        uppers: List<ConeCangJieType>,
    ): Boolean {
        val checker = subtypeChecker ?: return true
        if (lowers.any { !checker.isSubtypeOf(it, candidate) }) return false
        if (uppers.any { !checker.isSubtypeOf(candidate, it) }) return false
        return true
    }

    private fun materializeIdealType(type: ConeCangJieType): ConeCangJieType {
        if (type !is ConePrimitiveType) return type
        return when (type.kind) {
            PrimitiveTypeKind.IDEAL_INT -> ConePrimitiveType.INT64
            PrimitiveTypeKind.IDEAL_FLOAT -> ConePrimitiveType.FLOAT64
            else -> type
        }
    }

    private fun containsUnresolvedTypeVariable(
        type: ConeCangJieType,
        unresolvedNames: Set<String>,
        selfName: String,
    ): Boolean {
        return when (type) {
            is ConeTypeParameterType -> type.name in unresolvedNames && type.name != selfName
            is ConeClassLikeType -> type.typeArguments.any { containsUnresolvedTypeVariable(it, unresolvedNames, selfName) }
            is ConeStructType -> type.typeArguments.any { containsUnresolvedTypeVariable(it, unresolvedNames, selfName) }
            is ConeEnumType -> type.typeArguments.any { containsUnresolvedTypeVariable(it, unresolvedNames, selfName) }
            is ConeFuncType -> type.parameterTypes.any { containsUnresolvedTypeVariable(it, unresolvedNames, selfName) } ||
                containsUnresolvedTypeVariable(type.returnType, unresolvedNames, selfName)
            is ConeTupleType -> type.elementTypes.any { containsUnresolvedTypeVariable(it, unresolvedNames, selfName) }
            is ConeArrayType -> containsUnresolvedTypeVariable(type.elementType, unresolvedNames, selfName)
            is ConeVArrayType -> containsUnresolvedTypeVariable(type.elementType, unresolvedNames, selfName)
            is ConeIntersectionType -> type.intersectedTypes.any { containsUnresolvedTypeVariable(it, unresolvedNames, selfName) }
            is ConeUnionType -> type.unionTypes.any { containsUnresolvedTypeVariable(it, unresolvedNames, selfName) }
            is ConeFlexibleType -> containsUnresolvedTypeVariable(type.lowerBound, unresolvedNames, selfName) ||
                containsUnresolvedTypeVariable(type.upperBound, unresolvedNames, selfName)
            else -> false
        }
    }
}

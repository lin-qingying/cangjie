package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.types.ConeArrayType
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeFlexibleType
import org.cangnova.cangjie.cfir.types.ConeFuncType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.ConeVArrayType

internal data class CfirVariableFixationReadiness(
    val hasProperConstraint: Boolean,
    val hasDependencyOnOuterVariable: Boolean,
    val concreteConstraintCount: Int,
) : CfirFixationReadiness {
    override val allowsFixation: Boolean get() = hasProperConstraint && !hasDependencyOnOuterVariable
}

internal class CfirVariableFixationFinder {
    fun chooseNextVariable(unresolved: Set<CfirTypeVariable>): CfirTypeVariable? {
        if (unresolved.isEmpty()) return null
        return unresolved.maxByOrNull { variable ->
            val readiness = computeReadiness(variable, unresolved - variable)
            readinessScore(readiness)
        }
    }

    fun buildFixationLog(unresolved: Set<CfirTypeVariable>, chosen: CfirTypeVariable): InferenceLogger.FixationLogRecord {
        return InferenceLogger.FixationLogRecord(
            map = unresolved.associateWith { variable ->
                val readiness = computeReadiness(variable, unresolved - variable)
                InferenceLogger.FixationLogVariableInfo(
                    readiness = readiness,
                    constraints = buildList {
                        variable.upperBounds.forEach { add(CfirVariableConstraint(CfirConstraintKind.UPPER, it)) }
                        variable.lowerBounds.forEach { add(CfirVariableConstraint(CfirConstraintKind.LOWER, it)) }
                    },
                )
            },
            chosen = chosen,
        )
    }

    private fun computeReadiness(variable: CfirTypeVariable, candidates: Set<CfirTypeVariable>): CfirVariableFixationReadiness {
        val hasDependency = dependsOnUnfixedVariables(variable, candidates)
        val concreteSignal = concreteBoundSignal(variable, candidates)
        val hasProperConstraint = concreteSignal > 0
        return CfirVariableFixationReadiness(
            hasProperConstraint = hasProperConstraint,
            hasDependencyOnOuterVariable = hasDependency,
            concreteConstraintCount = concreteSignal,
        )
    }

    private fun readinessScore(readiness: CfirVariableFixationReadiness): Int {
        var score = 0
        if (readiness.allowsFixation) score += 1 shl 16
        if (readiness.hasProperConstraint) score += 1 shl 8
        score += readiness.concreteConstraintCount
        return score
    }

    private fun dependsOnUnfixedVariables(variable: CfirTypeVariable, candidates: Set<CfirTypeVariable>): Boolean {
        val candidateNames = candidates.mapTo(mutableSetOf()) { it.name }
        return variable.upperBounds.any { bound ->
            containsUnresolvedTypeVariable(bound, candidateNames, variable.name)
        } || variable.lowerBounds.any { bound ->
            containsUnresolvedTypeVariable(bound, candidateNames, variable.name)
        }
    }

    private fun concreteBoundSignal(variable: CfirTypeVariable, candidates: Set<CfirTypeVariable>): Int {
        if (candidates.isEmpty()) return variable.upperBounds.size + variable.lowerBounds.size
        val candidateNames = candidates.mapTo(mutableSetOf()) { it.name }
        val concreteUpper = variable.upperBounds.count { bound ->
            !containsUnresolvedTypeVariable(bound, candidateNames, variable.name)
        }
        val concreteLower = variable.lowerBounds.count { bound ->
            !containsUnresolvedTypeVariable(bound, candidateNames, variable.name)
        }
        return concreteUpper + concreteLower
    }

    private fun containsUnresolvedTypeVariable(
        type: ConeCangjieType,
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

package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.constraints.CfirConstraintResult
import org.cangnova.cangjie.cfir.constraints.CfirTypeSubstitutor
import org.cangnova.cangjie.cfir.constraints.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind

class CfirConstraintResultBuilder {
    fun build(store: CfirConstraintStore): CfirConstraintResult {
        val fixedVariables = store.typeVariables.filter { it.fixedType != null }
        val unresolvedVariables = store.typeVariables.filter { it.fixedType == null }
        val substitution = fixedVariables.associate { variable ->
            variable.lookupTag.name to finalizeIdealType(checkNotNull(variable.fixedType))
        }

        val substitutor: CfirTypeSubstitutor =
            if (substitution.isEmpty()) CfirTypeSubstitutor.Empty else CfirTypeSubstitutorByMap(substitution)

        val lowerBoundsByVariable = store.typeVariables.associate { variable ->
            variable.lookupTag.name to variable.lowerBounds.map { it.toString() }
        }

        val upperBoundsByVariable = store.typeVariables.associate { variable ->
            variable.lookupTag.name to variable.upperBounds.map { it.toString() }
        }

        val issueCountByKind = store.issues
            .groupingBy { it::class.simpleName ?: "UnknownIssue" }
            .eachCount()

        return CfirConstraintResult(
            substitutor = substitutor,
            fixedVariables = fixedVariables,
            unresolvedVariables = unresolvedVariables,
            issues = store.issues,
            constraintCount = store.constraints.size,
            typeVariableCount = store.typeVariables.size,
            lowerBoundsByVariable = lowerBoundsByVariable,
            upperBoundsByVariable = upperBoundsByVariable,
            issueCountByKind = issueCountByKind,
        )
    }

    /** 将残留的 ideal 类型终结化为具体类型 */
    private fun finalizeIdealType(type: ConeCangJieType): ConeCangJieType {
        if (type is ConePrimitiveType) {
            return when (type.kind) {
                PrimitiveTypeKind.IDEAL_INT -> ConePrimitiveType.INT64
                PrimitiveTypeKind.IDEAL_FLOAT -> ConePrimitiveType.FLOAT64
                else -> type
            }
        }
        return type
    }
}

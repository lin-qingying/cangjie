package org.cangnova.cangjie.cfir.constraints

data class CfirConstraintResult(
    val substitutor: CfirTypeSubstitutor,
    val fixedVariables: List<CfirTypeVariable>,
    val unresolvedVariables: List<CfirTypeVariable>,
    val issues: List<CfirConstraintIssue>,
    val constraintCount: Int,
    val typeVariableCount: Int,
    val lowerBoundsByVariable: Map<String, List<String>>,
    val upperBoundsByVariable: Map<String, List<String>>,
    val issueCountByKind: Map<String, Int>,
) {
    val hasErrors: Boolean get() = issues.isNotEmpty()
    val isFullyResolved: Boolean get() = unresolvedVariables.isEmpty() && !hasErrors
    val fixedVariableCount: Int get() = fixedVariables.size
    val unresolvedVariableCount: Int get() = unresolvedVariables.size

    fun lowerBoundsOf(variableName: String): List<String> = lowerBoundsByVariable[variableName].orEmpty()
    fun upperBoundsOf(variableName: String): List<String> = upperBoundsByVariable[variableName].orEmpty()
    fun issueCountOf(kind: String): Int = issueCountByKind[kind] ?: 0
}

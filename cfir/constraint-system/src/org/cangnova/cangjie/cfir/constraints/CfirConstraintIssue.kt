package org.cangnova.cangjie.cfir.constraints

sealed interface CfirConstraintIssue {
    val position: CfirConstraintPosition
    val message: String

    data class IncompatibleTypes(
        override val message: String,
        override val position: CfirConstraintPosition,
    ) : CfirConstraintIssue

    data class ConflictingBounds(
        val variable: CfirTypeVariable,
        override val position: CfirConstraintPosition,
        override val message: String,
    ) : CfirConstraintIssue

    data class UnresolvedVariable(
        val variable: CfirTypeVariable,
        override val position: CfirConstraintPosition,
        override val message: String,
    ) : CfirConstraintIssue
}

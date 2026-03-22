package org.cangnova.cangjie.cfir.constraints

sealed interface CfirConstraintPosition {
    data class ArgumentPosition(val index: Int) : CfirConstraintPosition
    data class ExplicitTypeArgument(val index: Int) : CfirConstraintPosition
    data object ExpectedType : CfirConstraintPosition
    data object ReturnType : CfirConstraintPosition
    data object UpperBound : CfirConstraintPosition
    data object Unknown : CfirConstraintPosition
}

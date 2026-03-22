package org.cangnova.cangjie.cfir.constraints

import org.cangnova.cangjie.cfir.types.ConeCangJieType

sealed interface CfirConstraint {
    val position: CfirConstraintPosition

    data class Equality(
        val left: ConeCangJieType,
        val right: ConeCangJieType,
        override val position: CfirConstraintPosition,
    ) : CfirConstraint

    data class Subtype(
        val lower: ConeCangJieType,
        val upper: ConeCangJieType,
        override val position: CfirConstraintPosition,
    ) : CfirConstraint

    data class Compatibility(
        val source: ConeCangJieType,
        val target: ConeCangJieType,
        override val position: CfirConstraintPosition,
    ) : CfirConstraint

    data class ExpectedType(
        val actual: ConeCangJieType,
        val expected: ConeCangJieType,
        override val position: CfirConstraintPosition = CfirConstraintPosition.ExpectedType,
    ) : CfirConstraint

    data class UpperBound(
        val variable: CfirTypeVariable,
        val bound: ConeCangJieType,
        override val position: CfirConstraintPosition = CfirConstraintPosition.UpperBound,
    ) : CfirConstraint
}

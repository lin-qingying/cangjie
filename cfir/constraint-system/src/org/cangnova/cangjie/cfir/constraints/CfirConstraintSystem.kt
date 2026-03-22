package org.cangnova.cangjie.cfir.constraints

import org.cangnova.cangjie.cfir.types.ConeCangJieType

interface CfirConstraintSystem {
    val errors: List<CfirConstraintIssue>
    val hasErrors: Boolean get() = errors.isNotEmpty()

    fun registerTypeVariable(typeVariable: CfirTypeVariable)
    fun addConstraint(constraint: CfirConstraint)
    fun addSubtypeConstraint(lower: ConeCangJieType, upper: ConeCangJieType, position: CfirConstraintPosition)
    fun addEqualityConstraint(left: ConeCangJieType, right: ConeCangJieType, position: CfirConstraintPosition)
    fun addSubtypeConstraintIfCompatible(lower: ConeCangJieType, upper: ConeCangJieType, position: CfirConstraintPosition): Boolean
    fun fixAllVariables()
    fun buildResult(): CfirConstraintResult
    fun buildResultingSubstitutor(): CfirTypeSubstitutor
}

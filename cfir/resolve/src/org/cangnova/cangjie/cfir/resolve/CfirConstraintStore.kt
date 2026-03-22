package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.constraints.CfirConstraint
import org.cangnova.cangjie.cfir.constraints.CfirConstraintIssue
import org.cangnova.cangjie.cfir.constraints.CfirTypeVariable

class CfirConstraintStore {
    private val _constraints = mutableListOf<CfirConstraint>()
    private val _typeVariables = mutableListOf<CfirTypeVariable>()
    private val _variableIndex = mutableMapOf<String, CfirTypeVariable>()
    private val _issues = mutableListOf<CfirConstraintIssue>()

    val constraints: List<CfirConstraint> get() = _constraints
    val typeVariables: List<CfirTypeVariable> get() = _typeVariables
    val issues: List<CfirConstraintIssue> get() = _issues

    val hasIssues: Boolean get() = _issues.isNotEmpty()

    fun findTypeVariable(name: String): CfirTypeVariable? {
        return _variableIndex[name]
    }

    fun clearIssues() {
        _issues.clear()
    }

    fun addConstraint(constraint: CfirConstraint) {
        _constraints += constraint
    }

    fun registerTypeVariable(typeVariable: CfirTypeVariable) {
        _typeVariables += typeVariable
        _variableIndex[typeVariable.lookupTag.name] = typeVariable
    }

    fun reportIssue(issue: CfirConstraintIssue) {
        _issues += issue
    }
}

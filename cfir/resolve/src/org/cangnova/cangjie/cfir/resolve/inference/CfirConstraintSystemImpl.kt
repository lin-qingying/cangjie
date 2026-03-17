package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.resolve.calls.CfirTypeSubstitutor
import org.cangnova.cangjie.cfir.resolve.calls.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeUnionType

/**
 * Simplified constraint system for CFIR call inference.
 */
class CfirConstraintSystemImpl(
    private val inferenceLogger: FirInferenceLogger? = null,
) : CfirConstraintSystem {

    private val _typeVariables: MutableList<CfirTypeVariable> = mutableListOf()
    private val _constraints: MutableList<CfirConstraint> = mutableListOf()
    private val _errors: MutableList<String> = mutableListOf()

    private val variableByName: MutableMap<String, CfirTypeVariable> = mutableMapOf()

    private var nextFreshId: Int = 0

    override val typeVariables: List<CfirTypeVariable> get() = _typeVariables
    override val constraints: List<CfirConstraint> get() = _constraints
    override val hasErrors: Boolean get() = _errors.isNotEmpty()
    override val errors: List<String> get() = _errors

    override fun registerTypeVariable(variable: CfirTypeVariable) {
        _typeVariables.add(variable)
        variableByName[variable.name] = variable
        inferenceLogger?.logNewVariable(variable, this)
    }

    override fun addSubtypeConstraint(
        subType: ConeCangjieType,
        superType: ConeCangjieType,
        position: CfirConstraintPosition,
    ) {
        val constraint = CfirSubtypeConstraint(subType, superType, position)
        _constraints.add(constraint)

        val initialConstraint = CfirInitialConstraint(
            a = subType,
            b = superType,
            constraintKind = CfirConstraintKind.UPPER,
            position = position,
        )
        inferenceLogger?.logInitial(initialConstraint, this)

        inferenceLogger.withOrigin(initialConstraint) {
            processSubtypeConstraint(subType, superType)
        }
    }

    override fun addEqualityConstraint(
        left: ConeCangjieType,
        right: ConeCangjieType,
        position: CfirConstraintPosition,
    ) {
        val constraint = CfirEqualityConstraint(left, right, position)
        _constraints.add(constraint)

        val initialConstraint = CfirInitialConstraint(
            a = left,
            b = right,
            constraintKind = CfirConstraintKind.EQUALITY,
            position = position,
        )
        inferenceLogger?.logInitial(initialConstraint, this)

        inferenceLogger.withOrigin(initialConstraint) {
            // Equality is modeled as two subtype relations.
            processSubtypeConstraint(left, right)
            processSubtypeConstraint(right, left)
        }
    }

    private fun processSubtypeConstraint(subType: ConeCangjieType, superType: ConeCangjieType) {
        val subVariable = findTypeVariable(subType)
        val superVariable = findTypeVariable(superType)

        when {
            subVariable != null && superVariable != null -> {
                // 双变量约束：sub <: super → sub 的上界包含 super 类型，super 的下界包含 sub 类型
                subVariable.upperBounds.add(superType)
                superVariable.lowerBounds.add(subType)
            }

            subVariable != null -> {
                subVariable.upperBounds.add(superType)
                inferenceLogger?.log(
                    subVariable,
                    CfirVariableConstraint(CfirConstraintKind.UPPER, superType),
                    this,
                )
            }

            superVariable != null -> {
                superVariable.lowerBounds.add(subType)
                inferenceLogger?.log(
                    superVariable,
                    CfirVariableConstraint(CfirConstraintKind.LOWER, subType),
                    this,
                )
            }
        }
    }

    private fun findTypeVariable(type: ConeCangjieType): CfirTypeVariable? {
        if (type !is ConeTypeParameterType) return null
        return variableByName[type.lookupTag.name]
    }

    override fun fixVariable(variable: CfirTypeVariable) {
        if (variable.isFixed) return

        logReadiness(variable)

        val fixedType = computeFixedType(variable)
        if (fixedType != null) {
            variable.fixedType = fixedType
            inferenceLogger?.logFixVariable(variable, fixedType, this)
        } else {
            val message = "Cannot infer type for ${variable.name}: not enough constraints"
            _errors.add(message)
            inferenceLogger?.logError(CfirConstraintSystemError(message), this)
        }
    }

    private fun logReadiness(variable: CfirTypeVariable) {
        val variableConstraints = buildList {
            variable.upperBounds.forEach { add(CfirVariableConstraint(CfirConstraintKind.UPPER, it)) }
            variable.lowerBounds.forEach { add(CfirVariableConstraint(CfirConstraintKind.LOWER, it)) }
        }

        val fixationLog = InferenceLogger.FixationLogRecord(
            map = mapOf(
                variable to InferenceLogger.FixationLogVariableInfo(
                    readiness = CfirSimpleFixationReadiness(variableConstraints.isNotEmpty()),
                    constraints = variableConstraints,
                )
            ),
            chosen = variable,
        )
        inferenceLogger?.logReadiness(fixationLog, this)
    }

    private fun computeFixedType(variable: CfirTypeVariable): ConeCangjieType? {
        val lowers = variable.lowerBounds
        val uppers = variable.upperBounds

        if (lowers.isNotEmpty()) {
            // 多个下界：取联合类型（LUB 近似）
            val distinct = lowers.distinct()
            return if (distinct.size == 1) distinct.single() else ConeUnionType(distinct.toSet())
        }

        if (uppers.isNotEmpty()) {
            // 多个上界：取交叉类型（GLB 近似）
            val distinct = uppers.distinct()
            return if (distinct.size == 1) distinct.single() else ConeIntersectionType(distinct)
        }

        return null
    }

    override fun fixAllVariables() {
        for (variable in _typeVariables) {
            if (!variable.isFixed) {
                fixVariable(variable)
            }
        }
    }

    override fun buildResultingSubstitutor(): CfirTypeSubstitutor {
        val substitution = mutableMapOf<String, ConeCangjieType>()
        for (variable in _typeVariables) {
            val fixed = variable.fixedType
            if (fixed != null) {
                substitution[variable.name] = fixed
            }
        }
        return if (substitution.isEmpty()) {
            CfirTypeSubstitutor.Empty
        } else {
            CfirTypeSubstitutorByMap(substitution)
        }
    }

    fun nextFreshTypeId(): Int = nextFreshId++

    override fun toString(): String = buildString {
        append("CfirConstraintSystem(")
        append("variables=${_typeVariables.size}, ")
        append("constraints=${_constraints.size}, ")
        append("errors=${_errors.size}")
        append(")")
    }
}


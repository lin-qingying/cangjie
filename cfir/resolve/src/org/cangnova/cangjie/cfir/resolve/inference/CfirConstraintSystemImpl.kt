package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.resolve.calls.CfirTypeSubstitutor
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeSubtypeChecker
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType

class CfirConstraintSystemImpl(
    private val subtypeChecker: ConeSubtypeChecker? = null,
    private val inferenceLogger: FirInferenceLogger? = null,
) : CfirConstraintSystem {
    private val storage = CfirConstraintStorage()
    private val incorporator = CfirConstraintIncorporator(storage)
    private val fixationFinder = CfirVariableFixationFinder()
    private val resultTypeResolver = CfirResultTypeResolver(storage, subtypeChecker)

    override val typeVariables: List<CfirTypeVariable> get() = storage.typeVariables
    override val constraints: List<CfirConstraint> get() = storage.constraints
    override val hasErrors: Boolean get() = storage.hasErrors
    override val errors: List<String> get() = storage.errors

    override fun registerTypeVariable(variable: CfirTypeVariable) {
        storage.registerTypeVariable(variable)
        inferenceLogger?.logNewVariable(variable, this)
    }

    override fun addSubtypeConstraint(
        subType: ConeCangjieType,
        superType: ConeCangjieType,
        position: CfirConstraintPosition,
    ) {
        storage.addConstraint(CfirSubtypeConstraint(subType, superType, position))

        val initialConstraint = CfirInitialConstraint(
            a = subType,
            b = superType,
            constraintKind = CfirConstraintKind.UPPER,
            position = position,
        )
        inferenceLogger?.logInitial(initialConstraint, this)
        inferenceLogger.withOrigin(initialConstraint) {
            incorporator.processSubtypeConstraint(
                subType = subType,
                superType = superType,
                onUpperBound = ::onUpperBoundAdded,
                onLowerBound = ::onLowerBoundAdded,
            )
        }
    }

    override fun addEqualityConstraint(
        left: ConeCangjieType,
        right: ConeCangjieType,
        position: CfirConstraintPosition,
    ) {
        storage.addConstraint(CfirEqualityConstraint(left, right, position))

        val initialConstraint = CfirInitialConstraint(
            a = left,
            b = right,
            constraintKind = CfirConstraintKind.EQUALITY,
            position = position,
        )
        inferenceLogger?.logInitial(initialConstraint, this)
        inferenceLogger.withOrigin(initialConstraint) {
            incorporator.processSubtypeConstraint(
                subType = left,
                superType = right,
                onUpperBound = ::onUpperBoundAdded,
                onLowerBound = ::onLowerBoundAdded,
            )
            incorporator.processSubtypeConstraint(
                subType = right,
                superType = left,
                onUpperBound = ::onUpperBoundAdded,
                onLowerBound = ::onLowerBoundAdded,
            )
        }
    }

    private fun onUpperBoundAdded(variable: CfirTypeVariable, type: ConeCangjieType) {
        inferenceLogger?.log(variable, CfirVariableConstraint(CfirConstraintKind.UPPER, type), this)
    }

    private fun onLowerBoundAdded(variable: CfirTypeVariable, type: ConeCangjieType) {
        inferenceLogger?.log(variable, CfirVariableConstraint(CfirConstraintKind.LOWER, type), this)
    }

    override fun fixVariable(variable: CfirTypeVariable) {
        if (variable.isFixed) return

        val hasConflictingConstraints = resultTypeResolver.hasConflictingBounds(variable)
        val fixedType = resultTypeResolver.computeResultType(variable)
        if (fixedType != null) {
            if (hasConflictingConstraints) {
                val message = "Cannot infer type for ${variable.name}: conflicting constraints"
                if (storage.reportError(message)) {
                    inferenceLogger?.logError(CfirConstraintSystemError(message), this)
                }
            }
            variable.fixedType = fixedType
            storage.markFixed(variable, fixedType)
            incorporateFixedVariable(variable, fixedType)
            storage.removeConstraintsContainingTypeVariable(variable.name)
            inferenceLogger?.logFixVariable(variable, fixedType, this)
            return
        }

        val message = if (hasConflictingConstraints) {
            "Cannot infer type for ${variable.name}: conflicting constraints"
        } else {
            "Cannot infer type for ${variable.name}: not enough constraints"
        }
        if (storage.reportError(message)) {
            inferenceLogger?.logError(CfirConstraintSystemError(message), this)
        }
    }

    override fun fixAllVariables() {
        storage.enterCompletionMode()
        val unresolved = storage.typeVariables.toMutableSet()
        var stagnationRounds = 0
        while (unresolved.isNotEmpty()) {
            val ready = fixationFinder.chooseNextVariable(unresolved) ?: unresolved.first()
            inferenceLogger?.logReadiness(fixationFinder.buildFixationLog(unresolved, ready), this)

            val wasFixed = ready.isFixed
            fixVariable(ready)
            if (ready.isFixed || wasFixed) {
                unresolved.remove(ready)
                stagnationRounds = 0
                continue
            }

            stagnationRounds++
            if (stagnationRounds >= unresolved.size) {
                unresolved.remove(ready)
                stagnationRounds = 0
            }
        }
        storage.freeze()
    }

    override fun buildResultingSubstitutor(): CfirTypeSubstitutor = storage.buildCurrentSubstitutor()

    fun nextFreshTypeId(): Int = storage.nextFreshTypeId()

    private fun incorporateFixedVariable(variable: CfirTypeVariable, fixedType: ConeCangjieType) {
        val variableType = ConeTypeParameterType(variable.lookupTag)
        val position = CfirConstraintPosition.FixVariable(variable.name)

        val initialConstraint = CfirInitialConstraint(
            a = variableType,
            b = fixedType,
            constraintKind = CfirConstraintKind.EQUALITY,
            position = position,
        )
        storage.addConstraint(CfirEqualityConstraint(variableType, fixedType, position))
        inferenceLogger?.logInitial(initialConstraint, this)

        inferenceLogger.withOrigin(initialConstraint) {
            incorporator.processSubtypeConstraint(
                subType = variableType,
                superType = fixedType,
                onUpperBound = ::onUpperBoundAdded,
                onLowerBound = ::onLowerBoundAdded,
            )
            incorporator.processSubtypeConstraint(
                subType = fixedType,
                superType = variableType,
                onUpperBound = ::onUpperBoundAdded,
                onLowerBound = ::onLowerBoundAdded,
            )
        }
    }

    override fun toString(): String = buildString {
        append("CfirConstraintSystem(")
        append("variables=${storage.typeVariables.size}, ")
        append("constraints=${storage.constraints.size}, ")
        append("errors=${storage.errors.size}")
        append(")")
    }
}

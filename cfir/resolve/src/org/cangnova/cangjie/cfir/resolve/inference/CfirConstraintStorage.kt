package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.resolve.calls.CfirTypeSubstitutor
import org.cangnova.cangjie.cfir.resolve.calls.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.semantics.CfirConstraintSystemError
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType

internal class CfirConstraintStorage {
    private enum class State {
        BUILDING,
        TRANSACTION,
        FREEZED,
        COMPLETION,
    }

    private data class TransactionSnapshot(
        val constraintsCount: Int,
        val errorsCount: Int,
        val nextFreshId: Int,
        val fixedTypes: Map<String, ConeCangJieType>,
        val variableBounds: Map<String, Pair<List<ConeCangJieType>, List<ConeCangJieType>>>,
        val dependencies: Map<String, Set<String>>,
    )

    private val _typeVariables: MutableList<CfirTypeVariable> = mutableListOf()
    private val _constraints: MutableList<CfirConstraint> = mutableListOf()
    private val _errors: MutableList<CfirConstraintSystemError> = mutableListOf()
    private val _fixedTypeVariables: MutableMap<String, ConeCangJieType> = linkedMapOf()
    private val variableByName: MutableMap<String, CfirTypeVariable> = mutableMapOf()
    private val typeVariableDependencies: MutableMap<String, MutableSet<String>> = linkedMapOf()
    private val transactions: MutableList<TransactionSnapshot> = mutableListOf()

    private var nextFreshId: Int = 0
    private var state: State = State.BUILDING

    val typeVariables: List<CfirTypeVariable> get() = _typeVariables
    val constraints: List<CfirConstraint> get() = _constraints
    val errors: List<CfirConstraintSystemError> get() = _errors
    val hasErrors: Boolean get() = _errors.isNotEmpty()

    fun nextFreshTypeId(): Int = nextFreshId++

    fun registerTypeVariable(variable: CfirTypeVariable) {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        _typeVariables.add(variable)
        variableByName[variable.name] = variable
        typeVariableDependencies.putIfAbsent(variable.name, linkedSetOf())
    }

    fun addConstraint(constraint: CfirConstraint) {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        _constraints.add(constraint)
    }

    fun findTypeVariable(type: ConeCangJieType): CfirTypeVariable? {
        if (type !is ConeTypeParameterType) return null
        return variableByName[type.lookupTag.name]
    }

    fun addUpperBound(variable: CfirTypeVariable, type: ConeCangJieType): Boolean {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        if (variable.upperBounds.contains(type)) return false
        variable.upperBounds.add(type)
        recordTypeVariableDependencies(variable, type)
        return true
    }

    fun addLowerBound(variable: CfirTypeVariable, type: ConeCangJieType): Boolean {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        if (variable.lowerBounds.contains(type)) return false
        variable.lowerBounds.add(type)
        recordTypeVariableDependencies(variable, type)
        return true
    }

    fun removeConstraintsContainingTypeVariable(referencedVariableName: String) {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        for (variable in _typeVariables) {
            if (variable.name == referencedVariableName) continue
            variable.upperBounds.removeAll { it.containsTypeVariableName(referencedVariableName) }
            variable.lowerBounds.removeAll { it.containsTypeVariableName(referencedVariableName) }
        }
        typeVariableDependencies.remove(referencedVariableName)
    }

    fun markFixed(variable: CfirTypeVariable, fixedType: ConeCangJieType) {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        _fixedTypeVariables[variable.name] = fixedType
    }

    fun getVariablesDependingOn(variableName: String): Set<CfirTypeVariable> {
        val dependents = typeVariableDependencies[variableName].orEmpty()
        return dependents.mapNotNull { variableByName[it] }.toSet()
    }

    fun enterCompletionMode() {
        checkState(State.BUILDING)
        state = State.COMPLETION
    }

    fun beginTransaction() {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        transactions += TransactionSnapshot(
            constraintsCount = _constraints.size,
            errorsCount = _errors.size,
            nextFreshId = nextFreshId,
            fixedTypes = _fixedTypeVariables.toMap(),
            variableBounds = _typeVariables.associate { variable ->
                variable.name to (variable.lowerBounds.toList() to variable.upperBounds.toList())
            },
            dependencies = typeVariableDependencies.mapValues { it.value.toSet() },
        )
        state = State.TRANSACTION
    }

    fun rollbackTransaction() {
        checkState(State.TRANSACTION)
        val snapshot = transactions.removeLastOrNull() ?: return
        _constraints.subList(snapshot.constraintsCount, _constraints.size).clear()
        _errors.subList(snapshot.errorsCount, _errors.size).clear()
        nextFreshId = snapshot.nextFreshId

        _fixedTypeVariables.clear()
        _fixedTypeVariables.putAll(snapshot.fixedTypes)

        for (variable in _typeVariables) {
            val (lower, upper) = snapshot.variableBounds[variable.name] ?: (emptyList<ConeCangJieType>() to emptyList())
            variable.lowerBounds.clear()
            variable.lowerBounds.addAll(lower)
            variable.upperBounds.clear()
            variable.upperBounds.addAll(upper)
            if (variable.name !in _fixedTypeVariables) {
                variable.fixedType = null
            }
        }

        typeVariableDependencies.clear()
        for ((key, value) in snapshot.dependencies) {
            typeVariableDependencies[key] = value.toMutableSet()
        }

        state = if (transactions.isEmpty()) State.BUILDING else State.TRANSACTION
    }

    fun closeTransaction() {
        checkState(State.TRANSACTION)
        transactions.removeLastOrNull()
        state = if (transactions.isEmpty()) State.BUILDING else State.TRANSACTION
    }

    fun freeze() {
        checkState(State.BUILDING, State.COMPLETION)
        state = State.FREEZED
    }

    fun reportError(error: CfirConstraintSystemError): Boolean {
        if (error in _errors) return false
        _errors.add(error)
        return true
    }

    fun buildCurrentSubstitutor(): CfirTypeSubstitutor {
        val substitution = buildMap<String, ConeCangJieType> {
            for ((name, fixedType) in _fixedTypeVariables) {
                put(name, fixedType)
            }
            for (variable in _typeVariables) {
                val fixed = variable.fixedType ?: continue
                if (variable.name !in this) {
                    put(variable.name, fixed)
                }
            }
        }
        return if (substitution.isEmpty()) CfirTypeSubstitutor.Empty else CfirTypeSubstitutorByMap(substitution)
    }

    private fun recordTypeVariableDependencies(owner: CfirTypeVariable, bound: ConeCangJieType) {
        val referencedVariables = mutableSetOf<String>()
        bound.collectTypeVariableNames(referencedVariables)
        for (referencedVariable in referencedVariables) {
            if (referencedVariable == owner.name) continue
            typeVariableDependencies.getOrPut(referencedVariable) { linkedSetOf() }.add(owner.name)
        }
    }

    private fun checkState(vararg allowedStates: State) {
        check(state in allowedStates) {
            "State $state is not allowed here. Allowed: ${allowedStates.joinToString()}"
        }
    }
}

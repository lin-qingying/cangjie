package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionContext
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.cangnova.cangjie.resolve.calls.inference.components.TrivialConstraintTypeInferenceOracle
import org.cangnova.cangjie.resolve.calls.inference.model.Constraint
import org.cangnova.cangjie.resolve.calls.inference.model.VariableWithConstraints
import org.cangnova.cangjie.resolve.calls.model.CollectionLiteralAtomMarker
import org.cangnova.cangjie.type.model.CangJieTypeMarker
import org.cangnova.cangjie.type.model.TypeVariableMarker
import java.util.ArrayDeque
import java.util.Queue

fun Candidate.computeCompletionMode(
    components: InferenceComponents,
    resolutionMode: ResolutionMode,
    currentReturnType: ConeCangJieType?,
): ConstraintSystemCompletionMode {
    return when {
        resolutionMode.forceFullCompletion -> ConstraintSystemCompletionMode.FULL
        callInfo.isCollectionLiteralCall -> error("Should not run completion for collection literal")
        currentReturnType == null -> ConstraintSystemCompletionMode.PARTIAL
        system.getBuilder().isProperType(currentReturnType) -> ConstraintSystemCompletionMode.FULL
        else -> CalculatorForNestedCall(
            this,
            currentReturnType,
            system.getBuilder(),
            components.trivialConstraintTypeInferenceOracle,
        ).computeCompletionMode()
    }
}

private typealias CsCompleterContext = ConstraintSystemCompletionContext

private class CalculatorForNestedCall(
    private val candidate: Candidate,
    private val returnType: ConeCangJieType,
    private val context: CsCompleterContext,
    private val oracle: TrivialConstraintTypeInferenceOracle,
) {
    private enum class FixationDirection {
        TO_SUBTYPE, EQUALITY
    }

    private val fixationDirectionsForVariables: MutableMap<VariableWithConstraints, FixationDirection> = linkedMapOf()
    private val variablesWithQueuedConstraints = mutableSetOf<TypeVariableMarker>()
    private val typesToProcess: Queue<CangJieTypeMarker> = ArrayDeque()

    private val postponedAtoms by lazy {
        ConstraintSystemCompleter.getOrderedNotAnalyzedPostponedArguments(candidate)
    }

    /**
     * 对齐 Kotlin FIR 的 nested-call completion 计算：
     * 若返回类型中所有未固定变量都已经具备足够 proper 约束，则当前嵌套调用可以立即 FULL completion。
     *
     * 仓颉没有 Kotlin 的声明型变、投影和星投影；普通泛型实参按不变位置处理。
     * 函数类型是语言内置结构，参数位按逆变、返回位按协变处理。
     */
    fun computeCompletionMode(): ConstraintSystemCompletionMode = with(context) {
        typesToProcess.add(returnType)
        computeDirections()

        if (directionRequirementsForVariablesHold() && postponedAtoms.none { it is CollectionLiteralAtomMarker && !it.analyzed }) {
            return ConstraintSystemCompletionMode.FULL
        }

        return ConstraintSystemCompletionMode.PARTIAL
    }

    private fun CsCompleterContext.computeDirections() {
        while (typesToProcess.isNotEmpty()) {
            val type = typesToProcess.poll() ?: break

            if (!containsNotFixedTypeVariable(type)) continue

            val fixationDirectionsFromType = mutableSetOf<FixationDirectionForVariable>()
            collectRequiredDirectionsForVariables(type, PositionVariance.OUT, fixationDirectionsFromType)

            for (directionForVariable in fixationDirectionsFromType) {
                updateDirection(directionForVariable)
                enqueueTypesFromConstraints(directionForVariable.variable)
            }
        }
    }

    private fun CsCompleterContext.containsNotFixedTypeVariable(type: CangJieTypeMarker): Boolean =
        type.contains { it.typeConstructor() in notFixedTypeVariables }

    private fun enqueueTypesFromConstraints(variableWithConstraints: VariableWithConstraints) {
        val variable = variableWithConstraints.typeVariable
        if (variable in variablesWithQueuedConstraints) return

        for (constraint in variableWithConstraints.constraints) {
            typesToProcess.add(constraint.type)
        }

        variablesWithQueuedConstraints.add(variable)
    }

    private fun CsCompleterContext.directionRequirementsForVariablesHold(): Boolean {
        for ((variable, fixationDirection) in fixationDirectionsForVariables) {
            if (!hasProperConstraint(variable, fixationDirection)) return false
        }
        return true
    }

    private fun updateDirection(directionForVariable: FixationDirectionForVariable) {
        val (variable, newDirection) = directionForVariable
        fixationDirectionsForVariables[variable]?.let { oldDirection ->
            if (oldDirection != FixationDirection.EQUALITY && oldDirection != newDirection) {
                fixationDirectionsForVariables[variable] = FixationDirection.EQUALITY
            }
        } ?: run {
            fixationDirectionsForVariables[variable] = newDirection
        }
    }

    private data class FixationDirectionForVariable(
        val variable: VariableWithConstraints,
        val direction: FixationDirection,
    )

    private enum class PositionVariance {
        IN, OUT, INV
    }

    private fun PositionVariance.reversed(): PositionVariance = when (this) {
        PositionVariance.IN -> PositionVariance.OUT
        PositionVariance.OUT -> PositionVariance.IN
        PositionVariance.INV -> PositionVariance.INV
    }

    private fun CsCompleterContext.collectRequiredDirectionsForVariables(
        type: CangJieTypeMarker,
        outerVariance: PositionVariance,
        fixationDirectionsCollector: MutableSet<FixationDirectionForVariable>,
    ) {
        val functionType = type as? ConeFunctionType
        if (functionType != null) {
            for (parameterType in functionType.parameterTypes) {
                collectRequiredDirectionsForVariables(parameterType, outerVariance.reversed(), fixationDirectionsCollector)
            }
            collectRequiredDirectionsForVariables(functionType.returnType, outerVariance, fixationDirectionsCollector)
            return
        }

        val typeArgumentsCount = type.argumentsCount()
        val typeConstructor = type.typeConstructor()
        if (typeArgumentsCount > 0 && !type.isError() && typeArgumentsCount == typeConstructor.parametersCount()) {
            for (position in 0 until typeArgumentsCount) {
                val argument = type.getArgument(position)
                typeConstructor.getParameter(position)
                collectRequiredDirectionsForVariables(
                    argument.getType() ?: continue,
                    PositionVariance.INV,
                    fixationDirectionsCollector,
                )
            }
        } else {
            processTypeWithoutParameters(type, outerVariance, fixationDirectionsCollector)
        }
    }

    private fun CsCompleterContext.processTypeWithoutParameters(
        type: CangJieTypeMarker,
        compositeVariance: PositionVariance,
        newRequirementsCollector: MutableSet<FixationDirectionForVariable>,
    ) {
        val variableWithConstraints = notFixedTypeVariables[type.typeConstructor()] ?: return
        val direction = when (compositeVariance) {
            PositionVariance.IN -> FixationDirection.EQUALITY
            PositionVariance.OUT -> FixationDirection.TO_SUBTYPE
            PositionVariance.INV -> FixationDirection.EQUALITY
        }
        newRequirementsCollector.add(FixationDirectionForVariable(variableWithConstraints, direction))
    }

    private fun CsCompleterContext.hasProperConstraint(
        variableWithConstraints: VariableWithConstraints,
        direction: FixationDirection,
    ): Boolean {
        val constraints = variableWithConstraints.constraints
        val variable = variableWithConstraints.typeVariable

        var iltConstraintPresent = false
        var properConstraintPresent = false
        var nonNothingProperConstraintPresent = false

        for (constraint in constraints) {
            if (!constraint.hasRequiredKind(direction) || !isProperType(constraint.type)) continue

            if (constraint.type.typeConstructor().isIntegerLiteralTypeConstructor()) {
                iltConstraintPresent = true
            } else if (oracle.isSuitableResultedType(constraint.type)) {
                properConstraintPresent = true
                nonNothingProperConstraintPresent = true
            } else if (!isLowerConstraintForPartiallyAnalyzedVariable(constraint, variable)) {
                properConstraintPresent = true
            }
        }

        if (!properConstraintPresent) return false
        return !iltConstraintPresent || nonNothingProperConstraintPresent
    }

    private fun Constraint.hasRequiredKind(direction: FixationDirection): Boolean = when (direction) {
        FixationDirection.TO_SUBTYPE -> kind.isLower() || kind.isEqual()
        FixationDirection.EQUALITY -> kind.isEqual()
    }

    private fun CsCompleterContext.isLowerConstraintForPartiallyAnalyzedVariable(
        constraint: Constraint,
        variable: TypeVariableMarker,
    ): Boolean {
        val defaultType = variable.defaultType()
        return constraint.kind.isLower() && postponedAtoms.any { atom ->
            atom.expectedType?.contains { type -> defaultType == type } ?: false
        }
    }
}

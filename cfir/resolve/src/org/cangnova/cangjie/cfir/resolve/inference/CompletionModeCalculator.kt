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

/**
 * 根据候选当前约束系统和返回类型计算调用完成模式。
 */
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

/**
 * 约束系统完成上下文别名。
 */
private typealias CsCompleterContext = ConstraintSystemCompletionContext

/**
 * 嵌套调用完成模式计算器。
 */
private class CalculatorForNestedCall(
    /**
     * 当前候选。
     */
    private val candidate: Candidate,
    /**
     * 候选当前返回类型。
     */
    private val returnType: ConeCangJieType,
    /**
     * 候选约束系统完成上下文。
     */
    private val context: CsCompleterContext,
    /**
     * trivial constraint 推断 oracle。
     */
    private val oracle: TrivialConstraintTypeInferenceOracle,
) {
    /**
     * 类型变量固定方向。
     */
    private enum class FixationDirection {
        /**
         * 需要可作为 subtype 方向固定的约束。
         */
        TO_SUBTYPE,
        /**
         * 需要等式约束才能固定。
         */
        EQUALITY
    }

    /**
     * 类型变量到所需固定方向的映射。
     */
    private val fixationDirectionsForVariables: MutableMap<VariableWithConstraints, FixationDirection> = linkedMapOf()
    /**
     * 已经把约束类型加入处理队列的类型变量集合。
     */
    private val variablesWithQueuedConstraints = mutableSetOf<TypeVariableMarker>()
    /**
     * 等待分析的类型队列。
     */
    private val typesToProcess: Queue<CangJieTypeMarker> = ArrayDeque()

    /**
     * 当前候选中尚未分析的 postponed atom。
     */
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

    /**
     * 从返回类型开始传播类型变量固定方向。
     */
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

    /**
     * 判断类型中是否包含未固定类型变量。
     */
    private fun CsCompleterContext.containsNotFixedTypeVariable(type: CangJieTypeMarker): Boolean =
        type.contains { it.typeConstructor() in notFixedTypeVariables }

    /**
     * 将类型变量已有约束中的类型加入待处理队列。
     */
    private fun enqueueTypesFromConstraints(variableWithConstraints: VariableWithConstraints) {
        val variable = variableWithConstraints.typeVariable
        if (variable in variablesWithQueuedConstraints) return

        for (constraint in variableWithConstraints.constraints) {
            typesToProcess.add(constraint.type)
        }

        variablesWithQueuedConstraints.add(variable)
    }

    /**
     * 判断所有要求的固定方向是否已经具备 proper 约束。
     */
    private fun CsCompleterContext.directionRequirementsForVariablesHold(): Boolean {
        for ((variable, fixationDirection) in fixationDirectionsForVariables) {
            if (!hasProperConstraint(variable, fixationDirection)) return false
        }
        return true
    }

    /**
     * 合并单个类型变量的固定方向要求。
     */
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

    /**
     * 类型变量及其所需固定方向。
     */
    private data class FixationDirectionForVariable(
        /**
         * 带约束的类型变量。
         */
        val variable: VariableWithConstraints,
        /**
         * 该变量需要满足的固定方向。
         */
        val direction: FixationDirection,
    )

    /**
     * 类型位置方差。
     */
    private enum class PositionVariance {
        /**
         * 输入位置。
         */
        IN,
        /**
         * 输出位置。
         */
        OUT,
        /**
         * 不变位置。
         */
        INV
    }

    /**
     * 返回反向方差。
     */
    private fun PositionVariance.reversed(): PositionVariance = when (this) {
        PositionVariance.IN -> PositionVariance.OUT
        PositionVariance.OUT -> PositionVariance.IN
        PositionVariance.INV -> PositionVariance.INV
    }

    /**
     * 从类型结构中收集类型变量所需固定方向。
     */
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

    /**
     * 处理没有可展开类型参数的类型位置。
     */
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

    /**
     * 判断类型变量是否具备指定方向所需的 proper 约束。
     */
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

    /**
     * 判断约束种类是否满足固定方向要求。
     */
    private fun Constraint.hasRequiredKind(direction: FixationDirection): Boolean = when (direction) {
        FixationDirection.TO_SUBTYPE -> kind.isLower() || kind.isEqual()
        FixationDirection.EQUALITY -> kind.isEqual()
    }

    /**
     * 判断约束是否只是部分分析延迟变量的 lower 约束。
     */
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

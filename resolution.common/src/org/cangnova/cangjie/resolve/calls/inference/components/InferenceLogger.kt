package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.resolve.calls.inference.model.Constraint
import org.cangnova.cangjie.resolve.calls.inference.model.InitialConstraint
import org.cangnova.cangjie.type.model.TypeVariableMarker

open class InferenceLogger {
    data class FixationLogVariableInfo(
        val readiness: Any?,
        val constraints: List<Constraint>,
    )

    data class FixationLogRecord(
        val readinessPerVariable: Map<TypeVariableMarker, FixationLogVariableInfo>,
        val chosenVariable: TypeVariableMarker?,
    )

    open fun logInitial(initialConstraint: InitialConstraint, context: Any?) {}

    open fun logNewVariable(variable: TypeVariableMarker, context: Any?) {}

    open fun logReadiness(record: FixationLogRecord, context: Any?) {}

    open fun <T> withOrigin(origin: Any?, action: () -> T): T = action()

    open fun <T> withOrigins(
        firstOwner: Any?,
        firstOrigin: Any?,
        secondOwner: Any?,
        secondOrigin: Any?,
        action: () -> T,
    ): T = action()

    object Dummy : InferenceLogger()
}

package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.types.ConeCangjieType

/**
 * Marker for identifying a single constraint system in inference logs.
 */
interface CfirConstraintSystemMarker

enum class CfirConstraintKind {
    UPPER,
    LOWER,
    EQUALITY,
}

data class CfirInitialConstraint(
    val a: ConeCangjieType,
    val b: ConeCangjieType,
    val constraintKind: CfirConstraintKind,
    val position: CfirConstraintPosition,
)

data class CfirVariableConstraint(
    val kind: CfirConstraintKind,
    val type: ConeCangjieType,
)

data class CfirConstraintSystemError(
    val message: String,
)

interface CfirFixationReadiness {
    val allowsFixation: Boolean
}

data class CfirSimpleFixationReadiness(
    override val allowsFixation: Boolean,
) : CfirFixationReadiness

abstract class InferenceLogger {
    abstract fun logInitial(constraint: CfirInitialConstraint, system: CfirConstraintSystemMarker)

    abstract fun log(
        variable: CfirTypeVariable,
        constraint: CfirVariableConstraint,
        system: CfirConstraintSystemMarker,
    )

    abstract fun logError(error: CfirConstraintSystemError, system: CfirConstraintSystemMarker)

    abstract fun logNewVariable(variable: CfirTypeVariable, system: CfirConstraintSystemMarker)

    class FixationLogRecord(
        val map: Map<CfirTypeVariable, FixationLogVariableInfo<*>>,
        val chosen: CfirTypeVariable?,
    ) {
        var fixedTo: ConeCangjieType? = null
            set(value) {
                field = value
                map.values.forEach { it.freezeConstraintsAfterFixation() }
            }
    }

    class FixationLogVariableInfo<Readiness : Any>(
        val readiness: Readiness,
        val constraints: List<CfirVariableConstraint>,
    ) {
        val formattedConstraintsBeforeFixation = constraints.associateWith(::formatConstraintForFixation)
        var formattedConstraintsAfterFixation: List<String>? = null

        fun freezeConstraintsAfterFixation(): List<String> = formattedConstraintsAfterFixation
            ?: constraints
                .map { it to formatConstraintForFixation(it) }
                .filter { (constraint, formatted) ->
                    constraint !in formattedConstraintsBeforeFixation || formatted != formattedConstraintsBeforeFixation[constraint]
                }
                .map { it.second }
                .also { formattedConstraintsAfterFixation = it }

        private fun formatConstraintForFixation(constraint: CfirVariableConstraint): String {
            val operator = when (constraint.kind) {
                CfirConstraintKind.LOWER -> ">:"
                CfirConstraintKind.UPPER -> "<:"
                CfirConstraintKind.EQUALITY -> "="
            }
            return "$operator ${constraint.type}"
        }
    }

    abstract fun logReadiness(
        fixationLog: FixationLogRecord,
        system: CfirConstraintSystemMarker,
    )

    abstract fun <T> withOrigin(constraint: CfirInitialConstraint, block: () -> T): T

    abstract fun <T> withOrigins(
        variable1: CfirTypeVariable,
        constraint1: CfirVariableConstraint,
        variable2: CfirTypeVariable,
        constraint2: CfirVariableConstraint,
        block: () -> T,
    ): T

    abstract fun logFixVariable(
        variable: CfirTypeVariable,
        resultType: ConeCangjieType,
        system: CfirConstraintSystemMarker,
    )

    object Dummy : InferenceLogger() {
        override fun logInitial(constraint: CfirInitialConstraint, system: CfirConstraintSystemMarker): Nothing {
            error("Should never be called")
        }

        override fun log(
            variable: CfirTypeVariable,
            constraint: CfirVariableConstraint,
            system: CfirConstraintSystemMarker,
        ): Nothing {
            error("Should never be called")
        }

        override fun logError(error: CfirConstraintSystemError, system: CfirConstraintSystemMarker): Nothing {
            error("Should never be called")
        }

        override fun logNewVariable(variable: CfirTypeVariable, system: CfirConstraintSystemMarker): Nothing {
            error("Should never be called")
        }

        override fun logReadiness(
            fixationLog: FixationLogRecord,
            system: CfirConstraintSystemMarker,
        ): Nothing {
            error("Should never be called")
        }

        override fun <T> withOrigin(constraint: CfirInitialConstraint, block: () -> T): Nothing {
            error("Should never be called")
        }

        override fun <T> withOrigins(
            variable1: CfirTypeVariable,
            constraint1: CfirVariableConstraint,
            variable2: CfirTypeVariable,
            constraint2: CfirVariableConstraint,
            block: () -> T,
        ): Nothing {
            error("Should never be called")
        }

        override fun logFixVariable(
            variable: CfirTypeVariable,
            resultType: ConeCangjieType,
            system: CfirConstraintSystemMarker,
        ): Nothing {
            error("Should never be called")
        }
    }
}

inline fun <T> InferenceLogger?.withOrigin(
    constraint: CfirInitialConstraint,
    crossinline block: () -> T,
): T = when {
    this == null -> block()
    else -> withOrigin(constraint) { block() }
}

inline fun <T> InferenceLogger?.withOrigins(
    variable1: CfirTypeVariable,
    constraint1: CfirVariableConstraint,
    variable2: CfirTypeVariable,
    constraint2: CfirVariableConstraint,
    crossinline block: () -> T,
): T = when {
    this == null -> block()
    else -> withOrigins(variable1, constraint1, variable2, constraint2) { block() }
}


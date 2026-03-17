package org.cangnova.cangjie.test

import org.cangnova.cangjie.test.model.AbstractGroupingPhaseTestFacade
import org.cangnova.cangjie.test.model.AbstractTestFacade
import org.cangnova.cangjie.test.model.AnalysisHandler
import org.cangnova.cangjie.test.model.GroupingPhaseHandler
import org.cangnova.cangjie.test.model.TestModule

/**
 * 包装的异常
 *
 * 对应 Kotlin K2 的 WrappedException
 */
sealed class WrappedException(
    cause: Throwable,
    val priority: Int,
    val additionalPriority: Int,
) : Exception(cause), Comparable<WrappedException> {
    open val failureDisablesNextSteps: Boolean get() = true
    class FromGroupingFacade(
        cause: Throwable,
        val facade: AbstractGroupingPhaseTestFacade<*, *>,
    ) : WrappedException(cause, 0, 1) {
        override val failedModule: TestModule?
            get() = null

        override val message: String
            get() = "Exception was thrown"

        override fun withReplacedCause(newCause: Throwable):  WrappedException {
            return FromGroupingFacade(newCause, facade)
        }
    }
    abstract val failedModule: TestModule?
    class FromGroupingHandler(
        cause: Throwable,
        val handler: GroupingPhaseHandler<*>,
    ) : WrappedException(cause, 1, 3) {
        override val failedModule: TestModule? get() = null

        override val failureDisablesNextSteps: Boolean
            get() = handler.failureDisablesNextSteps

        override fun withReplacedCause(newCause: Throwable):  WrappedException {
            return FromGroupingHandler(newCause, handler)
        }
    }
    class FromFacade(
        cause: Throwable,
        override val failedModule: TestModule,
        val facade: AbstractTestFacade<*, *>,
    ) : WrappedException(cause, 0, 1) {
        override val message: String
            get() = "Exception was thrown"

        override fun withReplacedCause(newCause: Throwable): WrappedException {
            return FromFacade(newCause, failedModule, facade)
        }
    }

    class FromHandler(
        cause: Throwable,
        override val failedModule: TestModule?,
        val handler: AnalysisHandler<*>,
    ) : WrappedException(cause, 1, 3) {
        override val failureDisablesNextSteps: Boolean
            get() = handler.failureDisablesNextSteps

        override fun withReplacedCause(newCause: Throwable): WrappedException {
            return FromHandler(newCause, failedModule, handler)
        }
    }

    sealed class WrappedExceptionWithoutModule(
        cause: Throwable,
        priority: Int,
        additionalPriority: Int,
    ) : WrappedException(cause, priority, additionalPriority) {
        override val failedModule: TestModule?
            get() = null
    }

    class FromPreAnalysisHandler(cause: Throwable) : WrappedExceptionWithoutModule(cause, 1, 1) {
        override fun withReplacedCause(newCause: Throwable): WrappedException {
            return FromPreAnalysisHandler(newCause)
        }
    }

    class FromMetaInfoHandler(cause: Throwable) : WrappedExceptionWithoutModule(cause, 1, 2) {
        override fun withReplacedCause(newCause: Throwable): WrappedException {
            return FromMetaInfoHandler(newCause)
        }
    }

    class FromAfterAnalysisChecker(cause: Throwable) : WrappedExceptionWithoutModule(cause, 2, 1) {
        override fun withReplacedCause(newCause: Throwable): WrappedException {
            return FromAfterAnalysisChecker(newCause)
        }
    }

    class FromModuleStructureTransformer(cause: Throwable) : WrappedExceptionWithoutModule(cause, 2, 1) {
        override fun withReplacedCause(newCause: Throwable): WrappedException {
            return FromModuleStructureTransformer(newCause)
        }
    }

    final override val cause: Throwable
        get() = super.cause!!

    override fun compareTo(other: WrappedException): Int {
        if (priority == other.priority) {
            return additionalPriority - other.additionalPriority
        }
        return priority - other.priority
    }

    abstract fun withReplacedCause(newCause: Throwable): WrappedException
}

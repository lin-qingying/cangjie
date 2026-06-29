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
    /**
     * 保存 `priority`，供测试基础设施在测试执行期间读取或传递。
     */
    val priority: Int,
    /**
     * 保存 `additionalPriority`，供测试基础设施在测试执行期间读取或传递。
     */
    val additionalPriority: Int,
) : Exception(cause), Comparable<WrappedException> {
    /**
     * 保存 `failureDisablesNextSteps`，供测试基础设施在测试执行期间读取或传递。
     */
    open val failureDisablesNextSteps: Boolean get() = true
    /**
     * 表示 `FromGroupingFacade`，承载测试基础设施中的配置数据、测试产物或处理步骤。
     */
    class FromGroupingFacade(
        cause: Throwable,
        /**
         * 保存 `facade`，供测试基础设施在测试执行期间读取或传递。
         */
        val facade: AbstractGroupingPhaseTestFacade<*, *>,
    ) : WrappedException(cause, 0, 1) {
        /**
         * 保存 `failedModule`，供测试基础设施在测试执行期间读取或传递。
         */
        override val failedModule: TestModule?
            get() = null

        /**
         * 保存 `message`，供测试基础设施在测试执行期间读取或传递。
         */
        override val message: String
            get() = "Exception was thrown"

        /**
         * 执行 `withReplacedCause` 对应的测试基础设施流程，维持测试框架的阶段契约。
         */
        override fun withReplacedCause(newCause: Throwable):  WrappedException {
            return FromGroupingFacade(newCause, facade)
        }
    }
    /**
     * 保存 `failedModule`，供测试基础设施在测试执行期间读取或传递。
     */
    abstract val failedModule: TestModule?
    /**
     * 表示 `FromGroupingHandler`，承载测试基础设施中的配置数据、测试产物或处理步骤。
     */
    class FromGroupingHandler(
        cause: Throwable,
        /**
         * 保存 `handler`，供测试基础设施在测试执行期间读取或传递。
         */
        val handler: GroupingPhaseHandler<*>,
    ) : WrappedException(cause, 1, 3) {
        /**
         * 保存 `failedModule`，供测试基础设施在测试执行期间读取或传递。
         */
        override val failedModule: TestModule? get() = null

        /**
         * 保存 `failureDisablesNextSteps`，供测试基础设施在测试执行期间读取或传递。
         */
        override val failureDisablesNextSteps: Boolean
            get() = handler.failureDisablesNextSteps

        /**
         * 执行 `withReplacedCause` 对应的测试基础设施流程，维持测试框架的阶段契约。
         */
        override fun withReplacedCause(newCause: Throwable):  WrappedException {
            return FromGroupingHandler(newCause, handler)
        }
    }
    /**
     * 表示 `FromFacade`，承载测试基础设施中的配置数据、测试产物或处理步骤。
     */
    class FromFacade(
        cause: Throwable,
        /**
         * 保存 `failedModule`，供测试基础设施在测试执行期间读取或传递。
         */
        override val failedModule: TestModule,
        /**
         * 保存 `facade`，供测试基础设施在测试执行期间读取或传递。
         */
        val facade: AbstractTestFacade<*, *>,
    ) : WrappedException(cause, 0, 1) {
        /**
         * 保存 `message`，供测试基础设施在测试执行期间读取或传递。
         */
        override val message: String
            get() = "Exception was thrown"

        /**
         * 执行 `withReplacedCause` 对应的测试基础设施流程，维持测试框架的阶段契约。
         */
        override fun withReplacedCause(newCause: Throwable): WrappedException {
            return FromFacade(newCause, failedModule, facade)
        }
    }

    /**
     * 表示 `FromHandler`，承载测试基础设施中的配置数据、测试产物或处理步骤。
     */
    class FromHandler(
        cause: Throwable,
        /**
         * 保存 `failedModule`，供测试基础设施在测试执行期间读取或传递。
         */
        override val failedModule: TestModule?,
        /**
         * 保存 `handler`，供测试基础设施在测试执行期间读取或传递。
         */
        val handler: AnalysisHandler<*>,
    ) : WrappedException(cause, 1, 3) {
        /**
         * 保存 `failureDisablesNextSteps`，供测试基础设施在测试执行期间读取或传递。
         */
        override val failureDisablesNextSteps: Boolean
            get() = handler.failureDisablesNextSteps

        /**
         * 执行 `withReplacedCause` 对应的测试基础设施流程，维持测试框架的阶段契约。
         */
        override fun withReplacedCause(newCause: Throwable): WrappedException {
            return FromHandler(newCause, failedModule, handler)
        }
    }

    /**
     * 表示 `WrappedExceptionWithoutModule`，承载测试基础设施中的配置数据、测试产物或处理步骤。
     */
    sealed class WrappedExceptionWithoutModule(
        cause: Throwable,
        priority: Int,
        additionalPriority: Int,
    ) : WrappedException(cause, priority, additionalPriority) {
        /**
         * 保存 `failedModule`，供测试基础设施在测试执行期间读取或传递。
         */
        override val failedModule: TestModule?
            get() = null
    }

    /**
     * 表示 `FromPreAnalysisHandler`，承载测试基础设施中的配置数据、测试产物或处理步骤。
     */
    class FromPreAnalysisHandler(cause: Throwable) : WrappedExceptionWithoutModule(cause, 1, 1) {
        /**
         * 执行 `withReplacedCause` 对应的测试基础设施流程，维持测试框架的阶段契约。
         */
        override fun withReplacedCause(newCause: Throwable): WrappedException {
            return FromPreAnalysisHandler(newCause)
        }
    }

    /**
     * 表示 `FromMetaInfoHandler`，承载测试基础设施中的配置数据、测试产物或处理步骤。
     */
    class FromMetaInfoHandler(cause: Throwable) : WrappedExceptionWithoutModule(cause, 1, 2) {
        /**
         * 执行 `withReplacedCause` 对应的测试基础设施流程，维持测试框架的阶段契约。
         */
        override fun withReplacedCause(newCause: Throwable): WrappedException {
            return FromMetaInfoHandler(newCause)
        }
    }

    /**
     * 表示 `FromAfterAnalysisChecker`，承载测试基础设施中的配置数据、测试产物或处理步骤。
     */
    class FromAfterAnalysisChecker(cause: Throwable) : WrappedExceptionWithoutModule(cause, 2, 1) {
        /**
         * 执行 `withReplacedCause` 对应的测试基础设施流程，维持测试框架的阶段契约。
         */
        override fun withReplacedCause(newCause: Throwable): WrappedException {
            return FromAfterAnalysisChecker(newCause)
        }
    }

    /**
     * 表示 `FromModuleStructureTransformer`，承载测试基础设施中的配置数据、测试产物或处理步骤。
     */
    class FromModuleStructureTransformer(cause: Throwable) : WrappedExceptionWithoutModule(cause, 2, 1) {
        /**
         * 执行 `withReplacedCause` 对应的测试基础设施流程，维持测试框架的阶段契约。
         */
        override fun withReplacedCause(newCause: Throwable): WrappedException {
            return FromModuleStructureTransformer(newCause)
        }
    }

    final override val cause: Throwable
        get() = super.cause!!

    /**
     * 执行 `compareTo` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    override fun compareTo(other: WrappedException): Int {
        if (priority == other.priority) {
            return additionalPriority - other.additionalPriority
        }
        return priority - other.priority
    }

    /**
     * 提供 `withReplacedCause` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    abstract fun withReplacedCause(newCause: Throwable): WrappedException
}

package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.resolve.calls.inference.model.Constraint
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemError
import org.cangnova.cangjie.resolve.calls.inference.model.InitialConstraint
import org.cangnova.cangjie.type.model.CangJieTypeMarker
import org.cangnova.cangjie.type.model.TypeVariableMarker

/**
 * 类型推断过程的可插拔日志记录器。
 */
open class InferenceLogger {
    /**
     * 固定候选变量的日志信息。
     */
    data class FixationLogVariableInfo(
        /** 变量当前 readiness。 */
        val readiness: Any?,
        /** 变量当前约束列表。 */
        val constraints: List<Constraint>,
    )

    /**
     * 一次变量固定选择的日志记录。
     */
    data class FixationLogRecord(
        /** 每个候选变量对应的 readiness 信息。 */
        val readinessPerVariable: Map<TypeVariableMarker, FixationLogVariableInfo>,
        /** 本轮最终选择固定的变量。 */
        val chosenVariable: TypeVariableMarker?,
    )

    /**
     * 记录初始约束。
     */
    open fun logInitial(initialConstraint: InitialConstraint, context: Any?) {}

    /**
     * 记录新注册的类型变量。
     */
    open fun logNewVariable(variable: TypeVariableMarker, context: Any?) {}

    /**
     * 记录类型变量新增约束。
     */
    open fun log(variable: TypeVariableMarker, constraint: Constraint, context: Any?) {}

    /**
     * 记录约束系统错误。
     */
    open fun logError(error: ConstraintSystemError, context: Any?) {}

    /**
     * 记录变量固定 readiness 选择。
     */
    open fun logReadiness(record: FixationLogRecord, context: Any?) {}

    /**
     * 记录类型变量固定结果。
     */
    open fun logFixVariable(variable: TypeVariableMarker, resultType: CangJieTypeMarker, context: Any?) {}

    /**
     * 在指定来源上下文中执行动作。
     */
    open fun <T> withOrigin(origin: Any?, action: () -> T): T = action()

    /**
     * 在两个来源上下文中执行动作。
     */
    open fun <T> withOrigins(
        firstOwner: Any?,
        firstOrigin: Any?,
        secondOwner: Any?,
        secondOrigin: Any?,
        action: () -> T,
    ): T = action()

    /**
     * 无操作日志记录器。
     */
    object Dummy : InferenceLogger()
}

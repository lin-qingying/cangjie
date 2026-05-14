package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.evaluation.CaCompileTimeValue
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.psi.CjExpression

/**
 * 编译期表达式求值协议。
 *
 * 设计要点/职责:
 * - 尝试把表达式归约为编译期常量结果 [CaCompileTimeValue],无法求值时返回 `null`。
 * - 仅暴露稳定结果,不对外提供求值引擎或中间状态;失败属于正常分支,不抛异常。
 *
 * 对齐 Kotlin Analysis API 的 `KaEvaluator`。
 */
interface CaEvaluator : CaLifetimeOwner {
    /**
     * 尝试把当前表达式归约为编译期常量值;若无法归约则返回 `null`。
     */
    fun CjExpression.evaluate(): CaCompileTimeValue?
}

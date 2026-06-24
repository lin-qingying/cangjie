package org.cangnova.cangjie.cfir

/**
 * CFIR 编译期求值结果。
 */
sealed class CfirEvaluatorResult {
    /**
     * 表示求值成功。
     *
     * @property result 求值后得到的 CFIR 元素。
     */
    class Evaluated(val result: CfirElement) : CfirEvaluatorResult() {
        /**
         * 使用 CFIR renderer 输出求值结果。
         */
        override fun toString(): String = result.render()
    }

    /**
     * 表示当前表达式没有完成编译期求值。
     */
    data object NotEvaluated : CfirEvaluatorResult()

    /**
     * 表示求值正在进行中，用于检测递归求值。
     */
    data object DuringEvaluation : CfirEvaluatorResult()

    /**
     * 编译期求值失败的基类。
     */
    sealed class CompileTimeException : CfirEvaluatorResult()

    /**
     * 编译期除零错误。
     */
    data object DivisionByZero : CompileTimeException()

    /**
     * 初始化器递归求值错误。
     */
    data object RecursionInInitializer : CompileTimeException()
}

/**
 * 将求值结果解包为 [T]。
 *
 * 若结果是编译期异常，则调用 [action]；若结果类型不匹配或未求值，返回 `null`。
 */
inline fun <reified T : CfirElement> CfirEvaluatorResult.unwrapOr(action: (CfirEvaluatorResult.CompileTimeException) -> Unit): T? {
    when (this) {
        is CfirEvaluatorResult.CompileTimeException -> action(this)
        is CfirEvaluatorResult.Evaluated -> return this.result as? T
        else -> return null
    }
    return null
}

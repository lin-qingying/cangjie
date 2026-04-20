package org.cangnova.cangjie.cfir

sealed class CfirEvaluatorResult {
    class Evaluated(val result: CfirElement) : CfirEvaluatorResult() {
        override fun toString(): String = result.render()
    }

    data object NotEvaluated : CfirEvaluatorResult()
    data object DuringEvaluation : CfirEvaluatorResult()

    sealed class CompileTimeException : CfirEvaluatorResult()
    data object DivisionByZero : CompileTimeException()
    data object RecursionInInitializer : CompileTimeException()
}

inline fun <reified T : CfirElement> CfirEvaluatorResult.unwrapOr(action: (CfirEvaluatorResult.CompileTimeException) -> Unit): T? {
    when (this) {
        is CfirEvaluatorResult.CompileTimeException -> action(this)
        is CfirEvaluatorResult.Evaluated -> return this.result as? T
        else -> return null
    }
    return null
}

package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirSpawnExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull

/**
 * Spawn 语义检查器
 *
 * 对齐 C++ TypeCheckExpr/SpawnExpr.cpp:
 * - spawn 表达式的 body 类型必须合法
 * - spawn 表达式本身的推断类型不能为错误类型
 *
 * 因为 CfirSpawnExpression 通过 visitAlso 注册到 BasicExpressionChecker，
 * 所以此 checker 需要在 check 方法中手动过滤类型。
 */
object CfirSpawnSemanticsChecker : CfirBasicExpressionChecker() {
    /** 过滤 spawn 表达式并执行 spawn body 类型与参数有效性检查。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirStatement) {
        if (expression !is CfirSpawnExpression) return

        checkSpawnBodyType(expression)
        checkSpawnArgNoEffect(expression)
    }

    /**
     * spawn body 中的类型推断必须成功。
     *
     * 对齐 C++ DiagKind::sema_spawn_invalid_argument
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSpawnBodyType(spawn: CfirSpawnExpression) {
        val bodyType = spawn.body.coneTypeOrNull
        if (bodyType is ConeErrorType) {
            reporter.reportOn(
                source = spawn.source,
                factory = CfirErrors.SPAWN_ARG_INVALID,
            )
        }
    }

    /**
     * spawn 参数在当前后端不生效时发出警告。
     *
     * 对齐 C++ DiagKind::sema_spawn_arg_no_effect:
     * 当 spawn 表达式携带 thread-context 参数(CFIR `threadContextArgument`,对齐 C++ AST 中 `SpawnExpr.arg`)
     * 但当前后端不消费该参数时发警告。仓颉前端始终忽略 spawn 参数,因此只要存在参数即报警告。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSpawnArgNoEffect(spawn: CfirSpawnExpression) {
        val ctxArg = spawn.threadContextArgument ?: return
        reporter.reportOn(
            source = ctxArg.source ?: spawn.source,
            factory = CfirErrors.SPAWN_ARG_NO_EFFECT,
        )
    }
}

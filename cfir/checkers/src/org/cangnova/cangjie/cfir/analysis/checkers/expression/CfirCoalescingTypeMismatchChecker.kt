package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOp
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOpKind
import org.cangnova.cangjie.cfir.types.ConeCoalescingLeftOperandInvalid
import org.cangnova.cangjie.cfir.types.ConeCoalescingRightOperandMismatch
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.reportOn

/**
 * `??` 右操作数的目标类型检查器。
 *
 * 对齐官方 `ChkCoalescingExpr`（`Sema/TypeCheckExpr/BinaryExpr.cpp:1135-1140`）：右操作数在
 * `realTgtTy` 下检查，失败时诊断锚在**右操作数**上——字面量走共享的 `CANNOT_CONVERT_LITERAL`
 * 分类，其余走 `TYPE_MISMATCH`。
 *
 * `realTgtTy`（上下文目标类型，缺省为左操作数 Option 的元素类型）只在 resolve 上下文可得
 * ——同一个 `??` 作为调用实参时，元素类型并不等于 `realTgtTy`（如 `test(v ?? "fail")`，
 * `v: ?Int64`、形参 `ToString`），checker 若自行用元素类型重判会把合法实参误报。因此判定
 * 归属 resolve：`CfirExpressionsResolveTransformer` 在同一判据（`isDefiniteCoalescingRightMismatch`）
 * 下把 `??` 标记为 [ConeCoalescingRightOperandMismatch] 并携带 `realTgtTy`，本 checker 只消费
 * 该标记把诊断锚到右操作数，两边不出现对同一事实的第二套判据。
 */
object CfirCoalescingTypeMismatchChecker : CfirBinaryOpChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirBinaryOp) {
        if (expression.kind != CfirBinaryOpKind.COALESCING) return

        val errorDiagnostic = (expression.coneTypeOrNull as? ConeErrorType)?.diagnostic
        when (errorDiagnostic) {
            // 官方 `ChkCoalescingExpr`（BinaryExpr.cpp:1105-1118）：左操作数已定型且不是
            // Option 时报 sema_invalid_coalescing，锚在左操作数。
            is ConeCoalescingLeftOperandInvalid -> reporter.reportOn(
                source = expression.left.source,
                factory = CfirErrors.INVALID_COALESCING,
            )

            is ConeCoalescingRightOperandMismatch -> {
                // 下钻共享 target-typed 检查：叶子按 `checkTypeMismatch` 分类（字面量 -> CANNOT_CONVERT_LITERAL，
                // 其余 -> TYPE_MISMATCH，均锚在右操作数），复合体（block/if/match/...）继续下钻到真实结果位置。
                val marker = errorDiagnostic as ConeCoalescingRightOperandMismatch
                checkTargetTypedTailExpression(expression.right, marker.targetType)
            }
        }
    }
}

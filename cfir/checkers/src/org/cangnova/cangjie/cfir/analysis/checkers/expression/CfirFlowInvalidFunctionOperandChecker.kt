/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.analysis.diagnostics.renderInvalidBinaryOperatorType
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOp
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFlowInvalidFunctionOperand
import org.cangnova.cangjie.cfir.types.FlowInvalidOperandKeyword
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.containsErrorType

/**
 * flow 表达式函数部分的裸 `this` / `super` 检查器。
 *
 * 对齐官方 `ChkFlowExpr`（`external/cangjie_compiler/src/Sema/TypeCheckExpr/BinaryExpr.cpp:1044-1049`）：
 * `~>` 的两侧、`|>` 的函数部分不允许是裸 `this`（`this |> f` 合法）。
 *
 * 判据本身归属于 resolve——命中时官方不再解糖并直接把 flow 表达式置为 InvalidTy，
 * 这与"构造合成调用"的常规路径互斥。因此 resolve 命中后携带
 * [ConeFlowInvalidFunctionOperand] 并返回未解糖的二元节点，本 checker 只消费该标记映射诊断，
 * 不重复判定，避免同一事实出现第二套判据。
 *
 * - 裸 `this`：报 flow 专属诊断（官方 `sema_flow_expressions_use_this_or_super`），锚在命中的
 *   `this` 操作数上；
 * - 裸 `super`：官方**不**报该 flow 诊断（裸 super 由 `InferSuperExpr` 的
 *   `sema_illegal_super_alone` 负责，本仓对应 [CfirErrors.ILLEGAL_SUPER_ALONE]，由
 *   `CfirIllegalSuperReferenceChecker` 在不再被包装的操作数上报告），flow 表达式解析失败本身
 *   报二元运算符诊断，锚在操作符上。
 */
object CfirFlowInvalidFunctionOperandChecker : CfirBinaryOpChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirBinaryOp) {
        val marker = (expression.coneTypeOrNull as? ConeErrorType)
            ?.diagnostic as? ConeFlowInvalidFunctionOperand
            ?: return

        when (marker.keyword) {
            FlowInvalidOperandKeyword.THIS -> reporter.reportOn(
                source = expression.operandAt(marker.isLeftOperand).source ?: expression.source,
                factory = CfirErrors.FLOW_EXPRESSIONS_USE_THIS_OR_SUPER,
                a = marker.keyword.sourceText,
            )

            FlowInvalidOperandKeyword.SUPER -> {
                // 与既有二元运算符诊断口径一致：任一侧含错误类型时不报，避免在错误操作数上
                // 派生二次诊断（对位 `invalidBinaryOperatorDiagnosticForOperatorCall` 的守卫）。
                val leftType = expression.left.coneTypeOrNull ?: return
                val rightType = expression.right.coneTypeOrNull ?: return
                if (leftType.containsErrorType() || rightType.containsErrorType()) return

                reporter.reportOn(
                    source = expression.source,
                    factory = CfirErrors.INVALID_BINARY_OPERATOR,
                    a = expression.kind.symbol,
                    b = leftType.renderInvalidBinaryOperatorType(context.session),
                    c = rightType.renderInvalidBinaryOperatorType(context.session),
                )
            }
        }
    }
}

/** 取标记指向的操作数。 */
private fun CfirBinaryOp.operandAt(isLeftOperand: Boolean): CfirExpression =
    if (isLeftOperand) left else right

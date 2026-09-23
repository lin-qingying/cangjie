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
 * which allows users to freely use, modify, and distribute the software,
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
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFlowInvalidFunctionOperand
import org.cangnova.cangjie.cfir.types.ConeInvalidFlowBinaryExpr
import org.cangnova.cangjie.cfir.types.FlowInvalidOperandKeyword
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.containsErrorType

/**
 * flow 表达式（`|>` / `~>`）失败的诊断消费端。
 *
 * flow 的两条官方失败路径都发生在 resolve 阶段，且都以**不解糖 / 回退到二元节点**收尾，
 * 因此都由 resolve 携带标记、本 checker 只负责把标记映射成诊断，不重复判定
 * （避免同一事实出现第二套判据）：
 *
 * 1. **裸 `this` / `super`** 命中官方 flow 操作数判据
 *    （`external/cangjie_compiler/src/Sema/TypeCheckExpr/BinaryExpr.cpp:1044-1049`）：
 *    `~>` 的两侧、`|>` 的函数部分不允许是裸 `this`（`this |> f` 合法）。标记为
 *    [ConeFlowInvalidFunctionOperand]——裸 `this` 报 flow 专属诊断
 *    （`sema_flow_expressions_use_this_or_super`）锚在命中的 `this` 上；裸 `super` 官方
 *    **不**报该诊断（由 `InferSuperExpr` 的 `sema_illegal_super_alone` 负责，本仓对应
 *    [CfirErrors.ILLEGAL_SUPER_ALONE]），flow 失败本身报二元运算符诊断。
 *
 * 2. **解糖调用失败**（`BinaryExpr.cpp:1057-1070`）：官方丢弃调用自身的全部诊断并
 *    `RecoverToBinaryExpr`，只在操作符上报 `sema_invalid_binary_expr`。标记为
 *    [ConeInvalidFlowBinaryExpr]——合成的 `this |> g` 实参失配、`x ~> g` 的
 *    `UNRESOLVED_REFERENCE` 都属于被丢弃的调用诊断，不会单独报出。
 */
object CfirFlowBinaryOpChecker : CfirBinaryOpChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirBinaryOp) {
        when (val marker = (expression.coneTypeOrNull as? ConeErrorType)?.diagnostic) {
            is ConeFlowInvalidFunctionOperand -> when (marker.keyword) {
                FlowInvalidOperandKeyword.THIS -> reporter.reportOn(
                    source = expression.operandAt(marker.isLeftOperand).source ?: expression.source,
                    factory = CfirErrors.FLOW_EXPRESSIONS_USE_THIS_OR_SUPER,
                    a = marker.keyword.sourceText,
                )

                FlowInvalidOperandKeyword.SUPER -> reportInvalidBinaryOperator(expression)
            }

            is ConeInvalidFlowBinaryExpr -> reportInvalidBinaryOperator(
                expression = expression,
                leftOperandType = marker.leftOperandType,
                rightOperandType = marker.rightOperandType,
            )

            else -> Unit
        }
    }

    /**
     * 在操作符上报二元运算符诊断，渲染两侧操作数类型。
     *
     * 类型优先取解糖前的快照：`|>` 的左操作数随后会作为解糖调用的实参被重新检查，失败时
     * 会被写成错误类型，而官方的操作符诊断渲染的是操作数综合阶段的类型。
     *
     * 守卫与既有二元运算符路径一致（对位 `invalidBinaryOperatorDiagnosticForOperatorCall`）：
     * 任一侧含错误类型时不报，避免在已经失败的操作数上派生二次诊断。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportInvalidBinaryOperator(
        expression: CfirBinaryOp,
        leftOperandType: ConeCangJieType? = null,
        rightOperandType: ConeCangJieType? = null,
    ) {
        val leftType = leftOperandType ?: expression.left.coneTypeOrNull ?: return
        val rightType = rightOperandType ?: expression.right.coneTypeOrNull ?: return
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

/** 取标记指向的操作数。 */
private fun CfirBinaryOp.operandAt(isLeftOperand: Boolean): CfirExpression =
    if (isLeftOperand) left else right

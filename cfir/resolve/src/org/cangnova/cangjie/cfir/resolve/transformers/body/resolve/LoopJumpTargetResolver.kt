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

package org.cangnova.cangjie.cfir.resolve.transformers.body.resolve

import org.cangnova.cangjie.cfir.expressions.CfirLoopExpression
import org.cangnova.cangjie.cfir.expressions.CfirLoopJump
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.types.ConeErrorType
import kotlin.collections.ArrayDeque

/**
 * loop jump 绑定使用的结构式作用域。
 *
 * body resolve 阶段不再像 raw build 那样按循环转换顺序压栈（`while(break)` 与
 * `do{}while(break)` 语义不同，顺序编码无法区分）；这里由表达式 transformer 按
 * 循环的**结构区域**显式维护：
 *
 * - [Body]：循环体区域，其中的 break/continue 绑定到该循环；
 * - [ConditionPart]：while 条件、do-while 条件、for-in iterable / patternGuard
 *   区域。官方 `IsRefLoop`（`ScopeManager.cpp`）语义下，jump 位于某循环自身
 *   条件区域时不绑定该循环，而是向外绑定；
 * - [FunctionBoundary]：函数边界。break/continue 不能穿透 lambda、局部函数等
 *   任何函数体，栈底恒为该边界。
 */
sealed class LoopJumpScope {
    /** 循环体区域。 */
    data class Body(val loop: CfirLoopExpression) : LoopJumpScope()

    /**
     * 循环条件类区域。
     *
     * 同一循环的 iterable 与 patternGuard 各有一个区域，但同一时刻最多只有一个
     * 处于栈顶，因此不会同时出现两个 `ConditionPart(loop)` 兄弟区域。
     */
    data class ConditionPart(val loop: CfirLoopExpression) : LoopJumpScope()

    /** 函数边界。 */
    data object FunctionBoundary : LoopJumpScope()
}

/**
 * 按结构式区域栈解析 loop jump 的隐式最近循环目标。
 *
 * 算法对齐官方 cjc `GetRefLoopSymbol` + `IsRefLoop`（`external/cangjie_compiler/
 * src/Sema/ScopeManager.cpp`）：从栈顶向下扫描，最内层是 [LoopJumpScope.Body]
 * 时直接绑定；最内层是某循环的 [LoopJumpScope.ConditionPart] 时先跳过该循环，
 * 继续向外；遇到另一个循环的区域则绑定之（对应 `while (while (break))` 中
 * break 绑定外层 while 的语义——外层条件里的嵌套循环子树被 walker 整体跳过）；
 * 命中 [LoopJumpScope.FunctionBoundary] 或栈空时没有可绑定循环，jump 挂
 * `JumpOutsideLoop` 错误类型，由诊断收集器映射为 `INVALID_LOOP_CONTROL`。
 */
object LoopJumpTargetResolver {

    /**
     * 绑定 [jump] 的隐式目标，找不到循环时在 jump 自身挂错误类型。
     *
     * 错误诊断不构造错误 loop 节点（对齐现有收集器：错误 jump 诊断直接挂在
     * jump 自身），因此 cfir2chir 等下游必须跳过带错误类型的 jump。
     */
    fun bindLoopJump(
        jump: CfirLoopJump,
        scopes: ArrayDeque<LoopJumpScope>,
    ) {
        val loop = resolveLoopTarget(scopes)
        if (loop == null) {
            jump.replaceConeTypeOrNull(
                ConeErrorType(
                    ConeSimpleDiagnostic(
                        reason = "'break' or 'continue' must be used inside a loop",
                        kind = DiagnosticKind.JumpOutsideLoop,
                    )
                )
            )
            return
        }
        jump.target.bind(loop)
    }

    /** 从区域栈解析隐式最近循环；无可用循环时返回 null。 */
    private fun resolveLoopTarget(scopes: ArrayDeque<LoopJumpScope>): CfirLoopExpression? {
        val first = scopes.lastOrNull() ?: return null
        return when (first) {
            is LoopJumpScope.Body -> first.loop
            is LoopJumpScope.ConditionPart -> {
                val skippedLoop = first.loop
                for (index in scopes.size - 2 downTo 0) {
                    when (val scope = scopes[index]) {
                        is LoopJumpScope.Body -> return scope.loop
                        is LoopJumpScope.ConditionPart -> {
                            if (scope.loop !== skippedLoop) {
                                return scope.loop
                            }
                        }
                        is LoopJumpScope.FunctionBoundary -> return null
                    }
                }
                null
            }
            is LoopJumpScope.FunctionBoundary -> null
        }
    }
}
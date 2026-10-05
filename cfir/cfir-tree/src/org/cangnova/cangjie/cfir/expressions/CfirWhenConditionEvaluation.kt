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
 */

package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.session.CfirConditionalCompilationCatalog
import org.cangnova.cangjie.cfir.session.CfirConditionalCompilationError
import org.cangnova.cangjie.cfir.session.CfirConditionalCompilationFailure
import org.cangnova.cangjie.cfir.session.CfirConditionalCompilationOperator

/** 官方 `cjc_version` 要求的三段版本号格式（`xx.xx.xx`，DiagnosticSema.def 的 "should be 'xx.xx.xx'"）。 */
private val CJC_VERSION_FORMAT = Regex("""\d+\.\d+\.\d+""")

/** [CfirComparisonOp] 到条件编译操作符契约枚举的映射。 */
private fun CfirComparisonOp.toConditionalOperator(): CfirConditionalCompilationOperator = when (this) {
    CfirComparisonOp.EQ -> CfirConditionalCompilationOperator.EQ
    CfirComparisonOp.NE -> CfirConditionalCompilationOperator.NE
    CfirComparisonOp.LT -> CfirConditionalCompilationOperator.LT
    CfirComparisonOp.GT -> CfirConditionalCompilationOperator.GT
    CfirComparisonOp.LE -> CfirConditionalCompilationOperator.LE
    CfirComparisonOp.GE -> CfirConditionalCompilationOperator.GE
}

/**
 * `@When[...]` 条件的 raw 求值；light-tree 与 PSI 两条 raw 路径共享的唯一判定入口。
 *
 * 官方（external/cangjie_compiler）对 `@When` 条件做独立校验，规则链与顺序
 * （cjc 1.0.5 / 1.1.3 双版实测，见 diagnostics2/conditional-compilation 夹具头部取证）：
 *  1. 条件名必须是 [CfirConditionalCompilationCatalog.builtins] 之一，否则
 *     [CfirConditionalCompilationError.UNKNOWN_CONDITION]（锚条件名）；
 *  2. 操作符必须在该条件的契约集合内（`debug`/`test` 为空集合，任何比较都不允许），
 *     否则 [CfirConditionalCompilationError.UNSUPPORTED_OPERATOR]（锚条件名）；
 *  3. 右值必须是字符串字面量（变量、整数字面量都不行），否则
 *     [CfirConditionalCompilationError.INVALID_VALUE]（锚右值）；
 *  4. `cjc_version` 的右值必须是三段版本号，否则
 *     [CfirConditionalCompilationError.INVALID_VERSION]（锚条件名）；
 *  5. 枚举条件的右值必须落在白名单内，否则
 *     [CfirConditionalCompilationError.UNSUPPORTED_VALUE]（锚条件名）。
 *
 * 只处理「条件名 op 右值」的比较形态：其它形态官方不产生语义诊断——`@When[1]`
 * 是官方 parse 阶段错误（本项目没有对应诊断名），`@When[debug]` 纯开关合法——
 * 因此返回 `null` 静默。诊断的 source 按项目 Diagnostic Range Policy 锚完整
 * 条件名 / 完整右值 token，不镜像官方的 GetBegin 1 字符锚。
 *
 * @return 求值失败事实；条件合法或形态不在校验范围内时返回 `null`。
 */
fun CfirComparisonExpression.whenConditionFailureOrNull(): CfirConditionalCompilationFailure? {
    val reference = (left as? CfirQualifiedAccessExpression)?.calleeReference as? CfirNamedReference
        ?: return null
    val conditionName = reference.name.asString()
    val operator = operation.toConditionalOperator()
    val spec = CfirConditionalCompilationCatalog.builtin(conditionName)
        ?: return failure(
            CfirConditionalCompilationError.UNKNOWN_CONDITION,
            source = left.source ?: source,
            conditionName = conditionName,
        )

    if (operator !in spec.operators) {
        return failure(
            CfirConditionalCompilationError.UNSUPPORTED_OPERATOR,
            source = left.source ?: source,
            conditionName = conditionName,
            operator = operator,
        )
    }

    val literal = right as? CfirLiteralExpression
    val rightValue = literal?.value as? String
    if (literal == null || literal.kind != CfirLiteralKind.STRING || rightValue == null) {
        return failure(
            CfirConditionalCompilationError.INVALID_VALUE,
            source = right.source ?: source,
        )
    }

    if (spec.name == CfirConditionalCompilationCatalog.cjcVersion.name && !rightValue.matches(CJC_VERSION_FORMAT)) {
        return failure(
            CfirConditionalCompilationError.INVALID_VERSION,
            source = left.source ?: source,
            conditionName = conditionName,
        )
    }

    val supportedValues = spec.supportedValues
    if (supportedValues != null && rightValue !in supportedValues) {
        return failure(
            CfirConditionalCompilationError.UNSUPPORTED_VALUE,
            source = left.source ?: source,
            conditionName = conditionName,
            rightValue = rightValue,
        )
    }

    return null
}

/** [CfirConditionalCompilationFailure] 的便捷构造：失败锚位由判定结果决定，见 [whenConditionFailureOrNull]。 */
private fun failure(
    reason: CfirConditionalCompilationError,
    source: org.cangnova.cangjie.source.CjSourceElement?,
    conditionName: String? = null,
    operator: CfirConditionalCompilationOperator? = null,
    rightValue: String? = null,
): CfirConditionalCompilationFailure = CfirConditionalCompilationFailure(
    reason = reason,
    source = source,
    conditionName = conditionName,
    rightValue = rightValue,
    operator = operator,
)

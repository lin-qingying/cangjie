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
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.expandedPatternEnumType
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirForInExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.patterns.CfirConstPattern
import org.cangnova.cangjie.cfir.patterns.CfirEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirExpressionPattern
import org.cangnova.cangjie.cfir.patterns.CfirOrPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirTuplePattern
import org.cangnova.cangjie.cfir.patterns.CfirTypePattern
import org.cangnova.cangjie.cfir.patterns.CfirVarOrEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirWildcardPattern
import org.cangnova.cangjie.cfir.resolve.isIterableForForIn
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull

/**
 * for-in 循环的模式语义检查。
 *
 * 对齐官方 `LoopExprs.cpp` 中 `Check(ctx, inPatternTy, pattern)` 之前的三项职责：
 * - 可迭代性检查（`GetIterableTy`，:95-110）：迭代对象类型未实现 Iterable 时报
 *   `EXPR_IN_FORIN_MUST_HAS_ITERATOR`，锚 iterable 表达式完整范围；类型为 null /
 *   错误类型时跳过（对齐官方 `CanSkipDiag` 门控，也覆盖语法恢复的
 *   buildErrorExpression iterable）；
 * - pattern legality 检查（复用 [CfirMatchPatternLegalityChecker.checkPattern]，
 *   包括 `sema_enum_pattern_param_size_error`），仅在元素类型有效时执行；
 * - 不可反驳性检查（`sema_forin_pattern_must_be_irrefutable`），不依赖元素类型，
 *   对齐官方 `IsIrrefutablePattern`（`TypeCheckMatchExpr.cpp`）。
 */
object CfirForInPatternChecker : CfirBasicExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirStatement) {
        val forIn = expression as? CfirForInExpression ?: return

        // 可迭代性检查（对齐官方 LoopExprs.cpp:95-110）：迭代对象类型不可迭代时报
        // EXPR_IN_FORIN_MUST_HAS_ITERATOR，锚 iterable 表达式完整范围；类型为 null/错误
        // 类型时跳过（对齐官方 CanSkipDiag 门控，也覆盖语法恢复的 buildErrorExpression iterable）。
        val iterableType = forIn.iterable.coneTypeOrNull
        if (iterableType != null && iterableType !is ConeErrorType && !iterableType.isIterableForForIn(context.session)) {
            reporter.reportOn(
                source = forIn.iterable.source,
                factory = CfirErrors.EXPR_IN_FORIN_MUST_HAS_ITERATOR,
                a = iterableType,
            )
        }

        // 元素类型由 resolver 写入 variable.returnTypeRef；合法性检查以可用的
        // 元素类型为前提（官方 probe 10：迭代对象无效时不报告 param-size 等 legality 诊断）。
        val elementType = forIn.variable.returnTypeRef.coneTypeOrNull
        if (elementType != null) {
            CfirMatchPatternLegalityChecker.checkPattern(forIn.variable.pattern, elementType)
        }

        // 不可反驳性检查不依赖元素类型（官方 probe 10：迭代对象无效时仍报告）。
        if (!forIn.variable.pattern.isIrrefutable(elementType, context)) {
            reporter.reportOn(
                source = forIn.source,
                factory = CfirErrors.FORIN_PATTERN_MUST_BE_IRREFUTABLE,
            )
        }
    }
}

/**
 * 官方 `IsIrrefutablePattern` 的本地等价实现：
 * - wildcard / var（裸名）恒不可反驳；
 * - const / type / expression / or 恒可反驳；
 * - tuple 要求所有元素按各自类型不可反驳；
 * - enum 要求期望类型确为 enum、枚举声明只有唯一构造器且所有实参不可反驳。
 *
 * 期望类型为 null 或错误类型时 enum 一律视为可反驳（官方 probe 10）。
 */
private fun CfirPattern.isIrrefutable(
    expectedType: ConeCangJieType?,
    context: CheckerContext,
): Boolean = when (this) {
    is CfirWildcardPattern,
    is CfirVarOrEnumPattern,
    -> true

    is CfirBindingPattern -> {
        val declaredType = (typeRef as? CfirResolvedTypeRef)?.coneType
        nestedPattern?.isIrrefutable(declaredType ?: expectedType, context) ?: true
    }

    is CfirTypePattern,
    is CfirConstPattern,
    is CfirExpressionPattern,
    is CfirOrPattern,
    -> false

    is CfirTuplePattern -> {
        val tupleType = expectedType as? ConeTupleType
        elements.withIndex().all { (index, element) ->
            val elementType = tupleType?.elementTypes?.getOrNull(index) ?: expectedType
            element.isIrrefutable(elementType, context)
        }
    }

    is CfirEnumPattern -> {
        val enumType = expectedType?.expandedPatternEnumType(context.session)
        val enumDeclaration = enumType?.classId?.let { classId ->
            context.session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir as? CfirEnum
        }
        enumDeclaration != null &&
            enumDeclaration.declarations.filterIsInstance<CfirEnumConstructor>().size == 1 &&
            arguments.all { argument -> argument.isIrrefutable(expectedType, context) }
    }
}

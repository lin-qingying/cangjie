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
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.expandedPatternEnumType
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
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.coneType

/**
 * 官方 `IsIrrefutablePattern`（`TypeCheckMatchExpr.cpp:468-497`）的本地等价实现。
 *
 * 官方判定只看模式自身，不看声明形态；调用点有三处：
 * - for-in 的循环变量模式（`LoopExprs.cpp:128`）；
 * - var-with-pattern 声明的不可反驳模式（`TypeCheckDecl.cpp:279`）；
 * - match 分支的 default 判定。
 *
 * - wildcard / var（裸名）恒不可反驳；
 * - const / type / expression / or 恒可反驳；
 * - tuple 要求所有元素按各自类型不可反驳；
 * - enum 要求期望类型确为 enum、枚举声明只有唯一构造器且所有实参不可反驳。
 *
 * 期望类型为 null 或错误类型时 enum 一律视为可反驳（官方 probe 10：迭代对象无效时
 * `IsIrrefutablePattern` 仍按模式形状报告）。
 */
internal fun CfirPattern.isIrrefutablePattern(
    expectedType: ConeCangJieType?,
    context: CheckerContext,
): Boolean = when (this) {
    is CfirWildcardPattern,
    is CfirVarOrEnumPattern,
    -> true

    is CfirBindingPattern -> {
        val declaredType = (typeRef as? CfirResolvedTypeRef)?.coneType
        nestedPattern?.isIrrefutablePattern(declaredType ?: expectedType, context) ?: true
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
            element.isIrrefutablePattern(elementType, context)
        }
    }

    is CfirEnumPattern -> {
        val enumType = expectedType?.expandedPatternEnumType(context.session)
        val enumDeclaration = enumType?.classId?.let { classId ->
            context.session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir as? CfirEnum
        }
        enumDeclaration != null &&
            enumDeclaration.declarations.filterIsInstance<CfirEnumConstructor>().size == 1 &&
            arguments.all { argument -> argument.isIrrefutablePattern(expectedType, context) }
    }
}

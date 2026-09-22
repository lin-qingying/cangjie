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

package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.expression.containsReportedErrorDiagnostic
import org.cangnova.cangjie.cfir.analysis.checkers.expression.isIrrefutablePattern
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull

/**
 * var-with-pattern 声明的不可反驳性检查。
 *
 * 官方 `CheckVarWithPatternDecl`（`TypeCheckDecl.cpp:270-282`）：initializer 类型不正确或
 * 模式缺失时先行返回，随后形状检查之外再独立判断一次 `IsIrrefutablePattern`——
 * 不可反驳的模式（类型模式、常量模式、多构造器 enum 模式等）不能用于变量声明初始化，
 * 报 `sema_pattern_can_not_be_assigned` 并锚定整个声明模式。
 *
 * 该判定与 for-in 循环变量的不可反驳性要求共用同一实现
 * （[isIrrefutablePattern]，官方两处调用同一个 `IsIrrefutablePattern`）。
 */
object CfirPatternDeclarationIrrefutabilityChecker : CfirPatternVariableChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirPatternVariable) {
        // 官方门控：initializer 类型不正确时不再追加模式诊断（避免级联）。
        val initializer = declaration.initializer ?: return
        if (initializer.coneTypeOrNull is ConeErrorType) return
        if (initializer.containsReportedErrorDiagnostic()) return

        // 模式自身的匹配类型：元组声明由 returnTypeRef 承载（与形状检查同一来源）。
        val patternType = declaration.returnTypeRef.coneTypeOrNull
        if (declaration.pattern.isIrrefutablePattern(patternType, context)) return

        reporter.reportOn(declaration.pattern.source, CfirErrors.PATTERN_CAN_NOT_BE_ASSIGNED)
    }
}

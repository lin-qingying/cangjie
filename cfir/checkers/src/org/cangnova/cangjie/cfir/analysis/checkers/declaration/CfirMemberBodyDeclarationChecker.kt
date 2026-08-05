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
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.name.Name

/**
 * 类成员缺失函数体/访问器检查。
 *
 * 对齐官方 `DeclAttributeChecker::CheckAttributesForPropAndFuncDeclInClass`：
 * class 体内无 body 的函数/属性会先成为 abstract，随后在 static 成员或非 abstract class 中报
 * `sema_missing_func_body`，诊断位置为声明起始 token 的首字符。
 */
object CfirMemberBodyDeclarationChecker : CfirDeclarationChecker<CfirMemberDeclaration>() {
    /**
     * 检查 class 直接成员是否缺少必需的函数体或属性访问器实现。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirMemberDeclaration) {
        val member = declaration.bodyRequiredMemberInfo() ?: return
        val owner = context.findClosestDeclaration<CfirClass>()
            ?: return

        if (!member.status.isAbstract) return
        val invalidAbstract =
            member.status.isStatic && !member.status.isForeign ||
                    !owner.status.isAbstract && !owner.status.isForeign
        if (!invalidAbstract) return

        reporter.reportOn(
            source = declaration.source?.firstCharacterDiagnosticSource(),
            factory = CfirErrors.MISSING_FUNC_BODY,
            a = member.kind,
            b = member.name,
        )
    }

    /**
     * 将需要函数体/访问器的成员声明转换为统一检查数据。
     */
    private fun CfirMemberDeclaration.bodyRequiredMemberInfo(): BodyRequiredMemberInfo? {
        return when (this) {
            is CfirNamedFunction -> BodyRequiredMemberInfo(
                status = status,
                kind = "function",
                name = name,
            )

            is CfirProperty -> BodyRequiredMemberInfo(
                status = status,
                kind = "property",
                name = name,
            )

            else -> null
        }
    }

    /**
     * 缺失 body 检查所需的成员摘要。
     *
     * @property status 成员声明状态。
     * @property kind 诊断展示的成员种类。
     * @property name 诊断展示的成员名称。
     */
    private data class BodyRequiredMemberInfo(
        /**
         * 成员声明状态。
         */
        val status: CfirDeclarationStatus,

        /**
         * 诊断展示的成员种类。
         */
        val kind: String,

        /**
         * 诊断展示的成员名称。
         */
        val name: Name,
    )
}

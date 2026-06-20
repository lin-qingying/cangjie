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
 */

package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.modifierByToken
import org.cangnova.cangjie.cfir.analysis.checkers.realSourceModifiers
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.lexer.CjTokens

/**
 * 检查非可继承 class 中被官方语义忽略的 `open` 成员。
 *
 * Kotlin 对应入口是 `FirOpenMemberChecker`，同样作为 class checker 遍历
 * 当前类型的直接成员。仓颉官方 `DeclAttributeChecker::CheckFuncDeclAttributes`
 * 对 final class 中的显式 `open` 成员报告忽略警告；CFIR 使用项目已有的
 * `IGNORE_OPEN` 诊断名承载该官方语义。
 */
object CfirOpenMemberChecker : CfirClassLikeChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        val klass = declaration as? CfirClass ?: return
        if (klass.status.isOpen || klass.status.isAbstract) return

        for (member in klass.declarations) {
            val callable = member as? CfirCallableDeclaration ?: continue
            if (callable is CfirConstructor || callable is CfirEnumConstructor) continue
            if (callable !is CfirNamedFunction && callable !is CfirProperty) continue
            if (!callable.status.isOpen) continue
            if (callable.source?.realSourceModifiers()?.modifierByToken(CjTokens.OPEN_KEYWORD) == null) continue

            reporter.reportOn(
                source = callable.source?.firstCharacterDiagnosticSource(),
                factory = CfirErrors.IGNORE_OPEN,
            )
        }
    }
}

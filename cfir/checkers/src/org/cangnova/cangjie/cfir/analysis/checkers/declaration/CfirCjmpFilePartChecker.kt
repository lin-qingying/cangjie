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
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CjmpGate
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.cjmpSettings

/**
 * CJMP 文件级 part 规则检查器（cjc 1.1.3 `MPParserImpl::CheckCJMPModifiers`）。
 *
 * 判据是编译模式（不是语言版本）：
 * - `specific` 修饰符出现在非 specific 编译（`--common-part-cjo` 未给出）→ `parse_specific_in_non_specific_file`；
 * - `common` 修饰符出现在非 common 编译（非 CHIR 输出）→ `parse_common_in_non_common_file`。
 * 1.1.3 实测：普通编译（两者皆否）写 `common` 同样报后者；两条都锚在文件节点起点（`package` 行 1:1），
 * 诊断引擎对同位置只保留首个——故每文件只报源码顺序上首个错位声明对应的那一条。
 *
 * 锚点：`package` 声明；文件无 `package` 时退到首个错位声明（官方锚文件起点，登记差异）。
 * 版本门关闭时整族静默（由 `CfirCjmpParseRulesChecker` 报版本门诊断）。
 */
object CfirCjmpFilePartChecker : CfirFileChecker() {
    /** 文件级 part 判定消费解析完成的声明状态。 */
    override val requiresImplementation: Boolean get() = true

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFile) {
        if (!context.languageVersionSettings.supportsFeature(org.cangnova.cangjie.LanguageFeature.CommonSpecificDeclarations)) return
        val mode = context.session.cjmpSettings.mode
        val offending = declaration.allDeclarations().firstOrNull { decl ->
            val status = decl.cjmpStatus() ?: return@firstOrNull false
            status.isSpecific && mode != CfirCjmpMode.SPECIFIC || status.isCommon && mode != CfirCjmpMode.COMMON
        } ?: return
        val factory = if (offending.cjmpStatus()?.isSpecific == true && mode != CfirCjmpMode.SPECIFIC) {
            CfirErrors.PARSE_SPECIFIC_IN_NON_SPECIFIC_FILE
        } else {
            CfirErrors.PARSE_COMMON_IN_NON_COMMON_FILE
        }
        reporter.reportOn(
            source = declaration.packageDirective.source ?: offending.source,
            factory = factory,
        )
    }

    /** 文件内全部声明（含成员，深度优先、保持源码顺序）。 */
    private fun CfirFile.allDeclarations(): Sequence<CfirDeclaration> =
        declarations.asSequence().flatMap { it.selfAndMembers() }

    /** 声明自身与其成员声明。 */
    private fun CfirDeclaration.selfAndMembers(): Sequence<CfirDeclaration> = sequence {
        yield(this@selfAndMembers)
        val members: List<CfirDeclaration> = when (val self = this@selfAndMembers) {
            is CfirClassLikeDeclaration -> self.declarations
            is CfirExtend -> self.declarations
            else -> emptyList()
        }
        for (member in members) {
            yieldAll(member.selfAndMembers())
        }
    }

    /** 可修饰声明的状态载体。 */
    private fun CfirDeclaration.cjmpStatus() = (this as? CfirMemberDeclaration)?.status
}

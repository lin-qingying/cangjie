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

package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.CjmpLanguageFeature
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.supportsCjmpDeclarationModel
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * CJMP（common/specific 跨平台声明族）门禁单一入口。
 *
 * 对齐计划 D16（门禁判定单一权威 + 一行式辅助）与 §8.1 三层门判定序：
 *
 * 1. **版本门**：统一由 `supportsCjmpDeclarationModel` 判断稳定 common/specific、
 *    alpha.34 至 beta.20 的 common/platform，及 beta.20 至稳定版的预发布 specific 表面。
 *    1.0.x 下这些声明不具备语言表面语义（官方 v1.0.x parse 期即报 parse_expected_decl），
 *    本仓库解析器配置无关，故整族在 checker 层短路：只报版本门诊断（复用既有通用
 *    [org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.UNSUPPORTED_FEATURE]，与
 *    Kotlin `UNSUPPORTED_FEATURE` 先例一致），30 条 sema + 14 条 parse 族诊断零报告。
 * 2. **模式门**（cjc 1.1.3 `CheckCJMPModifiers`）：`common` 只在 common 编译、`specific` 只在 specific
 *    编译中合法，错位报文件级 `parse_common_in_non_common_file` / `parse_specific_in_non_specific_file`；
 *    模式经 session 组件 [org.cangnova.cangjie.cfir.session.cjmpSettings] 进入（D14 四段式样板），
 *    判定由 [modeAdmits] 统一负责。
 * 3. （未来）**包布局门**：cjpm common/specific package part（已登记 P0 待办）。
 *
 * 判定顺序硬约束：① 不过时不进入 ②；② 不过时不进入语义检查。
 */
internal object CjmpGate {
    /**
     * 版本门：feature 不受支持时报
     * [org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.UNSUPPORTED_FEATURE]
     * 并返回 false（调用方据此整族短路）。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun requireVersionSupport(source: AbstractCjSourceElement?): Boolean =
        context.languageVersionSettings.supportsCjmpDeclarationModel() ||
                context.requireCjmpFeatureSupport(CjmpLanguageFeature.CommonSpecificDeclarations, source, reporter)

    /** 所有 checker 内非上报型版本判断共用这一入口。 */
    fun isVersionSupported(languageVersionSettings: LanguageVersionSettings): Boolean =
        languageVersionSettings.supportsCjmpDeclarationModel()

    /**
     * 组合门（非上报型）：版本门 ∧ 模式门，仅返回判定结果，不产生诊断。
     *
     * 供"整族静默跳过"的既有检查器消费（它们本就不产出门诊断；族级门诊断由
     * `CfirCjmpFeatureSupportChecker`（版本支持门）和共享 parser（文件角色错误）
     * 分别负责。语义 checker 不再定义或报告文件角色的语法诊断。
     */
    fun isEnabled(context: CheckerContext): Boolean =
        isVersionSupported(context.languageVersionSettings) && isModeEnabled(context)

    /** 模式门（非上报型）：当前 session 是否处于 CJMP 编译模式。 */
    fun isModeEnabled(context: CheckerContext): Boolean =
        context.session.cjmpSettings.let { it.compileCommon || it.compileSpecific }

    /**
     * 模式门（cjc 1.1.3 `MPParserImpl::CheckCJMPModifiers`）：声明的 CJMP 修饰符是否被当前编译模式接纳。
     *
     * - `common` 仅在 common 编译（CHIR 输出）中合法，`specific` 仅在 specific 编译中合法；
     * - 不合法时由 parser 报文件角色错误，调用方跳过该声明的其余 CJMP 规则。
     * - 两个编译谓词可以同时成立，不能用单值 enum 排除其中一个角色。
     */
    fun modeAdmits(context: CheckerContext, declaration: org.cangnova.cangjie.cfir.declarations.CfirDeclaration): Boolean {
        val status = (declaration as? org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration)?.status ?: return false
        val settings = context.session.cjmpSettings
        return (settings.compileCommon || settings.compileSpecific) &&
            (!status.isCommon || settings.compileCommon) && (!status.isSpecific || settings.compileSpecific)
    }
}

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
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.session.cfirProvider

/**
 * 同名宏声明重定义检查。
 *
 * 对齐官方 `TypeCheckerImpl::PreCheckMacroRedefinition`（PreCheck.cpp:1405-1430）：
 * 收集同一作用域内的宏声明（排除编译器注入），按名字分组后：
 * - 恰好 2 个且参数个数相等 → 在两个声明上都报 `redefinition of macro 'm'`
 *   （官方允许一个 attribute 宏 + 一个 non-attribute 宏的配对，参数个数不等即放行）；
 * - 任一声明缺函数体或缺参数列表 → 官方直接整组跳过；
 * - 3 个及以上同名宏 → 官方 1.0.5 / 1.1.3 双 SDK 实测均**不报**
 *   （`size > 2` 分支在实践中被上游去重吞掉，按可观测行为对齐）。
 *
 * 注册为 fileCheckers：宏声明只能在 macro package 顶层出现，同包跨文件的
 * 同名宏同属一个作用域，declaration 级 checker 看不到兄弟声明。
 */
object CfirMacroRedefinitionChecker : CfirFileChecker() {
    /**
     * 按名字分组同包全部宏声明并施加官方重定义判据。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFile) {
        val packageFiles = context.session.cfirProvider
            .getCfirFilesByPackage(declaration.packageDirective.packageFqName)
            .sortedWith(
                compareBy(
                    { packageFile -> packageFile.sourceFile?.name ?: packageFile.name },
                    { packageFile -> packageFile.sourceFile?.path ?: packageFile.name },
                ),
            )
        val macrosByName = packageFiles
            .flatMap { file -> file.declarations.filterIsInstance<CfirMacroDeclaration>() }
            .groupBy { it.name }
        for ((_, macros) in macrosByName) {
            // 官方 invalid 分支：任一宏缺函数体或缺参数列表则整组跳过。
            if (macros.any { it.body == null || it.valueParameters.isEmpty() }) continue
            // 官方允许 attribute + non-attribute 配对；>=3 个同名宏实测不报。
            if (macros.size != 2) continue
            val (first, second) = macros
            if (first.valueParameters.size != second.valueParameters.size) continue
            for (macro in macros) {
                reporter.reportOn(
                    source = macro.macroNameDiagnosticSource(),
                    factory = CfirErrors.EXPAND_MACRO_REDEFINITION,
                    a = macro.name.asString(),
                )
            }
        }
    }
}

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
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirMainFunction
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.session.cfirProvider

/**
 * 包内多个 `main` 入口检查（官方 `sema_redefinition_entry`）。
 *
 * 官方 `CheckEntryFunc`（`TypeCheckDecl.cpp:118-133`）把每个通过检查的 `main` 登记到
 * `mainFunctionMap[curFile]`：登记表非空且当前 `main` 不在自己文件的登记集里时报告。
 * 因此**包内第一个被处理的 `main` 被保留，其余每个 `main` 各报一次**——
 * cjc 1.0.5 三 `main` 探针实测为「第 2、3 个各一条」，跨文件两 `main` 探针实测为一条。
 *
 * 实现为文件级检查器：按包枚举全部文件（与 CFIR 包级检查的既有惯例一致），
 * 取包内第一个 `main` 作为被登记者，本文件内其余 `main` 逐个报告。
 * 诊断锚定 `main` 名称 token（官方 `Diagnose(fd)` 的首字符即 `main` 关键字）。
 */
object CfirRedefinitionEntryChecker : CfirFileChecker() {
    override val requiresImplementation: Boolean get() = true

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFile) {
        val fileMains = declaration.declarations.filterIsInstance<CfirMainFunction>()
        if (fileMains.isEmpty()) return

        val packageFiles = context.session.cfirProvider
            .getCfirFilesByPackage(declaration.packageDirective.packageFqName)
        val packageMains = packageFiles.flatMap { file ->
            file.declarations.filterIsInstance<CfirMainFunction>()
        }
        if (packageMains.size <= 1) return

        // 官方登记表保留第一个被处理的 main；其余全部报 redefinition。
        val registered = packageMains.first()
        for (main in fileMains) {
            if (main === registered) continue
            reporter.reportOn(
                source = main.mainFunctionNameDiagnosticSource() ?: main.source,
                factory = CfirErrors.REDEFINITION_ENTRY,
            )
        }
    }
}

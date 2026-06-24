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

import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirMainFunction
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.checkProgramEntry
import org.cangnova.cangjie.cfir.session.languageVersionSettings

/**
 * 模块级程序入口检查。
 *
 * 对齐官方仓颉 `TypeChecker::CheckWhetherHasProgramEntry`：可执行编译目标中，
 * 当前源码包没有任何 `main` 时，在第一个源码文件起始字符上报缺失入口。
 */
fun reportMissingProgramEntryIfNeeded(
    files: Collection<CfirFile>,
    session: CfirSession,
    reporter: DiagnosticReporter,
) {
    if (!session.checkProgramEntry) return
    if (files.any(CfirFile::hasProgramEntry)) return

    val firstFile = files.firstOrNull() ?: return
    val source = firstFile.source?.firstCharacterDiagnosticSource() ?: return
    reporter.reportOn(
        source = source,
        factory = CfirErrors.MISSING_ENTRY,
        context = ProgramEntryDiagnosticContext(session, firstFile),
    )
}

/**
 * 判断文件中是否包含程序入口函数。
 */
private fun CfirFile.hasProgramEntry(): Boolean =
    declarations.any { it is CfirMainFunction }

/**
 * 程序入口缺失诊断使用的最小上下文。
 *
 * @property session 当前 CFIR session，用于读取语言版本设置。
 * @property file 诊断所在的第一个源码文件。
 */
private class ProgramEntryDiagnosticContext(
    /**
     * 当前 CFIR session。
     */
    private val session: CfirSession,

    /**
     * 用于诊断定位的源码文件。
     */
    private val file: CfirFile,
) : DiagnosticContext {
    /**
     * 当前会话的语言版本设置。
     */
    override val languageVersionSettings: LanguageVersionSettings
        get() = session.languageVersionSettings

    /**
     * 当前文件路径。
     */
    override val containingFilePath: String?
        get() = file.sourceFile?.path

    /**
     * 程序入口缺失诊断不在该上下文中做额外 suppress。
     */
    override fun isDiagnosticSuppressed(diagnostic: CjDiagnostic): Boolean = false
}

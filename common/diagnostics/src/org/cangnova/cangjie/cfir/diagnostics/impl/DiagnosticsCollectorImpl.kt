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

package org.cangnova.cangjie.cfir.diagnostics.impl

import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext

/**
 * Standard implementation of [BaseDiagnosticsCollector]
 */
class DiagnosticsCollectorImpl : BaseDiagnosticsCollector() {
    /**
     * 按文件路径存储的可变诊断列表。
     */
    private val diagnosticsByFilePathStorage: MutableMap<String?, MutableList<CjDiagnostic>> = mutableMapOf()

    /**
     * 所有已收集诊断的扁平列表。
     */
    override val diagnostics: List<CjDiagnostic>
        get() = diagnosticsByFilePath.flatMap { it.value }

    /**
     * 按文件路径分组的诊断视图。
     */
    override val diagnosticsByFilePath: Map<String?, List<CjDiagnostic>>
        get() = diagnosticsByFilePathStorage

    /**
     * 是否已收集错误级诊断。
     */
    override var hasErrors = false
        private set

    /**
     * 是否已收集会在 Werror 下视为错误的警告。
     */
    override var hasWarningsForWError = false
        private set

    /**
     * 收集未被 suppress 的诊断，并更新错误状态。
     */
    override fun report(diagnostic: CjDiagnostic?, context: DiagnosticContext) {
        if (diagnostic != null && !context.isDiagnosticSuppressed(diagnostic)) {
            diagnosticsByFilePathStorage.getOrPut(context.containingFilePath) { mutableListOf() }.run {
                add(diagnostic)
                hasErrors = hasErrors || diagnostic.severity.isError
                hasWarningsForWError = hasWarningsForWError || diagnostic.severity.isErrorWhenWError
            }
        }
    }
}

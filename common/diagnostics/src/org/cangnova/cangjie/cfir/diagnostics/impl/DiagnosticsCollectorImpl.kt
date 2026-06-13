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
    private val diagnosticsByFilePathStorage: MutableMap<String?, MutableList<CjDiagnostic>> = mutableMapOf()

    override val diagnostics: List<CjDiagnostic>
        get() = diagnosticsByFilePath.flatMap { it.value }

    override val diagnosticsByFilePath: Map<String?, List<CjDiagnostic>>
        get() = diagnosticsByFilePathStorage

    override var hasErrors = false
        private set

    override var hasWarningsForWError = false
        private set

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

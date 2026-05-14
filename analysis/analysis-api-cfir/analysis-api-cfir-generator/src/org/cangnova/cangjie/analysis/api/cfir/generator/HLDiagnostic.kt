/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.api.cfir.generator

import org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model.DiagnosticData
import org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model.DiagnosticParameter
import kotlin.reflect.KType

data class HLDiagnostic(
    val original: DiagnosticData,
    val severity: HLDiagnosticSeverity?,
    val className: String,
    val implClassName: String,
    val parameters: List<HLDiagnosticParameter>,
)

enum class HLDiagnosticSeverity {
    ERROR,
    WARNING,
}

data class HLDiagnosticList(val diagnostics: List<HLDiagnostic>)

data class HLDiagnosticParameter(
    val original: DiagnosticParameter,
    val name: String,
    val type: KType,
    val originalParameterName: String,
    val conversion: HLParameterConversion,
    val importsToAdd: List<String>
)

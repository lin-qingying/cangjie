/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.api.cfir.generator

import org.cangnova.cangjie.analysis.api.cfir.generator.rendererrs.CfirDiagnosticToCaDiagnosticConverterRenderer
import org.cangnova.cangjie.analysis.api.cfir.generator.rendererrs.CaDiagnosticClassImplementationRenderer
import org.cangnova.cangjie.analysis.api.cfir.generator.rendererrs.CaDiagnosticClassRenderer
import org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model.DiagnosticList
import org.cangnova.cangjie.generators.util.getGenerationPath
import java.nio.file.Path

object DiagnosticClassGenerator {
    fun generate(rootPath: Path, diagnosticList: DiagnosticList, packageName: String) {
        val path = getGenerationPath(rootPath.toFile(), packageName)
        CaDiagnosticClassRenderer.render(
            file = path.resolve("CaCfirDiagnostics.kt"),
            diagnosticList = diagnosticList,
            packageName = packageName,
            starImportsToAdd = emptySet(),
        )

        CaDiagnosticClassImplementationRenderer.render(
            file = path.resolve("CaCfirDiagnosticsImpl.kt"),
            diagnosticList = diagnosticList,
            packageName = packageName,
            starImportsToAdd = emptySet(),
        )

        CfirDiagnosticToCaDiagnosticConverterRenderer.render(
            file = path.resolve("CaCfirDataClassConverters.kt"),
            diagnosticList = diagnosticList,
            packageName = packageName,
            starImportsToAdd = emptySet(),
        )

        ArgumentsConverterGenerator.render(
            file = path.resolve("CaCfirArgumentsConverter.kt"),
            packageName = packageName,
        )
    }
}

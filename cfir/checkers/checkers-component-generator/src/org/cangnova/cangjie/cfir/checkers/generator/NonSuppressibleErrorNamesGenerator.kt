/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.checkers.generator

import org.cangnova.cangjie.cfir.checkers.generator.diagnostics.DIAGNOSTICS_LIST
import org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model.DeprecationDiagnosticData
import org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model.RegularDiagnosticData
import org.cangnova.cangjie.cfir.diagnostics.Severity
import org.cangnova.cangjie.cfir.tree.generator.util.writeToFileUsingSmartPrinterIfFileContentChanged
import org.cangnova.cangjie.generators.util.getGenerationPath
import org.cangnova.cangjie.generators.util.printCopyright
import org.cangnova.cangjie.generators.util.printGeneratedMessage
import java.io.File

fun generateNonSuppressibleErrorNamesFile(generationPath: File, packageName: String) {
    getGenerationPath(generationPath, packageName).resolve("CfirNonSuppressibleErrorNames.kt")
        .writeToFileUsingSmartPrinterIfFileContentChanged {
            printCopyright()
            println("package $packageName")
            println()
            printGeneratedMessage()
            println("val FIR_NON_SUPPRESSIBLE_ERROR_NAMES: Set<String> = setOf(")

            for (diagnostic in DIAGNOSTICS_LIST.allDiagnostics) {
                if (diagnostic is RegularDiagnosticData && diagnostic.severity == Severity.ERROR && !diagnostic.isSuppressible) {
                    println("    \"${diagnostic.name}\",")
                }
                if (diagnostic is DeprecationDiagnosticData) {
                    println("    \"${diagnostic.name}_ERROR\",")
                }
            }

            println(")")
        }
}


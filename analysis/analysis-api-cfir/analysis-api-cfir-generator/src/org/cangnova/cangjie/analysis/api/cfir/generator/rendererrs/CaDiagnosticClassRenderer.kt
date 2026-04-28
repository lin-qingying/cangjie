/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.api.cfir.generator.rendererrs

import org.cangnova.cangjie.analysis.api.cfir.generator.HLDiagnostic
import org.cangnova.cangjie.analysis.api.cfir.generator.HLDiagnosticList
import org.cangnova.cangjie.analysis.api.cfir.generator.HLDiagnosticParameter
import org.cangnova.cangjie.analysis.api.cfir.generator.printTypeWithShortNames
import org.cangnova.cangjie.generators.util.printBlock
import org.cangnova.cangjie.utils.SmartPrinter
import kotlin.reflect.KType

object CaDiagnosticClassRenderer : AbstractDiagnosticsDataClassRenderer() {
    override fun SmartPrinter.render(diagnosticList: HLDiagnosticList, packageName: String) {
        printHeader(packageName, diagnosticList)
        printDiagnosticClasses(diagnosticList)
    }

    private fun SmartPrinter.printDiagnosticClasses(diagnosticList: HLDiagnosticList) {
        printBlock("sealed interface CaCfirDiagnostic<PSI : PsiElement> : CaDiagnosticWithPsi<PSI>") {
            for (diagnostic in diagnosticList.diagnostics) {
                printDiagnosticClass(diagnostic, diagnosticList)
                println()
            }
        }
    }

    private fun SmartPrinter.printDiagnosticClass(diagnostic: HLDiagnostic, diagnosticList: HLDiagnosticList) {
        print("interface ${diagnostic.className} : CaCfirDiagnostic<")
        printTypeWithShortNames(diagnostic.original.psiType)
        print(">")
        printBlock {
            println("override val diagnosticClass get() = ${diagnostic.className}::class")
            printDiagnosticParameters(diagnostic, diagnosticList)
        }
    }

    private fun SmartPrinter.printDiagnosticParameters(diagnostic: HLDiagnostic, diagnosticList: HLDiagnosticList) {
        diagnostic.parameters.forEach { parameter ->
            print("val ${parameter.name}: ")
            printTypeWithShortNames(parameter.type) { type ->
                diagnosticList.containsClashingBySimpleNameType(type)
            }
            println()
        }
    }

    override fun collectImportsForDiagnosticParameterReflect(diagnosticParameter: HLDiagnosticParameter): Collection<KType> {
        return listOf(diagnosticParameter.type)
    }

    override fun collectImportsForDiagnosticParameterSimple(diagnosticParameter: HLDiagnosticParameter): Collection<String> {
        return emptyList()
    }

    override val defaultImports = listOf(
        "org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi",
        "com.intellij.psi.PsiElement",
    )
}

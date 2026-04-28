/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.api.cfir.generator.rendererrs

import org.cangnova.cangjie.analysis.api.cfir.generator.HLDiagnostic
import org.cangnova.cangjie.analysis.api.cfir.generator.HLDiagnosticList
import org.cangnova.cangjie.analysis.api.cfir.generator.HLDiagnosticParameter
import org.cangnova.cangjie.analysis.api.cfir.generator.printTypeWithShortNames
import org.cangnova.cangjie.utils.SmartPrinter
import org.cangnova.cangjie.utils.withIndent
import kotlin.reflect.KType

object CaDiagnosticClassImplementationRenderer : AbstractDiagnosticsDataClassRenderer() {
    override fun SmartPrinter.render(diagnosticList: HLDiagnosticList, packageName: String) {
        printHeader(packageName, diagnosticList)
        printDiagnosticClassesImplementation(diagnosticList)
    }

    private fun SmartPrinter.printDiagnosticClassesImplementation(diagnosticList: HLDiagnosticList) {
        for (diagnostic in diagnosticList.diagnostics) {
            printDiagnosticImplementation(diagnostic, diagnosticList)
            println()
        }
    }

    private fun SmartPrinter.printDiagnosticImplementation(diagnostic: HLDiagnostic, diagnosticList: HLDiagnosticList) {
        println("internal class ${diagnostic.implClassName}(")
        withIndent {
            printParameters(diagnostic, diagnosticList)
        }
        print(") : CaAbstractCfirDiagnostic<")
        printTypeWithShortNames(diagnostic.original.psiType)
        println(">(cfirDiagnostic, token), CaCfirDiagnostic.${diagnostic.className}")
    }

    private fun SmartPrinter.printParameters(diagnostic: HLDiagnostic, diagnosticList: HLDiagnosticList) {
        for (parameter in diagnostic.parameters) {
            printParameter(parameter, diagnosticList)
        }
        println("cfirDiagnostic: CjPsiDiagnostic,")
        println("token: CaLifetimeToken,")
    }

    private fun SmartPrinter.printParameter(parameter: HLDiagnosticParameter, diagnosticList: HLDiagnosticList) {
        print("override val ${parameter.name}: ")
        printTypeWithShortNames(parameter.type) {
            diagnosticList.containsClashingBySimpleNameType(it)
        }
        println(",")
    }

    override fun collectImportsForDiagnosticParameterReflect(diagnosticParameter: HLDiagnosticParameter): Collection<KType> {
        return listOf(diagnosticParameter.type)
    }

    override fun collectImportsForDiagnosticParameterSimple(diagnosticParameter: HLDiagnosticParameter): Collection<String> {
        return emptyList()
    }

    override val defaultImports = listOf(
        "org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic",
        "org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken",
    )
}

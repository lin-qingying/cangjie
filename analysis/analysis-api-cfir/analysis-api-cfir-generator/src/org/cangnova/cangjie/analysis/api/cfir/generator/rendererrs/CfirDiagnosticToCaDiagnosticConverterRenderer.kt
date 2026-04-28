/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.api.cfir.generator.rendererrs

import org.cangnova.cangjie.analysis.api.cfir.generator.ConversionContext
import org.cangnova.cangjie.analysis.api.cfir.generator.HLDiagnostic
import org.cangnova.cangjie.analysis.api.cfir.generator.HLDiagnosticList
import org.cangnova.cangjie.analysis.api.cfir.generator.HLDiagnosticParameter
import org.cangnova.cangjie.generators.util.printBlock
import org.cangnova.cangjie.utils.SmartPrinter
import org.cangnova.cangjie.utils.withIndent
import java.util.*
import kotlin.math.abs
import kotlin.reflect.KType

object CfirDiagnosticToCaDiagnosticConverterRenderer : AbstractDiagnosticsDataClassRenderer() {
    override fun SmartPrinter.render(diagnosticList: HLDiagnosticList, packageName: String) {
        printHeader(packageName, diagnosticList)
        printDiagnosticConverter(diagnosticList)
    }

    private fun SmartPrinter.printDiagnosticConverter(diagnosticList: HLDiagnosticList) {
        // Render diagnostics in chunks to help tooling to analyze the file faster
        val diagnosticGroups = diagnosticList.diagnostics.groupByTo(TreeMap()) { abs(it.className.hashCode()) % 200 }.entries
        val functionNameTemplate = "addConversions"
        printBlock("internal val CJ_DIAGNOSTIC_CONVERTER: CaDiagnosticConverter = CaDiagnosticConverterBuilder.buildConverter") {
            for ((index, _) in diagnosticGroups) {
                println("$functionNameTemplate$index()")
            }
        }

        for ((index, diagnostics) in diagnosticGroups) {
            println()
            printBlock("private fun CaDiagnosticConverterBuilder.$functionNameTemplate$index()") {
                for (diagnostic in diagnostics) {
                    printConverter(diagnostic)
                }
            }
        }
    }

    private fun SmartPrinter.printConverter(diagnostic: HLDiagnostic) {
        print("add(${diagnostic.original.containingObjectName}.${diagnostic.original.name}")
        if (diagnostic.severity != null) {
            print(".${diagnostic.severity.name.lowercase()}Factory")
        }
        println(") { cfirDiagnostic ->")
        withIndent {
            println("${diagnostic.implClassName}(")
            withIndent {
                printDiagnosticParameters(diagnostic)
            }
            println(")")
        }
        println("}")
    }

    private fun SmartPrinter.printDiagnosticParameters(diagnostic: HLDiagnostic) {
        printCustomParameters(diagnostic)
        println("cfirDiagnostic as CjPsiDiagnostic,")
        println("token,")
    }


    private fun SmartPrinter.printCustomParameters(diagnostic: HLDiagnostic) {
        diagnostic.parameters.forEach { parameter ->
            printParameter(parameter)
        }
    }

    private fun SmartPrinter.printParameter(parameter: HLDiagnosticParameter) {
        val expression = parameter.conversion.convertExpression(
            "cfirDiagnostic.${parameter.originalParameterName}",
            ConversionContext(currentIndentLengthInUnits, indentUnitLength)
        )
        println("$expression,")
    }

    override fun collectImportsForDiagnosticParameterReflect(diagnosticParameter: HLDiagnosticParameter): Collection<KType> {
        return emptyList()
    }

    override fun collectImportsForDiagnosticParameterSimple(diagnosticParameter: HLDiagnosticParameter): Collection<String> {
        return diagnosticParameter.importsToAdd
    }

    override val defaultImports = listOf(
        "org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic",
        "org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors",
    )
}

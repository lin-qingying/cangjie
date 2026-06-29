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

/**
 * 生成 CFIR 诊断到 Analysis API CFIR 诊断实现的转换器文件。
 */
object CfirDiagnosticToCaDiagnosticConverterRenderer : AbstractDiagnosticsDataClassRenderer() {
    /**
     * 输出文件头和诊断转换器定义。
     */
    override fun SmartPrinter.render(diagnosticList: HLDiagnosticList, packageName: String) {
        printHeader(packageName, diagnosticList)
        printDiagnosticConverter(diagnosticList)
    }

    /**
     * 输出全局诊断转换器，并将大量诊断注册拆成多个函数。
     */
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

    /**
     * 输出单个 CFIR 诊断 factory 到公开诊断实现类的注册逻辑。
     */
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

    /**
     * 输出实现类构造所需的诊断参数、原始诊断和 token。
     */
    private fun SmartPrinter.printDiagnosticParameters(diagnostic: HLDiagnostic) {
        printCustomParameters(diagnostic)
        println("cfirDiagnostic as CjPsiDiagnostic,")
        println("token,")
    }


    /**
     * 输出诊断定义中声明的自定义参数转换结果。
     */
    private fun SmartPrinter.printCustomParameters(diagnostic: HLDiagnostic) {
        diagnostic.parameters.forEach { parameter ->
            printParameter(parameter)
        }
    }

    /**
     * 输出单个参数从原始 CFIR 诊断字段到公开 API 参数的转换表达式。
     */
    private fun SmartPrinter.printParameter(parameter: HLDiagnosticParameter) {
        val expression = parameter.conversion.convertExpression(
            "cfirDiagnostic.${parameter.originalParameterName}",
            ConversionContext(currentIndentLengthInUnits, indentUnitLength)
        )
        println("$expression,")
    }

    /**
     * 转换器文件不需要通过反射类型导入参数类型。
     */
    override fun collectImportsForDiagnosticParameterReflect(diagnosticParameter: HLDiagnosticParameter): Collection<KType> {
        return emptyList()
    }

    /**
     * 转换器文件需要导入参数转换规则声明的额外符号。
     */
    override fun collectImportsForDiagnosticParameterSimple(diagnosticParameter: HLDiagnosticParameter): Collection<String> {
        return diagnosticParameter.importsToAdd
    }

    /**
     * 转换器文件固定依赖的默认导入。
     */
    override val defaultImports = listOf(
        "org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic",
        "org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors",
    )
}

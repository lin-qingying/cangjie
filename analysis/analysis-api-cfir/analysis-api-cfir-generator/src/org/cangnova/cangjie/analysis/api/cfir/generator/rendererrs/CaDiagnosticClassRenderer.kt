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

/**
 * 生成 Analysis API CFIR 诊断公开接口文件的 renderer。
 */
object CaDiagnosticClassRenderer : AbstractDiagnosticsDataClassRenderer() {
    /**
     * 输出文件头和所有公开诊断接口声明。
     */
    override fun SmartPrinter.render(diagnosticList: HLDiagnosticList, packageName: String) {
        printHeader(packageName, diagnosticList)
        printDiagnosticClasses(diagnosticList)
    }

    /**
     * 输出封装全部 CFIR 诊断接口的 sealed interface。
     */
    private fun SmartPrinter.printDiagnosticClasses(diagnosticList: HLDiagnosticList) {
        printBlock("sealed interface CaCfirDiagnostic<PSI : PsiElement> : CaDiagnosticWithPsi<PSI>") {
            for (diagnostic in diagnosticList.diagnostics) {
                printDiagnosticClass(diagnostic, diagnosticList)
                println()
            }
        }
    }

    /**
     * 输出单个公开诊断接口。
     */
    private fun SmartPrinter.printDiagnosticClass(diagnostic: HLDiagnostic, diagnosticList: HLDiagnosticList) {
        print("interface ${diagnostic.className} : CaCfirDiagnostic<")
        printTypeWithShortNames(diagnostic.original.psiType)
        print(">")
        printBlock {
            println("override val diagnosticClass get() = ${diagnostic.className}::class")
            printDiagnosticParameters(diagnostic, diagnosticList)
        }
    }

    /**
     * 输出诊断接口上的公开参数属性。
     */
    private fun SmartPrinter.printDiagnosticParameters(diagnostic: HLDiagnostic, diagnosticList: HLDiagnosticList) {
        diagnostic.parameters.forEach { parameter ->
            print("val ${parameter.name}: ")
            printTypeWithShortNames(parameter.type) { type ->
                diagnosticList.containsClashingBySimpleNameType(type)
            }
            println()
        }
    }

    /**
     * 公开接口需要导入参数的公开类型。
     */
    override fun collectImportsForDiagnosticParameterReflect(diagnosticParameter: HLDiagnosticParameter): Collection<KType> {
        return listOf(diagnosticParameter.type)
    }

    /**
     * 公开接口不需要额外的简单字符串导入。
     */
    override fun collectImportsForDiagnosticParameterSimple(diagnosticParameter: HLDiagnosticParameter): Collection<String> {
        return emptyList()
    }

    /**
     * 公开诊断接口文件固定依赖的默认导入。
     */
    override val defaultImports = listOf(
        "org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi",
        "com.intellij.psi.PsiElement",
    )
}

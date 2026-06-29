/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.api.cfir.generator.rendererrs

import org.cangnova.cangjie.analysis.api.cfir.generator.HLDiagnosticConverter
import org.cangnova.cangjie.analysis.api.cfir.generator.HLDiagnosticList
import org.cangnova.cangjie.analysis.api.cfir.generator.HLDiagnosticParameter
import org.cangnova.cangjie.analysis.api.cfir.generator.simpleName
import org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model.DiagnosticList
import org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model.DiagnosticListRenderer
import org.cangnova.cangjie.cfir.tree.generator.util.writeToFileUsingSmartPrinterIfFileContentChanged
import org.cangnova.cangjie.generators.util.printCopyright
import org.cangnova.cangjie.generators.util.printGeneratedMessage
import org.cangnova.cangjie.generators.util.printImports
import org.cangnova.cangjie.utils.SmartPrinter
import java.io.File
import kotlin.reflect.KType

/**
 * Analysis API CFIR 诊断相关数据类 renderer 的公共基类。
 *
 * 该基类统一完成原始诊断到高层模型的转换、生成文件写入、文件头和导入收集，
 * 子类只需要定义具体代码结构和参数导入策略。
 */
abstract class AbstractDiagnosticsDataClassRenderer : DiagnosticListRenderer() {
    /**
     * 将 CFIR 诊断列表转换为高层模型并写入目标文件。
     */
    override fun render(file: File, diagnosticList: DiagnosticList, packageName: String, starImportsToAdd: Set<String>) {
        val hlDiagnosticsList = HLDiagnosticConverter.convert(diagnosticList)
        file.writeToFileUsingSmartPrinterIfFileContentChanged { render(hlDiagnosticsList, packageName) }
    }

    /**
     * 收集诊断 PSI 类型、参数类型和子类默认导入后输出 import 区域。
     */
    private fun SmartPrinter.collectAndPrintImports(diagnosticList: HLDiagnosticList, packageName: String) {
        val importableTypes = diagnosticList.diagnostics.flatMap {
            buildList {
                add(it.original.psiType)
                it.parameters.forEach { parameter ->
                    addAll(collectImportsForDiagnosticParameterReflect(parameter))
                }
            }
        }

        val simpleImports = buildList {
            addAll(defaultImports)

            diagnosticList.diagnostics.forEach { diagnostic ->
                diagnostic.parameters.forEach { diagnosticParameter ->
                    addAll(collectImportsForDiagnosticParameterSimple(diagnosticParameter))
                }
            }
        }

        this.printImports(
            packageName = packageName,
            importableTypes,
            simpleImports,
            starImports = emptyList()
        )
    }

    /**
     * 输出生成文件的版权、包名、导入和生成文件标记。
     */
    protected fun SmartPrinter.printHeader(packageName: String, diagnosticList: HLDiagnosticList) {
        printCopyright()
        println("package $packageName")
        println()
        collectAndPrintImports(diagnosticList, packageName)
        printGeneratedMessage()
    }

    /**
     * 判断指定类型的简单名是否与生成的诊断接口名冲突。
     */
    protected fun HLDiagnosticList.containsClashingBySimpleNameType(type: KType): Boolean {
        return diagnostics.any { it.className == type.simpleName }
    }

    /**
     * 收集参数类型中需要通过反射类型导入工具处理的类型。
     */
    protected abstract fun collectImportsForDiagnosticParameterReflect(diagnosticParameter: HLDiagnosticParameter): Collection<KType>

    /**
     * 收集参数转换代码中需要直接加入的简单导入字符串。
     */
    protected abstract fun collectImportsForDiagnosticParameterSimple(diagnosticParameter: HLDiagnosticParameter): Collection<String>

    /**
     * 输出子类负责的完整文件正文。
     */
    protected abstract fun SmartPrinter.render(diagnosticList: HLDiagnosticList, packageName: String)

    /**
     * 子类生成文件固定需要的导入列表。
     */
    protected abstract val defaultImports: Collection<String>
}

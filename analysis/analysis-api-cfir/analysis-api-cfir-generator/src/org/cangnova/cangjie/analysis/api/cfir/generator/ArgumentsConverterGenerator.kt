/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.api.cfir.generator

import org.cangnova.cangjie.cfir.checkers.generator.printImports
import org.cangnova.cangjie.cfir.tree.generator.util.writeToFileUsingSmartPrinterIfFileContentChanged
import org.cangnova.cangjie.generators.util.printCopyright
import org.cangnova.cangjie.generators.util.printGeneratedMessage
import org.cangnova.cangjie.utils.SmartPrinter
import org.cangnova.cangjie.utils.withIndent
import java.io.File
import kotlin.collections.iterator
import kotlin.reflect.KClass
import kotlin.reflect.full.createType

/**
 * 生成的诊断参数转换函数名。
 */
private const val CONVERT_ARGUMENT = "convertArgument"

/**
 * 生成 CFIR 诊断参数到 Analysis API 公开参数的转换辅助文件。
 *
 * 该生成器为每个可转换的 CFIR 参数类型输出一个重载函数，并生成一个按运行时类型分派的入口函数。
 */
object ArgumentsConverterGenerator {
    /**
     * 生成并写入参数转换源码文件。
     *
     * @param file 目标生成文件。
     * @param packageName 生成文件所在包名。
     */
    fun render(file: File, packageName: String) {
        val convertArgumentFunctionCallConversion =
            HLFunctionCallConversion(
                "$CONVERT_ARGUMENT({0}, cfirSymbolBuilder)",
                callType = Any::class.createType(nullable = true)
            )
        val convertersMap = CfirToCjConversionCreator.getAllConverters(convertArgumentFunctionCallConversion)
        file.writeToFileUsingSmartPrinterIfFileContentChanged { generate(packageName, convertersMap) }
    }

    /**
     * 输出完整的参数转换文件内容。
     */
    private fun SmartPrinter.generate(packageName: String, convertersMap: Map<KClass<*>, HLParameterConversion>) {
        printCopyright()
        println("@file:Suppress(\"UNUSED_PARAMETER\")")
        println()
        println("package $packageName")
        println()
        collectAndPrintImports(convertersMap)
        println()
        printGeneratedMessage()

        generateDispatchingConverter(convertersMap)
        for ((type, converter) in convertersMap) {
            generateSingleConverter(type, converter)
        }
    }

    /**
     * 收集转换函数所需导入并写入 import 区域。
     */
    private fun SmartPrinter.collectAndPrintImports(convertersMap: Map<KClass<*>, HLParameterConversion>) {
        val imports = buildList {
            add("org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder")
            add("org.cangnova.cangjie.analysis.api.cfir.CaCfirSession")
            convertersMap.values.flatMapTo(this) { it.importsToAdd }
            convertersMap.keys.mapNotNullTo(this) { it.qualifiedName }
        }
        printImports(imports)
    }

    /**
     * 输出接收 `Any?` 的分派转换函数。
     *
     * 该函数先处理空值，再按 CFIR 参数的运行时类型委派到具体重载。
     */
    private fun SmartPrinter.generateDispatchingConverter(convertersMap: Map<KClass<*>, HLParameterConversion>) {
        println("internal fun $CONVERT_ARGUMENT(argument: Any?, analysisSession: CaCfirSession): Any? {")
        withIndent {
            println("return $CONVERT_ARGUMENT(argument, analysisSession.cfirSymbolBuilder)")
        }
        println("}")
        println()

        println("private fun $CONVERT_ARGUMENT(argument: Any?, cfirSymbolBuilder: CaSymbolByCfirBuilder): Any? {")
        withIndent {
            println("return when (argument) {")
            withIndent {
                println("null -> null")
                for (type in convertersMap.keys) {
                    println("is ${type.typeWithStars} -> $CONVERT_ARGUMENT(argument, cfirSymbolBuilder)")
                }
                println("else -> argument")
            }
            println("}")
        }
        println("}")
        println()
    }

    /**
     * 输出单个 CFIR 参数类型对应的重载转换函数。
     */
    private fun SmartPrinter.generateSingleConverter(type: KClass<*>, converter: HLParameterConversion) {
        println("private fun $CONVERT_ARGUMENT(argument: ${type.typeWithStars}, cfirSymbolBuilder: CaSymbolByCfirBuilder): Any? {")
        withIndent {
            println("return ${converter.convertExpression("argument",
                ConversionContext(
                    currentIndentLengthInUnits,
                    indentUnitLength
                )
            )}")
        }
        println("}")
        println()
    }

    /**
     * 带星投影的类型文本，用于生成可接收任意实参的重载签名。
     */
    private val KClass<*>.typeWithStars: String
        get() = buildString {
            append(simpleName)
            if (typeParameters.isNotEmpty()) {
                append(typeParameters.joinToString(", ", "<", ">") { "*" })
            }
        }
}

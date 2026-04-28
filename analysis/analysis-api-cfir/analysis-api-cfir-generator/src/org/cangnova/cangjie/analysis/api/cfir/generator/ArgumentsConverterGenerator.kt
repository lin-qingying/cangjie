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

private const val CONVERT_ARGUMENT = "convertArgument"

object ArgumentsConverterGenerator {
    fun render(file: File, packageName: String) {
        val convertArgumentFunctionCallConversion =
            HLFunctionCallConversion(
                "$CONVERT_ARGUMENT({0}, cfirSymbolBuilder)",
                callType = Any::class.createType(nullable = true)
            )
        val convertersMap = CfirToCjConversionCreator.getAllConverters(convertArgumentFunctionCallConversion)
        file.writeToFileUsingSmartPrinterIfFileContentChanged { generate(packageName, convertersMap) }
    }

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

    private fun SmartPrinter.collectAndPrintImports(convertersMap: Map<KClass<*>, HLParameterConversion>) {
        val imports = buildList {
            add("org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder")
            add("org.cangnova.cangjie.analysis.api.cfir.CaCfirSession")
            convertersMap.values.flatMapTo(this) { it.importsToAdd }
            convertersMap.keys.mapNotNullTo(this) { it.qualifiedName }
        }
        printImports(imports)
    }

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

    private val KClass<*>.typeWithStars: String
        get() = buildString {
            append(simpleName)
            if (typeParameters.isNotEmpty()) {
                append(typeParameters.joinToString(", ", "<", ">") { "*" })
            }
        }
}

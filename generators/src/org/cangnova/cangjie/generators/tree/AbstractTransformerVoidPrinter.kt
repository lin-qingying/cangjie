/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.generators.tree

import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.generators.tree.printer.FunctionParameter
import org.cangnova.cangjie.generators.tree.printer.ImportCollectingPrinter
import org.cangnova.cangjie.generators.tree.printer.printFunctionDeclaration
import org.cangnova.cangjie.utils.withIndent

/**
 * Void 风格 Transformer 打印器。
 *
 * 通过 `Nothing?` 作为数据参数类型，生成不依赖外部数据的 transform API。
 */
abstract class AbstractTransformerVoidPrinter<Element : AbstractElement<Element, Field, *>, Field : AbstractField<Field>>(
    printer: ImportCollectingPrinter
) : AbstractTransformerPrinter<Element, Field>(printer) {

    /**
     * Void transformer 不声明 visitor 类型参数。
     */
    final override val visitorTypeParameters: List<TypeVariable>
        get() = emptyList()

    /**
     * Void transformer 的 data 参数使用可空 [Nothing] 占位。
     */
    final override val visitorDataType: TypeRef
        get() = StandardTypes.nothing.copy(nullable = true)

    /**
     * 打印无 data 参数的 transform 方法，并生成带 data 参数签名到无 data 版本的桥接。
     */
    override fun printMethodsForElement(element: Element) {
        printer.run {
            val elementParameterName = element.visitorParameterName
            val dataParameter = FunctionParameter("data", visitorDataType)
            val methodName = "transform" + element.name
            if (element.isRootElement) {
                println()
                val elementTP = TypeVariable("E", listOf(element))
                printFunctionDeclaration(
                    name = methodName,
                    parameters = listOf(FunctionParameter(elementParameterName, elementTP)),
                    returnType = elementTP,
                    typeParameters = listOf(elementTP),
                    modality = Modality.ABSTRACT,
                )
                println()
                println()
                printFunctionDeclaration(
                    name = methodName,
                    parameters = listOf(FunctionParameter(elementParameterName, elementTP), dataParameter),
                    returnType = elementTP,
                    typeParameters = listOf(elementTP),
                    modality = Modality.FINAL,
                    override = true,
                )
            } else {
                val parentInVisitor = parentInVisitor(element) ?: return
                val returnType = visitMethodReturnType(element)
                println()
                printFunctionDeclaration(
                    name = methodName,
                    parameters = listOf(FunctionParameter(elementParameterName, element)),
                    returnType = returnType,
                    modality = Modality.OPEN,
                )
                println(" =")
                withIndent {
                    println("transform", parentInVisitor.name, "(", elementParameterName, ")")
                }
                println()
                printFunctionDeclaration(
                    name = methodName,
                    parameters = listOf(FunctionParameter(elementParameterName, element), dataParameter),
                    returnType = returnType,
                    modality = Modality.FINAL,
                    override = true,
                )
            }
            println(" =")
            withIndent {
                println(methodName, "(", elementParameterName, ")")
            }
        }
    }
}

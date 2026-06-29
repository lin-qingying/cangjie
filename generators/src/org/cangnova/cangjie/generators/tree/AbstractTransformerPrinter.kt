

package org.cangnova.cangjie.generators.tree

import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.generators.tree.printer.FunctionParameter
import org.cangnova.cangjie.generators.tree.printer.ImportCollectingPrinter
import org.cangnova.cangjie.generators.tree.printer.printFunctionDeclaration
import org.cangnova.cangjie.generators.tree.printer.printFunctionWithBlockBody
import org.cangnova.cangjie.generators.util.printBlock

/**
 * 抽象 Transformer 打印器。
 *
 * 为元素生成 `transform*` 方法及与 `visit*` 方法的桥接逻辑。
 */
abstract class AbstractTransformerPrinter<Element : AbstractElement<Element, Field, *>, Field : AbstractField<Field>>(
    printer: ImportCollectingPrinter,
) : AbstractVisitorPrinter<Element, Field>(printer) {

    /**
     * transformer 的 visit 方法返回当前元素对应的 transformer 结果类型。
     */
    override fun visitMethodReturnType(element: Element) = element.transformerClass

    /**
     * 打印指定元素的 transform 方法以及桥接到 visitor 方法的 final override。
     */
    override fun printMethodsForElement(element: Element) {
        printer.run {
            println()
            val elementParameterName = element.visitorParameterName
            if (element.isRootElement) {
                val elementTP = TypeVariable("E", listOf(element))
                printFunctionDeclaration(
                    name = "transformElement",
                    parameters = listOf(
                        FunctionParameter(elementParameterName, elementTP),
                        FunctionParameter("data", dataTypeVariable)
                    ),
                    returnType = elementTP,
                    typeParameters = listOf(elementTP),
                    modality = Modality.ABSTRACT,
                )
                println()
            } else {
                val parentInVisitor = parentInVisitor(element) ?: return
                printFunctionWithBlockBody(
                    name = "transform" + element.name,
                    parameters = listOf(
                        FunctionParameter(elementParameterName, element.withSelfArgs()),
                        FunctionParameter("data", dataTypeVariable)
                    ),
                    returnType = visitMethodReturnType(element),
                    typeParameters = element.params,
                    modality = Modality.OPEN,
                ) {
                    println("return transform", parentInVisitor.name, "(", elementParameterName, ", data)")
                }
            }
            println()
            printVisitMethodDeclaration(
                element = element,
                modality = Modality.FINAL,
                override = true,
            )
            printBlock {
                println(
                    "return transform",
                    element.name,
                    "(",
                    element.visitorParameterName,
                    ", ",
                    "data)"
                )
            }
        }
    }
}

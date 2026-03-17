/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.generators.tree

import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.generators.tree.imports.ImportCollecting
import org.cangnova.cangjie.generators.tree.printer.*
import org.cangnova.cangjie.generators.util.printBlock
import org.cangnova.cangjie.utils.withIndent

abstract class AbstractVisitorPrinter<Element : AbstractElement<Element, Field, *>, Field : AbstractField<Field>>(
    val printer: ImportCollectingPrinter,
) {

    /**
     * 要生成的 Visitor 类型。
     */
    abstract val visitorType: ClassRef<*>

    protected open val annotations: List<Annotation>
        get() = emptyList()

    open val implementationKind: ImplementationKind
        get() = when (visitorType.kind) {
            TypeKind.Class -> ImplementationKind.AbstractClass
            TypeKind.Interface -> ImplementationKind.Interface
        }

    open val constructorParameters: List<PrimaryConstructorParameter>
        get() = emptyList()

    open val optIns: List<ClassRef<*>>
        get() = emptyList()

    /**
     * Visitor 的结果类型参数；所有访问方法均返回该类型。
     */
    protected val resultTypeVariable = TypeVariable("R", emptyList(), Variance.OUT_VARIANCE)

    /**
     * Visitor 的数据类型参数；所有访问方法均接收该类型参数。
     */
    protected val dataTypeVariable = TypeVariable("D", emptyList(), Variance.IN_VARIANCE)

    /**
     * Visitor 类的类型参数列表。
     * Void 版本通常无类型参数，常规版本通常包含 [resultTypeVariable] 与 [dataTypeVariable]。
     */
    abstract val visitorTypeParameters: List<TypeVariable>

    abstract val visitorDataType: TypeRef

    abstract fun visitMethodReturnType(element: Element): TypeRef

    /**
     * Visitor 类的父类型列表。
     */
    abstract val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>>

    /**
     * 若为 `true`，泛型元素的 visitor 方法会保留对应类型参数；
     * 否则会将泛型参数替换为 `*`。
     */
    open val allowTypeParametersInVisitorMethods: Boolean
        get() = false

    /**
     * 自定义当前 [element] 未被覆盖时默认委托到哪个父元素访问方法。
     *
     * 若返回 `null`，则该元素在本 visitor 中不会生成覆盖方法（根元素除外）。
     */
    open fun parentInVisitor(element: Element): Element? = element.parentInVisitor

    open fun skipElement(element: Element): Boolean = false

    /**
     * 输出单个 visitor 方法声明（不含方法体）。
     */
    protected fun ImportCollectingPrinter.printVisitMethodDeclaration(
        element: Element,
        hasDataParameter: Boolean = true,
        modality: Modality? = null,
        override: Boolean = false,
        returnType: TypeRef = visitMethodReturnType(element),
    ) {
        val visitorParameterType = ElementRef(
            element,
            element.params.associateWith { if (allowTypeParametersInVisitorMethods) it else TypeRef.Star }
        )
        val parameters = buildList {
            add(FunctionParameter(element.visitorParameterName, visitorParameterType))
            if (hasDataParameter) add(FunctionParameter("data", visitorDataType))
        }
        printFunctionDeclaration(
            name = element.visitFunctionName,
            parameters = parameters,
            returnType = returnType,
            typeParameters = if (allowTypeParametersInVisitorMethods) {
                element.params
            } else {
                emptyList()
            },
            modality = modality,
            override = override,
        )
    }

    protected fun printMethodDeclarationForElement(element: Element, modality: Modality? = null, override: Boolean) {
        printer.run {
            println()
            printVisitMethodDeclaration(
                element,
                modality = modality,
                override = override
            )
        }
    }

    protected open fun printMethodsForElement(element: Element) {
        printer.run {
            val parentInVisitor = parentInVisitor(element)
            if (parentInVisitor == null && !element.isRootElement) return
            printMethodDeclarationForElement(
                element,
                modality = when {
                    visitorSuperTypes.isEmpty() && parentInVisitor == null && visitorType.kind == TypeKind.Class -> Modality.ABSTRACT
                    visitorSuperTypes.isEmpty() && parentInVisitor != null && visitorType.kind == TypeKind.Class -> Modality.OPEN
                    else -> null
                },
                override = parentInVisitor != null && visitorSuperTypes.isNotEmpty(),
            )
            if (parentInVisitor != null) {
                println(" =")
                withIndent {
                    print(parentInVisitor.visitFunctionName, "(", element.visitorParameterName, ", data)")
                }
            }
            println()
        }
    }

    protected open fun ImportCollectingPrinter.printAdditionalMethods() {
    }

    protected open val ImportCollecting.classKDoc: String
        get() = ""

    open fun printVisitor(elements: List<Element>) {
        val visitorType = this.visitorType
        printer.run {
            printKDoc(
                buildString {
                    val classKDoc = classKDoc
                    if (classKDoc.isNotBlank()) {
                        append(classKDoc.trim())
                        appendLine()
                        appendLine()
                    }
                    append("Auto-generated by [${this@AbstractVisitorPrinter::class.qualifiedName}]")
                }
            )
            for (annotation in annotations) {
                printAnnotation(annotation)
            }
            optIns.forEach { println("@OptIn(", it.render(), "::class)") }
            print(implementationKind.title, " ")
            print(visitorType.simpleName, visitorTypeParameters.typeParameters())
            if (constructorParameters.isNotEmpty()) {
                println("(")
                withIndent {
                    for (parameter in constructorParameters) {
                        printPropertyDeclaration(
                            name = parameter.name,
                            type = parameter.type,
                            kind = parameter.kind,
                            inConstructor = true,
                            visibility = parameter.visibility,
                            initializer = parameter.defaultValue
                        )
                        println()
                    }
                }
                print(")")
            }
            printInheritanceClause(visitorSuperTypes)
            print(visitorTypeParameters.multipleUpperBoundsList())
            printBlock {
                printAdditionalMethods()
                for (element in elements) {
                    if (skipElement(element)) continue
                    printMethodsForElement(element)
                }
            }
        }
    }
}

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

/**
 * 打印树 visitor 或 transformer 访问器类型的公共基类。
 *
 * 该类统一生成 visitor 类型声明、类型参数、继承子句、构造参数以及每个元素对应的访问方法。
 */
abstract class AbstractVisitorPrinter<Element : AbstractElement<Element, Field, *>, Field : AbstractField<Field>>(
    /**
     * 带导入收集能力的目标源码打印器。
     */
    val printer: ImportCollectingPrinter,
) {

    /**
     * 要生成的 Visitor 类型。
     */
    abstract val visitorType: ClassRef<*>

    /**
     * 打印到 visitor 类型声明前的注解。
     */
    protected open val annotations: List<Annotation>
        get() = emptyList()

    /**
     * visitor 类型本身应生成的实现种类。
     */
    open val implementationKind: ImplementationKind
        get() = when (visitorType.kind) {
            TypeKind.Class -> ImplementationKind.AbstractClass
            TypeKind.Interface -> ImplementationKind.Interface
        }

    /**
     * visitor 主构造函数参数列表。
     */
    open val constructorParameters: List<PrimaryConstructorParameter>
        get() = emptyList()

    /**
     * visitor 类型声明前需要打印的 opt-in 注解。
     */
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

    /**
     * visitor 方法中 `data` 参数的类型。
     */
    abstract val visitorDataType: TypeRef

    /**
     * 返回指定元素的 visit 方法返回类型。
     */
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

    /**
     * 判断指定元素是否跳过 visitor 方法生成。
     */
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

    /**
     * 输出指定元素的 visitor 方法声明并添加前导空行。
     */
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

    /**
     * 打印指定元素对应的 visitor 方法。
     *
     * 默认实现会在存在 visitor 父元素时委托给父元素访问方法，否则根据 visitor 类型决定是否生成抽象方法。
     */
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

    /**
     * 打印 visitor 类型体内的额外方法。
     */
    protected open fun ImportCollectingPrinter.printAdditionalMethods() {
    }

    /**
     * visitor 类型声明前的额外 KDoc 文本。
     */
    protected open val ImportCollecting.classKDoc: String
        get() = ""

    /**
     * 打印完整 visitor 类型。
     */
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

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.generators.tree

import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.generators.tree.printer.ImportCollectingPrinter
import org.cangnova.cangjie.generators.util.printBlock

/**
 * Void 风格 Visitor 打印器。
 *
 * 生成 `visit*` 无数据版本，并适配到带 data 参数的方法签名。
 */
abstract class AbstractVisitorVoidPrinter<Element, Field>(
    printer: ImportCollectingPrinter,
) : AbstractVisitorPrinter<Element, Field>(printer)
        where Element : AbstractElement<Element, Field, *>,
              Field : AbstractField<Field> {

    /**
     * Void visitor 不声明 result/data 类型参数。
     */
    final override val visitorTypeParameters: List<TypeVariable>
        get() = emptyList()

    /**
     * Void visitor 的 data 参数使用可空 [Nothing] 占位。
     */
    final override val visitorDataType: TypeRef
        get() = StandardTypes.nothing.copy(nullable = true)

    /**
     * Void visitor 的访问方法返回 [Unit]。
     */
    override fun visitMethodReturnType(element: Element) = StandardTypes.unit

    /**
     * Void visitor 继承的带 data 参数 visitor 基类。
     */
    abstract val visitorSuperClass: ClassRef<PositionTypeParameterRef>

    /**
     * Void visitor 的父类型列表。
     */
    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>>
        get() = listOf(visitorSuperClass.withArgs(StandardTypes.unit, visitorDataType))

    /**
     * 根元素的无 data 访问方法是否生成为抽象方法。
     */
    abstract val useAbstractMethodForRootElement: Boolean

    /**
     * 覆盖带 data 参数的 visit 方法时是否标记为 final。
     */
    abstract val overriddenVisitMethodsAreFinal: Boolean

    /**
     * 判断指定元素的无 data visit 方法是否需要覆盖父类型方法。
     */
    protected open fun shouldOverrideMethodWithNoDataParameter(element: Element): Boolean = false

    /**
     * 打印无 data visit 方法，以及带 data 参数方法到无 data 方法的桥接。
     */
    final override fun printMethodsForElement(element: Element) {
        val parentInVisitor = parentInVisitor(element)
        if (!element.isRootElement && parentInVisitor == null) return

        val isAbstractVisitRootElementMethod = element.isRootElement && useAbstractMethodForRootElement

        printMethodDeclarationForElement(
            element,
            modality = Modality.FINAL.takeIf { overriddenVisitMethodsAreFinal },
            override = true,
        )

        fun ImportCollectingPrinter.printBody(parentInVisitor: Element?) {
            printBlock {
                if (parentInVisitor != null) {
                    println(parentInVisitor.visitFunctionName, "(", element.visitorParameterName, ")")
                }
            }
        }

        printer.run {
            printBody(element)
            println()
            val override = shouldOverrideMethodWithNoDataParameter(element)
            printVisitMethodDeclaration(
                element,
                hasDataParameter = false,
                modality = when {
                    isAbstractVisitRootElementMethod && visitorType.kind == TypeKind.Class -> Modality.ABSTRACT
                    !override && !isAbstractVisitRootElementMethod && visitorType.kind == TypeKind.Class -> Modality.OPEN
                    else -> null
                },
                override = override,
            )
            if (isAbstractVisitRootElementMethod) {
                println()
            } else {
                printBody(parentInVisitor)
            }
        }
    }
}

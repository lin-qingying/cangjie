/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.generators.tree

import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.generators.tree.imports.ImportCollecting
import org.cangnova.cangjie.generators.tree.imports.ImportCollector
import org.cangnova.cangjie.generators.tree.printer.*
import org.cangnova.cangjie.utils.SmartPrinter
import org.cangnova.cangjie.utils.withIndent

/**
 * 打印树元素接口或抽象类的公共基类。
 *
 * 该打印器负责输出元素 KDoc、类型声明、继承子句、字段属性以及由具体树生成器补充的额外方法。
 */
abstract class AbstractElementPrinter<Element : AbstractElement<Element, Field, *>, Field : AbstractField<Field>>(
    /**
     * 带导入收集能力的目标源码打印器。
     */
    private val printer: ImportCollectingPrinter,
) {

    /**
     * 创建用于打印元素字段的字段打印器。
     */
    protected abstract fun makeFieldPrinter(printer: ImportCollectingPrinter): AbstractFieldPrinter<Field>

    /**
     * 打印当前元素除字段以外的额外成员方法。
     */
    protected abstract fun ImportCollectingPrinter.printAdditionalMethods(element: Element)

    /**
     * 字段之间是否额外插入空行。
     */
    protected open val separateFieldsWithBlankLine: Boolean
        get() = false

    /**
     * 返回当前元素需要打印到类型体中的字段集合。
     */
    protected open fun filterFields(element: Element): Collection<Field> = element.allFields

    /**
     * 生成当前元素类型声明前的 KDoc 文本。
     */
    protected open fun ImportCollecting.elementKDoc(element: Element): String = element.extendedKDoc()

    /**
     * 打印单个元素类型声明。
     */
    fun printElement(element: Element) {
        printer.run {
            val kind = element.kind ?: error("Expected non-null element kind")

            printKDoc(elementKDoc(element))
            print(kind.title, " ", element.typeName)
            print(element.params.typeParameters())
            printInheritanceClause(element.parentRefs)
            print(element.params.multipleUpperBoundsList())

            val printer = SmartPrinter(StringBuilder())
            this@AbstractElementPrinter.printer.withNewPrinter(printer) {
                val fieldPrinter = makeFieldPrinter(this)
                withIndent {
                    for (field in filterFields(element)) {
                        if (field.isParameter) continue
                        if (field.isFinal && field.isOverride) {
                            continue
                        }
                        if (separateFieldsWithBlankLine) println()
                        fieldPrinter.printField(
                            field,
                            inImplementation = false,
                            override = field.isOverride,
                            modality = Modality.ABSTRACT.takeIf { !field.isFinal && !kind.isInterface },
                        )
                    }
                    printAdditionalMethods(element)
                }
            }
            val body = printer.toString()

            if (body.isNotEmpty()) {
                println(" {")
                print(body.trimStart('\n'))
                print("}")
            }
            println()
            addAllImports(element.additionalImports)
        }
    }
}

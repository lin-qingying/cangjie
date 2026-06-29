/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.generators.tree

import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.generators.tree.imports.ImportCollecting
import org.cangnova.cangjie.generators.tree.printer.*
import org.cangnova.cangjie.utils.SmartPrinter
import org.cangnova.cangjie.utils.ifNotEmpty
import org.cangnova.cangjie.utils.withIndent

/**
 * 打印树元素具体实现类的公共基类。
 *
 * 该打印器负责输出实现类 KDoc、opt-in、可见性、主构造函数、父类型调用、类体字段和额外方法。
 */
abstract class AbstractImplementationPrinter<Implementation, Element, Field>(
    /**
     * 带导入收集能力的目标源码打印器。
     */
    private val printer: ImportCollectingPrinter,
)
        where Implementation : AbstractImplementation<Implementation, Element, Field>,
              Element : AbstractElement<Element, Field, Implementation>,
              Field : AbstractField<Field> {


    /**
     * 保护生成实现细节 API 的 opt-in 注解。
     */
    protected abstract val implementationOptInAnnotation: ClassRef<*>

    /**
     * 返回当前实现类需要继承的纯抽象元素类型。
     */
    protected abstract fun getPureAbstractElementType(implementation: Implementation): ClassRef<*>

    /**
     * 字段之间是否额外插入空行。
     */
    protected open val separateFieldsWithBlankLine: Boolean
        get() = false

    /**
     * 为父类构造函数调用生成参数表达式列表。
     */
    protected open fun ImportCollecting.parentConstructorArguments(implementation: Implementation): List<String> =
        emptyList()

    /**
     * 创建用于打印实现类字段的字段打印器。
     */
    protected abstract fun makeFieldPrinter(printer: ImportCollectingPrinter): AbstractFieldPrinter<Field>

    /**
     * 打印实现类除字段以外的额外成员方法。
     */
    protected open fun ImportCollectingPrinter.printAdditionalMethods(implementation: Implementation) {
    }

    /**
     * 返回需要插入到主构造函数开头的额外参数。
     */
    protected open fun additionalConstructorParameters(implementation: Implementation): List<FunctionParameter> = emptyList()

    /**
     * 打印单个实现类声明。
     */
    fun printImplementation(implementation: Implementation) {
        printer.run {
            printKDoc(implementation.kDoc)
            buildSet {
                if (implementation.requiresOptIn) {
                    add(implementationOptInAnnotation)
                }

                for (field in implementation.fieldsInConstructor) {
                    field.optInAnnotation?.let {
                        add(it)
                    }
                }
            }.ifNotEmpty {
                println("@OptIn(", joinToString { "${it.render()}::class" }, ")")
            }

            if (!implementation.isPublic) {
                print("internal ")
            }

            val kind = implementation.kind ?: error("Expected non-null element kind")
            print("${kind.title} ${implementation.typeName}")
            print(implementation.element.params.typeParameters())

            val isInterface = kind == ImplementationKind.Interface || kind == ImplementationKind.SealedInterface
            val isAbstract = kind == ImplementationKind.AbstractClass || kind == ImplementationKind.SealedClass

            val fieldPrinter = makeFieldPrinter(this)

            val additionalConstructorParameters = additionalConstructorParameters(implementation)
            if (!isInterface &&
                !isAbstract &&
                (implementation.fieldsInConstructor.isNotEmpty() || additionalConstructorParameters.isNotEmpty())
            ) {
                var printConstructor = false
                if (implementation.isPublic && implementation.isConstructorPublic && implementation.putImplementationOptInInConstructor) {
                    print(" @", implementationOptInAnnotation.render())
                    printConstructor = true
                }
                if (implementation.isPublic && !implementation.isConstructorPublic) {
                    print(" internal")
                    printConstructor = true
                }

                if (printConstructor) {
                    print(" constructor")
                }

                println("(")
                withIndent {
                    for (parameter in additionalConstructorParameters) {
                        println(parameter.render(this), ",")
                    }
                    implementation.fieldsInConstructor
                        .reorderFieldsIfNecessary(implementation.constructorParameterOrderOverride)
                        .forEachIndexed { _, field ->
                            if (field.isParameter) {
                                print(field.name, ": ", field.typeRef.render())
                                println(",")
                            } else if (!field.isFinal) {
                                fieldPrinter.printField(field, inImplementation = true, override = true, inConstructor = true)
                            }
                        }
                }
                print(")")
            }

            val parentRefs = listOfNotNull(getPureAbstractElementType(implementation).takeIf { implementation.needPureAbstractElement }) +
                    implementation.allParents.map { it.withSelfArgs() }
            printInheritanceClause(parentRefs, parentConstructorArguments(implementation))
            val printer = SmartPrinter(StringBuilder())
            withNewPrinter(printer) {
                val bodyFieldPrinter = makeFieldPrinter(this)
                withIndent {
                    val fields = if (isInterface || isAbstract) implementation.allFields
                    else implementation.fieldsInBody
                    fields.forEachIndexed { index, field ->
                        if (index > 0 && separateFieldsWithBlankLine) {
                            println()
                        }
                        bodyFieldPrinter.printField(
                            field,
                            inImplementation = true,
                            override = true,
                            modality = Modality.ABSTRACT.takeIf { isAbstract },
                        )
                    }

                    printAdditionalMethods(implementation)
                }
            }
            val body = printer.toString()
            if (body.isNotEmpty()) {
                println(" {")
                print(body)
                println("}")
            } else {
                println()
            }
            addAllImports(implementation.additionalImports)
        }
    }
}

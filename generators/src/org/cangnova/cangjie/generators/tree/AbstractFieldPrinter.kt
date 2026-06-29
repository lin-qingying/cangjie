/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.generators.tree

import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.generators.tree.printer.ImportCollectingPrinter
import org.cangnova.cangjie.generators.tree.printer.VariableKind
import org.cangnova.cangjie.generators.tree.printer.printPropertyDeclaration
import org.cangnova.cangjie.generators.util.printBlock
import org.cangnova.cangjie.utils.withIndent

/**
 * 打印树模型字段属性或构造参数的公共基类。
 *
 * 具体树生成器可覆盖类型选择、可变性和 opt-in 包裹策略，以适配接口、实现类和构建器中的不同字段形态。
 */
abstract class AbstractFieldPrinter<Field : AbstractField<*>>(
    /**
     * 带导入收集能力的目标源码打印器。
     */
    private val printer: ImportCollectingPrinter,
) {

    /**
     * 强制把字段打印为 `var`。
     *
     * 默认遵循字段自身可变性，具体生成器可在实现类需要可写属性时覆盖。
     */
    protected open fun forceMutable(field: Field): Boolean = false

    /**
     * 返回字段在当前位置实际打印的类型。
     *
     * 例如实现类中的列表字段可以从只读 [List] 改为 [MutableList]。
     */
    protected open fun actualTypeOfField(field: Field): TypeRefWithNullability = field.typeRef

    /**
     * 是否在默认值 getter 场景中包裹 opt-in 注解。
     */
    protected open val wrapOptInAnnotations: Boolean
        get() = false

    /**
     * 打印字段声明。
     *
     * @param field 要打印的字段模型。
     * @param inImplementation 是否处于实现类生成阶段。
     * @param override 是否添加 `override` 修饰。
     * @param inConstructor 是否作为主构造函数参数打印。
     * @param modality 需要显式输出的成员可见/抽象形态。
     */
    fun printField(
        field: Field,
        inImplementation: Boolean,
        override: Boolean,
        inConstructor: Boolean = false,
        modality: Modality? = null,
    ) {
        printer.run {
            val defaultValue = if (inImplementation)
                field.implementationDefaultStrategy as? AbstractField.ImplementationDefaultStrategy.DefaultValue
            else null
            val shouldBeParameter = inConstructor && field.customSetter != null
            printPropertyDeclaration(
                name = field.name,
                type = actualTypeOfField(field),
                kind = when {
                    shouldBeParameter -> VariableKind.PARAMETER
                    forceMutable(field) || field.isFinal && field.isMutable -> VariableKind.VAR
                    else -> VariableKind.VAL
                },
                inConstructor = inConstructor,
                visibility = field.visibility,
                modality = modality.takeUnless { shouldBeParameter },
                override = override && !shouldBeParameter,
                isLateinit = !shouldBeParameter && (inImplementation || field.isFinal) && field.implementationDefaultStrategy is AbstractField.ImplementationDefaultStrategy.Lateinit,
                isVolatile = !shouldBeParameter && (inImplementation || field.isFinal) && field.isVolatile,
                optInAnnotation = field.optInAnnotation,
                printOptInWrapped = wrapOptInAnnotations && defaultValue != null,
                deprecation = field.deprecation,
                kDoc = field.kDoc.takeIf { !inImplementation },
                initializer = when {
                    defaultValue?.withGetter == true -> null
                    defaultValue != null -> defaultValue.defaultValue
                    !inConstructor && field.customSetter != null -> field.name
                    else -> null
                },
                additionalAnnotations = if (!shouldBeParameter && (inImplementation || field.isFinal)) field.additionalAnnotations else emptyList(),
            )
            println()

            if (defaultValue != null && defaultValue.withGetter) {
                withIndent {
                    println("get() = ${defaultValue.defaultValue}")
                }
            }

            if (inImplementation && !inConstructor) {
                field.customSetter?.let {
                    withIndent {
                        print("set(value)")
                        printBlock {
                            printlnMultiLine(it)
                        }
                    }
                }
            }
        }
    }
}

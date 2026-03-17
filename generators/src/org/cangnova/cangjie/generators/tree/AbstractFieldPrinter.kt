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

abstract class AbstractFieldPrinter<Field : AbstractField<*>>(
    private val printer: ImportCollectingPrinter,
) {

    /**
     * 允许将字段强制生成为 `var`，而不是 `val`。
     */
    protected open fun forceMutable(field: Field): Boolean = false

    /**
     * 允许覆盖 [field] 的输出类型。
     * 例如在实现类中，列表字段可使用 [MutableList] 而不是 [List]。
     */
    protected open fun actualTypeOfField(field: Field): TypeRefWithNullability = field.typeRef

    protected open val wrapOptInAnnotations: Boolean
        get() = false

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

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.generators.tree

import org.cangnova.cangjie.generators.tree.printer.FunctionParameter
import org.cangnova.cangjie.generators.tree.printer.ImportCollectingPrinter
import org.cangnova.cangjie.generators.tree.printer.printFunctionWithBlockBody
import org.cangnova.cangjie.generators.util.printBlock
import org.cangnova.cangjie.utils.withIndent

/**
 * 打印树 Builder DSL 类型和顶层构建函数的公共基类。
 *
 * 该打印器负责生成中间 Builder、叶子 Builder、`build*` 函数以及可选的 copy 构建函数。
 */
abstract class AbstractBuilderPrinter<Element, Implementation, ElementField>(
    /**
     * 带导入收集能力的目标源码打印器。
     */
    val printer: ImportCollectingPrinter,
)
        where Element : AbstractElement<Element, ElementField, Implementation>,
              Implementation : AbstractImplementation<Implementation, Element, ElementField>,
              ElementField : AbstractField<ElementField> {

    /**
     * Builder 打印器共享的静态类型引用。
     */
    companion object {
        /**
         * Kotlin contracts 实验性 API 的 opt-in 注解类型。
         */
        private val experimentalContractsAnnotation =
            type("kotlin.contracts", "ExperimentalContracts", TypeKind.Class)
    }

    /**
     * 访问 implementation detail API 时需要使用的 opt-in 注解。
     */
    protected abstract val implementationDetailAnnotation: ClassRef<*>

    /**
     * 打印到 Builder 类型声明前的 DSL 标记注解。
     */
    protected abstract val builderDslAnnotation: ClassRef<*>

    /**
     * 打印实现类构造调用中的字段实参表达式。
     */
    protected open fun ImportCollectingPrinter.printFieldReferenceInImplementationConstructorCall(field: ElementField) {
        print(field.name)
    }

    /**
     * 返回字段在 Builder 中实际暴露的类型。
     *
     * 列表字段默认使用可变列表类型，普通字段使用字段自身类型。
     */
    protected open fun actualTypeOfField(field: ElementField): TypeRefWithNullability =
        if (field is ListField) StandardTypes.mutableList.withArgs(field.baseType) else field.typeRef

    /**
     * 将原始元素中的字段值复制到 copy Builder。
     */
    protected open fun copyField(field: ElementField, originalParameterName: String, copyBuilderVariableName: String) {
        printer.run {
            when {
                field is ListField -> println(
                    copyBuilderVariableName,
                    ".",
                    field.name,
                    ".addAll(",
                    originalParameterName,
                    ".",
                    field.name,
                    ")",
                )
                else -> println(copyBuilderVariableName, ".", field.name, " = ", originalParameterName, ".", field.name)
            }
        }
    }

    /**
     * 打印一个 Builder 类型及其关联的顶层 DSL 函数。
     */
    fun printBuilder(builder: Builder<ElementField, Element>) {
        printer.run {
            addAllImports(builder.usedTypes)
            if (builder is LeafBuilder<*, *, *> && builder.allFields.isEmpty()) {
                @Suppress("UNCHECKED_CAST")
                printDslBuildFunction(builder as LeafBuilder<ElementField, Element, Implementation>, hasRequiredFields = false)
                return
            }

            println("@", builderDslAnnotation.render())
            when (builder) {
                is IntermediateBuilder -> print("${if (builder.isSealed) "sealed " else ""}interface ")
                is LeafBuilder<*, *, *> -> {
                    if (builder.isOpen) {
                        print("open ")
                    }
                    print("class ")
                }
            }
            print(builder.render())
            if (builder.parents.isNotEmpty()) {
                print(builder.parents.joinToString(separator = ", ", prefix = " : ") { it.render() })
            }
            var hasRequiredFields = false
            printBlock {
                var needNewLine = false
                for (field in builder.allFields) {
                    val (newLine, requiredFields) = printFieldInBuilder(field, builder, fieldIsUseless = false)
                    needNewLine = newLine
                    hasRequiredFields = hasRequiredFields || requiredFields
                }
                val hasBackingFields = builder.allFields.any { it.nullable }
                if (needNewLine) {
                    println()
                }
                val buildType = when (builder) {
                    is LeafBuilder<*, *, *> -> builder.implementation.element.render()
                    is IntermediateBuilder -> builder.materializedElement!!.withStarArgs().render()
                }
                if (builder is LeafBuilder<*, *, *> && builder.implementation.isPublic) {
                    println("@OptIn(", implementationDetailAnnotation.render(), "::class)")
                }
                if (builder.parents.isNotEmpty()) {
                    print("override ")
                }
                print("fun build(): ", buildType)
                if (builder is LeafBuilder<*, *, *>) {
                    printBlock {
                        println("return ${builder.implementation.render()}(")
                        withIndent {
                            for (field in builder.allFields) {
                                if (field.invisibleField) continue
                                printFieldReferenceInImplementationConstructorCall(field)
                                println(",")
                            }
                        }
                        println(")")
                    }
                    if (hasBackingFields) {
                        println()
                    }
                } else {
                    println()
                }

                if (builder is LeafBuilder<*, *, *>) {
                    if (builder.uselessFields.isNotEmpty()) {
                        println()
                        builder.uselessFields.forEachIndexed { index, field ->
                            if (index > 0) {
                                println()
                            }
                            printFieldInBuilder(field, builder, fieldIsUseless = true)
                        }
                    }
                }
            }
            if (builder is LeafBuilder<*, *, *>) {
                println()
                @Suppress("UNCHECKED_CAST")
                printDslBuildFunction(builder as LeafBuilder<ElementField, Element, Implementation>, hasRequiredFields)

                if (builder.wantsCopy) {
                    println()
                    printDslBuildCopyFunction(builder, hasRequiredFields)
                }
            }
        }
    }

    /**
     * 创建顶层构建函数的初始化 Lambda 参数。
     */
    private fun lambdaParameterForBuilderFunction(builder: Builder<ElementField, Element>, hasRequiredFields: Boolean) =
        FunctionParameter(
            name = "init",
            type = Lambda(receiver = builder, returnType = StandardTypes.unit),
            defaultValue = "{}".takeIf { !hasRequiredFields },
        )

    /**
     * 打印 `callsInPlace(init, InvocationKind.EXACTLY_ONCE)` contracts 语句块。
     */
    private fun ImportCollectingPrinter.contractCallsInPlaceExactlyOnce() {
        addStarImport("kotlin.contracts")
        print("contract")
        printBlock {
            println("callsInPlace(init, InvocationKind.EXACTLY_ONCE)")
        }
    }

    /**
     * 根据实现类或元素名生成顶层构建函数名。
     */
    private fun builderFunctionName(builder: LeafBuilder<ElementField, Element, Implementation>) =
        "build" + builder.implementation.run { name?.removePrefix(namePrefix) ?: element.name }

    /**
     * 打印 `build*` 顶层 DSL 构建函数。
     */
    private fun ImportCollectingPrinter.printDslBuildFunction(
        builder: LeafBuilder<ElementField, Element, Implementation>,
        hasRequiredFields: Boolean,
    ) {
        val isEmpty = builder.allFields.isEmpty()
        if (!isEmpty) {
            println("@OptIn(", experimentalContractsAnnotation.render(), "::class)")
        } else if (builder.implementation.isPublic) {
            println("@OptIn(", implementationDetailAnnotation.render(), "::class)")
        }
        val initParameter = if (isEmpty) null else lambdaParameterForBuilderFunction(builder, hasRequiredFields)
        printFunctionWithBlockBody(
            name = builderFunctionName(builder),
            parameters = listOfNotNull(initParameter),
            returnType = builder.implementation.element,
            typeParameters = builder.implementation.element.params,
            isInline = !isEmpty,
        ) {
            if (!isEmpty) {
                addStarImport("kotlin.contracts")
                println("contract {")
                withIndent {
                    println("callsInPlace(init, InvocationKind.EXACTLY_ONCE)")
                }
                println("}")
            }
            print("return ")
            if (isEmpty) {
                println(builder.implementation.render(), "()")
            } else {
                println(builder.render(), "().apply(init).build()")
            }
        }
    }

    /**
     * 判断字段是否需要 Builder 内部存储字段承接调用方赋值。
     */
    private fun ElementField.needBackingField(fieldIsUseless: Boolean) =
        !nullable && this !is ListField && if (fieldIsUseless) {
            implementationDefaultStrategy?.defaultValue == null
        } else {
            defaultValueInBuilder == null
        }

    /**
     * 判断字段是否应使用 `Delegates.notNull` 生成非空委托。
     */
    private fun ElementField.needNotNullDelegate(fieldIsUseless: Boolean) =
        needBackingField(fieldIsUseless) && (typeRef == StandardTypes.boolean || typeRef == StandardTypes.int)

    /**
     * 打印 Builder 中的单个字段声明。
     *
     * @return 第一项表示是否已输出需要空行分隔的字段体，第二项表示该字段是否是必填字段。
     */
    private fun ImportCollectingPrinter.printFieldInBuilder(
        field: ElementField,
        builder: Builder<ElementField, Element>,
        fieldIsUseless: Boolean,
    ): Pair<Boolean, Boolean> {
        if (
            field.implementationDefaultStrategy?.withGetter == true
            && !fieldIsUseless || field.invisibleField
        ) return false to false
        if (field is ListField) {
            @Suppress("UNCHECKED_CAST")
            printFieldListInBuilder(field as ElementField, builder, fieldIsUseless)
            return true to false
        }
        val defaultValue = if (fieldIsUseless)
            field.implementationDefaultStrategy!!.defaultValue
        else
            field.defaultValueInBuilder

        printDeprecationOnUselessFieldIfNeeded(field, builder, fieldIsUseless)
        printModifiers(builder, field, fieldIsUseless)
        print("var ${field.name}: ${field.typeRef.render()}")
        var hasRequiredFields = false
        val needNewLine = when {
            fieldIsUseless -> {
                println()
                withIndent {
                    println("get() = throw IllegalStateException()")
                    println("set(_) {")
                    withIndent {
                        println("throw IllegalStateException()")
                    }
                    println("}")
                }
                true
            }
            builder is IntermediateBuilder -> {
                println()
                false
            }
            field.needNotNullDelegate(fieldIsUseless = false) -> {
                println(" by kotlin.properties.Delegates.notNull<${field.typeRef.render()}>()")
                hasRequiredFields = true
                true
            }
            field.needBackingField(fieldIsUseless = false) -> {
                println()
                hasRequiredFields = true
                true
            }
            else -> {
                println(" = $defaultValue")
                true
            }
        }
        return needNewLine to hasRequiredFields
    }

    /**
     * 为无效字段打印隐藏级别的废弃注解。
     */
    private fun ImportCollectingPrinter.printDeprecationOnUselessFieldIfNeeded(
        field: AbstractField<*>,
        builder: Builder<ElementField, Element>,
        fieldIsUseless: Boolean,
    ) {
        if (fieldIsUseless) {
            println(
                "@Deprecated(\"Modification of '",
                field.name,
                "' has no impact for ",
                builder.typeName,
                "\", level = DeprecationLevel.HIDDEN)",
            )
        }
    }

    /**
     * 打印 Builder 中的列表字段声明。
     */
    private fun ImportCollectingPrinter.printFieldListInBuilder(
        field: ElementField,
        builder: Builder<ElementField, Element>,
        fieldIsUseless: Boolean,
    ) {
        printDeprecationOnUselessFieldIfNeeded(field, builder, fieldIsUseless)
        printModifiers(builder, field, fieldIsUseless)
        print("val ", field.name, ": ", actualTypeOfField(field).render())
        if (builder is LeafBuilder<*, *, *>) {
            print(" = mutableListOf()")
        }
        println()
    }

    /**
     * 打印 Builder 字段声明前的 `abstract`、`override`、`open` 或 `lateinit` 修饰符。
     */
    private fun ImportCollectingPrinter.printModifiers(builder: Builder<ElementField, Element>, field: AbstractField<*>, fieldIsUseless: Boolean) {
        if (builder is IntermediateBuilder) {
            print("abstract ")
        }
        if (builder.isFromParent(field)) {
            print("override ")
        } else if (builder is LeafBuilder<*, *, *> && builder.isOpen) {
            print("open ")
        }
        @Suppress("UNCHECKED_CAST")
        if (builder is LeafBuilder<*, *, *> &&
            (field as ElementField).needBackingField(fieldIsUseless) &&
            !fieldIsUseless &&
            !field.needNotNullDelegate(fieldIsUseless = false)
        ) {
            print("lateinit ")
        }
    }

    /**
     * 打印基于已有元素创建副本的 `build*Copy` 顶层 DSL 函数。
     */
    private fun ImportCollectingPrinter.printDslBuildCopyFunction(
        builder: LeafBuilder<ElementField, Element, Implementation>,
        hasRequiredFields: Boolean,
    ) {
        val optIns = builder.allFields
            .filter { !it.invisibleField }
            .mapNotNullTo(mutableSetOf(experimentalContractsAnnotation)) { it.optInAnnotation }
        println("@OptIn(", optIns.joinToString { "${it.render()}::class" }, ")")
        val originalParameter = FunctionParameter(name = "original", type = builder.implementation.element)
        val initParameter = lambdaParameterForBuilderFunction(builder, hasRequiredFields)
        printFunctionWithBlockBody(
            name = builderFunctionName(builder) + "Copy",
            parameters = listOf(originalParameter, initParameter),
            returnType = builder.implementation.element,
            typeParameters = builder.implementation.element.params,
            isInline = true,
        ) {
            print("contract")
            printBlock {
                println("callsInPlace(init, InvocationKind.EXACTLY_ONCE)")
            }
            val copyBuilderVariableName = "copyBuilder"
            println("val ", copyBuilderVariableName, " = ", builder.render(), "()")
            for (field in builder.allFields) {
                if (field.invisibleField || field.skippedInCopy) continue
                copyField(field, originalParameter.name, copyBuilderVariableName)
            }
            println("return ", copyBuilderVariableName, ".apply(", initParameter.name, ").build()")
        }
    }
}

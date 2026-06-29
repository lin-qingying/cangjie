/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.generators.tree.config

import org.cangnova.cangjie.generators.tree.*
import org.cangnova.cangjie.generators.tree.imports.Importable
import org.cangnova.cangjie.utils.DummyDelegate
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * 为树节点的 Builder 生成提供 DSL 配置能力。
 * 例如可新增中间 Builder、或为生成属性设置默认值。
 */
abstract class AbstractBuilderConfigurator<Element, Implementation, ElementField>(
    /**
     * 已构建完成并等待配置 Builder 的树模型。
     */
    val model: Model<Element>
) where Element : AbstractElement<Element, ElementField, Implementation>,
        Implementation : AbstractImplementation<Implementation, Element, ElementField>,
        ElementField : AbstractField<ElementField> {

    /**
     * 生成 Builder 类名时使用的前缀。
     *
     * 应与 [AbstractElement.namePrefix] 保持一致。
     */
    protected abstract val namePrefix: String

    /**
     * [IntermediateBuilder] 的默认生成包名。
     */
    protected abstract val defaultBuilderPackage: String

    /**
     * 自定义入口：用于微调已有 Builder 或新增 Builder。
     *
     * 可在重写中使用以下 DSL：
     * - [builder]
     * - [noBuilder]
     * - [configureFieldInAllLeafBuilders]
     */
    abstract fun configureBuilders()

    /**
     * 当前配置器新增的全部中间层 Builder。
     */
    val intermediateBuilders = mutableListOf<IntermediateBuilder<ElementField, Element>>()

    /**
     * 配置中间 Builder 类。
     *
     * @param config 配置块，DSL 见 [IntermediateBuilderConfigurationContext]。
     */
    protected fun builder(config: IntermediateBuilderConfigurationContext.() -> Unit) = IntermediateBuilderDelegateProvider(config)

    /**
     * 配置叶子 Builder 类（即最终负责构造对应实现类实例的 Builder）。
     *
     * @param element 要配置 Builder 生成行为的元素。
     * @param config 配置块，DSL 见 [LeafBuilderConfigurationContext]。
     */
    protected fun builder(element: Element, type: String? = null, config: LeafBuilderConfigurationContext.() -> Unit) {
        val implementation = element.extractImplementation(type)
        val builder = implementation.builder
        requireNotNull(builder)
        LeafBuilderConfigurationContext(builder).apply(config)
    }

    /**
     * 禁用 [element] 的 Builder 生成。
     */
    protected fun noBuilder(element: Element, type: String? = null) {
        val implementation = element.extractImplementation(type)
        implementation.builder = null
    }

    /**
     * 从元素中解析需要配置的具体实现类。
     *
     * 未指定 [type] 时要求元素只有一个可生成叶子 Builder 的实现；指定 [type] 时按实现类类型名精确匹配。
     */
    private fun Element.extractImplementation(type: String?): Implementation {
        return if (type == null) {
            implementations.singleOrNull { it.kind?.hasLeafBuilder == true } ?: this@AbstractBuilderConfigurator.run {
                val message = buildString {
                    appendLine("${this@extractImplementation} has multiple implementations:")
                    for (implementation in implementations) {
                        appendLine("  - ${implementation.typeName}")
                    }
                    appendLine("Please specify implementation is needed")
                }
                throw IllegalArgumentException(message)
            }
        } else {
            implementations.firstOrNull { it.typeName == type } ?: this@AbstractBuilderConfigurator.run {
                val message = buildString {
                    appendLine("${this@extractImplementation} has not implementation $type. Existing implementations:")
                    for (implementation in implementations) {
                        appendLine("  - ${implementation.typeName}")
                    }
                    appendLine("Please specify implementation is needed")
                }
                throw IllegalArgumentException(message)
            }
        }
    }

    /**
     * 在所有实现中筛选：
     * 1. 满足 [implementationPredicate]；
     * 2. 且 [element] 是其非直接父类之一。
     */
    protected inline fun findImplementationsWithElementInParents(
        element: Element,
        implementationPredicate: (Implementation) -> Boolean = { true }
    ): Collection<Implementation> {
        return model.elements
            .flatMap { it.implementations }
            .mapNotNullTo(mutableSetOf()) { implementation ->
                if (!implementationPredicate(implementation)) return@mapNotNullTo null
                if (implementation.element == element) return@mapNotNullTo null
                val hasElementInParents = implementation.element.isSubclassOf(element)
                implementation.takeIf { hasElementInParents }
        }
    }

    /**
     * 当前模型中所有仍启用的叶子 Builder。
     */
    private val allLeafBuilders: List<LeafBuilder<ElementField, Element, Implementation>>
        get() = model.elements.flatMap { it.implementations }.mapNotNull { it.builder }

    /**
     * 对满足 [builderPredicate] 的所有叶子 Builder 中指定字段批量应用 [config]。
     *
     * @param field 需要配置的字段名。
     * @param builderPredicate 仅匹配该谓词的 Builder 会参与配置。
     * @param fieldPredicate 仅匹配该谓词的字段会参与配置。
     * @param config 配置块，参数为字段名；DSL 见 [LeafBuilderConfigurationContext]。
     */
    protected fun configureFieldInAllLeafBuilders(
        field: String,
        builderPredicate: ((LeafBuilder<ElementField, Element, Implementation>) -> Boolean)? = null,
        fieldPredicate: ((ElementField) -> Boolean)? = null,
        config: LeafBuilderConfigurationContext.(field: String) -> Unit
    ) {
        for (builder in allLeafBuilders) {
            if (builderPredicate != null && !builderPredicate(builder)) continue
            if (!builder.allFields.any { it.name == field }) continue
            if (fieldPredicate != null && !fieldPredicate(builder[field])) continue
            LeafBuilderConfigurationContext(builder).config(field)
        }
    }

    /**
     * 对所有叶子 Builder 批量应用 [config]。
     *
     * @param config 配置块，DSL 见 [LeafBuilderConfigurationContext]。
     */
    protected fun configureAllLeafBuilders(config: LeafBuilderConfigurationContext.() -> Unit) {
        for (builder in allLeafBuilders) {
            LeafBuilderConfigurationContext(builder).config()
        }
    }

    /**
     * 用于配置一个或多个中间/叶子 Builder 的 DSL 基类。
     */
    protected abstract inner class BuilderConfigurationContext {
        /**
         * 当前 DSL 上下文正在配置的 Builder。
         */
        protected abstract val builder: Builder<ElementField, Element>

        private fun getField(name: String): ElementField {
            return builder[name]
        }

        /**
         * 为 Builder 文件追加额外导入的类型/函数。
         *
         * 当字段默认值引用其他包中的符号时可使用此方法。
         * 注意：字段类型本身涉及的类型会自动导入。
         */
        fun additionalImports(vararg types: Importable) {
            types.forEach { builder.usedTypes += it }
        }

        /**
         * 指定本 Builder 中 [field] 的默认值，值可以是任意 Kotlin 代码片段。
         *
         * 若默认值依赖未导入符号，请配合 [additionalImports]。
         */
        fun default(field: String, value: String) {
            default(field) {
                this.value = value
            }
        }

        /**
         * 将 [fields] 在本 Builder 中的默认值统一设为 `true`。
         */
        fun defaultTrue(vararg fields: String) {
            for (field in fields) {
                default(field) {
                    value = "true"
                }
            }
        }

        /**
         * 将 [fields] 在本 Builder 中的默认值统一设为 `false`。
         */
        fun defaultFalse(vararg fields: String) {
            for (field in fields) {
                default(field) {
                    value = "false"
                }
            }
        }

        /**
         * 将 [fields] 在本 Builder 中的默认值统一设为 `null`。
         *
         * 注意：字段必须为可空类型。
         */
        fun defaultNull(vararg fields: String) {
            for (field in fields) {
                default(field) {
                    value = "null"
                }
                require(getField(field).nullable) {
                    "$field is not nullable field"
                }
            }
        }

        /**
         * 以 DSL 方式配置本 Builder 中 [field] 的默认值。
         *
         * 详见 [DefaultValueContext]。
         */
        fun default(field: String, init: DefaultValueContext.() -> Unit) {
            DefaultValueContext(getField(field)).apply(init).applyConfiguration()
        }

        /**
         * 字段默认值配置 DSL。
         */
        inner class DefaultValueContext(private val field: ElementField) {

            /**
             * 该字段在 Builder 中的默认值，可为任意 Kotlin 代码。
             *
             * 若依赖未导入符号，请使用 [additionalImports]。
             */
            var value: String? = null

            /**
             * 将默认值配置写入字段模型。
             */
            fun applyConfiguration() {
                if (value != null) field.defaultValueInBuilder = value
            }
        }
    }

    /**
     * 用于配置一个或多个中间 Builder 的 DSL。
     *
     * 可使用以下语法配置生成字段集合：
     * ```kotlin
     * fields from myElement // 使用 myElement 的全部字段
     * fields from myElement without "myField" // 排除 myField
     * fields from myElement without listOf("foo", "bar") // 排除 foo 与 bar
     * ```
     */
    protected inner class IntermediateBuilderConfigurationContext(
        /**
         * 当前正在配置的中间层 Builder。
         */
        override val builder: IntermediateBuilder<ElementField, Element>
    ) : BuilderConfigurationContext() {
        inner class Fields {

            /**
             * 将 [element] 的全部字段复制到当前 Builder。
             */
            infix fun from(element: Element): ExceptConfigurator {
                builder.fields += element.allFields.map { it.copy() }
                builder.packageName = "${element.packageName}.builder"
                builder.materializedElement = element
                return ExceptConfigurator()
            }
        }

        inner class ExceptConfigurator {

            /**
             * 从当前 Builder 中排除字段 [name]。
             */
            infix fun without(name: String) {
                without(listOf(name))
            }

            /**
             * 从当前 Builder 中批量排除 [names]。
             */
            infix fun without(names: List<String>) {
                builder.fields.removeAll { it.name in names }
            }
        }

        /**
         * 字段复制配置入口（从元素复制到当前中间 Builder）。
         *
         * 用法见 [IntermediateBuilderConfigurationContext]。
         */
        val fields = Fields()

        /**
         * 当前中间 Builder 的父 Builder 列表，可用于追加父类关系。
         */
        val parents: MutableList<IntermediateBuilder<ElementField, Element>>
            get() = builder.parents

        /**
         * 当前中间 Builder 是否生成 sealed interface。
         */
        var isSealed: Boolean
            get() = builder.isSealed
            set(value) {
                builder.isSealed = value
            }
    }

    /**
     * 支持 `val xxx by builder { ... }` 语法的委托提供器。
     */
    protected inner class IntermediateBuilderDelegateProvider(
        /**
         * 延迟应用到新建中间 Builder 上的配置块。
         */
        private val block: IntermediateBuilderConfigurationContext.() -> Unit
    ) {
        /**
         * 由委托属性名推导并创建出的中间 Builder。
         */
        lateinit var builder: IntermediateBuilder<ElementField, Element>

        /**
         * 根据被委托属性名创建中间 Builder 并注册配置。
         */
        operator fun provideDelegate(
            thisRef: Nothing?,
            prop: KProperty<*>
        ): ReadOnlyProperty<Nothing?, IntermediateBuilder<ElementField, Element>> {
            val name = namePrefix + prop.name.replaceFirstChar(Char::uppercaseChar)
            builder = IntermediateBuilder<ElementField, Element>(name, defaultBuilderPackage).apply {
                intermediateBuilders += this
                IntermediateBuilderConfigurationContext(this).block()
            }
            return DummyDelegate(builder)
        }
    }

    /**
     * 用于配置一个或多个叶子 Builder 的 DSL。
     */
    protected inner class LeafBuilderConfigurationContext(
        /**
         * 当前正在配置的叶子 Builder。
         */
        override val builder: LeafBuilder<ElementField, Element, Implementation>
    ) : BuilderConfigurationContext() {

        /**
         * 当前叶子 Builder 的父 Builder 列表，可用于追加父类关系。
         */
        val parents: MutableList<IntermediateBuilder<ElementField, Element>>
            get() = builder.parents

        /**
         * 将当前 Builder 生成为 `open class`。
         */
        fun openBuilder() {
            builder.isOpen = true
        }

        /**
         * 除常规 `build*()` 外，同时生成 `build*Copy()`：
         * 接收对应树元素实例并复制其值到 Builder，便于在复制过程中修改。
         */
        fun withCopy() {
            builder.wantsCopy = true
        }
    }
}

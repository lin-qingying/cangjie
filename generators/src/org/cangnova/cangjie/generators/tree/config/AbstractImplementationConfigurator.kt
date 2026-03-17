/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.generators.tree.config

import org.cangnova.cangjie.generators.tree.*
import org.cangnova.cangjie.generators.tree.imports.Importable
import org.cangnova.cangjie.generators.tree.printer.call
import kotlin.text.get

/**
 * 为语法树节点的 Impl 实现类提供 DSL 配置能力的抽象基类。
 *
 * 三个泛型参数约束：
 *   Implementation —— 实现类模型（如 CfirImplementation）
 *   Element        —— 树节点元素（如 CfirElement）
 *   ElementField   —— 节点字段（如 CfirField）
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class AbstractImplementationConfigurator<Implementation, Element, ElementField>
        where Implementation : AbstractImplementation<Implementation, Element, ElementField>,
              Element : AbstractElement<Element, ElementField, Implementation>,
              ElementField : AbstractField<ElementField> {

    /** 记录所有已显式调用 impl() 配置过的节点，用于后续批量处理 */
    private val elementsWithImpl = mutableSetOf<Element>()

    /** 子类实现：根据 element 和可选的 name 创建一个新的实现类模型 */
    protected abstract fun createImplementation(element: Element, name: String?): Implementation

    /**
     * 配置流程的总入口，按顺序执行四个步骤：
     *  1. configure()                        —— 子类自定义配置（noImpl / impl）
     *  2. generateDefaultImplementations()   —— 为未配置的叶子节点自动生成默认实现
     *  3. inheritImplementationFieldSpecifications() —— 从祖先节点继承字段默认值策略
     *  4. configureAllImplementations()      —— 子类批量后处理（兜底配置）
     */
    fun configureImplementations(model: Model<Element>) {
        configure(model)
        generateDefaultImplementations(model.elements)
        inheritImplementationFieldSpecifications(model.elements)
        configureAllImplementations(model)
    }

    /**
     * 【扩展点1】子类在此方法中使用 noImpl / impl 对各节点逐一配置。
     */
    protected abstract fun configure(model: Model<Element>)

    /**
     * 【扩展点2】子类在此方法中使用 configureFieldInAllImplementations 对所有实现做批量后处理。
     */
    protected abstract fun configureAllImplementations(model: Model<Element>)

    /**
     * 标记 [element] 不需要生成任何实现类。
     * 对应 ImplementationConfigurator 中的 noImpl(expression) 等调用。
     */
    protected fun noImpl(element: Element) {
        element.doesNotNeedImplementation = true
    }

    /**
     * 为 [element] 创建或获取一个实现类模型，并通过 [config] DSL 对其进行配置。
     *
     * @param element 要配置实现的节点
     * @param name    实现类的名称；为 null 时使用默认名称。若同名实现已存在则复用，否则新建。
     * @param config  配置块，DSL 由 [ImplementationContext] 提供
     * @return        配置完成的实现类模型
     */
    protected fun impl(element: Element, name: String? = null, config: ImplementationContext.() -> Unit = {}): Implementation {
        // 优先复用同名已有实现，避免重复创建
        val implementation = element.implementations.firstOrNull { it.name == name }
            ?: createImplementation(element, name)
        val context = ImplementationContext(implementation)
        context.apply(config)
        elementsWithImpl += element   // 加入"已配置集合"，供后续批量处理使用
        return implementation
    }

    /**
     * 为所有继承自 [element] 的实现类批量应用 [config] 配置。
     * 配置结果会在 inheritImplementationFieldSpecifications() 阶段下发到具体实现。
     */
    protected fun allImplOf(element: Element, config: ElementContext.() -> Unit) {
        val context = ElementContext(element)
        context.apply(config)
    }

    /**
     * 为"没有子节点、且未被显式 impl() 配置、且未被 noImpl() 排除"的叶子节点
     * 自动生成默认实现，确保所有具体节点都有对应的实现类。
     */
    private fun generateDefaultImplementations(elements: List<Element>) {
        elements
            .filter { it.subElements.isEmpty() && it !in elementsWithImpl && !it.doesNotNeedImplementation }
            .forEach {
                impl(it)
            }
    }

    /**
     * 字段默认值策略的继承传播：
     * 对每个实现类的每个字段，若该字段尚未指定 implementationDefaultStrategy，
     * 则沿祖先链（广度优先）查找最近的父节点中对同名字段的配置并继承；
     * 若整条链上都没有配置，则标记为 Required（调用方必须显式赋值）。
     *
     * 若多个父节点对同一字段有不同配置，则报错要求子类显式指定，避免歧义。
     */
    private fun inheritImplementationFieldSpecifications(elements: List<Element>) {
        for (element in elements) {
            for (implementation in element.implementations) {
                for (field in implementation.allFields) {
                    if (field.implementationDefaultStrategy == null) {
                        for (ancestor in element.elementAncestorsAndSelfBreadthFirst()) {
                            val inheritedDefaults = ancestor.elementParents
                                .mapNotNull { it.element.getOrNull(field.name) }
                                .mapNotNull { it.implementationDefaultStrategy }
                            if (inheritedDefaults.isNotEmpty()) {
                                // 有且仅有一个祖先配置时才能安全继承，否则报歧义错误
                                field.implementationDefaultStrategy = inheritedDefaults.singleOrNull()
                                    ?: error("Field $field has ambiguous default value, please specify it explicitly for the ${element.name} element")
                                break
                            }
                        }

                        // 整条祖先链都没有配置 → 标记为必填，强制调用方赋值
                        if (field.implementationDefaultStrategy == null) {
                            field.implementationDefaultStrategy = AbstractField.ImplementationDefaultStrategy.Required
                        }
                    }
                }
            }
        }
    }

    /**
     * 对所有满足 [implementationPredicate] 条件的实现类，批量配置指定字段。
     *
     * @param fieldName               要配置的字段名；为 null 时对所有字段应用 config
     * @param implementationPredicate 实现类过滤条件，默认全部匹配
     * @param fieldPredicate          字段过滤条件，默认全部匹配
     * @param config                  配置块，接收字段名作为参数
     */
    protected fun configureFieldInAllImplementations(
        fieldName: String?,
        implementationPredicate: (Implementation) -> Boolean = { true },
        fieldPredicate: (ElementField) -> Boolean = { true },
        config: ImplementationContext.(field: String) -> Unit,
    ) {
        for (element in elementsWithImpl) {
            for (implementation in element.implementations) {
                if (!implementationPredicate(implementation)) continue
                // 若指定了字段名但该实现中不含该字段，则跳过
                if (fieldName != null && !implementation.allFields.any { it.name == fieldName }) continue

                val fields = if (fieldName != null) {
                    listOf(implementation[fieldName])
                } else {
                    implementation.allFields
                }

                for (field in fields.filter(fieldPredicate)) {
                    ImplementationContext(implementation).config(field.name)
                }
            }
        }
    }

    /**
     * 对所有满足 [implementationPredicate] 条件的实现类批量应用 [config]（不针对特定字段）。
     */
    protected fun configureAllImplementations(
        implementationPredicate: (Implementation) -> Boolean = { true },
        config: ImplementationContext.() -> Unit,
    ) {
        for (element in elementsWithImpl) {
            for (implementation in element.implementations) {
                if (!implementationPredicate(implementation)) continue
                ImplementationContext(implementation).config()
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // 字段容器 DSL 基类：被 ImplementationContext 和 ElementContext 共同继承
    // ──────────────────────────────────────────────────────────────────────

    protected abstract class FieldContainerContext<Field>(
        private val fieldContainer: FieldContainer<Field>,
    ) where Field : AbstractField<*> {

        /** 将指定字段在实现类中强制设为可变（var），即使元素配置器中声明为不可变 */
        fun isMutable(vararg fields: String) {
            fields.forEach {
                val field = fieldContainer[it]
                field.isMutable = true
            }
        }

        /** 将指定字段在实现类中设为 lateinit，延迟初始化 */
        fun isLateinit(vararg fields: String) {
            fields.forEach {
                val field = fieldContainer[it]
                field.implementationDefaultStrategy = AbstractField.ImplementationDefaultStrategy.Lateinit
            }
        }

        /**
         * 为 [field] 指定任意代码字符串作为默认值。
         *
         * @param withGetter 为 true 时生成计算属性（getter），为 false 时生成存储属性（初始值）
         */
        fun default(field: String, value: String, withGetter: Boolean = false) {
            default(field) {
                this.value = value
                this.withGetter = withGetter
            }
        }

        /**
         * 生成一个只读计算属性：getter 返回 [value]，setter 抛出错误。
         * 适用于逻辑上"不可变但需要 getter 形式"的字段。
         */
        fun defaultWithErrorOnSet(field: String, value: String) {
            default(field) {
                this.value = value
                this.withGetter = true
                this.customSetter = "error(\"Mutation of $field is not supported for this class.\")"
            }
        }

        /**
         * 将指定字段的默认值设为 true。
         * @param withGetter true → 计算属性（getter 返回 true）；false → 存储属性（初始值 true）
         */
        fun defaultTrue(vararg fields: String, withGetter: Boolean = false) {
            for (field in fields) {
                default(field) {
                    value = "true"
                    this.withGetter = withGetter
                }
            }
        }

        /**
         * 将指定字段的默认值设为 false。
         * @param withGetter true → 计算属性；false → 存储属性
         */
        fun defaultFalse(vararg fields: String, withGetter: Boolean = false) {
            for (field in fields) {
                default(field) {
                    value = "false"
                    this.withGetter = withGetter
                }
            }
        }

        /**
         * 将指定字段的默认值设为 null。
         * 要求字段本身必须是可空类型，否则抛出异常。
         *
         * @param withGetter true  → 生成计算属性：`get() = null`（无后备字段，不占内存）
         *                   false → 生成存储属性：`= null`（有后备字段）
         *
         * source 字段正是通过此方法配置的：
         *   defaultNull("source", withGetter = true)
         *   → 生成：override val source: SourceElement? get() = null
         */
        fun defaultNull(vararg fields: String, withGetter: Boolean = false) {
            for (field in fields) {
                default(field) {
                    value = "null"
                    this.withGetter = withGetter
                }
                // 安全校验：非可空字段不允许设 null 默认值
                require(fieldContainer[field].nullable) {
                    "$field is not nullable field"
                }
            }
        }

        /**
         * 将指定列表字段的默认值设为 emptyList()。
         * 要求字段必须是 ListField 类型。
         */
        fun defaultEmptyList(vararg fields: String, withGetter: Boolean = false) {
            for (field in fields) {
                require(fieldContainer[field] is ListField) {
                    "$field is list field"
                }
                default(field) {
                    value = "emptyList()"
                    this.withGetter = withGetter
                }
            }
        }

        /**
         * 为指定字段添加额外注解。
         * 生成代码时会在字段声明前打印 `@AnnotationName`。
         * 构造函数参数中则打印 `@property:AnnotationName`。
         */
        fun annotateField(fieldName: String, vararg annotations: ClassRef<*>) {
            val field = fieldContainer[fieldName]
            field.additionalAnnotations.addAll(annotations)
        }

        /** 通过 DSL 块精细配置字段默认值，见 [DefaultValueContext] */
        fun default(field: String, init: DefaultValueContext.() -> Unit) {
            DefaultValueContext(fieldContainer[field]).apply(init).applyConfiguration()
        }

        /**
         * 将 [fields] 列表中的每个字段的 getter 委托给 [delegate] 的同名属性。
         *
         * 例如 delegateFields(listOf("foo"), "myDelegate") 会生成：
         *   val foo: Foo
         *       get() = myDelegate.foo
         */
        fun delegateFields(fields: List<String>, delegate: String) {
            for (field in fields) {
                default(field) {
                    this.delegate = delegate
                }
            }
        }

        /**
         * 字段默认值配置 DSL，负责收集配置项并通过 applyConfiguration() 写入字段模型。
         */
        inner class DefaultValueContext(private val field: Field) {

            /** 字段的默认值，可以是任意 Kotlin 代码字符串 */
            var value: String? = null

            /**
             * 委托字段名：将本字段的 getter 委托给另一个字段的同名属性。
             * 设置后自动启用 withGetter = true。
             */
            var delegate: String? = null
                set(value) {
                    field = value
                    if (value != null) {
                        withGetter = true
                    }
                }

            /**
             * 当 delegate 不为 null 时，指定委托调用的具体表达式。
             * 例如 delegate="myDelegate", delegateCall="getFoo()" →
             *   get() = myDelegate.getFoo()
             */
            var delegateCall: String? = null

            /** 强制覆盖字段可变性；为 null 时沿用元素配置器中的声明 */
            var isMutable: Boolean? = null

            /**
             * 是否生成计算属性（带 getter）而非存储属性。
             * 设为 true 时：若无 customSetter 则自动设为不可变（val）。
             *
             * withGetter = true  → override val source: T? get() = null  （无后备字段）
             * withGetter = false → override val source: T? = null         （有后备字段）
             */
            var withGetter: Boolean = false
                set(value) {
                    field = value
                    if (value) {
                        isMutable = customSetter != null
                    }
                }

            /**
             * 自定义 setter 代码；设置后字段自动变为可变（var）。
             * 可配合 withGetter 实现"只读对外、setter 抛错"的模式。
             */
            var customSetter: String? = null
                set(value) {
                    field = value
                    isMutable = true
                }

            /**
             * 将本 DSL 上下文收集到的配置写入字段模型（implementationDefaultStrategy）。
             * 优先级：显式 value > delegate 委托 > 不写入（保持已有策略）
             */
            fun applyConfiguration() {
                field.customSetter = customSetter
                isMutable?.let { field.isMutable = it }

                var value = value
                when {
                    // 有明确的默认值 → 直接写入 DefaultValue 策略
                    value != null -> field.implementationDefaultStrategy =
                        AbstractField.ImplementationDefaultStrategy.DefaultValue(value, withGetter)

                    // 有委托字段 → 生成委托表达式后写入 DefaultValue 策略
                    delegate != null -> {
                        val actualDelegateField = fieldContainer[delegate!!]
                        val name = delegateCall ?: field.name
                        value = "${actualDelegateField.name}${actualDelegateField.call()}$name"
                        field.implementationDefaultStrategy =
                            AbstractField.ImplementationDefaultStrategy.DefaultValue(value, withGetter)
                    }
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // 具体 DSL 上下文类
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 单个实现类的配置 DSL，继承字段配置能力，并额外提供实现类级别的配置方法。
     */
    protected inner class ImplementationContext(val implementation: Implementation) :
        FieldContainerContext<ElementField>(implementation) {

        /** 为实现类添加 @OptIn 注解，当类内代码需要 opt-in API 时使用 */
        fun optInToInternals() {
            implementation.requiresOptIn = true
        }

        /**
         * 将实现类可见性从默认的 internal 提升为 public。
         * 对应 ImplementationConfigurator 中的 publicImplementation() 调用。
         */
        fun publicImplementation() {
            implementation.isPublic = true
        }

        /**
         * 追加额外的 import 声明。
         * 当字段默认值代码引用了其他包的类型/函数时，需要在此声明，
         * 字段类型本身引用的类会被自动导入，无需手动添加。
         */
        fun additionalImports(vararg importables: Importable) {
            implementation.additionalImports.addAll(importables)
        }

        /**
         * 自定义实现类的类型（open class / object 等）。
         * 为 null 时由 InterfaceAndAbstractClassConfigurator 自动决定。
         */
        var kind: ImplementationKind?
            get() = implementation.kind
            set(value) {
                implementation.kind = value
            }

        /** 为实现类设置 KDoc 注释 */
        fun kDoc(kDoc: String) {
            implementation.kDoc = kDoc
        }
    }

    /**
     * 针对某个元素（及其所有子实现）的批量配置 DSL。
     * 通过 allImplOf() 使用，配置结果会在 inheritImplementationFieldSpecifications() 阶段向下传播。
     */
    protected inner class ElementContext(val element: Element) : FieldContainerContext<ElementField>(element)
}
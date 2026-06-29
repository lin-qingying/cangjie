/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.generators.tree

import org.cangnova.cangjie.generators.tree.imports.Importable

/**
 * 树生成器中的字段模型基类。
 *
 * 字段描述会同时参与元素接口、实现类、构建器、visitor/transformer 子节点遍历等代码生成阶段，
 * 因此这里集中保存字段类型、可变性、默认值、注解、可见性和继承覆盖关系。
 */
abstract class AbstractField<Field : AbstractField<Field>> {

    /**
     * 字段在生成源码中的属性名。
     */
    abstract val name: String

    /**
     * 字段在生成源码中的完整类型引用。
     */
    abstract val typeRef: TypeRefWithNullability

    /**
     * 字段类型是否可空。
     */
    val nullable: Boolean
        get() = typeRef.nullable

    /**
     * 生成到字段属性上的 KDoc 文本。
     */
    var kDoc: String? = null

    /**
     * 是否将生成属性标记为 `@Volatile`。
     */
    open val isVolatile: Boolean
        get() = false

    /**
     * 字段是否在生成模型中保持不可变。
     */
    abstract val isFinal: Boolean

    /**
     * 字段是否只作为构造参数参与生成，而不是生成普通属性。
     */
    open val isParameter: Boolean
        get() = false

    /**
     * 字段额外依赖的导入项。
     *
     * 这些导入不一定能从 [typeRef] 推导出来，例如自定义初始化表达式中使用的类型。
     */
    open val arbitraryImportables: MutableList<Importable> = mutableListOf()

    /**
     * 直接打印到生成属性上的额外注解。
     */
    open val additionalAnnotations: MutableList<ClassRef<*>> = mutableListOf()

    /**
     * 需要放在字段声明上的 opt-in 注解。
     */
    open var optInAnnotation: ClassRef<*>? = null

    /**
     * 用于替换继承字段 opt-in 注解的注解类型。
     */
    open var replaceOptInAnnotation: ClassRef<*>? = null

    /**
     * 字段在生成实现中是否可变。
     */
    abstract var isMutable: Boolean

    /**
     * 自定义初始化调用文本。
     *
     * 该值不为 `null` 时字段被视为不可见字段，不直接作为普通构造参数暴露。
     */
    open var customInitializationCall: String? = null

    /**
     * 字段是否不应作为普通可见属性生成。
     */
    val invisibleField: Boolean
        get() = customInitializationCall != null

    /**
     * 生成字段时附加的废弃信息。
     */
    var deprecation: Deprecated? = null

    /**
     * 生成字段的可见性。
     */
    var visibility: Visibility = Visibility.PUBLIC

    /**
     * 生成字段是否覆盖父类型中的属性。
     */
    var isOverride: Boolean = false

    /**
     * 如果为 `true`，该字段会在 `build%Element%Copy` 函数中被跳过。
     *
     *  @see AbstractBuilderPrinter.printDslBuildCopyFunction
     */
    open var skippedInCopy: Boolean = false

    /**
     * 该字段是否可能直接或通过列表等容器持有树元素。
     */
    open val containsElement: Boolean
        get() = typeRef is ElementOrRef<*> || this is ListField && baseType is ElementOrRef<*>

    /**
     * 字段在生成实现类中的初始化策略。
     *
     * `null` 表示尚未显式配置，后续会从祖先元素继承，或回落为 [ImplementationDefaultStrategy.Required]。
     *
     * @see org.cangnova.cangjie.generators.tree.config.AbstractImplementationConfigurator.inheritImplementationFieldSpecifications .
     */
    open var implementationDefaultStrategy: ImplementationDefaultStrategy? = null

    /**
     * 构建器中该字段的默认值表达式。
     */
    abstract var defaultValueInBuilder: String?

    /**
     * 构建器中该字段的自定义 setter 方法体文本。
     */
    abstract var customSetter: String?

    /**
     * @see org.cangnova.cangjie.generators.tree.detectBaseTransformerTypes
     */
    var useInBaseTransformerDetection = true

    /**
     * 该字段在语义上是否表示树的子节点引用。
     *
     * 该标志会影响生成的 `acceptChildren` 与 `transformChildren` 是否访问该字段；
     * 被标记为子节点的字段总会参与这些方法中的递归访问。
     *
     * 只有 [containsElement] 为 `true` 时该标志才有实际意义。
     */
    abstract val isChild: Boolean

    /**
     * 当前字段覆盖或继承来源中的同名父字段集合。
     */
    open val overriddenFields: MutableSet<Field> = mutableSetOf<Field>()

    /**
     * 根据父字段集合更新当前字段的继承属性。
     *
     * 目前会记录覆盖关系，并在任一父字段可变时把当前字段提升为可变字段。
     */
    open fun updatePropertiesFromOverriddenFields(parentFields: List<Field>) {
        overriddenFields += parentFields
        isMutable = isMutable || parentFields.any { it.isMutable }
    }

    /**
     * 返回字段名，便于配置错误与调试输出。
     */
    override fun toString(): String {
        return name
    }

    /**
     * 在可行时用 [TypeRef.substitute] 的结果替换字段类型。
     */
    abstract fun substituteType(map: TypeParameterSubstitutionMap)

    /**
     * 创建当前字段模型的副本。
     */
    fun copy() = internalCopy().also(::updateFieldsInCopy)

    /**
     * 由具体字段类型实现的浅拷贝创建逻辑。
     */
    protected abstract fun internalCopy(): Field

    /**
     * 把通用字段配置同步到 [copy]。
     */
    protected open fun updateFieldsInCopy(copy: Field) {
        copy.kDoc = kDoc
        copy.arbitraryImportables += arbitraryImportables
        copy.additionalAnnotations += additionalAnnotations
        copy.optInAnnotation = optInAnnotation
        copy.replaceOptInAnnotation = replaceOptInAnnotation
        copy.isMutable = isMutable
        copy.deprecation = deprecation
        copy.visibility = visibility
        copy.isOverride = isOverride
        copy.useInBaseTransformerDetection = useInBaseTransformerDetection
        copy.overriddenFields += overriddenFields
        copy.implementationDefaultStrategy = implementationDefaultStrategy
    }

    /**
     * 字段在实现类中的默认初始化策略。
     */
    sealed interface ImplementationDefaultStrategy {
        /**
         * 生成实现类属性或 getter 时使用的默认值表达式。
         */
        open val defaultValue: String?
            get() = null

        /**
         * 默认值是否通过自定义 getter 生成，而不是作为存储属性初始化值生成。
         */
        open val withGetter: Boolean
            get() = false


        /**
         * 字段必须在实现类构造函数中显式初始化。
         */
        data object Required : ImplementationDefaultStrategy

        /**
         * 字段会生成为 `lateinit var`。
         */
        data object Lateinit : ImplementationDefaultStrategy

        /**
         * 使用固定默认值初始化字段。
         *
         * 当 [withGetter] 为 `false` 时生成带初始化值的存储属性；
         * 当 [withGetter] 为 `true` 时生成计算属性并让 getter 返回 [defaultValue]。
         */
        data class DefaultValue(
            /**
             * 默认值表达式源码。
             */
            override val defaultValue: String,
            /**
             * 是否通过 getter 返回默认值。
             */
            override val withGetter: Boolean,
        ) : ImplementationDefaultStrategy
    }

    /**
     * 如果字段表示声明符号，则记录该符号与当前元素之间的所有权关系。
     *
     * 对元素 `someElement` 来说，[symbolFieldRole] 为 [SymbolFieldRole.DECLARED] 表示
     * `someElement.symbol.owner === someElement`；其他符号引用则使用 [SymbolFieldRole.REFERENCED]。
     *
     * 字段不表示符号时该属性应保持为 `null`。
     */
    var symbolFieldRole: SymbolFieldRole? = null

    /**
     * 符号字段在其所属元素中的角色。
     */
    enum class SymbolFieldRole {
        DECLARED, REFERENCED
    }
}

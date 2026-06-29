

package org.cangnova.cangjie.generators.tree

import org.cangnova.cangjie.generators.tree.imports.ImportCollecting
import org.cangnova.cangjie.generators.tree.imports.Importable

/**
 * 生成器 Builder 模型基类。
 *
 * Builder 本身也是可渲染的类型引用，并作为字段容器参与字段继承、DSL 构造函数和 copy 函数生成。
 */
sealed class Builder<ElementField, Element> : FieldContainer<ElementField>, TypeRefWithNullability, Importable
        where ElementField : AbstractField<*>,
              Element : AbstractElement<Element, *, *> {

    /**
     * 当前 Builder 继承的中间层 Builder。
     */
    val parents: MutableList<IntermediateBuilder<ElementField, Element>> = mutableListOf()

    /**
     * Builder 源码中额外使用到的类型。
     */
    val usedTypes: MutableList<Importable> = mutableListOf()

    /**
     * 继承链中存在但当前 Builder 不应暴露为可配置属性的字段。
     */
    abstract val uselessFields: List<ElementField>

    /**
     * 按字段名缓存字段是否来自父 Builder。
     */
    private val fieldsFromParentIndex: Map<String, Boolean> by lazy {
        mutableMapOf<String, Boolean>().apply {
            for (field in allFields + uselessFields) {
                this[field.name] = parents.any { field.name in it.allFields.map { it.name } }
            }
        }
    }

    /**
     * 判断指定字段是否由父 Builder 提供。
     */
    fun isFromParent(field: AbstractField<*>): Boolean = fieldsFromParentIndex.getValue(field.name)

    /**
     * Builder 类型定义自身不受类型参数替换影响。
     */
    override fun substitute(map: TypeParameterSubstitutionMap) = this

    /**
     * 渲染 Builder 类型名并记录导入。
     */
    override fun renderTo(appendable: Appendable, importCollector: ImportCollecting) {
        importCollector.addImport(this)
        appendable.append(typeName)
    }

    /**
     * Builder 类型引用始终按非空类型渲染。
     */
    override val nullable: Boolean
        get() = false

    /**
     * Builder 不支持可空变体，复制时返回自身。
     */
    override fun copy(nullable: Boolean) = this

    /**
     * Builder 不生成 visitor children 方法。
     */
    override var hasAcceptChildrenMethod: Boolean = false

    /**
     * Builder 不生成 transformer children 方法。
     */
    override var hasTransformChildrenMethod: Boolean = false
}

/**
 * 与具体实现类一一对应的叶子 Builder。
 */
class LeafBuilder<Field, Element, Implementation>(
    /**
     * 该 Builder 构造的具体实现类。
     */
    val implementation: Implementation,
) : Builder<Field, Element>()
        where Field : AbstractField<Field>,
              Element : AbstractElement<Element, Field, Implementation>,
              Implementation : AbstractImplementation<Implementation, Element, Field> {
    /**
     * 生成源码中的 Builder 类型名。
     */
    override val typeName: String
        get() = (implementation.name ?: implementation.element.typeName) + "Builder"

    /**
     * 叶子 Builder 暴露给调用方配置的字段。
     */
    override val allFields: List<Field> by lazy { implementation.fieldsInConstructor.filter { !it.isFinal } }

    /**
     * 父 Builder 提供但当前具体实现构造不再需要的字段。
     */
    override val uselessFields: List<Field> by lazy {
        val fieldsFromParents = parents.flatMap { it.allFields }.map { it.name }.toSet()
        val fieldsFromImplementation = implementation.allFields
        (fieldsFromImplementation - allFields).filter { it.name in fieldsFromParents }
    }

    /**
     * Builder 生成文件所在包名。
     */
    override val packageName: String = implementation.packageName.replace(".impl", ".builder")

    /**
     * 生成的 Builder 类型是否允许继续继承。
     */
    var isOpen: Boolean = false

    /**
     * 是否为该 Builder 生成 copy 风格 DSL 函数。
     */
    var wantsCopy: Boolean = false
}

/**
 * 中间层 Builder 模型。
 */
class IntermediateBuilder<Field, Element>(
    /**
     * 生成源码中的中间 Builder 类型名。
     */
    override val typeName: String,
    /**
     * 中间 Builder 生成文件所在包名。
     */
    override var packageName: String,
) : Builder<Field, Element>()
        where Field : AbstractField<*>,
              Element : AbstractElement<Element, *, *> {
    /**
     * 当前中间 Builder 直接声明的字段。
     */
    val fields: MutableList<Field> = mutableListOf()

    /**
     * 当前中间 Builder 对应的物化元素。
     */
    var materializedElement: Element? = null

    /**
     * 当前中间 Builder 是否生成 sealed 类型。
     */
    var isSealed: Boolean = false

    /**
     * 合并父 Builder 与当前 Builder 直接字段后的完整字段列表。
     */
    override val allFields: List<Field> by lazy {
        buildMap<String, Field> {
            parents.forEach { parent ->
                parent.allFields.associateByTo(this) { it.name }
            }
            fields.associateByTo(this) { it.name }
        }.values.toList()
    }

    /**
     * 中间 Builder 不维护无效字段集合。
     */
    override val uselessFields: List<Field> = emptyList()
}

/** 当前实现类型是否需要生成叶子 Builder。 */
val ImplementationKind.hasLeafBuilder: Boolean
    get() = this == ImplementationKind.FinalClass || this == ImplementationKind.OpenClass

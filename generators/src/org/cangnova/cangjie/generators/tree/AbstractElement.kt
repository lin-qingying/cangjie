

package org.cangnova.cangjie.generators.tree

import org.cangnova.cangjie.generators.tree.imports.ImportCollecting
import org.cangnova.cangjie.generators.tree.imports.Importable

/**
 * 树生成器中树节点的抽象类/接口。
 *
 * 例如：`CfirElement`、`CfirRegularClass`。
 */
abstract class AbstractElement<Element, Field, Implementation>(
    /**
     * 元素在树 DSL 中的短名称。
     *
     * 生成源码中的最终类型名由 [namePrefix] 与该名称共同组成。
     */
    val name: String,
) : ElementOrRef<Element>, FieldContainer<Field>, ImplementationKindOwner, ImportCollecting
        where Element : AbstractElement<Element, Field, Implementation>,
              Field : AbstractField<Field>,
              Implementation : AbstractImplementation<Implementation, Element, *> {

    /**
     * 元素作为属性名或 DSL 名称使用时的基础名称。
     */
    abstract val propertyName: String

    /**
     * 生成元素类型名时使用的统一前缀。
     */
    abstract val namePrefix: String

    /**
     * 生成到元素类型上的 KDoc 文本。
     */
    var kDoc: String? = null

    /**
     * 当前元素直接声明的字段集合。
     *
     * 继承字段不会写入该集合，而是在 [inheritFields] 中合并到 [allFields]。
     */
    val fields = mutableSetOf<Field>()

    /**
     * 当前元素声明的类型参数列表。
     */
    val params = mutableListOf<TypeVariable>()

    /**
     * 当前元素的直接父元素引用列表。
     */
    private val _elementParents = mutableListOf<ElementRef<Element>>()

    /**
     * 当前元素的只读直接父元素引用列表。
     */
    val elementParents: List<ElementRef<Element>> get() = _elementParents

    /**
     * 为当前元素添加一个直接父元素。
     *
     * 该操作会同时维护父元素的子元素反向索引。
     */
    fun addParent(parent: ElementRef<Element>) {
        _elementParents.add(parent)
        parent.element._subElements.add(element)
    }

    /**
     * 将一个已有父元素替换为新的父元素引用。
     *
     * 替换时会同步更新旧父元素和新父元素上的子元素反向索引。
     */
    fun replaceParent(oldParent: Element, newParent: ElementRef<Element>) {
        val parentIndex = _elementParents.indexOfFirst { it.element == oldParent }
        require(parentIndex >= 0) {
            "$oldParent is not parent of $this"
        }
        _elementParents[parentIndex] = newParent
        oldParent._subElements.remove(element)
        newParent.element._subElements.add(element)
    }

    /**
     * 除树元素父类型以外的额外父类型。
     *
     * 常用于给生成元素补充纯抽象父类或外部接口。
     */
    val otherParents = mutableListOf<ClassRef<*>>()

    /**
     * 当前元素完整的父类型引用列表。
     */
    val parentRefs: List<ClassOrElementRef>
        get() = elementParents + otherParents

    /**
     * 当前元素是否为树模型根元素。
     */
    val isRootElement: Boolean
        get() = elementParents.isEmpty()

    @Suppress("PropertyName")
    /**
     * 当前元素的直接子元素集合。
     *
     * 下划线名称保留给生成器内部维护可变反向索引使用。
     */
    val _subElements = mutableSetOf<Element>()

    /**
     * 当前元素的只读直接子元素集合。
     */
    val subElements: Set<Element> get() = _subElements

    /**
     * 当前元素是否应生成 sealed class 或 sealed interface。
     */
    var isSealed: Boolean = false

    /**
     * 当前元素出现在 visitor 方法名中的名称片段。
     */
    var nameInVisitorMethod: String = name

    /**
     * 当前元素对应的 visitor 方法名。
     */
    val visitFunctionName: String
        get() = "visit$nameInVisitorMethod"

    /**
     * visitor 方法中当前元素参数的名称。
     */
    abstract val visitorParameterName: String

    /**
     * visitor 层级中显式指定的父元素。
     */
    var customParentInVisitor: Element? = null

    /**
     * visitor 生成时使用的父元素。
     *
     * 默认取唯一非根父元素；多继承或根元素场景下需要通过 [customParentInVisitor] 明确指定。
     */
    open val parentInVisitor: Element?
        get() = customParentInVisitor ?: elementParents.singleOrNull()?.element?.takeIf { !it.isRootElement }

    /**
     * 当前元素实现关系中的直接父元素。
     */
    override val allParents: List<Element>
        get() = elementParents.map { it.element }

    /**
     * 生成源码中的元素类型名。
     */
    override val typeName: String
        get() = namePrefix + name

    /**
     * 将当前元素类型名渲染到源码，并记录对应导入。
     */
    final override fun renderTo(appendable: Appendable, importCollector: ImportCollecting) {
        importCollector.addImport(this)
        appendable.append(typeName)
    }

    /**
     * 当前元素合并继承后的完整字段列表。
     */
    override lateinit var allFields: List<Field>

    /**
     * 继承父元素字段并计算 [allFields]。
     *
     * 同名字段会按类型判定是否可自动继承；类型不唯一时要求当前元素显式声明字段以消除歧义。
     */
    internal fun inheritFields() {
        val result = LinkedHashMap<String, Field>()
        fields.toList().asReversed().associateByTo(result) { it.name }

        val allInheritedFieldsByParent = buildMap<String, MutableList<Pair<ElementRef<Element>, Field>>> {
            elementParents.asReversed().forEach { parentRef ->
                parentRef.element.allFields.asReversed().forEach { field ->
                    val list = remove(field.name) ?: mutableListOf()
                    list.add(parentRef to field)
                    put(field.name, list)
                }
            }
        }

        for ((fieldName, inheritedFieldsByParent) in allInheritedFieldsByParent) {
            var field = result[fieldName]
            if (field == null) {
                val inheritFrom = inheritedFieldsByParent.distinctBy { it.second.typeRef }.singleOrNull() ?: error(
                    "Field $fieldName has ambiguous type, coming from [${inheritedFieldsByParent.joinToString { it.first.element.typeName }}], " +
                            "please specify it explicitly for the ${element.name} element"
                )

                field = inheritFrom.second.copy().apply {
                    substituteType(inheritFrom.first.args)
                }

                result[fieldName] = field
            }

            val inheritedFields = inheritedFieldsByParent.map { it.second }
            field.isOverride = true
            field.updatePropertiesFromOverriddenFields(inheritedFields)
        }

        allFields = result.values.toList().asReversed()
    }

    /**
     * transformer 访问该元素时显式指定的返回元素类型。
     */
    var transformerReturnType: Element? = null

    /**
     * 根据字段引用关系推导出的基础 transformer 返回类型。
     */
    internal var baseTransformerType: Element? = null

    /**
     * 当前元素在 transformer 方法签名中使用的返回元素类型。
     */
    val transformerClass: Element
        get() = transformerReturnType ?: baseTransformerType ?: element

    /**
     * 当前元素声明的所有具体实现类。
     */
    val implementations = mutableListOf<Implementation>()

    /**
     * 当前元素是否不需要生成实现类。
     */
    var doesNotNeedImplementation: Boolean = false

    /**
     * 当前元素生成源码时需要额外输出的导入。
     */
    val additionalImports = mutableListOf<Importable>()

    /**
     * 记录元素生成源码中额外需要的导入。
     */
    override fun addImport(importable: Importable) {
        additionalImports.add(importable)
    }

    /**
     * 当前元素最终推导或显式配置的实现种类。
     */
    override var kind: ImplementationKind? = null

    /**
     * 当前元素是否生成 `acceptChildren` 方法。
     */
    override var hasAcceptChildrenMethod: Boolean = false

    /**
     * 当前元素是否生成 `transformChildren` 方法。
     */
    override var hasTransformChildrenMethod: Boolean = false

    @Suppress("UNCHECKED_CAST")
    final override val element: Element
        get() = this as Element

    final override val args: Map<NamedTypeParameterRef, TypeRef>
        get() = emptyMap()

    final override val nullable: Boolean
        get() = false

    /**
     * 当前元素是否应写入生成输出。
     */
    var doPrint = true

    /**
     * 使用新的可空性复制为元素引用。
     */
    final override fun copy(nullable: Boolean) = ElementRef(element, args, nullable)

    /**
     * 使用新的类型实参复制为元素引用。
     */
    final override fun copy(args: Map<NamedTypeParameterRef, TypeRef>) = ElementRef(element, args, nullable)

    @Suppress("UNCHECKED_CAST")
    /**
     * 元素定义自身不受类型参数替换影响。
     */
    override fun substitute(map: TypeParameterSubstitutionMap): Element = this as Element

    /**
     * 创建将所有元素类型参数替换为星号投影的元素引用。
     */
    fun withStarArgs(): ElementRef<Element> = copy(params.associateWith { TypeRef.Star })

    /**
     * 创建将所有元素类型参数替换为自身的元素引用。
     */
    fun withSelfArgs(): ElementRef<Element> = copy(params.associateWith { it })

    /**
     * 将类型变量加入当前元素的类型参数列表。
     */
    operator fun TypeVariable.unaryPlus() = apply {
        params.add(this)
    }

    /**
     * 将字段加入当前元素的直接字段集合。
     */
    operator fun Field.unaryPlus() = apply {
        fields.add(this)
    }

    /**
     * 返回当前元素类型名，便于配置错误和调试输出。
     */
    override fun toString(): String = buildString { renderTo(this, ImportCollecting.Empty) }
}

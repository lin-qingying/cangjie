package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.type.model.AnnotationMarker
import org.cangnova.cangjie.util.AttributeArrayOwner
import org.cangnova.cangjie.util.TypeRegistry
import org.cangnova.cangjie.utils.addIfNotNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.get
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass

/**
 * Cone 类型属性的基类。
 *
 * 属性用于在类型对象上附加注解、缩写类型、推断保留信息等额外语义。每个属性通过
 * [key] 在 [ConeAttributes] 中唯一注册，并提供 union/intersect/add/subtype 等组合规则。
 */
abstract class ConeAttribute<out T : ConeAttribute<T>> : AnnotationMarker {
    /**
     * 计算当前属性与 [other] 的并集结果。
     */
    abstract fun union(other: @UnsafeVariance T?): T?

    /**
     * 计算当前属性与 [other] 的交集结果。
     */
    abstract fun intersect(other: @UnsafeVariance T?): T?

    /**
     * 决定 typealias 展开链上多个同类属性如何累加。
     *
     * 例如 `typealias B = @SomeAttribute(1) A`、`typealias C = @SomeAttribute(2) B`，
     * 计算 `C` 的展开类型属性时需要把 `@SomeAttribute(2)` 加到 `@SomeAttribute(1)` 上。
     */
    abstract fun add(other: @UnsafeVariance T?): T?

    /**
     * 判断当前属性是否是 [other] 的子属性。
     */
    abstract fun isSubtypeOf(other: @UnsafeVariance T?): Boolean

    /**
     * 返回调试输出文本。
     */
    abstract override fun toString(): String

    /**
     * 返回面向用户可读渲染的文本。
     */
    open fun renderForReadability(): String? = null

    /**
     * 当前属性是否可靠实现了 [equals] 与 [hashCode] 协议。
     *
     * 返回 `true` 时，[ConeAttributes.definitelyDifferFrom] 会用结构相等比较该属性。
     */
    open val implementsEquality: Boolean get() = false

    /**
     * 当前属性在属性集合中的唯一键。
     */
    abstract val key: KClass<out T>

    /**
     * 推断声明返回类型时是否保留该属性。
     */
    abstract val keepInInferredDeclarationType: Boolean
}

/**
 * 内部携带 [ConeCangJieType] 的属性。
 *
 * 该属性中的 [coneType] 与它附着的外层类型存在语义关联，因此当外层类型被替换、
 * 展开或变换时，属性中的类型也必须同步变换。
 */
abstract class ConeAttributeWithConeType<out T : ConeAttributeWithConeType<T>> : ConeAttribute<T>() {
    /**
     * 属性携带的内部 Cone 类型。
     */
    abstract val coneType: ConeCangJieType

    /**
     * 使用 [newType] 复制当前属性。
     */
    abstract fun copyWith(newType: ConeCangJieType): T
}

/**
 * 对携带类型的属性应用 [transform]。
 *
 * 如果变换结果为空，属性被移除；如果结果与原类型相同，直接复用当前属性。
 */
inline fun <T : ConeAttributeWithConeType<T>> ConeAttributeWithConeType<T>.transformOrNull(
    transform: (ConeCangJieType) -> ConeCangJieType?,
): ConeAttributeWithConeType<T>? {
    val transformedType = transform(coneType) ?: return null
    if (transformedType == coneType) return this

    // If the type contains the attribute itself, use the nested type from the attribute to prevent exponential growth.
    // As an example, consider a substitution {T -> Attr(Foo) Bar} applied to a type `Attr(T) T`.
    // If we don't flatten the attribute chain, we would get `Attr(Attr(Foo) Bar) Bar`.
    return copyWith(transformedType.attributes[key]?.coneType ?: transformedType)
}

/**
 * Cone 属性集合使用的属性键类型。
 */
typealias ConeAttributeKey = KClass<out ConeAttribute<*>>

/**
 * Cone 类型属性集合。
 *
 * 集合以属性类型作为键，并复用 [AttributeArrayOwner] 的数组存储，保证类型热路径上
 * 属性读取不需要遍历列表。
 */
class ConeAttributes private constructor(attributes: List<ConeAttribute<*>>) : AttributeArrayOwner<ConeAttribute<*>, ConeAttribute<*>>(),
    Iterable<ConeAttribute<*>> {

    /**
     * Cone 属性访问器工厂和全局注册表。
     */
    companion object : TypeRegistry<ConeAttribute<*>, ConeAttribute<*>>() {
        /**
         * 为属性类型 [T] 生成可为空读取访问器。
         */
        inline fun <reified T : ConeAttribute<T>> attributeAccessor(): ReadOnlyProperty<ConeAttributes, T?> {
            @Suppress("UNCHECKED_CAST")
            return generateNullableAccessor(T::class) as ReadOnlyProperty<ConeAttributes, T?>
        }

        /**
         * 空属性集合共享实例。
         */
        val Empty: ConeAttributes = ConeAttributes(emptyList())

        /**
         * 根据 [attributes] 创建属性集合。
         */
        fun create(attributes: List<ConeAttribute<*>>): ConeAttributes {
            return if (attributes.isEmpty()) {
                Empty
            } else {
                ConeAttributes(attributes)
            }
        }

        /**
         * 为属性类型键分配稳定下标。
         */
        override fun ConcurrentHashMap<String, Int>.customComputeIfAbsent(
            key: String,
            compute: (String) -> Int,
        ): Int {
            return this.computeIfAbsent(key, compute)
        }
    }

    /**
     * 单属性集合构造器。
     */
    private constructor(attribute: ConeAttribute<*>) : this(listOf(attribute))

    init {
        for (attribute in attributes) {
            registerComponent(attribute.key, attribute)
        }
    }

    /**
     * 计算两个属性集合的并集。
     */
    fun union(other: ConeAttributes): ConeAttributes {
        return perform(other) { this.union(it) }
    }

    /**
     * 计算两个属性集合的交集。
     */
    fun intersect(other: ConeAttributes): ConeAttributes {
        return perform(other) { this.intersect(it) }
    }

    /**
     * 将 [other] 属性集合累加到当前集合上。
     */
    fun add(other: ConeAttributes): ConeAttributes {
        return perform(other) { this.add(it) }
    }

    /**
     * 将单个 [attribute] 累加到当前集合上。
     */
    fun add(attribute: ConeAttribute<*>): ConeAttributes {
        return add(create(listOf(attribute)))
    }

    /**
     * 判断集合中是否包含与 [attribute] 同键的属性。
     */
    operator fun contains(attribute: ConeAttribute<*>): Boolean {
        return contains(attribute.key)
    }

    /**
     * 判断集合中是否包含 [attributeKey] 对应的属性。
     */
    operator fun contains(attributeKey: KClass<out ConeAttribute<*>>): Boolean {
        return get(attributeKey) != null
    }

    /**
     * 读取 [attributeKey] 对应的属性实例。
     */
    operator fun <T : ConeAttribute<*>> get(attributeKey: KClass<T>) : T? {
        val index = Companion.getId(attributeKey)
        @Suppress("UNCHECKED_CAST")
        return arrayMap[index] as T?
    }

    /**
     * 从集合中移除 [attribute] 实例。
     */
    fun remove(attribute: ConeAttribute<*>): ConeAttributes {
        if (isEmpty()) return this
        val attributes = arrayMap.filter { it != attribute }
        if (attributes.size == arrayMap.size) return this
        return create(attributes)
    }

    /**
     * 从集合中移除 [key] 对应的属性。
     */
    fun remove(key: ConeAttributeKey): ConeAttributes {
        if (isEmpty()) return this
        val attributes = arrayMap.filter { it.key != key }
        if (attributes.size == arrayMap.size) return this
        return create(attributes)
    }

    /**
     * 过滤出推断声明返回类型时必须保留的属性。
     */
    fun filterNecessaryToKeep(): ConeAttributes {
        return if (all { it.keepInInferredDeclarationType }) this
        else create(filter { it.keepInInferredDeclarationType })
    }

    /**
     * 按属性键逐项执行 [op]，并创建新的属性集合。
     */
    private inline fun perform(other: ConeAttributes, op: ConeAttribute<*>.(ConeAttribute<*>?) -> ConeAttribute<*>?): ConeAttributes {
        if (this.isEmpty() && other.isEmpty()) return this
        val attributes = mutableListOf<ConeAttribute<*>>()
        for (index in Companion.allValuesThreadUnsafeForRendering().values) {
            val a = arrayMap[index]
            val b = other.arrayMap[index]
            val res = if (a == null) b?.op(a) else a.op(b)
            attributes.addIfNotNull(res)
        }
        return create(attributes)
    }

    /**
     * 判断当前属性集合是否可以确定不同于 [other]。
     *
     * 返回 `true` 表示某个可靠实现结构相等的属性不同；返回 `false` 不保证两个集合相等，
     * 因为部分属性可能没有声明 [ConeAttribute.implementsEquality]。
     *
     * @see org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl.equals
     */
    infix fun definitelyDifferFrom(other: ConeAttributes): Boolean {
        if (this === other) return false
        if (this.isEmpty() && other.isEmpty()) return false

        for (index in Companion.allValuesThreadUnsafeForRendering().values) {
            val a = arrayMap[index]
            val b = other.arrayMap[index]

            if (a == null && b == null) continue
            if ((a ?: b)!!.implementsEquality) {
                if ((a == null) != (b == null)) return true
                if (a != b) return true
            }
        }

        return false
    }

    /**
     * 对所有 [ConeAttributeWithConeType] 属性中的内部类型应用 [transform]。
     *
     * 返回新的属性集合；若没有属性被变换，返回 `null`。
     */
    inline fun transformTypesWith(transform: (ConeCangJieType) -> ConeCangJieType?): ConeAttributes? {
        if (isEmpty()) return null

        // List will be allocated on demand
        var newList: MutableList<ConeAttribute<*>>? = null
        var hasDifference = false

        for ((i, attr) in this.withIndex()) {
            if (attr !is ConeAttributeWithConeType) continue
            val substitutedAttribute = attr.transformOrNull(transform) ?: continue
            if (newList == null) {
                newList = this.toMutableList()
            }
            newList[i] = substitutedAttribute
            hasDifference = hasDifference || substitutedAttribute != attr
        }

        if (newList != null && !hasDifference) {
            return this
        }

        return newList?.let(Companion::create)
    }

    /**
     * 当前属性集合使用的注册表。
     */
    override val typeRegistry: TypeRegistry<ConeAttribute<*>, ConeAttribute<*>>
        get() = Companion
}

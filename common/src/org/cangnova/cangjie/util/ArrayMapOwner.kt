package org.cangnova.cangjie.util

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * 抽象数组映射持有者（对齐 Kotlin 的 AbstractArrayMapOwner）。
 *
 * 拥有 ArrayMap + TypeRegistry，提供类型安全的组件注册和查询。
 */
abstract class AbstractArrayMapOwner<K : Any, V : Any> : Iterable<V> {
    /**
     * 存储组件值的数组映射。
     */
    protected abstract val arrayMap: ArrayMap<V>
    /**
     * 将组件类型映射为数组索引的注册表。
     */
    protected abstract val typeRegistry: TypeRegistry<K, V>

    /**
     * 基于注册表 ID 访问数组映射值的委托基类。
     */
    abstract class AbstractArrayMapAccessor<K : Any, V : Any, T : V>(
        /**
         * 组件在数组映射中的整数 ID。
         */
        protected val id: Int,
    ) {
        /**
         * 从 owner 中读取该访问器对应的组件值。
         */
        protected fun extractValue(thisRef: AbstractArrayMapOwner<K, V>): T? {
            @Suppress("UNCHECKED_CAST")
            return thisRef.arrayMap[id] as T?
        }
    }

    /**
     * 注册指定类型名称对应的组件值。
     */
    protected abstract fun registerComponent(keyQualifiedName: String, value: V)

    /**
     * 根据 KClass 注册组件值。
     */
    protected fun registerComponent(tClass: KClass<out K>, value: V) {
        registerComponent(tClass.qualifiedName!!, value)
    }

    /**
     * 遍历所有已注册组件值。
     */
    final override fun iterator(): Iterator<V> = arrayMap.iterator()

    /**
     * 判断当前 owner 是否没有任何组件。
     */
    fun isEmpty(): Boolean = arrayMap.size == 0

    /**
     * 判断当前 owner 是否至少包含一个组件。
     */
    fun isNotEmpty(): Boolean = arrayMap.size != 0

    /**
     * 按数组索引读取组件值。
     */
    operator fun get(index: Int): V? = arrayMap[index]
}

/**
 * 非空组件访问器委托（对齐 Kotlin 的 ArrayMapAccessor）。
 */
class ArrayMapAccessor<K : Any, V : Any, T : V>(
    /**
     * 组件类型的限定名，用于错误消息和注册表索引。
     */
    private val keyQualifiedName: String,
    id: Int,
    /**
     * 组件缺失时可返回的默认值。
     */
    val default: T? = null,
) : AbstractArrayMapOwner.AbstractArrayMapAccessor<K, V, T>(id), ReadOnlyProperty<AbstractArrayMapOwner<K, V>, T> {
    /**
     * 读取非空组件；若未注册且没有默认值则报告 owner 配置错误。
     */
    override fun getValue(thisRef: AbstractArrayMapOwner<K, V>, property: KProperty<*>): T {
        return extractValue(thisRef)
            ?: default
            ?: error("No '$keyQualifiedName'($id) in array owner: $thisRef")
    }
}

/**
 * 可空组件访问器委托（对齐 Kotlin 的 NullableArrayMapAccessor）。
 */
class NullableArrayMapAccessor<K : Any, V : Any, T : V>(
    id: Int,
) : AbstractArrayMapOwner.AbstractArrayMapAccessor<K, V, T>(id), ReadOnlyProperty<AbstractArrayMapOwner<K, V>, V?> {
    /**
     * 读取可空组件；组件缺失时返回 null。
     */
    override fun getValue(thisRef: AbstractArrayMapOwner<K, V>, property: KProperty<*>): T? {
        return extractValue(thisRef)
    }
}

/**
 * 类型注册表（对齐 Kotlin 的 TypeRegistry）。
 *
 * 将 KClass 的 qualifiedName 映射为自增 Int ID，
 * 生成 ArrayMapAccessor 委托属性，实现 O(1) 组件查找。
 */
abstract class TypeRegistry<K : Any, V : Any> {
    /**
     * 组件类型限定名到数组索引的映射。
     */
    private val idPerType = ConcurrentHashMap<String, Int>()
    /**
     * 为新组件类型分配索引的自增计数器。
     */
    private val idCounter = AtomicInteger(0)

    /**
     * 为指定 KClass 生成非空组件访问器。
     */
    fun <T : V, KK : K> generateAccessor(kClass: KClass<KK>, default: T? = null): ArrayMapAccessor<K, V, T> {
        return ArrayMapAccessor(kClass.qualifiedName!!, getId(kClass), default)
    }

    /*
     * This function is needed for compatibility with JDK 6
     * ArrayMap and other infrastructure is used in KotlinType, declared in :core:descriptors module, which is
     *   compiled against JDK 6 (because it's used in kotlin-reflect, which is still compatible with Java 6)
     * So the problem is that JDK 6 does not have thread-safe computeIfAbsent for ConcurrentHashMap,
     *   and we need this method to add ability to provide thread-safe implementation by hand
     */
    /**
     * 线程安全地为指定 key 计算并写入缺失的 ID。
     */
    abstract fun ConcurrentHashMap<String, Int>.customComputeIfAbsent(
        key: String,
        compute: (String) -> Int
    ): Int
    /**
     * 为指定类型限定名生成非空组件访问器。
     */
    fun <T : V> generateAccessor(keyQualifiedName: String, default: T? = null): ArrayMapAccessor<K, V, T> {
        return ArrayMapAccessor(keyQualifiedName, getId(keyQualifiedName), default)
    }

    /**
     * 为指定 KClass 生成可空组件访问器。
     */
    fun <T : V, KK : K> generateNullableAccessor(kClass: KClass<KK>): NullableArrayMapAccessor<K, V, T> {
        return NullableArrayMapAccessor(getId(kClass))
    }

    /**
     * 为指定 KClass 生成不约束值类型的可空组件访问器。
     */
    fun <KK : K> generateAnyNullableAccessor(kClass: KClass<KK>): NullableArrayMapAccessor<K, V, *> {
        return NullableArrayMapAccessor(getId(kClass))
    }

    /**
     * 获取指定 KClass 对应的数组索引。
     */
    fun <T : K> getId(kClass: KClass<T>): Int {
        return getId(kClass.qualifiedName!!)
    }

    /**
     * 获取指定类型限定名对应的数组索引，不存在时分配新索引。
     */
    fun getId(keyQualifiedName: String): Int {
        return idPerType.computeIfAbsent(keyQualifiedName) { idCounter.getAndIncrement() }
    }

    /**
     * 返回注册表当前内容；仅用于渲染或调试，不保证并发一致快照。
     */
    fun allValuesThreadUnsafeForRendering(): Map<String, Int> {
        return idPerType
    }

    /**
     * 当前注册表中所有已分配索引。
     */
    protected val indices: Collection<Int>
        get() = idPerType.values
}

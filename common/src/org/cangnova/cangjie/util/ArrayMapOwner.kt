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
    protected abstract val arrayMap: ArrayMap<V>
    protected abstract val typeRegistry: TypeRegistry<K, V>

    abstract class AbstractArrayMapAccessor<K : Any, V : Any, T : V>(
        protected val id: Int,
    ) {
        protected fun extractValue(thisRef: AbstractArrayMapOwner<K, V>): T? {
            @Suppress("UNCHECKED_CAST")
            return thisRef.arrayMap[id] as T?
        }
    }

    protected abstract fun registerComponent(keyQualifiedName: String, value: V)

    protected fun registerComponent(tClass: KClass<out K>, value: V) {
        registerComponent(tClass.qualifiedName!!, value)
    }

    final override fun iterator(): Iterator<V> = arrayMap.iterator()

    fun isEmpty(): Boolean = arrayMap.size == 0

    fun isNotEmpty(): Boolean = arrayMap.size != 0

    operator fun get(index: Int): V? = arrayMap[index]
}

/**
 * 非空组件访问器委托（对齐 Kotlin 的 ArrayMapAccessor）。
 */
class ArrayMapAccessor<K : Any, V : Any, T : V>(
    private val keyQualifiedName: String,
    id: Int,
    val default: T? = null,
) : AbstractArrayMapOwner.AbstractArrayMapAccessor<K, V, T>(id), ReadOnlyProperty<AbstractArrayMapOwner<K, V>, T> {
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
    private val idPerType = ConcurrentHashMap<String, Int>()
    private val idCounter = AtomicInteger(0)

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
    abstract fun ConcurrentHashMap<String, Int>.customComputeIfAbsent(
        key: String,
        compute: (String) -> Int
    ): Int
    fun <T : V> generateAccessor(keyQualifiedName: String, default: T? = null): ArrayMapAccessor<K, V, T> {
        return ArrayMapAccessor(keyQualifiedName, getId(keyQualifiedName), default)
    }

    fun <T : V, KK : K> generateNullableAccessor(kClass: KClass<KK>): NullableArrayMapAccessor<K, V, T> {
        return NullableArrayMapAccessor(getId(kClass))
    }

    fun <T : K> getId(kClass: KClass<T>): Int {
        return getId(kClass.qualifiedName!!)
    }

    fun getId(keyQualifiedName: String): Int {
        return idPerType.computeIfAbsent(keyQualifiedName) { idCounter.getAndIncrement() }
    }

    fun allValuesThreadUnsafeForRendering(): Map<String, Int> {
        return idPerType
    }

    protected val indices: Collection<Int>
        get() = idPerType.values
}

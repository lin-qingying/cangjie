package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirDeclarationDataKey
import org.cangnova.cangjie.cfir.ConeTypeRegistry
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.util.ArrayMap
import org.cangnova.cangjie.util.AttributeArrayOwner
import org.cangnova.cangjie.util.NullableArrayMapAccessor
import org.cangnova.cangjie.util.TypeRegistry
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * CFIR 声明上的扩展属性容器。
 *
 * 该容器用于承载不属于生成式声明树主结构、但需要随声明一起传递的框架级数据，
 * 例如 checker 辅助诊断数据、lazy resolve 附加状态或符号侧查询缓存键。
 *
 * 线程安全策略与 Kotlin FIR 一致：该容器自身不提供并发写保护，写入应发生在声明发布前，
 * 或由对应 resolve phase / low-level lazy resolve 锁协议保证互斥。
 */
class CfirDeclarationAttributes : AttributeArrayOwner<CfirDeclarationDataKey, Any> {
    /**
     * 声明属性使用的全局类型注册表。
     *
     * 每一种 [CfirDeclarationDataKey] 通过该注册表分配数组槽位，使属性读写保持低开销。
     */
    override val typeRegistry: TypeRegistry<CfirDeclarationDataKey, Any>
        get() = CfirDeclarationDataRegistry

    /**
     * 创建空的声明属性容器。
     */
    constructor() : super()

    /**
     * 基于已有数组映射创建副本容器。
     *
     * 该构造器只用于 [copy]，避免调用方绕过属性注册表直接共享内部存储。
     */
    private constructor(arrayMap: ArrayMap<Any>) : super(arrayMap)

    /**
     * 写入或移除指定键对应的属性值。
     *
     * 传入 `null` 表示删除该键，非空值会注册到 [CfirDeclarationDataRegistry] 分配的槽位。
     */
    internal operator fun set(key: KClass<out CfirDeclarationDataKey>, value: Any?) {
        if (value == null) {
            removeComponent(key)
        } else {
            registerComponent(key, value)
        }
    }

    /**
     * 复制当前属性容器及其数组映射。
     *
     * 返回值与当前容器不共享可变 [ArrayMap]，适合声明复制、替换 owner 或生成 synthetic 声明时使用。
     */
    fun copy(): CfirDeclarationAttributes = CfirDeclarationAttributes(arrayMap.copy())

    /**
     * 声明属性容器的工厂与常用常量。
     */
    companion object {
        /**
         * 新建一个空属性容器。
         *
         * 这里每次返回新实例，而不是共享单例，避免调用方对空属性容器写入时污染其他声明。
         */
        val EMPTY: CfirDeclarationAttributes
            get() = CfirDeclarationAttributes()
    }
}

/**
 * CFIR 声明属性键的注册表与委托访问器工厂。
 *
 * 声明侧、符号侧和属性容器侧都通过这里创建访问器，从而保证同一 [CfirDeclarationDataKey]
 * 在不同宿主对象上访问的是同一数组槽位。
 */
object CfirDeclarationDataRegistry : ConeTypeRegistry<CfirDeclarationDataKey, Any>() {
    /**
     * 创建绑定到 [CfirDeclaration] 的属性委托。
     */
    fun <K : CfirDeclarationDataKey> data(key: K): DeclarationDataAccessor {
        val keyClass = key::class
        return DeclarationDataAccessor(generateAnyNullableAccessor(keyClass), keyClass)
    }

    /**
     * 创建绑定到 [CfirBasedSymbol] 的只读属性委托。
     *
     * 符号访问器实际读取的是符号绑定声明上的 [CfirDeclaration.attributes]。
     */
    fun <K : CfirDeclarationDataKey> symbolAccessor(key: K): SymbolDataAccessor {
        val keyClass = key::class
        return SymbolDataAccessor(generateAnyNullableAccessor(keyClass), keyClass)
    }

    /**
     * 创建直接绑定到 [CfirDeclarationAttributes] 容器的可读写属性委托。
     */
    fun <K : CfirDeclarationDataKey, V : Any> attributesAccessor(key: K): ReadWriteProperty<CfirDeclarationAttributes, V?> {
        val keyClass = key::class
        return AttributeDataAccessor(generateNullableAccessor(keyClass), keyClass)
    }

    /**
     * 声明对象上的属性委托。
     *
     * @property dataAccessor 底层数组访问器。
     * @property key 当前委托绑定的属性键类型。
     */
    class DeclarationDataAccessor(
        private val dataAccessor: NullableArrayMapAccessor<CfirDeclarationDataKey, Any, *>,
        val key: KClass<out CfirDeclarationDataKey>,
    ) {
        /**
         * 从声明属性容器中读取委托值。
         */
        operator fun <V> getValue(thisRef: CfirDeclaration, property: KProperty<*>): V? {
            @Suppress("UNCHECKED_CAST")
            return dataAccessor.getValue(thisRef.attributes, property) as? V
        }

        /**
         * 将委托值写入声明属性容器；`null` 表示移除该属性。
         */
        operator fun <V> setValue(thisRef: CfirDeclaration, property: KProperty<*>, value: V?) {
            thisRef.attributes[key] = value
        }
    }

    /**
     * 符号对象上的声明属性只读委托。
     *
     * @property dataAccessor 底层数组访问器。
     * @property key 当前委托绑定的属性键类型。
     */
    class SymbolDataAccessor(
        private val dataAccessor: NullableArrayMapAccessor<CfirDeclarationDataKey, Any, *>,
        val key: KClass<out CfirDeclarationDataKey>,
    ) {
        /**
         * 通过符号绑定的 CFIR 声明读取属性值。
         */
        operator fun <V> getValue(thisRef: CfirBasedSymbol<*>, property: KProperty<*>): V? {
            @Suppress("UNCHECKED_CAST")
            return dataAccessor.getValue(thisRef.cfir.attributes, property) as? V
        }
    }

    /**
     * 直接作用在 [CfirDeclarationAttributes] 上的可读写委托。
     *
     * @property dataAccessor 底层数组访问器。
     * @property key 当前委托绑定的属性键类型。
     */
    private class AttributeDataAccessor<V : Any>(
        val dataAccessor: NullableArrayMapAccessor<CfirDeclarationDataKey, Any, V>,
        val key: KClass<out CfirDeclarationDataKey>,
    ) : ReadWriteProperty<CfirDeclarationAttributes, V?> {
        /**
         * 从属性容器读取委托值。
         */
        override fun getValue(thisRef: CfirDeclarationAttributes, property: KProperty<*>): V? {
            return dataAccessor.getValue(thisRef, property)
        }

        /**
         * 将委托值写入属性容器；`null` 表示移除该属性。
         */
        override fun setValue(thisRef: CfirDeclarationAttributes, property: KProperty<*>, value: V?) {
            thisRef[key] = value
        }
    }
}

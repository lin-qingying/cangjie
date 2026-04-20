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
 * Thread-safety matches Kotlin FIR: mutations are not thread-safe and should happen
 * before publication or under a phase/lock discipline.
 */
class CfirDeclarationAttributes : AttributeArrayOwner<CfirDeclarationDataKey, Any> {
    override val typeRegistry: TypeRegistry<CfirDeclarationDataKey, Any>
        get() = CfirDeclarationDataRegistry

    constructor() : super()

    private constructor(arrayMap: ArrayMap<Any>) : super(arrayMap)

    internal operator fun set(key: KClass<out CfirDeclarationDataKey>, value: Any?) {
        if (value == null) {
            removeComponent(key)
        } else {
            registerComponent(key, value)
        }
    }

    fun copy(): CfirDeclarationAttributes = CfirDeclarationAttributes(arrayMap.copy())

    companion object {
        val EMPTY: CfirDeclarationAttributes
            get() = CfirDeclarationAttributes()
    }
}

object CfirDeclarationDataRegistry : ConeTypeRegistry<CfirDeclarationDataKey, Any>() {
    fun <K : CfirDeclarationDataKey> data(key: K): DeclarationDataAccessor {
        val keyClass = key::class
        return DeclarationDataAccessor(generateAnyNullableAccessor(keyClass), keyClass)
    }

    fun <K : CfirDeclarationDataKey> symbolAccessor(key: K): SymbolDataAccessor {
        val keyClass = key::class
        return SymbolDataAccessor(generateAnyNullableAccessor(keyClass), keyClass)
    }

    fun <K : CfirDeclarationDataKey, V : Any> attributesAccessor(key: K): ReadWriteProperty<CfirDeclarationAttributes, V?> {
        val keyClass = key::class
        return AttributeDataAccessor(generateNullableAccessor(keyClass), keyClass)
    }

    class DeclarationDataAccessor(
        private val dataAccessor: NullableArrayMapAccessor<CfirDeclarationDataKey, Any, *>,
        val key: KClass<out CfirDeclarationDataKey>,
    ) {
        operator fun <V> getValue(thisRef: CfirDeclaration, property: KProperty<*>): V? {
            @Suppress("UNCHECKED_CAST")
            return dataAccessor.getValue(thisRef.attributes, property) as? V
        }

        operator fun <V> setValue(thisRef: CfirDeclaration, property: KProperty<*>, value: V?) {
            thisRef.attributes[key] = value
        }
    }

    class SymbolDataAccessor(
        private val dataAccessor: NullableArrayMapAccessor<CfirDeclarationDataKey, Any, *>,
        val key: KClass<out CfirDeclarationDataKey>,
    ) {
        operator fun <V> getValue(thisRef: CfirBasedSymbol<*>, property: KProperty<*>): V? {
            @Suppress("UNCHECKED_CAST")
            return dataAccessor.getValue(thisRef.cfir.attributes, property) as? V
        }
    }

    private class AttributeDataAccessor<V : Any>(
        val dataAccessor: NullableArrayMapAccessor<CfirDeclarationDataKey, Any, V>,
        val key: KClass<out CfirDeclarationDataKey>,
    ) : ReadWriteProperty<CfirDeclarationAttributes, V?> {
        override fun getValue(thisRef: CfirDeclarationAttributes, property: KProperty<*>): V? {
            return dataAccessor.getValue(thisRef, property)
        }

        override fun setValue(thisRef: CfirDeclarationAttributes, property: KProperty<*>, value: V?) {
            thisRef[key] = value
        }
    }
}

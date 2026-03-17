package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.util.TypeRegistry
import java.util.concurrent.ConcurrentHashMap

abstract class ConeTypeRegistry<K : Any, V : Any> : TypeRegistry<K, V>() {
    override fun ConcurrentHashMap<String, Int>.customComputeIfAbsent(
        key: String,
        compute: (String) -> Int
    ): Int {
        return this.computeIfAbsent(key, compute)
    }
}
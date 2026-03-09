package org.cangnova.cangjie.util

import kotlin.reflect.KClass

/**
 * 基于 ArrayMap 的组件持有者（对齐 Kotlin 的 ComponentArrayOwner）。
 *
 * 用于 session 等需要高频组件查找的实体，O(1) 注册和查找。
 */
abstract class ComponentArrayOwner<K : Any, V : Any> : AbstractArrayMapOwner<K, V>() {
    final override val arrayMap: ArrayMap<V> = ArrayMapImpl()

    final override fun registerComponent(keyQualifiedName: String, value: V) {
        val id = typeRegistry.getId(keyQualifiedName)
        try {
            arrayMap[id] = value
        } catch (e: Exception) {
            throw RuntimeException(createDiagnosticMessage(id, keyQualifiedName), e)
        }
    }

    protected operator fun get(key: KClass<out K>): V {
        getOrNull(key)?.let { return it }
        val id = typeRegistry.getId(key)
        error("No '$key'($id) component in array: $this")
    }

    protected fun getOrNull(key: KClass<out K>): V? {
        val id = typeRegistry.getId(key)
        return arrayMap[id]
    }

    private fun createDiagnosticMessage(id: Int, keyQualifiedName: String): String = buildString {
        appendLine("Error occurred during registration of component in array")
        appendLine("Currently registered")
        appendLine("  $id: $keyQualifiedName")
        appendLine("Registrar:")
        for ((kClass, x) in typeRegistry.allValuesThreadUnsafeForRendering()) {
            appendLine("  $x: $kClass")
        }
        appendLine("Array map:")
        for (i in 0 until arrayMap.size) {
            val element: Any? = arrayMap[i]
            appendLine("  $i: ${element?.let { it::class }}")
        }
    }
}

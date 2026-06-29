package org.cangnova.cangjie.util

import kotlin.reflect.KClass

/**
 * 基于 ArrayMap 的组件持有者（对齐 Kotlin 的 ComponentArrayOwner）。
 *
 * 用于 session 等需要高频组件查找的实体，O(1) 注册和查找。
 */
abstract class ComponentArrayOwner<K : Any, V : Any> : AbstractArrayMapOwner<K, V>() {
    /**
     * 保存组件实例的紧凑数组映射。
     */
    final override val arrayMap: ArrayMap<V> = ArrayMapImpl()

    /**
     * 按组件类型的限定名注册组件实例。
     */
    final override fun registerComponent(keyQualifiedName: String, value: V) {
        val id = typeRegistry.getId(keyQualifiedName)
        try {
            arrayMap[id] = value
        } catch (e: Exception) {
            throw RuntimeException(createDiagnosticMessage(id, keyQualifiedName), e)
        }
    }

    /**
     * 读取指定类型的必需组件，缺失时抛出包含注册表信息的错误。
     */
    protected operator fun get(key: KClass<out K>): V {
        getOrNull(key)?.let { return it }
        val id = typeRegistry.getId(key)
        error("No '$key'($id) component in array: $this")
    }

    /**
     * 读取指定类型的可选组件，未注册时返回 null。
     */
    protected fun getOrNull(key: KClass<out K>): V? {
        val id = typeRegistry.getId(key)
        return arrayMap[id]
    }

    /**
     * 构造组件注册失败时的诊断文本，包含当前注册表与数组内容。
     */
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

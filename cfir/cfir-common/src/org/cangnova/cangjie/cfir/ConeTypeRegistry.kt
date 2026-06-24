package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.util.TypeRegistry
import java.util.concurrent.ConcurrentHashMap

/**
 * CFIR 层统一使用的类型注册表基类。
 *
 * 该注册表复用 [TypeRegistry] 的数组下标分配机制，并在 JVM 上直接委托
 * [ConcurrentHashMap.computeIfAbsent]，用于 session component、module capability 等
 * 编译器服务的稳定 O(1) 访问器生成。
 */
abstract class ConeTypeRegistry<K : Any, V : Any> : TypeRegistry<K, V>() {
    /**
     * 为给定字符串键计算并缓存类型下标。
     *
     * CFIR session 初始化阶段可能由不同组件并发请求访问器，使用
     * [ConcurrentHashMap.computeIfAbsent] 保证同一个键只落到一个稳定下标。
     */
    override fun ConcurrentHashMap<String, Int>.customComputeIfAbsent(
        key: String,
        compute: (String) -> Int
    ): Int {
        return this.computeIfAbsent(key, compute)
    }
}

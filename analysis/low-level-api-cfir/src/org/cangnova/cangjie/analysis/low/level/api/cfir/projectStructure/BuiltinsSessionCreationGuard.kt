package org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure

/**
 * 建 session 期间的同线程重入守卫。
 *
 * session 的创建过程（注册组件、建库符号提供器、解析 builtins 搜索根）可能同线程回到
 * “取这个 session” 的入口。若不做任何处理，只有两条出路：
 *
 * - 直接递归：缓存用的 `ConcurrentHashMap.getOrPut` 在映射函数里访问同一个 map 会抛
 *   `IllegalStateException: Recursive update`，退化成不可诊断的崩溃；
 * - 静默拿到空 session：调用方拿到没有注册组件的 session，等到后面读 `cfirProvider` /
 *   `symbolProvider` 时才在离创建点很远的地方报 “No component in array”。
 *
 * 这里显式记录“当前线程正在创建中的 (键, 值)”：
 * - 命中时调用方拿到的是**半成品 session**，即已经注册到重入那一刻的组件；
 * - 调用方只应使用重入点之前已注册的组件（IDE 公共组件、module data、作用域提供器），
 *   读后半段注册的 `CfirProvider` / `CfirSymbolProvider` 仍然会失败，错误信息里会带上
 *   session 的构造状态。
 *
 * 守卫只跟踪当前线程：不同线程的并发建 session 由缓存自己的可见性语义处理。
 * 不要换成对缓存 map 的 `computeIfAbsent`，那会在映射函数内再次访问同一个 map。
 */
internal class BuiltinsSessionCreationGuard<T : Any, K : Any> {
    /**
     * 当前线程正在创建中的键值对；未创建任何 session 时为 `null`。
     */
    private val inProgress = ThreadLocal<MutableMap<K, T>?>()

    /**
     * 返回当前线程上正在为 [key] 创建的半成品 session；没有则 `null`。
     */
    fun halfBakedSessionFor(key: K): T? = inProgress.get()?.get(key)

    /**
     * 在 [block] 执行期间把 [value] 记为 [key] 正在创建中，退出时（正常或异常）清除登记。
     */
    fun <R> withSessionUnderConstruction(key: K, value: T, block: () -> R): R {
        val sessions = inProgress.get() ?: mutableMapOf<K, T>().also { inProgress.set(it) }
        check(sessions.put(key, value) == null) {
            "Session for `$key` is already registered as under construction on thread ${Thread.currentThread().name}; " +
                "reentrant creation must be short-circuited by `halfBakedSessionFor` instead of recursing"
        }
        try {
            return block()
        } finally {
            sessions.remove(key)
            if (sessions.isEmpty()) {
                inProgress.remove()
            }
        }
    }

    /**
     * 当前线程是否正在创建任何 session；只用于诊断与断言。
     */
    fun hasSessionUnderConstruction(): Boolean = inProgress.get()?.isNotEmpty() == true
}
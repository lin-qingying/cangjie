package org.cangnova.cangjie.chir.core.pipeline

/**
 * CHIR 分析结果描述符。
 */
class ChirAnalysisDescriptor<T : Any>(
    /**
     * 分析结果名称。
     */
    val name: String,

    /**
     * 分析结果依赖的数据域集合。
     */
    val domains: Set<ChirDataDomain>,
) {
    init {
        require(name.isNotBlank()) { "analysis descriptor name must not be blank" }
        require(domains.isNotEmpty()) { "analysis descriptor domains must not be empty" }
    }
}

/**
 * CHIR 分析结果缓存。
 */
class ChirAnalysisCache {
    /**
     * 单条缓存记录。
     */
    private data class Entry(
        /**
         * 缓存值。
         */
        val value: Any,

        /**
         * 缓存值所属数据域集合。
         */
        val domains: Set<ChirDataDomain>,
    )

    /**
     * 分析名称到缓存记录的映射。
     */
    private val store = LinkedHashMap<String, Entry>()

    /**
     * 按描述符读取缓存分析结果。
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(descriptor: ChirAnalysisDescriptor<T>): T? {
        return store[descriptor.name]?.value as? T
    }

    /**
     * 写入分析结果缓存。
     */
    fun <T : Any> put(descriptor: ChirAnalysisDescriptor<T>, value: T) {
        store[descriptor.name] = Entry(value = value, domains = descriptor.domains)
    }

    /**
     * 失效所有依赖 [domains] 中任意数据域的缓存记录。
     */
    fun invalidate(domains: Set<ChirDataDomain>) {
        if (domains.isEmpty()) return
        val iterator = store.iterator()
        while (iterator.hasNext()) {
            val (_, entry) = iterator.next()
            if (entry.domains.any { it in domains }) {
                iterator.remove()
            }
        }
    }

    /**
     * 返回当前缓存记录数量。
     */
    fun size(): Int = store.size
}

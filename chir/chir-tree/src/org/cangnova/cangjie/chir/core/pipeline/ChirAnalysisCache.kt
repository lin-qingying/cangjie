package org.cangnova.cangjie.chir.core.pipeline

class ChirAnalysisDescriptor<T : Any>(
    val name: String,
    val domains: Set<ChirDataDomain>,
) {
    init {
        require(name.isNotBlank()) { "analysis descriptor name must not be blank" }
        require(domains.isNotEmpty()) { "analysis descriptor domains must not be empty" }
    }
}

class ChirAnalysisCache {
    private data class Entry(
        val value: Any,
        val domains: Set<ChirDataDomain>,
    )

    private val store = LinkedHashMap<String, Entry>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(descriptor: ChirAnalysisDescriptor<T>): T? {
        return store[descriptor.name]?.value as? T
    }

    fun <T : Any> put(descriptor: ChirAnalysisDescriptor<T>, value: T) {
        store[descriptor.name] = Entry(value = value, domains = descriptor.domains)
    }

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

    fun size(): Int = store.size
}

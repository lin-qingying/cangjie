package org.cangnova.cangjie.lsp.completion

import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot

/**
 * LSP 补全会话存储。
 *
 * 候选不是「当时返回、当时应用」：客户端可能过一会儿才接受某一个，
 * 因此服务端必须把这次补全的候选留到那一刻——但**不能无限留**。
 * 存储按会话键保留最近若干次补全，超过容量即淘汰最早的；被淘汰或显式失效的会话
 * 在 `applyCompletion` 时返回 `null`，由调用方转成**明确的过期错误**，
 * 绝不按过期区间去写文档（设计 §7.11.4）。
 *
 * 保留的是**不可变快照**而不是 `LookupElement` 或分析符号：
 * 快照本来就不持有会话（契约硬约束），跨请求持有它是安全的。
 *
 * 键由服务端生成（序号），客户端只负责原样回传：让客户端参与生成键
 * 会给「伪造一个指向别处区间的键」留下空间。
 */
class LspCompletionSessionStore(private val capacity: Int = DEFAULT_CAPACITY) {
    /**
     * 一次补全的会话。
     *
     * @param key 会话键，随候选项的 `data` 与命令实参回传。
     * @param uri 触发补全的文档。
     * @param version 生成候选时的文档版本；文档已变时应用前必须复验。
     * @param replaceStart 待替换前缀的起点（文档偏移）。
     * @param replaceEnd 待替换前缀的终点（文档偏移）。
     * @param items 该次补全的候选，顺序即最终展示顺序。
     */
    data class Session(
        val key: String,
        val uri: String,
        val version: Int,
        val replaceStart: Int,
        val replaceEnd: Int,
        val items: List<CompletionSnapshot>,
    ) {
        /**
         * 取第 [index] 个候选；越界返回 `null`（客户端可能回传过时下标）。
         */
        fun itemAt(index: Int): CompletionSnapshot? = items.getOrNull(index)
    }

    init {
        require(capacity > 0) { "会话容量必须为正，实际 $capacity" }
    }

    /** 按创建顺序保存的会话；超出容量时从最早的一端淘汰。 */
    private val sessions = LinkedHashMap<String, Session>()

    /** 会话键的单调序号。 */
    private var sequence = 0L

    /**
     * 记录一次补全并返回其会话。
     */
    @Synchronized
    fun create(
        uri: String,
        version: Int,
        replaceStart: Int,
        replaceEnd: Int,
        items: List<CompletionSnapshot>,
    ): Session {
        val session = Session(
            key = keyOf(++sequence),
            uri = uri,
            version = version,
            replaceStart = replaceStart,
            replaceEnd = replaceEnd,
            items = items,
        )
        sessions[session.key] = session
        while (sessions.size > capacity) {
            val eldest = sessions.keys.first()
            sessions.remove(eldest)
        }
        return session
    }

    /**
     * 取回会话；不存在（已过期或被淘汰）时返回 `null`。
     */
    @Synchronized
    fun find(key: String): Session? = sessions[key]

    /**
     * 丢弃某个文档的全部会话。
     *
     * 文档内容变化或关闭后，旧会话的替换区间与候选都可能不再成立，
     * 留着只会让用户接受一个落在错误区间的编辑。
     */
    @Synchronized
    fun invalidate(uri: String) {
        sessions.entries.removeIf { entry -> entry.value.uri == uri }
    }

    /**
     * 丢弃全部会话。
     */
    @Synchronized
    fun clear() {
        sessions.clear()
    }

    companion object {
        /**
         * 默认保留的会话数。
         *
         * 8 次足够覆盖「连续敲几下、回头选一个」的实际节奏，
         * 同时把常驻内存钉在一个很小的上界（每次会话只留候选快照）。
         */
        const val DEFAULT_CAPACITY: Int = 8

        /**
         * 会话键：序号带固定前缀，便于日志里一眼看出是本模块生成的。
         */
        private fun keyOf(sequence: Long): String = "cj-completion-$sequence"
    }
}

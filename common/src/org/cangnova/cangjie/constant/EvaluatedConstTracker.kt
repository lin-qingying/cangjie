package org.cangnova.cangjie.constant

/**
 * 编译期常量求值追踪器。
 *
 * 记录源码位置到常量值的映射，支持增量编译时的常量传播。
 * 对应 K2 的 EvaluatedConstTracker。
 */
abstract class EvaluatedConstTracker {

    /**
     * 常量追踪键。标识一个可关联常量值的源文件。
     *
     * [CfirFileSymbol][org.cangnova.cangjie.cfir.symbols.CfirFileSymbol] 实现此接口，
     * 以便按文件追踪常量。
     */
    interface Key {
        /** 转换为基于字符串路径的键，用于序列化和持久化 */
        fun asStringBasedKey(): StringBased?

        /** 基于文件路径的键 */
        data class StringBased(val path: String)
    }

    /** 保存指定源码区间的常量求值结果 */
    abstract fun save(startOffset: Int, endOffset: Int, fileKey: Key, constant: Any?)

    /** 加载指定源码区间的常量求值结果 */
    abstract fun load(startOffset: Int, endOffset: Int, fileKey: Key): Any?

    companion object {
        /**
         * 创建默认内存常量追踪器。
         */
        fun create(): EvaluatedConstTracker = EvaluatedConstTrackerImpl()
    }
}

/**
 * 使用 HashMap 存储常量求值结果的默认实现。
 */
private class EvaluatedConstTrackerImpl : EvaluatedConstTracker() {
    /**
     * 源码区间和文件键到常量值的映射。
     */
    private val map = HashMap<Triple<Int, Int, Key>, Any?>()

    /**
     * 保存指定源码区间的常量值。
     */
    override fun save(startOffset: Int, endOffset: Int, fileKey: Key, constant: Any?) {
        map[Triple(startOffset, endOffset, fileKey)] = constant
    }

    /**
     * 读取指定源码区间的常量值。
     */
    override fun load(startOffset: Int, endOffset: Int, fileKey: Key): Any? {
        return map[Triple(startOffset, endOffset, fileKey)]
    }
}

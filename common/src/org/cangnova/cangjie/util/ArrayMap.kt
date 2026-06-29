package org.cangnova.cangjie.util

/**
 * 基于数组的映射，以 Int 为索引（对齐 Kotlin 的 ArrayMap）。
 *
 * 用于 session 组件等需要高频查找的场景，O(1) 读写。
 */
sealed class ArrayMap<T : Any> : Iterable<T> {
    /**
     * 当前映射中已存储的非空元素数量。
     */
    abstract val size: Int

    /**
     * 将值写入指定整数索引。
     */
    abstract operator fun set(index: Int, value: T)
    /**
     * 读取指定整数索引上的值；未设置时返回 null。
     */
    abstract operator fun get(index: Int): T?

    /**
     * 创建当前映射的独立副本。
     */
    abstract fun copy(): ArrayMap<T>
}

/**
 * 不包含任何元素的数组映射实现。
 */
internal object EmptyArrayMap : ArrayMap<Nothing>() {
    /**
     * 空映射大小恒为 0。
     */
    override val size: Int get() = 0

    /**
     * 空映射不可写入。
     */
    override fun set(index: Int, value: Nothing) {
        throw IllegalStateException()
    }

    /**
     * 空映射读取任何索引都返回 null。
     */
    override fun get(index: Int): Nothing? = null

    /**
     * 空映射副本仍然是自身。
     */
    override fun copy(): ArrayMap<Nothing> = this

    /**
     * 返回永不产生元素的迭代器。
     */
    override fun iterator(): Iterator<Nothing> = object : Iterator<Nothing> {
        override fun hasNext(): Boolean = false
        override fun next(): Nothing = throw NoSuchElementException()
    }
}

/**
 * 只包含一个索引和值的紧凑数组映射实现。
 */
internal class OneElementArrayMap<T : Any>(val value: T, val index: Int) : ArrayMap<T>() {
    /**
     * 单元素映射大小恒为 1。
     */
    override val size: Int get() = 1

    /**
     * 单元素映射不可原地写入。
     */
    override fun set(index: Int, value: T) {
        throw IllegalStateException()
    }

    /**
     * 读取唯一索引上的值；其他索引返回 null。
     */
    override fun get(index: Int): T? =
        if (index == this.index) value else null

    /**
     * 创建同一索引和值的新单元素映射。
     */
    override fun copy(): ArrayMap<T> = OneElementArrayMap(value, index)

    /**
     * 返回只访问一次唯一值的迭代器。
     */
    override fun iterator(): Iterator<T> = object : Iterator<T> {
        /**
         * 唯一值是否尚未被迭代。
         */
        private var notVisited = true

        override fun hasNext(): Boolean = notVisited

        override fun next(): T {
            if (notVisited) {
                notVisited = false
                return value
            } else {
                throw NoSuchElementException()
            }
        }
    }
}

/**
 * 使用可增长数组存储稀疏整数索引的映射实现。
 */
internal class ArrayMapImpl<T : Any> private constructor(
    /**
     * 按整数索引存放值的底层数组。
     */
    private var data: Array<Any?>,
    initialSize: Int,
) : ArrayMap<T>() {
    companion object {
        private const val DEFAULT_SIZE = 20
        private const val INCREASE_K = 2
    }

    constructor() : this(arrayOfNulls<Any>(DEFAULT_SIZE), 0)

    /**
     * 当前底层数组中非空槽位数量。
     */
    override var size: Int = initialSize
        private set

    /**
     * 确保底层数组能容纳指定索引。
     */
    private fun ensureCapacity(index: Int) {
        if (data.size > index) return
        var newSize = data.size
        do {
            newSize *= INCREASE_K
        } while (newSize <= index)
        data = data.copyOf(newSize)
    }

    /**
     * 将值写入指定索引，并在新槽位写入时增加 size。
     */
    override operator fun set(index: Int, value: T) {
        ensureCapacity(index)
        if (data[index] == null) {
            size++
        }
        data[index] = value
    }

    /**
     * 读取指定索引上的值；索引越界或未写入时返回 null。
     */
    override operator fun get(index: Int): T? {
        @Suppress("UNCHECKED_CAST")
        return data.getOrNull(index) as T?
    }
    /**
     * 数组映射条目视图。
     */
    data class Entry<T>(override val key: Int, override val value: T) : Map.Entry<Int, T>

    /**
     * 复制底层数组并保留当前 size。
     */
    override fun copy(): ArrayMap<T> = ArrayMapImpl(data.copyOf(), size)
    /**
     * 返回所有非空槽位对应的条目列表。
     */
    fun entries(): List<Entry<T>> {
        @Suppress("UNCHECKED_CAST")
        return data.mapIndexedNotNull { index, value -> if (value != null) Entry(index, value as T) else null }
    }
    /**
     * 移除指定索引上的值，并在原槽位非空时减少 size。
     */
    fun remove(index: Int) {
        if (data[index] != null) {
            size--
        }
        data[index] = null
    }

    /**
     * 返回按数组索引顺序遍历非空值的迭代器。
     */
    override fun iterator(): Iterator<T> = object : AbstractIterator<T>() {
        /**
         * 当前扫描到的数组索引。
         */
        private var index = -1

        override fun computeNext() {
            do {
                index++
            } while (index < data.size && data[index] == null)
            if (index >= data.size) {
                done()
            } else {
                @Suppress("UNCHECKED_CAST")
                setNext(data[index] as T)
            }
        }
    }
}

package org.cangnova.cangjie.utils

import java.util.Collections
import java.util.NoSuchElementException



/**
 * A set which is optimized for small sizes and maintains the order in which the elements were added.
 * This set is not synchronized and it does not support removal operations such as [MutableSet.remove],
 * [MutableSet.removeAll] and [MutableSet.retainAll].
 * Also, [iterator] returns an iterator which does not support [MutableIterator.remove].
 */
@Suppress("UNCHECKED_CAST")
class SmartSet<T> private constructor() : AbstractMutableSet<T>() {
    /**
     * [SmartSet] 的工厂入口。
     */
    companion object {
        /**
         * 从数组存储切换到 LinkedHashSet 存储的元素数量阈值。
         */
        private const val ARRAY_THRESHOLD = 5

        /**
         * 创建空的 [SmartSet]。
         */
        @JvmStatic
        fun <T> create() = SmartSet<T>()

        /**
         * 创建包含 [set] 所有元素的 [SmartSet]。
         */
        @JvmStatic
        fun <T> create(set: Collection<T>) = SmartSet<T>().apply { this.addAll(set) }
    }

    // null if size = 0, object if size = 1, array of objects if size < threshold, linked hash set otherwise
    /**
     * 根据 [size] 保存不同形态的底层数据。
     */
    private var data: Any? = null

    /**
     * 当前集合中的元素数量。
     */
    override var size: Int = 0

    /**
     * 返回与当前存储形态匹配的迭代器。
     */
    override fun iterator(): MutableIterator<T> = when {
        size == 0 -> Collections.emptySet<T>().iterator()
        size == 1 -> SingletonIterator(data as T)
        size < ARRAY_THRESHOLD -> ArrayIterator(data as Array<T>)
        else -> (data as MutableSet<T>).iterator()
    }

    /**
     * 添加元素，并在元素数量增长时切换底层存储形态。
     */
    override fun add(element: T): Boolean {
        when {
            size == 0 -> {
                data = element
            }
            size == 1 -> {
                if (data == element) return false
                data = arrayOf(data, element)
            }
            size < ARRAY_THRESHOLD -> {
                val arr = data as Array<T>
                if (element in arr) return false
                data = if (size == ARRAY_THRESHOLD - 1) linkedSetOf(*arr).apply { add(element) }
                else arr.copyOf(size + 1).apply { set(size - 1, element) }
            }
            else -> {
                val set = data as MutableSet<T>
                if (!set.add(element)) return false
            }
        }

        size++
        return true
    }

    /**
     * 清空集合并释放底层存储。
     */
    override fun clear() {
        data = null
        size = 0
    }

    /**
     * 判断集合中是否包含指定元素。
     */
    override fun contains(element: T): Boolean = when {
        size == 0 -> false
        size == 1 -> data == element
        size < ARRAY_THRESHOLD -> element in data as Array<T>
        else -> element in data as Set<T>
    }

    /**
     * 单元素存储形态使用的迭代器。
     */
    private class SingletonIterator<out T>(
        /**
         * 需要返回的唯一元素。
         */
        private val element: T,
    ) : MutableIterator<T> {
        /**
         * 是否还有唯一元素尚未返回。
         */
        private var hasNext = true

        /**
         * 返回唯一元素，第二次调用时抛出 [NoSuchElementException]。
         */
        override fun next(): T =
            if (hasNext) {
                hasNext = false
                element
            } else throw NoSuchElementException()

        /**
         * 判断唯一元素是否尚未返回。
         */
        override fun hasNext() = hasNext

        /**
         * [SmartSet] 的迭代器不支持删除元素。
         */
        override fun remove() = throw UnsupportedOperationException()
    }

    /**
     * 小数组存储形态使用的迭代器。
     */
    private class ArrayIterator<out T>(array: Array<T>) : MutableIterator<T> {
        /**
         * 委托给数组自身的只读迭代器。
         */
        private val arrayIterator = array.iterator()

        /**
         * 判断数组迭代器是否还有后续元素。
         */
        override fun hasNext(): Boolean = arrayIterator.hasNext()
        /**
         * 返回数组迭代器的下一个元素。
         */
        override fun next(): T = arrayIterator.next()
        /**
         * [SmartSet] 的迭代器不支持删除元素。
         */
        override fun remove() = throw UnsupportedOperationException()
    }
}

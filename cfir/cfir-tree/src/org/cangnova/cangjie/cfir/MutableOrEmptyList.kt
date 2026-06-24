package org.cangnova.cangjie.cfir

/**
 * 将可为空列表转换为 [MutableOrEmptyList]。
 */
fun <T> List<T>?.toMutableOrEmpty(): MutableOrEmptyList<T> =
    if (isNullOrEmpty()) MutableOrEmptyList.empty() else MutableOrEmptyList(this.toMutableList())

/**
 * 用 `null` 表示空列表的轻量 List 包装。
 *
 * 该结构减少 CFIR 树大量空列表字段的对象分配，同时对外仍暴露只读 [List] 接口。
 */
@JvmInline
value class MutableOrEmptyList<out T>(internal val list: MutableList<@UnsafeVariance T>?) : List<T> {

    /**
     * 构造共享空实例。
     */
    private constructor(list: Nothing?) : this(list as MutableList<T>?)

    /**
     * 当前列表大小。
     */
    override val size: Int
        get() = list?.size ?: 0

    /**
     * 返回指定位置元素。
     */
    override fun get(index: Int): T {
        return list!![index]
    }

    /**
     * 当前列表是否为空。
     */
    override fun isEmpty(): Boolean {
        return list?.isEmpty() ?: true
    }

    /**
     * 返回列表迭代器。
     */
    override fun iterator(): Iterator<T> {
        return list?.iterator() ?: EMPTY_LIST_STUB_ITERATOR
    }

    /**
     * 返回列表双向迭代器。
     */
    override fun listIterator(): ListIterator<T> {
        return list?.listIterator() ?: EMPTY_LIST_STUB_LIST_ITERATOR
    }

    /**
     * 从 [index] 开始返回双向迭代器。
     */
    override fun listIterator(index: Int): ListIterator<T> {
        return list?.listIterator(index) ?: EMPTY_LIST_STUB_LIST_ITERATOR
    }

    /**
     * 返回子列表。
     */
    override fun subList(fromIndex: Int, toIndex: Int): List<T> {
        if (list == null && fromIndex == 0 && toIndex == 0) return this
        return list!!.subList(fromIndex, toIndex)
    }

    /**
     * 返回 [element] 最后一次出现的位置。
     */
    override fun lastIndexOf(element: @UnsafeVariance T): Int {
        return list?.lastIndexOf(element) ?: -1
    }

    /**
     * 返回 [element] 第一次出现的位置。
     */
    override fun indexOf(element: @UnsafeVariance T): Int {
        return list?.indexOf(element) ?: -1
    }

    /**
     * 判断是否包含 [elements] 中的全部元素。
     */
    override fun containsAll(elements: Collection<@UnsafeVariance T>): Boolean {
        return list?.containsAll(elements) ?: elements.isEmpty()
    }

    /**
     * 判断是否包含 [element]。
     */
    override fun contains(element: @UnsafeVariance T): Boolean {
        return list?.contains(element) ?: false
    }

    /**
     * 返回列表调试文本。
     */
    override fun toString(): String {
        return list?.joinToString(prefix = "[", postfix = "]") ?: "[]"
    }

    /**
     * 空列表共享实例与空迭代器缓存。
     */
    companion object {
        /**
         * 共享空实例。
         */
        private val EMPTY = MutableOrEmptyList<Nothing>(null)

        /**
         * 空列表迭代器来源。
         */
        private val EMPTY_LIST_STUB = emptyList<Nothing>()

        /**
         * 共享空迭代器。
         */
        private val EMPTY_LIST_STUB_ITERATOR = EMPTY_LIST_STUB.iterator()

        /**
         * 共享空双向迭代器。
         */
        private val EMPTY_LIST_STUB_LIST_ITERATOR = EMPTY_LIST_STUB.listIterator()

        /**
         * 返回泛型化的共享空列表。
         */
        fun <T> empty():  MutableOrEmptyList<T> = EMPTY
    }
}

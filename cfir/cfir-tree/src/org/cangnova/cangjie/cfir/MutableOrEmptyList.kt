package org.cangnova.cangjie.cfir

@JvmInline
value class MutableOrEmptyList<out T>(internal val list: MutableList<@UnsafeVariance T>?) : List<T> {

    private constructor(list: Nothing?) : this(list as MutableList<T>?)

    override val size: Int
        get() = list?.size ?: 0

    override fun get(index: Int): T {
        return list!![index]
    }

    override fun isEmpty(): Boolean {
        return list?.isEmpty() ?: true
    }

    override fun iterator(): Iterator<T> {
        return list?.iterator() ?: EMPTY_LIST_STUB_ITERATOR
    }

    override fun listIterator(): ListIterator<T> {
        return list?.listIterator() ?: EMPTY_LIST_STUB_LIST_ITERATOR
    }

    override fun listIterator(index: Int): ListIterator<T> {
        return list?.listIterator(index) ?: EMPTY_LIST_STUB_LIST_ITERATOR
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<T> {
        if (list == null && fromIndex == 0 && toIndex == 0) return this
        return list!!.subList(fromIndex, toIndex)
    }

    override fun lastIndexOf(element: @UnsafeVariance T): Int {
        return list?.lastIndexOf(element) ?: -1
    }

    override fun indexOf(element: @UnsafeVariance T): Int {
        return list?.indexOf(element) ?: -1
    }

    override fun containsAll(elements: Collection<@UnsafeVariance T>): Boolean {
        return list?.containsAll(elements) ?: elements.isEmpty()
    }

    override fun contains(element: @UnsafeVariance T): Boolean {
        return list?.contains(element) ?: false
    }

    override fun toString(): String {
        return list?.joinToString(prefix = "[", postfix = "]") ?: "[]"
    }

    companion object {
        private val EMPTY = MutableOrEmptyList<Nothing>(null)

        private val EMPTY_LIST_STUB = emptyList<Nothing>()

        private val EMPTY_LIST_STUB_ITERATOR = EMPTY_LIST_STUB.iterator()

        private val EMPTY_LIST_STUB_LIST_ITERATOR = EMPTY_LIST_STUB.listIterator()

        fun <T> empty():  MutableOrEmptyList<T> = EMPTY
    }
}
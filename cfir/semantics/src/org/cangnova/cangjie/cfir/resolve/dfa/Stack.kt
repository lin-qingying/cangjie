package org.cangnova.cangjie.cfir.resolve.dfa

abstract class Stack<T> {
    abstract val size: Int
    abstract fun top(): T
    abstract fun pop(): T
    abstract fun push(value: T)
    abstract fun reset()
    abstract fun all(): List<T>
    abstract fun <R> createSnapshot(transform: (T) -> R): Stack<R>
}

fun <T> stackOf(vararg values: T): Stack<T> = StackImpl(*values)
val Stack<*>.isEmpty: Boolean get() = size == 0
val Stack<*>.isNotEmpty: Boolean get() = size != 0
fun <T> Stack<T>.topOrNull(): T? = if (size == 0) null else top()

private class StackImpl<T>(values: List<T>) : Stack<T>() {
    constructor(vararg values: T) : this(values.asList())

    private val values = ArrayList(values)

    override val size: Int
        get() = values.size

    override fun top(): T = values[values.size - 1]

    override fun pop(): T = values.removeAt(values.size - 1)

    override fun push(value: T) {
        values.add(value)
    }

    override fun reset() {
        values.clear()
    }

    override fun all(): List<T> = values.asReversed()

    override fun <R> createSnapshot(transform: (T) -> R): Stack<R> {
        return StackImpl(values.map(transform))
    }
}

package org.cangnova.cangjie.cfir.resolve.dfa

/**
 * DFA 构造过程中使用的可变栈抽象。
 */
abstract class Stack<T> {
    /** 当前栈元素数量。 */
    abstract val size: Int

    /** 返回栈顶元素。 */
    abstract fun top(): T

    /** 弹出并返回栈顶元素。 */
    abstract fun pop(): T

    /** 压入新元素。 */
    abstract fun push(value: T)

    /** 清空栈。 */
    abstract fun reset()

    /** 以从栈顶到栈底的顺序返回所有元素。 */
    abstract fun all(): List<T>

    /**
     * 创建当前栈的映射快照。
     */
    abstract fun <R> createSnapshot(transform: (T) -> R): Stack<R>
}

/**
 * 创建包含初始元素的栈。
 */
fun <T> stackOf(vararg values: T): Stack<T> = StackImpl(*values)

/** 当前栈是否为空。 */
val Stack<*>.isEmpty: Boolean get() = size == 0

/** 当前栈是否非空。 */
val Stack<*>.isNotEmpty: Boolean get() = size != 0

/**
 * 安全读取栈顶元素。
 */
fun <T> Stack<T>.topOrNull(): T? = if (size == 0) null else top()

/**
 * 基于 [ArrayList] 的栈实现。
 *
 * @property values 初始元素列表，列表尾部作为栈顶。
 */
private class StackImpl<T>(values: List<T>) : Stack<T>() {
    constructor(vararg values: T) : this(values.asList())

    /** 栈内元素，列表尾部是栈顶。 */
    private val values = ArrayList(values)

    /** 当前栈元素数量。 */
    override val size: Int
        get() = values.size

    /** 返回栈顶元素。 */
    override fun top(): T = values[values.size - 1]

    /** 弹出并返回栈顶元素。 */
    override fun pop(): T = values.removeAt(values.size - 1)

    /** 压入新元素。 */
    override fun push(value: T) {
        values.add(value)
    }

    /** 清空栈。 */
    override fun reset() {
        values.clear()
    }

    /** 以从栈顶到栈底的顺序返回所有元素。 */
    override fun all(): List<T> = values.asReversed()

    /** 创建当前栈的映射快照。 */
    override fun <R> createSnapshot(transform: (T) -> R): Stack<R> {
        return StackImpl(values.map(transform))
    }
}

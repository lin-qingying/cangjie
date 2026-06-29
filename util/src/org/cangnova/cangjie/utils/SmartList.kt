/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0
 */

package org.cangnova.cangjie.utils

/**
 * 内存优化的智能列表
 *
 * 针对小列表场景进行了专项优化：
 * - 空列表：不分配任何存储空间
 * - 单元素列表：直接存储元素本身，不创建数组
 * - 多元素列表：使用数组存储，按需扩容
 *
 * ## 性能特点
 *
 * 在编译器中，大多数列表（如候选函数列表、约束列表）
 * 通常只有 0~2 个元素，此类可显著减少内存分配和 GC 压力。
 *
 * ## 注意
 *
 * 非线程安全，不支持并发修改。
 *
 * @param E 列表元素类型
 */
class SmartList<E> private constructor(
    /**
     * 当前列表元素数量。
     */
    private var mySize: Int,
    /**
     * 底层紧凑存储；null 表示空列表，单个元素直接存储，多元素使用数组存储。
     */
    private var myElem: Any?, // null=空, E=单元素, Array<Any?>=多元素
) : AbstractMutableList<E>(), RandomAccess {

    /** 创建空列表 */
    constructor() : this(0, null)

    /** 创建包含单个元素的列表 */
    constructor(element: E) : this(0, null) {
        add(element)
    }

    /** 从集合创建列表 */
    constructor(elements: Collection<E>) : this(0, null) {
        val size = elements.size
        when {
            size == 1 -> add(
                if (elements is List) elements[0]
                else elements.iterator().next()
            )
            size > 0 -> {
                mySize = size
                // 改前：elements.toTypedArray()
                // 改后：用 arrayOfNulls 创建数组再填充
                val array = arrayOfNulls<Any>(size)
                elements.forEachIndexed { index, element ->
                    array[index] = element
                }
                myElem = array
            }
        }
    }
    /** 从可变参数创建列表 */
    constructor(vararg elements: E) : this(0, null) {
        when {
            elements.size == 1 -> add(elements[0])
            elements.size > 1 -> {
                mySize = elements.size
                myElem = elements.copyOf()
            }
        }
    }

    /**
     * 当前列表大小。
     */
    override val size: Int get() = mySize

    /**
     * 返回指定下标的元素。
     */
    @Suppress("UNCHECKED_CAST")
    override fun get(index: Int): E {
        checkIndex(index)
        return when (mySize) {
            1 -> myElem as E
            else -> (myElem as Array<Any?>)[index] as E
        }
    }

    /**
     * 在指定位置插入元素，并根据列表大小维护紧凑存储形态。
     */
    override fun add(index: Int, element: E) {
        if (index < 0 || index > mySize) {
            throw IndexOutOfBoundsException("Index: $index, Size: $mySize")
        }

        when {
            mySize == 0 -> {
                myElem = element
            }
            mySize == 1 && index == 0 -> {
                myElem = arrayOf(element, myElem)
            }
            mySize == 1 -> {
                // index == 1
                myElem = arrayOf(myElem, element)
            }
            else -> {
                val oldArray = myElem as Array<Any?>
                val newArray = arrayOfNulls<Any>(mySize + 1)
                oldArray.copyInto(newArray, 0, 0, index)
                newArray[index] = element
                oldArray.copyInto(newArray, index + 1, index, mySize)
                myElem = newArray
            }
        }

        mySize++
        modCount++
    }

    /**
     * 替换指定位置的元素并返回旧元素。
     */
    @Suppress("UNCHECKED_CAST")
    override fun set(index: Int, element: E): E {
        checkIndex(index)
        return when (mySize) {
            1 -> {
                val old = myElem as E
                myElem = element
                old
            }
            else -> {
                val array = myElem as Array<Any?>
                val old = array[index] as E
                array[index] = element
                old
            }
        }
    }

    /**
     * 删除指定位置的元素并返回旧元素。
     */
    @Suppress("UNCHECKED_CAST")
    override fun removeAt(index: Int): E {
        checkIndex(index)
        val oldValue: E
        when {
            mySize == 1 -> {
                oldValue = myElem as E
                myElem = null
            }
            mySize == 2 -> {
                val array = myElem as Array<Any?>
                oldValue = array[index] as E
                myElem = array[1 - index]
            }
            else -> {
                val array = myElem as Array<Any?>
                oldValue = array[index] as E
                val numMoved = mySize - index - 1
                if (numMoved > 0) {
                    array.copyInto(array, index, index + 1, mySize)
                }
                array[mySize - 1] = null
            }
        }
        mySize--
        modCount++
        return oldValue
    }

    /**
     * 清空列表并释放底层存储。
     */
    override fun clear() {
        myElem = null
        mySize = 0
        modCount++
    }

    /**
     * 根据当前列表大小返回最轻量的迭代器。
     */
    override fun iterator(): MutableIterator<E> = when (mySize) {
        0 -> EmptyIterator()
        1 -> SingletonIterator()
        else -> super.iterator()
    }

    /**
     * 对列表元素排序
     *
     * 仅在元素数量 >= 2 时执行排序，单元素和空列表直接跳过。
     *
     * @param comparator 排序比较器
     */
    @Suppress("UNCHECKED_CAST")
    fun sort(comparator: Comparator<in E>) {
        if (mySize >= 2) {
            (myElem as Array<E>).sortWith(comparator, 0, mySize)
        }
    }

    /**
     * 压缩列表容量至当前实际大小
     *
     * 释放多余的数组空间，减少内存占用。
     * 适合在列表不再增长后调用。
     */
    fun trimToSize() {
        if (mySize < 2) return
        val array = myElem as Array<Any?>
        if (mySize < array.size) {
            modCount++
            myElem = array.copyOf(mySize)
        }
    }

    /** 获取当前修改计数，用于并发修改检测 */
    fun getModificationCount(): Int = modCount

    // ── 私有工具 ──────────────────────────────────────────────────────

    /**
     * 校验下标是否位于当前列表范围内。
     */
    private fun checkIndex(index: Int) {
        if (index < 0 || index >= mySize) {
            throw IndexOutOfBoundsException("Index: $index, Size: $mySize")
        }
    }

    /**
     * 空列表迭代器
     *
     * 不分配任何资源，直接返回"无更多元素"。
     */
    private class EmptyIterator<T> : MutableIterator<T> {
        /**
         * 空迭代器始终没有下一个元素。
         */
        override fun hasNext() = false
        /**
         * 空迭代器读取元素时始终抛出 [NoSuchElementException]。
         */
        override fun next() = throw NoSuchElementException()
        /**
         * 空迭代器不支持删除元素。
         */
        override fun remove() = throw IllegalStateException()
    }

    /**
     * 单元素迭代器
     *
     * 避免为单元素列表创建数组迭代器的开销。
     * 支持并发修改检测。
     */
    private inner class SingletonIterator : MutableIterator<E> {
        private var visited = false
        private val initialModCount = modCount

        override fun hasNext() = !visited

        @Suppress("UNCHECKED_CAST")
        override fun next(): E {
            if (visited) throw NoSuchElementException()
            if (modCount != initialModCount) {
                throw ConcurrentModificationException(
                    "ModCount: $modCount; expected: $initialModCount"
                )
            }
            visited = true
            return myElem as E
        }

        override fun remove() {
            if (modCount != initialModCount) {
                throw ConcurrentModificationException(
                    "ModCount: $modCount; expected: $initialModCount"
                )
            }
            clear()
        }
    }
}

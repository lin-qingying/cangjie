/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.utils

import java.lang.reflect.Modifier
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 将嵌套可迭代集合中的所有元素追加到目标可变集合 [c]。
 */
fun <T, C : MutableCollection<in T>> Iterable<Iterable<T>>.flattenTo(c: C): C {
    for (element in this) {
        c.addAll(element)
    }
    return c
}

/**
 * 以 Long 精度对集合元素映射结果求和。
 */
inline fun <T> Iterable<T>.sumByLong(selector: (T) -> Long): Long {
    var sum: Long = 0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

/**
 * 构造按需计算元素值的序列。
 *
 * 每个元素函数在序列消费到对应位置时才会被调用。
 */
fun <T> sequenceOfLazyValues(vararg elements: () -> T): Sequence<T> = elements.asSequence().map { it() }

/**
 * 仅当集合非空时在该集合上执行 [body]。
 */
inline fun <T, C : Collection<T>, O> C.ifNotEmpty(body: C.() -> O?): O? = if (isNotEmpty()) this.body() else null

/**
 * 返回第一个由 [transform] 计算出的非空结果。
 */
inline fun <T, R : Any> Iterable<T>.firstNotNullResult(transform: (T) -> R?): R? {
    for (element in this) {
        val result = transform(element)
        if (result != null) return result
    }
    return null
}

/**
 * 移除并返回可变列表的最后一个元素。
 */
fun <E> MutableList<E>.popLast(): E = removeAt(lastIndex)

/**
 * 当布尔值为 true 时执行 [body]，否则返回 null。
 */
inline fun <T> Boolean.ifTrue(body: () -> T?): T? =
    if (this) body() else null

/**
 * 标记理论上不应到达的控制流分支。
 */
fun shouldNotBeCalled(message: String = "should not be called"): Nothing {
    error(message)
}

/**
 * 返回序列中第一个类型为 [T] 的元素。
 *
 * 如果没有匹配元素则抛出 [NoSuchElementException]。
 */
inline fun <reified T> Sequence<*>.firstIsInstance(): T {
    for (element in this) if (element is T) return element
    throw NoSuchElementException("No element of given type found")
}

/**
 * 返回数组中第一个类型为 [T] 的元素。
 *
 * 如果没有匹配元素则抛出 [NoSuchElementException]。
 */
inline fun <reified T> Array<*>.firstIsInstance(): T {
    for (element in this) if (element is T) return element
    throw NoSuchElementException("No element of given type found")
}

/**
 * 返回数组中第一个类型为 [T] 的元素，未找到时返回 null。
 */
inline fun <reified T : Any> Array<*>.firstIsInstanceOrNull(): T? {
    for (element in this) if (element is T) return element
    return null
}

/**
 * 在可能的情况下把 Map 压缩为 JDK/Kotlin 的小集合实现。
 */
fun <K, V> Map<K, V>.compactIfPossible(): Map<K, V> =
    when (size) {
        0 -> emptyMap()
        1 -> Collections.singletonMap(keys.single(), values.single())
        else -> this
    }

/**
 * 对集合执行可空扁平映射。
 *
 * 只要任一元素的 [transform] 返回 null，整个函数立即返回 null；否则将所有结果追加到 [destination]。
 */
inline fun <T, R, C : MutableCollection<in R>> Iterable<T>.flatMapToNullable(
    destination: C,
    transform: (T) -> Iterable<R>?,
): C? {
    for (element in this) {
        val list = transform(element) ?: return null
        destination.addAll(list)
    }
    return destination
}

/**
 * 将可变列表截断到指定大小。
 */
fun <E> MutableList<E>.trimToSize(newSize: Int) {
    subList(newSize, size).clear()
}

/**
 * 在可能的情况下把 Set 压缩为 Kotlin 的小集合实现。
 */
fun <T> Set<T>.compactIfPossible(): Set<T> =
    when (size) {
        0 -> emptySet()
        1 -> setOf(single())
        else -> this
    }

/**
 * 当 [condition] 为 true 时执行 [block]。
 */
inline fun <R> runIf(condition: Boolean, block: () -> R): R? = if (condition) block() else null

/**
 * 返回序列中第一个类型为 [T] 的元素，未找到时返回 null。
 */
inline fun <reified T : Any> Sequence<*>.firstIsInstanceOrNull(): T? {
    for (element in this) if (element is T) return element
    return null
}

/**
 * 标记调用点依赖不安全强制类型转换。
 */
annotation class UnsafeCastFunction

/**
 * 对任意值执行不安全强制转换。
 */
@UnsafeCastFunction
inline fun <reified T : Any> Any?.cast(): T = this as T

/**
 * 保存无捕获常量计算 lambda 与其计算结果的进程级缓存。
 */
private val constantMap = ConcurrentHashMap<Function0<*>, Any>()

/**
 * 计算并缓存无捕获 lambda 对应的常量值。
 *
 * 该函数要求 [calculator] 不捕获外部字段，以保证同一个 lambda 对象可以安全代表同一常量。
 */
fun <T : Any> constant(calculator: () -> T): T {
    val cached = constantMap[calculator]
    @Suppress("UNCHECKED_CAST")
    if (cached != null) return cached as T

    val fields = calculator::class.java.declaredFields.filter { it.modifiers.and(Modifier.STATIC) == 0 }
    assert(fields.isEmpty()) {
        "No fields in the passed lambda expected but ${fields.joinToString()} found"
    }

    val value = calculator()
    constantMap[calculator] = value
    return value
}

/**
 * 对任意值执行安全类型转换。
 */
@Suppress( "INVISIBLE_MEMBER")
@UnsafeCastFunction
inline fun <reified T : Any> Any?.safeAs():   T? = this as? T

/**
 * 从可迭代对象尾部开始查找最后一个类型为 [T] 的元素。
 */
inline fun <reified T : Any> Iterable<*>.lastIsInstanceOrNull(): T? {
    when (this) {
        is List<*> -> {
            for (i in this.indices.reversed()) {
                val element = this[i]
                if (element is T) return element
            }
            return null
        }

        else -> {
            return reversed().firstIsInstanceOrNull<T>()
        }
    }
}

/**
 * 返回可迭代对象中第一个类型为 [T] 的元素，未找到时返回 null。
 */
inline fun <reified T : Any> Iterable<*>.firstIsInstanceOrNull(): T? {
    for (element in this) if (element is T) return element
    return null
}

/**
 * Checks if a bit flag is set in this integer value.
 * @param flag The flag to check
 * @return true if the flag is set, false otherwise
 */
infix fun Int.hasFlag(flag: Int): Boolean = (this and flag) == flag

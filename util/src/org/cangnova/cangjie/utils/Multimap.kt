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
/**
 * `Multimap` is a flexible, type-safe way to represent a key-to-collection mapping.
 * It:
 * * Generalizes the concept of a `Map` by allowing multiple values per key.
 * * Supports different collection types (e.g., [SetMultimap], [ListMultimap]).
 *
 * ### Example usage
 *
 * ```kotlin
 * val setMultimap = setMultimapOf<String, Int>()
 * setMultimap.put("even", 2)
 * setMultimap.put("even", 4)
 * setMultimap.put("even", 2) // Duplicate ignored
 * println(setMultimap["even"]) // {2, 4}
 *
 * val listMultimap = listMultimapOf<String, Int>()
 * listMultimap.put("odds", 1)
 * listMultimap.put("odds", 3)
 * listMultimap.put("odds", 1) // Duplicate allowed
 * println(listMultimap["odds"]) // [1, 3, 1]
 * ```
 */
interface Multimap<K, out V, out C : Collection<V>> : Iterable<Map.Entry<K, C>> {
    operator fun get(key: K): C
    operator fun contains(key: K): Boolean
    val keys: Set<K>
    val values: Collection<V>

    override operator fun iterator(): Iterator<Map.Entry<K, C>>
}

/**
 * 支持增删的多值映射视图。
 *
 * 在 [Multimap] 只读能力之上提供写入端：单值插入、批量插入、
 * 按值移除与整键移除；容器类型 `C` 由具体实现决定去重与顺序语义。
 */
interface MutableMultimap<K, V, C : Collection<V>> : Multimap<K, V, C> {
    /** 向 [key] 的容器追加一个值；键不存在时先创建容器。 */
    fun put(key: K, value: V)

    /** 向 [key] 的容器逐个追加 [values] 中的全部值。 */
    fun putAll(key: K, values: Collection<V>) {
        values.forEach { put(key, it) }
    }

    /** 从 [key] 的容器中移除单个值；容器变空时连同键一起移除。 */
    fun remove(key: K, value: V)

    /** 移除整个键并返回其原容器；键不存在时返回空容器。 */
    fun removeKey(key: K): C

    /** 清空全部键值对。 */
    fun clear()
}

/**
 * [MutableMultimap] 的通用骨架实现。
 *
 * 以 `MutableMap<K, MC>` 存储键到可变容器的映射；子类只需提供容器
 * 的创建方式（[createContainer]）与只读空容器（[createEmptyContainer]），
 * 即可确定去重/顺序语义。缺失键的读取一律返回空容器而不是 `null`。
 */
abstract class BaseMultimap<K, V, C : Collection<V>, MC : MutableCollection<V>> : MutableMultimap<K, V, C> {
    private val map: MutableMap<K, MC> = mutableMapOf()
    protected abstract fun createContainer(): MC
    protected abstract fun createEmptyContainer(): C

    override fun get(key: K): C {
        @Suppress("UNCHECKED_CAST")
        return map[key] as C? ?: createEmptyContainer()
    }

    override operator fun contains(key: K): Boolean {
        return key in map
    }

    override val keys: Set<K>
        get() = map.keys

    override val values: Collection<V>
        get() = object : AbstractCollection<V>() {
            override val size: Int
                get() = map.values.sumOf { it.size }

            override fun iterator(): Iterator<V> {
                return ChainedIterator(map.values.map { it.iterator() })
            }
        }

    override fun put(key: K, value: V) {
        val container = map.getOrPut(key) { createContainer() }
        container.add(value)
    }

    override fun remove(key: K, value: V) {
        val collection = map[key] ?: return
        collection.remove(value)
        if (collection.isEmpty()) {
            map.remove(key)
        }
    }

    override fun removeKey(key: K): C {
        @Suppress("UNCHECKED_CAST")
        return map.remove(key) as C? ?: createEmptyContainer()
    }

    override fun clear() {
        map.clear()
    }

    override fun iterator(): Iterator<Map.Entry<K, C>> {
        @Suppress("UNCHECKED_CAST")
        return map.iterator() as Iterator<Map.Entry<K, C>>
    }
}

/** 值容器为 [Set] 的多值映射：同一键下值去重、无序。 */
class SetMultimap<K, V> : BaseMultimap<K, V, Set<V>, MutableSet<V>>() {
    override fun createContainer(): MutableSet<V> {
        return mutableSetOf()
    }

    override fun createEmptyContainer(): Set<V> {
        return emptySet()
    }
}

/** 值容器为 [List] 的多值映射：同一键下保留插入顺序、允许重复值。 */
class ListMultimap<K, V> : BaseMultimap<K, V, List<V>, MutableList<V>>() {
    override fun createContainer(): MutableList<V> {
        return mutableListOf()
    }

    override fun createEmptyContainer(): List<V> {
        return emptyList()
    }
}

/** 创建空的 [SetMultimap]（值去重、无序）。 */
fun <K, V> setMultimapOf(): SetMultimap<K, V> = SetMultimap()

/** 创建空的 [ListMultimap]（值有序、允许重复）。 */
fun <K, V> listMultimapOf(): ListMultimap<K, V> = ListMultimap()

/** 把 [map] 中每个键的值集合批量并入当前 multimap（`+=` 运算符形式）。 */
operator fun <K, V> MutableMultimap<K, V, *>.plusAssign(map: Map<K, Collection<V>>) {
    for ((key, values) in map) {
        this.putAll(key, values)
    }
}

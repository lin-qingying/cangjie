package org.cangnova.cangjie.utils

import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf

/**
 * 基于 persistent collection 的不可变一键多值映射。
 */
class PersistentMultimap<K, V> private constructor(
    /**
     * 保存键到不可变列表的底层映射。
     */
    private val map: PersistentMap<K, PersistentList<V>>,
) {

    constructor() : this(persistentMapOf())

    /**
     * 返回包含新增键值对的新 multimap。
     */
    fun put(key: K, value: V): PersistentMultimap<K, V> {
        val collection = map[key] ?: persistentListOf()
        val newSet = collection.add(value)
        if (newSet === collection) return this
        val newMap = map.put(key, newSet)
        return PersistentMultimap(newMap)
    }

    /**
     * 返回移除指定键值对后的新 multimap。
     */
    fun remove(key: K, value: V): PersistentMultimap<K, V> {
        val list = map.get(key) ?: return this
        val newSet = list.remove(value)
        if (list === newSet) return this
        val newMap = if (newSet.isEmpty()) {
            map.remove(key)
        } else {
            map.put(key, newSet)
        }
        return PersistentMultimap(newMap)
    }

    /**
     * 返回指定键关联的值列表。
     */
    operator fun get(key: K): List<V> {
        return map[key] ?: emptyList()
    }

    /**
     * 当前 multimap 中存在的键集合。
     */
    val keys: ImmutableSet<K> get() = map.keys
}

/**
 * 基于 persistent collection 的不可变一键多值 Set 映射。
 */
class PersistentSetMultimap<K, V> private constructor(
    /**
     * 保存键到不可变集合的底层映射。
     */
    private val map: PersistentMap<K, PersistentSet<V>>,
) {

    constructor() : this(persistentMapOf())

    /**
     * 返回包含新增键值对的新 set multimap。
     */
    fun put(key: K, value: V): PersistentSetMultimap<K, V> {
        val set = map[key] ?: persistentSetOf()
        val newSet = set.add(value)
        if (newSet === set) return this
        val newMap = map.put(key, newSet)
        return PersistentSetMultimap(newMap)
    }

    /**
     * 返回移除指定键值对后的新 set multimap。
     */
    fun remove(key: K, value: V): PersistentSetMultimap<K, V> {
        val set = map.get(key) ?: return this
        val newSet = set.remove(value)
        if (set === newSet) return this
        val newMap = if (newSet.isEmpty()) {
            map.remove(key)
        } else {
            map.put(key, newSet)
        }
        return PersistentSetMultimap(newMap)
    }

    /**
     * 返回指定键关联的值集合。
     */
    operator fun get(key: K): Set<V> {
        return map[key] ?: emptySet()
    }

    /**
     * 当前 set multimap 的底层条目视图。
     */
    val entries: ImmutableSet<Map.Entry<K, PersistentSet<V>>> get() = map.entries
}

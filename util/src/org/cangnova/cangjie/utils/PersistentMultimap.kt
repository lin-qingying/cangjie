package org.cangnova.cangjie.utils

import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf

class PersistentMultimap<K, V> private constructor(private val map: PersistentMap<K, PersistentList<V>>) {

    constructor() : this(persistentMapOf())

    fun put(key: K, value: V): PersistentMultimap<K, V> {
        val collection = map[key] ?: persistentListOf()
        val newSet = collection.add(value)
        if (newSet === collection) return this
        val newMap = map.put(key, newSet)
        return PersistentMultimap(newMap)
    }

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

    operator fun get(key: K): List<V> {
        return map[key] ?: emptyList()
    }

    val keys: ImmutableSet<K> get() = map.keys
}

class PersistentSetMultimap<K, V> private constructor(private val map: PersistentMap<K, PersistentSet<V>>) {

    constructor() : this(persistentMapOf())

    fun put(key: K, value: V): PersistentSetMultimap<K, V> {
        val set = map[key] ?: persistentSetOf()
        val newSet = set.add(value)
        if (newSet === set) return this
        val newMap = map.put(key, newSet)
        return PersistentSetMultimap(newMap)
    }

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

    operator fun get(key: K): Set<V> {
        return map[key] ?: emptySet()
    }

    val entries: ImmutableSet<Map.Entry<K, PersistentSet<V>>> get() = map.entries
}

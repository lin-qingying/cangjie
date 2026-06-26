/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.utils.caches

import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.containers.ContainerUtil
import java.util.concurrent.ConcurrentHashMap

/**
 * 支持软引用键/值和可选 tracker 失效依赖的缓存映射抽象。
 */
public abstract class SoftCachedMap<K : Any, V : Any> {
    /**
     * 读取指定 key 的缓存值；不存在时用 [create] 创建并写入缓存。
     */
    public abstract fun getOrPut(key: K, create: () -> V): V

    /**
     * 清空缓存条目本身。
     */
    public abstract fun clear()

    /**
     * 清空缓存条目内部的 CachedValue 值，保留映射结构。
     */
    public abstract fun clearCachedValues()

    public companion object {
        public fun <K : Any, V : Any> create(
            project: Project,
            kind: Kind,
            trackers: List<Any>,
        ): SoftCachedMap<K, V> = when {
            trackers.isEmpty() -> SoftCachedMapWithoutTrackers(kind)
            else -> SoftCachedMapWithTrackers(project, kind, trackers.toTypedArray())
        }
    }

    /**
     * 缓存键和值的引用强度配置。
     */
    public enum class Kind {
        SOFT_KEYS_SOFT_VALUES,
        STRONG_KEYS_SOFT_VALUES,
    }
}

/**
 * 带 IntelliJ tracker 依赖的软缓存映射实现。
 */
private class SoftCachedMapWithTrackers<K : Any, V : Any>(
    /**
     * 创建 CachedValue 所属的 project。
     */
    private val project: Project,
    kind: SoftCachedMap.Kind,
    /**
     * CachedValue 失效依赖。
     */
    private val trackers: Array<Any>,
) : SoftCachedMap<K, V>() {
    /**
     * 实际缓存存储，value 使用 CachedValue 承载 tracker 失效语义。
     */
    private val cache = when (kind) {
        SoftCachedMap.Kind.SOFT_KEYS_SOFT_VALUES -> ContainerUtil.createConcurrentSoftMap<K, CachedValue<V>>()
        SoftCachedMap.Kind.STRONG_KEYS_SOFT_VALUES -> ConcurrentHashMap<K, CachedValue<V>>()
    }

    /**
     * 清空所有 key 到 CachedValue 的映射。
     */
    override fun clear() {
        cache.clear()
    }

    /**
     * 尝试清空每个 CachedValue 的内部值，使下次访问重新计算。
     */
    override fun clearCachedValues() {
        cache.values.forEach {
            /*
             * Kotlin 上游这里直接依赖 `CachedValueBase.clear()`。
             * 当前仓库裁剪后的 IntelliJ 依赖不暴露该基类类型，因此只能退到运行时反射；
             * 如果底层实现仍然提供 `clear()`，语义与上游保持一致。
             */
            it.javaClass.methods
                .firstOrNull { method -> method.name == "clear" && method.parameterCount == 0 }
                ?.invoke(it)
        }
    }

    /**
     * 获取或创建带 tracker 依赖的缓存值。
     */
    override fun getOrPut(key: K, create: () -> V): V {
        return cache.getOrPut(key) {
            CachedValuesManager.getManager(project).createCachedValue {
                CachedValueProvider.Result(create(), *trackers)
            }
        }.value
    }
}

/**
 * 不带 tracker 依赖的软缓存映射实现。
 */
private class SoftCachedMapWithoutTrackers<K : Any, V : Any>(
    kind: SoftCachedMap.Kind,
) : SoftCachedMap<K, V>() {
    /**
     * 直接保存实际值的软缓存存储。
     */
    private val cache = when (kind) {
        SoftCachedMap.Kind.SOFT_KEYS_SOFT_VALUES -> ContainerUtil.createConcurrentSoftKeySoftValueMap<K, V>()
        SoftCachedMap.Kind.STRONG_KEYS_SOFT_VALUES -> ContainerUtil.createSoftValueMap<K, V>()
    }

    /**
     * 清空所有缓存项。
     */
    override fun clear() {
        cache.clear()
    }

    /**
     * 无 tracker 实现没有内层 CachedValue，因此不需要额外清理。
     */
    override fun clearCachedValues() {}

    /**
     * 获取或创建无 tracker 的缓存值。
     */
    override fun getOrPut(key: K, create: () -> V): V {
        return cache.getOrPut(key, create)
    }
}

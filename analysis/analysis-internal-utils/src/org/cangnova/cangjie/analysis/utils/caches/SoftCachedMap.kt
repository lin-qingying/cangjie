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

public abstract class SoftCachedMap<K : Any, V : Any> {
    public abstract fun getOrPut(key: K, create: () -> V): V

    public abstract fun clear()

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

    public enum class Kind {
        SOFT_KEYS_SOFT_VALUES,
        STRONG_KEYS_SOFT_VALUES,
    }
}

private class SoftCachedMapWithTrackers<K : Any, V : Any>(
    private val project: Project,
    kind: SoftCachedMap.Kind,
    private val trackers: Array<Any>,
) : SoftCachedMap<K, V>() {
    private val cache = when (kind) {
        SoftCachedMap.Kind.SOFT_KEYS_SOFT_VALUES -> ContainerUtil.createConcurrentSoftMap<K, CachedValue<V>>()
        SoftCachedMap.Kind.STRONG_KEYS_SOFT_VALUES -> ConcurrentHashMap<K, CachedValue<V>>()
    }

    override fun clear() {
        cache.clear()
    }

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

    override fun getOrPut(key: K, create: () -> V): V {
        return cache.getOrPut(key) {
            CachedValuesManager.getManager(project).createCachedValue {
                CachedValueProvider.Result(create(), *trackers)
            }
        }.value
    }
}

private class SoftCachedMapWithoutTrackers<K : Any, V : Any>(
    kind: SoftCachedMap.Kind,
) : SoftCachedMap<K, V>() {
    private val cache = when (kind) {
        SoftCachedMap.Kind.SOFT_KEYS_SOFT_VALUES -> ContainerUtil.createConcurrentSoftKeySoftValueMap<K, V>()
        SoftCachedMap.Kind.STRONG_KEYS_SOFT_VALUES -> ContainerUtil.createSoftValueMap<K, V>()
    }

    override fun clear() {
        cache.clear()
    }

    override fun clearCachedValues() {}

    override fun getOrPut(key: K, create: () -> V): V {
        return cache.getOrPut(key, create)
    }
}

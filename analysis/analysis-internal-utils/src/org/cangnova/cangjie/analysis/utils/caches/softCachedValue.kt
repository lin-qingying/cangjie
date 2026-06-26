package org.cangnova.cangjie.analysis.utils.caches

import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import kotlin.reflect.KProperty

@Suppress("NOTHING_TO_INLINE")
/**
 * 允许 [CachedValue] 作为 Kotlin 委托属性读取其当前值。
 */
inline operator fun <T> CachedValue<T>.getValue(thisRef: Any?, property: KProperty<*>): T = value

/**
 * 创建带软引用语义和指定失效依赖的 IntelliJ CachedValue。
 */
inline fun <T> softCachedValue(
    project: Project,
    vararg dependencies: Any,
    crossinline createValue: () -> T,
): CachedValue<T> =
    CachedValuesManager.getManager(project).createCachedValue {
        CachedValueProvider.Result(
            createValue(),
            dependencies,
        )
    }

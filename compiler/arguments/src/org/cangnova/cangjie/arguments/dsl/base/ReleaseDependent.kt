package org.cangnova.cangjie.arguments.dsl.base

/**
 * 表示一个会随仓颉发布版本变化的值。
 */
data class ReleaseDependent<T>(
    /**
     * 当前最新版本使用的值。
     */
    val current: T,
    /**
     * 历史版本到对应旧值的映射。
     */
    val history: Map<CangJieReleaseVersion, T> = emptyMap()
)

/**
 * 将普通值包装为仅包含当前值的版本相关值。
 */
fun <T> T.asReleaseDependent(): ReleaseDependent<T> = ReleaseDependent(this)

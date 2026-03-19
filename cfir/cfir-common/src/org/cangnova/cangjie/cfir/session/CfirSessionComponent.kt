package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.SessionConfiguration

/**
 * 标记接口，所有 session 组件的基接口。
 *
 * 每个编译器服务（provider、scope、diagnostic 等）都实现此接口，
 * 并通过 [CfirSession] 注册和查询。
 */
interface CfirSessionComponent


/**
 * Marker interface for session components which could have different implementations depending
 * on the target (e.g. different implementations for JVM and Native). Such components should be
 * able to be composed with each other in case of metadata compilation for several targets.
 */
interface CfirComposableSessionComponent<T : CfirComposableSessionComponent<T>> : CfirSessionComponent {
    @SessionConfiguration
    fun compose(other: T): T {
        val components = buildList {
            addAll(components)
            addAll(other.components)
        }.distinct()
        components.singleOrNull()?.let { return it }
        @Suppress("UNCHECKED_CAST")
        return createComposed(components) as T
    }

    @Suppress("UNCHECKED_CAST")
    val components: List<T>
        get() = listOf(this as T)

    @SessionConfiguration
    fun createComposed(components: List<T>): Composed<T>

    interface Composed<T : CfirComposableSessionComponent<T>> : CfirComposableSessionComponent<T> {
        override val components: List<T>
    }
}

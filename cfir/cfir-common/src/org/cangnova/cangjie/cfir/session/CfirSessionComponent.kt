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
 * 可组合 session component 的基接口。
 *
 * 这类组件允许不同目标平台提供不同实现；当一次编译需要同时承载多个目标的
 * metadata 时，session 构造流程会把多个实现组合成一个组件实例。
 */
interface CfirComposableSessionComponent<T : CfirComposableSessionComponent<T>> : CfirSessionComponent {
    /**
     * 将当前组件与 [other] 组合为一个组件。
     *
     * 若去重后只剩一个组件，直接返回该组件；否则交给 [createComposed] 创建组合实现。
     */
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

    /**
     * 当前组件展开后的原始组件列表。
     */
    @Suppress("UNCHECKED_CAST")
    val components: List<T>
        get() = listOf(this as T)

    /**
     * 根据 [components] 创建组合组件实例。
     */
    @SessionConfiguration
    fun createComposed(components: List<T>): Composed<T>

    /**
     * 表示多个同类 session component 的组合结果。
     */
    interface Composed<T : CfirComposableSessionComponent<T>> : CfirComposableSessionComponent<T> {
        /**
         * 组合中包含的原始组件列表。
         */
        override val components: List<T>
    }
}

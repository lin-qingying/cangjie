package org.cangnova.cangjie.analysis.api.lifetime

/**
 * `CaSession` 的组件接口。
 *
 * 对齐 Kotlin `KaSessionComponent`：组件成员会混入 `CaSession`，
 * 调用方应通过 session 上下文或 context bridge 访问，而不是直接持有组件实例。
 */
@CaSessionComponentImplementationDetail
@SubclassOptInRequired(CaSessionComponentImplementationDetail::class)
interface CaSessionComponent : CaLifetimeOwner

/**
 * 标记 `CaSessionComponent` 本身属于 Analysis API 的实现细节。
 *
 * 组件实例不应被直接消费；应通过 `CaSession` 或对应的 context bridge 使用其成员。
 */
@Target(AnnotationTarget.CLASS)
@RequiresOptIn(
    "The session component is an implementation detail of the Analysis API and should be accessed via `CaSession` or context parameter bridges instead.",
    level = RequiresOptIn.Level.WARNING,
)
annotation class CaSessionComponentImplementationDetail

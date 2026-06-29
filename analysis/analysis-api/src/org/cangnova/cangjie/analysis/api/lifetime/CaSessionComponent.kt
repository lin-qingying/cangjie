package org.cangnova.cangjie.analysis.api.lifetime

/**
 * `CaSession` 的组件接口。
 *
 * 对齐 Kotlin `KaSessionComponent`:
 * - 组件成员通过 mix-in 形式注入 `CaSession`,
 *   调用方应通过 session 上下文或 context bridge 访问,而不是直接持有组件实例;
 * - 组件本身只是实现细节,提供具体能力(符号查询、类型计算、解析等)。
 *
 * 所有公开 API 组件应直接继承 [CaSessionComponent],
 * 以便 context parameter bridge 校验工具能保证每个入口都有对应的 bridge。
 */
@CaSessionComponentImplementationDetail
@SubclassOptInRequired(CaSessionComponentImplementationDetail::class)
interface CaSessionComponent : CaLifetimeOwner

/**
 * 标记 `CaSessionComponent` 本身属于 Analysis API 的实现细节。
 *
 * - 组件实例不应被直接消费;
 * - 应通过 `CaSession` 或对应的 context bridge 使用其成员;
 * - 子类化需要显式 opt-in,以避免误用。
 */
@Target(AnnotationTarget.CLASS)
@RequiresOptIn(
    "The session component is an implementation detail of the Analysis API and should be accessed via `CaSession` or context parameter bridges instead.",
    level = RequiresOptIn.Level.WARNING,
)
/**
 * 需要显式 opt-in 才能实现或直接使用的 session component 实现细节标记。
 */
annotation class CaSessionComponentImplementationDetail

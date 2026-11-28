package org.cangnova.cangjie.analysis.low.level.api.cfir.macro

import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult

/**
 * 宏语义快照发布契约的唯一判定入口。
 *
 * 修复前的宿主与 analysis 层各有一份"无宏效果"判断：宿主只看 `allSurfaces.isEmpty()`，
 * analysis 层还会看生成源码与来源 surface。两份判据容易漂移，也与"宿主不得决定语义"冲突。
 *
 * 现在判据只在这里定义一次：
 * - [canHaveMacroSemanticEffect] 是展开前的**必要**条件，供框架在整模块 raw 构建之外提前短路；
 * - [hasMacroSemanticEffect] 是展开后的**权威**判定，供 `prepareMacroSemanticSnapshot` 拒绝
 *   identity 快照。
 *
 * 两个函数必须保持一致：任何让 `has` 为真的场景都必须让 `can` 为真（surface 是宏效果的唯一入口）。
 */
object MacroSemanticSnapshotContract {
    /**
     * 展开前的廉价必要条件：pre-macro 输入至少捕获到一个宏调用 surface。
     *
     * 没有 surface 时展开不可能产生生成源码或来源映射，因此可以安全地在整模块 raw 构建之后、
     * 展开之前短路。
     */
    fun canHaveMacroSemanticEffect(pre: PreMacroRawBuildResult): Boolean = pre.allSurfaces.isNotEmpty()

    /**
     * 展开后的权威判定：实际发生了宏语义变化。
     *
     * 三个条件必须同时为空才是 identity 结果；任一非空都说明快照与实时视图不同构。
     */
    fun hasMacroSemanticEffect(
        pre: PreMacroRawBuildResult,
        result: MacroConstructionResult.Recordable,
    ): Boolean = pre.allSurfaces.isNotEmpty() ||
        result.registry.generatedSources.isNotEmpty() ||
        result.registry.originSurfaceById.isNotEmpty()
}

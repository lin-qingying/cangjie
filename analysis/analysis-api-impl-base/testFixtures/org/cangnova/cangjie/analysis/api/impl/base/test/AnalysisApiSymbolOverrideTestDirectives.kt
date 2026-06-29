package org.cangnova.cangjie.analysis.api.impl.base.test

import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.directives.model.singleValue

/**
 * overrides 能力族专用指令。
 *
 * 这里继续复用公共 `TARGET_NAME`，只补充当前用例真正关心的三类元信息：
 * 1. 目标 callable 的种类
 * 2. 递归覆写链期望
 * 3. 直接覆写链期望
 */
object AnalysisApiSymbolOverrideTestDirectives : SimpleDirectivesContainer() {
    /**
     * 指定 override 测试目标 callable 的种类。
     *
     * 测试框架根据该值区分成员函数、成员属性和 extend 成员等形态，避免同名 callable 在不同声明位置
     * 被错误选中。
     */
    val OVERRIDE_TARGET_KIND by stringDirective(
        description = "override 测试目标 callable 的种类，例如 MEMBER_FUNCTION / MEMBER_PROPERTY / EXTEND_FUNCTION。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录递归覆写链中应出现的所有 overridden symbols。
     *
     * 该指令允许重复声明并保留顺序，用于断言 Analysis API 对间接覆写关系的完整展开结果。
     */
    val EXPECTED_ALL_OVERRIDDEN by stringDirective(
        description = "allOverriddenSymbols 应输出的稳定签名，可重复声明多次以保留顺序。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录直接覆写链中应出现的 directly overridden symbols。
     *
     * 该指令与递归覆写链期望分离，便于测试同时覆盖直接父声明和完整覆写集合。
     */
    val EXPECTED_DIRECT_OVERRIDDEN by stringDirective(
        description = "directlyOverriddenSymbols 应输出的稳定签名，可重复声明多次以保留顺序。",
        applicability = DirectiveApplicability.File,
    )
}

/**
 * 读取 override 测试目标 callable 的种类。
 *
 * 返回值会驱动目标定位逻辑，确保后续 symbol 关系查询作用在 testData 指定的声明形态上。
 */
val RegisteredDirectives.overrideTargetKind: String
    get() = singleValue(AnalysisApiSymbolOverrideTestDirectives.OVERRIDE_TARGET_KIND)

/**
 * 读取递归覆写链的期望签名列表。
 *
 * 该列表保留 testData 中的声明顺序，用于与 `allOverriddenSymbols` 的稳定渲染结果逐项比较。
 */
val RegisteredDirectives.expectedAllOverridden: List<String>
    get() = this[AnalysisApiSymbolOverrideTestDirectives.EXPECTED_ALL_OVERRIDDEN]

/**
 * 读取直接覆写链的期望签名列表。
 *
 * 该列表只对应 `directlyOverriddenSymbols`，不包含间接祖先声明。
 */
val RegisteredDirectives.expectedDirectOverridden: List<String>
    get() = this[AnalysisApiSymbolOverrideTestDirectives.EXPECTED_DIRECT_OVERRIDDEN]

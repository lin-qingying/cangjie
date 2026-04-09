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
    val OVERRIDE_TARGET_KIND by stringDirective(
        description = "override 测试目标 callable 的种类，例如 MEMBER_FUNCTION / MEMBER_PROPERTY / EXTEND_FUNCTION。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_ALL_OVERRIDDEN by stringDirective(
        description = "allOverriddenSymbols 应输出的稳定签名，可重复声明多次以保留顺序。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_DIRECT_OVERRIDDEN by stringDirective(
        description = "directlyOverriddenSymbols 应输出的稳定签名，可重复声明多次以保留顺序。",
        applicability = DirectiveApplicability.File,
    )
}

val RegisteredDirectives.overrideTargetKind: String
    get() = singleValue(AnalysisApiSymbolOverrideTestDirectives.OVERRIDE_TARGET_KIND)

val RegisteredDirectives.expectedAllOverridden: List<String>
    get() = this[AnalysisApiSymbolOverrideTestDirectives.EXPECTED_ALL_OVERRIDDEN]

val RegisteredDirectives.expectedDirectOverridden: List<String>
    get() = this[AnalysisApiSymbolOverrideTestDirectives.EXPECTED_DIRECT_OVERRIDDEN]

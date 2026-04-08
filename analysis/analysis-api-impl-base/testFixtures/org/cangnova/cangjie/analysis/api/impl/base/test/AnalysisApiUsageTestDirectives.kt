package org.cangnova.cangjie.analysis.api.impl.base.test

import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.directives.model.singleValue

/**
 * usages / find usages 能力族的专用测试指令。
 *
 * 这组指令不混进通用 component 指令集里，而是单独建模：
 * 1. `find usages` 的目标声明种类与普通 resolver/renderer 测试不同；
 * 2. usages 测试需要显式表达 use-scope 预期；
 * 3. 后续扩展 local/member/top-level/import-alias/extend/pattern-binding 场景时，
 *    不会污染其它能力族的指令语义。
 */
object AnalysisApiUsageTestDirectives : SimpleDirectivesContainer() {
    val TARGET_KIND by stringDirective(
        description = "当前 usages 测试目标声明的种类。",
        applicability = DirectiveApplicability.File,
    )

    val TARGET_NAME by stringDirective(
        description = "当前 usages 测试目标声明的名字。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_USAGE_COUNT by stringDirective(
        description = "find usages 搜索后应命中的引用数量。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_USAGE_SCOPE_KIND by stringDirective(
        description = "目标声明应暴露的 use-scope 种类，例如 LOCAL / ANY。",
        applicability = DirectiveApplicability.File,
    )
}

val RegisteredDirectives.usageTargetKind: String
    get() = singleValue(AnalysisApiUsageTestDirectives.TARGET_KIND)

val RegisteredDirectives.usageTargetName: String
    get() = singleValue(AnalysisApiUsageTestDirectives.TARGET_NAME)

val RegisteredDirectives.expectedUsageCount: Int
    get() = singleValue(AnalysisApiUsageTestDirectives.EXPECTED_USAGE_COUNT).toInt()

val RegisteredDirectives.expectedUsageScopeKind: String
    get() = singleValue(AnalysisApiUsageTestDirectives.EXPECTED_USAGE_SCOPE_KIND)

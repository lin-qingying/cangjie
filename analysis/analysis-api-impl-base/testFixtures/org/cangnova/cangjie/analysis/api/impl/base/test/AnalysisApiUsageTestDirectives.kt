package org.cangnova.cangjie.analysis.api.impl.base.test

import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.directives.model.singleValue

/**
 * usages / find usages 能力族的专用测试指令。
 *
 * `TARGET_NAME` 由公共 component 指令统一承载，这里只保留 usages 测试特有的
 * 目标种类、命中数量和 use-scope 期望，避免不同测试族重复声明同名 directive。
 */
object AnalysisApiUsageTestDirectives : SimpleDirectivesContainer() {
    val TARGET_KIND by stringDirective(
        description = "当前 usages 测试目标声明的种类。",
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
    get() = targetNameText

val RegisteredDirectives.expectedUsageCount: Int
    get() = singleValue(AnalysisApiUsageTestDirectives.EXPECTED_USAGE_COUNT).toInt()

val RegisteredDirectives.expectedUsageScopeKind: String
    get() = singleValue(AnalysisApiUsageTestDirectives.EXPECTED_USAGE_SCOPE_KIND)

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
    /**
     * 指定 usages 测试目标声明的种类。
     *
     * 测试框架利用该值在同名声明中选择正确目标，并决定应走本地搜索、成员搜索还是普通引用搜索路径。
     */
    val TARGET_KIND by stringDirective(
        description = "当前 usages 测试目标声明的种类。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录 find usages 搜索后应命中的引用数量。
     *
     * 该字段断言搜索结果规模，防止 use-scope 或引用提供器变化导致多报、漏报而未被发现。
     */
    val EXPECTED_USAGE_COUNT by stringDirective(
        description = "find usages 搜索后应命中的引用数量。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录目标声明应暴露的 use-scope 种类。
     *
     * 用例通过该字段校验 Analysis API / IDE 搜索边界是否与声明种类和可见性语义一致。
     */
    val EXPECTED_USAGE_SCOPE_KIND by stringDirective(
        description = "目标声明应暴露的 use-scope 种类，例如 LOCAL / ANY。",
        applicability = DirectiveApplicability.File,
    )
}

/**
 * 读取 usages 测试目标声明的种类。
 *
 * 返回值用于目标定位和搜索策略选择，是 usages 测试与普通 reference 测试的主要区分字段。
 */
val RegisteredDirectives.usageTargetKind: String
    get() = singleValue(AnalysisApiUsageTestDirectives.TARGET_KIND)

/**
 * 读取 usages 测试目标声明的名称。
 *
 * 该名称复用公共 `TARGET_NAME`，保证引用解析和 usages 搜索围绕同一个声明选择协议运行。
 */
val RegisteredDirectives.usageTargetName: String
    get() = targetNameText

/**
 * 读取 find usages 结果数量的期望值。
 *
 * 访问器集中执行整数解析，避免测试实现分散处理 testData 字符串到数值的转换。
 */
val RegisteredDirectives.expectedUsageCount: Int
    get() = singleValue(AnalysisApiUsageTestDirectives.EXPECTED_USAGE_COUNT).toInt()

/**
 * 读取目标声明 use-scope 种类的期望值。
 *
 * 返回值用于与测试渲染出的搜索范围分类比较，确认搜索边界没有退化。
 */
val RegisteredDirectives.expectedUsageScopeKind: String
    get() = singleValue(AnalysisApiUsageTestDirectives.EXPECTED_USAGE_SCOPE_KIND)

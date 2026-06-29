package org.cangnova.cangjie.analysis.api.impl.base.test

import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.directives.model.singleValue

/**
 * 引用缩短命令测试专用指令。
 */
object AnalysisApiShorteningCommandTestDirectives : SimpleDirectivesContainer() {
    /**
     * 指定引用缩短命令测试中作为选择范围入口的表达式文本。
     *
     * 测试基类用该文本在主文件中定位 shorten range，并将范围传给公开的引用缩短接口，
     * 从而验证命令层计算出的替换和导入计划是否符合 testData 期望。
     */
    val TARGET_EXPRESSION by stringDirective(
        description = "shortenRange 测试中要作为选择范围入口的表达式文本。",
        applicability = DirectiveApplicability.File,
    )
}

/**
 * 读取引用缩短命令测试的目标表达式文本。
 *
 * 该访问器封装 `TARGET_EXPRESSION` 的单值读取规则，让实际测试逻辑只关注 PSI 定位与缩短命令断言。
 */
val RegisteredDirectives.targetExpressionText: String
    get() = singleValue(AnalysisApiShorteningCommandTestDirectives.TARGET_EXPRESSION)

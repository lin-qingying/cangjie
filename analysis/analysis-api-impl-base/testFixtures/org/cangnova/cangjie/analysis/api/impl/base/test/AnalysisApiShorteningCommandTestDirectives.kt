package org.cangnova.cangjie.analysis.api.impl.base.test

import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.directives.model.singleValue

/**
 * 引用缩短命令测试专用指令。
 */
object AnalysisApiShorteningCommandTestDirectives : SimpleDirectivesContainer() {
    val TARGET_EXPRESSION by stringDirective(
        description = "shortenRange 测试中要作为选择范围入口的表达式文本。",
        applicability = DirectiveApplicability.File,
    )
}

val RegisteredDirectives.targetExpressionText: String
    get() = singleValue(AnalysisApiShorteningCommandTestDirectives.TARGET_EXPRESSION)

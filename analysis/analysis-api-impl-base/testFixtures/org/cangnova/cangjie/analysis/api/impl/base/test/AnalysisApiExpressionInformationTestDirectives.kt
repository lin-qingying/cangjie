package org.cangnova.cangjie.analysis.api.impl.base.test

import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.directives.model.singleValue
import kotlin.text.toBooleanStrict

/**
 * expression information 能力族专用指令。
 */
object AnalysisApiExpressionInformationTestDirectives : SimpleDirectivesContainer() {
    val TARGET_EXPRESSION_TEXT by stringDirective(
        description = "当前用例中要查询的表达式文本，要求在主文件里唯一。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_IS_STATEMENT_LIKE by stringDirective(
        description = "expression.isStatementLike 的期望结果，取值 true / false。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_IS_COMPILE_TIME_CONSTANT by stringDirective(
        description = "expression.isCompileTimeConstant 的期望结果，取值 true / false。",
        applicability = DirectiveApplicability.File,
    )
}

val RegisteredDirectives.expressionInfoTargetExpressionText: String
    get() = this[AnalysisApiExpressionInformationTestDirectives.TARGET_EXPRESSION_TEXT].joinToString(", ")

val RegisteredDirectives.expectedIsStatementLike: Boolean
    get() = singleValue(AnalysisApiExpressionInformationTestDirectives.EXPECTED_IS_STATEMENT_LIKE).toBooleanStrict()

val RegisteredDirectives.expectedIsCompileTimeConstant: Boolean
    get() = singleValue(AnalysisApiExpressionInformationTestDirectives.EXPECTED_IS_COMPILE_TIME_CONSTANT).toBooleanStrict()

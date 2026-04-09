package org.cangnova.cangjie.analysis.api.impl.base.test

import org.cangnova.cangjie.analysis.api.dataFlow.CaDataFlowStability
import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.directives.model.singleValue
import kotlin.text.toBooleanStrict

/**
 * data flow 基础快照测试指令。
 */
object AnalysisApiDataFlowInfoTestDirectives : SimpleDirectivesContainer() {
    val TARGET_EXPRESSION_TEXT by stringDirective(
        description = "当前用例中要查询 data flow 的表达式文本，要求在主文件里唯一。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_STABILITY by stringDirective(
        description = "dataFlowInfo.stability 的期望枚举值。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_IS_PURE_REFERENCE by stringDirective(
        description = "dataFlowInfo.isPureReference 的期望结果，取值 true / false。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_COMPILE_TIME_VALUE by stringDirective(
        description = "dataFlowInfo.compileTimeValue?.renderedText 的期望文本。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_EXPRESSION_TYPE by stringDirective(
        description = "dataFlowInfo.expressionType 渲染后的期望文本。",
        applicability = DirectiveApplicability.File,
    )
}

val RegisteredDirectives.dataFlowTargetExpressionText: String
    get() = singleValue(AnalysisApiDataFlowInfoTestDirectives.TARGET_EXPRESSION_TEXT)

val RegisteredDirectives.expectedDataFlowStability: CaDataFlowStability
    get() = CaDataFlowStability.valueOf(singleValue(AnalysisApiDataFlowInfoTestDirectives.EXPECTED_STABILITY))

val RegisteredDirectives.expectedIsPureReference: Boolean
    get() = singleValue(AnalysisApiDataFlowInfoTestDirectives.EXPECTED_IS_PURE_REFERENCE).toBooleanStrict()

val RegisteredDirectives.expectedCompileTimeValueText: String?
    get() = this[AnalysisApiDataFlowInfoTestDirectives.EXPECTED_COMPILE_TIME_VALUE].singleOrNull()

val RegisteredDirectives.expectedDataFlowExpressionType: String?
    get() = this[AnalysisApiDataFlowInfoTestDirectives.EXPECTED_EXPRESSION_TYPE].singleOrNull()

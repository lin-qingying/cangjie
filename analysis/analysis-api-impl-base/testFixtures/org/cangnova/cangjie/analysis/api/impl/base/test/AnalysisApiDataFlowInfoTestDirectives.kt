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
    /**
     * 指定当前用例中要查询 data flow 的目标表达式文本。
     *
     * 抽象测试会在主文件中按文本定位唯一表达式，并以该表达式为入口读取公开
     * data-flow 信息，保证稳定性、纯引用、编译期值和表达式类型断言围绕同一目标执行。
     */
    val TARGET_EXPRESSION_TEXT by stringDirective(
        description = "当前用例中要查询 data flow 的表达式文本，要求在主文件里唯一。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录 `dataFlowInfo.stability` 的期望枚举名称。
     *
     * 该字段直接映射到 `CaDataFlowStability`，用于校验 Analysis API 对目标表达式稳定性的公开判断。
     */
    val EXPECTED_STABILITY by stringDirective(
        description = "dataFlowInfo.stability 的期望枚举值。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录 `dataFlowInfo.isPureReference` 的期望布尔值。
     *
     * 测试框架会严格解析该布尔字符串，确保纯引用判断的 testData 不会因为拼写宽松而被静默接受。
     */
    val EXPECTED_IS_PURE_REFERENCE by stringDirective(
        description = "dataFlowInfo.isPureReference 的期望结果，取值 true / false。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录 data-flow 编译期值渲染文本的期望结果。
     *
     * 该指令可省略；省略时表示当前表达式不要求暴露编译期值，从而把“无值”和“值文本不一致”
     * 区分为两个不同测试语义。
     */
    val EXPECTED_DATA_FLOW_COMPILE_TIME_VALUE by stringDirective(
        description = "dataFlowInfo.compileTimeValue?.renderedText 的期望文本。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录 data-flow 表达式类型渲染文本的期望结果。
     *
     * 该指令可省略；存在时测试会将公开表达式类型渲染为文本后与该期望比较。
     */
    val EXPECTED_DATA_FLOW_EXPRESSION_TYPE by stringDirective(
        description = "dataFlowInfo.expressionType 渲染后的期望文本。",
        applicability = DirectiveApplicability.File,
    )
}

/**
 * 读取 data-flow 测试的目标表达式文本。
 *
 * 多个 token 会按逗号还原，支持 testData 中包含逗号或被 directive 解析器拆分的表达式文本。
 */
val RegisteredDirectives.dataFlowTargetExpressionText: String
    get() = this[AnalysisApiDataFlowInfoTestDirectives.TARGET_EXPRESSION_TEXT].joinToString(", ")

/**
 * 读取 data-flow 稳定性枚举的期望值。
 *
 * 访问器集中完成字符串到 `CaDataFlowStability` 的转换，让测试断言直接比较强类型枚举。
 */
val RegisteredDirectives.expectedDataFlowStability: CaDataFlowStability
    get() = CaDataFlowStability.valueOf(singleValue(AnalysisApiDataFlowInfoTestDirectives.EXPECTED_STABILITY))

/**
 * 读取 data-flow 纯引用判断的期望值。
 *
 * 该值使用严格布尔解析，保证非法布尔文本立即暴露为测试数据错误。
 */
val RegisteredDirectives.expectedIsPureReference: Boolean
    get() = singleValue(AnalysisApiDataFlowInfoTestDirectives.EXPECTED_IS_PURE_REFERENCE).toBooleanStrict()

/**
 * 读取 data-flow 编译期值渲染文本的可选期望。
 *
 * 当指令不存在时返回 `null`，表示当前用例不对 compile-time value 做断言。
 */
val RegisteredDirectives.expectedCompileTimeValueText: String?
    get() = this[AnalysisApiDataFlowInfoTestDirectives.EXPECTED_DATA_FLOW_COMPILE_TIME_VALUE]
        .takeIf(List<String>::isNotEmpty)
        ?.joinToString(", ")

/**
 * 读取 data-flow 表达式类型渲染文本的可选期望。
 *
 * 当指令存在时会按逗号还原为一个文本值，用于和公开类型渲染结果比较。
 */
val RegisteredDirectives.expectedDataFlowExpressionType: String?
    get() = this[AnalysisApiDataFlowInfoTestDirectives.EXPECTED_DATA_FLOW_EXPRESSION_TYPE]
        .takeIf(List<String>::isNotEmpty)
        ?.joinToString(", ")

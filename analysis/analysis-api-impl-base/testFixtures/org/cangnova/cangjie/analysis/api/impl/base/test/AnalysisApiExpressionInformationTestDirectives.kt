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
    /**
     * 指定当前用例中要查询表达式信息的源码文本。
     *
     * 抽象测试会在主文件中按文本精确定位唯一表达式，并以该表达式作为
     * `isStatementLike` 与 `isCompileTimeConstant` 等公开 Analysis API 查询的目标。
     */
    val TARGET_EXPRESSION_TEXT by stringDirective(
        description = "当前用例中要查询的表达式文本，要求在主文件里唯一。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 描述目标表达式是否应被 Analysis API 视为 statement-like。
     *
     * 该指令直接约束 `CaExpressionInformationProvider` 对表达式语句语义的公开判断，
     * 用于捕获不同语法位置下表达式是否可作为语句使用的稳定结果。
     */
    val EXPECTED_IS_STATEMENT_LIKE by stringDirective(
        description = "expression.isStatementLike 的期望结果，取值 true / false。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 描述目标表达式是否应被 Analysis API 视为编译期常量。
     *
     * 测试框架会将该字符串严格转换为布尔值，避免 testData 中出现大小写或非布尔拼写后
     * 被静默接受，从而保证常量性断言具有明确失败信号。
     */
    val EXPECTED_IS_COMPILE_TIME_CONSTANT by stringDirective(
        description = "expression.isCompileTimeConstant 的期望结果，取值 true / false。",
        applicability = DirectiveApplicability.File,
    )
}

/**
 * 读取表达式信息测试的目标源码文本。
 *
 * 多段 directive 值会按逗号拼接，以便测试数据中的表达式文本可以包含框架拆分后的片段，
 * 同时仍向定位逻辑提供单个稳定字符串。
 */
val RegisteredDirectives.expressionInfoTargetExpressionText: String
    get() = this[AnalysisApiExpressionInformationTestDirectives.TARGET_EXPRESSION_TEXT].joinToString(", ")

/**
 * 读取目标表达式 statement-like 语义的期望值。
 *
 * 访问器集中执行严格布尔解析，保证所有 expression-information 测试共享同一套
 * testData 合法性规则。
 */
val RegisteredDirectives.expectedIsStatementLike: Boolean
    get() = singleValue(AnalysisApiExpressionInformationTestDirectives.EXPECTED_IS_STATEMENT_LIKE).toBooleanStrict()

/**
 * 读取目标表达式编译期常量语义的期望值。
 *
 * 返回值直接参与测试断言，用于比较 Analysis API 查询结果与 testData 中声明的稳定布尔语义。
 */
val RegisteredDirectives.expectedIsCompileTimeConstant: Boolean
    get() = singleValue(AnalysisApiExpressionInformationTestDirectives.EXPECTED_IS_COMPILE_TIME_CONSTANT).toBooleanStrict()

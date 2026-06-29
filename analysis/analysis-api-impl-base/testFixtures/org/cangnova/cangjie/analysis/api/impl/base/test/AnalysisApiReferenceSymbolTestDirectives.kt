package org.cangnova.cangjie.analysis.api.impl.base.test

import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.directives.model.singleValue

/**
 * 引用 -> symbol / renderer 能力族的专用测试指令。
 *
 * 目标 simple-name 统一复用公共 `TARGET_NAME`，这里仅声明该能力族独有的输出期望，
 * 保证 symbol / renderer / resolver 等测试族可以共享同一套目标选择协议。
 */
object AnalysisApiReferenceSymbolTestDirectives : SimpleDirectivesContainer() {
    /**
     * 记录引用解析后应得到的公开 symbol 简单类名。
     *
     * 用例通过该指令确认 resolver 返回的 Analysis API symbol 类型正确，避免只比较名称时
     * 把 callable、class-like 或 package 等不同 symbol 形态混淆。
     */
    val EXPECTED_SYMBOL_CLASS by stringDirective(
        description = "引用解析后应得到的 symbol 简单类名。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录引用解析后应得到的 symbol 名称。
     *
     * 该字段用于校验解析目标在公开 API 层暴露的名称，而不是 PSI 文本中的任意同名 token。
     */
    val EXPECTED_SYMBOL_NAME by stringDirective(
        description = "引用解析后应得到的 symbol 名字。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录 symbol 关联的原始 PSI 应恢复到的元素类名。
     *
     * 该断言覆盖 symbol 到 PSI 的回跳能力，能发现 stub、decompiled 或恢复路径返回错误元素层级的问题。
     */
    val EXPECTED_ORIGINAL_PSI_CLASS by stringDirective(
        description = "symbol.psi 应恢复到的 PSI 简单类名。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录通过引用恢复 declaration symbol 后的标准渲染文本。
     *
     * 多 token 的期望文本会由访问器重新拼接，保证 renderer 输出可以在 testData 中保持可读格式。
     */
    val EXPECTED_RENDERED_DECLARATION by stringDirective(
        description = "通过引用恢复 declaration symbol 后的标准渲染文本。",
        applicability = DirectiveApplicability.File,
    )
}

/**
 * 读取引用转 symbol 测试要定位的目标 simple-name。
 *
 * 该访问器复用公共 `TARGET_NAME`，保证 symbol、resolver 与 renderer 相关测试共享同一套目标选择协议。
 */
val RegisteredDirectives.referenceTargetName: String
    get() = targetNameText

/**
 * 读取期望的公开 symbol 简单类名。
 *
 * 返回值直接参与类型断言，用于确认引用解析结果属于 testData 指定的 symbol 家族。
 */
val RegisteredDirectives.expectedSymbolClass: String
    get() = singleValue(AnalysisApiReferenceSymbolTestDirectives.EXPECTED_SYMBOL_CLASS)

/**
 * 读取期望的公开 symbol 名称。
 *
 * 该名称来自 Analysis API symbol，而不是直接来自 PSI 文本，因而可用于校验别名、恢复和渲染前的语义目标。
 */
val RegisteredDirectives.expectedSymbolName: String
    get() = singleValue(AnalysisApiReferenceSymbolTestDirectives.EXPECTED_SYMBOL_NAME)

/**
 * 读取期望恢复出的原始 PSI 简单类名。
 *
 * 访问器集中约束该指令必须是单值，保证 PSI 恢复断言没有多重期望造成的歧义。
 */
val RegisteredDirectives.expectedOriginalPsiClass: String
    get() = singleValue(AnalysisApiReferenceSymbolTestDirectives.EXPECTED_ORIGINAL_PSI_CLASS)

/**
 * 读取 declaration symbol 的期望渲染文本。
 *
 * 指令值按空格还原，支持测试数据把较长声明渲染结果拆成多个 token 记录。
 */
val RegisteredDirectives.expectedRenderedDeclaration: String
    get() = this[AnalysisApiReferenceSymbolTestDirectives.EXPECTED_RENDERED_DECLARATION].joinToString(" ")

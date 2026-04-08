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
    val EXPECTED_SYMBOL_CLASS by stringDirective(
        description = "引用解析后应得到的 symbol 简单类名。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_SYMBOL_NAME by stringDirective(
        description = "引用解析后应得到的 symbol 名字。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_ORIGINAL_PSI_CLASS by stringDirective(
        description = "symbol.getOriginalPsi() 应恢复到的 PSI 简单类名。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_RENDERED_DECLARATION by stringDirective(
        description = "通过引用恢复 declaration symbol 后的标准渲染文本。",
        applicability = DirectiveApplicability.File,
    )
}

val RegisteredDirectives.referenceTargetName: String
    get() = targetNameText

val RegisteredDirectives.expectedSymbolClass: String
    get() = singleValue(AnalysisApiReferenceSymbolTestDirectives.EXPECTED_SYMBOL_CLASS)

val RegisteredDirectives.expectedSymbolName: String
    get() = singleValue(AnalysisApiReferenceSymbolTestDirectives.EXPECTED_SYMBOL_NAME)

val RegisteredDirectives.expectedOriginalPsiClass: String
    get() = singleValue(AnalysisApiReferenceSymbolTestDirectives.EXPECTED_ORIGINAL_PSI_CLASS)

val RegisteredDirectives.expectedRenderedDeclaration: String
    get() = this[AnalysisApiReferenceSymbolTestDirectives.EXPECTED_RENDERED_DECLARATION].joinToString(" ")

package org.cangnova.cangjie.analysis.api.impl.base.test

import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.directives.model.singleValue
import kotlin.text.toBooleanStrict

/**
 * visibility 能力族专用指令。
 *
 * `TARGET_NAME` 继续复用公共 component 指令；
 * 这里补充“目标声明种类 + 期望 visibility + 当前 session 可见性”三类专属元信息，
 * 让 generated 测试可以稳定覆盖 source/local/extend/跨模块场景。
 */
object AnalysisApiVisibilityTestDirectives : SimpleDirectivesContainer() {
    val VISIBILITY_TARGET_KIND by stringDirective(
        description = "visibility 测试目标声明种类，例如 TOP_LEVEL_FUNCTION / BINDING_PATTERN / EXTEND_MEMBER。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_SYMBOL_VISIBILITY by stringDirective(
        description = "目标声明公开 symbol 上暴露的 CaSymbolVisibility。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_VISIBILITY_EXPLICIT by stringDirective(
        description = "目标声明的可见性是否由源码显式写出，取值 true / false。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_IS_VISIBLE by stringDirective(
        description = "目标声明在当前 use-site session 中是否可见，取值 true / false。",
        applicability = DirectiveApplicability.File,
    )
}

val RegisteredDirectives.visibilityTargetKind: String
    get() = singleValue(AnalysisApiVisibilityTestDirectives.VISIBILITY_TARGET_KIND)

val RegisteredDirectives.expectedSymbolVisibility: CaSymbolVisibility
    get() = CaSymbolVisibility.valueOf(singleValue(AnalysisApiVisibilityTestDirectives.EXPECTED_SYMBOL_VISIBILITY))

val RegisteredDirectives.expectedVisibilityExplicit: Boolean
    get() = singleValue(AnalysisApiVisibilityTestDirectives.EXPECTED_VISIBILITY_EXPLICIT).toBooleanStrict()

val RegisteredDirectives.expectedVisibleInSession: Boolean
    get() = singleValue(AnalysisApiVisibilityTestDirectives.EXPECTED_IS_VISIBLE).toBooleanStrict()

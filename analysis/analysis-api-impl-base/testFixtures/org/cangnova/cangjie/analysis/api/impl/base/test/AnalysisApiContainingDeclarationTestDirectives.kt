package org.cangnova.cangjie.analysis.api.impl.base.test

import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer

/**
 * containing declaration 能力族的专用测试指令。
 *
 * 目标引用名称复用公共 `TARGET_NAME`，这里只描述容器链本身的期望输出，
 * 让 containing-declaration 测试与其它按引用定位目标的测试族共享统一入口。
 */
object AnalysisApiContainingDeclarationTestDirectives : SimpleDirectivesContainer() {
    val EXPECTED_CONTAINING_DECLARATION by stringDirective(
        description = "从当前 symbol 向外追踪 containingDeclaration 链时应得到的稳定文本。",
        applicability = DirectiveApplicability.File,
    )
}

val RegisteredDirectives.containingDeclarationTargetName: String
    get() = targetNameText

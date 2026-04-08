package org.cangnova.cangjie.analysis.api.impl.base.test

import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.directives.model.singleValue

/**
 * reference 行为测试族专用指令。
 *
 * `TARGET_NAME` 已经由公共 component 指令骨架统一声明，这里只保留
 * reference 行为族独有的期望字段，避免同名 directive 在测试框架中发生解析冲突。
 */
object AnalysisApiReferenceBehaviorTestDirectives : SimpleDirectivesContainer() {
    val TARGET_KIND by stringDirective(
        description = "reference 行为测试目标声明的种类。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_TARGET_CLASS by stringDirective(
        description = "reference 行为测试目标 PSI 的简单类名。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_ALIAS_NAME by stringDirective(
        description = "import alias 测试中目标 alias 的名字。",
        applicability = DirectiveApplicability.File,
    )
}

val RegisteredDirectives.referenceBehaviorTargetKind: String
    get() = singleValue(AnalysisApiReferenceBehaviorTestDirectives.TARGET_KIND)

val RegisteredDirectives.referenceBehaviorTargetName: String
    get() = targetNameText

val RegisteredDirectives.expectedReferenceTargetClass: String
    get() = singleValue(AnalysisApiReferenceBehaviorTestDirectives.EXPECTED_TARGET_CLASS)

val RegisteredDirectives.expectedAliasName: String
    get() = singleValue(AnalysisApiReferenceBehaviorTestDirectives.EXPECTED_ALIAS_NAME)

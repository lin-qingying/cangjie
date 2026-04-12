package org.cangnova.cangjie.analysis.api.impl.base.test

import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer

/**
 * element.diagnostics 测试指令。
 */
object AnalysisApiElementDiagnosticTestDirectives : SimpleDirectivesContainer() {
    val TARGET_ELEMENT_TEXT by stringDirective(
        description = "当前 element diagnostics 用例要查询的元素文本，要求在主文件中唯一。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_ELEMENT_DIAGNOSTIC by stringDirective(
        description = "目标元素上应可见的诊断 factoryName，可重复声明。",
        applicability = DirectiveApplicability.File,
    )
}

val RegisteredDirectives.diagnosticTargetElementText: String
    get() = this[AnalysisApiElementDiagnosticTestDirectives.TARGET_ELEMENT_TEXT].joinToString(", ")

val RegisteredDirectives.expectedElementDiagnostics: List<String>
    get() = this[AnalysisApiElementDiagnosticTestDirectives.EXPECTED_ELEMENT_DIAGNOSTIC].sorted()

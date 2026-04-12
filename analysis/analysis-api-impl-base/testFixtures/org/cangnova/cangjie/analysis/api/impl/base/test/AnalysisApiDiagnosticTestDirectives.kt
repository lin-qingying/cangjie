package org.cangnova.cangjie.analysis.api.impl.base.test

import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer

/**
 * diagnostics generated 测试指令。
 *
 * 这里不暴露底层 `CjPsiDiagnostic` 细节，只固定 public Analysis API
 * 能稳定承诺的诊断工厂名列表，并按不同 checker filter 分开建模。
 */
object AnalysisApiDiagnosticTestDirectives : SimpleDirectivesContainer() {
    val EXPECTED_COMMON_DIAGNOSTIC by stringDirective(
        description = "ONLY_COMMON_CHECKERS 下应出现的 factoryName，可重复声明。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_EXTENDED_DIAGNOSTIC by stringDirective(
        description = "ONLY_EXTENDED_CHECKERS 下应出现的 factoryName，可重复声明。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_EXPERIMENTAL_DIAGNOSTIC by stringDirective(
        description = "ONLY_EXPERIMENTAL_CHECKERS 下应出现的 factoryName，可重复声明。",
        applicability = DirectiveApplicability.File,
    )
}

val RegisteredDirectives.expectedCommonDiagnostics: List<String>
    get() = this[AnalysisApiDiagnosticTestDirectives.EXPECTED_COMMON_DIAGNOSTIC].sorted()

val RegisteredDirectives.expectedExtendedDiagnostics: List<String>
    get() = this[AnalysisApiDiagnosticTestDirectives.EXPECTED_EXTENDED_DIAGNOSTIC].sorted()

val RegisteredDirectives.expectedExperimentalDiagnostics: List<String>
    get() = this[AnalysisApiDiagnosticTestDirectives.EXPECTED_EXPERIMENTAL_DIAGNOSTIC].sorted()

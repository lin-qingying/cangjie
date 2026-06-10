package org.cangnova.cangjie.cfir.diagnostics

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.cfir.diagnostics.rendering.BaseDiagnosticRendererFactory

/**
 * 官方词法/语法前端诊断。
 *
 * LLT 官方测试数据保留了 cjc 的 `lex_*` 诊断名；这些诊断来自解析阶段，
 * 不属于 CFIR 语义 checker，因此独立于 [org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors]。
 */
object CjSyntaxErrors : CjDiagnosticsContainer() {
    val LEX_UNKNOWN_SUFFIX: CjDiagnosticFactory0 = CjDiagnosticFactory0(
        "lex_unknown_suffix",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        PsiElement::class,
        getRendererFactory(),
    )

    val LEX_UNEXPECTED_DIGIT: CjDiagnosticFactory0 = CjDiagnosticFactory0(
        "lex_unexpected_digit",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        PsiElement::class,
        getRendererFactory(),
    )

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = CjSyntaxErrorsDefaultMessages

    fun factoryForParserMessage(message: String?): CjDiagnosticFactory0? {
        val normalized = message.orEmpty().lowercase()
        return when {
            "unknown suffix" in normalized -> LEX_UNKNOWN_SUFFIX
            "unexpected digit" in normalized -> LEX_UNEXPECTED_DIGIT
            else -> null
        }
    }
}

object CjSyntaxErrorsDefaultMessages : BaseDiagnosticRendererFactory() {
    override val MAP by CjDiagnosticFactoryToRendererMap("CjSyntaxErrors") { map ->
        map.put(CjSyntaxErrors.LEX_UNKNOWN_SUFFIX, "Unknown suffix for number literal")
        map.put(CjSyntaxErrors.LEX_UNEXPECTED_DIGIT, "Unexpected digit in number literal")
    }
}

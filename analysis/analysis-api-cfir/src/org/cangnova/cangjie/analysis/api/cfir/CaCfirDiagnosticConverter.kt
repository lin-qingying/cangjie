package org.cangnova.cangjie.analysis.api.cfir

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi
import org.cangnova.cangjie.analysis.api.diagnostics.CaSeverity
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.Severity
import kotlin.reflect.KClass

/**
 * CjPsiDiagnostic → CaDiagnosticWithPsi 的转换（对齐 K2 的 asKaDiagnostic()）。
 */
internal fun CjPsiDiagnostic.asCaDiagnostic(token: CaLifetimeToken): CaDiagnosticWithPsi<PsiElement> {
    return CaCfirDiagnostic(this, token)
}

/**
 * CFIR 诊断的 Analysis API 包装类。
 *
 * 将底层 [CjPsiDiagnostic] 适配为 [CaDiagnosticWithPsi] 接口。
 */
private class CaCfirDiagnostic(
    private val psiDiagnostic: CjPsiDiagnostic,
    override val token: CaLifetimeToken,
) : CaDiagnosticWithPsi<PsiElement> {
    override val psi: PsiElement
        get() = psiDiagnostic.psiElement

    override val textRanges: Collection<TextRange>
        get() = psiDiagnostic.textRanges

    /**
     * Analysis API 对外暴露的是稳定的诊断语义名，不泄漏 CFIR 实现前缀。
     */
    override val factoryName: String
        get() = psiDiagnostic.factoryName.removePrefix("CFIR_")

    override val severity: CaSeverity
        get() = psiDiagnostic.severity.toCaSeverity()

    override val defaultMessage: String
        get() = (psiDiagnostic as CjDiagnostic).renderMessage()

    override val diagnosticClass: KClass<out CaDiagnosticWithPsi<PsiElement>>
        get() = CaCfirDiagnostic::class
}

internal fun Severity.toCaSeverity(): CaSeverity = when (this) {
    Severity.ERROR -> CaSeverity.ERROR
    Severity.WARNING, Severity.STRONG_WARNING, Severity.FIXED_WARNING -> CaSeverity.WARNING
    Severity.INFO -> CaSeverity.INFO
}

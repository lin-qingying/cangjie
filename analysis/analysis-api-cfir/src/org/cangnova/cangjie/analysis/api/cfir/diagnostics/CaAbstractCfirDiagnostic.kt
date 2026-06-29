package org.cangnova.cangjie.analysis.api.cfir.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi
import org.cangnova.cangjie.analysis.api.diagnostics.CaSeverity
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.Severity

/**
 * CFIR typed diagnostics 的公共基类。
 *
 * 该类对齐 Kotlin `KaAbstractFirDiagnostic` 的职责：统一承载底层诊断、生命周期 token、
 * PSI、文本范围、默认消息和严重级别。具体诊断参数由生成的 `CaCfirDiagnostic.*` 接口声明。
 */
internal abstract class CaAbstractCfirDiagnostic<PSI : PsiElement>(
    /**
     * 底层 CFIR PSI 诊断对象。
     */
    private val cfirDiagnostic: CjPsiDiagnostic,
    /**
     * 诊断对象所属的生命周期令牌。
     */
    override val token: CaLifetimeToken,
) : CaDiagnosticWithPsi<PSI>, CaLifetimeOwner {

    /**
     * 去掉 CFIR 前缀后的诊断工厂名。
     */
    override val factoryName: String
        get() = withValidityAssertion { cfirDiagnostic.factory.name.removePrefix("CFIR_") }

    /**
     * 底层诊断 renderer 生成的默认消息。
     */
    override val defaultMessage: String
        get() = withValidityAssertion { (cfirDiagnostic as CjDiagnostic).renderMessage() }

    /**
     * 诊断覆盖的文本范围集合。
     */
    override val textRanges: Collection<TextRange>
        get() = withValidityAssertion { cfirDiagnostic.textRanges }

    /**
     * 诊断绑定的 PSI 元素。
     */
    @Suppress("UNCHECKED_CAST")
    override val psi: PSI
        get() = withValidityAssertion { cfirDiagnostic.psiElement as PSI }

    /**
     * Analysis API 公开诊断严重级别。
     */
    override val severity: CaSeverity
        get() = withValidityAssertion { cfirDiagnostic.severity.toCaSeverity() }
}

/**
 * 将 CFIR 诊断严重级别转换为 Analysis API 公开严重级别。
 */
internal fun Severity.toCaSeverity(): CaSeverity = when (this) {
    Severity.ERROR -> CaSeverity.ERROR
    Severity.WARNING, Severity.STRONG_WARNING, Severity.FIXED_WARNING -> CaSeverity.WARNING
    Severity.INFO -> CaSeverity.INFO
}

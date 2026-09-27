package org.cangnova.cangjie.analysis.api.cfir.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticRelatedInformation
import org.cangnova.cangjie.analysis.api.diagnostics.CaSeverity
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.Severity
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjPsiSourceElement

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

    /** Analysis API 相关来源的生命周期安全实现。 */
    private class RelatedInformationImpl(
        private val relatedPsi: PsiElement?,
        private val relatedTextRange: TextRange?,
        private val relatedFilePath: String?,
        private val relatedLine: Int?,
        private val relatedColumn: Int?,
        private val relatedMessage: String,
        override val token: CaLifetimeToken,
    ) : CaDiagnosticRelatedInformation {
        override val psi: PsiElement?
            get() = withValidityAssertion { relatedPsi }

        override val textRange: TextRange?
            get() = withValidityAssertion { relatedTextRange }

        override val filePath: String?
            get() = withValidityAssertion { relatedFilePath }

        override val line: Int?
            get() = withValidityAssertion { relatedLine }

        override val column: Int?
            get() = withValidityAssertion { relatedColumn }

        override val message: String
            get() = withValidityAssertion { relatedMessage }
    }

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

    /** 公开底层 CFIR diagnostic 附带的 peer 源码位置与说明。 */
    override val relatedInformation: List<CaDiagnosticRelatedInformation>
        get() = withValidityAssertion {
            (cfirDiagnostic as CjDiagnostic).relatedInformation.map { information ->
                val source = information.element
                val psiSource = when (source) {
                    is CjPsiSourceElement -> source
                    is CjLightSourceElement -> source.unwrapToCjPsiSourceElement()
                    else -> null
                }
                val psi = psiSource?.psi
                RelatedInformationImpl(
                    relatedPsi = psi,
                    relatedTextRange = psi?.let { TextRange(0, it.textLength) },
                    relatedFilePath = information.sourceLocation?.filePath
                        ?: psi?.containingFile?.virtualFile?.path
                        ?: (source as? CjBinarySourceElement)?.binaryFilePath,
                    relatedLine = information.sourceLocation?.line,
                    relatedColumn = information.sourceLocation?.column,
                    relatedMessage = information.message,
                    token = token,
                )
            }
        }
}

/**
 * 将 CFIR 诊断严重级别转换为 Analysis API 公开严重级别。
 */
internal fun Severity.toCaSeverity(): CaSeverity = when (this) {
    Severity.ERROR -> CaSeverity.ERROR
    Severity.WARNING, Severity.STRONG_WARNING, Severity.FIXED_WARNING -> CaSeverity.WARNING
    Severity.INFO -> CaSeverity.INFO
}

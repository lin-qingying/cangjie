package org.cangnova.cangjie.analysis.api.cfir.components

import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaReferenceShortener
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.imports.CaReferenceShorteningCommand
import org.cangnova.cangjie.analysis.api.imports.CaReferenceShorteningPlan
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile

/**
 * 引用缩短规划入口。
 */
internal class CaCfirReferenceShortener(
    /**
     * 延迟取得当前 CFIR Analysis session，具体规划逻辑委托给 session 级实现。
     */
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaReferenceShortener {
    /**
     * 收集整个文件内可执行的引用缩短规划。
     */
    override fun CjFile.collectReferenceShorteningPlan(): CaReferenceShorteningPlan = withValidityAssertion {
        analysisSession.collectReferenceShorteningPlan(this@collectReferenceShorteningPlan)
    }

    /**
     * 收集指定文本范围内的引用缩短命令。
     */
    override fun CjFile.collectReferenceShortenings(selection: TextRange): CaReferenceShorteningCommand = withValidityAssertion {
        analysisSession.collectReferenceShortenings(
            file = this@collectReferenceShortenings,
            selection = selection,
        )
    }

    /**
     * 收集当前 PSI 元素文本范围内的引用缩短命令。
     */
    override fun CjElement.collectReferenceShorteningsInElement(): CaReferenceShorteningCommand = withValidityAssertion {
        val file = containingFile as? CjFile
            ?: error("引用缩短命令只能在 CjFile 上下文中收集：${this@collectReferenceShorteningsInElement::class.simpleName}")
        analysisSession.collectReferenceShortenings(
            file = file,
            selection = textRange,
        )
    }
}

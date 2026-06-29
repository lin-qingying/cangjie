package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaOriginalPsiProvider
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.originalCjFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.originalDeclaration
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjFile

/**
 * 对位 Kotlin `KaFirOriginalPsiProvider` 的原始 PSI provider。
 */
internal class CaCfirOriginalPsiProvider(
    /**
     * 延迟取得当前 CFIR Analysis session。
     */
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaOriginalPsiProvider, CaCfirSessionComponent {
    /**
     * 记录当前文件对应的原始仓颉文件。
     */
    @Deprecated("Obsolete API")
    override fun CjFile.recordOriginalCjFile(file: CjFile) = withValidityAssertion {
        originalCjFile = file
    }

    /**
     * 记录当前声明对应的原始仓颉声明。
     */
    @Deprecated("Obsolete API")
    override fun CjDeclaration.recordOriginalDeclaration(declaration: CjDeclaration) = withValidityAssertion {
        originalDeclaration = declaration
    }

    /**
     * 读取当前文件记录的原始仓颉文件。
     */
    @Deprecated("Obsolete API")
    override fun CjFile.getOriginalCjFile(): CjFile? = withValidityAssertion {
        return originalCjFile
    }

    /**
     * 读取当前声明记录的原始仓颉声明。
     */
    @Deprecated("Obsolete API")
    override fun CjDeclaration.getOriginalDeclaration(): CjDeclaration? = withValidityAssertion {
        return originalDeclaration
    }
}

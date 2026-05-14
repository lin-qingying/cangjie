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
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaOriginalPsiProvider, CaCfirSessionComponent {
    @Deprecated("Obsolete API")
    override fun CjFile.recordOriginalCjFile(file: CjFile) = withValidityAssertion {
        originalCjFile = file
    }

    @Deprecated("Obsolete API")
    override fun CjDeclaration.recordOriginalDeclaration(declaration: CjDeclaration) = withValidityAssertion {
        originalDeclaration = declaration
    }

    @Deprecated("Obsolete API")
    override fun CjFile.getOriginalCjFile(): CjFile? = withValidityAssertion {
        return originalCjFile
    }

    @Deprecated("Obsolete API")
    override fun CjDeclaration.getOriginalDeclaration(): CjDeclaration? = withValidityAssertion {
        return originalDeclaration
    }
}

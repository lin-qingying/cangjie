package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.*

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirBackedSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirExtendSymbol
import org.cangnova.cangjie.analysis.api.components.CaOriginalPsiProvider
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.originalCjFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.originalDeclaration
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjFile

/**
 * 原始 PSI 定位入口。
 *
 * 优先返回声明自身对应的 PSI；若当前 symbol 只存在 decompiled 载体，
 * 则统一退回到共享的 decompiled source 导航支持层。
 */
internal class CaCfirOriginalPsiProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaOriginalPsiProvider,CaCfirSessionComponent {
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

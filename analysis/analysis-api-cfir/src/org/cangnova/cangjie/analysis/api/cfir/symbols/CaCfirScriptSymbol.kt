package org.cangnova.cangjie.analysis.api.cfir.symbols

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.components.asCaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.components.renderAnnotations
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirScriptSymbolPointer
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaAnnotatedSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaScriptSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjScript

/**
 * script 叶子实现。
 *
 * 对齐 Kotlin 的 `KaFirScriptSymbol` 落位，把 script 叶子从巨型模型文件中拆出。
 */
internal class CaCfirScriptSymbolImpl(
    internal val scriptPsi: CjScript,
    private val scriptFileSymbol: CaFileSymbol?,
    private val analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirSymbolBase(containingModule, token), CaScriptSymbol, CaNamedSymbol {
    override val annotations: CaAnnotationList
        get() = withValidityAssertion {
            analysisSession.renderAnnotations(scriptPsi).asCaAnnotationList(token)
        }

    override val origin: CaSymbolOrigin
        get() = if (scriptPsi.containingCjFile.isCompiled) CaSymbolOrigin.LIBRARY else CaSymbolOrigin.SOURCE

    override val location: CaSymbolLocation
        get() = CaSymbolLocation.TOP_LEVEL

    override val psi: PsiElement
        get() = scriptPsi

    override val containingDeclaration: CaSymbol?
        get() = scriptFileSymbol

    override val visibility: CaSymbolVisibility
        get() = CaSymbolVisibility.PUBLIC

    override val isVisibilityExplicit: Boolean
        get() = false

    override val modality: CaSymbolModality?
        get() = CaSymbolModality.FINAL

    override val isModalityExplicit: Boolean
        get() = false

    override val name: Name
        get() = scriptPsi.nameAsSafeName

    override val fileSymbol: CaFileSymbol?
        get() = scriptFileSymbol

    override fun createPointer(): CaSymbolPointer<CaAnnotatedSymbol> = withValidityAssertion {
        CaCfirScriptSymbolPointer(scriptPsi)
    }
}

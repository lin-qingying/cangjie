package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.components.asCaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.components.renderAnnotations
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirValueParameterSymbolPointer
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaValueParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.psi

/**
 * 值参数叶子实现。
 *
 * 值参数的 owner 恢复、稳定索引和默认值语义都与普通局部变量不同，
 * 因此单独落位，避免继续依赖“大而全”的变量族文件。
 */
internal class CaCfirValueParameterSymbolImpl(
    final override val backingSymbol: CfirValueParameterSymbol,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
    internal val ownerSymbol: CaValueParameterOwnerSymbol? = null,
    internal val stableParameterIndex: Int? = null,
    private val parameterPsi: org.cangnova.cangjie.psi.CjParameter? = null,
) : CaValueParameterSymbol(), CaCfirVariableSymbolSupport<CfirValueParameterSymbol> {
    private val parameterDeclaration: CfirValueParameter
        get() = backingSymbol.cfir

    override val annotations: CaAnnotationList
        get() = withValidityAssertion { analysisSession.renderAnnotations(this).asCaAnnotationList(token) }

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = null

    override val receiverType: CaType?
        get() = receiverTypeImpl

    override val returnType: CaType
        get() = returnTypeImpl

    override val location: CaSymbolLocation
        get() = CaSymbolLocation.LOCAL

    override fun createPointer(): CaSymbolPointer<CaCallableSymbol> = withValidityAssertion {
        val owner = (ownerSymbol ?: containingDeclaration) as? CaSymbol
            ?: error("Value parameter `${name}` is missing pointer-restorable owner")
        val parameterIndex = stableParameterIndex
            ?: error("Value parameter `${name}` is missing stable parameter index")
        CaCfirValueParameterSymbolPointer(owner.createPointer(), name, parameterIndex)
    }

    override val name: Name
        get() = backingSymbol.name

    override val isLet: Boolean
        get() = !parameterDeclaration.isVar

    override val isNamed: Boolean
        get() = parameterDeclaration.isNamed

    override val isVararg: Boolean
        get() = resolvedParameterPsi?.isVarArg == true

    override val hasDefaultValue: Boolean
        get() = parameterDeclaration.defaultValue != null || resolvedParameterPsi?.defaultValue != null

    override val visibility: CaSymbolVisibility
        get() = CaSymbolVisibility.LOCAL

    override val isVisibilityExplicit: Boolean
        get() = false

    override val modality: CaSymbolModality?
        get() = CaSymbolModality.FINAL

    override val isModalityExplicit: Boolean
        get() = false

    override val containingDeclaration: CaSymbol?
        get() = ownerSymbol ?: analysisSession.findContainingDeclarationSymbol(psi)

    private val resolvedParameterPsi: org.cangnova.cangjie.psi.CjParameter?
        get() = parameterDeclaration.source?.psi as? org.cangnova.cangjie.psi.CjParameter
            ?: parameterPsi
            ?: psi as? org.cangnova.cangjie.psi.CjParameter
}

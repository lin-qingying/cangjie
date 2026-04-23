package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.findPsi
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
internal class CaCfirValueParameterSymbol private constructor(
    override val backingPsi: org.cangnova.cangjie.psi.CjParameter?,
    override val analysisSession: CaCfirSession,
    override val lazyCfirSymbol: Lazy<CfirValueParameterSymbol>,
    internal val ownerSymbol: CaValueParameterOwnerSymbol? = null,
    internal val stableParameterIndex: Int? = null,
    private val explicitParameterPsi: org.cangnova.cangjie.psi.CjParameter? = null,
) : CaValueParameterSymbol(),
    CaCfirCjBasedSymbol<org.cangnova.cangjie.psi.CjParameter, CfirValueParameterSymbol>,
    CaCfirVariableSymbolSupport<CfirValueParameterSymbol> {
    override val cfirSymbol: CfirValueParameterSymbol
        get() = super<CaCfirCjBasedSymbol>.cfirSymbol

    constructor(declaration: org.cangnova.cangjie.psi.CjParameter, session: CaCfirSession) : this(
        backingPsi = declaration,
        analysisSession = session,
        lazyCfirSymbol = lazyCfirSymbol(declaration, session),
    )

    constructor(
        symbol: CfirValueParameterSymbol,
        session: CaCfirSession,
        ownerSymbol: CaValueParameterOwnerSymbol? = null,
        stableParameterIndex: Int? = null,
        parameterPsi: org.cangnova.cangjie.psi.CjParameter? = null,
    ) : this(
        backingPsi = symbol.backingPsiIfApplicable as? org.cangnova.cangjie.psi.CjParameter ?: parameterPsi,
        analysisSession = session,
        lazyCfirSymbol = lazyOf(symbol),
        ownerSymbol = ownerSymbol,
        stableParameterIndex = stableParameterIndex,
        explicitParameterPsi = parameterPsi,
    )

    override val backingSymbol: CfirValueParameterSymbol
        get() = cfirSymbol

    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    private val parameterDeclaration: CfirValueParameter
        get() = backingSymbol.cfir

    override val annotations: CaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    override val psi
        get() = withValidityAssertion { backingPsi ?: findPsi() }

    override val origin
        get() = withValidityAssertion { psiOrSymbolOrigin() }

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = null

    override val receiverType: CaType?
        get() = withValidityAssertion { receiverTypeImpl }

    override val returnType: CaType
        get() = withValidityAssertion { returnTypeImpl }

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
        get() = withValidityAssertion { backingPsi?.nameAsSafeName ?: backingSymbol.name }

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
            ?: explicitParameterPsi
            ?: psi as? org.cangnova.cangjie.psi.CjParameter
}

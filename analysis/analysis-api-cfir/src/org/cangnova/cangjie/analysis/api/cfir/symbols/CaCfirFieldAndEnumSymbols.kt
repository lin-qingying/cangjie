package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.findPsi
import org.cangnova.cangjie.analysis.api.cfir.getExplicitCallableReceiverType
import org.cangnova.cangjie.analysis.api.cfir.location
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor as CfirEnumConstructorDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.payloadParameterTypesOrEmpty
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.CjFieldVariable

/**
 * 字段与枚举构造器叶子实现。
 *
 * 这两类都属于 variable/callable 分支，但公开语义与 property、value parameter 完全不同，
 * 独立落位后更容易维持稳定的 pointer 与宿主恢复规则。
 */
internal class CaCfirFieldSymbol private constructor(
    override val backingPsi: CjFieldVariable?,
    override val analysisSession: CaCfirSession,
    override val lazyCfirSymbol: Lazy<CfirFieldVariableSymbol>,
) : CaFieldSymbol(),
    CaCfirCjBasedSymbol<CjFieldVariable, CfirFieldVariableSymbol> {
    constructor(declaration: CjFieldVariable, session: CaCfirSession) : this(
        backingPsi = declaration,
        analysisSession = session,
        lazyCfirSymbol = lazyCfirSymbol(declaration, session),
    )

    constructor(symbol: CfirFieldVariableSymbol, session: CaCfirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? CjFieldVariable,
        analysisSession = session,
        lazyCfirSymbol = lazyOf(symbol),
    )

    override val cfirSymbol: CfirFieldVariableSymbol
        get() = super<CaCfirCjBasedSymbol>.cfirSymbol

    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    private val status
        get() = (cfirSymbol.cfir as? CfirMemberDeclaration)?.status

    override val psi
        get() = withValidityAssertion { backingPsi ?: findPsi() }

    override val origin
        get() = withValidityAssertion { psiOrSymbolOrigin() }

    override val annotations: CaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = cfirSymbol.getCallableId()

    override val receiverType: CaType?
        get() = analysisSession.getExplicitCallableReceiverType(cfirSymbol, backingPsi = null, builder)

    override val returnType: CaType
        get() = cfirSymbol.returnType(builder)

    override val location: CaSymbolLocation
        get() = if (backingPsi != null) CaSymbolLocation.CLASS
        else if (cfirSymbol.callableId.classId == null) CaSymbolLocation.TOP_LEVEL
        else CaSymbolLocation.CLASS

    override val visibility: CaSymbolVisibility
        get() = withValidityAssertion { status?.visibility?.asPublicVisibility() ?: CaSymbolVisibility.PUBLIC }

    override val isVisibilityExplicit: Boolean
        get() = withValidityAssertion { status?.isVisibilityExplicit == true }

    override val modality: CaSymbolModality?
        get() = withValidityAssertion { status?.modality?.asPublicModality() }

    override val isModalityExplicit: Boolean
        get() = withValidityAssertion { status?.isModalityExplicit == true }

    override fun createPointer(): CaSymbolPointer<CaCallableSymbol> = withValidityAssertion {
        error("Field symbol cannot create a stable pointer")
    }

    override val isLet: Boolean
        get() = withValidityAssertion { backingPsi?.isVar != true }

    override val isStatic: Boolean
        get() = withValidityAssertion { backingPsi?.isStatic ?: (status?.isStatic == true) }

    override val isConst: Boolean
        get() = withValidityAssertion { backingPsi?.isConst ?: (status?.isConst == true) }

    override val name: Name
        get() = withValidityAssertion { backingPsi?.nameAsSafeName ?: cfirSymbol.name }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()
}

internal class CaCfirEnumConstructorSymbol private constructor(
    override val backingPsi: CjEnumConstructor?,
    override val analysisSession: CaCfirSession,
    override val lazyCfirSymbol: Lazy<CfirEnumConstructorSymbol>,
) : CaEnumConstructorSymbol(),
    CaCfirCjBasedSymbol<CjEnumConstructor, CfirEnumConstructorSymbol>,
    CaNamedSymbol {
    constructor(declaration: CjEnumConstructor, session: CaCfirSession) : this(
        backingPsi = declaration,
        analysisSession = session,
        lazyCfirSymbol = lazyCfirSymbol<CfirEnumConstructorDeclaration, CfirEnumConstructorSymbol>(
            declaration,
            session,
        ) { constructor -> constructor.symbol },
    )

    constructor(symbol: CfirEnumConstructorSymbol, session: CaCfirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? CjEnumConstructor,
        analysisSession = session,
        lazyCfirSymbol = lazyOf(symbol),
    )

    override val cfirSymbol: CfirEnumConstructorSymbol
        get() = super<CaCfirCjBasedSymbol>.cfirSymbol

    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    private val status
        get() = (cfirSymbol.cfir as? CfirMemberDeclaration)?.status

    override val psi
        get() = withValidityAssertion { backingPsi ?: findPsi() }

    override val origin
        get() = withValidityAssertion { psiOrSymbolOrigin() }

    override val annotations: CaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = cfirSymbol.getCallableId()

    override val receiverType: CaType?
        get() = analysisSession.getExplicitCallableReceiverType(cfirSymbol, backingPsi = null, builder)

    override val returnType: CaType
        get() = cfirSymbol.returnType(builder)

    override val location: CaSymbolLocation
        get() = CaSymbolLocation.CLASS

    override val visibility: CaSymbolVisibility
        get() = withValidityAssertion { status?.visibility?.asPublicVisibility() ?: CaSymbolVisibility.PUBLIC }

    override val isVisibilityExplicit: Boolean
        get() = withValidityAssertion { status?.isVisibilityExplicit == true }

    override val modality: CaSymbolModality?
        get() = withValidityAssertion { status?.modality?.asPublicModality() }

    override val isModalityExplicit: Boolean
        get() = withValidityAssertion { status?.isModalityExplicit == true }

    override fun createPointer(): CaSymbolPointer<CaCallableSymbol> = withValidityAssertion {
        error("Enum constructor symbol cannot create a stable pointer")
    }

    override val name: Name
        get() = withValidityAssertion { backingPsi?.name?.let(Name::identifier) ?: cfirSymbol.name }

    override val containingEnumClassId: ClassId?
        get() = withValidityAssertion {
            backingPsi?.parentEnum?.getClassId() ?: analysisSession.cfirSession.cfirProvider.getContainingClass(cfirSymbol)?.classId
        }

    override val payloadTypes: List<CaType>
        get() = withValidityAssertion { cfirSymbol.cfir.payloadParameterTypesOrEmpty().map(builder.typeBuilder::buildType) }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()
}

package org.cangnova.cangjie.analysis.api.cfir.symbols

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.findPsi
import org.cangnova.cangjie.analysis.api.cfir.getAllowedPsi
import org.cangnova.cangjie.analysis.api.cfir.location
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirAnonymousFunctionSymbolPointer
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaAnonymousFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFinalizerSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.symbols.CfirAnonymousFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFinalizerSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjFinalizer
import org.cangnova.cangjie.psi.CjFunction
import org.cangnova.cangjie.psi.CjFunctionLiteral

/**
 * 局部或生命周期函数叶子实现。
 *
 * 匿名函数、析构器这类函数虽然都属于 `CaFunctionSymbol` 族，
 * 但它们的公开语义和 pointer/宿主恢复策略不同，单独落位更接近 Kotlin FIR 的叶子组织方式。
 */
internal class CaCfirAnonymousFunctionSymbol private constructor(
    override val backingPsi: CjFunction?,
    override val analysisSession: CaCfirSession,
    override val lazyCfirSymbol: Lazy<CfirAnonymousFunctionSymbol>,
) : CaAnonymousFunctionSymbol(),
    CaCfirCjBasedSymbol<CjFunction, CfirAnonymousFunctionSymbol> {
    constructor(declaration: CjFunctionLiteral, session: CaCfirSession) : this(
        backingPsi = declaration,
        analysisSession = session,
        lazyCfirSymbol = lazyCfirSymbol<CfirAnonymousFunctionExpression, CfirAnonymousFunctionSymbol>(
            declaration,
            session,
        ) { expression -> expression.anonymousFunction.symbol },
    )

    constructor(symbol: CfirAnonymousFunctionSymbol, session: CaCfirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? CjFunction,
        analysisSession = session,
        lazyCfirSymbol = lazyOf(symbol),
    )

    override val cfirSymbol: CfirAnonymousFunctionSymbol
        get() = super<CaCfirCjBasedSymbol>.cfirSymbol

    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    private val status
        get() = (cfirSymbol.cfir as? CfirMemberDeclaration)?.status

    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsi ?: cfirSymbol.cfir.getAllowedPsi() }

    override val origin
        get() = withValidityAssertion { psiOrSymbolOrigin() }

    override val annotations: CaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    override val receiverType: CaType?
        get() = withValidityAssertion { (cfirSymbol.cfir as? CfirCallableDeclaration)?.dispatchReceiverType?.let(builder.typeBuilder::buildType) }

    override val returnType: CaType
        get() = withValidityAssertion { createReturnType() }

    override val location: CaSymbolLocation
        get() = CaSymbolLocation.LOCAL

    override fun createPointer(): CaSymbolPointer<CaFunctionSymbol> = withValidityAssertion {
        val sourcePsi = psi ?: error("Anonymous function symbol is missing PSI")
        @Suppress("UNCHECKED_CAST")
        CaCfirAnonymousFunctionSymbolPointer(sourcePsi) as CaSymbolPointer<CaFunctionSymbol>
    }

    override val isStatic: Boolean
        get() = withValidityAssertion { status?.isStatic == true }

    override val isConst: Boolean
        get() = withValidityAssertion { status?.isConst == true }

    override val isMutating: Boolean
        get() = withValidityAssertion { status?.isMut == true }

    override val isOverride: Boolean
        get() = withValidityAssertion { status?.isOverride == true }

    override val isOperator: Boolean
        get() = withValidityAssertion { status?.isOperator == true }

    override val isUnsafe: Boolean
        get() = withValidityAssertion { status?.isUnsafe == true }

    override val isForeign: Boolean
        get() = withValidityAssertion { status?.isForeign == true }

    override val typeParameters: List<CaTypeParameterSymbol>
        get() = withValidityAssertion {
            createCaTypeParameters() ?: (cfirSymbol.cfir as? CfirCallableDeclaration)
                ?.typeParameters
                ?.map { typeParameter -> builder.classifierBuilder.buildTypeParameterSymbol(typeParameter.symbol) }
                .orEmpty()
        }

    override val valueParameters: List<CaValueParameterSymbol>
        get() = withValidityAssertion {
            createCaValueParameters() ?: (cfirSymbol.cfir as? CfirFunction)
                ?.valueParameters
                ?.map { valueParameter -> builder.variableBuilder.buildValueParameterSymbol(valueParameter.symbol) }
                .orEmpty()
        }

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = null

    override val visibility: CaSymbolVisibility
        get() = CaSymbolVisibility.LOCAL

    override val isVisibilityExplicit: Boolean
        get() = false

    override val modality: CaSymbolModality?
        get() = CaSymbolModality.FINAL

    override val isModalityExplicit: Boolean
        get() = false

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()
}

internal class CaCfirFinalizerSymbol private constructor(
    override val backingPsi: CjFinalizer?,
    override val analysisSession: CaCfirSession,
    override val lazyCfirSymbol: Lazy<CfirFinalizerSymbol>,
) : CaFinalizerSymbol(),
    CaCfirCjBasedSymbol<CjFinalizer, CfirFinalizerSymbol> {
    constructor(declaration: CjFinalizer, session: CaCfirSession) : this(
        backingPsi = declaration,
        analysisSession = session,
        lazyCfirSymbol = lazyCfirSymbol(declaration, session),
    )

    constructor(symbol: CfirFinalizerSymbol, session: CaCfirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? CjFinalizer,
        analysisSession = session,
        lazyCfirSymbol = lazyOf(symbol),
    )

    override val cfirSymbol: CfirFinalizerSymbol
        get() = super<CaCfirCjBasedSymbol>.cfirSymbol

    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    private val status
        get() = (cfirSymbol.cfir as? CfirMemberDeclaration)?.status

    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsi ?: findPsi() }

    override val origin
        get() = withValidityAssertion { psiOrSymbolOrigin() }

    override val annotations: CaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = withValidityAssertion {
            val callableDeclaration = cfirSymbol.cfir as? CfirCallableDeclaration
            cfirSymbol.callableId.takeUnless { callableDeclaration?.isLocal == true }
        }

    override val receiverType: CaType?
        get() = withValidityAssertion { (cfirSymbol.cfir as? CfirCallableDeclaration)?.dispatchReceiverType?.let(builder.typeBuilder::buildType) }

    override val returnType: CaType
        get() = withValidityAssertion { createReturnType() }

    override val location: CaSymbolLocation
        get() = withValidityAssertion { backingPsi?.location ?: CaSymbolLocation.CLASS }

    override val visibility: CaSymbolVisibility
        get() = withValidityAssertion { status?.visibility?.asPublicVisibility() ?: CaSymbolVisibility.PUBLIC }

    override val isVisibilityExplicit: Boolean
        get() = withValidityAssertion { status?.isVisibilityExplicit == true }

    override val modality: CaSymbolModality?
        get() = withValidityAssertion { status?.modality?.asPublicModality() }

    override val isModalityExplicit: Boolean
        get() = withValidityAssertion { status?.isModalityExplicit == true }

    override fun createPointer(): CaSymbolPointer<CaFunctionSymbol> = withValidityAssertion {
        error("Finalizer symbol cannot create a stable pointer")
    }

    override val isStatic: Boolean
        get() = withValidityAssertion { status?.isStatic == true }

    override val isConst: Boolean
        get() = withValidityAssertion { status?.isConst == true }

    override val isMutating: Boolean
        get() = withValidityAssertion { status?.isMut == true }

    override val isOverride: Boolean
        get() = withValidityAssertion { status?.isOverride == true }

    override val isOperator: Boolean
        get() = withValidityAssertion { status?.isOperator == true }

    override val isUnsafe: Boolean
        get() = withValidityAssertion { status?.isUnsafe == true }

    override val isForeign: Boolean
        get() = withValidityAssertion { status?.isForeign == true }

    override val typeParameters: List<CaTypeParameterSymbol>
        get() = withValidityAssertion {
            createCaTypeParameters() ?: (cfirSymbol.cfir as? CfirCallableDeclaration)
                ?.typeParameters
                ?.map { typeParameter -> builder.classifierBuilder.buildTypeParameterSymbol(typeParameter.symbol) }
                .orEmpty()
        }

    override val valueParameters: List<CaValueParameterSymbol>
        get() = withValidityAssertion {
            createCaValueParameters() ?: (cfirSymbol.cfir as? CfirFunction)
                ?.valueParameters
                ?.map { valueParameter -> builder.variableBuilder.buildValueParameterSymbol(valueParameter.symbol) }
                .orEmpty()
        }

    override val containingClassId: ClassId?
        get() = (psi as? CjFinalizer)?.getContainingTypeStatement()?.getClassId()

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()
}

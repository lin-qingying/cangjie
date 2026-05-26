package org.cangnova.cangjie.analysis.api.cfir.symbols

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.caSymbolModalityByModifiers
import org.cangnova.cangjie.analysis.api.cfir.findPsi
import org.cangnova.cangjie.analysis.api.cfir.getCallableSymbolLocation
import org.cangnova.cangjie.analysis.api.cfir.getExplicitCallableReceiverType
import org.cangnova.cangjie.analysis.api.cfir.isOpenFromInterface
import org.cangnova.cangjie.analysis.api.cfir.location
import org.cangnova.cangjie.analysis.api.cfir.psiBasedDefaultCaModality
import org.cangnova.cangjie.analysis.api.cfir.psiBasedVisibility
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirMemberFunctionSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirTopLevelFunctionSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.createOwnerPointer
import org.cangnova.cangjie.analysis.api.cfir.visibilityByModifiers
import org.cangnova.cangjie.analysis.api.impl.base.util.callableId
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaMacroSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaMainFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.low.level.api.cfir.providers.CfirCallableSignature
import org.cangnova.cangjie.cfir.containingClassLookupTag
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.symbols.CfirMacroDeclarationSymbol
import org.cangnova.cangjie.cfir.symbols.CfirMainFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.lexer.CjModifierKeywordToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjMacroDeclaration
import org.cangnova.cangjie.psi.CjNamedFunction
import kotlin.toString

/**
 * 命名函数族叶子实现。
 *
 * 对齐 Kotlin FIR 中 `KaFirNamedFunctionSymbol`、入口函数及类似特殊命名函数的分文件落位，
 * 保持仓颉函数公开语义不变，只收敛 CFIR 后端组织方式。
 */
internal class CaCfirNamedFunctionSymbol private constructor(
    override val backingPsi: CjNamedFunction?,
    override val analysisSession: CaCfirSession,
    override val lazyCfirSymbol: Lazy<CfirNamedFunctionSymbol>,
) : CaNamedFunctionSymbol(),
    CaCfirCjBasedSymbol<CjNamedFunction, CfirNamedFunctionSymbol> {

    init {
        require(backingPsi?.isAnonymous != true)
    }

    constructor(declaration: CjNamedFunction, session: CaCfirSession) : this(
        backingPsi = declaration,
        lazyCfirSymbol = lazyCfirSymbol(declaration, session),
        analysisSession = session,
    )

    constructor(symbol: CfirNamedFunctionSymbol, session: CaCfirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? CjNamedFunction,
        lazyCfirSymbol = lazyOf(symbol),
        analysisSession = session,
    )

    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    private val status
        get() = (cfirSymbol.cfir as? CfirMemberDeclaration)?.status

    override val psi: PsiElement? get() = withValidityAssertion { backingPsiOrFindCurrentPsi { findPsi() } }
    override val name: Name get() = withValidityAssertion { backingPsi?.nameAsSafeName ?: cfirSymbol.name }
    override val origin get() = withValidityAssertion { psiOrSymbolOrigin() }

    override val annotations: CaAnnotationList
        get() = withValidityAssertion {
            psiOrSymbolAnnotationList()
        }
    override val typeParameters: List<CaTypeParameterSymbol>
        get() = withValidityAssertion {
            createCaTypeParameters() ?: cfirSymbol.createCjTypeParameters(builder)
        }
    override val valueParameters: List<CaValueParameterSymbol>
        get() = withValidityAssertion {
            createCaValueParameters() ?: cfirSymbol.createCjValueParameters(builder)
        }

    override val callableId: CallableId?
        get() = withValidityAssertion {
            if (backingPsi != null)
                backingPsi.callableId
            else
                cfirSymbol.getCallableId()
        }

    override val receiverType: CaType?
        get() = withValidityAssertion { analysisSession.getExplicitCallableReceiverType(backingPsi, builder) { cfirSymbol } }

    override val returnType: CaType
        get() = withValidityAssertion { createReturnType() }

    override val location: CaSymbolLocation
        get() = withValidityAssertion { analysisSession.getCallableSymbolLocation(backingPsi) { cfirSymbol } }

    override val visibility: CaSymbolVisibility
        get() = withValidityAssertion {
            backingPsi?.psiBasedVisibility(::isOverride)?.asPublicVisibility()
                ?: status?.visibility?.asPublicVisibility()
                ?: CaSymbolVisibility.PUBLIC
        }

    override val isVisibilityExplicit: Boolean
        get() = withValidityAssertion {
            backingPsi?.let { it.visibilityByModifiers != null }
                ?: (status?.isVisibilityExplicit == true)
        }

    override val modality: CaSymbolModality?
        get() = withValidityAssertion {
            val psiBasedModality = backingPsi?.run {
                val modalityByModifiers = caSymbolModalityByModifiers
                when {
                    modalityByModifiers != null -> when {
                        modalityByModifiers.isOpenFromInterface && !hasBody() -> CaSymbolModality.ABSTRACT
                        else -> modalityByModifiers
                    }

                    isTopLevel || isLocal -> CaSymbolModality.FINAL
                    hasModifier(CjTokens.CONST_KEYWORD) -> CaSymbolModality.FINAL
                    else -> psiBasedDefaultCaModality(::isOverride)
                }
            }

            psiBasedModality ?: status?.modality?.asPublicModality()
        }

    override val isModalityExplicit: Boolean
        get() = withValidityAssertion {
            backingPsi?.let { it.caSymbolModalityByModifiers != null }
                ?: (status?.isModalityExplicit == true)
        }

    @OptIn(CaImplementationDetail::class)
    override fun createPointer(): CaSymbolPointer<CaNamedFunctionSymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<CaNamedFunctionSymbol> { psi ->
            (psi as? CjNamedFunction)?.symbol
        }?.let { return it }

        when (val kind = location) {
            CaSymbolLocation.TOP_LEVEL -> CaCfirTopLevelFunctionSymbolPointer(
                cfirSymbol.callableId,
                CfirCallableSignature.createSignature(cfirSymbol),
                this,
            )

            CaSymbolLocation.CLASS -> createMemberFunctionPointer()

            CaSymbolLocation.LOCAL -> error("Local library named function cannot create stable pointer: ${callableId ?: name.asString()}")

            else -> error("Unsupported named function symbol location: $kind")
        }
    }
    override val isStatic: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.STATIC_KEYWORD) ?: (status?.isStatic == true) }

    override val isConst: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.CONST_KEYWORD) ?: (status?.isConst == true) }

    override val isMutating: Boolean
        get() = withValidityAssertion {
            psiHasModifierConsideringInheritance(CjTokens.MUT_KEYWORD) ?: (status?.isMut == true)
        }

    override val isOperator: Boolean
        get() = withValidityAssertion {
            psiHasModifierConsideringInheritance(CjTokens.OPERATOR_KEYWORD) ?: (status?.isOperator == true)
        }
    override val isUnsafe: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.UNSAFE_KEYWORD) ?: (status?.isUnsafe == true) }

    override val isForeign: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.FOREIGN_KEYWORD) ?: (status?.isForeign == true) }

    override val isOverride: Boolean
        get() = withValidityAssertion { isOverrideWithWorkaround }

    private fun psiHasModifierConsideringInheritance(modifierToken: CjModifierKeywordToken): Boolean? {
        if (backingPsi == null) return null

        val hasModifier = backingPsi.hasModifier(modifierToken)
        return when {
            // The modifier is explicitly declared, so it shouldn't be changed
            hasModifier -> true
            // The modifier is inherited, so it might be changed
            isOverride -> null
            // The modifier is not explicitly declared and not inherited, so it should be false
            else -> false
        }
    }

}

@OptIn(CaImplementationDetail::class)
private fun CaCfirNamedFunctionSymbol.createMemberFunctionPointer(): CaSymbolPointer<CaNamedFunctionSymbol> {
    return CaCfirMemberFunctionSymbolPointer(
        ownerPointer = analysisSession.createOwnerPointer<CaDeclarationContainerSymbol>(this),
        name = name,
        signature = CfirCallableSignature.createSignature(cfirSymbol),
    )
}

internal class CaCfirMainFunctionSymbol(
    final override val cfirSymbol: CfirMainFunctionSymbol,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : CaMainFunctionSymbol(), CaCfirSymbol<CfirMainFunctionSymbol> {
    private val status
        get() = (cfirSymbol.cfir as? CfirMemberDeclaration)?.status

    override val annotations: CaAnnotationList
        get() = withValidityAssertion {
            CaCfirAnnotationListForDeclaration.create(cfirSymbol, builder)
        }

    override val psi: PsiElement?
        get() = null

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = cfirSymbol.getCallableId()

    override val receiverType: CaType?
        get() = analysisSession.getExplicitCallableReceiverType(backingPsi = null, builder) { cfirSymbol }

    override val returnType: CaType
        get() = cfirSymbol.returnType(builder)

    override val location: CaSymbolLocation
        get() = analysisSession.getCallableSymbolLocation(backingPsi = null) { cfirSymbol }

    override val visibility: CaSymbolVisibility
        get() = status?.visibility?.asPublicVisibility() ?: CaSymbolVisibility.PUBLIC

    override val isVisibilityExplicit: Boolean
        get() = status?.isVisibilityExplicit == true

    override val modality: CaSymbolModality?
        get() = status?.modality?.asPublicModality()

    override val isModalityExplicit: Boolean
        get() = status?.isModalityExplicit == true

    override fun createPointer(): CaSymbolPointer<CaFunctionSymbol> = withValidityAssertion {
        error("Main function symbol cannot create a stable pointer")
    }

    override val isStatic: Boolean
        get() = status?.isStatic == true

    override val isConst: Boolean
        get() = status?.isConst == true

    override val isMutating: Boolean
        get() = status?.isMut == true

    override val isOverride: Boolean
        get() = status?.isOverride == true

    override val isOperator: Boolean
        get() = status?.isOperator == true

    override val isUnsafe: Boolean
        get() = status?.isUnsafe == true

    override val isForeign: Boolean
        get() = status?.isForeign == true

    override val typeParameters: List<CaTypeParameterSymbol>
        get() = (cfirSymbol.cfir as? CfirCallableDeclaration)
            ?.typeParameters
            ?.map { typeParameter -> builder.classifierBuilder.buildTypeParameterSymbol(typeParameter.symbol) }
            .orEmpty()

    override val valueParameters: List<CaValueParameterSymbol>
        get() = (cfirSymbol.cfir as? CfirFunction)
            ?.valueParameters
            ?.map { valueParameter -> builder.variableBuilder.buildValueParameterSymbol(valueParameter.symbol) }
            .orEmpty()

    override val name: Name
        get() = cfirSymbol.name
}

internal class CaCfirMacroSymbol private constructor(
    override val backingPsi: CjMacroDeclaration?,
    override val analysisSession: CaCfirSession,
    override val lazyCfirSymbol: Lazy<CfirMacroDeclarationSymbol>,
) : CaMacroSymbol(),
    CaCfirCjBasedSymbol<CjMacroDeclaration, CfirMacroDeclarationSymbol> {
    constructor(declaration: CjMacroDeclaration, session: CaCfirSession) : this(
        backingPsi = declaration,
        analysisSession = session,
        lazyCfirSymbol = lazyCfirSymbol(declaration, session),
    )

    constructor(symbol: CfirMacroDeclarationSymbol, session: CaCfirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? CjMacroDeclaration,
        analysisSession = session,
        lazyCfirSymbol = lazyOf(symbol),
    )

    override val cfirSymbol: CfirMacroDeclarationSymbol
        get() = super<CaCfirCjBasedSymbol>.cfirSymbol

    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    private val status
        get() = (cfirSymbol.cfir as? CfirMemberDeclaration)?.status

    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsiOrFindCurrentPsi { findPsi() } }

    override val origin
        get() = withValidityAssertion { psiOrSymbolOrigin() }

    override val annotations: CaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = withValidityAssertion {
            cfirSymbol.getCallableId()
        }

    override val receiverType: CaType?
        get() = withValidityAssertion { analysisSession.getExplicitCallableReceiverType(backingPsi, builder) { cfirSymbol } }

    override val returnType: CaType
        get() = withValidityAssertion { createReturnType() }

    override val location: CaSymbolLocation
        get() = withValidityAssertion { analysisSession.getCallableSymbolLocation(backingPsi) { cfirSymbol } }

    override val visibility: CaSymbolVisibility
        get() = withValidityAssertion {
            backingPsi?.psiBasedVisibility(::isOverride)?.asPublicVisibility()
                ?: status?.visibility?.asPublicVisibility()
                ?: CaSymbolVisibility.PUBLIC
        }

    override val isVisibilityExplicit: Boolean
        get() = withValidityAssertion {
            backingPsi?.let { it.visibilityByModifiers != null }
                ?: (status?.isVisibilityExplicit == true)
        }

    override val modality: CaSymbolModality?
        get() = withValidityAssertion {
            val psiBasedModality = backingPsi?.run {
                caSymbolModalityByModifiers ?: psiBasedDefaultCaModality(::isOverride)
            }
            psiBasedModality ?: status?.modality?.asPublicModality()
        }

    override val isModalityExplicit: Boolean
        get() = withValidityAssertion {
            backingPsi?.let { it.caSymbolModalityByModifiers != null }
                ?: (status?.isModalityExplicit == true)
        }

    override fun createPointer(): CaSymbolPointer<CaFunctionSymbol> = withValidityAssertion {
        error("Macro symbol cannot create a stable pointer")
    }

    override val isStatic: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.STATIC_KEYWORD) ?: (status?.isStatic == true) }

    override val isConst: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.CONST_KEYWORD) ?: (status?.isConst == true) }

    override val isMutating: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.MUT_KEYWORD) ?: (status?.isMut == true) }

    override val isOverride: Boolean
        get() = withValidityAssertion { isOverrideWithWorkaround }

    override val isOperator: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.OPERATOR_KEYWORD) ?: (status?.isOperator == true) }

    override val isUnsafe: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.UNSAFE_KEYWORD) ?: (status?.isUnsafe == true) }

    override val isForeign: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.FOREIGN_KEYWORD) ?: (status?.isForeign == true) }

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

    override val name: Name
        get() = withValidityAssertion { backingPsi?.nameAsSafeName ?: cfirSymbol.name }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()
}

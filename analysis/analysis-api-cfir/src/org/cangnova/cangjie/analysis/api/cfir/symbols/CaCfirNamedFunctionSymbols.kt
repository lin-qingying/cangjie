package org.cangnova.cangjie.analysis.api.cfir.symbols

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.components.asCaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.components.renderAnnotations
import org.cangnova.cangjie.analysis.api.cfir.findPsi
import org.cangnova.cangjie.analysis.api.cfir.location
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirMemberFunctionSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirTopLevelFunctionSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.createOwnerPointer
import org.cangnova.cangjie.analysis.api.impl.base.util.callableId
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaMacroSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaMainFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
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
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.util.isOperator
import org.cangnova.cangjie.cfir.symbols.CfirMacroDeclarationSymbol
import org.cangnova.cangjie.cfir.symbols.CfirMainFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.lexer.CjModifierKeywordToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name
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
    CaCfirCjBasedSymbol<CjNamedFunction, CfirNamedFunctionSymbol>,
    CaCfirNamedFunctionSymbolSupport<CfirNamedFunctionSymbol> {
    init {
        require(backingPsi?.isAnonymous != true)
    }

    constructor(declaration: CjNamedFunction, session: CaCfirSession) : this(
        backingPsi = declaration,
        lazyCfirSymbol = lazyNamedFunctionSymbol(declaration, session),
        analysisSession = session,
    )

    constructor(symbol: CfirNamedFunctionSymbol, session: CaCfirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? CjNamedFunction,
        lazyCfirSymbol = lazyOf(symbol),
        analysisSession = session,
    )

    override val backingSymbol: CfirNamedFunctionSymbol
        get() = cfirSymbol

    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    override val psi: PsiElement? get() = withValidityAssertion { backingPsi ?: findPsi() }
    override val name: Name get() = withValidityAssertion { backingPsi?.nameAsSafeName ?: cfirSymbol.name }
    override val origin get() = withValidityAssertion { psiOrSymbolOrigin() }

    override val annotations: CaAnnotationList
        get() = withValidityAssertion {
            analysisSession.renderAnnotations(this).asCaAnnotationList(token)
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
        get() = withValidityAssertion { receiverTypeImpl }

    override val returnType: CaType
        get() = withValidityAssertion { createReturnType() }

    override val location: CaSymbolLocation
        get() = withValidityAssertion {
            when {
                backingPsi != null -> backingPsi.location
//                cfirSymbol.origin == CfirDeclarationOrigin.DynamicScope -> CaSymbolLocation.CLASS
                cfirSymbol.rawStatus.visibility == Visibilities.Local -> CaSymbolLocation.LOCAL
                cfirSymbol.containingClassLookupTag()?.classId == null -> CaSymbolLocation.TOP_LEVEL
                else -> CaSymbolLocation.CLASS
            }
        }

    override val containingDeclaration: CaSymbol?
        get() = withValidityAssertion { analysisSession.findContainingDeclarationSymbol(psi) }

    override val visibility: CaSymbolVisibility
        get() = withValidityAssertion { status?.visibility?.asPublicVisibility() ?: CaSymbolVisibility.PUBLIC }

    override val isVisibilityExplicit: Boolean
        get() = withValidityAssertion { status?.isVisibilityExplicit == true }

    override val modality: CaSymbolModality?
        get() = withValidityAssertion { status?.modality?.asPublicModality() }

    override val isModalityExplicit: Boolean
        get() = withValidityAssertion { status?.isModalityExplicit == true }

    @OptIn(CaImplementationDetail::class)
    override fun createPointer(): CaSymbolPointer<CaNamedFunctionSymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<CaNamedFunctionSymbol> { psi ->
            analysisSession.getPublicSymbolByPsi(psi)
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
        get() = withValidityAssertion { isStaticImpl }

    override val isConst: Boolean
        get() = withValidityAssertion { isConstImpl }

    override val isMutating: Boolean
        get() = withValidityAssertion {
            psiHasModifierConsideringInheritance(CjTokens.MUT_KEYWORD) ?: cfirSymbol.isOperator
        }

    override val isOperator: Boolean
        get() = withValidityAssertion {
            psiHasModifierConsideringInheritance(CjTokens.OPERATOR_KEYWORD) ?: cfirSymbol.isOperator
        }
    override val isUnsafe: Boolean
        get() = withValidityAssertion { isUnsafeImpl }

    override val isForeign: Boolean
        get() = withValidityAssertion { isForeignImpl }

    override val isOverride: Boolean
        get() = withValidityAssertion { isOverrideImpl }

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
    final override val backingSymbol: CfirMainFunctionSymbol,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : CaMainFunctionSymbol(), CaCfirNamedFunctionSymbolSupport<CfirMainFunctionSymbol> {
    override val annotations: CaAnnotationList
        get() = withValidityAssertion {
            analysisSession.renderAnnotations(this).asCaAnnotationList(token)
        }

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = callableIdImpl

    override val receiverType: CaType?
        get() = receiverTypeImpl

    override val returnType: CaType
        get() = returnTypeImpl

    override val location: CaSymbolLocation
        get() = locationImpl

    override fun createPointer(): CaSymbolPointer<CaFunctionSymbol> = withValidityAssertion {
        createStableCallablePointer(CaFunctionSymbol::class.java)
    }

    override val isStatic: Boolean
        get() = isStaticImpl

    override val isConst: Boolean
        get() = isConstImpl

    override val isMutating: Boolean
        get() = isMutatingImpl

    override val isOverride: Boolean
        get() = isOverrideImpl

    override val isOperator: Boolean
        get() = isOperatorImpl

    override val isUnsafe: Boolean
        get() = isUnsafeImpl

    override val isForeign: Boolean
        get() = isForeignImpl

    override val typeParameters: List<CaTypeParameterSymbol>
        get() = typeParametersImpl

    override val valueParameters: List<CaValueParameterSymbol>
        get() = valueParametersImpl

    override val name: Name
        get() = nameImpl
}

internal class CaCfirMacroSymbol(
    final override val backingSymbol: CfirMacroDeclarationSymbol,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : CaMacroSymbol(), CaCfirNamedFunctionSymbolSupport<CfirMacroDeclarationSymbol> {
    override val annotations: CaAnnotationList
        get() = withValidityAssertion {
            analysisSession.renderAnnotations(this).asCaAnnotationList(token)
        }

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = callableIdImpl

    override val receiverType: CaType?
        get() = receiverTypeImpl

    override val returnType: CaType
        get() = returnTypeImpl

    override val location: CaSymbolLocation
        get() = locationImpl

    override fun createPointer(): CaSymbolPointer<CaFunctionSymbol> = withValidityAssertion {
        createStableCallablePointer(CaFunctionSymbol::class.java)
    }

    override val isStatic: Boolean
        get() = isStaticImpl

    override val isConst: Boolean
        get() = isConstImpl

    override val isMutating: Boolean
        get() = isMutatingImpl

    override val isOverride: Boolean
        get() = isOverrideImpl

    override val isOperator: Boolean
        get() = isOperatorImpl

    override val isUnsafe: Boolean
        get() = isUnsafeImpl

    override val isForeign: Boolean
        get() = isForeignImpl

    override val typeParameters: List<CaTypeParameterSymbol>
        get() = typeParametersImpl

    override val valueParameters: List<CaValueParameterSymbol>
        get() = valueParametersImpl

    override val name: Name
        get() = nameImpl
}

/**
 * 命名函数 PSI 到 CFIR 函数符号的懒恢复入口。
 *
 * 这里保持和 Kotlin `lazyFirSymbol(declaration, session)` 同一职责：
 * 只负责把 declaration 懒绑定到同类后端符号，不在这里再发明额外恢复协议。
 */
private fun lazyNamedFunctionSymbol(
    declaration: CjNamedFunction,
    session: CaCfirSession,
): Lazy<CfirNamedFunctionSymbol> = lazy(LazyThreadSafetyMode.NONE) {
    session.symbolQueries.lookupSymbolsByPsi(declaration)
        .filterIsInstance<CfirNamedFunctionSymbol>()
        .singleOrNull()
        ?: error("Cannot resolve CFIR named-function symbol for `${declaration.fqName ?: declaration.nameAsSafeName}`")
}

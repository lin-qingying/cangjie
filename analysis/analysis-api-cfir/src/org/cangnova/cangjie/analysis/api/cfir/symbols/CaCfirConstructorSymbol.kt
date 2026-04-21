package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.cfir.*

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.findPsi
import org.cangnova.cangjie.analysis.api.cfir.components.asCaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.components.renderAnnotations
import org.cangnova.cangjie.analysis.api.cfir.utils.asCaType
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjConstructor
import org.cangnova.cangjie.psi.CjPrimaryConstructor
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType

/**
 * 构造器叶子实现。
 *
 * 构造器的 `modality` 与普通函数不同，在公开 Analysis API 中是固定语义，
 * 因此单独落位，避免错误复用通用函数 support。
 */
internal class CaCfirConstructorSymbol private constructor(
    override val backingPsi: CjConstructor<*>?,
    override val analysisSession: CaCfirSession,
    override val lazyCfirSymbol: Lazy<CfirConstructorSymbol>,
) : CaConstructorSymbol(),
    CaCfirCjBasedSymbol<CjConstructor<*>, CfirConstructorSymbol>,
    CaCfirBackedSymbol<CfirConstructorSymbol> {
    constructor(declaration: CjConstructor<*>, session: CaCfirSession) : this(
        backingPsi = declaration,
        analysisSession = session,
        lazyCfirSymbol = lazyCfirSymbol(declaration, session),
    )

    constructor(symbol: CfirConstructorSymbol, session: CaCfirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? CjConstructor<*>,
        analysisSession = session,
        lazyCfirSymbol = lazyOf(symbol),
    )

    override val backingSymbol: CfirConstructorSymbol
        get() = cfirSymbol

    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    private val status
        get() = (cfirSymbol.cfir as? CfirMemberDeclaration)?.status

    override val annotations: CaAnnotationList
        get() = withValidityAssertion { analysisSession.renderAnnotations(this).asCaAnnotationList(token) }

    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsi ?: findPsi() }

    override val origin
        get() = withValidityAssertion { psiOrSymbolOrigin() }

    override val containingDeclaration
        get() = withValidityAssertion { analysisSession.findContainingDeclarationSymbol(psi) }

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = withValidityAssertion {
            val callableDeclaration = cfirSymbol.cfir as? CfirCallableDeclaration
            cfirSymbol.callableId.takeUnless { callableDeclaration?.isLocal == true }
        }

    override val visibility: CaSymbolVisibility
        get() = withValidityAssertion { status?.visibility?.asPublicVisibility() ?: CaSymbolVisibility.PUBLIC }

    override val isVisibilityExplicit: Boolean
        get() = withValidityAssertion { status?.isVisibilityExplicit == true }

    override val isModalityExplicit: Boolean
        get() = withValidityAssertion { false }

    override val receiverType: CaType?
        get() = withValidityAssertion {
            val callablePsi = psi as? org.cangnova.cangjie.psi.CjCallableDeclaration ?: return@withValidityAssertion null
            if (callablePsi.getStrictParentOfType<org.cangnova.cangjie.psi.CjExtend>() == null) return@withValidityAssertion null
            (cfirSymbol.cfir as? CfirCallableDeclaration)?.dispatchReceiverType?.asCaType(analysisSession)
        }

    override val returnType: CaType
        get() = withValidityAssertion {
            analysisSession.typeQueries.queryCallableReturnType(cfirSymbol)?.asCaType(analysisSession)
                ?: error("Cannot build return type for `${cfirSymbol::class.simpleName}`")
        }

    override val location: CaSymbolLocation
        get() = withValidityAssertion { analysisSession.locationForDeclaration(this) }

    override fun createPointer(): CaSymbolPointer<CaConstructorSymbol> = withValidityAssertion {
        createStableCallablePointer(CaConstructorSymbol::class.java)
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
            (cfirSymbol.cfir as? CfirCallableDeclaration)
                ?.typeParameters
                ?.filterIsInstance<CfirTypeParameter>()
                ?.map { typeParameter -> analysisSession.createTypeParameterSymbol(typeParameter.symbol) }
                .orEmpty()
        }

    override val valueParameters: List<CaValueParameterSymbol>
        get() = withValidityAssertion {
            (cfirSymbol.cfir as? CfirFunction)
                ?.valueParameters
                ?.mapIndexed { index, parameter ->
                    analysisSession.createValueParameterSymbol(
                        ownerSymbol = this,
                        parameter = parameter,
                        parameterIndex = index,
                    )
                }
                .orEmpty()
        }

    override val isPrimary: Boolean
        get() = withValidityAssertion { backingPsi is CjPrimaryConstructor || cfirSymbol.cfir.isPrimary }

    override val containingClassId: ClassId?
        get() = withValidityAssertion { backingPsi?.getContainingTypeStatement()?.getClassId() }
}

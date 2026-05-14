package org.cangnova.cangjie.analysis.api.cfir.symbols

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.findPsi
import org.cangnova.cangjie.analysis.api.cfir.getExplicitCallableReceiverType
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
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjConstructor
import org.cangnova.cangjie.psi.CjPrimaryConstructor
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType

/**
 * 构造器叶子实现。
 *
 * 对齐 Kotlin `KaFirConstructorSymbol` 的独立叶子落位：
 * 构造器不复用普通命名函数叶子，而是直接在类内表达自身的参数、返回类型、
 * 所属类与 pointer 语义。
 */
internal class CaCfirConstructorSymbol private constructor(
    override val backingPsi: CjConstructor<*>?,
    override val analysisSession: CaCfirSession,
    override val lazyCfirSymbol: Lazy<CfirConstructorSymbol>,
) : CaConstructorSymbol(),
    CaCfirCjBasedSymbol<CjConstructor<*>, CfirConstructorSymbol> {
    override val cfirSymbol: CfirConstructorSymbol
        get() = super<CaCfirCjBasedSymbol>.cfirSymbol

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

    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    private val status
        get() = (cfirSymbol.cfir as? CfirMemberDeclaration)?.status

    override val annotations: CaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsi ?: findPsi() }

    override val origin
        get() = withValidityAssertion { psiOrSymbolOrigin() }

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
        get() = withValidityAssertion { analysisSession.getExplicitCallableReceiverType(cfirSymbol, backingPsi, builder) }

    override val returnType: CaType
        get() = withValidityAssertion { cfirSymbol.returnType(builder) }

    override val location: CaSymbolLocation
        get() = withValidityAssertion { CaSymbolLocation.CLASS }

    override fun createPointer(): CaSymbolPointer<CaConstructorSymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<CaConstructorSymbol> { psi ->
            (psi as? CjConstructor<*>)?.symbol
        } ?: error("Constructor symbol cannot create a stable pointer")
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
            with(analysisSession) {
                backingPsi?.getContainingTypeStatement()?.classSymbol?.typeParameters ?: cfirSymbol.createCjTypeParameters(builder)
            }
        }

    override val valueParameters: List<CaValueParameterSymbol>
        get() = withValidityAssertion {
            createCaValueParameters() ?: cfirSymbol.createCjValueParameters(builder)
        }

    override val isPrimary: Boolean
        get() = withValidityAssertion { backingPsi is CjPrimaryConstructor || cfirSymbol.cfir.isPrimary }

    override val containingClassId: ClassId?
        get() = withValidityAssertion {
            backingPsi?.getContainingTypeStatement()?.getClassId()
                ?: cfirSymbol.callableId.classId
        }
}

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
    /**
     * 构造器对应的源码 PSI。
     */
    override val backingPsi: CjConstructor<*>?,
    /**
     * 当前符号绑定的 CFIR Analysis session。
     */
    override val analysisSession: CaCfirSession,
    /**
     * 延迟取得的底层 CFIR 构造器符号。
     */
    override val lazyCfirSymbol: Lazy<CfirConstructorSymbol>,
) : CaConstructorSymbol(),
    CaCfirCjBasedSymbol<CjConstructor<*>, CfirConstructorSymbol> {
    /**
     * 构造器底层 CFIR 符号。
     */
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

    /**
     * 构造器所在的 use-site 模块。
     */
    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    /**
     * 构造器 CFIR member 状态。
     */
    private val status
        get() = (cfirSymbol.cfir as? CfirMemberDeclaration)?.status

    /**
     * 构造器公开注解列表。
     */
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    /**
     * 构造器对应的 PSI。
     */
    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsiOrFindCurrentPsi { findPsi() } }

    /**
     * 构造器公开来源。
     */
    override val origin
        get() = withValidityAssertion { psiOrSymbolOrigin() }

    /**
     * 构造器 callableId。
     */
    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = withValidityAssertion {
            val callableDeclaration = cfirSymbol.cfir as? CfirCallableDeclaration
            cfirSymbol.callableId.takeUnless { callableDeclaration?.isLocal == true }
        }

    /**
     * 构造器可见性。
     */
    override val visibility: CaSymbolVisibility
        get() = withValidityAssertion { status?.visibility?.asPublicVisibility() ?: CaSymbolVisibility.PUBLIC }

    /**
     * 构造器可见性是否显式声明。
     */
    override val isVisibilityExplicit: Boolean
        get() = withValidityAssertion { status?.isVisibilityExplicit == true }

    /**
     * 构造器不显式暴露 modality。
     */
    override val isModalityExplicit: Boolean
        get() = withValidityAssertion { false }

    /**
     * 构造器显式 receiver 类型。
     */
    override val receiverType: CaType?
        get() = withValidityAssertion { analysisSession.getExplicitCallableReceiverType(backingPsi, builder) { cfirSymbol } }

    /**
     * 构造器返回类型。
     */
    override val returnType: CaType
        get() = withValidityAssertion { cfirSymbol.returnType(builder) }

    /**
     * 构造器公开符号位置固定为 class。
     */
    override val location: CaSymbolLocation
        get() = withValidityAssertion { CaSymbolLocation.CLASS }

    /**
     * 创建构造器符号 pointer。
     */
    override fun createPointer(): CaSymbolPointer<CaConstructorSymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<CaConstructorSymbol> { psi ->
            (psi as? CjConstructor<*>)?.symbol
        } ?: error("Constructor symbol cannot create a stable pointer")
    }

    /**
     * 构造器是否为 static。
     */
    override val isStatic: Boolean
        get() = withValidityAssertion { status?.isStatic == true }

    /**
     * 构造器是否为 const。
     */
    override val isConst: Boolean
        get() = withValidityAssertion { status?.isConst == true }

    /**
     * 构造器是否为 mutating。
     */
    override val isMutating: Boolean
        get() = withValidityAssertion { status?.isMut == true }

    /**
     * 构造器是否为 override。
     */
    override val isOverride: Boolean
        get() = withValidityAssertion { status?.isOverride == true }

    /**
     * 构造器是否为 operator。
     */
    override val isOperator: Boolean
        get() = withValidityAssertion { status?.isOperator == true }

    /**
     * 构造器是否为 unsafe。
     */
    override val isUnsafe: Boolean
        get() = withValidityAssertion { status?.isUnsafe == true }

    /**
     * 构造器是否为 foreign。
     */
    override val isForeign: Boolean
        get() = withValidityAssertion { status?.isForeign == true }

    /**
     * 构造器所属类型的类型参数列表。
     */
    override val typeParameters: List<CaTypeParameterSymbol>
        get() = withValidityAssertion {
            with(analysisSession) {
                backingPsi?.getContainingTypeStatement()?.classSymbol?.typeParameters ?: cfirSymbol.createCjTypeParameters(builder)
            }
        }

    /**
     * 构造器值参数列表。
     */
    override val valueParameters: List<CaValueParameterSymbol>
        get() = withValidityAssertion {
            createCaValueParameters() ?: cfirSymbol.createCjValueParameters(builder)
        }

    /**
     * 构造器是否为主构造器。
     */
    override val isPrimary: Boolean
        get() = withValidityAssertion { backingPsi is CjPrimaryConstructor || cfirSymbol.cfir.isPrimary }

    /**
     * 构造器所属类的 classId。
     */
    override val containingClassId: ClassId?
        get() = withValidityAssertion {
            backingPsi?.getContainingTypeStatement()?.getClassId()
                ?: cfirSymbol.callableId.classId
        }
}

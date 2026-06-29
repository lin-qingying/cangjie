package org.cangnova.cangjie.analysis.api.cfir.symbols

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.findPsi
import org.cangnova.cangjie.analysis.api.cfir.getAllowedPsi
import org.cangnova.cangjie.analysis.api.cfir.getExplicitCallableReceiverType
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
    /**
     * 匿名函数对应的源码 PSI。
     */
    override val backingPsi: CjFunction?,
    /**
     * 当前符号绑定的 CFIR Analysis session。
     */
    override val analysisSession: CaCfirSession,
    /**
     * 延迟取得的底层 CFIR 匿名函数符号。
     */
    override val lazyCfirSymbol: Lazy<CfirAnonymousFunctionSymbol>,
) : CaAnonymousFunctionSymbol(),
    CaCfirCjBasedSymbol<CjFunction, CfirAnonymousFunctionSymbol> {
    constructor(declaration: CjFunctionLiteral, session: CaCfirSession) : this(
        backingPsi = declaration,
        analysisSession = session,
        lazyCfirSymbol = lazyCfirSymbol(declaration, session),
    )

    constructor(symbol: CfirAnonymousFunctionSymbol, session: CaCfirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? CjFunction,
        analysisSession = session,
        lazyCfirSymbol = lazyOf(symbol),
    )

    /**
     * 匿名函数底层 CFIR 符号。
     */
    override val cfirSymbol: CfirAnonymousFunctionSymbol
        get() = super<CaCfirCjBasedSymbol>.cfirSymbol

    /**
     * 匿名函数所在的 use-site 模块。
     */
    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    /**
     * 匿名函数 CFIR member 状态。
     */
    private val status
        get() = (cfirSymbol.cfir as? CfirMemberDeclaration)?.status

    /**
     * 匿名函数对应的 PSI。
     */
    override val psi: PsiElement?
        get() = withValidityAssertion {
            backingPsiOrFindCurrentPsi { cfirSymbol.cfir.getAllowedPsi(analysisSession.project) ?: findPsi() }
        }

    /**
     * 匿名函数公开来源。
     */
    override val origin
        get() = withValidityAssertion { psiOrSymbolOrigin() }

    /**
     * 匿名函数公开注解列表。
     */
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    /**
     * 匿名函数显式 receiver 类型。
     */
    override val receiverType: CaType?
        get() = withValidityAssertion { analysisSession.getExplicitCallableReceiverType(backingPsi, builder) { cfirSymbol } }

    /**
     * 匿名函数返回类型。
     */
    override val returnType: CaType
        get() = withValidityAssertion { createReturnType() }

    /**
     * 匿名函数公开符号位置。
     */
    override val location: CaSymbolLocation
        get() = CaSymbolLocation.LOCAL

    /**
     * 创建匿名函数符号 pointer。
     */
    override fun createPointer(): CaSymbolPointer<CaFunctionSymbol> = withValidityAssertion {
        val sourcePsi = psi ?: error("Anonymous function symbol is missing PSI")
        @Suppress("UNCHECKED_CAST")
        CaCfirAnonymousFunctionSymbolPointer(sourcePsi) as CaSymbolPointer<CaFunctionSymbol>
    }

    /**
     * 匿名函数是否为 static。
     */
    override val isStatic: Boolean
        get() = withValidityAssertion { status?.isStatic == true }

    /**
     * 匿名函数是否为 const。
     */
    override val isConst: Boolean
        get() = withValidityAssertion { status?.isConst == true }

    /**
     * 匿名函数是否为 mutating。
     */
    override val isMutating: Boolean
        get() = withValidityAssertion { status?.isMut == true }

    /**
     * 匿名函数是否为 override。
     */
    override val isOverride: Boolean
        get() = withValidityAssertion { status?.isOverride == true }

    /**
     * 匿名函数是否为 operator。
     */
    override val isOperator: Boolean
        get() = withValidityAssertion { status?.isOperator == true }

    /**
     * 匿名函数是否为 unsafe。
     */
    override val isUnsafe: Boolean
        get() = withValidityAssertion { status?.isUnsafe == true }

    /**
     * 匿名函数是否为 foreign。
     */
    override val isForeign: Boolean
        get() = withValidityAssertion { status?.isForeign == true }

    /**
     * 匿名函数类型参数列表。
     */
    override val typeParameters: List<CaTypeParameterSymbol>
        get() = withValidityAssertion {
            createCaTypeParameters() ?: (cfirSymbol.cfir as? CfirCallableDeclaration)
                ?.typeParameters
                ?.map { typeParameter -> builder.classifierBuilder.buildTypeParameterSymbol(typeParameter.symbol) }
                .orEmpty()
        }

    /**
     * 匿名函数值参数列表。
     */
    override val valueParameters: List<CaValueParameterSymbol>
        get() = withValidityAssertion {
            createCaValueParameters() ?: (cfirSymbol.cfir as? CfirFunction)
                ?.valueParameters
                ?.map { valueParameter -> builder.variableBuilder.buildValueParameterSymbol(valueParameter.symbol) }
                .orEmpty()
        }

    /**
     * 匿名函数没有稳定 callableId。
     */
    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = null

    /**
     * 匿名函数可见性固定为局部。
     */
    override val visibility: CaSymbolVisibility
        get() = CaSymbolVisibility.LOCAL

    /**
     * 匿名函数不显式声明可见性。
     */
    override val isVisibilityExplicit: Boolean
        get() = false

    /**
     * 匿名函数 modality 固定为 final。
     */
    override val modality: CaSymbolModality?
        get() = CaSymbolModality.FINAL

    /**
     * 匿名函数不显式声明 modality。
     */
    override val isModalityExplicit: Boolean
        get() = false

    /**
     * 按 PSI 或 CFIR 符号身份比较匿名函数。
     */
    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    /**
     * 按 PSI 或 CFIR 符号身份计算匿名函数 hash。
     */
    override fun hashCode(): Int = psiOrSymbolHashCode()
}

/**
 * CFIR finalizer 符号实现。
 */
internal class CaCfirFinalizerSymbol private constructor(
    /**
     * finalizer 对应的源码 PSI。
     */
    override val backingPsi: CjFinalizer?,
    /**
     * 当前符号绑定的 CFIR Analysis session。
     */
    override val analysisSession: CaCfirSession,
    /**
     * 延迟取得的底层 CFIR finalizer 符号。
     */
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

    /**
     * finalizer 底层 CFIR 符号。
     */
    override val cfirSymbol: CfirFinalizerSymbol
        get() = super<CaCfirCjBasedSymbol>.cfirSymbol

    /**
     * finalizer 所在的 use-site 模块。
     */
    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    /**
     * finalizer CFIR member 状态。
     */
    private val status
        get() = (cfirSymbol.cfir as? CfirMemberDeclaration)?.status

    /**
     * finalizer 对应的 PSI。
     */
    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsiOrFindCurrentPsi { findPsi() } }

    /**
     * finalizer 公开来源。
     */
    override val origin
        get() = withValidityAssertion { psiOrSymbolOrigin() }

    /**
     * finalizer 公开注解列表。
     */
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    /**
     * finalizer callableId。
     */
    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = withValidityAssertion {
            val callableDeclaration = cfirSymbol.cfir as? CfirCallableDeclaration
            cfirSymbol.callableId.takeUnless { callableDeclaration?.isLocal == true }
        }

    /**
     * finalizer 显式 receiver 类型。
     */
    override val receiverType: CaType?
        get() = withValidityAssertion { analysisSession.getExplicitCallableReceiverType(backingPsi, builder) { cfirSymbol } }

    /**
     * finalizer 返回类型。
     */
    override val returnType: CaType
        get() = withValidityAssertion { createReturnType() }

    /**
     * finalizer 公开符号位置。
     */
    override val location: CaSymbolLocation
        get() = withValidityAssertion { backingPsi?.location ?: CaSymbolLocation.CLASS }

    /**
     * finalizer 可见性。
     */
    override val visibility: CaSymbolVisibility
        get() = withValidityAssertion { status?.visibility?.asPublicVisibility() ?: CaSymbolVisibility.PUBLIC }

    /**
     * finalizer 可见性是否显式声明。
     */
    override val isVisibilityExplicit: Boolean
        get() = withValidityAssertion { status?.isVisibilityExplicit == true }

    /**
     * finalizer modality。
     */
    override val modality: CaSymbolModality?
        get() = withValidityAssertion { status?.modality?.asPublicModality() }

    /**
     * finalizer modality 是否显式声明。
     */
    override val isModalityExplicit: Boolean
        get() = withValidityAssertion { status?.isModalityExplicit == true }

    /**
     * finalizer 当前没有稳定 pointer。
     */
    override fun createPointer(): CaSymbolPointer<CaFunctionSymbol> = withValidityAssertion {
        error("Finalizer symbol cannot create a stable pointer")
    }

    /**
     * finalizer 是否为 static。
     */
    override val isStatic: Boolean
        get() = withValidityAssertion { status?.isStatic == true }

    /**
     * finalizer 是否为 const。
     */
    override val isConst: Boolean
        get() = withValidityAssertion { status?.isConst == true }

    /**
     * finalizer 是否为 mutating。
     */
    override val isMutating: Boolean
        get() = withValidityAssertion { status?.isMut == true }

    /**
     * finalizer 是否为 override。
     */
    override val isOverride: Boolean
        get() = withValidityAssertion { status?.isOverride == true }

    /**
     * finalizer 是否为 operator。
     */
    override val isOperator: Boolean
        get() = withValidityAssertion { status?.isOperator == true }

    /**
     * finalizer 是否为 unsafe。
     */
    override val isUnsafe: Boolean
        get() = withValidityAssertion { status?.isUnsafe == true }

    /**
     * finalizer 是否为 foreign。
     */
    override val isForeign: Boolean
        get() = withValidityAssertion { status?.isForeign == true }

    /**
     * finalizer 类型参数列表。
     */
    override val typeParameters: List<CaTypeParameterSymbol>
        get() = withValidityAssertion {
            createCaTypeParameters() ?: (cfirSymbol.cfir as? CfirCallableDeclaration)
                ?.typeParameters
                ?.map { typeParameter -> builder.classifierBuilder.buildTypeParameterSymbol(typeParameter.symbol) }
                .orEmpty()
        }

    /**
     * finalizer 值参数列表。
     */
    override val valueParameters: List<CaValueParameterSymbol>
        get() = withValidityAssertion {
            createCaValueParameters() ?: (cfirSymbol.cfir as? CfirFunction)
                ?.valueParameters
                ?.map { valueParameter -> builder.variableBuilder.buildValueParameterSymbol(valueParameter.symbol) }
                .orEmpty()
        }

    /**
     * finalizer 所属类型的 classId。
     */
    override val containingClassId: ClassId?
        get() = (psi as? CjFinalizer)?.getContainingTypeStatement()?.getClassId()

    /**
     * 按 PSI 或 CFIR 符号身份比较 finalizer。
     */
    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    /**
     * 按 PSI 或 CFIR 符号身份计算 finalizer hash。
     */
    override fun hashCode(): Int = psiOrSymbolHashCode()
}

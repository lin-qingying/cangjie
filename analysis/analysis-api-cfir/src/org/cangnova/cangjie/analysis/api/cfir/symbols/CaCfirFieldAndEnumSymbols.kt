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
    /**
     * 字段变量对应的源码 PSI。
     */
    override val backingPsi: CjFieldVariable?,
    /**
     * 当前符号绑定的 CFIR Analysis session。
     */
    override val analysisSession: CaCfirSession,
    /**
     * 延迟取得的底层 CFIR 字段符号。
     */
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

    /**
     * 字段底层 CFIR 符号。
     */
    override val cfirSymbol: CfirFieldVariableSymbol
        get() = super<CaCfirCjBasedSymbol>.cfirSymbol

    /**
     * 字段所在的 use-site 模块。
     */
    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    /**
     * 字段 CFIR member 状态。
     */
    private val status
        get() = (cfirSymbol.cfir as? CfirMemberDeclaration)?.status

    /**
     * 字段对应的 PSI。
     */
    override val psi
        get() = withValidityAssertion { backingPsiOrFindCurrentPsi { findPsi() } }

    /**
     * 字段公开来源。
     */
    override val origin
        get() = withValidityAssertion { psiOrSymbolOrigin() }

    /**
     * 字段公开注解列表。
     */
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    /**
     * 字段 callableId。
     */
    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = cfirSymbol.getCallableId()

    /**
     * 字段显式 receiver 类型。
     */
    override val receiverType: CaType?
        get() = analysisSession.getExplicitCallableReceiverType(backingPsi = null, builder) { cfirSymbol }

    /**
     * 字段类型。
     */
    override val returnType: CaType
        get() = cfirSymbol.returnType(builder)

    /**
     * 字段公开符号位置。
     */
    override val location: CaSymbolLocation
        get() = if (backingPsi != null) CaSymbolLocation.CLASS
        else if (cfirSymbol.callableId.classId == null) CaSymbolLocation.TOP_LEVEL
        else CaSymbolLocation.CLASS

    /**
     * 字段可见性。
     */
    override val visibility: CaSymbolVisibility
        get() = withValidityAssertion { status?.visibility?.asPublicVisibility() ?: CaSymbolVisibility.PUBLIC }

    /**
     * 字段可见性是否显式声明。
     */
    override val isVisibilityExplicit: Boolean
        get() = withValidityAssertion { status?.isVisibilityExplicit == true }

    /**
     * 字段 modality。
     */
    override val modality: CaSymbolModality?
        get() = withValidityAssertion { status?.modality?.asPublicModality() }

    /**
     * 字段 modality 是否显式声明。
     */
    override val isModalityExplicit: Boolean
        get() = withValidityAssertion { status?.isModalityExplicit == true }

    /**
     * 字段当前没有稳定 pointer。
     */
    override fun createPointer(): CaSymbolPointer<CaCallableSymbol> = withValidityAssertion {
        error("Field symbol cannot create a stable pointer")
    }

    /**
     * 字段是否不可变。
     */
    override val isLet: Boolean
        get() = withValidityAssertion { backingPsi?.isVar != true }

    /**
     * 字段是否为 static。
     */
    override val isStatic: Boolean
        get() = withValidityAssertion { backingPsi?.isStatic ?: (status?.isStatic == true) }

    /**
     * 字段是否为 const。
     */
    override val isConst: Boolean
        get() = withValidityAssertion { backingPsi?.isConst ?: (status?.isConst == true) }

    /**
     * 字段名称。
     */
    override val name: Name
        get() = withValidityAssertion { backingPsi?.nameAsSafeName ?: cfirSymbol.name }

    /**
     * 按 PSI 或 CFIR 符号身份比较字段。
     */
    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    /**
     * 按 PSI 或 CFIR 符号身份计算字段 hash。
     */
    override fun hashCode(): Int = psiOrSymbolHashCode()
}

/**
 * CFIR enum constructor 符号实现。
 */
internal class CaCfirEnumConstructorSymbol private constructor(
    /**
     * enum constructor 对应的源码 PSI。
     */
    override val backingPsi: CjEnumConstructor?,
    /**
     * 当前符号绑定的 CFIR Analysis session。
     */
    override val analysisSession: CaCfirSession,
    /**
     * 延迟取得的底层 CFIR enum constructor 符号。
     */
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

    /**
     * enum constructor 底层 CFIR 符号。
     */
    override val cfirSymbol: CfirEnumConstructorSymbol
        get() = super<CaCfirCjBasedSymbol>.cfirSymbol

    /**
     * enum constructor 所在的 use-site 模块。
     */
    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    /**
     * enum constructor CFIR member 状态。
     */
    private val status
        get() = (cfirSymbol.cfir as? CfirMemberDeclaration)?.status

    /**
     * enum constructor 对应的 PSI。
     */
    override val psi
        get() = withValidityAssertion { backingPsiOrFindCurrentPsi { findPsi() } }

    /**
     * enum constructor 公开来源。
     */
    override val origin
        get() = withValidityAssertion { psiOrSymbolOrigin() }

    /**
     * enum constructor 公开注解列表。
     */
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    /**
     * enum constructor callableId。
     */
    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = cfirSymbol.getCallableId()

    /**
     * enum constructor 显式 receiver 类型。
     */
    override val receiverType: CaType?
        get() = analysisSession.getExplicitCallableReceiverType(backingPsi = null, builder) { cfirSymbol }

    /**
     * enum constructor 返回类型。
     */
    override val returnType: CaType
        get() = cfirSymbol.returnType(builder)

    /**
     * enum constructor 公开符号位置。
     */
    override val location: CaSymbolLocation
        get() = CaSymbolLocation.CLASS

    /**
     * enum constructor 可见性。
     */
    override val visibility: CaSymbolVisibility
        get() = withValidityAssertion { status?.visibility?.asPublicVisibility() ?: CaSymbolVisibility.PUBLIC }

    /**
     * enum constructor 可见性是否显式声明。
     */
    override val isVisibilityExplicit: Boolean
        get() = withValidityAssertion { status?.isVisibilityExplicit == true }

    /**
     * enum constructor modality。
     */
    override val modality: CaSymbolModality?
        get() = withValidityAssertion { status?.modality?.asPublicModality() }

    /**
     * enum constructor modality 是否显式声明。
     */
    override val isModalityExplicit: Boolean
        get() = withValidityAssertion { status?.isModalityExplicit == true }

    /**
     * enum constructor 当前没有稳定 pointer。
     */
    override fun createPointer(): CaSymbolPointer<CaCallableSymbol> = withValidityAssertion {
        error("Enum constructor symbol cannot create a stable pointer")
    }

    /**
     * enum constructor 名称。
     */
    override val name: Name
        get() = withValidityAssertion { backingPsi?.name?.let(Name::identifier) ?: cfirSymbol.name }

    /**
     * 所属 enum class 的 classId。
     */
    override val containingEnumClassId: ClassId?
        get() = withValidityAssertion {
            backingPsi?.parentEnum?.getClassId() ?: analysisSession.cfirSession.cfirProvider.getContainingClass(cfirSymbol)?.classId
        }

    /**
     * enum constructor payload 参数类型列表。
     */
    override val payloadTypes: List<CaType>
        get() = withValidityAssertion { cfirSymbol.cfir.payloadParameterTypesOrEmpty().map(builder.typeBuilder::buildType) }

    /**
     * 按 PSI 或 CFIR 符号身份比较 enum constructor。
     */
    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    /**
     * 按 PSI 或 CFIR 符号身份计算 enum constructor hash。
     */
    override fun hashCode(): Int = psiOrSymbolHashCode()
}

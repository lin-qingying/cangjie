package org.cangnova.cangjie.analysis.api.cfir.symbols

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
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirPropertyGetterSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirPropertySetterSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.visibilityByModifiers
import org.cangnova.cangjie.analysis.api.impl.base.util.callableId
import org.cangnova.cangjie.analysis.api.impl.base.util.callableIdForName
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertyGetterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySetterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjProperty

/**
 * 属性及访问器叶子实现。
 *
 * 这里对齐 Kotlin FIR 对 property / getter / setter 分别建模的方式，
 * 让属性族不再和局部变量、值参数等完全不同的语义揉在一个文件里。
 */
internal class CaCfirPropertyGetterSymbol(
    /**
     * getter 所属的属性符号。
     */
    override val owningCaProperty: CaPropertySymbol,
) : CaPropertyGetterSymbol(), CaCfirBasePropertyGetterSymbol {
    /**
     * getter 的公开注解列表。
     */
    override val annotations: CaAnnotationList
        get() = annotationsImpl

    /**
     * getter 对应的 PSI。
     */
    override val psi
        get() = psiImpl

    /**
     * getter 的公开来源。
     */
    override val origin
        get() = originImpl

    /**
     * getter 的 callableId。
     */
    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = callableIdImpl

    /**
     * getter 显式 receiver 类型。
     */
    override val receiverType: CaType?
        get() = receiverTypeImpl

    /**
     * getter 返回类型。
     */
    override val returnType: CaType
        get() = returnTypeImpl

    /**
     * getter 的公开符号位置。
     */
    override val location: CaSymbolLocation
        get() = locationImpl

    /**
     * getter 可见性。
     */
    override val visibility: CaSymbolVisibility
        get() = visibilityImpl

    /**
     * getter 可见性是否显式声明。
     */
    override val isVisibilityExplicit: Boolean
        get() = isVisibilityExplicitImpl

    /**
     * getter modality。
     */
    override val modality: CaSymbolModality?
        get() = modalityImpl

    /**
     * getter modality 是否显式声明。
     */
    override val isModalityExplicit: Boolean
        get() = isModalityExplicitImpl

    /**
     * 创建 getter 符号 pointer。
     */
    override fun createPointer(): CaSymbolPointer<CaFunctionSymbol> = createGetterPointer()

    /**
     * getter 是否为 static。
     */
    override val isStatic: Boolean
        get() = isStaticImpl

    /**
     * getter 是否为 const。
     */
    override val isConst: Boolean
        get() = isConstImpl

    /**
     * getter 是否为 mutating。
     */
    override val isMutating: Boolean
        get() = isMutatingImpl

    /**
     * getter 是否为 override。
     */
    override val isOverride: Boolean
        get() = isOverrideImpl

    /**
     * getter 是否为 operator。
     */
    override val isOperator: Boolean
        get() = isOperatorImpl

    /**
     * getter 是否为 unsafe。
     */
    override val isUnsafe: Boolean
        get() = isUnsafeImpl

    /**
     * getter 是否为 foreign。
     */
    override val isForeign: Boolean
        get() = isForeignImpl

    /**
     * getter 类型参数列表。
     */
    override val typeParameters: List<CaTypeParameterSymbol>
        get() = typeParametersImpl

    /**
     * getter 值参数列表。
     */
    override val valueParameters: List<CaValueParameterSymbol>
        get() = valueParametersImpl

    /**
     * getter 所属属性。
     */
    override val owningProperty: CaPropertySymbol
        get() = owningPropertyImpl

    /**
     * getter 是否为默认生成访问器。
     */
    override val isDefault: Boolean
        get() = isDefaultImpl

    /**
     * 标识当前访问器为 getter。
     */
    override val isGetter: Boolean
        get() = true

    /**
     * 按 PSI 或 CFIR 符号身份比较 getter。
     */
    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)

    /**
     * 按 PSI 或 CFIR 符号身份计算 getter hash。
     */
    override fun hashCode(): Int = psiOrSymbolHashCode()
}

/**
 * CFIR 属性 setter 符号实现。
 */
internal class CaCfirPropertySetterSymbol(
    /**
     * setter 所属的属性符号。
     */
    override val owningCaProperty: CaPropertySymbol,
) : CaPropertySetterSymbol(), CaCfirBasePropertySetterSymbol {
    /**
     * setter 的公开注解列表。
     */
    override val annotations: CaAnnotationList
        get() = annotationsImpl

    /**
     * setter 对应的 PSI。
     */
    override val psi
        get() = psiImpl

    /**
     * setter 的公开来源。
     */
    override val origin
        get() = originImpl

    /**
     * setter 的 callableId。
     */
    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = callableIdImpl

    /**
     * setter 显式 receiver 类型。
     */
    override val receiverType: CaType?
        get() = receiverTypeImpl

    /**
     * setter 返回类型。
     */
    override val returnType: CaType
        get() = returnTypeImpl

    /**
     * setter 的公开符号位置。
     */
    override val location: CaSymbolLocation
        get() = locationImpl

    /**
     * setter 可见性。
     */
    override val visibility: CaSymbolVisibility
        get() = visibilityImpl

    /**
     * setter 可见性是否显式声明。
     */
    override val isVisibilityExplicit: Boolean
        get() = isVisibilityExplicitImpl

    /**
     * setter modality。
     */
    override val modality: CaSymbolModality?
        get() = modalityImpl

    /**
     * setter modality 是否显式声明。
     */
    override val isModalityExplicit: Boolean
        get() = isModalityExplicitImpl

    /**
     * 创建 setter 符号 pointer。
     */
    override fun createPointer(): CaSymbolPointer<CaFunctionSymbol> = createSetterPointer()

    /**
     * setter 是否为 static。
     */
    override val isStatic: Boolean
        get() = isStaticImpl

    /**
     * setter 是否为 const。
     */
    override val isConst: Boolean
        get() = isConstImpl

    /**
     * setter 是否为 mutating。
     */
    override val isMutating: Boolean
        get() = isMutatingImpl

    /**
     * setter 是否为 override。
     */
    override val isOverride: Boolean
        get() = isOverrideImpl

    /**
     * setter 是否为 operator。
     */
    override val isOperator: Boolean
        get() = isOperatorImpl

    /**
     * setter 是否为 unsafe。
     */
    override val isUnsafe: Boolean
        get() = isUnsafeImpl

    /**
     * setter 是否为 foreign。
     */
    override val isForeign: Boolean
        get() = isForeignImpl

    /**
     * setter 类型参数列表。
     */
    override val typeParameters: List<CaTypeParameterSymbol>
        get() = typeParametersImpl

    /**
     * setter 值参数列表。
     */
    override val valueParameters: List<CaValueParameterSymbol>
        get() = valueParametersImpl

    /**
     * setter 所属属性。
     */
    override val owningProperty: CaPropertySymbol
        get() = owningPropertyImpl

    /**
     * setter 是否为默认生成访问器。
     */
    override val isDefault: Boolean
        get() = isDefaultImpl

    /**
     * 标识当前访问器不是 getter。
     */
    override val isGetter: Boolean
        get() = false

    /**
     * setter 的赋值参数。
     */
    override val parameter: CaValueParameterSymbol
        get() = parameterImpl

    /**
     * 按 PSI 或 CFIR 符号身份比较 setter。
     */
    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)

    /**
     * 按 PSI 或 CFIR 符号身份计算 setter hash。
     */
    override fun hashCode(): Int = psiOrSymbolHashCode()
}

/**
 * CFIR 属性符号实现。
 */
internal class CaCfirPropertySymbol private constructor(
    /**
     * 属性对应的源码 PSI。
     */
    override val backingPsi: CjProperty?,
    /**
     * 当前符号绑定的 CFIR Analysis session。
     */
    override val analysisSession: CaCfirSession,
    /**
     * 延迟取得的底层 CFIR 属性符号。
     */
    override val lazyCfirSymbol: Lazy<CfirPropertySymbol>,
) : CaPropertySymbol(),
    CaCfirCjBasedSymbol<CjProperty, CfirPropertySymbol>,
    CaTypeParameterOwnerSymbol,
    CaDeclarationContainerSymbol {
    /**
     * 属性底层 CFIR 符号。
     */
    override val cfirSymbol: CfirPropertySymbol
        get() = super<CaCfirCjBasedSymbol>.cfirSymbol

    constructor(declaration: CjProperty, session: CaCfirSession) : this(
        backingPsi = declaration,
        analysisSession = session,
        lazyCfirSymbol = lazyCfirSymbol(declaration, session),
    )

    constructor(symbol: CfirPropertySymbol, session: CaCfirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? CjProperty,
        analysisSession = session,
        lazyCfirSymbol = lazyOf(symbol),
    )

    /**
     * 属性所在的 use-site 模块。
     */
    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    /**
     * 属性 CFIR member 状态。
     */
    private val status
        get() = (cfirSymbol.cfir as? CfirMemberDeclaration)?.status

    /**
     * 属性公开注解列表。
     */
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    /**
     * 属性对应的 PSI。
     */
    override val psi
        get() = withValidityAssertion { backingPsiOrFindCurrentPsi { findPsi() } }

    /**
     * 属性公开来源。
     */
    override val origin
        get() = withValidityAssertion { psiOrSymbolOrigin() }

    /**
     * 属性 callableId。
     */
    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = withValidityAssertion { if (backingPsi != null) backingPsi.callableIdForName(backingPsi.nameAsSafeName) else cfirSymbol.getCallableId() }

    /**
     * 属性显式 receiver 类型。
     */
    override val receiverType: CaType?
        get() = withValidityAssertion { analysisSession.getExplicitCallableReceiverType(backingPsi, builder) { cfirSymbol } }

    /**
     * 属性返回类型。
     */
    override val returnType: CaType
        get() = withValidityAssertion { cfirSymbol.returnType(builder) }

    /**
     * 属性在公开 API 中的位置。
     */
    override val location: CaSymbolLocation
        get() = withValidityAssertion {
            analysisSession.getCallableSymbolLocation(backingPsi) { cfirSymbol }
        }

    /**
     * 属性可见性。
     */
    override val visibility: CaSymbolVisibility
        get() = withValidityAssertion {
            backingPsi?.psiBasedVisibility(::isOverride)?.asPublicVisibility()
                ?: status?.visibility?.asPublicVisibility()
                ?: CaSymbolVisibility.PUBLIC
        }

    /**
     * 属性可见性是否显式声明。
     */
    override val isVisibilityExplicit: Boolean
        get() = withValidityAssertion {
            backingPsi?.let { it.visibilityByModifiers != null }
                ?: (status?.isVisibilityExplicit == true)
        }

    /**
     * 属性 modality。
     */
    override val modality: CaSymbolModality?
        get() = withValidityAssertion {
            val psiBasedModality = backingPsi?.run {
                val modalityByModifiers = caSymbolModalityByModifiers
                when {
                    modalityByModifiers != null -> when {
                        modalityByModifiers.isOpenFromInterface && !hasBody() -> CaSymbolModality.ABSTRACT
                        else -> modalityByModifiers
                    }

                    hasModifier(CjTokens.CONST_KEYWORD) -> CaSymbolModality.FINAL
                    else -> psiBasedDefaultCaModality(::isOverride)
                }
            }

            psiBasedModality ?: status?.modality?.asPublicModality()
        }

    /**
     * 属性 modality 是否显式声明。
     */
    override val isModalityExplicit: Boolean
        get() = withValidityAssertion {
            backingPsi?.let { it.caSymbolModalityByModifiers != null }
                ?: (status?.isModalityExplicit == true)
        }

    /**
     * 创建属性符号 pointer。
     */
    override fun createPointer(): CaSymbolPointer<CaPropertySymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<CaPropertySymbol> { psi ->
            (psi as? CjProperty)?.symbol
        } ?: error("Property symbol `${name}` cannot create a stable pointer")
    }

    /**
     * 属性是否为不可变 let 形态。
     */
    override val isLet: Boolean
        get() = withValidityAssertion { backingPsi?.isVar != true }

    /**
     * 属性是否为 static。
     */
    override val isStatic: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.STATIC_KEYWORD) ?: (status?.isStatic == true) }

    /**
     * 属性是否为 const。
     */
    override val isConst: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.CONST_KEYWORD) ?: (status?.isConst == true) }

    /**
     * 属性是否为 mutating。
     */
    override val isMutating: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.MUT_KEYWORD) ?: (status?.isMut == true) }

    /**
     * 属性是否为 override。
     */
    override val isOverride: Boolean
        get() = withValidityAssertion { isOverrideWithWorkaround }

    /**
     * 属性是否为 unsafe。
     */
    override val isUnsafe: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.UNSAFE_KEYWORD) ?: (status?.isUnsafe == true) }

    /**
     * 属性是否为 foreign。
     */
    override val isForeign: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.FOREIGN_KEYWORD) ?: (status?.isForeign == true) }

    /**
     * 属性类型参数列表。
     */
    override val typeParameters: List<CaTypeParameterSymbol>
        get() = withValidityAssertion {
            createCaTypeParameters() ?: (cfirSymbol.cfir as? CfirProperty)
                ?.typeParameters
                ?.map { builder.classifierBuilder.buildTypeParameterSymbol(it.symbol) }
                .orEmpty()
        }

    /**
     * 属性 getter 符号。
     */
    override val getter: CaPropertyGetterSymbol?
        get() = (cfirSymbol.cfir as? CfirProperty)?.getter?.symbol?.let { getterSymbol ->
            builder.functionBuilder.buildPropertyAccessorSymbol(getterSymbol, this, CaCfirPropertyAccessorKind.GETTER) as CaPropertyGetterSymbol
        }

    /**
     * 属性 setter 符号。
     */
    override val setter: CaPropertySetterSymbol?
        get() = (cfirSymbol.cfir as? CfirProperty)?.setter?.symbol?.let { setterSymbol ->
            builder.functionBuilder.buildPropertyAccessorSymbol(setterSymbol, this, CaCfirPropertyAccessorKind.SETTER) as CaPropertySetterSymbol
        }

    /**
     * 属性名称。
     */
    override val name: Name
        get() = withValidityAssertion { backingPsi?.nameAsSafeName ?: cfirSymbol.name }
}

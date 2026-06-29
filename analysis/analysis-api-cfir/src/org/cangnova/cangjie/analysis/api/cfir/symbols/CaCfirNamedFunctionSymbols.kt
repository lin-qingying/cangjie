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
    /**
     * 命名函数对应的源码 PSI。
     */
    override val backingPsi: CjNamedFunction?,
    /**
     * 当前符号绑定的 CFIR Analysis session。
     */
    override val analysisSession: CaCfirSession,
    /**
     * 延迟取得的底层 CFIR 命名函数符号。
     */
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

    /**
     * 函数所在的 use-site 模块。
     */
    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    /**
     * 函数 CFIR member 状态。
     */
    private val status
        get() = (cfirSymbol.cfir as? CfirMemberDeclaration)?.status

    /**
     * 函数对应的 PSI。
     */
    override val psi: PsiElement? get() = withValidityAssertion { backingPsiOrFindCurrentPsi { findPsi() } }
    /**
     * 函数名称。
     */
    override val name: Name get() = withValidityAssertion { backingPsi?.nameAsSafeName ?: cfirSymbol.name }
    /**
     * 函数公开来源。
     */
    override val origin get() = withValidityAssertion { psiOrSymbolOrigin() }

    /**
     * 函数公开注解列表。
     */
    override val annotations: CaAnnotationList
        get() = withValidityAssertion {
            psiOrSymbolAnnotationList()
        }
    /**
     * 函数类型参数列表。
     */
    override val typeParameters: List<CaTypeParameterSymbol>
        get() = withValidityAssertion {
            createCaTypeParameters() ?: cfirSymbol.createCjTypeParameters(builder)
        }
    /**
     * 函数值参数列表。
     */
    override val valueParameters: List<CaValueParameterSymbol>
        get() = withValidityAssertion {
            createCaValueParameters() ?: cfirSymbol.createCjValueParameters(builder)
        }

    /**
     * 函数 callableId。
     */
    override val callableId: CallableId?
        get() = withValidityAssertion {
            if (backingPsi != null)
                backingPsi.callableId
            else
                cfirSymbol.getCallableId()
        }

    /**
     * 函数显式 receiver 类型。
     */
    override val receiverType: CaType?
        get() = withValidityAssertion { analysisSession.getExplicitCallableReceiverType(backingPsi, builder) { cfirSymbol } }

    /**
     * 函数返回类型。
     */
    override val returnType: CaType
        get() = withValidityAssertion { createReturnType() }

    /**
     * 函数在公开 API 中的位置。
     */
    override val location: CaSymbolLocation
        get() = withValidityAssertion { analysisSession.getCallableSymbolLocation(backingPsi) { cfirSymbol } }

    /**
     * 函数可见性。
     */
    override val visibility: CaSymbolVisibility
        get() = withValidityAssertion {
            backingPsi?.psiBasedVisibility(::isOverride)?.asPublicVisibility()
                ?: status?.visibility?.asPublicVisibility()
                ?: CaSymbolVisibility.PUBLIC
        }

    /**
     * 函数可见性是否显式声明。
     */
    override val isVisibilityExplicit: Boolean
        get() = withValidityAssertion {
            backingPsi?.let { it.visibilityByModifiers != null }
                ?: (status?.isVisibilityExplicit == true)
        }

    /**
     * 函数 modality。
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

                    isTopLevel || isLocal -> CaSymbolModality.FINAL
                    hasModifier(CjTokens.CONST_KEYWORD) -> CaSymbolModality.FINAL
                    else -> psiBasedDefaultCaModality(::isOverride)
                }
            }

            psiBasedModality ?: status?.modality?.asPublicModality()
        }

    /**
     * 函数 modality 是否显式声明。
     */
    override val isModalityExplicit: Boolean
        get() = withValidityAssertion {
            backingPsi?.let { it.caSymbolModalityByModifiers != null }
                ?: (status?.isModalityExplicit == true)
        }

    @OptIn(CaImplementationDetail::class)
    /**
     * 创建可恢复当前命名函数符号的 pointer。
     */
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
    /**
     * 函数是否为 static。
     */
    override val isStatic: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.STATIC_KEYWORD) ?: (status?.isStatic == true) }

    /**
     * 函数是否为 const。
     */
    override val isConst: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.CONST_KEYWORD) ?: (status?.isConst == true) }

    /**
     * 函数是否为 mutating。
     */
    override val isMutating: Boolean
        get() = withValidityAssertion {
            psiHasModifierConsideringInheritance(CjTokens.MUT_KEYWORD) ?: (status?.isMut == true)
        }

    /**
     * 函数是否为 operator。
     */
    override val isOperator: Boolean
        get() = withValidityAssertion {
            psiHasModifierConsideringInheritance(CjTokens.OPERATOR_KEYWORD) ?: (status?.isOperator == true)
        }
    /**
     * 函数是否为 unsafe。
     */
    override val isUnsafe: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.UNSAFE_KEYWORD) ?: (status?.isUnsafe == true) }

    /**
     * 函数是否为 foreign。
     */
    override val isForeign: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.FOREIGN_KEYWORD) ?: (status?.isForeign == true) }

    /**
     * 函数是否为 override。
     */
    override val isOverride: Boolean
        get() = withValidityAssertion { isOverrideWithWorkaround }

    /**
     * 按源码显式修饰符和 override 继承规则判断函数修饰符。
     */
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
/**
 * 为成员命名函数创建 owner-based pointer。
 */
private fun CaCfirNamedFunctionSymbol.createMemberFunctionPointer(): CaSymbolPointer<CaNamedFunctionSymbol> {
    return CaCfirMemberFunctionSymbolPointer(
        ownerPointer = analysisSession.createOwnerPointer<CaDeclarationContainerSymbol>(this),
        name = name,
        signature = CfirCallableSignature.createSignature(cfirSymbol),
    )
}

/**
 * CFIR main 函数符号实现。
 */
internal class CaCfirMainFunctionSymbol(
    /**
     * 底层 CFIR main 函数符号。
     */
    final override val cfirSymbol: CfirMainFunctionSymbol,
    /**
     * 当前符号绑定的 CFIR Analysis session。
     */
    final override val analysisSession: CaCfirSession,
    /**
     * main 函数所在模块。
     */
    final override val containingModule: CaModule,
    /**
     * main 函数符号生命周期 token。
     */
    final override val token: CaLifetimeToken,
) : CaMainFunctionSymbol(), CaCfirSymbol<CfirMainFunctionSymbol> {
    /**
     * main 函数 CFIR member 状态。
     */
    private val status
        get() = (cfirSymbol.cfir as? CfirMemberDeclaration)?.status

    /**
     * main 函数公开注解列表。
     */
    override val annotations: CaAnnotationList
        get() = withValidityAssertion {
            CaCfirAnnotationListForDeclaration.create(cfirSymbol, builder)
        }

    /**
     * main 函数符号当前不暴露 PSI。
     */
    override val psi: PsiElement?
        get() = null

    /**
     * main 函数 callableId。
     */
    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = cfirSymbol.getCallableId()

    /**
     * main 函数显式 receiver 类型。
     */
    override val receiverType: CaType?
        get() = analysisSession.getExplicitCallableReceiverType(backingPsi = null, builder) { cfirSymbol }

    /**
     * main 函数返回类型。
     */
    override val returnType: CaType
        get() = cfirSymbol.returnType(builder)

    /**
     * main 函数公开符号位置。
     */
    override val location: CaSymbolLocation
        get() = analysisSession.getCallableSymbolLocation(backingPsi = null) { cfirSymbol }

    /**
     * main 函数可见性。
     */
    override val visibility: CaSymbolVisibility
        get() = status?.visibility?.asPublicVisibility() ?: CaSymbolVisibility.PUBLIC

    /**
     * main 函数可见性是否显式声明。
     */
    override val isVisibilityExplicit: Boolean
        get() = status?.isVisibilityExplicit == true

    /**
     * main 函数 modality。
     */
    override val modality: CaSymbolModality?
        get() = status?.modality?.asPublicModality()

    /**
     * main 函数 modality 是否显式声明。
     */
    override val isModalityExplicit: Boolean
        get() = status?.isModalityExplicit == true

    /**
     * main 函数当前没有稳定 pointer。
     */
    override fun createPointer(): CaSymbolPointer<CaFunctionSymbol> = withValidityAssertion {
        error("Main function symbol cannot create a stable pointer")
    }

    /**
     * main 函数是否为 static。
     */
    override val isStatic: Boolean
        get() = status?.isStatic == true

    /**
     * main 函数是否为 const。
     */
    override val isConst: Boolean
        get() = status?.isConst == true

    /**
     * main 函数是否为 mutating。
     */
    override val isMutating: Boolean
        get() = status?.isMut == true

    /**
     * main 函数是否为 override。
     */
    override val isOverride: Boolean
        get() = status?.isOverride == true

    /**
     * main 函数是否为 operator。
     */
    override val isOperator: Boolean
        get() = status?.isOperator == true

    /**
     * main 函数是否为 unsafe。
     */
    override val isUnsafe: Boolean
        get() = status?.isUnsafe == true

    /**
     * main 函数是否为 foreign。
     */
    override val isForeign: Boolean
        get() = status?.isForeign == true

    /**
     * main 函数类型参数列表。
     */
    override val typeParameters: List<CaTypeParameterSymbol>
        get() = (cfirSymbol.cfir as? CfirCallableDeclaration)
            ?.typeParameters
            ?.map { typeParameter -> builder.classifierBuilder.buildTypeParameterSymbol(typeParameter.symbol) }
            .orEmpty()

    /**
     * main 函数值参数列表。
     */
    override val valueParameters: List<CaValueParameterSymbol>
        get() = (cfirSymbol.cfir as? CfirFunction)
            ?.valueParameters
            ?.map { valueParameter -> builder.variableBuilder.buildValueParameterSymbol(valueParameter.symbol) }
            .orEmpty()

    /**
     * main 函数名称。
     */
    override val name: Name
        get() = cfirSymbol.name
}

/**
 * CFIR macro 函数符号实现。
 */
internal class CaCfirMacroSymbol private constructor(
    /**
     * macro 声明对应的源码 PSI。
     */
    override val backingPsi: CjMacroDeclaration?,
    /**
     * 当前符号绑定的 CFIR Analysis session。
     */
    override val analysisSession: CaCfirSession,
    /**
     * 延迟取得的底层 CFIR macro 符号。
     */
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

    /**
     * macro 底层 CFIR 符号。
     */
    override val cfirSymbol: CfirMacroDeclarationSymbol
        get() = super<CaCfirCjBasedSymbol>.cfirSymbol

    /**
     * macro 所在的 use-site 模块。
     */
    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    /**
     * macro CFIR member 状态。
     */
    private val status
        get() = (cfirSymbol.cfir as? CfirMemberDeclaration)?.status

    /**
     * macro 对应的 PSI。
     */
    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsiOrFindCurrentPsi { findPsi() } }

    /**
     * macro 公开来源。
     */
    override val origin
        get() = withValidityAssertion { psiOrSymbolOrigin() }

    /**
     * macro 公开注解列表。
     */
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    /**
     * macro callableId。
     */
    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = withValidityAssertion {
            cfirSymbol.getCallableId()
        }

    /**
     * macro 显式 receiver 类型。
     */
    override val receiverType: CaType?
        get() = withValidityAssertion { analysisSession.getExplicitCallableReceiverType(backingPsi, builder) { cfirSymbol } }

    /**
     * macro 返回类型。
     */
    override val returnType: CaType
        get() = withValidityAssertion { createReturnType() }

    /**
     * macro 公开符号位置。
     */
    override val location: CaSymbolLocation
        get() = withValidityAssertion { analysisSession.getCallableSymbolLocation(backingPsi) { cfirSymbol } }

    /**
     * macro 可见性。
     */
    override val visibility: CaSymbolVisibility
        get() = withValidityAssertion {
            backingPsi?.psiBasedVisibility(::isOverride)?.asPublicVisibility()
                ?: status?.visibility?.asPublicVisibility()
                ?: CaSymbolVisibility.PUBLIC
        }

    /**
     * macro 可见性是否显式声明。
     */
    override val isVisibilityExplicit: Boolean
        get() = withValidityAssertion {
            backingPsi?.let { it.visibilityByModifiers != null }
                ?: (status?.isVisibilityExplicit == true)
        }

    /**
     * macro modality。
     */
    override val modality: CaSymbolModality?
        get() = withValidityAssertion {
            val psiBasedModality = backingPsi?.run {
                caSymbolModalityByModifiers ?: psiBasedDefaultCaModality(::isOverride)
            }
            psiBasedModality ?: status?.modality?.asPublicModality()
        }

    /**
     * macro modality 是否显式声明。
     */
    override val isModalityExplicit: Boolean
        get() = withValidityAssertion {
            backingPsi?.let { it.caSymbolModalityByModifiers != null }
                ?: (status?.isModalityExplicit == true)
        }

    /**
     * macro 当前没有稳定 pointer。
     */
    override fun createPointer(): CaSymbolPointer<CaFunctionSymbol> = withValidityAssertion {
        error("Macro symbol cannot create a stable pointer")
    }

    /**
     * macro 是否为 static。
     */
    override val isStatic: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.STATIC_KEYWORD) ?: (status?.isStatic == true) }

    /**
     * macro 是否为 const。
     */
    override val isConst: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.CONST_KEYWORD) ?: (status?.isConst == true) }

    /**
     * macro 是否为 mutating。
     */
    override val isMutating: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.MUT_KEYWORD) ?: (status?.isMut == true) }

    /**
     * macro 是否为 override。
     */
    override val isOverride: Boolean
        get() = withValidityAssertion { isOverrideWithWorkaround }

    /**
     * macro 是否为 operator。
     */
    override val isOperator: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.OPERATOR_KEYWORD) ?: (status?.isOperator == true) }

    /**
     * macro 是否为 unsafe。
     */
    override val isUnsafe: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.UNSAFE_KEYWORD) ?: (status?.isUnsafe == true) }

    /**
     * macro 是否为 foreign。
     */
    override val isForeign: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.FOREIGN_KEYWORD) ?: (status?.isForeign == true) }

    /**
     * macro 类型参数列表。
     */
    override val typeParameters: List<CaTypeParameterSymbol>
        get() = withValidityAssertion {
            createCaTypeParameters() ?: (cfirSymbol.cfir as? CfirCallableDeclaration)
                ?.typeParameters
                ?.map { typeParameter -> builder.classifierBuilder.buildTypeParameterSymbol(typeParameter.symbol) }
                .orEmpty()
        }

    /**
     * macro 值参数列表。
     */
    override val valueParameters: List<CaValueParameterSymbol>
        get() = withValidityAssertion {
            createCaValueParameters() ?: (cfirSymbol.cfir as? CfirFunction)
                ?.valueParameters
                ?.map { valueParameter -> builder.variableBuilder.buildValueParameterSymbol(valueParameter.symbol) }
                .orEmpty()
        }

    /**
     * macro 名称。
     */
    override val name: Name
        get() = withValidityAssertion { backingPsi?.nameAsSafeName ?: cfirSymbol.name }

    /**
     * 按 PSI 或 CFIR 符号身份比较 macro。
     */
    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    /**
     * 按 PSI 或 CFIR 符号身份计算 macro hash。
     */
    override fun hashCode(): Int = psiOrSymbolHashCode()
}

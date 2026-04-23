package org.cangnova.cangjie.analysis.api.cfir.symbols

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.findPsi
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirAnonymousFunctionSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirCallableSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirExtendMemberCallableSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirLocalVariableSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirPatternBindingSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirPatternVariableSymbolPointer
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaValueParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType

internal interface CaCfirBackedSymbol<T : CfirBasedSymbol<*>> {
    val backingSymbol: T

    val cfirSymbol: T
        get() = backingSymbol
}

/**
 * callable 公共语义在 CFIR 侧的复用 support。
 *
 * Kotlin FIR 的做法也是让 callable leaf 直接继承公开抽象叶子类型，
 * 然后把 CFIR 复用逻辑放到 support/helper 层，避免内部基类劫持公开继承树。
 */
internal interface CaCfirCallableSymbolSupport<T : CfirCallableSymbol<*>> :
    CaDeclarationSymbol,
    CaCfirSymbol<T>,
    CaCfirBackedSymbol<T> {
    override val cfirSymbol: T
        get() = backingSymbol

    val status: CfirDeclarationStatus?
        get() = (backingSymbol.cfir as? CfirMemberDeclaration)?.status

    override val annotations: CaAnnotationList
        get() = withValidityAssertion {
            CaCfirAnnotationListForDeclaration.create(backingSymbol, builder)
        }

    override val psi: PsiElement?
        get() = backingSymbol.backingPsiIfApplicable ?: findPsi()

    override val origin: CaSymbolOrigin
        get() = backingSymbol.origin.asPublicOrigin()

    override val containingDeclaration: CaSymbol?
        get() = analysisSession.findContainingDeclarationSymbol(psi)

    override val visibility: CaSymbolVisibility
        get() = status?.visibility?.asPublicVisibility() ?: CaSymbolVisibility.PUBLIC

    override val isVisibilityExplicit: Boolean
        get() = status?.isVisibilityExplicit == true

    override val modality: CaSymbolModality?
        get() = status?.modality?.asPublicModality()

    override val isModalityExplicit: Boolean
        get() = status?.isModalityExplicit == true

    val callableIdImpl: CallableId?
        get() {
            val callableDeclaration = backingSymbol.cfir as? CfirCallableDeclaration
            return backingSymbol.callableId.takeUnless { callableDeclaration?.isLocal == true }
        }

    val receiverTypeImpl: CaType?
        get() {
            val callablePsi = psi as? org.cangnova.cangjie.psi.CjCallableDeclaration ?: return null
            if (callablePsi.getStrictParentOfType<org.cangnova.cangjie.psi.CjExtend>() == null) return null
            return backingSymbol.cfir?.dispatchReceiverType?.let(builder.typeBuilder::buildType)
        }

    val returnTypeImpl: CaType
        get() = backingSymbol.returnType(builder)

    val locationImpl: CaSymbolLocation
        get() = analysisSession.locationForDeclaration(this)
}

/**
 * 函数族 public 叶子实现共享的 CFIR support。
 */
internal interface CaCfirFunctionSymbolSupport<T : CfirCallableSymbol<*>> : CaCfirCallableSymbolSupport<T> {
    val isStaticImpl: Boolean
        get() = status?.isStatic == true

    val isConstImpl: Boolean
        get() = status?.isConst == true

    val isMutatingImpl: Boolean
        get() = status?.isMut == true

    val isOverrideImpl: Boolean
        get() = status?.isOverride == true

    val isOperatorImpl: Boolean
        get() = status?.isOperator == true

    val isUnsafeImpl: Boolean
        get() = status?.isUnsafe == true

    val isForeignImpl: Boolean
        get() = status?.isForeign == true

    val typeParametersImpl: List<CaTypeParameterSymbol>
        get() = (backingSymbol.cfir as? CfirCallableDeclaration)
            ?.typeParameters
            ?.filterIsInstance<CfirTypeParameter>()
            ?.map { typeParameter -> builder.classifierBuilder.buildTypeParameterSymbol(typeParameter.symbol) }
            .orEmpty()

    val valueParametersImpl: List<CaValueParameterSymbol>
        get() = (backingSymbol.cfir as? CfirFunction)
            ?.valueParameters
            ?.mapIndexed { index, parameter ->
                builder.variableBuilder.buildOwnedValueParameterSymbol(
                    ownerSymbol = this as CaValueParameterOwnerSymbol,
                    parameter = parameter,
                    parameterIndex = index,
                )
            }
            .orEmpty()
}

/**
 * callable 恢复逻辑严格落在专用 pointer 类型中。
 *
 * 这里仅负责根据 public cache key 选择对应 pointer，不再引入统一 restore-key 协议。
 */
internal fun <S : CaCallableSymbol> CaSymbol.createStableCallablePointer(
    symbolType: Class<S>,
): CaSymbolPointer<S> {
    val cacheKey = publicSymbolCacheKeyOrNull()
        ?: error("Callable symbol `${this::class.simpleName}` is missing a stable public cache key")
    return when (cacheKey) {
        is CaCfirCallableSymbolCacheKey -> CaCfirCallableSymbolPointer(cacheKey, symbolType)
        is CaCfirExtendMemberCallableSymbolCacheKey -> CaCfirExtendMemberCallableSymbolPointer(cacheKey, symbolType)
        is CaCfirPsiSymbolCacheKey -> createPsiBasedCallablePointer(cacheKey, symbolType)
        else -> error("Unsupported callable pointer key `${cacheKey::class.simpleName}` for `${this::class.simpleName}`")
    }
}

@Suppress("UNCHECKED_CAST")
private fun <S : CaCallableSymbol> createPsiBasedCallablePointer(
    cacheKey: CaCfirPsiSymbolCacheKey,
    symbolType: Class<S>,
): CaSymbolPointer<S> {
    val pointer = when (cacheKey.kind) {
        CaCfirPsiSymbolKind.ANONYMOUS_FUNCTION -> CaCfirAnonymousFunctionSymbolPointer(cacheKey.psi)
        CaCfirPsiSymbolKind.LOCAL_VARIABLE -> CaCfirLocalVariableSymbolPointer(cacheKey.psi)
        CaCfirPsiSymbolKind.PATTERN_VARIABLE -> CaCfirPatternVariableSymbolPointer(cacheKey.psi)
        CaCfirPsiSymbolKind.PATTERN_BINDING -> CaCfirPatternBindingSymbolPointer(cacheKey.psi)
        else -> error("Psi symbol kind `${cacheKey.kind}` is not a callable pointer kind")
    }
    return pointer as CaSymbolPointer<S>
}

internal interface CaCfirNamedFunctionSymbolSupport<T : CfirCallableSymbol<*>> :
    CaCfirFunctionSymbolSupport<T>,
    CaNamedSymbol {
    val nameImpl: Name
        get() = backingSymbol.name
}

/**
 * 变量族 public 叶子实现共享的 CFIR support。
 */
internal interface CaCfirVariableSymbolSupport<T : CfirCallableSymbol<*>> :
    CaCfirCallableSymbolSupport<T>,
    CaNamedSymbol {
    val nameImpl: Name
        get() = backingSymbol.name
}

/**
 * 局部变量族在 public API 中拥有固定的局部可见性语义。
 */
internal interface CaCfirLocalVariableSymbolSupport<T : CfirCallableSymbol<*>> : CaCfirVariableSymbolSupport<T> {
    val localCallableIdImpl: CallableId?
        get() = null

    val localVisibilityImpl: CaSymbolVisibility
        get() = CaSymbolVisibility.LOCAL

    val isVisibilityExplicitImpl: Boolean
        get() = false

    val modalityImpl: CaSymbolModality?
        get() = CaSymbolModality.FINAL

    val isModalityExplicitImpl: Boolean
        get() = false

    val localLocationImpl: CaSymbolLocation
        get() = CaSymbolLocation.LOCAL
}

/**
 * 属性访问器在公开 API 中是函数族叶子，但其 owning property / 默认实现语义固定。
 */
internal interface CaCfirPropertyAccessorSymbolSupport<T : CfirCallableSymbol<*>> : CaCfirFunctionSymbolSupport<T> {
    val accessorCallableIdImpl: CallableId?
        get() = null

    val owningPropertyImpl: CaPropertySymbol
        get() = (psi as? org.cangnova.cangjie.psi.CjPropertyAccessor)?.property?.let { propertyPsi ->
            analysisSession.getPublicSymbolByPsi<CaPropertySymbol>(propertyPsi)
        } ?: error("Property accessor requires owning property")

    val isDefaultImpl: Boolean
        get() = (psi as? org.cangnova.cangjie.psi.CjPropertyAccessor)?.hasBody() == false
}

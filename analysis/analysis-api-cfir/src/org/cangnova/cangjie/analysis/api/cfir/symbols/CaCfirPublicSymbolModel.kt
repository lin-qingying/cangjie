package org.cangnova.cangjie.analysis.api.cfir.symbols

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirCallableSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirClassLikeSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirExtendMemberCallableSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirExtendSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirFileSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirPackageSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirPsiSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirPsiSymbolKind
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirPropertyAccessorKind
import org.cangnova.cangjie.analysis.api.cfir.components.asCaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.components.createFileSymbol
import org.cangnova.cangjie.analysis.api.cfir.components.createPropertyAccessorSymbol
import org.cangnova.cangjie.analysis.api.cfir.components.createScriptSymbol
import org.cangnova.cangjie.analysis.api.cfir.components.createTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.cfir.components.createValueParameterSymbol
import org.cangnova.cangjie.analysis.api.cfir.components.getPublicSymbol
import org.cangnova.cangjie.analysis.api.cfir.components.publicSymbolCacheKeyOrNull
import org.cangnova.cangjie.analysis.api.cfir.components.renderAnnotations
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirAnonymousFunctionSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirCallableSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirClassLikeSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirExtendMemberCallableSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirExtendSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirFileSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirLocalVariableSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirPackageSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirPatternBindingSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirPatternVariableSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirPropertyGetterSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirPropertySetterSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirScriptSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirSourceTypeParameterSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirTypeParameterSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirValueParameterSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.types.asCaType
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaAnnotatedSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaAnonymousFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassKind
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFinalizerSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaMacroSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaMainFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertyGetterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySetterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaScriptSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.api.symbols.CaTypeAliasSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaValueParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.payloadParameterTypesOrEmpty
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirExtendSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjScript
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.source.psi

/**
 * CFIR 公共 symbol 的最小共享宿主协议。
 *
 * Kotlin Analysis API FIR 侧同样会把 public leaf 与后端复用逻辑拆开，
 * 避免 public 抽象类与实现基类发生双重继承冲突。
 */
internal interface CaCfirSymbolMixin : CaSymbol {
    override val containingModule: CaModule

    override val token: CaLifetimeToken

}

/**
 * CFIR public symbol 统一通过 restore key 构造指针。
 *
 * 这样 public leaf 在覆写更精确的 pointer 返回类型时，仍然复用同一套恢复协议，
 * 避免每个具体符号类重复编写相同的指针桥接代码。
 */

internal sealed class CaCfirSymbolBase(
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : CaCfirSymbolMixin

internal interface CaCfirBackedSymbol<T : CfirSymbol<*>> {
    val backingSymbol: T
}

/**
 * declaration-backed public symbol 的统一桥接层。
 *
 * Kotlin FIR 侧也是把 public leaf 与后端共享逻辑拆开：public leaf 直接继承
 * Analysis API 抽象叶子类型，后端复用逻辑则下沉到 helper / mixin。
 * 这样可以避免 public 抽象类与内部实现基类发生双 class 继承冲突。
 */
internal interface CaCfirDeclarationBackedSymbolMixin<T : CfirSymbol<*>> :
    CaDeclarationSymbol,
    CaCfirSymbolMixin,
    CaCfirBackedSymbol<T> {
    val analysisSession: CaCfirSession

    val declaration: CfirDeclaration
        get() = backingSymbol.cfir as CfirDeclaration

    val status: CfirDeclarationStatus?
        get() = (declaration as? CfirMemberDeclaration)?.status

    override val annotations: CaAnnotationList
        get() = withValidityAssertion {
            analysisSession.renderAnnotations(this).asCaAnnotationList(token)
        }

    override val psi: PsiElement?
        get() = analysisSession.lookupSourcePsi(backingSymbol)

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

}

internal abstract class CaCfirDeclarationBackedSymbol<T : CfirSymbol<*>>(
    final override val backingSymbol: T,
    final override val analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirSymbolBase(containingModule, token), CaCfirDeclarationBackedSymbolMixin<T> {
    /**
     * declaration-backed 公共 symbol 统一复用 session 侧的注解构造协议，
     * 保持 declaration annotations 与 metadata 解释逻辑只有一份来源。
     */
    override val annotations: CaAnnotationList
        get() = withValidityAssertion {
            analysisSession.renderAnnotations(this).asCaAnnotationList(token)
        }

    final override val psi: PsiElement?
        get() = analysisSession.lookupSourcePsi(backingSymbol)

    override val declaration: CfirDeclaration
        get() = backingSymbol.cfir as CfirDeclaration

    override val status: CfirDeclarationStatus?
        get() = (declaration as? CfirMemberDeclaration)?.status

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

}

internal abstract class CaCfirClassifierBackedSymbol<T : CfirSymbol<*>>(
    backingSymbol: T,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirDeclarationBackedSymbol<T>(backingSymbol, analysisSession, containingModule, token), CaClassifierSymbol

internal abstract class CaCfirClassLikeSymbolBase<T : CfirClassLikeSymbol<*>>(
    backingSymbol: T,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirClassifierBackedSymbol<T>(backingSymbol, analysisSession, containingModule, token),
    CaClassLikeSymbol,
    CaNamedSymbol,
    CaTypeParameterOwnerSymbol {
    override val classId: ClassId?
        get() = backingSymbol.classId

    override val name: Name
        get() = backingSymbol.name

    override val typeParameters: List<CaTypeParameterSymbol>
        get() = (backingSymbol.cfir as? CfirClassLikeDeclaration)
            ?.typeParameters
            ?.map { typeParameter -> analysisSession.createTypeParameterSymbol(typeParameter.symbol) }
            .orEmpty()

    override val location: CaSymbolLocation
        get() = analysisSession.locationForDeclaration(this)
}

internal class CaCfirPackageSymbolImpl(
    override val fqName: FqName,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirSymbolBase(containingModule, token), CaPackageSymbol, CaNamedSymbol {
    override val psi: PsiElement?
        get() = null

    override val origin: CaSymbolOrigin
        get() = CaSymbolOrigin.UNKNOWN

    override val location: CaSymbolLocation
        get() = CaSymbolLocation.TOP_LEVEL

    override val name: Name
        get() = fqName.shortNameOrSpecial()

    override fun createPointer(): CaSymbolPointer<CaSymbol> = withValidityAssertion {
        CaCfirPackageSymbolPointer(CaCfirPackageSymbolCacheKey(fqName))
    }
}

internal class CaCfirFileSymbolImpl(
    override val backingSymbol: CfirFileSymbol,
    override val file: CjFile,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirSymbolBase(containingModule, token), CaFileSymbol, CaNamedSymbol, CaCfirBackedSymbol<CfirFileSymbol> {
    override val psi: PsiElement?
        get() = withValidityAssertion { file }

    override val origin: CaSymbolOrigin
        get() = backingSymbol.origin.asPublicOrigin()

    override val location: CaSymbolLocation
        get() = CaSymbolLocation.TOP_LEVEL

    override val packageFqName: FqName
        get() = file.packageFqName

    override val name: Name
        get() = Name.identifier(file.name)

    override fun createPointer(): CaSymbolPointer<CaSymbol> = withValidityAssertion {
        CaCfirFileSymbolPointer(CaCfirFileSymbolCacheKey(file))
    }
}

internal class CaCfirClassSymbolImpl(
    backingSymbol: CfirClassLikeSymbol<*>,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirClassLikeSymbolBase<CfirClassLikeSymbol<*>>(backingSymbol, analysisSession, containingModule, token), CaClassSymbol {
    override val classKind: CaClassKind
        get() = when (backingSymbol) {
            is CfirClassSymbol -> CaClassKind.CLASS
            is CfirInterfaceSymbol -> CaClassKind.INTERFACE
            is org.cangnova.cangjie.cfir.symbols.CfirStructSymbol -> CaClassKind.STRUCT
            is org.cangnova.cangjie.cfir.symbols.CfirEnumSymbol -> CaClassKind.ENUM
            else -> error("Unsupported class-like symbol `${backingSymbol::class.simpleName}`")
        }

    override val superTypes: List<CaType>
        get() = analysisSession.queryClassLikeSuperTypes(backingSymbol).map { superType -> superType.asCaType(analysisSession) }

    override fun createPointer(): CaSymbolPointer<CaAnnotatedSymbol> = withValidityAssertion {
        val stableClassId = classId ?: error("Class symbol `${name}` is missing ClassId")
        CaCfirClassLikeSymbolPointer(CaCfirClassLikeSymbolCacheKey(stableClassId), CaAnnotatedSymbol::class.java)
    }
}

internal class CaCfirTypeAliasSymbolImpl(
    backingSymbol: CfirTypeAliasSymbol,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirClassLikeSymbolBase<CfirTypeAliasSymbol>(backingSymbol, analysisSession, containingModule, token), CaTypeAliasSymbol {
    override val expandedType: CaType
        get() = ((backingSymbol.cfir as CfirTypeAlias).expandedTypeRef.coneTypeOrNull?.asCaType(analysisSession))
            ?: error("Cannot build expanded type for `${backingSymbol.classId.asString()}`")

    override fun createPointer(): CaSymbolPointer<CaAnnotatedSymbol> = withValidityAssertion {
        CaCfirClassLikeSymbolPointer(CaCfirClassLikeSymbolCacheKey(backingSymbol.classId), CaAnnotatedSymbol::class.java)
    }
}

internal class CaCfirExtendSymbolImpl(
    backingSymbol: CfirExtendSymbol,
    internal val extendPsi: org.cangnova.cangjie.psi.CjExtend?,
    private val stableExtendId: String,
    internal val extendPackageFqName: FqName,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirDeclarationBackedSymbol<CfirExtendSymbol>(backingSymbol, analysisSession, containingModule, token),
    CaExtendSymbol,
    CaTypeParameterOwnerSymbol {
    private val extendDeclaration: CfirExtend
        get() = backingSymbol.cfir

    override val extendId: String
        get() = stableExtendId

    override val targetClassId: ClassId?
        get() = extendDeclaration.extendedTypeRef.coneTypeOrNull?.classIdOrPrimitiveClassId

    override val extendedType: CaType
        get() = extendDeclaration.extendedTypeRef.coneTypeOrNull?.asCaType(analysisSession)
            ?: error("Cannot build extended type for extend `${extendId}`")

    override val superTypes: List<CaType>
        get() = extendDeclaration.superTypeRefs.mapNotNull { superTypeRef -> superTypeRef.coneTypeOrNull?.asCaType(analysisSession) }

    override val typeParameters: List<CaTypeParameterSymbol>
        get() = extendDeclaration.typeParameters.map { typeParameter -> analysisSession.createTypeParameterSymbol(typeParameter.symbol) }

    override val location: CaSymbolLocation
        get() = CaSymbolLocation.TOP_LEVEL

    override val containingDeclaration: CaSymbol?
        get() = analysisSession.findContainingDeclarationSymbol(extendPsi ?: psi)

    override fun createPointer(): CaSymbolPointer<CaAnnotatedSymbol> = withValidityAssertion {
        CaCfirExtendSymbolPointer(CaCfirExtendSymbolCacheKey(extendId))
    }
}

internal class CaCfirTypeParameterSymbolImpl(
    backingSymbol: CfirTypeParameterSymbol,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
    internal val stableParameterIndex: Int? = null,
) : CaCfirClassifierBackedSymbol<CfirTypeParameterSymbol>(backingSymbol, analysisSession, containingModule, token),
    CaTypeParameterSymbol,
    CaNamedSymbol {
    override val name: Name
        get() = backingSymbol.name

    override val upperBounds: List<CaType>
        get() = backingSymbol.cfir.bounds.mapNotNull { bound -> bound.coneTypeOrNull?.asCaType(analysisSession) }

    override val origin: CaSymbolOrigin
        get() = backingSymbol.origin.asPublicOrigin()

    override val containingDeclaration: CaSymbol?
        get() = analysisSession.findContainingDeclarationSymbol(psi)

    override val visibility: CaSymbolVisibility
        get() = CaSymbolVisibility.LOCAL

    override val isVisibilityExplicit: Boolean
        get() = false

    override val modality: CaSymbolModality?
        get() = CaSymbolModality.FINAL

    override val isModalityExplicit: Boolean
        get() = false

    override val location: CaSymbolLocation
        get() = CaSymbolLocation.LOCAL

    override fun createPointer(): CaSymbolPointer<CaAnnotatedSymbol> = withValidityAssertion {
        val owner = containingDeclaration
        if (owner == null) {
            val sourcePsi = psi ?: error("Source-only type parameter `${name}` is missing PSI")
            return@withValidityAssertion CaCfirSourceTypeParameterSymbolPointer(sourcePsi)
        }

        val parameterIndex = stableParameterIndex
            ?: error("Type parameter `${name}` is missing a stable owner index")
        val ownerPointer: CaSymbolPointer<CaSymbol> = owner.createPointer()
        CaCfirTypeParameterSymbolPointer(ownerPointer, name, parameterIndex)
    }
}

internal class CaCfirScriptSymbolImpl(
    internal val scriptPsi: CjScript,
    private val scriptFileSymbol: CaFileSymbol?,
    private val analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirSymbolBase(containingModule, token), CaScriptSymbol, CaNamedSymbol {
    override val annotations: CaAnnotationList
        get() = withValidityAssertion {
            analysisSession.renderAnnotations(scriptPsi).asCaAnnotationList(token)
        }

    override val origin: CaSymbolOrigin
        get() = if (scriptPsi.containingCjFile.isCompiled) CaSymbolOrigin.LIBRARY else CaSymbolOrigin.SOURCE

    override val location: CaSymbolLocation
        get() = CaSymbolLocation.TOP_LEVEL

    override val psi: PsiElement
        get() = scriptPsi

    override val containingDeclaration: CaSymbol?
        get() = scriptFileSymbol

    override val visibility: CaSymbolVisibility
        get() = CaSymbolVisibility.PUBLIC

    override val isVisibilityExplicit: Boolean
        get() = false

    override val modality: CaSymbolModality?
        get() = CaSymbolModality.FINAL

    override val isModalityExplicit: Boolean
        get() = false

    override val name: Name
        get() = scriptPsi.nameAsSafeName

    override val fileSymbol: CaFileSymbol?
        get() = scriptFileSymbol

    override fun createPointer(): CaSymbolPointer<CaAnnotatedSymbol> = withValidityAssertion {
        CaCfirScriptSymbolPointer(scriptPsi)
    }
}

internal typealias CaCfirCallableSymbolBase<T> = CaCfirCallableSymbolSupport<T>

internal typealias CaCfirFunctionSymbolBase<T> = CaCfirFunctionSymbolSupport<T>

internal typealias CaCfirVariableSymbolBase<T> = CaCfirVariableSymbolSupport<T>

internal typealias CaCfirLocalVariableSymbolBase<T> = CaCfirLocalVariableSymbolSupport<T>

/**
 * callable 公共语义在 CFIR 侧的复用 mixin。
 *
 * 这里对齐 Kotlin FIR 的实现方式：public 叶子类直接继承 analysis-api 暴露的抽象叶子类型，
 * 后端共享逻辑则通过 mixin 提供，避免“内部基类 + public 抽象类”形成双 class 继承冲突。
 */
internal interface CaCfirCallableSymbolSupport<T : CfirCallableSymbol<*>> : CaCfirDeclarationBackedSymbolMixin<T> {
    val callableIdImpl: org.cangnova.cangjie.name.CallableId?
        get() {
            val callableDeclaration = backingSymbol.cfir as? CfirCallableDeclaration
            return backingSymbol.callableId.takeUnless { callableDeclaration?.isLocal == true }
        }

    val receiverTypeImpl: CaType?
        get() {
            val callablePsi = psi as? org.cangnova.cangjie.psi.CjCallableDeclaration ?: return null
            if (callablePsi.getStrictParentOfType<org.cangnova.cangjie.psi.CjExtend>() == null) return null
            return (backingSymbol.cfir as? CfirCallableDeclaration)?.dispatchReceiverType?.asCaType(analysisSession)
        }

    val returnTypeImpl: CaType
        get() = analysisSession.queryCallableReturnType(backingSymbol)?.asCaType(analysisSession)
            ?: error("Cannot build return type for `${backingSymbol::class.simpleName}`")

    val locationImpl: CaSymbolLocation
        get() = analysisSession.locationForDeclaration(this)
}

/**
 * 函数族 public 叶子实现共享的 CFIR mixin。
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
            ?.map { typeParameter -> analysisSession.createTypeParameterSymbol(typeParameter.symbol) }
            .orEmpty()

    val valueParametersImpl: List<CaValueParameterSymbol>
        get() = (backingSymbol.cfir as? org.cangnova.cangjie.cfir.declarations.CfirFunction)
            ?.valueParameters
            ?.mapIndexed { index, parameter ->
                analysisSession.createValueParameterSymbol(
                    ownerSymbol = this as CaValueParameterOwnerSymbol,
                    parameter = parameter,
                    parameterIndex = index,
                )
            }
            .orEmpty()
}

/**
 * callable 恢复逻辑严格落在专用 pointer 类中。
 *
 * 这里仅负责根据 public cache key 选择对应 pointer，不再保留统一 restore-key 协议。
 */
private fun <S : CaCallableSymbol> CaSymbol.createStableCallablePointer(
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
 * 变量族 public 叶子实现共享的 CFIR mixin。
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
    val localCallableIdImpl: org.cangnova.cangjie.name.CallableId?
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
 * 属性访问器在公开 API 中是函数族叶子类型，但其 owning property / 默认实现语义固定。
 */
internal interface CaCfirPropertyAccessorSymbolSupport<T : CfirCallableSymbol<*>> : CaCfirFunctionSymbolSupport<T> {
    val accessorCallableIdImpl: org.cangnova.cangjie.name.CallableId?
        get() = null

    val owningPropertyImpl: CaPropertySymbol
        get() = (psi as? org.cangnova.cangjie.psi.CjPropertyAccessor)?.property?.let { propertyPsi ->
            analysisSession.getPublicSymbolByPsi<CaPropertySymbol>(propertyPsi)
        } ?: error("Property accessor requires owning property")

    val isDefaultImpl: Boolean
        get() = (psi as? org.cangnova.cangjie.psi.CjPropertyAccessor)?.hasBody() == false
}

internal open class CaCfirNamedFunctionSymbolImpl(
    final override val backingSymbol: CfirCallableSymbol<*>,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol(),
    CaCfirNamedFunctionSymbolSupport<CfirCallableSymbol<*>> {
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

internal class CaCfirMainFunctionSymbolImpl(
    final override val backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirMainFunctionSymbol,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : CaMainFunctionSymbol(), CaCfirNamedFunctionSymbolSupport<org.cangnova.cangjie.cfir.symbols.CfirMainFunctionSymbol> {
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

internal class CaCfirMacroSymbolImpl(
    final override val backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirMacroDeclarationSymbol,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : CaMacroSymbol(), CaCfirNamedFunctionSymbolSupport<org.cangnova.cangjie.cfir.symbols.CfirMacroDeclarationSymbol> {
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

internal class CaCfirAnonymousFunctionSymbolImpl(
    final override val backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirAnonymousFunctionSymbol,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : CaAnonymousFunctionSymbol(), CaCfirFunctionSymbolSupport<org.cangnova.cangjie.cfir.symbols.CfirAnonymousFunctionSymbol> {
    override val annotations: CaAnnotationList
        get() = withValidityAssertion {
            analysisSession.renderAnnotations(this).asCaAnnotationList(token)
        }

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

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = null

    override val visibility: CaSymbolVisibility
        get() = CaSymbolVisibility.LOCAL

    override val isVisibilityExplicit: Boolean
        get() = false

    override val modality: CaSymbolModality?
        get() = CaSymbolModality.FINAL

    override val isModalityExplicit: Boolean
        get() = false
}

internal class CaCfirConstructorSymbolImpl(
    final override val backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol,
    val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : CaConstructorSymbol(),
    CaCfirBackedSymbol<org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol>,
    CaCfirSymbolMixin {
    /**
     * 构造器在公开 Analysis API 中把 `modality` 固定为 `FINAL`。
     *
     * 因此这里不能复用通用 `CaCfirFunctionSymbolSupport`：
     * 那套 mixin 会继续从 declaration status 派生 modality，
     * 与 `CaConstructorSymbol` 已经固定下来的公开语义发生冲突。
     */
    private val constructorStatus: CfirDeclarationStatus?
        get() = (backingSymbol.cfir as? CfirMemberDeclaration)?.status

    override val annotations: CaAnnotationList
        get() = withValidityAssertion {
            analysisSession.renderAnnotations(this).asCaAnnotationList(token)
        }

    override val psi: PsiElement?
        get() = analysisSession.lookupSourcePsi(backingSymbol)

    override val origin: CaSymbolOrigin
        get() = backingSymbol.origin.asPublicOrigin()

    override val containingDeclaration: CaSymbol?
        get() = analysisSession.findContainingDeclarationSymbol(psi)

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() {
            val callableDeclaration = backingSymbol.cfir as? CfirCallableDeclaration
            return backingSymbol.callableId.takeUnless { callableDeclaration?.isLocal == true }
        }

    override val receiverType: CaType?
        get() {
            val callablePsi = psi as? org.cangnova.cangjie.psi.CjCallableDeclaration ?: return null
            if (callablePsi.getStrictParentOfType<org.cangnova.cangjie.psi.CjExtend>() == null) return null
            return (backingSymbol.cfir as? CfirCallableDeclaration)?.dispatchReceiverType?.asCaType(analysisSession)
        }

    override val returnType: CaType
        get() = analysisSession.queryCallableReturnType(backingSymbol)?.asCaType(analysisSession)
            ?: error("Cannot build return type for `${backingSymbol::class.simpleName}`")

    override val visibility: CaSymbolVisibility
        get() = constructorStatus?.visibility?.asPublicVisibility() ?: CaSymbolVisibility.PUBLIC

    override val isVisibilityExplicit: Boolean
        get() = constructorStatus?.isVisibilityExplicit == true

    override val isModalityExplicit: Boolean
        get() = false

    override val location: CaSymbolLocation
        get() = analysisSession.locationForDeclaration(this)

    override fun createPointer(): CaSymbolPointer<CaConstructorSymbol> = withValidityAssertion {
        createStableCallablePointer(CaConstructorSymbol::class.java)
    }

    override val isStatic: Boolean
        get() = constructorStatus?.isStatic == true

    override val isConst: Boolean
        get() = constructorStatus?.isConst == true

    override val isMutating: Boolean
        get() = constructorStatus?.isMut == true

    override val isOverride: Boolean
        get() = constructorStatus?.isOverride == true

    override val isOperator: Boolean
        get() = constructorStatus?.isOperator == true

    override val isUnsafe: Boolean
        get() = constructorStatus?.isUnsafe == true

    override val isForeign: Boolean
        get() = constructorStatus?.isForeign == true

    override val typeParameters: List<CaTypeParameterSymbol>
        get() = (backingSymbol.cfir as? CfirCallableDeclaration)
            ?.typeParameters
            ?.filterIsInstance<CfirTypeParameter>()
            ?.map { typeParameter -> analysisSession.createTypeParameterSymbol(typeParameter.symbol) }
            .orEmpty()

    override val valueParameters: List<CaValueParameterSymbol>
        get() = (backingSymbol.cfir as? org.cangnova.cangjie.cfir.declarations.CfirFunction)
            ?.valueParameters
            ?.mapIndexed { index, parameter ->
                analysisSession.createValueParameterSymbol(
                    ownerSymbol = this,
                    parameter = parameter,
                    parameterIndex = index,
                )
            }
            .orEmpty()

    override val isPrimary: Boolean
        get() = psi is org.cangnova.cangjie.psi.CjPrimaryConstructor

    override val containingClassId: ClassId?
        get() = (psi as? org.cangnova.cangjie.psi.CjConstructor<*>)?.getContainingTypeStatement()?.getClassId()
}

internal class CaCfirFinalizerSymbolImpl(
    final override val backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirFinalizerSymbol,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : CaFinalizerSymbol(), CaCfirFunctionSymbolSupport<org.cangnova.cangjie.cfir.symbols.CfirFinalizerSymbol> {
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

    override val containingClassId: ClassId?
        get() = (psi as? org.cangnova.cangjie.psi.CjFinalizer)?.getContainingTypeStatement()?.getClassId()
}

internal typealias CaCfirPropertyAccessorSymbolBase = CaCfirPropertyAccessorSymbolSupport<org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>>

internal class CaCfirPropertyGetterSymbolImpl(
    final override val backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : CaPropertyGetterSymbol(), CaCfirPropertyAccessorSymbolSupport<org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>> {
    override val annotations: CaAnnotationList
        get() = withValidityAssertion {
            analysisSession.renderAnnotations(this).asCaAnnotationList(token)
        }

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = accessorCallableIdImpl

    override val receiverType: CaType?
        get() = receiverTypeImpl

    override val returnType: CaType
        get() = returnTypeImpl

    override val location: CaSymbolLocation
        get() = locationImpl

    override fun createPointer(): CaSymbolPointer<CaFunctionSymbol> = withValidityAssertion {
        CaCfirPropertyGetterSymbolPointer(owningProperty.createPointer())
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

    override val owningProperty: CaPropertySymbol
        get() = owningPropertyImpl

    override val isDefault: Boolean
        get() = isDefaultImpl

    override val isGetter: Boolean
        get() = true
}

internal class CaCfirPropertySetterSymbolImpl(
    final override val backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : CaPropertySetterSymbol(), CaCfirPropertyAccessorSymbolSupport<org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>> {
    override val annotations: CaAnnotationList
        get() = withValidityAssertion {
            analysisSession.renderAnnotations(this).asCaAnnotationList(token)
        }

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = accessorCallableIdImpl

    override val receiverType: CaType?
        get() = receiverTypeImpl

    override val returnType: CaType
        get() = returnTypeImpl

    override val location: CaSymbolLocation
        get() = locationImpl

    override fun createPointer(): CaSymbolPointer<CaFunctionSymbol> = withValidityAssertion {
        CaCfirPropertySetterSymbolPointer(owningProperty.createPointer())
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

    override val owningProperty: CaPropertySymbol
        get() = owningPropertyImpl

    override val isDefault: Boolean
        get() = isDefaultImpl

    override val isGetter: Boolean
        get() = false

    override val parameter: CaValueParameterSymbol
        get() = valueParameters.single()
}

internal class CaCfirPropertySymbolImpl(
    final override val backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : CaPropertySymbol(),
    CaCfirVariableSymbolSupport<org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol>,
    CaTypeParameterOwnerSymbol,
    CaDeclarationContainerSymbol {
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

    override fun createPointer(): CaSymbolPointer<CaPropertySymbol> = withValidityAssertion {
        createStableCallablePointer(CaPropertySymbol::class.java)
    }

    override val isLet: Boolean
        get() = true

    override val isStatic: Boolean
        get() = status?.isStatic == true

    override val isConst: Boolean
        get() = status?.isConst == true

    override val isMutating: Boolean
        get() = status?.isMut == true

    override val isOverride: Boolean
        get() = status?.isOverride == true

    override val isUnsafe: Boolean
        get() = status?.isUnsafe == true

    override val isForeign: Boolean
        get() = status?.isForeign == true

    override val typeParameters: List<CaTypeParameterSymbol>
        get() = (backingSymbol.cfir as? CfirProperty)
            ?.typeParameters
            ?.map { typeParameter -> analysisSession.createTypeParameterSymbol(typeParameter.symbol) }
            .orEmpty()

    override val getter: CaPropertyGetterSymbol?
        get() = (backingSymbol.cfir as? CfirProperty)
            ?.getter
            ?.symbol
            ?.let { getterSymbol ->
                analysisSession.createPropertyAccessorSymbol(
                    backingSymbol = getterSymbol,
                    ownerSymbol = this,
                    kind = CaCfirPropertyAccessorKind.GETTER,
                ) as CaPropertyGetterSymbol
            }

    override val setter: CaPropertySetterSymbol?
        get() = (backingSymbol.cfir as? CfirProperty)
            ?.setter
            ?.symbol
            ?.let { setterSymbol ->
                analysisSession.createPropertyAccessorSymbol(
                    backingSymbol = setterSymbol,
                    ownerSymbol = this,
                    kind = CaCfirPropertyAccessorKind.SETTER,
                ) as CaPropertySetterSymbol
            }

    override val name: Name
        get() = nameImpl
}

internal class CaCfirFieldSymbolImpl(
    final override val backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : CaFieldSymbol(), CaCfirVariableSymbolSupport<org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol> {
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

    override fun createPointer(): CaSymbolPointer<CaCallableSymbol> = withValidityAssertion {
        createStableCallablePointer(CaCallableSymbol::class.java)
    }

    override val isLet: Boolean
        get() = !(backingSymbol.cfir as org.cangnova.cangjie.cfir.declarations.CfirFieldVariable).isVar

    override val isStatic: Boolean
        get() = status?.isStatic == true

    override val isConst: Boolean
        get() = status?.isConst == true

    override val name: Name
        get() = nameImpl
}

internal open class CaCfirLocalVariableSymbolImpl(
    final override val backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : org.cangnova.cangjie.analysis.api.symbols.CaLocalVariableSymbol(),
    CaCfirLocalVariableSymbolSupport<org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>> {
    override val annotations: CaAnnotationList
        get() = withValidityAssertion {
            analysisSession.renderAnnotations(this).asCaAnnotationList(token)
        }

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = localCallableIdImpl

    override val receiverType: CaType?
        get() = receiverTypeImpl

    override val returnType: CaType
        get() = returnTypeImpl

    override val visibility: CaSymbolVisibility
        get() = localVisibilityImpl

    override val isVisibilityExplicit: Boolean
        get() = isVisibilityExplicitImpl

    override val modality: CaSymbolModality?
        get() = modalityImpl

    override val isModalityExplicit: Boolean
        get() = isModalityExplicitImpl

    override val location: CaSymbolLocation
        get() = localLocationImpl

    override fun createPointer(): CaSymbolPointer<CaCallableSymbol> = withValidityAssertion {
        createStableCallablePointer(CaCallableSymbol::class.java)
    }

    override val isLet: Boolean
        get() = when (val currentDeclaration = backingSymbol.cfir) {
            is org.cangnova.cangjie.cfir.declarations.CfirPatternVariable -> !currentDeclaration.isVar
            is org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable -> !currentDeclaration.isVar
            else -> true
        }

    override val name: Name
        get() = nameImpl
}

internal class CaCfirPatternVariableSymbolImpl(
    final override val backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : CaPatternVariableSymbol(), CaCfirLocalVariableSymbolSupport<org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol> {
    override val annotations: CaAnnotationList
        get() = withValidityAssertion {
            analysisSession.renderAnnotations(this).asCaAnnotationList(token)
        }

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = localCallableIdImpl

    override val receiverType: CaType?
        get() = receiverTypeImpl

    override val returnType: CaType
        get() = returnTypeImpl

    override val visibility: CaSymbolVisibility
        get() = localVisibilityImpl

    override val isVisibilityExplicit: Boolean
        get() = isVisibilityExplicitImpl

    override val modality: CaSymbolModality?
        get() = modalityImpl

    override val isModalityExplicit: Boolean
        get() = isModalityExplicitImpl

    override val location: CaSymbolLocation
        get() = localLocationImpl

    override fun createPointer(): CaSymbolPointer<CaCallableSymbol> = withValidityAssertion {
        createStableCallablePointer(CaCallableSymbol::class.java)
    }

    override val isLet: Boolean
        get() = !(backingSymbol.cfir as org.cangnova.cangjie.cfir.declarations.CfirPatternVariable).isVar

    override val name: Name
        get() = nameImpl
}

internal class CaCfirPatternBindingSymbolImpl(
    final override val backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : CaPatternBindingSymbol(), CaCfirLocalVariableSymbolSupport<org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol> {
    override val annotations: CaAnnotationList
        get() = withValidityAssertion {
            analysisSession.renderAnnotations(this).asCaAnnotationList(token)
        }

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = localCallableIdImpl

    override val receiverType: CaType?
        get() = receiverTypeImpl

    override val returnType: CaType
        get() = returnTypeImpl

    override val visibility: CaSymbolVisibility
        get() = localVisibilityImpl

    override val isVisibilityExplicit: Boolean
        get() = isVisibilityExplicitImpl

    override val modality: CaSymbolModality?
        get() = modalityImpl

    override val isModalityExplicit: Boolean
        get() = isModalityExplicitImpl

    override val location: CaSymbolLocation
        get() = localLocationImpl

    override fun createPointer(): CaSymbolPointer<CaCallableSymbol> = withValidityAssertion {
        createStableCallablePointer(CaCallableSymbol::class.java)
    }

    override val isLet: Boolean
        get() = !(backingSymbol.cfir as org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable).isVar

    override val name: Name
        get() = nameImpl
}

internal class CaCfirValueParameterSymbolImpl(
    final override val backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
    internal val ownerSymbol: CaValueParameterOwnerSymbol? = null,
    internal val stableParameterIndex: Int? = null,
    private val parameterPsi: org.cangnova.cangjie.psi.CjParameter? = null,
) : CaValueParameterSymbol(), CaCfirVariableSymbolSupport<org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol> {
    private val parameterDeclaration: CfirValueParameter
        get() = backingSymbol.cfir

    override val annotations: CaAnnotationList
        get() = withValidityAssertion {
            analysisSession.renderAnnotations(this).asCaAnnotationList(token)
        }

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = null

    override val receiverType: CaType?
        get() = receiverTypeImpl

    override val returnType: CaType
        get() = returnTypeImpl

    override val location: CaSymbolLocation
        get() = CaSymbolLocation.LOCAL

    override fun createPointer(): CaSymbolPointer<CaCallableSymbol> = withValidityAssertion {
        val owner = (ownerSymbol ?: containingDeclaration) as? CaSymbol
            ?: error("Value parameter `${name}` is missing pointer-restorable owner")
        val parameterIndex = stableParameterIndex
            ?: error("Value parameter `${name}` is missing stable parameter index")
        CaCfirValueParameterSymbolPointer(
            ownerPointer = owner.createPointer(),
            parameterName = name,
            parameterIndex = parameterIndex,
        )
    }

    override val name: Name
        get() = backingSymbol.name

    override val isLet: Boolean
        get() = !parameterDeclaration.isVar

    override val isNamed: Boolean
        get() = parameterDeclaration.isNamed

    override val isVararg: Boolean
        get() = resolvedParameterPsi?.isVarArg == true

    override val hasDefaultValue: Boolean
        get() = parameterDeclaration.defaultValue != null || resolvedParameterPsi?.defaultValue != null

    override val visibility: CaSymbolVisibility
        get() = CaSymbolVisibility.LOCAL

    override val isVisibilityExplicit: Boolean
        get() = false

    override val modality: CaSymbolModality?
        get() = CaSymbolModality.FINAL

    override val isModalityExplicit: Boolean
        get() = false

    override val containingDeclaration: CaSymbol?
        get() = ownerSymbol ?: analysisSession.findContainingDeclarationSymbol(psi)

    private val resolvedParameterPsi: org.cangnova.cangjie.psi.CjParameter?
        get() = parameterDeclaration.source?.psi as? org.cangnova.cangjie.psi.CjParameter
            ?: parameterPsi
            ?: psi as? org.cangnova.cangjie.psi.CjParameter
}

internal class CaCfirEnumConstructorSymbolImpl(
    final override val backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : CaEnumConstructorSymbol(),
    CaCfirCallableSymbolSupport<org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol>,
    CaNamedSymbol {
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

    override fun createPointer(): CaSymbolPointer<CaCallableSymbol> = withValidityAssertion {
        createStableCallablePointer(CaCallableSymbol::class.java)
    }

    override val name: Name
        get() = backingSymbol.name

    override val containingEnumClassId: ClassId?
        get() = analysisSession.cfirSession.symbolProvider.getEnumConstructorOwnerClassId(backingSymbol)

    override val payloadTypes: List<CaType>
        get() = backingSymbol.cfir.payloadParameterTypesOrEmpty().map { type -> type.asCaType(analysisSession) }
}

internal fun CaCfirSession.locationForDeclaration(symbol: CaDeclarationSymbol): CaSymbolLocation = when (symbol) {
    is CaPropertyGetterSymbol,
    is CaPropertySetterSymbol,
    -> CaSymbolLocation.PROPERTY

    is CaAnonymousFunctionSymbol,
    is CaParameterSymbol,
    is CaPatternVariableSymbol,
    is CaPatternBindingSymbol,
    is CaTypeParameterSymbol,
    is org.cangnova.cangjie.analysis.api.symbols.CaLocalVariableSymbol,
    -> CaSymbolLocation.LOCAL

    else -> when (symbol.containingDeclaration) {
        is CaExtendSymbol -> CaSymbolLocation.EXTEND
        is CaClassSymbol -> CaSymbolLocation.CLASS
        is CaPropertySymbol -> CaSymbolLocation.PROPERTY
        else -> CaSymbolLocation.TOP_LEVEL
    }
}

internal fun <S : CaSymbol> CaCfirSession.getPublicSymbolByPsi(
    psi: PsiElement,
    symbolType: Class<S>,
): S? {
    val matches = lookupSymbolsByPsi(psi)
        .map(::getPublicSymbol)
        .filter { symbol -> symbolType.isInstance(symbol) }
        .map(symbolType::cast)
    return matches.singleOrNull()
}

internal inline fun <reified S : CaSymbol> CaCfirSession.getPublicSymbolByPsi(psi: PsiElement): S? =
    getPublicSymbolByPsi(psi, S::class.java)

internal fun CaCfirSession.findContainingDeclarationSymbol(psi: PsiElement?): CaSymbol? {
    var current = psi?.parent
    while (current != null) {
        val container = when (current) {
            is org.cangnova.cangjie.psi.CjPropertyAccessor -> getPublicSymbolByPsi<org.cangnova.cangjie.analysis.api.symbols.CaPropertyAccessorSymbol>(current)
            is org.cangnova.cangjie.psi.CjProperty -> getPublicSymbolByPsi<CaPropertySymbol>(current)
            is org.cangnova.cangjie.psi.CjExtend -> getPublicSymbolByPsi<CaExtendSymbol>(current)
            is org.cangnova.cangjie.psi.CjTypeAlias -> getPublicSymbolByPsi<CaTypeAliasSymbol>(current)
            is CjTypeStatement -> getPublicSymbolByPsi<CaClassSymbol>(current)
            is org.cangnova.cangjie.psi.CjNamedFunction -> getPublicSymbolByPsi<org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol>(current)
            is org.cangnova.cangjie.psi.CjFunctionLiteral -> getPublicSymbolByPsi<CaAnonymousFunctionSymbol>(current)
            is org.cangnova.cangjie.psi.CjConstructor<*> -> getPublicSymbolByPsi<CaConstructorSymbol>(current)
            is org.cangnova.cangjie.psi.CjFinalizer -> getPublicSymbolByPsi<CaFinalizerSymbol>(current)
            is org.cangnova.cangjie.psi.CjMacroDeclaration -> getPublicSymbolByPsi<CaMacroSymbol>(current)
            is CjScript -> createScriptSymbol(current)
            is CjFile -> createFileSymbol(current)
            else -> null
        }
        if (container != null) return container
        current = current.parent
    }
    return null
}

internal fun CfirDeclarationOrigin.asPublicOrigin(): CaSymbolOrigin = when (this) {
    CfirDeclarationOrigin.Source -> CaSymbolOrigin.SOURCE
    CfirDeclarationOrigin.Library -> CaSymbolOrigin.LIBRARY
    CfirDeclarationOrigin.ImplicitDefault -> CaSymbolOrigin.IMPLICIT_DEFAULT
    CfirDeclarationOrigin.GenericInstantiation -> CaSymbolOrigin.GENERIC_INSTANTIATION
    CfirDeclarationOrigin.Extension -> CaSymbolOrigin.EXTENSION
    CfirDeclarationOrigin.SamConstructor -> CaSymbolOrigin.SAM_CONSTRUCTOR
    CfirDeclarationOrigin.SubstitutionOverride.DeclarationSite -> CaSymbolOrigin.SUBSTITUTION_OVERRIDE_DECLARATION_SITE
    CfirDeclarationOrigin.SubstitutionOverride.CallSite -> CaSymbolOrigin.SUBSTITUTION_OVERRIDE_CALL_SITE
    is CfirDeclarationOrigin.Synthetic -> CaSymbolOrigin.SYNTHETIC
}

internal fun Visibility.asPublicVisibility(): CaSymbolVisibility = when (this) {
    Visibilities.Private -> CaSymbolVisibility.PRIVATE
    Visibilities.PrivateToThis -> CaSymbolVisibility.PRIVATE_TO_THIS
    Visibilities.Protected -> CaSymbolVisibility.PROTECTED
    Visibilities.Internal -> CaSymbolVisibility.INTERNAL
    Visibilities.Public -> CaSymbolVisibility.PUBLIC
    Visibilities.Local -> CaSymbolVisibility.LOCAL
    else -> CaSymbolVisibility.UNKNOWN
}

internal fun Modality.asPublicModality(): CaSymbolModality = when (this) {
    Modality.FINAL -> CaSymbolModality.FINAL
    Modality.SEALED -> CaSymbolModality.SEALED
    Modality.OPEN -> CaSymbolModality.OPEN
    Modality.ABSTRACT -> CaSymbolModality.ABSTRACT
}

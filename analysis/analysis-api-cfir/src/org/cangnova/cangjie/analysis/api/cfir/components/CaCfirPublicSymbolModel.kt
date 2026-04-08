package org.cangnova.cangjie.analysis.api.cfir.components

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.symbols.CaAnonymousFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassKind
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumEntrySymbol
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
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjScript
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.source.psi

internal sealed class CaCfirSymbolBase(
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : CaSymbol

internal interface CaCfirBackedSymbol<T : CfirSymbol<*>> {
    val backingSymbol: T
}

internal abstract class CaCfirDeclarationBackedSymbol<T : CfirSymbol<*>>(
    final override val backingSymbol: T,
    internal val analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirSymbolBase(containingModule, token), CaDeclarationSymbol, CaCfirBackedSymbol<T> {
    final override val psi: PsiElement?
        get() = analysisSession.lookupSourcePsi(backingSymbol)

    protected val declaration: CfirDeclaration
        get() = backingSymbol.cfir as CfirDeclaration

    protected open val status: CfirDeclarationStatus?
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
    override val origin: CaSymbolOrigin
        get() = CaSymbolOrigin.UNKNOWN

    override val location: CaSymbolLocation
        get() = CaSymbolLocation.TOP_LEVEL

    override val name: Name
        get() = fqName.shortNameOrSpecial()
}

internal class CaCfirFileSymbolImpl(
    override val backingSymbol: CfirFileSymbol,
    override val file: CjFile,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirSymbolBase(containingModule, token), CaFileSymbol, CaNamedSymbol, CaCfirBackedSymbol<CfirFileSymbol> {
    override val origin: CaSymbolOrigin
        get() = backingSymbol.origin.asPublicOrigin()

    override val location: CaSymbolLocation
        get() = CaSymbolLocation.TOP_LEVEL

    override val packageFqName: FqName
        get() = file.packageFqName

    override val name: Name
        get() = Name.identifier(file.name)
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
        get() = analysisSession.queryClassLikeSuperTypes(backingSymbol).map { superType -> superType.asCaType(token) }
}

internal class CaCfirTypeAliasSymbolImpl(
    backingSymbol: CfirTypeAliasSymbol,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirClassLikeSymbolBase<CfirTypeAliasSymbol>(backingSymbol, analysisSession, containingModule, token), CaTypeAliasSymbol {
    override val expandedType: CaType
        get() = ((backingSymbol.cfir as CfirTypeAlias).expandedTypeRef.coneTypeOrNull?.asCaType(token))
            ?: error("Cannot build expanded type for `${backingSymbol.classId.asString()}`")
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
        get() = extendDeclaration.extendedTypeRef.coneTypeOrNull?.asCaType(token)
            ?: error("Cannot build extended type for extend `${extendId}`")

    override val superTypes: List<CaType>
        get() = extendDeclaration.superTypeRefs.mapNotNull { superTypeRef -> superTypeRef.coneTypeOrNull?.asCaType(token) }

    override val typeParameters: List<CaTypeParameterSymbol>
        get() = extendDeclaration.typeParameters.map { typeParameter -> analysisSession.createTypeParameterSymbol(typeParameter.symbol) }

    override val location: CaSymbolLocation
        get() = CaSymbolLocation.TOP_LEVEL

    override val containingDeclaration: CaSymbol?
        get() = analysisSession.findContainingDeclarationSymbol(extendPsi ?: psi)
}

internal class CaCfirTypeParameterSymbolImpl(
    backingSymbol: CfirTypeParameterSymbol,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirClassifierBackedSymbol<CfirTypeParameterSymbol>(backingSymbol, analysisSession, containingModule, token),
    CaTypeParameterSymbol,
    CaNamedSymbol {
    override val name: Name
        get() = backingSymbol.name

    override val upperBounds: List<CaType>
        get() = backingSymbol.cfir.bounds.mapNotNull { bound -> bound.coneTypeOrNull?.asCaType(token) }

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
}

internal class CaCfirScriptSymbolImpl(
    internal val scriptPsi: CjScript,
    private val scriptFileSymbol: CaFileSymbol?,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirSymbolBase(containingModule, token), CaScriptSymbol, CaNamedSymbol {
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
}

internal abstract class CaCfirCallableSymbolBase<T : org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>>(
    backingSymbol: T,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirDeclarationBackedSymbol<T>(backingSymbol, analysisSession, containingModule, token), CaCallableSymbol {
    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() {
            val callableDeclaration = backingSymbol.cfir as? CfirCallableDeclaration
            return backingSymbol.callableId.takeUnless { callableDeclaration?.isLocal == true }
        }

    override val receiverType: CaType?
        get() {
            val callablePsi = psi as? org.cangnova.cangjie.psi.CjCallableDeclaration ?: return null
            if (callablePsi.getStrictParentOfType<org.cangnova.cangjie.psi.CjExtend>() == null) return null
            return (backingSymbol.cfir as? CfirCallableDeclaration)?.dispatchReceiverType?.asCaType(token)
        }

    override val returnType: CaType
        get() = analysisSession.queryCallableReturnType(backingSymbol)?.asCaType(token)
            ?: error("Cannot build return type for `${backingSymbol::class.simpleName}`")

    override val location: CaSymbolLocation
        get() = analysisSession.locationForDeclaration(this)
}

internal abstract class CaCfirFunctionSymbolBase<T : org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>>(
    backingSymbol: T,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirCallableSymbolBase<T>(backingSymbol, analysisSession, containingModule, token),
    CaFunctionSymbol,
    CaTypeParameterOwnerSymbol,
    CaValueParameterOwnerSymbol {
    override val isStatic: Boolean
        get() = status?.isStatic == true

    override val isConst: Boolean
        get() = status?.isConst == true

    override val isMutating: Boolean
        get() = status?.isMut == true

    override val isOverride: Boolean
        get() = status?.isOverride == true

    override val isOperator: Boolean
        get() = status?.isOperator == true

    override val isUnsafe: Boolean
        get() = status?.isUnsafe == true

    override val isForeign: Boolean
        get() = status?.isForeign == true

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
}

internal abstract class CaCfirVariableSymbolBase<T : org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>>(
    backingSymbol: T,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirCallableSymbolBase<T>(backingSymbol, analysisSession, containingModule, token),
    CaVariableSymbol,
    CaNamedSymbol {
    override val name: Name
        get() = backingSymbol.name
}

internal abstract class CaCfirLocalVariableSymbolBase<T : org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>>(
    backingSymbol: T,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirVariableSymbolBase<T>(backingSymbol, analysisSession, containingModule, token) {
    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = null
}

internal open class CaCfirNamedFunctionSymbolImpl(
    backingSymbol: CfirCallableSymbol<*>,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirFunctionSymbolBase<CfirCallableSymbol<*>>(backingSymbol, analysisSession, containingModule, token),
    org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol,
    CaNamedSymbol {
    override val name: Name
        get() = backingSymbol.name
}

internal class CaCfirMainFunctionSymbolImpl(
    backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirMainFunctionSymbol,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirNamedFunctionSymbolImpl(backingSymbol, analysisSession, containingModule, token), CaMainFunctionSymbol

internal class CaCfirMacroSymbolImpl(
    backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirMacroDeclarationSymbol,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirNamedFunctionSymbolImpl(backingSymbol, analysisSession, containingModule, token), CaMacroSymbol

internal class CaCfirAnonymousFunctionSymbolImpl(
    backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirAnonymousFunctionSymbol,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirFunctionSymbolBase<org.cangnova.cangjie.cfir.symbols.CfirAnonymousFunctionSymbol>(backingSymbol, analysisSession, containingModule, token), CaAnonymousFunctionSymbol {
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
    backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirFunctionSymbolBase<org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol>(backingSymbol, analysisSession, containingModule, token), CaConstructorSymbol {
    override val isPrimary: Boolean
        get() = psi is org.cangnova.cangjie.psi.CjPrimaryConstructor

    override val containingClassId: ClassId?
        get() = (psi as? org.cangnova.cangjie.psi.CjConstructor<*>)?.getContainingTypeStatement()?.getClassId()
}

internal class CaCfirFinalizerSymbolImpl(
    backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirFinalizerSymbol,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirFunctionSymbolBase<org.cangnova.cangjie.cfir.symbols.CfirFinalizerSymbol>(backingSymbol, analysisSession, containingModule, token), CaFinalizerSymbol {
    override val containingClassId: ClassId?
        get() = (psi as? org.cangnova.cangjie.psi.CjFinalizer)?.getContainingTypeStatement()?.getClassId()
}

internal abstract class CaCfirPropertyAccessorSymbolBase(
    backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirFunctionSymbolBase<org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>>(backingSymbol, analysisSession, containingModule, token),
    org.cangnova.cangjie.analysis.api.symbols.CaPropertyAccessorSymbol {
    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = null

    override val owningProperty: CaPropertySymbol
        get() = (psi as? org.cangnova.cangjie.psi.CjPropertyAccessor)?.property?.let { propertyPsi ->
            analysisSession.getPublicSymbolByPsi<CaPropertySymbol>(propertyPsi)
        } ?: error("Property accessor requires owning property")

    override val isDefault: Boolean
        get() = (psi as? org.cangnova.cangjie.psi.CjPropertyAccessor)?.hasBody() == false
}

internal class CaCfirPropertyGetterSymbolImpl(
    backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirPropertyAccessorSymbolBase(backingSymbol, analysisSession, containingModule, token), CaPropertyGetterSymbol {
    override val isGetter: Boolean
        get() = true
}

internal class CaCfirPropertySetterSymbolImpl(
    backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirPropertyAccessorSymbolBase(backingSymbol, analysisSession, containingModule, token), CaPropertySetterSymbol {
    override val isGetter: Boolean
        get() = false

    override val parameter: CaValueParameterSymbol
        get() = valueParameters.single()
}

internal class CaCfirPropertySymbolImpl(
    backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirVariableSymbolBase<org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol>(backingSymbol, analysisSession, containingModule, token),
    CaPropertySymbol,
    CaTypeParameterOwnerSymbol,
    CaDeclarationContainerSymbol {
    override val isVal: Boolean
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
}

internal class CaCfirFieldSymbolImpl(
    backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirVariableSymbolBase<org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol>(backingSymbol, analysisSession, containingModule, token), CaFieldSymbol {
    override val isVal: Boolean
        get() = !(backingSymbol.cfir as org.cangnova.cangjie.cfir.declarations.CfirFieldVariable).isVar

    override val isStatic: Boolean
        get() = status?.isStatic == true

    override val isConst: Boolean
        get() = status?.isConst == true
}

internal open class CaCfirLocalVariableSymbolImpl(
    backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirLocalVariableSymbolBase<org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>>(backingSymbol, analysisSession, containingModule, token),
    org.cangnova.cangjie.analysis.api.symbols.CaLocalVariableSymbol {
    override val isVal: Boolean
        get() = when (val currentDeclaration = backingSymbol.cfir) {
            is org.cangnova.cangjie.cfir.declarations.CfirPatternVariable -> !currentDeclaration.isVar
            is org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable -> !currentDeclaration.isVar
            else -> true
        }

    override val visibility: CaSymbolVisibility
        get() = CaSymbolVisibility.LOCAL

    override val isVisibilityExplicit: Boolean
        get() = false

    override val modality: CaSymbolModality?
        get() = CaSymbolModality.FINAL

    override val isModalityExplicit: Boolean
        get() = false
}

internal class CaCfirPatternVariableSymbolImpl(
    backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirLocalVariableSymbolImpl(backingSymbol, analysisSession, containingModule, token), CaPatternVariableSymbol

internal class CaCfirPatternBindingSymbolImpl(
    backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirLocalVariableSymbolImpl(backingSymbol, analysisSession, containingModule, token), CaPatternBindingSymbol

internal class CaCfirValueParameterSymbolImpl(
    backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
    internal val ownerSymbol: CaValueParameterOwnerSymbol? = null,
    internal val stableParameterIndex: Int? = null,
    private val parameterPsi: org.cangnova.cangjie.psi.CjParameter? = null,
) : CaCfirVariableSymbolBase<org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol>(backingSymbol, analysisSession, containingModule, token),
    CaValueParameterSymbol,
    CaParameterSymbol {
    private val parameterDeclaration: CfirValueParameter
        get() = backingSymbol.cfir

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = null

    override val isVal: Boolean
        get() = !parameterDeclaration.isVar

    override val isNamed: Boolean
        get() = parameterDeclaration.isNamed

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

internal class CaCfirEnumEntrySymbolImpl(
    backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirVariableSymbolBase<org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol>(backingSymbol, analysisSession, containingModule, token), CaEnumEntrySymbol {
    override val isVal: Boolean
        get() = true
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

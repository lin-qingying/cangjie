package org.cangnova.cangjie.analysis.api.cfir.components

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.CaLibraryModule
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.CaCfirTopLevelPublicSymbolQueryValue
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledPsiProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaProjectStructureProvider
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertyGetterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySetterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaScriptSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeAliasSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaValueParameterOwnerSymbol
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirAnonymousFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirExtendSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFinalizerSymbol
import org.cangnova.cangjie.cfir.symbols.CfirMacroDeclarationSymbol
import org.cangnova.cangjie.cfir.symbols.CfirMainFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.buildExtendId
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjPropertyAccessor
import org.cangnova.cangjie.psi.CjScript
import org.cangnova.cangjie.source.psi

internal sealed interface CaCfirPublicSymbolCacheKey

internal data class CaCfirPackageSymbolCacheKey(
    val fqName: FqName,
) : CaCfirPublicSymbolCacheKey

internal data class CaCfirFileSymbolCacheKey(
    val file: CjFile,
) : CaCfirPublicSymbolCacheKey

internal data class CaCfirClassLikeSymbolCacheKey(
    val classId: ClassId,
) : CaCfirPublicSymbolCacheKey

internal data class CaCfirExtendSymbolCacheKey(
    val extendId: String,
) : CaCfirPublicSymbolCacheKey

internal data class CaCfirCallableSymbolCacheKey(
    val callableId: CallableId,
    val kind: CaCfirCallableSymbolKind,
) : CaCfirPublicSymbolCacheKey

internal data class CaCfirPropertyAccessorSymbolCacheKey(
    val ownerKey: CaCfirPublicSymbolCacheKey,
    val kind: CaCfirPropertyAccessorKind,
) : CaCfirPublicSymbolCacheKey

internal data class CaCfirValueParameterSymbolCacheKey(
    val ownerKey: CaCfirPublicSymbolCacheKey,
    val parameterIndex: Int,
    val parameterName: Name,
) : CaCfirPublicSymbolCacheKey

internal data class CaCfirTypeParameterSymbolCacheKey(
    val ownerKey: CaCfirPublicSymbolCacheKey,
    val parameterName: Name,
) : CaCfirPublicSymbolCacheKey

internal data class CaCfirPsiSymbolCacheKey(
    val psi: PsiElement,
    val kind: CaCfirPsiSymbolKind,
) : CaCfirPublicSymbolCacheKey

internal enum class CaCfirCallableSymbolKind {
    NAMED_FUNCTION,
    MAIN_FUNCTION,
    MACRO,
    FINALIZER,
    CONSTRUCTOR,
    PROPERTY,
    FIELD,
    PATTERN_VARIABLE,
    PATTERN_BINDING,
    ENUM_ENTRY,
}

internal enum class CaCfirPropertyAccessorKind {
    GETTER,
    SETTER,
}

internal enum class CaCfirPsiSymbolKind {
    SCRIPT,
    ANONYMOUS_FUNCTION,
    LOCAL_VARIABLE,
    PATTERN_VARIABLE,
    PATTERN_BINDING,
    TYPE_PARAMETER,
}

internal sealed interface CaCfirCompletionSymbolKey

internal data class CaCfirStableCompletionSymbolKey(
    val symbolKey: CaCfirPublicSymbolCacheKey,
) : CaCfirCompletionSymbolKey

internal data class CaCfirEphemeralCompletionSymbolKey(
    val symbol: CaSymbol,
) : CaCfirCompletionSymbolKey

internal fun CaCfirSession.getPublicSymbol(symbol: CfirSymbol<*>): CaSymbol = when (symbol) {
    is CfirClassLikeSymbol<*> -> createClassLikeSymbol(symbol)
    is CfirCallableSymbol<*> -> createCallableSymbol(symbol)
    is CfirTypeParameterSymbol -> createTypeParameterSymbol(symbol)
    is CfirFileSymbol -> createFilePublicSymbol(symbol)
    is CfirExtendSymbol -> createExtendSymbol(symbol)
    else -> error("Unsupported public symbol mapping for `${symbol::class.simpleName}`")
}

internal fun CaCfirSession.getPackagePublicSymbol(fqName: FqName): CaPackageSymbol? {
    if (!hasVisiblePackage(fqName)) return null
    return createPackageSymbol(fqName)
}

internal fun CaCfirSession.getClassLikePublicSymbol(classId: ClassId): CaClassLikeSymbol? {
    val symbol = lookupClassLikeSymbol(classId) ?: return null
    return createClassLikeSymbol(symbol)
}

internal fun CaCfirSession.getClassPublicSymbol(classId: ClassId): CaClassSymbol? =
    getClassLikePublicSymbol(classId) as? CaClassSymbol

internal fun CaCfirSession.getTypeAliasPublicSymbol(classId: ClassId): CaTypeAliasSymbol? =
    getClassLikePublicSymbol(classId) as? CaTypeAliasSymbol

internal fun CaCfirSession.createFilePublicSymbol(symbol: CfirFileSymbol): CaFileSymbol {
    val file = lookupContainingFile(symbol)
        ?: error("File symbol `${symbol}` is missing recoverable CjFile source")
    return createFileSymbol(file)
}

internal fun CaCfirSession.getOrCreateTopLevelPublicSymbols(
    packageFqName: FqName,
    name: Name,
): CaCfirTopLevelPublicSymbolQueryValue {
    return getOrCreateTopLevelSymbolQuery(packageFqName, name) {
        val queryResult = queryTopLevelSymbols(packageFqName, name)
        CaCfirTopLevelPublicSymbolQueryValue(
            classLikeSymbols = queryResult.classLikeSymbols.map(::createClassLikeSymbol),
            callableSymbols = queryResult.callableSymbols.map(::createCallableSymbol),
        )
    }
}

internal fun CaCfirSession.getTopLevelExtendPublicSymbols(packageFqName: FqName): List<CaExtendSymbol> =
    cfirSession.extendProviderOrNull
        ?.getExtendsInPackage(packageFqName)
        ?.map { extend -> createExtendSymbol(extend.symbol) }
        .orEmpty()

internal fun CaCfirSession.getExtendPublicSymbols(targetClassId: ClassId): List<CaExtendSymbol> =
    cfirSession.extendProviderOrNull
        ?.getExtendsForClass(targetClassId)
        ?.map { extend -> createExtendSymbol(extend.symbol) }
        .orEmpty()

internal fun CaCfirSession.createPackageSymbol(fqName: FqName): CaPackageSymbol =
    getOrCreatePublicSymbol(CaCfirPackageSymbolCacheKey(fqName)) {
        CaCfirPackageSymbolImpl(fqName, useSiteModule, token)
    }

internal fun CaCfirSession.createFileSymbol(file: CjFile): CaFileSymbol =
    getOrCreatePublicSymbol(CaCfirFileSymbolCacheKey(file)) {
        val fileSymbol = lookupFileSymbol(file) ?: error("Cannot build low-level file symbol for `${file.name}`")
        CaCfirFileSymbolImpl(fileSymbol, file, useSiteModule, token)
    }

internal fun CaCfirSession.createClassLikeSymbol(symbol: CfirClassLikeSymbol<*>): CaClassLikeSymbol =
    getOrCreatePublicSymbol(CaCfirClassLikeSymbolCacheKey(symbol.classId)) {
        when (symbol) {
            is CfirTypeAliasSymbol -> CaCfirTypeAliasSymbolImpl(symbol, this, useSiteModule, token)
            else -> CaCfirClassSymbolImpl(symbol, this, useSiteModule, token)
        }
    }

internal fun CaCfirSession.createExtendSymbol(symbol: CfirExtendSymbol): CaExtendSymbol {
    val identity = resolveExtendIdentity(symbol)
    return getOrCreatePublicSymbol(CaCfirExtendSymbolCacheKey(identity.extendId)) {
        CaCfirExtendSymbolImpl(symbol, identity.extendPsi, identity.extendId, identity.packageFqName, this, useSiteModule, token)
    }
}

internal fun CaCfirSession.createCallableSymbol(symbol: CfirCallableSymbol<*>): CaCallableSymbol {
    val cacheKey = symbol.publicSymbolCacheKeyOrNull(this)
    return if (cacheKey != null) {
        getOrCreatePublicSymbol(cacheKey) {
            createUncachedCallableSymbol(symbol)
        }
    } else {
        createUncachedCallableSymbol(symbol)
    }
}

internal fun CaCfirSession.createTypeParameterSymbol(symbol: CfirTypeParameterSymbol): CaTypeParameterSymbol {
    val ownerKey = symbol.publicTypeParameterOwnerKey(this)
    if (ownerKey == null) {
        val psi = symbol.cfir.source?.psi ?: error("Type parameter `${symbol.name}` requires source PSI")
        return getOrCreatePublicSymbol(CaCfirPsiSymbolCacheKey(psi, CaCfirPsiSymbolKind.TYPE_PARAMETER)) {
            CaCfirTypeParameterSymbolImpl(symbol, this, useSiteModule, token)
        }
    }
    return getOrCreatePublicSymbol(CaCfirTypeParameterSymbolCacheKey(ownerKey, symbol.name)) {
        CaCfirTypeParameterSymbolImpl(symbol, this, useSiteModule, token)
    }
}

internal fun CaCfirSession.createValueParameterSymbol(symbol: CfirValueParameter): CaValueParameterSymbol {
    return createValueParameterSymbol(symbol.symbol)
}

internal fun CaCfirSession.createValueParameterSymbol(
    ownerSymbol: CaValueParameterOwnerSymbol,
    parameter: CfirValueParameter,
    parameterIndex: Int,
): CaValueParameterSymbol {
    val ownerKey = ownerSymbol.publicSymbolCacheKeyOrNull()
    return if (ownerKey != null) {
        getOrCreatePublicSymbol(
            CaCfirValueParameterSymbolCacheKey(
                ownerKey = ownerKey,
                parameterIndex = parameterIndex,
                parameterName = parameter.name,
            ),
        ) {
            CaCfirValueParameterSymbolImpl(
                backingSymbol = parameter.symbol,
                analysisSession = this,
                containingModule = useSiteModule,
                token = token,
                ownerSymbol = ownerSymbol,
                stableParameterIndex = parameterIndex,
                parameterPsi = (ownerSymbol as? CaDeclarationSymbol)
                    ?.psi
                    ?.let { ownerPsi -> (ownerPsi as? org.cangnova.cangjie.psi.CjCallableDeclaration)?.valueParameters?.getOrNull(parameterIndex) },
            )
        }
    } else {
        createValueParameterSymbol(parameter.symbol)
    }
}

internal fun CaCfirSession.createValueParameterSymbol(symbol: CfirValueParameterSymbol): CaValueParameterSymbol {
    val psi = lookupSourcePsi(symbol)
    val parameterPsi = psi as? org.cangnova.cangjie.psi.CjParameter
    val ownerSymbol = parameterPsi
        ?.let(::findContainingDeclarationSymbol)
        as? CaValueParameterOwnerSymbol
    val parameterIndex = parameterPsi
        ?.let { currentParameter ->
            (currentParameter.parent as? org.cangnova.cangjie.psi.CjParameterList)
                ?.parameters
                ?.indexOf(currentParameter)
        }
    val cacheKey = psi?.let { CaCfirPsiSymbolCacheKey(it, CaCfirPsiSymbolKind.LOCAL_VARIABLE) }
    return if (cacheKey != null) {
        getOrCreatePublicSymbol(cacheKey) {
            CaCfirValueParameterSymbolImpl(
                backingSymbol = symbol,
                analysisSession = this,
                containingModule = useSiteModule,
                token = token,
                ownerSymbol = ownerSymbol,
                stableParameterIndex = parameterIndex,
                parameterPsi = parameterPsi,
            )
        }
    } else {
        CaCfirValueParameterSymbolImpl(
            backingSymbol = symbol,
            analysisSession = this,
            containingModule = useSiteModule,
            token = token,
            ownerSymbol = ownerSymbol,
            stableParameterIndex = parameterIndex,
            parameterPsi = parameterPsi,
        )
    }
}

internal fun CaCfirSession.createPropertyAccessorSymbol(
    backingSymbol: CfirCallableSymbol<*>,
    ownerSymbol: CaPropertySymbol,
    kind: CaCfirPropertyAccessorKind,
): CaSymbol {
    val ownerKey = ownerSymbol.publicSymbolCacheKeyOrNull()
        ?: error("Property accessor owner must expose a stable public key")
    return getOrCreatePublicSymbol(CaCfirPropertyAccessorSymbolCacheKey(ownerKey, kind)) {
        when (kind) {
            CaCfirPropertyAccessorKind.GETTER -> CaCfirPropertyGetterSymbolImpl(backingSymbol, this, useSiteModule, token)
            CaCfirPropertyAccessorKind.SETTER -> CaCfirPropertySetterSymbolImpl(backingSymbol, this, useSiteModule, token)
        }
    }
}

internal fun CaCfirSession.createScriptSymbol(script: CjScript): CaCfirScriptSymbolImpl =
    getOrCreatePublicSymbol(CaCfirPsiSymbolCacheKey(script, CaCfirPsiSymbolKind.SCRIPT)) {
        CaCfirScriptSymbolImpl(
            scriptPsi = script,
            scriptFileSymbol = script.containingCjFile?.let(::createFileSymbol),
            containingModule = useSiteModule,
            token = token,
        )
    }

private fun CaCfirSession.createUncachedCallableSymbol(symbol: CfirCallableSymbol<*>): CaCallableSymbol {
    val psi = lookupSourcePsi(symbol)
    return when {
        psi is CjPropertyAccessor -> {
            val owner = psi.property.let { propertyPsi ->
                getPublicSymbolByPsi<CaPropertySymbol>(propertyPsi)
            } ?: error("Property accessor `${psi.text}` is missing owning property symbol")
            val kind = if (psi.isGetter) CaCfirPropertyAccessorKind.GETTER else CaCfirPropertyAccessorKind.SETTER
            createPropertyAccessorSymbol(symbol, owner, kind) as CaCallableSymbol
        }

        symbol is CfirAnonymousFunctionSymbol -> CaCfirAnonymousFunctionSymbolImpl(symbol, this, useSiteModule, token)
        symbol is CfirMainFunctionSymbol -> CaCfirMainFunctionSymbolImpl(symbol, this, useSiteModule, token)
        symbol is CfirMacroDeclarationSymbol -> CaCfirMacroSymbolImpl(symbol, this, useSiteModule, token)
        symbol is CfirFinalizerSymbol -> CaCfirFinalizerSymbolImpl(symbol, this, useSiteModule, token)
        symbol is CfirConstructorSymbol -> CaCfirConstructorSymbolImpl(symbol, this, useSiteModule, token)
        symbol is CfirNamedFunctionSymbol -> CaCfirNamedFunctionSymbolImpl(symbol, this, useSiteModule, token)
        symbol is CfirPropertySymbol -> CaCfirPropertySymbolImpl(symbol, this, useSiteModule, token)
        symbol is CfirFieldVariableSymbol -> CaCfirFieldSymbolImpl(symbol, this, useSiteModule, token)
        symbol is CfirPatternVariableSymbol -> CaCfirPatternVariableSymbolImpl(symbol, this, useSiteModule, token)
        symbol is CfirPatternBindingSymbol -> CaCfirPatternBindingSymbolImpl(symbol, this, useSiteModule, token)
        symbol is CfirValueParameterSymbol -> CaCfirValueParameterSymbolImpl(symbol, this, useSiteModule, token)
        symbol is CfirEnumConstructorSymbol -> CaCfirEnumEntrySymbolImpl(symbol, this, useSiteModule, token)
        else -> error("Unsupported callable public symbol mapping for `${symbol::class.simpleName}`")
    }
}

private fun CfirSymbol<*>.publicTypeParameterOwnerKey(session: CaCfirSession): CaCfirPublicSymbolCacheKey? = when (this) {
    is CfirClassLikeSymbol<*> -> CaCfirClassLikeSymbolCacheKey(classId)
    is CfirExtendSymbol -> CaCfirExtendSymbolCacheKey(session.resolveExtendIdentity(this).extendId)
    is CfirCallableSymbol<*> -> publicSymbolCacheKeyOrNull(session)
    else -> null
}

private fun CfirCallableSymbol<*>.publicSymbolCacheKeyOrNull(session: CaCfirSession): CaCfirPublicSymbolCacheKey? {
    val psi = session.lookupSourcePsi(this)
    if ((cfir as? CfirCallableDeclaration)?.isLocal == true) {
        return psi?.let { localPsi ->
            when (this) {
                is CfirAnonymousFunctionSymbol -> CaCfirPsiSymbolCacheKey(localPsi, CaCfirPsiSymbolKind.ANONYMOUS_FUNCTION)
                is CfirPatternVariableSymbol -> CaCfirPsiSymbolCacheKey(localPsi, CaCfirPsiSymbolKind.PATTERN_VARIABLE)
                is CfirPatternBindingSymbol -> CaCfirPsiSymbolCacheKey(localPsi, CaCfirPsiSymbolKind.PATTERN_BINDING)
                else -> CaCfirPsiSymbolCacheKey(localPsi, CaCfirPsiSymbolKind.LOCAL_VARIABLE)
            }
        }
    }

    return when (this) {
        is CfirAnonymousFunctionSymbol -> psi?.let { CaCfirPsiSymbolCacheKey(it, CaCfirPsiSymbolKind.ANONYMOUS_FUNCTION) }
        is CfirNamedFunctionSymbol -> CaCfirCallableSymbolCacheKey(callableId, CaCfirCallableSymbolKind.NAMED_FUNCTION)
        is CfirMainFunctionSymbol -> CaCfirCallableSymbolCacheKey(callableId, CaCfirCallableSymbolKind.MAIN_FUNCTION)
        is CfirMacroDeclarationSymbol -> CaCfirCallableSymbolCacheKey(callableId, CaCfirCallableSymbolKind.MACRO)
        is CfirFinalizerSymbol -> CaCfirCallableSymbolCacheKey(callableId, CaCfirCallableSymbolKind.FINALIZER)
        is CfirConstructorSymbol -> CaCfirCallableSymbolCacheKey(callableId, CaCfirCallableSymbolKind.CONSTRUCTOR)
        is CfirPropertySymbol -> CaCfirCallableSymbolCacheKey(callableId, CaCfirCallableSymbolKind.PROPERTY)
        is CfirFieldVariableSymbol -> CaCfirCallableSymbolCacheKey(callableId, CaCfirCallableSymbolKind.FIELD)
        is CfirPatternVariableSymbol -> CaCfirCallableSymbolCacheKey(callableId, CaCfirCallableSymbolKind.PATTERN_VARIABLE)
        is CfirPatternBindingSymbol -> CaCfirCallableSymbolCacheKey(callableId, CaCfirCallableSymbolKind.PATTERN_BINDING)
        is CfirEnumConstructorSymbol -> CaCfirCallableSymbolCacheKey(callableId, CaCfirCallableSymbolKind.ENUM_ENTRY)
        is CfirValueParameterSymbol -> null
        else -> null
    }
}

internal fun CaCfirSession.restoreCallablePublicSymbol(
    callableId: CallableId,
    kind: CaCfirCallableSymbolKind,
): CaCallableSymbol? {
    val ownerClassId = callableId.classId
    val candidates = when (ownerClassId) {
        null -> getOrCreateTopLevelPublicSymbols(callableId.packageName, callableId.callableName).callableSymbols
        else -> queryDeclaredMemberScope(ownerClassId)
            ?.getCallableSymbols(callableId.callableName)
            ?.map(::createCallableSymbol)
            .orEmpty()
    }
    val stableCandidate = candidates.singleOrNull { candidate -> candidate.matchesStableCallable(callableId, kind) }
    if (stableCandidate != null) return stableCandidate

    if (kind == CaCfirCallableSymbolKind.PATTERN_VARIABLE || kind == CaCfirCallableSymbolKind.PATTERN_BINDING) {
        return resolutionFacade.cfirFiles
            .asSequence()
            .mapNotNull { file -> file.findCallableSymbol(callableId, kind) }
            .firstOrNull()
            ?.let(::createCallableSymbol)
    }

    return null
}

private fun CfirFile.findCallableSymbol(
    callableId: CallableId,
    kind: CaCfirCallableSymbolKind,
): CfirCallableSymbol<*>? {
    var result: CfirCallableSymbol<*>? = null
    accept(object : CfirVisitorVoid() {
        override fun visitElement(element: org.cangnova.cangjie.cfir.CfirElement) {
            if (result != null) return
            when (element) {
                is CfirCallableDeclaration -> {
                    val symbol = element.symbol
                    if (symbol.callableId == callableId && symbol.matchesKind(kind)) {
                        result = symbol
                        return
                    }
                }
            }
            element.acceptChildren(this)
        }
    }, null)
    return result
}

private fun CfirCallableSymbol<*>.matchesKind(kind: CaCfirCallableSymbolKind): Boolean = when (kind) {
    CaCfirCallableSymbolKind.PATTERN_VARIABLE -> this is CfirPatternVariableSymbol
    CaCfirCallableSymbolKind.PATTERN_BINDING -> this is CfirPatternBindingSymbol
    CaCfirCallableSymbolKind.NAMED_FUNCTION -> this is CfirNamedFunctionSymbol
    CaCfirCallableSymbolKind.MAIN_FUNCTION -> this is CfirMainFunctionSymbol
    CaCfirCallableSymbolKind.MACRO -> this is CfirMacroDeclarationSymbol
    CaCfirCallableSymbolKind.FINALIZER -> this is CfirFinalizerSymbol
    CaCfirCallableSymbolKind.CONSTRUCTOR -> this is CfirConstructorSymbol
    CaCfirCallableSymbolKind.PROPERTY -> this is CfirPropertySymbol
    CaCfirCallableSymbolKind.FIELD -> this is CfirFieldVariableSymbol
    CaCfirCallableSymbolKind.ENUM_ENTRY -> this is CfirEnumConstructorSymbol
}

internal fun CaCfirSession.restoreExtendPublicSymbol(extendId: String): CaExtendSymbol? {
    val packageFqName = parseExtendPackageFqName(extendId) ?: return null
    val extendDeclaration = cfirSession.extendProviderOrNull
        ?.getExtendsInPackage(packageFqName)
        ?.firstOrNull { extend -> resolveExtendIdentity(extend.symbol).extendId == extendId }
        ?: return null
    return createExtendSymbol(extendDeclaration.symbol)
}

private data class CaCfirResolvedExtendIdentity(
    val extendId: String,
    val extendPsi: CjExtend?,
    val packageFqName: FqName,
)

/**
 * 为 extend 声明统一解析稳定身份。
 *
 * 规则是：
 * 1. 优先使用 source/decompiled PSI 的 `getExtendId()`；
 * 2. 如果当前 session 拿不到 PSI，再退回到基于 CFIR 声明语义的稳定构造；
 * 3. cache key、pointer restore、public symbol 构造全部共用这一套逻辑。
 */
private fun CaCfirSession.resolveExtendIdentity(symbol: CfirExtendSymbol): CaCfirResolvedExtendIdentity {
    val packageFqName = cfirSession.symbolProvider.getContainingFile(symbol)?.packageDirective?.packageFqName ?: FqName.ROOT
    val sourceExtendPsi = lookupSourcePsi(symbol) as? CjExtend
    val provisionalExtendId = buildStableExtendId(packageFqName, symbol.cfir, sourceExtendPsi)
    val resolvedExtendPsi = sourceExtendPsi ?: findDecompiledExtendPsi(project, packageFqName, provisionalExtendId, useSiteModule)
    val stableExtendId = buildStableExtendId(packageFqName, symbol.cfir, resolvedExtendPsi)
    return CaCfirResolvedExtendIdentity(
        extendId = stableExtendId,
        extendPsi = resolvedExtendPsi,
        packageFqName = packageFqName,
    )
}

private fun buildStableExtendId(
    packageFqName: FqName,
    extend: CfirExtend,
    extendPsi: CjExtend? = null,
): String {
    extendPsi?.let { return it.getExtendId() }

    val readableRenderer = CfirRenderer.withReadability()
    val extendedTypeText = normalizeExtendTypeText(readableRenderer.renderElementAsString(extend.extendedTypeRef))
    val superTypeTexts = extend.superTypeRefs
        .map { superTypeRef -> normalizeExtendTypeText(readableRenderer.renderElementAsString(superTypeRef)) }
        .filter(String::isNotBlank)
    return buildExtendId(
        packageFqName = packageFqName,
        receiverTypeText = extendedTypeText,
        superTypeTexts = superTypeTexts,
    )
}

private fun normalizeExtendTypeText(rendered: String): String {
    return rendered.removePrefix("R|").removeSuffix("|").trim()
}

private fun findDecompiledExtendPsi(
    project: Project,
    packageFqName: FqName,
    extendId: String,
    preferredModule: org.cangnova.cangjie.analysis.api.CaModule?,
): CjExtend? {
    val psiProvider = project.getService(CaDecompiledPsiProvider::class.java) ?: return null
    val projectStructure = CaProjectStructureProvider.getInstance(project)

    fun findInLibraryModule(module: CaLibraryModule): CjExtend? {
        val file = psiProvider.findDecompiledFile(module, packageFqName) ?: return null
        return file.declarations.filterIsInstance<CjExtend>().firstOrNull { candidate -> candidate.getExtendId() == extendId }
    }

    fun findInBuiltinsModule(module: CaBuiltinsModule): CjExtend? {
        val file = psiProvider.findDecompiledFile(module, packageFqName) ?: return null
        return file.declarations.filterIsInstance<CjExtend>().firstOrNull { candidate -> candidate.getExtendId() == extendId }
    }

    when (preferredModule) {
        is CaBuiltinsModule -> findInBuiltinsModule(preferredModule)?.let { return it }
        is CaLibraryModule -> findInLibraryModule(preferredModule)?.let { return it }
    }

    projectStructure.allModules.filterIsInstance<CaBuiltinsModule>().forEach { module ->
        if (module === preferredModule) return@forEach
        findInBuiltinsModule(module)?.let { return it }
    }
    projectStructure.allModules.filterIsInstance<CaLibraryModule>().forEach { module ->
        if (module === preferredModule) return@forEach
        findInLibraryModule(module)?.let { return it }
    }
    return null
}

private fun parseExtendPackageFqName(extendId: String): FqName? {
    val separatorIndex = extendId.indexOf(':')
    if (separatorIndex < 0) return null
    return FqName(extendId.substring(0, separatorIndex))
}

private fun CaCallableSymbol.matchesStableCallable(
    callableId: CallableId,
    kind: CaCfirCallableSymbolKind,
): Boolean {
    if (this.callableId != callableId) return false
    return when (kind) {
        CaCfirCallableSymbolKind.NAMED_FUNCTION -> this is org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol &&
            this !is org.cangnova.cangjie.analysis.api.symbols.CaMainFunctionSymbol &&
            this !is org.cangnova.cangjie.analysis.api.symbols.CaMacroSymbol
        CaCfirCallableSymbolKind.MAIN_FUNCTION -> this is org.cangnova.cangjie.analysis.api.symbols.CaMainFunctionSymbol
        CaCfirCallableSymbolKind.MACRO -> this is org.cangnova.cangjie.analysis.api.symbols.CaMacroSymbol
        CaCfirCallableSymbolKind.FINALIZER -> this is org.cangnova.cangjie.analysis.api.symbols.CaFinalizerSymbol
        CaCfirCallableSymbolKind.CONSTRUCTOR -> this is org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
        CaCfirCallableSymbolKind.PROPERTY -> this is CaPropertySymbol
        CaCfirCallableSymbolKind.FIELD -> this is org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol
        CaCfirCallableSymbolKind.PATTERN_VARIABLE -> this is org.cangnova.cangjie.analysis.api.symbols.CaPatternVariableSymbol
        CaCfirCallableSymbolKind.PATTERN_BINDING -> this is org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol
        CaCfirCallableSymbolKind.ENUM_ENTRY -> this is org.cangnova.cangjie.analysis.api.symbols.CaEnumEntrySymbol
    }
}

internal fun CaSymbol.publicSymbolCacheKeyOrNull(): CaCfirPublicSymbolCacheKey? = when (this) {
    is CaCfirFileSymbolImpl -> CaCfirFileSymbolCacheKey(file)
    is CaCfirPackageSymbolImpl -> CaCfirPackageSymbolCacheKey(fqName)
    is CaCfirClassLikeSymbolBase<*> -> classId?.let(::CaCfirClassLikeSymbolCacheKey)
    is CaCfirExtendSymbolImpl -> CaCfirExtendSymbolCacheKey(extendId)
    is CaCfirPropertyAccessorSymbolBase -> {
        val ownerKey = owningProperty.publicSymbolCacheKeyOrNull() ?: return null
        CaCfirPropertyAccessorSymbolCacheKey(
            ownerKey = ownerKey,
            kind = if (isGetter) CaCfirPropertyAccessorKind.GETTER else CaCfirPropertyAccessorKind.SETTER,
        )
    }
    is CaCfirValueParameterSymbolImpl -> {
        val ownerKey = (containingDeclaration as? CaSymbol)?.publicSymbolCacheKeyOrNull() ?: return null
        val parameterIndex = stableParameterIndex ?: return null
        CaCfirValueParameterSymbolCacheKey(ownerKey, parameterIndex, name)
    }
    is CaCfirTypeParameterSymbolImpl -> {
        val owner = containingDeclaration ?: return null
        val ownerKey = owner.publicSymbolCacheKeyOrNull() ?: return null
        CaCfirTypeParameterSymbolCacheKey(ownerKey, name)
    }
    is CaCfirCallableSymbolBase<*> -> backingSymbol.publicSymbolCacheKeyOrNull(analysisSession)
    else -> null
}

internal fun CaSymbol.completionDecisionKey(): CaCfirCompletionSymbolKey =
    publicSymbolCacheKeyOrNull()?.let(::CaCfirStableCompletionSymbolKey)
        ?: CaCfirEphemeralCompletionSymbolKey(this)

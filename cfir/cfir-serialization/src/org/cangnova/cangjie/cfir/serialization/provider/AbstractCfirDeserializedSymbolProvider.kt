package org.cangnova.cangjie.cfir.serialization.provider

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeclDeserializer
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirTypeDeserializer
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import java.util.concurrent.ConcurrentHashMap

/**
 * Abstract deserialized symbol provider aligned with Kotlin's AbstractFirDeserializedSymbolProvider shape.
 *
 * Subclasses only provide package deserializers; cache/query strategy lives here.
 */
abstract class AbstractCfirDeserializedSymbolProvider(
    session: CfirSession,
    protected val cangjieScopeProvider: CfirCangJieScopeProvider,
    protected val libraryModuleData: CfirModuleData,
) : CfirSymbolProvider(session) {

    abstract override val symbolNamesProvider: CfirSymbolNamesProvider

    private val contextCache = ConcurrentHashMap<String, PackageDeserializers>()
    private val missingContexts = ConcurrentHashMap.newKeySet<String>()

    private val classCache = ConcurrentHashMap<ClassId, CfirClassLikeSymbol<*>>()
    private val missingClasses = ConcurrentHashMap.newKeySet<ClassId>()

    private val callableCache = ConcurrentHashMap<CallableId, List<CfirCallableSymbol<*>>>()
    private val functionCache = ConcurrentHashMap<CallableId, List<CfirNamedFunctionSymbol>>()
    private val propertyCache = ConcurrentHashMap<CallableId, List<CfirPropertySymbol>>()
    private val extendCache = ConcurrentHashMap<FqName, List<CfirExtend>>()
    private val promotedEnumCallableCache = ConcurrentHashMap<CallableId, List<CfirCallableSymbol<*>>>()

    private val scopeSession = ScopeSession()
    private val initializedPackageScopes = ConcurrentHashMap.newKeySet<FqName>()

    protected abstract fun loadPackageDeserializers(packageFqName: String): PackageDeserializers?

    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? {
        classCache[classId]?.let { return it }
        if (classId in missingClasses) return null

        if (!mayHaveClassifier(classId)) {
            missingClasses += classId
            return null
        }

        val loaded = loadTopLevelClassSymbol(classId)

        if (loaded == null) {
            missingClasses += classId
            return null
        }

        missingClasses.remove(classId)
        classCache.putIfAbsent(classId, loaded)
        return classCache[classId] ?: loaded
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(
        destination: MutableList<CfirCallableSymbol<*>>,
        packageFqName: FqName,
        name: Name,
    ) {
        val callableId = CallableId(packageFqName, name)
        callableCache[callableId]?.let {
            destination += it
            return
        }

        val deserializers = getOrCreateDeserializers(packageFqName.asString())
        val directLoaded = deserializers?.header?.topLevelNameToIndices
            ?.get(name.asString())
            .orEmpty()
            .mapNotNull { declIndex ->
                val decl = deserializers?.declDeserializer?.deserializeDecl(declIndex)
                (decl as? CfirCallableDeclaration)?.symbol as? CfirCallableSymbol<*>
            }
        val promotedEnumCtors = getPromotedTopLevelEnumConstructors(packageFqName, name)
        val loaded = (directLoaded + promotedEnumCtors).distinct()

        callableCache.putIfAbsent(callableId, loaded)
        destination += callableCache[callableId] ?: loaded
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<CfirNamedFunctionSymbol>,
        packageFqName: FqName,
        name: Name,
    ) {
        val callableId = CallableId(packageFqName, name)
        functionCache[callableId]?.let {
            destination += it
            return
        }

        val loaded = getTopLevelCallableSymbols(packageFqName, name).filterIsInstance<CfirNamedFunctionSymbol>()
        functionCache.putIfAbsent(callableId, loaded)
        destination += functionCache[callableId] ?: loaded
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(
        destination: MutableList<CfirPropertySymbol>,
        packageFqName: FqName,
        name: Name,
    ) {
        val callableId = CallableId(packageFqName, name)
        propertyCache[callableId]?.let {
            destination += it
            return
        }

        val loaded = getTopLevelCallableSymbols(packageFqName, name).filterIsInstance<CfirPropertySymbol>()
        propertyCache.putIfAbsent(callableId, loaded)
        destination += propertyCache[callableId] ?: loaded
    }

    fun getTopLevelExtendDeclarations(packageFqName: FqName): List<CfirExtend> {
        extendCache[packageFqName]?.let { return it }

        val deserializers = getOrCreateDeserializers(packageFqName.asString()) ?: return emptyList()
        val declIndices = deserializers.header.topLevelExtendIndices
        val loaded = declIndices.mapNotNull { declIndex ->
            deserializers.declDeserializer.deserializeDecl(declIndex) as? CfirExtend
        }

        extendCache.putIfAbsent(packageFqName, loaded)
        return extendCache[packageFqName] ?: loaded
    }

    protected fun getOrCreateDeserializers(fullPkgName: String): PackageDeserializers? {
        contextCache[fullPkgName]?.let { return it }
        if (fullPkgName in missingContexts) return null

        val created = loadPackageDeserializers(fullPkgName)
        if (created == null) {
            missingContexts += fullPkgName
            return null
        }

        missingContexts.remove(fullPkgName)
        contextCache.putIfAbsent(fullPkgName, created)

        val packageFqName = FqName(fullPkgName)
        initializePackageScope(packageFqName)

        return contextCache[fullPkgName] ?: created
    }

    private fun initializePackageScope(packageFqName: FqName) {
        if (!initializedPackageScopes.add(packageFqName)) return
        cangjieScopeProvider.getPackageMemberScope(packageFqName, this, session, scopeSession)
    }

    private fun mayHaveClassifier(classId: ClassId): Boolean {
        val names = symbolNamesProvider.getTopLevelClassifierNamesInPackage(classId.packageFqName)
        return names == null || classId.shortClassName in names
    }

    private fun loadTopLevelClassSymbol(classId: ClassId): CfirClassLikeSymbol<*>? {
        val deserializers = getOrCreateDeserializers(classId.packageFqName.asString()) ?: return null
        val shortName = classId.shortClassName.asString()
        val indices = deserializers.header.topLevelClassifierNameToIndices[shortName].orEmpty()

        for (declIndex in indices) {
            val decl = deserializers.declDeserializer.deserializeDecl(declIndex)
            if (decl is CfirClassLikeDeclaration && decl.symbol is CfirClassLikeSymbol<*> && declSymbolName(
                    decl
                ) == shortName
            ) {
                val symbol = decl.symbol
                return symbol
            }
        }

        return null
    }

    private fun getPromotedTopLevelEnumConstructors(packageFqName: FqName, name: Name): List<CfirCallableSymbol<*>> {
        val callableId = CallableId(packageFqName, name)
        promotedEnumCallableCache[callableId]?.let { return it }

        val deserializers = getOrCreateDeserializers(packageFqName.asString()) ?: return emptyList()
        val result = mutableListOf<CfirCallableSymbol<*>>()

        for (className in deserializers.header.topLevelClassNames) {
            val classId = ClassId(packageFqName, className)
            val classSymbol = getClassLikeSymbolByClassId(classId) ?: continue
            if (!classSymbol.isBound) continue
            val klass = classSymbol.cfir
            if (klass !is CfirEnum) continue
            klass.declarations.asSequence()
                .filterIsInstance<CfirEnumConstructor>()
                .mapNotNull { declaration ->
                    if (declaration.name != name) return@mapNotNull null
                    val symbol = declaration.symbol as? CfirCallableSymbol<*> ?: return@mapNotNull null
                    symbol
                }
                .forEach(result::add)
        }

        val distinct = result.distinct()
        promotedEnumCallableCache.putIfAbsent(callableId, distinct)
        return promotedEnumCallableCache[callableId] ?: distinct
    }

    private fun declSymbolName(declaration: CfirClassLikeDeclaration): String = when (declaration) {
        is CfirClass -> declaration.name.asString()
        is org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration -> declaration.name.asString()
        is org.cangnova.cangjie.cfir.declarations.CfirInterface -> declaration.name.asString()
        is org.cangnova.cangjie.cfir.declarations.CfirStruct -> declaration.name.asString()
        is CfirEnum -> declaration.name.asString()
        is org.cangnova.cangjie.cfir.declarations.CfirTypeAlias -> declaration.name.asString()
    }

    protected class PackageDeserializers(
        val header: CjoPackageHeader,
        @Suppress("unused") val typeDeserializer: CfirTypeDeserializer,
        val declDeserializer: CfirDeclDeserializer,
    )
}

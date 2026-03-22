package org.cangnova.cangjie.cfir.serialization.provider

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassKind
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeclDeserializer
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirTypeDeserializer
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
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

    private val classCache = ConcurrentHashMap<ClassId, CfirClassSymbol>()
    private val classIdBySymbolCache = ConcurrentHashMap<CfirClassSymbol, ClassId>()
    private val missingClasses = ConcurrentHashMap.newKeySet<ClassId>()

    private val callableCache = ConcurrentHashMap<CallableId, List<CfirCallableSymbol<*>>>()
    private val functionCache = ConcurrentHashMap<CallableId, List<CfirFunctionSymbol>>()
    private val propertyCache = ConcurrentHashMap<CallableId, List<CfirPropertySymbol>>()
    private val enumCtorOwnerClassIdCache = ConcurrentHashMap<CfirEnumConstructorSymbol, ClassId>()
    private val promotedEnumCallableCache = ConcurrentHashMap<CallableId, List<CfirCallableSymbol<*>>>()

    private val scopeSession = ScopeSession()
    private val initializedPackageScopes = ConcurrentHashMap.newKeySet<FqName>()

    protected abstract fun loadPackageDeserializers(packageFqName: String): PackageDeserializers?

    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassSymbol? {
        classCache[classId]?.let { return it }
        if (classId in missingClasses) return null

        if (!mayHaveClassifier(classId)) {
            missingClasses += classId
            return null
        }

        val loaded = if (classId.isNestedClass) {
            loadNestedClassSymbol(classId)
        } else {
            loadTopLevelClassSymbol(classId)
        }

        if (loaded == null) {
            missingClasses += classId
            return null
        }

        missingClasses.remove(classId)
        classCache.putIfAbsent(classId, loaded)
        classIdBySymbolCache.putIfAbsent(loaded, classId)
        return classCache[classId] ?: loaded
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<CfirCallableSymbol<*>> {
        val callableId = CallableId(packageFqName, name)
        callableCache[callableId]?.let { return it }

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
        return callableCache[callableId] ?: loaded
    }

    override fun getTopLevelFunctionSymbols(packageFqName: FqName, name: Name): List<CfirFunctionSymbol> {
        val callableId = CallableId(packageFqName, name)
        functionCache[callableId]?.let { return it }

        val loaded = getTopLevelCallableSymbols(packageFqName, name).filterIsInstance<CfirFunctionSymbol>()
        functionCache.putIfAbsent(callableId, loaded)
        return functionCache[callableId] ?: loaded
    }

    override fun getTopLevelPropertySymbols(packageFqName: FqName, name: Name): List<CfirPropertySymbol> {
        val callableId = CallableId(packageFqName, name)
        propertyCache[callableId]?.let { return it }

        val loaded = getTopLevelCallableSymbols(packageFqName, name).filterIsInstance<CfirPropertySymbol>()
        propertyCache.putIfAbsent(callableId, loaded)
        return propertyCache[callableId] ?: loaded
    }

    override fun getClassIdBySymbol(classSymbol: CfirClassSymbol): ClassId? {
        return classIdBySymbolCache[classSymbol]
    }

    override fun getEnumConstructorOwnerClassId(symbol: CfirEnumConstructorSymbol): ClassId? {
        return enumCtorOwnerClassIdCache[symbol]
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
        val topLevelClassId = classId.outermostClassId
        val names = symbolNamesProvider.getTopLevelClassifierNamesInPackage(topLevelClassId.packageFqName)
        return names == null || topLevelClassId.shortClassName in names
    }

    private fun loadTopLevelClassSymbol(classId: ClassId): CfirClassSymbol? {
        val deserializers = getOrCreateDeserializers(classId.packageFqName.asString()) ?: return null
        val shortName = classId.shortClassName.asString()
        val indices = deserializers.header.topLevelNameToIndices[shortName].orEmpty()

        for (declIndex in indices) {
            val decl = deserializers.declDeserializer.deserializeDecl(declIndex)
            if (decl is CfirClass && decl.name.asString() == shortName) {
                val symbol = decl.symbol as CfirClassSymbol
                classIdBySymbolCache.putIfAbsent(symbol, classId)
                registerEnumConstructorOwnersIfNeeded(classId, decl)
                return symbol
            }
        }

        return null
    }

    private fun loadNestedClassSymbol(classId: ClassId): CfirClassSymbol? {
        val outerClassId = classId.outerClassId ?: return null
        val outer = getClassLikeSymbolByClassId(outerClassId) ?: return null
        val nestedName = classId.shortClassName

        return outer.cfir.declarations
            .asSequence()
            .mapNotNull { it as? CfirClass }
            .firstOrNull { it.name == nestedName }
            ?.let { nested ->
                registerEnumConstructorOwnersIfNeeded(classId, nested)
                (nested.symbol as? CfirClassSymbol)?.also { classIdBySymbolCache.putIfAbsent(it, classId) }
            }
    }

    private fun registerEnumConstructorOwnersIfNeeded(classId: ClassId, klass: CfirClass) {
        if (klass.classKind != CfirClassKind.ENUM) return
        klass.declarations.asSequence()
            .filterIsInstance<CfirEnumConstructor>()
            .mapNotNull { it.symbol as? CfirEnumConstructorSymbol }
            .forEach { enumCtorOwnerClassIdCache.putIfAbsent(it, classId) }
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
            if (klass.classKind != CfirClassKind.ENUM) continue
            registerEnumConstructorOwnersIfNeeded(classId, klass)
            klass.declarations.asSequence()
                .filterIsInstance<CfirEnumConstructor>()
                .mapNotNull { declaration ->
                    if (declaration.name != name) return@mapNotNull null
                    val symbol = declaration.symbol as? CfirCallableSymbol<*> ?: return@mapNotNull null
                    (symbol as? CfirEnumConstructorSymbol)?.let {
                        enumCtorOwnerClassIdCache.putIfAbsent(it, classId)
                    }
                    symbol
                }
                .forEach(result::add)
        }

        val distinct = result.distinct()
        promotedEnumCallableCache.putIfAbsent(callableId, distinct)
        return promotedEnumCallableCache[callableId] ?: distinct
    }

    protected class PackageDeserializers(
        val header: CjoPackageHeader,
        @Suppress("unused") val typeDeserializer: CfirTypeDeserializer,
        val declDeserializer: CfirDeclDeserializer,
    )
}

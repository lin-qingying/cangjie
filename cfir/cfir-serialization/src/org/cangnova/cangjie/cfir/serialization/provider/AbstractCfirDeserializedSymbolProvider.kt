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
import org.cangnova.cangjie.cfir.serialization.cjo.exportedMemberName
import org.cangnova.cangjie.cfir.serialization.cjo.importedMemberName
import org.cangnova.cangjie.cfir.serialization.cjo.importedPackageFqName
import org.cangnova.cangjie.cfir.serialization.cjo.isPublicExportImport
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext
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

        val loaded = loadTopLevelClassSymbolRecursively(classId, linkedSetOf())

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
        destination += resolveTopLevelCallableSymbols(packageFqName, name, linkedSetOf())
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
        val declDeserializer = deserializers.createDeclDeserializer()
        val loaded = declIndices.mapNotNull { declIndex ->
            declDeserializer.deserializeDecl(declIndex) as? CfirExtend
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

    private fun loadTopLevelClassSymbolRecursively(
        classId: ClassId,
        visitingPackages: LinkedHashSet<FqName>,
    ): CfirClassLikeSymbol<*>? {
        loadDirectTopLevelClassSymbol(classId)?.let { return it }
        if (!visitingPackages.add(classId.packageFqName)) return null
        val exported = loadExportedTopLevelClassSymbol(classId, visitingPackages)
        visitingPackages.remove(classId.packageFqName)
        return exported
    }

    private fun loadDirectTopLevelClassSymbol(classId: ClassId): CfirClassLikeSymbol<*>? {
        val deserializers = getOrCreateDeserializers(classId.packageFqName.asString()) ?: return null
        val shortName = classId.shortClassName.asString()
        val indices = deserializers.header.topLevelClassifierNameToIndices[shortName].orEmpty()
        val declDeserializer = deserializers.createDeclDeserializer()

        for (declIndex in indices) {
            val decl = declDeserializer.deserializeDecl(declIndex)
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

    /**
     * `public import` 导出的 classifier 在本包里没有物理 `Decl`，
     * 因此必须沿着导出 import 递归到真实声明包取 symbol。
     */
    private fun loadExportedTopLevelClassSymbol(
        classId: ClassId,
        visitingPackages: LinkedHashSet<FqName>,
    ): CfirClassLikeSymbol<*>? {
        val header = getOrCreateDeserializers(classId.packageFqName.asString())?.header ?: return null
        for (entry in header.fileImportEntries) {
            if (!entry.isPublicExportImport()) continue
            val importedPackageFqName = entry.importedPackageFqName() ?: continue
            if (entry.isAllUnder) {
                loadTopLevelClassSymbolRecursively(
                    ClassId(importedPackageFqName, classId.shortClassName),
                    visitingPackages,
                )?.let { return it }
                continue
            }

            val exportedName = entry.exportedMemberName() ?: continue
            if (exportedName != classId.shortClassName) continue

            val importedMemberName = entry.importedMemberName() ?: continue
            loadTopLevelClassSymbolRecursively(
                ClassId(importedPackageFqName, importedMemberName),
                visitingPackages,
            )?.let { return it }
        }
        return null
    }

    private fun resolveTopLevelCallableSymbols(
        packageFqName: FqName,
        name: Name,
        visitingPackages: LinkedHashSet<FqName>,
    ): List<CfirCallableSymbol<*>> {
        val callableId = CallableId(packageFqName, name)
        callableCache[callableId]?.let { return it }

        val directLoaded = loadDirectTopLevelCallableSymbols(packageFqName, name)
        val promotedEnumCtors = getPromotedTopLevelEnumConstructors(packageFqName, name)
        val exportedLoaded = if (visitingPackages.add(packageFqName)) {
            val symbols = loadExportedTopLevelCallableSymbols(packageFqName, name, visitingPackages)
            visitingPackages.remove(packageFqName)
            symbols
        } else {
            emptyList()
        }
        val loaded = (directLoaded + promotedEnumCtors + exportedLoaded).distinct()

        callableCache.putIfAbsent(callableId, loaded)
        return callableCache[callableId] ?: loaded
    }

    private fun loadDirectTopLevelCallableSymbols(
        packageFqName: FqName,
        name: Name,
    ): List<CfirCallableSymbol<*>> {
        val deserializers = getOrCreateDeserializers(packageFqName.asString()) ?: return emptyList()
        val declDeserializer = deserializers.createDeclDeserializer()
        return deserializers.header.topLevelNameToIndices[name.asString()]
            .orEmpty()
            .mapNotNull { declIndex ->
                val decl = declDeserializer.deserializeDecl(declIndex)
                (decl as? CfirCallableDeclaration)?.symbol as? CfirCallableSymbol<*>
            }
    }

    /**
     * `public import` 导出的 callable 与顶层物理声明一样参与普通包成员查询。
     */
    private fun loadExportedTopLevelCallableSymbols(
        packageFqName: FqName,
        name: Name,
        visitingPackages: LinkedHashSet<FqName>,
    ): List<CfirCallableSymbol<*>> {
        val header = getOrCreateDeserializers(packageFqName.asString())?.header ?: return emptyList()
        val result = mutableListOf<CfirCallableSymbol<*>>()

        for (entry in header.fileImportEntries) {
            if (!entry.isPublicExportImport()) continue
            val importedPackageFqName = entry.importedPackageFqName() ?: continue
            if (entry.isAllUnder) {
                result += resolveTopLevelCallableSymbols(importedPackageFqName, name, visitingPackages)
                continue
            }

            val exportedName = entry.exportedMemberName() ?: continue
            if (exportedName != name) continue

            val importedMemberName = entry.importedMemberName() ?: continue
            result += resolveTopLevelCallableSymbols(importedPackageFqName, importedMemberName, visitingPackages)
        }

        return result.distinct()
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
        private val context: CfirDeserializationContext,
    ) {
        /**
         * 对齐 Kotlin provider 只缓存 package context 的所有权：
         * 具体 deserializer 必须按次创建，避免把 owner 栈和递归检测状态泄漏到并发查询之间。
         */
        fun createDeclDeserializer() = context.createDeclDeserializer()
    }
}

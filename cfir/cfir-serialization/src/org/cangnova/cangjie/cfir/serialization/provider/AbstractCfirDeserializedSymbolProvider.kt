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
 * 反序列化符号提供器的公共基类。
 *
 * 结构对齐 Kotlin 的 `AbstractFirDeserializedSymbolProvider`：子类只负责加载包级 deserializer，
 * 这里集中实现 class-like、callable、extend 与 public import re-export 的缓存和查询策略。
 */
abstract class AbstractCfirDeserializedSymbolProvider(
    /** 当前 CFIR session。 */
    session: CfirSession,
    /** 用于初始化反序列化包成员 scope 的 scope provider。 */
    protected val cangjieScopeProvider: CfirCangJieScopeProvider,
    /** 库声明归属的模块数据。 */
    protected val libraryModuleData: CfirModuleData,
) : CfirSymbolProvider(session) {

    /** 反序列化包的顶层名称索引提供器。 */
    abstract override val symbolNamesProvider: CfirSymbolNamesProvider

    /** 完整包名到包级反序列化器持有对象的缓存。 */
    private val contextCache = ConcurrentHashMap<String, PackageDeserializers>()
    /** 已确认无法加载的完整包名集合。 */
    private val missingContexts = ConcurrentHashMap.newKeySet<String>()

    /** classId 到 class-like symbol 的缓存。 */
    private val classCache = ConcurrentHashMap<ClassId, CfirClassLikeSymbol<*>>()
    /** 已确认不存在的 classId 集合。 */
    private val missingClasses = ConcurrentHashMap.newKeySet<ClassId>()

    /** 顶层 callableId 到 callable symbol 列表的统一缓存。 */
    private val callableCache = ConcurrentHashMap<CallableId, List<CfirCallableSymbol<*>>>()
    /** 顶层函数 callableId 到函数 symbol 列表的缓存。 */
    private val functionCache = ConcurrentHashMap<CallableId, List<CfirNamedFunctionSymbol>>()
    /** 顶层属性 callableId 到属性 symbol 列表的缓存。 */
    private val propertyCache = ConcurrentHashMap<CallableId, List<CfirPropertySymbol>>()
    /** 包名到顶层 extend 声明列表的缓存。 */
    private val extendCache = ConcurrentHashMap<FqName, List<CfirExtend>>()
    /** enum constructor 提升为顶层 callable 查询结果时使用的缓存。 */
    private val promotedEnumCallableCache = ConcurrentHashMap<CallableId, List<CfirCallableSymbol<*>>>()

    /** 反序列化 scope 初始化使用的共享 scope session。 */
    private val scopeSession = ScopeSession()
    /** 已初始化包成员 scope 的包名集合。 */
    private val initializedPackageScopes = ConcurrentHashMap.newKeySet<FqName>()

    /** 加载指定完整包名的包级 deserializer；加载失败返回 null。 */
    protected abstract fun loadPackageDeserializers(packageFqName: String): PackageDeserializers?

    /** 按 classId 查询库 class-like symbol，必要时递归跟随 public import re-export。 */
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
    /** 将顶层 callable 查询结果追加到 [destination]。 */
    override fun getTopLevelCallableSymbolsTo(
        destination: MutableList<CfirCallableSymbol<*>>,
        packageFqName: FqName,
        name: Name,
    ) {
        destination += resolveTopLevelCallableSymbols(packageFqName, name, linkedSetOf())
    }

    @CfirSymbolProviderInternals
    /** 将顶层函数 symbol 查询结果追加到 [destination]。 */
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
    /** 将顶层属性 symbol 查询结果追加到 [destination]。 */
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

    /** 查询指定包内的顶层 extend 声明。 */
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

    /**
     * 获取或创建指定完整包名的包级 deserializer。
     *
     * 成功加载后会初始化对应包成员 scope，确保 provider 查询路径与普通 scope 查询路径共享同一包 scope。
     */
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

    /** 初始化指定包的包成员 scope，重复调用只会执行一次。 */
    private fun initializePackageScope(packageFqName: FqName) {
        if (!initializedPackageScopes.add(packageFqName)) return
        cangjieScopeProvider.getPackageMemberScope(packageFqName, this, session, scopeSession)
    }

    /** 使用名称索引快速判断包中是否可能包含指定 classifier。 */
    private fun mayHaveClassifier(classId: ClassId): Boolean {
        val names = symbolNamesProvider.getTopLevelClassifierNamesInPackage(classId.packageFqName)
        return names == null || classId.shortClassName in names
    }

    /**
     * 递归加载顶层 class-like symbol。
     *
     * 先查当前包物理声明，再沿 public import re-export 追踪真实声明包；[visitingPackages] 防止循环导出。
     */
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

    /** 只从指定包的物理顶层声明中加载 class-like symbol。 */
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

    /**
     * 解析顶层 callable symbol 列表。
     *
     * 结果包含物理顶层声明、提升后的 enum constructor 和 public import re-export 的 callable。
     */
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

    /** 只从指定包的物理顶层声明索引中加载 callable symbol。 */
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

    /**
     * 收集 enum constructor 作为包级 callable 的提升查询结果。
     *
     * 仓颉 enum constructor 需要像顶层 callable 一样被普通名称查询命中。
     */
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

    /** 提取 class-like 声明对应的源码级名称文本。 */
    private fun declSymbolName(declaration: CfirClassLikeDeclaration): String = when (declaration) {
        is CfirClass -> declaration.name.asString()
        is org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration -> declaration.name.asString()
        is org.cangnova.cangjie.cfir.declarations.CfirInterface -> declaration.name.asString()
        is org.cangnova.cangjie.cfir.declarations.CfirStruct -> declaration.name.asString()
        is CfirEnum -> declaration.name.asString()
        is org.cangnova.cangjie.cfir.declarations.CfirTypeAlias -> declaration.name.asString()
    }

    /**
     * 单个包的反序列化入口持有对象。
     *
     * 只缓存包头与上下文；声明 deserializer 每次查询按需新建，避免 owner 栈和递归检测状态跨查询共享。
     */
    protected class PackageDeserializers(
        /** 当前包头轻量索引。 */
        val header: CjoPackageHeader,
        /** 当前包的共享反序列化上下文。 */
        private val context: CfirDeserializationContext,
    ) {
        /**
         * 对齐 Kotlin provider 只缓存 package context 的所有权：
         * 具体 deserializer 必须按次创建，避免把 owner 栈和递归检测状态泄漏到并发查询之间。
         */
        /** 创建一次性的声明反序列化器。 */
        fun createDeclDeserializer() = context.createDeclDeserializer()
    }
}

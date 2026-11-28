package org.cangnova.cangjie.cfir.serialization.provider

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirBuiltInDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.declarations.cjoDeclarationOrigin
import org.cangnova.cangjie.cfir.source.CfirSourceGraph
import org.cangnova.cangjie.cfir.source.CfirSourceImportedFunctionHeader
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirSourceGraphLoader
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoSourceContentRoot
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
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
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
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
) : CfirSymbolProvider(session), CfirClassLikeSymbolLookup {

    /** 反序列化包的顶层名称索引提供器。 */
    abstract override val symbolNamesProvider: CfirSymbolNamesProvider

    /**
     * 反序列化声明全部来自 `.cjo`，本身就是库侧视图，不需要另建一份。
     */
    final override val libraryDeclarationsNamesProvider: CfirSymbolNamesProvider
        get() = symbolNamesProvider

    /** 完整包名到包级反序列化器持有对象的缓存。 */
    private val contextCache = ConcurrentHashMap<String, List<PackageDeserializers>>()
    /** 已确认无法加载的完整包名集合。 */
    private val missingContexts = ConcurrentHashMap.newKeySet<String>()

    /** classId 到 class-like symbol 的缓存。 */
    private val classCache = ConcurrentHashMap<ClassId, CfirClassLikeSymbol<*>>()
    /** 同一 package 的多个显式 CJMP parent 可各自声明同一 classifier。 */
    private val classSymbolsCache = ConcurrentHashMap<ClassId, List<CfirClassLikeSymbol<*>>>()
    /** 已确认不存在的 classId 集合。 */
    private val missingClasses = ConcurrentHashMap.newKeySet<ClassId>()

    /** canonical context 同时包含 binding/module/source/mode；同名 target 不能跨图复用。 */
    private data class CallableContextKey(val context: CfirDeserializationContext, val callableId: CallableId)

    /** 顶层 callableId 到 callable symbol 列表的统一缓存。 */
    private val callableCache = ConcurrentHashMap<CallableContextKey, List<CfirCallableSymbol<*>>>()
    /** 顶层函数 callableId 到函数 symbol 列表的缓存。 */
    private val functionCache = ConcurrentHashMap<CallableId, List<CfirNamedFunctionSymbol>>()
    /** 顶层属性 callableId 到属性 symbol 列表的缓存。 */
    private val propertyCache = ConcurrentHashMap<CallableId, List<CfirPropertySymbol>>()
    /** 包名到顶层 extend 声明列表的缓存。 */
    private val extendCache = ConcurrentHashMap<FqName, List<CfirExtend>>()
    /** enum constructor 提升为顶层 callable 查询结果时使用的缓存。 */
    private val promotedEnumCallableCache = ConcurrentHashMap<CallableContextKey, List<CfirCallableSymbol<*>>>()

    /** 反序列化 scope 初始化使用的共享 scope session。 */
    private val scopeSession = ScopeSession()
    /** 已初始化包成员 scope 的包名集合。 */
    private val initializedPackageScopes = ConcurrentHashMap.newKeySet<FqName>()

    /** 加载指定完整包名的包级 deserializer；加载失败返回 null。 */
    protected abstract fun loadPackageDeserializers(packageFqName: String): List<PackageDeserializers>?

    /** 与 symbol header 的 postCompute 完成事务共同发布 class cache。 */
    protected abstract fun postComputeClassSymbol(classId: ClassId, publish: () -> Unit)

    /** 按 classId 查询库 class-like symbol，必要时递归跟随 public import re-export。 */
    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? {
        classCache[classId]?.let { return it }
        val loaded = getClassLikeSymbolsByClassId(classId).firstOrNull() ?: return null
        missingClasses.remove(classId)
        postComputeClassSymbol(classId) { classCache.putIfAbsent(classId, loaded) }
        return loaded
    }

    /** 多 parent matching 需要保留同一 ClassId 下各输入 parent 的原始 classifier。 */
    override fun getClassLikeSymbolsByClassId(classId: ClassId): List<CfirClassLikeSymbol<*>> {
        classSymbolsCache[classId]?.let { return it }
        if (classId in missingClasses || !mayHaveClassifier(classId)) return emptyList()
        val loaded = loadTopLevelClassSymbolsRecursively(classId, linkedSetOf()).distinct()
        if (loaded.isEmpty()) {
            missingClasses += classId
            return emptyList()
        }
        missingClasses.remove(classId)
        postComputeClassSymbol(classId) { classSymbolsCache.putIfAbsent(classId, loaded) }
        return loaded
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
        val loaded = deserializers.flatMap { packageDeserializers ->
            val indices = packageDeserializers.header.topLevelExtendIndices
            val reader = packageDeserializers.createDeclDeserializer()
            session.cjoDeserializationTimingObserverOrNull.measureCjoDeclaration(indices.size) {
                indices.mapNotNull { reader.deserializeDecl(it) as? CfirExtend }
            }
        }

        extendCache.putIfAbsent(packageFqName, loaded)
        return extendCache[packageFqName] ?: loaded
    }

    /**
     * 获取或创建指定完整包名的包级 deserializer。
     *
     * 成功加载后会初始化对应包成员 scope，确保 provider 查询路径与普通 scope 查询路径共享同一包 scope。
     */
    protected fun getOrCreateDeserializers(fullPkgName: String): List<PackageDeserializers>? {
        contextCache[fullPkgName]?.let { return it }
        if (fullPkgName in missingContexts) return null

        // Timing starts after the cache guard: a cache hit costs no loading, and counting it
        // dilutes "package load duration" into "symbol query duration". The first miss on a search
        // root also builds that root .cjo directory index (reading every .cjo header); that cost
        // lands in the first package-load sample, which is where it belongs - it is part of
        // "opening this library".
        return session.cjoDeserializationTimingObserverOrNull.measureCjoPackageLoad {
            loadDeserializersAndInitializeScope(fullPkgName)
        }
    }

    /**
     * The cache-miss load path of [getOrCreateDeserializers]; called inside its timing wrapper.
     */
    private fun loadDeserializersAndInitializeScope(fullPkgName: String): List<PackageDeserializers>? {
        val created = loadPackageDeserializers(fullPkgName)
        if (created.isNullOrEmpty()) {
            missingContexts += fullPkgName
            return null
        }

        missingContexts.remove(fullPkgName)
        contextCache.putIfAbsent(fullPkgName, created)

        val packageFqName = FqName(fullPkgName)
        initializePackageScope(packageFqName)

        return contextCache[fullPkgName] ?: created
    }

    /** 通过此 provider 的共享包 context 按 CJO 声明下标恢复 canonical class member。 */
    internal fun deserializeCjoInitializationDependency(
        fullPackageName: String,
        ownerDeclarationIndex: Int,
        declarationIndex: Int,
    ): CfirDeclaration? = deserializeCjoInitializationDependency(
        sourcePath = null,
        fullPackageName = fullPackageName,
        ownerDeclarationIndex = ownerDeclarationIndex,
        declarationIndex = declarationIndex,
    )

    internal fun deserializeCjoInitializationDependency(
        sourcePath: java.nio.file.Path?,
        fullPackageName: String,
        ownerDeclarationIndex: Int,
        declarationIndex: Int,
    ): CfirDeclaration? {
        val deserializers = getOrCreateDeserializers(fullPackageName).orEmpty()
        val selected = if (sourcePath != null) {
            deserializers.singleOrNull { it.sourcePath == sourcePath.toAbsolutePath().normalize() }
        } else deserializers.singleOrNull()
        return selected?.deserializeInitializationDependency(ownerDeclarationIndex, declarationIndex)
    }

    /** 按已选快照取得真实声明，源路径或唯一上下文缺失时不替换到其它 parent。 */
    internal fun deserializeCjoDeclaration(
        sourcePath: java.nio.file.Path?,
        fullPackageName: String,
        declarationIndex: Int,
    ): CfirDeclaration? {
        val candidates = getOrCreateDeserializers(fullPackageName).orEmpty()
        val selected = if (sourcePath != null) {
            candidates.singleOrNull { it.sourcePath == sourcePath.toAbsolutePath().normalize() }
        } else candidates.singleOrNull()
        return selected?.createDeclDeserializer()?.deserializeCanonicalDeclaration(declarationIndex)
    }

    internal fun deserializeCjoParameterDefaults(sourcePath: java.nio.file.Path?, fullPackageName: String, indices: List<Int>): Map<CfirValueParameter, CfirExpression?> {
        val candidates = getOrCreateDeserializers(fullPackageName).orEmpty()
        val selected = if (sourcePath != null) candidates.singleOrNull { it.sourcePath == sourcePath.toAbsolutePath().normalize() }
            else candidates.singleOrNull()
        return checkNotNull(selected).createDeclDeserializer().deserializeParameterDefaults(indices)
    }

    /** 以实际声明 provenance 和对象身份定位所属 context，不能把其它 parent 的正文交给此声明。 */
    protected fun importedSourceGraph(function: CfirFunction, fullPackageName: String): CfirSourceGraph? {
        val origin = function.cjoDeclarationOrigin ?: return null
        val path = java.nio.file.Path.of(origin.packagePath).toAbsolutePath().normalize()
        val selected = existingPackageContexts().singleOrNull { it.sourcePath == path && it.header.fullPkgName == fullPackageName }
            ?: return null
        return selected.sourceGraphOf(function, origin.declarationIndex)
    }

    protected open fun existingPackageContexts(): Sequence<PackageDeserializers> =
        contextCache.values.asSequence().flatten()

    private fun contentContextOf(declaration: CfirDeclaration): PackageDeserializers? =
        existingPackageContexts().singleOrNull { it.ownsDeclaration(declaration) }

    protected fun importedSourceHeader(function: CfirCallableDeclaration): CfirSourceImportedFunctionHeader? {
        val selected = contentContextOf(function) ?: return null
        return selected.sourceHeaderOf(function)
    }

    /** 匿名F没有d路径，按真实已发表owner选择原context/F token；不制造声明索引。 */
    protected fun importedAnonymousSourceGraph(function: org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction): CfirSourceGraph? {
        val selected = contentContextOf(function) ?: return null
        return selected.anonymousSourceGraphOf(function)
    }

    protected fun importedHeaderContents(declaration: CfirDeclaration): Boolean {
        val selected = contentContextOf(declaration) ?: return false
        selected.completeHeader(declaration)
        return true
    }

    protected fun discoverImportedFunctionHeaders(function: CfirFunction): Boolean {
        val selected = contentContextOf(function) ?: return false
        selected.discoverFunctionHeaders(function)
        return true
    }

    /** 真实 owner 身份选择已载入 context；匿名 F 不伪造路径/d，旧代或其它 provider 不参与查询。 */
    protected fun importedSourceBody(function: org.cangnova.cangjie.cfir.CfirFunctionBodyOwner): org.cangnova.cangjie.cfir.expressions.CfirBlock? {
        val declaration = function as CfirCallableDeclaration
        val selected = contentContextOf(declaration) ?: return null
        return selected.createDeclDeserializer().deserializeFunctionBody(declaration)
    }

    protected fun importedSourceInitializer(variable: org.cangnova.cangjie.cfir.declarations.CfirVariable): org.cangnova.cangjie.cfir.expressions.CfirExpression? {
        val selected = contentContextOf(variable) ?: return null
        return selected.createDeclDeserializer().deserializeVariableInitializer(variable)
    }

    /** 已归属声明、snapshot和context三者同代；只用路径定位候选，再验证实际装载身份。 */
    protected fun importedPackageSourceGraph(declaration: CfirDeclaration, snapshot: org.cangnova.cangjie.cfir.serialization.cjo.CjoLoadedPackage): CfirSourceGraph? {
        val origin = declaration.cjoDeclarationOrigin ?: return null
        val selected = existingPackageContexts().singleOrNull { it.snapshot === snapshot } ?: return null
        return selected.packageSourceGraphOf(declaration, origin.declarationIndex)
    }

    protected fun importedSourceExportSnapshot(
        declaration: CfirDeclaration,
        snapshot: org.cangnova.cangjie.cfir.serialization.cjo.CjoLoadedPackage,
        includeContent: Boolean,
    ): org.cangnova.cangjie.cfir.serialization.cjo.CjoSourceExportSnapshot? {
        val origin = declaration.cjoDeclarationOrigin ?: return null
        val selected = existingPackageContexts().singleOrNull { it.snapshot === snapshot } ?: return null
        return selected.sourceExportSnapshotOf(declaration, origin.declarationIndex, includeContent)
    }

    /** 被动完成代读取只在既有 context cache 准入，不按路径冷创建 deserializer。 */
    protected fun completedImportedSourceExportSnapshot(declaration: CfirDeclaration, includeContent: Boolean): org.cangnova.cangjie.cfir.serialization.cjo.CjoSourceExportSnapshot? =
        contentContextOf(declaration)?.completedSourceExportSnapshotOf(declaration, includeContent)

    /** 原 request 联合 source/binder/session 准入；不以同包名或仅 source 字节认领。 */
    protected fun importedDefinitionExportSnapshot(request: org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionSnapshotRequest,
        completedOnly: Boolean): org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionSnapshotResult {
        val owners = existingPackageContexts().filter { it.matchesDefinitionSource(request) }.toList()
        check(owners.size <= 1) { "A definition request resolves to multiple actual contexts" }
        val owner = owners.singleOrNull() ?: return org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionSnapshotResult.NotOwner
        return if (completedOnly) owner.completedDefinitionExportSnapshotOf(request) else owner.definitionExportSnapshotOf(request)
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
    private fun loadTopLevelClassSymbolsRecursively(
        classId: ClassId,
        visitingPackages: LinkedHashSet<FqName>,
        targetContext: CfirDeserializationContext? = null,
    ): List<CfirClassLikeSymbol<*>> {
        if (targetContext == null) {
            return getOrCreateDeserializers(classId.packageFqName.asString()).orEmpty().flatMap {
                loadTopLevelClassSymbolsRecursively(classId, visitingPackages, it.context)
            }.distinct()
        }
        check(classId.packageFqName.asString() == targetContext.header.fullPkgName)
        val reader = targetContext.createDeclDeserializer()
        val shortName = classId.shortClassName.asString()
        val indices = targetContext.header.topLevelClassifierNameToIndices[shortName].orEmpty()
        val direct = session.cjoDeserializationTimingObserverOrNull.measureCjoDeclaration(indices.size) {
            indices.mapNotNull { index ->
                (reader.deserializeSymbolForReference(index)?.cfir as? CfirClassLikeDeclaration)
                    ?.takeIf { declSymbolName(it) == shortName }?.symbol
            }
        }
        if (direct.isNotEmpty() || !visitingPackages.add(classId.packageFqName)) return direct
        return try {
            targetContext.header.fileImportEntries.flatMap { entry ->
                if (!entry.isPublicExportImport()) return@flatMap emptyList()
                val importedPackage = entry.importedPackageFqName() ?: return@flatMap emptyList()
                val importedName = if (entry.isAllUnder) classId.shortClassName else {
                    if (entry.exportedMemberName() != classId.shortClassName) return@flatMap emptyList()
                    entry.importedMemberName() ?: return@flatMap emptyList()
                }
                loadTopLevelClassSymbolsRecursively(
                    ClassId(importedPackage, importedName), visitingPackages, exportedContext(targetContext, importedPackage),
                )
            }.distinct()
        } finally {
            visitingPackages.remove(classId.packageFqName)
        }
    }

    /** actual snapshot 不借 ClassId-only cache，复用原声明/re-export traversal 与原循环守卫。 */
    internal fun getClassLikeSymbolInContext(classId: ClassId, context: CfirDeserializationContext): CfirClassLikeSymbol<*>? =
        loadTopLevelClassSymbolsRecursively(classId, linkedSetOf(), context).firstOrNull()

    private fun resolveTopLevelCallableSymbols(
        packageFqName: FqName,
        name: Name,
        visitingPackages: LinkedHashSet<FqName>,
        targetContext: CfirDeserializationContext? = null,
    ): List<CfirCallableSymbol<*>> {
        if (targetContext == null) {
            return getOrCreateDeserializers(packageFqName.asString()).orEmpty().flatMap {
                resolveTopLevelCallableSymbols(packageFqName, name, visitingPackages, it.context)
            }.distinct()
        }
        val context = targetContext.canonical()
        check(context.header.fullPkgName == packageFqName.asString())
        val cacheKey = CallableContextKey(context, CallableId(packageFqName, name))
        callableCache[cacheKey]?.let { return it }
        return context.materialization.read {
            val direct = loadDirectTopLevelCallableSymbols(context, name)
            val promoted = getPromotedTopLevelEnumConstructors(context, name)
            if (!visitingPackages.add(packageFqName)) return@read (direct + promoted).distinct()
            val exported = try {
                context.header.fileImportEntries.flatMap { entry ->
                    if (!entry.isPublicExportImport()) return@flatMap emptyList()
                    val importedPackage = entry.importedPackageFqName() ?: return@flatMap emptyList()
                    val importedName = if (entry.isAllUnder) name else {
                        if (entry.exportedMemberName() != name) return@flatMap emptyList()
                        entry.importedMemberName() ?: return@flatMap emptyList()
                    }
                    resolveTopLevelCallableSymbols(importedPackage, importedName, visitingPackages, exportedContext(context, importedPackage))
                }
            } finally {
                visitingPackages.remove(packageFqName)
            }
            val loaded = (direct + promoted + exported).distinct()
            context.materialization.publishWhenComplete { callableCache.putIfAbsent(cacheKey, loaded) }
            loaded
        }
    }

    /** 顶层 callable 只读取选定来源的索引，避免整个包物化。 */
    private fun loadDirectTopLevelCallableSymbols(context: CfirDeserializationContext, name: Name): List<CfirCallableSymbol<*>> {
        val indices = context.header.topLevelCallableNameToIndices[name.asString()].orEmpty()
        val reader = context.createDeclDeserializer()
        return session.cjoDeserializationTimingObserverOrNull.measureCjoDeclaration(indices.size) {
            indices.mapNotNull { reader.deserializeSymbol(it) as? CfirCallableSymbol<*> }
        }
    }

    /** 枚举构造器来自实际 wire 索引；没有名称匹配时不物化 enum header。 */
    private fun getPromotedTopLevelEnumConstructors(context: CfirDeserializationContext, name: Name): List<CfirCallableSymbol<*>> {
        val cacheKey = CallableContextKey(context, CallableId(FqName(context.header.fullPkgName), name))
        promotedEnumCallableCache[cacheKey]?.let { return it }
        val indices = context.header.promotedEnumConstructorNameToIndices[name.asString()].orEmpty()
        val reader = context.createDeclDeserializer()
        val symbols = indices.map { index ->
            val symbol = reader.deserializeSymbol(index)
            check(symbol is CfirEnumConstructorSymbol) { "Indexed enum constructor has the wrong wire family" }
            symbol
        }.distinct()
        context.materialization.publishWhenComplete { promotedEnumCallableCache.putIfAbsent(cacheKey, symbols) }
        return symbols
    }

    /** 重导出只沿真实 import 目标；不能扩大 named provider 的 own 包暴露面。 */
    private fun exportedContext(context: CfirDeserializationContext, packageName: FqName): CfirDeserializationContext =
        checkNotNull(context.packageContextOrNull(packageName.asString())) {
            "Cannot load CJO public import target " + packageName
        }

    /** 提取 class-like 声明对应的源码级名称文本。 */
    private fun declSymbolName(declaration: CfirClassLikeDeclaration): String = when (declaration) {
        is CfirClass -> declaration.name.asString()
        is CfirPrimitiveTypeDeclaration -> declaration.name.asString()
        is CfirBuiltInDeclaration -> declaration.name.asString()
        is CfirInterface -> declaration.name.asString()
        is CfirStruct -> declaration.name.asString()
        is CfirEnum -> declaration.name.asString()
        is CfirTypeAlias -> declaration.name.asString()
    }

    /**
     * 单个包的反序列化入口持有对象。
     *
     * 只缓存包头与上下文；声明 deserializer 每次查询按需新建，避免 owner 栈和递归检测状态跨查询共享。
     */
    protected class PackageDeserializers(
        /** 实际装载快照与context一对一；包名或路径相同不能替代其身份。 */
        val snapshot: org.cangnova.cangjie.cfir.serialization.cjo.CjoLoadedPackage,
        /** 当前包的共享反序列化上下文。 */
        val context: CfirDeserializationContext,
    ) {
        val header: CjoPackageHeader get() = snapshot.header
        val sourcePath: java.nio.file.Path get() = snapshot.sourcePath

        init {
            check(context.loadedPackage === snapshot && context.sourcePath == snapshot.sourcePath)
        }
        /**
         * 对齐 Kotlin provider 只缓存 package context 的所有权：
         * 具体 deserializer 必须按次创建，避免把 owner 栈和递归检测状态泄漏到并发查询之间。
         */
        /** 创建一次性的声明反序列化器。 */
        fun createDeclDeserializer() = context.createDeclDeserializer()

        /** 已发表的 d/F 身份用于正式内容路由，包 context 本体不跨层泄露。 */
        fun ownsDeclaration(declaration: CfirDeclaration): Boolean = context.publishedDeclarationRoot(declaration) != null
        fun completeHeader(declaration: CfirDeclaration) =
            context.publishedContentSource.completeHeader(checkNotNull(context.publishedDeclarationRoot(declaration)))

        fun sourceHeaderOf(function: CfirCallableDeclaration): CfirSourceImportedFunctionHeader? =
            context.completedSourceHeaders[function]

        fun discoverFunctionHeaders(function: CfirFunction) = CfirSourceGraphLoader(context).discoverFunctionHeaders(function)

        fun anonymousSourceGraphOf(function: org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction): CfirSourceGraph? {
            val identity = context.sourceFunctionIdentity(function)
            if (context.sourceHeaderRegistry.functions.getValue(identity).body.body == 0u) return null
            return CfirSourceGraphLoader(context).content(listOf(CjoSourceContentRoot.FunctionBody(identity))).graph
        }

        fun sourceGraphOf(function: CfirFunction, index: Int): CfirSourceGraph? {
            val declaration = context.createDeclDeserializer().deserializeCanonicalDeclaration(index)
            if (declaration !== function) return null
            val wire = checkNotNull(context.pkg.allDecls(index))
            if (wire.kind != PackageFormat.DeclKind.FuncDecl) return null
            val body = (wire.info(PackageFormat.FuncInfo()) as PackageFormat.FuncInfo).funcBody ?: return null
            if (body.body == 0u) return null
            val identity = context.sourceHeaderRegistry.functionByDeclaration.getValue((index + 1).toUInt())
            val graph = CfirSourceGraphLoader(context).content(listOf(CjoSourceContentRoot.FunctionBody(identity))).graph
            check(graph.functionBodies.containsKey(function)) { "Selected SOURCE body was not materialized" }
            return graph
        }

        fun sourceHeaderOf(function: CfirCallableDeclaration, index: Int): CfirSourceImportedFunctionHeader? {
            if (context.createDeclDeserializer().deserializeCanonicalDeclaration(index) !== function) return null
            context.completedSourceHeaders[function]?.let { return it }
            val wire = checkNotNull(context.pkg.allDecls(index))
            if (wire.kind != PackageFormat.DeclKind.FuncDecl) return null
            val body = (wire.info(PackageFormat.FuncInfo()) as PackageFormat.FuncInfo).funcBody ?: return null
            return CfirSourceGraphLoader(context).header(function, body)
        }

        fun packageSourceGraphOf(declaration: CfirDeclaration, index: Int): CfirSourceGraph? {
            if (context.createDeclDeserializer().deserializeCanonicalDeclaration(index) !== context.publishedDeclarationRoot(declaration)) return null
            return CfirSourceGraphLoader(context).load().graph
        }

        fun sourceExportSnapshotOf(
            declaration: CfirDeclaration,
            index: Int,
            includeContent: Boolean,
        ): org.cangnova.cangjie.cfir.serialization.cjo.CjoSourceExportSnapshot? {
            if (context.createDeclDeserializer().deserializeCanonicalDeclaration(index) !== context.publishedDeclarationRoot(declaration)) return null
            val loader = CfirSourceGraphLoader(context)
            if (includeContent) loader.load() else loader.loadHeaders()
            return context.sourceExportSnapshot(snapshot, includeContent)
        }

        fun completedSourceExportSnapshotOf(declaration: CfirDeclaration, includeContent: Boolean): org.cangnova.cangjie.cfir.serialization.cjo.CjoSourceExportSnapshot {
            val owner = checkNotNull(context.publishedDeclarationRoot(declaration))
            check(owner.symbol.isBound && owner.symbol.cfir === owner &&
                owner.contentSource === context.publishedContentSource) {
                "Completed SOURCE export requires the actual canonical declaration and content owner"
            }
            return context.sourceExportSnapshot(snapshot, includeContent)
        }

        fun matchesDefinitionSource(request: org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionSnapshotRequest): Boolean =
            snapshot === request.source && context.matchesDefinitionSource(request)

        fun definitionExportSnapshotOf(request: org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionSnapshotRequest): org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionSnapshotResult =
            CfirSourceGraphLoader(context).definitionSnapshot(request)

        fun completedDefinitionExportSnapshotOf(request: org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionSnapshotRequest): org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionSnapshotResult {
            context.definitionRequestViolation(request)?.let { return org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionSnapshotResult.Failure(it) }
            return context.completedDefinitionSnapshot(request)
                ?: org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionSnapshotResult.Failure("The actual definition request has no successful completed scope")
        }

        /** 先以真实 class owner 构造声明树，再从共享 cache 返回指定 CJO index 的节点。 */
        fun deserializeInitializationDependency(
            ownerDeclarationIndex: Int,
            declarationIndex: Int,
        ): CfirDeclaration? = context.createDeclDeserializer().deserializeInitializationDependency(
            ownerDeclarationIndex,
            declarationIndex,
        )
    }
}

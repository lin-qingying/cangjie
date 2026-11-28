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
    private val contextCache = ConcurrentHashMap<String, List<PackageDeserializers>>()
    /** 已确认无法加载的完整包名集合。 */
    private val missingContexts = ConcurrentHashMap.newKeySet<String>()

    /** classId 到 class-like symbol 的缓存。 */
    private val classCache = ConcurrentHashMap<ClassId, CfirClassLikeSymbol<*>>()
    /** 同一 package 的多个显式 CJMP parent 可各自声明同一 classifier。 */
    private val classSymbolsCache = ConcurrentHashMap<ClassId, List<CfirClassLikeSymbol<*>>>()
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
    protected abstract fun loadPackageDeserializers(packageFqName: String): List<PackageDeserializers>?

    /** 按 classId 查询库 class-like symbol，必要时递归跟随 public import re-export。 */
    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? {
        classCache[classId]?.let { return it }
        val loaded = getClassLikeSymbolsByClassId(classId).firstOrNull() ?: return null
        missingClasses.remove(classId)
        classCache.putIfAbsent(classId, loaded)
        return classCache[classId] ?: loaded
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
        classSymbolsCache.putIfAbsent(classId, loaded)
        return classSymbolsCache[classId] ?: loaded
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
            val declDeserializer = packageDeserializers.createDeclDeserializer()
            packageDeserializers.header.topLevelExtendIndices.mapNotNull { declIndex ->
                declDeserializer.deserializeDecl(declIndex) as? CfirExtend
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
        val candidates = getOrCreateDeserializers(fullPackageName).orEmpty()
        val selected = candidates.singleOrNull { it.sourcePath == path } ?: return null
        return selected.sourceGraphOf(function, origin.declarationIndex)
    }

    private fun contentContextOf(declaration: CfirDeclaration): PackageDeserializers? =
        contextCache.values.asSequence().flatten().singleOrNull { it.ownsDeclaration(declaration) }

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
        val selected = getOrCreateDeserializers(snapshot.header.fullPkgName).orEmpty()
            .singleOrNull { it.snapshot === snapshot } ?: return null
        return selected.packageSourceGraphOf(declaration, origin.declarationIndex)
    }

    protected fun importedSourceExportSnapshot(
        declaration: CfirDeclaration,
        snapshot: org.cangnova.cangjie.cfir.serialization.cjo.CjoLoadedPackage,
        includeContent: Boolean,
    ): org.cangnova.cangjie.cfir.serialization.cjo.CjoSourceExportSnapshot? {
        val origin = declaration.cjoDeclarationOrigin ?: return null
        val selected = getOrCreateDeserializers(snapshot.header.fullPkgName).orEmpty()
            .singleOrNull { it.snapshot === snapshot } ?: return null
        return selected.sourceExportSnapshotOf(declaration, origin.declarationIndex, includeContent)
    }

    /** 被动完成代读取只在既有 context cache 准入，不按路径冷创建 deserializer。 */
    protected fun completedImportedSourceExportSnapshot(declaration: CfirDeclaration, includeContent: Boolean): org.cangnova.cangjie.cfir.serialization.cjo.CjoSourceExportSnapshot? =
        contentContextOf(declaration)?.completedSourceExportSnapshotOf(declaration, includeContent)

    /** 原 request 联合 source/binder/session 准入；不以同包名或仅 source 字节认领。 */
    protected fun importedDefinitionExportSnapshot(request: org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionSnapshotRequest,
        completedOnly: Boolean): org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionSnapshotResult {
        val owners = contextCache.values.asSequence().flatten().filter { it.matchesDefinitionSource(request) }.toList()
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
    ): List<CfirClassLikeSymbol<*>> {
        val direct = loadDirectTopLevelClassSymbols(classId)
        if (!visitingPackages.add(classId.packageFqName)) return direct
        val exported = loadExportedTopLevelClassSymbols(classId, visitingPackages)
        visitingPackages.remove(classId.packageFqName)
        return (direct + exported).distinct()
    }

    /** 从同包的每份独立 CJO 快照中读取物理顶层 class-like 声明。 */
    private fun loadDirectTopLevelClassSymbols(classId: ClassId): List<CfirClassLikeSymbol<*>> {
        val deserializers = getOrCreateDeserializers(classId.packageFqName.asString()) ?: return emptyList()
        val shortName = classId.shortClassName.asString()
        return deserializers.flatMap { packageDeserializers ->
            val declDeserializer = packageDeserializers.createDeclDeserializer()
            packageDeserializers.header.topLevelClassifierNameToIndices[shortName].orEmpty().mapNotNull { declIndex ->
                val decl = declDeserializer.deserializeDecl(declIndex)
                if (decl is CfirClassLikeDeclaration && decl.symbol is CfirClassLikeSymbol<*> && declSymbolName(decl) == shortName) {
                    decl.symbol
                } else {
                    null
                }
            }
        }
    }

    /**
     * `public import` 导出的 classifier 在本包里没有物理 `Decl`，
     * 因此必须沿着导出 import 递归到真实声明包取 symbol。
     */
    private fun loadExportedTopLevelClassSymbols(
        classId: ClassId,
        visitingPackages: LinkedHashSet<FqName>,
    ): List<CfirClassLikeSymbol<*>> {
        val headers = getOrCreateDeserializers(classId.packageFqName.asString())
            ?.map { it.header }
            .orEmpty()
        val result = mutableListOf<CfirClassLikeSymbol<*>>()
        for (entry in headers.flatMap { it.fileImportEntries }) {
            if (!entry.isPublicExportImport()) continue
            val importedPackageFqName = entry.importedPackageFqName() ?: continue
            if (entry.isAllUnder) {
                result += loadTopLevelClassSymbolsRecursively(
                    ClassId(importedPackageFqName, classId.shortClassName),
                    visitingPackages,
                )
                continue
            }

            val exportedName = entry.exportedMemberName() ?: continue
            if (exportedName != classId.shortClassName) continue

            val importedMemberName = entry.importedMemberName() ?: continue
            result += loadTopLevelClassSymbolsRecursively(
                ClassId(importedPackageFqName, importedMemberName),
                visitingPackages,
            )
        }
        return result.distinct()
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
        return deserializers.flatMap { packageDeserializers ->
            val declDeserializer = packageDeserializers.createDeclDeserializer()
            packageDeserializers.header.topLevelNameToIndices[name.asString()]
                .orEmpty()
                .mapNotNull { declIndex ->
                    val decl = declDeserializer.deserializeDecl(declIndex)
                    (decl as? CfirCallableDeclaration)?.symbol as? CfirCallableSymbol<*>
                }
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
        val headers = getOrCreateDeserializers(packageFqName.asString())?.map { it.header }.orEmpty()
        val result = mutableListOf<CfirCallableSymbol<*>>()

        for (entry in headers.flatMap { it.fileImportEntries }) {
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

        for (className in deserializers.flatMap { it.header.topLevelClassNames }.distinct()) {
            val classId = ClassId(packageFqName, className)
            for (classSymbol in getClassLikeSymbolsByClassId(classId)) {
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
        }

        val distinct = result.distinct()
        promotedEnumCallableCache.putIfAbsent(callableId, distinct)
        return promotedEnumCallableCache[callableId] ?: distinct
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
        private val context: CfirDeserializationContext,
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
        fun ownsDeclaration(declaration: CfirDeclaration): Boolean = context.isPublishedDeclaration(declaration)
        fun completeHeader(declaration: CfirDeclaration) = context.publishedContentSource.completeHeader(declaration)

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
            if (context.createDeclDeserializer().deserializeCanonicalDeclaration(index) !== declaration) return null
            return CfirSourceGraphLoader(context).load().graph
        }

        fun sourceExportSnapshotOf(
            declaration: CfirDeclaration,
            index: Int,
            includeContent: Boolean,
        ): org.cangnova.cangjie.cfir.serialization.cjo.CjoSourceExportSnapshot? {
            if (context.createDeclDeserializer().deserializeCanonicalDeclaration(index) !== declaration) return null
            val loader = CfirSourceGraphLoader(context)
            if (includeContent) loader.load() else loader.loadHeaders()
            return context.sourceExportSnapshot(snapshot, includeContent)
        }

        fun completedSourceExportSnapshotOf(declaration: CfirDeclaration, includeContent: Boolean): org.cangnova.cangjie.cfir.serialization.cjo.CjoSourceExportSnapshot {
            check(declaration.symbol.isBound && declaration.symbol.cfir === declaration &&
                declaration.contentSource === context.publishedContentSource && ownsDeclaration(declaration)) {
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

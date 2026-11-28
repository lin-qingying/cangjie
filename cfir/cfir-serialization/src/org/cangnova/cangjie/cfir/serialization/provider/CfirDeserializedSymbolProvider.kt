package org.cangnova.cangjie.cfir.serialization.provider

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.cjoDeclarationOrigin
import org.cangnova.cangjie.cfir.source.CfirSourceGraph
import org.cangnova.cangjie.cfir.source.CfirSourceImportedFunctionHeader
import org.cangnova.cangjie.cfir.source.CfirImportedSourceGraphProvider
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.deserialization.ModuleDataProvider
import org.cangnova.cangjie.cfir.deserialization.cjmpModuleDataProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjmpCommonPartLoadGate
import org.cangnova.cangjie.cfir.serialization.cjo.CjoLoadedPackage
import org.cangnova.cangjie.cfir.serialization.cjo.CjoLoadedPackageSnapshotProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSourceExportSnapshotProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSourceExportSnapshot
import org.cangnova.cangjie.cfir.serialization.cjo.CjoFormatVersionPolicy
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeserializationMode
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext
import org.cangnova.cangjie.cfir.session.CfirCjmpLoadDiagnostic
import org.cangnova.cangjie.cfir.session.CfirCjmpLoadDiagnosticsCollector
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.resolve.providers.cfirProvider
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.CjmpLanguageFeature
import org.cangnova.cangjie.supportsCjmpFeature
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.name.FqName
import java.nio.file.Path

/**
 * 基于 CJO 包管理器的反序列化 symbol provider 具体实现。
 *
 * 库内容在本代内按需读取并保持稳定。CJO、sidecar 或搜索根变化后，调用方必须重建
 * CjoSearchPath、CjoManager、provider 及其所属 session/module；不能只清 contextCache，
 * 否则名称、符号、包 scope、extend 与缺失结果仍可能引用旧声明。
 */
class CfirDeserializedSymbolProvider(
    /** 当前 CFIR session。 */
    session: CfirSession,
    /** `.cjo` 包管理器，负责包头和完整包数据加载。 */
    private val cjoManager: CjoManager,
    /** 包成员 scope provider。 */
    cangjieScopeProvider: CfirCangJieScopeProvider,
    /** 库模块数据。 */
    libraryModuleData: CfirModuleData,
    /**
     * 按 cjo 文件路径细分模块归属的提供器（Kotlin `ModuleDataProvider.getModuleData(path)` 对位）。
     *
     * CJMP specific 编译中 common part cjo 属于 depends-on 模块（D1：common/specific 双模块），
     * 其声明必须带 depends-on 模块数据，配对查找（`CfirCjmpResolver`）才能按 refinement 依赖识别它；
     * 必须由原装配方提供路径 owner 服务；缺失服务或无法确认实际来源时明确拒绝物化。
     */
    private val moduleDataProvider: ModuleDataProvider? = null,
    /**
     * 解析导入包（跨包 `FullId`）时使用的 `.cjo` 管理器。
     *
     * 库模块的 `.cjo` 会在包头依赖表里引用 SDK 的 `std.*` 包等外部包；这些包不在库模块根里，
     * 但本 provider 也不能因此把它们当作自己的包对外暴露（会与 builtins provider 重复，
     * 表现为 `Array` / `Byte` 等 `ambiguous use`）。因此包暴露与包依赖解析必须分开：
     * [cjoManager] 决定本 provider 的包集合，本字段决定反序列化期间导入包能从哪里加载。
     */
    private val dependencyCjoManager: CjoManager = cjoManager,
    private val deserializationMode: CjoDeserializationMode = CjoDeserializationMode.FULL,
    /** LL 的导入 owner 由实际依赖 session 提供；配置此入口后缺失目标直接拒绝，不借 consumer manager 造副本。 */
    private val importedPackageContextProvider: CjoPackageContextProvider? = null,
    binding: CjoDeserializationBinding? = null,
) : AbstractCfirDeserializedSymbolProvider(
    session = session,
    cangjieScopeProvider = cangjieScopeProvider,
    libraryModuleData = libraryModuleData,
), CjoPackageContextProvider, CjoSourceExportSnapshotProvider, CfirImportedSourceGraphProvider {

    /** 装配身份由原 producer 创建一次；所有 mode/owner 派生保持原服务。 */
    val deserializationBinding: CjoDeserializationBinding = binding ?: CjoDeserializationBinding(
        session, cjoManager, cangjieScopeProvider, libraryModuleData,
        checkNotNull(moduleDataProvider) { "Named CJO producer requires its original ModuleDataProvider" },
        dependencyCjoManager, importedPackageContextProvider,
    )

    override fun sourceGraphOf(function: CfirFunction): CfirSourceGraph? {
        val origin = function.cjoDeclarationOrigin ?: return if (function is org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction)
            importedAnonymousSourceGraph(function) else null
        val loaded = cjoManager.loadedPackageSnapshot(Path.of(origin.packagePath)) ?: return null
        return importedSourceGraph(function, loaded.header.fullPkgName)
    }

    override fun sourceHeaderOf(function: CfirCallableDeclaration): CfirSourceImportedFunctionHeader? {
        return importedSourceHeader(function)
    }

    override fun sourceHeaderContentsOf(declaration: CfirDeclaration): Boolean = importedHeaderContents(declaration)
    override fun discoverSourceFunctionHeadersOf(function: CfirFunction): Boolean = discoverImportedFunctionHeaders(function)

    override fun sourceBodyOf(function: org.cangnova.cangjie.cfir.CfirFunctionBodyOwner): org.cangnova.cangjie.cfir.expressions.CfirBlock? =
        importedSourceBody(function)

    override fun sourceInitializerOf(variable: org.cangnova.cangjie.cfir.declarations.CfirVariable): org.cangnova.cangjie.cfir.expressions.CfirExpression? =
        importedSourceInitializer(variable)

    override fun packageSourceGraphOf(declaration: CfirDeclaration): CfirSourceGraph? {
        val origin = declaration.cjoDeclarationOrigin ?: return null
        val snapshot = cjoManager.loadedPackageSnapshot(Path.of(origin.packagePath)) ?: return null
        return importedPackageSourceGraph(declaration, snapshot)
    }

    /** 按反序列化声明记录的来源路径取得同一代 CJO 快照；不触发重新搜索或文件读取。 */
    override fun loadedPackageSnapshot(sourcePath: Path): CjoLoadedPackage? = cjoManager.loadedPackageSnapshot(sourcePath)

    override fun sourceExportSnapshotOf(declaration: CfirDeclaration, includeContent: Boolean): CjoSourceExportSnapshot? {
        val origin = declaration.cjoDeclarationOrigin ?: return null
        val snapshot = cjoManager.loadedPackageSnapshot(Path.of(origin.packagePath)) ?: return null
        return importedSourceExportSnapshot(declaration, snapshot, includeContent)
    }

    override fun completedSourceExportSnapshotOf(declaration: CfirDeclaration, includeContent: Boolean): CjoSourceExportSnapshot? =
        completedImportedSourceExportSnapshot(declaration, includeContent)

    override fun definitionExportSnapshotOf(request: org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionSnapshotRequest): org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionSnapshotResult =
        importedDefinitionExportSnapshot(request, completedOnly = false)

    override fun completedDefinitionExportSnapshotOf(request: org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionSnapshotRequest): org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionSnapshotResult =
        importedDefinitionExportSnapshot(request, completedOnly = true)

    /** 此 CJO provider 的加载事实随 provider 暴露给所有消费侧分析器。 */
    private val cjmpLoadDiagnostics = CfirCjmpLoadDiagnosticsCollector()

    /** 当前 provider 暴露的反序列化名称索引。 */
    override val symbolNamesProvider: CfirSymbolNamesProvider =
        CfirDeserializedSymbolNamesProvider(cjoManager, deserializationBinding, deserializationMode)

    init {
        if (binding == null) {
            dependencyCjoManager.deserializedSymbolLookups.register(deserializationBinding, deserializationMode, this)
        } else {
            binding.requireImportedRouterAuthority(importedPackageContextProvider)
            require(binding.session === session && binding.ownManager === cjoManager &&
                binding.dependencyManager === dependencyCjoManager && binding.scopeProvider === cangjieScopeProvider &&
                binding.libraryModuleData === libraryModuleData && binding.moduleDataProvider === moduleDataProvider) {
                "Derived CJO provider does not match its original binding"
            }
        }
    }

    /** 外部并发查询不能从 provider 的 class cache 观察 annotation 完成中的 header。 */
    override fun getClassLikeSymbolByClassId(classId: org.cangnova.cangjie.name.ClassId):
        org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol<*>? {
        return dependencyCjoManager.deserializedSymbolLookups.materialization().read {
            super.getClassLikeSymbolByClassId(classId)
        }
    }

    override fun getClassLikeSymbolsByClassId(classId: org.cangnova.cangjie.name.ClassId):
        List<org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol<*>> =
        dependencyCjoManager.deserializedSymbolLookups.materialization().read {
            super.getClassLikeSymbolsByClassId(classId)
        }

    override fun postComputeClassSymbol(classId: org.cangnova.cangjie.name.ClassId, publish: () -> Unit) {
        dependencyCjoManager.deserializedSymbolLookups.materialization().publishWhenComplete(publish)
    }

    /** 返回由本 provider 加载指定包时产生的事实。 */
    override fun getCjmpLoadDiagnostics(packageFqName: FqName): List<CfirCjmpLoadDiagnostic> {
        // 名称索引可先触发 CJO 头加载并拒绝包；同步 manager 事实可覆盖尚未进入 declaration deserializer 的失败。
        cjoManager.loadDiagnostics(packageFqName.asString()).forEach(cjmpLoadDiagnostics::record)
        return cjmpLoadDiagnostics.diagnosticsForPackage(packageFqName.asString())
    }

    /**
     * 按消费端的 depends-on 图和 CJMP 设置评估该包的 common-part 加载门。
     *
     * 二进制库 provider 属于独立库 session；common CJO 的角色必须由消费端 ModuleDataProvider
     * 按真实来源路径判定，不能从二进制 provider 自身的 session 设置推断。
     */
    override fun getCjmpLoadDiagnostics(
        packageFqName: FqName,
        consumerSession: CfirSession,
    ): List<CfirCjmpLoadDiagnostic> {
        val diagnostics = getCjmpLoadDiagnostics(packageFqName).toMutableSet()
        val consumerModuleDataProvider = consumerSession.cjmpModuleDataProvider ?: return diagnostics.toList()
        val cjmpSettings = consumerSession.cjmpSettings
        if (cjmpSettings.mode != CfirCjmpMode.SPECIFIC) return diagnostics.toList()

        val loadedPackages = cjoManager.loadPackageSnapshots(packageFqName.asString())
        cjoManager.loadDiagnostics(packageFqName.asString()).forEach(cjmpLoadDiagnostics::record)
        diagnostics += cjmpLoadDiagnostics.diagnosticsForPackage(packageFqName.asString())

        val checksFeaturesAndOptions = consumerSession.languageVersionSettings
            .supportsCjmpFeature(CjmpLanguageFeature.CommonPartFeaturesAndOptionsGate)

        for (loaded in loadedPackages) {
            val commonModuleData = consumerModuleDataProvider.getModuleData(loaded.sourcePath) ?: continue
            if (commonModuleData !in consumerModuleDataProvider.dependsOnDependenciesModuleData) continue

            CjoFormatVersionPolicy.Compilation(consumerSession.languageVersionSettings)
                .checkCommonPart(packageFqName.asString(), loaded.header.cjoVersion)
                ?.let(diagnostics::add)

            if (checksFeaturesAndOptions) {
                if (loaded.header.fileFeatures.isNotEmpty()) {
                    val specificFeatures = buildSet {
                        addAll(cjmpSettings.packageFeaturesByPackage[packageFqName.asString()].orEmpty())
                        consumerSession.cfirProvider.getCfirFilesByPackage(packageFqName)
                            .asSequence()
                            .filter { it.moduleData == consumerSession.moduleData }
                            .flatMap { it.featuresDirective?.featureIds.orEmpty().asSequence() }
                            .mapTo(this) { it }
                    }
                    CjmpCommonPartLoadGate.checkFeaturesSubset(
                        packageName = packageFqName.asString(),
                        commonFileFeatures = loaded.header.fileFeatures,
                        specificFeatures = specificFeatures,
                    ).forEach(diagnostics::add)
                }
                CjmpCommonPartLoadGate.checkOptions(
                    packageName = packageFqName.asString(),
                    options = loaded.header.options,
                    debug = cjmpSettings.moduleDebug,
                    optLevel = cjmpSettings.moduleOptLevel,
                ).forEach(diagnostics::add)
            }
        }

        return diagnostics.toList()
    }

    /** 判断指定包是否存在对应 `.cjo` 数据。 */
    override fun hasPackage(fqName: FqName): Boolean = cjoManager.hasPackage(fqName)

    /** 加载指定包名的包头、FlatBuffers package 和共享反序列化上下文。 */
    override fun loadPackageDeserializers(packageFqName: String): List<PackageDeserializers>? {
        val loadedPackages = cjoManager.loadPackageSnapshots(packageFqName)
        cjoManager.loadDiagnostics(packageFqName).forEach(cjmpLoadDiagnostics::record)
        if (loadedPackages.isEmpty()) return null

        return loadedPackages.map { loaded ->
            val context = createPackageContext(loaded, deserializationMode)
            recordLoadGateDiagnostics(packageFqName, loaded.sourcePath, context.moduleData, loaded.header)
            PackageDeserializers(loaded, context)
        }
    }

    internal fun selectImportedSnapshot(requestingModule: CfirModuleData, fullPackageName: String): CjoLoadedPackage? =
        deserializationBinding.selectOwnSnapshot(fullPackageName, requestingModule)

    /** named provider 的暴露面只读自身 roots；跨模块选择只能检查此方法返回的真实资源。 */
    fun getOwnPackageContext(packageName: String, mode: CjoDeserializationMode): CfirDeserializationContext? =
        deserializationBinding.getOwnPackageContext(packageName, mode)

    override fun getPackageContext(packageName: String, mode: CjoDeserializationMode): CfirDeserializationContext? =
        deserializationBinding.getPackageContext(packageName, mode)

    /** actual-file 读取复用本 binding 与 manager，版本/包名仍由 snapshot producer 检查。 */
    fun getOwnPackageContext(packageName: String, sourcePath: java.nio.file.Path, content: ByteArray, mode: CjoDeserializationMode): CfirDeserializationContext {
        checkNotNull(deserializationBinding.moduleDataProvider.getModuleData(sourcePath)) {
            "No module owner for CJO " + sourcePath
        }
        return createPackageContext(cjoManager.loadPackageSnapshot(packageName, sourcePath, content), mode)
    }

    override fun existingPackageContexts(): Sequence<PackageDeserializers> =
        dependencyCjoManager.deserializedSymbolLookups.existingContexts(deserializationBinding, deserializationMode)
            .mapNotNull { context -> context.loadedPackage?.let { PackageDeserializers(it, context) } }

    private fun createPackageContext(loaded: CjoLoadedPackage, mode: CjoDeserializationMode): CfirDeserializationContext =
        deserializationBinding.contextForSnapshot(loaded, mode)

    /**
     * 收集本包触发的 CJMP 加载门诊断。
     *
     * features 子集门与编译选项门只对显式 common-part 输入且归属 depends-on 模块的快照生效。
     * 声明属性、options 或 features 描述 CJO 内容，不能代替加载来源和模块依赖角色。
     */
    private fun recordLoadGateDiagnostics(
        requestedPackageName: String,
        sourcePath: java.nio.file.Path,
        moduleData: CfirModuleData,
        header: CjoPackageHeader,
    ) {
        val settings = session.cjmpSettings
        if (!session.languageVersionSettings.supportsCjmpFeature(CjmpLanguageFeature.CommonPartFeaturesAndOptionsGate)) return
        if (settings.mode != org.cangnova.cangjie.cfir.session.CfirCjmpMode.SPECIFIC) return
        if (!cjoManager.isExplicitCommonPartPath(sourcePath)) return
        if (moduleData !in moduleDataProvider?.dependsOnDependenciesModuleData.orEmpty()) return
        CjmpCommonPartLoadGate.checkFeaturesSubset(
            packageName = requestedPackageName,
            commonFileFeatures = header.fileFeatures,
            specificFeatures = settings.packageFeaturesByPackage[requestedPackageName].orEmpty(),
        ).forEach(cjmpLoadDiagnostics::record)
        CjmpCommonPartLoadGate.checkOptions(
            packageName = requestedPackageName,
            options = header.options,
            debug = settings.moduleDebug,
            optLevel = settings.moduleOptLevel,
        ).forEach(cjmpLoadDiagnostics::record)
    }
}

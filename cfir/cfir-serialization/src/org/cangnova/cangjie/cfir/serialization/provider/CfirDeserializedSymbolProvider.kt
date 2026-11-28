package org.cangnova.cangjie.cfir.serialization.provider

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.deserialization.ModuleDataProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjmpCommonPartLoadGate
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoLoadedPackage
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeserializationMode
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cjmpLoadDiagnostics
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.name.FqName

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
), CjoPackageContextProvider {

    /** 装配身份由原 producer 创建一次；所有 mode/owner 派生保持原服务。 */
    val deserializationBinding: CjoDeserializationBinding = binding ?: CjoDeserializationBinding(
        session, cjoManager, cangjieScopeProvider, libraryModuleData,
        checkNotNull(moduleDataProvider) { "Named CJO producer requires its original ModuleDataProvider" },
        dependencyCjoManager, importedPackageContextProvider,
    )

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

    override fun postComputeClassSymbol(classId: org.cangnova.cangjie.name.ClassId, publish: () -> Unit) {
        dependencyCjoManager.deserializedSymbolLookups.materialization().publishWhenComplete(publish)
    }

    /** 判断指定包是否存在对应 `.cjo` 数据。 */
    override fun hasPackage(fqName: FqName): Boolean = cjoManager.hasPackage(fqName)

    /** 加载指定包名的包头、FlatBuffers package 和共享反序列化上下文。 */
    override fun loadPackageDeserializers(packageFqName: String): PackageDeserializers? {
        val loaded = cjoManager.loadPackageSnapshot(packageFqName)
        if (loaded == null) {
            // 版本门 / 包名门拒绝装载时，拒绝原因随 null 结果上浮到收集器（G17）
            session.cjmpLoadDiagnostics?.let { collector ->
                cjoManager.loadDiagnostics(packageFqName).forEach(collector::record)
            }
            return null
        }
        recordLoadGateDiagnostics(loaded.header)
        return PackageDeserializers(loaded.header, createPackageContext(loaded, deserializationMode))
    }

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

    private fun createPackageContext(loaded: CjoLoadedPackage, mode: CjoDeserializationMode): CfirDeserializationContext =
        deserializationBinding.contextForSnapshot(loaded, mode)

    /**
     * 收集本包触发的 CJMP 加载门诊断（G17 通道）。
     *
     * - 版本门 / 包名门由 [cjoManager] 在快照装载时判定，违反即拒绝装载（见 [loadPackageDeserializers]）；
     * - features 子集门与编译选项门只在 **specific 模式加载带 CJMP 内容的 common part cjo** 时判定
     *  （官方 `PreloadCommonPartOfPackage` 只在 specific 编译加载 common part 时跑这些门）——
     *  普通库加载（含 stdlib）不做 features/选项比对。
     */
    private fun recordLoadGateDiagnostics(header: CjoPackageHeader) {
        val settings = session.cjmpSettings
        // 无外显通道的装配（LL/单测会话）不收集
        val collector = session.cjmpLoadDiagnostics ?: return
        if (settings.mode != org.cangnova.cangjie.cfir.session.CfirCjmpMode.SPECIFIC) return
        if (!header.isCjmpCommonPart) return
        CjmpCommonPartLoadGate.checkFeaturesSubset(
            packageName = header.fullPkgName,
            commonFileFeatures = header.fileFeatures,
            specificFeatures = settings.packageFeatures,
        ).forEach(collector::record)
        CjmpCommonPartLoadGate.checkOptions(
            packageName = header.fullPkgName,
            options = header.options,
            debug = settings.moduleDebug,
            optLevel = CjmpCommonPartLoadGate.optLevelOrdinal(settings.moduleOptLevel),
        ).forEach(collector::record)
    }
}

package org.cangnova.cangjie.cfir.serialization.provider

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.deserialization.ModuleDataProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjmpCommonPartLoadGate
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
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
     * 为 null 或路径无专属模块时统一归入 [libraryModuleData]。
     */
    private val moduleDataProvider: ModuleDataProvider? = null,
) : AbstractCfirDeserializedSymbolProvider(
    session = session,
    cangjieScopeProvider = cangjieScopeProvider,
    libraryModuleData = libraryModuleData,
) {

    /** 当前 provider 暴露的反序列化名称索引。 */
    override val symbolNamesProvider: CfirSymbolNamesProvider =
        CfirDeserializedSymbolNamesProvider(cjoManager)

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
        val header = loaded.header
        val pkg = loaded.pkg

        val context = CfirDeserializationContext(
            pkg = pkg,
            header = header,
            moduleData = moduleDataFor(loaded.sourcePath),
            cjoManager = cjoManager,
            classSymbolLookup = this,
            sourcePath = loaded.sourcePath,
        )
        recordLoadGateDiagnostics(header)
        return PackageDeserializers(header, context)
    }

    /** cjo 文件所属模块：专属路径过滤命中的模块优先，否则为库模块。 */
    private fun moduleDataFor(sourcePath: java.nio.file.Path?): CfirModuleData =
        moduleDataProvider?.getModuleData(sourcePath) ?: libraryModuleData

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

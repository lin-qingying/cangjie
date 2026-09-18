package org.cangnova.cangjie.cfir.serialization.provider

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext
import org.cangnova.cangjie.cfir.session.CfirSession
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
        val loaded = cjoManager.loadPackageSnapshot(packageFqName) ?: return null
        val header = loaded.header
        val pkg = loaded.pkg

        val context = CfirDeserializationContext(
            pkg = pkg,
            header = header,
            moduleData = libraryModuleData,
            cjoManager = cjoManager,
            sourcePath = loaded.sourcePath,
        )
        return PackageDeserializers(header, context)
    }
}

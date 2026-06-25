package org.cangnova.cangjie.cfir.serialization.provider

import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjoExportedTopLevelNamesResolver
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 基于 `.cjo` 反序列化的符号名称提供器。
 *
 * 从 `CjoPackageHeader` 中提取顶层名称，用于快速过滤。
 */
class CfirDeserializedSymbolNamesProvider(
    /** 负责读取 `.cjo` 包头并枚举包名的管理器。 */
    private val cjoManager: CjoManager,
) : CfirSymbolNamesProvider() {
    /** 递归解析 public import re-export 后的顶层名称视图。 */
    private val exportedTopLevelNamesResolver = CjoExportedTopLevelNamesResolver(cjoManager)

    /** 返回当前搜索路径中可用的包名集合。 */
    override fun getPackageNames(): Set<String>? =
        cjoManager.getAvailablePackageNames().mapTo(linkedSetOf()) { it.asString() }

    /** `.cjo` 名称提供器不按 classifier 查询提前缩小包名集合。 */
    override val hasSpecificClassifierPackageNamesComputation: Boolean
        get() = false

    /** 返回指定包导出的顶层 classifier 名称集合。 */
    override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? {
        return exportedTopLevelNamesResolver.resolve(packageFqName).classifierNames
    }

    /** `.cjo` 名称提供器不按 callable 查询提前缩小包名集合。 */
    override val hasSpecificCallablePackageNamesComputation: Boolean
        get() = false

    /** 返回指定包导出的顶层 callable 名称集合。 */
    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? {
        return exportedTopLevelNamesResolver.resolve(packageFqName).callableNames
    }
}

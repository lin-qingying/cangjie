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
    private val cjoManager: CjoManager,
) : CfirSymbolNamesProvider() {
    private val exportedTopLevelNamesResolver = CjoExportedTopLevelNamesResolver(cjoManager)

    override fun getPackageNames(): Set<String>? =
        cjoManager.getAvailablePackageNames().mapTo(linkedSetOf()) { it.asString() }

    override val hasSpecificClassifierPackageNamesComputation: Boolean
        get() = false

    override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? {
        return exportedTopLevelNamesResolver.resolve(packageFqName).classifierNames
    }

    override val hasSpecificCallablePackageNamesComputation: Boolean
        get() = false

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? {
        return exportedTopLevelNamesResolver.resolve(packageFqName).callableNames
    }
}

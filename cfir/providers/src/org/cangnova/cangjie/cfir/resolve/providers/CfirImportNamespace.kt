package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/** 导入选择的命名空间；组织约束命名空间，不要求重导出声明仍属于相同组织。 */
data class CfirImportNamespaceContext(val packageFqName: FqName, val organizationName: Name?)

/** provider 在指定组织命名空间中公开的成员视图，导出边由 provider 自身解释。 */
interface CfirImportNamespace {
    /** 当前组织命名空间是否存在；允许前缀时，真实子包也建立该祖先命名空间。 */
    fun hasPackage(includeSubpackages: Boolean = false): Boolean
    fun classifiers(name: Name): List<CfirClassLikeSymbol<*>>
    fun callables(name: Name): List<CfirCallableSymbol<*>>
    fun functions(name: Name): List<CfirNamedFunctionSymbol>
    fun properties(name: Name): List<CfirPropertySymbol>
}

/** 没有重导出协议的普通 provider 通过声明归属识别命名空间。 */
internal class CfirDirectImportNamespace(
    private val provider: CfirSymbolProvider,
    private val context: CfirImportNamespaceContext,
) : CfirImportNamespace {
    override fun hasPackage(includeSubpackages: Boolean): Boolean {
        val organization = context.organizationName
        if (organization == null) return if (includeSubpackages) provider.hasPackageOrSubpackages(context.packageFqName)
            else provider.hasPackage(context.packageFqName)
        // 二进制 provider 使用官方 CJO 的 pkg@org 身份；源码 provider 自己解释文件组织。
        val suffix = "@${organization.asString()}"
        val candidate = context.packageFqName.asString()
        if (!candidate.endsWith(suffix)) return false
        if (provider.hasPackage(context.packageFqName)) return true
        if (!includeSubpackages) return false
        val packagePrefix = candidate.removeSuffix(suffix)
        return provider.symbolNamesProvider.getPackageNames()?.any { name ->
            name.endsWith(suffix) && FqName(name.removeSuffix(suffix)).startsWith(FqName(packagePrefix))
        } == true
    }

    override fun classifiers(name: Name): List<CfirClassLikeSymbol<*>> =
        provider.getClassLikeSymbolsByClassId(ClassId(context.packageFqName, name))
            .filter { it.belongsToImportOrganization(context.organizationName, context.packageFqName) }
    override fun callables(name: Name): List<CfirCallableSymbol<*>> =
        provider.getTopLevelCallableSymbols(context.packageFqName, name)
            .filter { it.belongsToImportOrganization(context.organizationName, context.packageFqName) }
    override fun functions(name: Name): List<CfirNamedFunctionSymbol> =
        provider.getTopLevelFunctionSymbols(context.packageFqName, name)
            .filter { it.belongsToImportOrganization(context.organizationName, context.packageFqName) }
    override fun properties(name: Name): List<CfirPropertySymbol> =
        provider.getTopLevelPropertySymbols(context.packageFqName, name)
            .filter { it.belongsToImportOrganization(context.organizationName, context.packageFqName) }
}

/** 聚合器必须向每个实际来源传递命名空间，不能先合并最终符号再按声明组织过滤。 */
fun compositeImportNamespace(
    providers: List<CfirSymbolProvider>,
    context: CfirImportNamespaceContext,
): CfirImportNamespace = object : CfirImportNamespace {
    private val namespaces = providers.map { it.getImportNamespace(context) }
    override fun hasPackage(includeSubpackages: Boolean): Boolean = namespaces.any { it.hasPackage(includeSubpackages) }
    override fun classifiers(name: Name): List<CfirClassLikeSymbol<*>> = namespaces.flatMap { it.classifiers(name) }.distinct()
    override fun callables(name: Name): List<CfirCallableSymbol<*>> = namespaces.flatMap { it.callables(name) }.distinct()
    override fun functions(name: Name): List<CfirNamedFunctionSymbol> = namespaces.flatMap { it.functions(name) }.distinct()
    override fun properties(name: Name): List<CfirPropertySymbol> = namespaces.flatMap { it.properties(name) }.distinct()
}

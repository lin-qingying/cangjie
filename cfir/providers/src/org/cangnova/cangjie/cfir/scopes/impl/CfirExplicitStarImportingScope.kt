package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupOrigin
import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupOriginScope
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirImportNamespaceContext
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportBinding
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportTarget
import org.cangnova.cangjie.cfir.scopes.CfirImportScope
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.name.Name

/**
 * 星号导入 scope，处理形如 `import foo.bar.*` 的通配符导入。
 *
 * 收集所有星号导入的目标包名，查找时委托 [CfirSymbolProvider] 在对应包中按名称搜索。
 *
 * 参考 K2 FirExplicitStarImportingScope。
 */
class CfirExplicitStarImportingScope(
    resolvedImports: List<CfirResolvedImportBinding>,
    /**
     * 用于在星号导入包内查询 symbol 的 provider。
     */
    private val symbolProvider: CfirSymbolProvider,
) : CfirImportScope(), CfirLookupOriginScope {

    /** 当前 star import scope 的结构性来源。 */
    override val lookupOrigin: CfirLookupOrigin = resolvedImports.singleImportLookupOrigin()

    /**
     * 所有星号导入的目标包名。
     */
    val starImportPackages: List<CfirResolvedImportTarget.Package>

    init {
        require(resolvedImports.all { it.importDirective.isAllUnder }) {
            "Star importing scope received a simple import binding"
        }
        starImportPackages = resolvedImports
            .asSequence()
            .flatMap { binding ->
            binding.targets.asSequence().mapNotNull { target ->
                target as? CfirResolvedImportTarget.Package
            }
        }
            .distinct()
            .toList()
    }

    /**
     * 在所有星号导入包内处理 classifier。
     */
    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        for (target in starImportPackages) {
            symbolProvider.getImportNamespace(CfirImportNamespaceContext(target.fqName, target.organizationName))
                .classifiers(name).forEach(processor)
        }
    }

    /** 该星号导入 scope 可能暴露的顶层 classifier 名称。 */
    fun getClassifierNames(): Set<Name>? = starImportPackages.namesOrNull { namespace -> namespace.classifierNames() }

    /** 该星号导入 scope 可能暴露的顶层 callable 名称。 */
    fun getCallableNames(): Set<Name>? = starImportPackages.namesOrNull { namespace -> namespace.callableNames() }

    private fun List<CfirResolvedImportTarget.Package>.namesOrNull(
        names: (org.cangnova.cangjie.cfir.resolve.providers.CfirImportNamespace) -> Set<Name>?,
    ): Set<Name>? {
        val result = linkedSetOf<Name>()
        for (target in this) {
            val namespace = symbolProvider.getImportNamespace(CfirImportNamespaceContext(target.fqName, target.organizationName))
            result += names(namespace) ?: return null
        }
        return result
    }

    /**
     * 在所有星号导入包内处理函数。
     */
    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        for (target in starImportPackages) {
            symbolProvider.getImportNamespace(CfirImportNamespaceContext(target.fqName, target.organizationName))
                .functions(name).forEach(processor)
        }
    }

    /**
     * 在所有星号导入包内处理 callable。
     */
    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        for (target in starImportPackages) {
            symbolProvider.getImportNamespace(CfirImportNamespaceContext(target.fqName, target.organizationName))
                .callables(name).forEach(processor)
        }
    }

    /**
     * 在所有星号导入包内处理属性。
     */
    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        for (target in starImportPackages) {
            symbolProvider.getImportNamespace(CfirImportNamespaceContext(target.fqName, target.organizationName))
                .properties(name).forEach(processor)
        }
    }

    /** 在星号导入的包中处理顶层模式绑定变量。 */
    override fun processVariablesByName(name: Name, processor: (CfirVariableSymbol<*>) -> Unit) {
        for (target in starImportPackages) {
            symbolProvider.getImportNamespace(CfirImportNamespaceContext(target.fqName, target.organizationName))
                .variables(name).forEach(processor)
        }
    }
}

package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupOrigin
import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupOriginScope
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportBinding
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportTarget
import org.cangnova.cangjie.cfir.scopes.CfirImportScope
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
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
    private val starImportPackages: List<FqName>

    init {
        require(resolvedImports.all { it.importDirective.isAllUnder }) {
            "Star importing scope received a simple import binding"
        }
        starImportPackages = resolvedImports
            .asSequence()
            .flatMap { binding ->
            binding.targets.asSequence().mapNotNull { target ->
                (target as? CfirResolvedImportTarget.Package)?.fqName
            }
        }
            .distinct()
            .toList()
    }

    /**
     * 在所有星号导入包内处理 classifier。
     */
    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        for (packageFqName in starImportPackages) {
            val classId = ClassId(packageFqName, FqName.topLevel(name))
            val symbol = symbolProvider.getClassLikeSymbolByClassId(classId)
            if (symbol != null) processor(symbol)
        }
    }

    /**
     * 在所有星号导入包内处理函数。
     */
    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        for (packageFqName in starImportPackages) {
            symbolProvider.getTopLevelFunctionSymbols(packageFqName, name).forEach(processor)
        }
    }

    /**
     * 在所有星号导入包内处理 callable。
     */
    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        for (packageFqName in starImportPackages) {
            symbolProvider.getTopLevelCallableSymbols(packageFqName, name).forEach(processor)
        }
    }

    /**
     * 在所有星号导入包内处理属性。
     */
    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        for (packageFqName in starImportPackages) {
            symbolProvider.getTopLevelPropertySymbols(packageFqName, name).forEach(processor)
        }
    }
}

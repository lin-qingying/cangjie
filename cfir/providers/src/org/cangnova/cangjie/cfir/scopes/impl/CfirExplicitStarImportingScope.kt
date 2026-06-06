package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.declarations.CfirImport
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
    imports: List<CfirImport>,
    private val symbolProvider: CfirSymbolProvider,
    resolvedImports: List<CfirResolvedImportBinding>? = null,
) : CfirImportScope() {

    /** 所有星号导入的目标包名 */
    private val starImportPackages: List<FqName>

    init {
        starImportPackages = imports
            .filter { it.isAllUnder }
            .mapNotNull { it.importedFqName }
            .distinct()
    }
    private val resolvedStarImportPackages: List<FqName>? = resolvedImports
        ?.asSequence()
        ?.filter { it.importDirective.isAllUnder }
        ?.flatMap { binding ->
            binding.targets.asSequence().mapNotNull { target ->
                (target as? CfirResolvedImportTarget.Package)?.fqName
            }
        }
        ?.distinct()
        ?.toList()

    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        for (packageFqName in packages()) {
            val classId = ClassId(packageFqName, FqName.topLevel(name))
            val symbol = symbolProvider.getClassLikeSymbolByClassId(classId)
            if (symbol != null) processor(symbol)
        }
    }

    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        for (packageFqName in packages()) {
            symbolProvider.getTopLevelFunctionSymbols(packageFqName, name).forEach(processor)
        }
    }

    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        for (packageFqName in packages()) {
            symbolProvider.getTopLevelCallableSymbols(packageFqName, name).forEach(processor)
        }
    }

    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        for (packageFqName in packages()) {
            symbolProvider.getTopLevelPropertySymbols(packageFqName, name).forEach(processor)
        }
    }

    private fun packages(): List<FqName> = resolvedStarImportPackages ?: starImportPackages
}

package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.declarations.CfirImport
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirImportScope
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
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
) : CfirImportScope {

    /** 所有星号导入的目标包名 */
    private val starImportPackages: List<FqName>

    init {
        starImportPackages = imports
            .filter { it.isAllUnder }
            .map { it.importedFqName }
            .distinct()
    }

    override fun processClassifiersByName(name: Name, processor: (CfirClassSymbol) -> Unit) {
        for (packageFqName in starImportPackages) {
            val classId = ClassId(packageFqName, FqName.topLevel(name), isLocal = false)
            val symbol = symbolProvider.getClassLikeSymbolByClassId(classId)
            if (symbol != null) processor(symbol)
        }
    }

    override fun processFunctionsByName(name: Name, processor: (CfirFunctionSymbol) -> Unit) {
        for (packageFqName in starImportPackages) {
            symbolProvider.getTopLevelFunctionSymbols(packageFqName, name).forEach(processor)
        }
    }

    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        for (packageFqName in starImportPackages) {
            symbolProvider.getTopLevelPropertySymbols(packageFqName, name).forEach(processor)
        }
    }
}

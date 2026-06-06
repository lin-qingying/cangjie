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
import org.cangnova.cangjie.name.Name

/**
 * 简单导入 scope，处理形如 `import foo.bar.Baz` 的精确导入。
 *
 * 逐条处理导入声明，按导入别名（或原始短名称）注册符号。
 * 查找时通过名称直接定位已注册的符号。
 *
 * 参考 K2 FirExplicitSimpleImportingScope。
 */
class CfirExplicitSimpleImportingScope(
    imports: List<CfirImport>,
    private val symbolProvider: CfirSymbolProvider,
    resolvedImports: List<CfirResolvedImportBinding>? = null,
) : CfirImportScope() {

    /** 按有效名称（别名或短名称）索引的导入条目 */
    private val importsByName: Map<Name, List<CfirImport>>
    private val resolvedImportsByName: Map<Name, List<CfirResolvedImportBinding>>?

    init {
        val map = HashMap<Name, MutableList<CfirImport>>()
        for (import in imports) {
            if (import.isAllUnder) continue // 忽略星号导入
            val importedFqName = import.importedFqName
            val effectiveName = import.aliasName ?: importedFqName?.shortName() ?: continue
            map.getOrPut(effectiveName) { mutableListOf() }.add(import)
        }
        importsByName = map
        resolvedImportsByName = resolvedImports
            ?.filter { !it.importDirective.isAllUnder }
            ?.groupBy { it.effectiveName }
    }

    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        val resolvedImports = resolvedImportsByName?.get(name)
        if (resolvedImports != null) {
            resolvedImports.forEachTarget<CfirResolvedImportTarget.ClassLike> { processor(it.symbol) }
            return
        }

        val imports = importsByName[name] ?: return
        for (import in imports) {
            val importedFqName = import.importedFqName ?: continue
            val classId = ClassId.topLevel(importedFqName)
            val symbol = symbolProvider.getClassLikeSymbolByClassId(classId)
            if (symbol != null) processor(symbol)
        }
    }

    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        val resolvedImports = resolvedImportsByName?.get(name)
        if (resolvedImports != null) {
            resolvedImports.forEachCallableTarget<CfirNamedFunctionSymbol>(processor)
            return
        }

        val imports = importsByName[name] ?: return
        for (import in imports) {
            val fqName = import.importedFqName ?: continue
            val packageFqName = fqName.parent()
            val callableName = fqName.shortName()
            symbolProvider.getTopLevelFunctionSymbols(packageFqName, callableName).forEach(processor)
        }
    }

    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        val resolvedImports = resolvedImportsByName?.get(name)
        if (resolvedImports != null) {
            resolvedImports.forEachCallableTarget(processor)
            return
        }

        val imports = importsByName[name] ?: return
        for (import in imports) {
            val fqName = import.importedFqName ?: continue
            val packageFqName = fqName.parent()
            val callableName = fqName.shortName()
            symbolProvider.getTopLevelCallableSymbols(packageFqName, callableName).forEach(processor)
        }
    }

    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        val resolvedImports = resolvedImportsByName?.get(name)
        if (resolvedImports != null) {
            resolvedImports.forEachCallableTarget<CfirPropertySymbol>(processor)
            return
        }

        val imports = importsByName[name] ?: return
        for (import in imports) {
            val fqName = import.importedFqName ?: continue
            val packageFqName = fqName.parent()
            val callableName = fqName.shortName()
            symbolProvider.getTopLevelPropertySymbols(packageFqName, callableName).forEach(processor)
        }
    }

    private inline fun <reified T : CfirResolvedImportTarget> List<CfirResolvedImportBinding>.forEachTarget(processor: (T) -> Unit) {
        for (import in this) {
            import.targets.filterIsInstance<T>().forEach(processor)
        }
    }

    private inline fun <reified S : CfirCallableSymbol<*>> List<CfirResolvedImportBinding>.forEachCallableTarget(
        processor: (S) -> Unit,
    ) {
        forEachTarget<CfirResolvedImportTarget.Callable> { target ->
            target.symbols.filterIsInstance<S>().forEach(processor)
        }
    }
}

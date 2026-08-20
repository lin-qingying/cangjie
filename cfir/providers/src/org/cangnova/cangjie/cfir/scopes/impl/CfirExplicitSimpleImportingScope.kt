package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupOrigin
import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupOriginScope
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportBinding
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportTarget
import org.cangnova.cangjie.cfir.scopes.CfirImportScope
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
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
    resolvedImports: List<CfirResolvedImportBinding>,
) : CfirImportScope(), CfirLookupOriginScope {

    /** 当前 simple import scope 的结构性来源。 */
    override val lookupOrigin: CfirLookupOrigin = resolvedImports.singleImportLookupOrigin()

    /**
     * 按有效名称索引的已解析导入绑定。
     */
    private val resolvedImportsByName: Map<Name, List<CfirResolvedImportBinding>>

    init {
        require(resolvedImports.none { it.importDirective.isAllUnder }) {
            "Simple importing scope received an all-under import binding"
        }
        resolvedImportsByName = resolvedImports
            .groupBy { it.effectiveName }
    }

    /**
     * 按名称处理精确导入的 classifier。
     */
    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        resolvedImportsByName[name]
            ?.forEachTarget<CfirResolvedImportTarget.ClassLike> { processor(it.symbol) }
    }

    /**
     * 按名称处理精确导入的函数。
     */
    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        resolvedImportsByName[name]?.forEachCallableTarget(processor)
    }

    /**
     * 按名称处理精确导入的 callable。
     */
    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        resolvedImportsByName[name]?.forEachCallableTarget(processor)
    }

    /**
     * 按名称处理精确导入的属性。
     */
    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        resolvedImportsByName[name]?.forEachCallableTarget(processor)
    }

    /**
     * 遍历已解析导入绑定中的指定目标类型。
     */
    private inline fun <reified T : CfirResolvedImportTarget> List<CfirResolvedImportBinding>.forEachTarget(processor: (T) -> Unit) {
        for (import in this) {
            import.targets.filterIsInstance<T>().forEach(processor)
        }
    }

    /**
     * 遍历已解析导入绑定中的 callable 目标。
     */
    private inline fun <reified S : CfirCallableSymbol<*>> List<CfirResolvedImportBinding>.forEachCallableTarget(
        processor: (S) -> Unit,
    ) {
        forEachTarget<CfirResolvedImportTarget.Callable> { target ->
            target.symbols.filterIsInstance<S>().forEach(processor)
        }
    }
}

/** simple/star importing scope 共享的单一 origin 不变量。 */
internal fun List<CfirResolvedImportBinding>.singleImportLookupOrigin(): CfirLookupOrigin {
    require(isNotEmpty()) { "Importing scope requires at least one resolved import binding" }
    val origins = mapTo(linkedSetOf()) { it.lookupOrigin }
    require(origins.size == 1) { "Importing scope cannot mix lookup origins: $origins" }
    return origins.single()
}

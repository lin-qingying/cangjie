package org.cangnova.cangjie.analysis.api.cfir.resolve

import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * use-site 视角下的 low-level 可见符号入口。
 *
 * 当前仓库此前直接让 `analysis-api-cfir` 读取 `useSiteFirSession.symbolProvider`，这会带来两个问题：
 * 1. 上层组件重新感知底层 session 与 symbol provider 结构，破坏 `analysis-api-cfir -> low-level-api-cfir` 的边界。
 * 2. 模块闭包、解析策略与可见性语义虽然已经下沉到 low-level，但顶层符号查询仍绕过了这层统一建模。
 *
 * 这里显式以 use-site 模块闭包为单位收口“包是否存在 / 顶层 callable / 顶层 class-like / ClassId 查询”
 * 四类低层能力。后续无论接入 PSI-aware symbol provider、library/source 差异 provider，还是继续向
 * Kotlin `LLModuleWithDependenciesSymbolProvider` 演进，都只需要在这一层扩展。
 */
internal class CaCfirVisibleSymbolProvider(
    private val moduleResolveComponents: CaCfirModuleResolveComponents,
) {
    /**
     * 保持 use-site 模块闭包顺序，先看当前模块，再看依赖模块。
     *
     * 这里不直接暴露组合后的 `CfirSymbolProvider`，而是保留“顺序 + 去重”逻辑在 low-level 层，
     * 避免上层重新决定查询顺序。
     */
    private val providersInLookupOrder: List<CfirSymbolProvider> by lazy(LazyThreadSafetyMode.NONE) {
        moduleResolveComponents.allModules
            .map(moduleResolveComponents.sessionProvider::getSession)
            .map { session -> session.symbolProvider }
            .distinctBy { provider -> provider::class.qualifiedName to System.identityHashCode(provider) }
    }

    fun hasPackage(packageFqName: FqName): Boolean {
        return providersInLookupOrder.any { provider -> provider.hasPackage(packageFqName) }
    }

    fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? {
        return providersInLookupOrder.firstNotNullOfOrNull { provider ->
            provider.getClassLikeSymbolByClassId(classId)
        }
    }

    fun getTopLevelClassifierSymbols(packageFqName: FqName, name: Name): List<CfirClassLikeSymbol<*>> {
        return buildList {
            providersInLookupOrder.forEach { provider ->
                addAll(provider.getTopLevelClassifierSymbols(packageFqName, name))
            }
        }.distinctBy { symbol -> symbol.classId }
    }

    fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<CfirCallableSymbol<*>> {
        return buildList {
            providersInLookupOrder.forEach { provider ->
                provider.getTopLevelCallableSymbols(packageFqName, name).forEach { symbol ->
                    add(VisibleCallableSymbol(provider, symbol))
                }
            }
        }.distinctBy { entry -> entry.symbol.visibleSymbolKey(entry.provider) }
            .map(VisibleCallableSymbol::symbol)
    }

    /**
     * 统一查询同一包和短名下的顶层公开符号。
     *
     * class-like 与 callable 必须共享同一套 provider 遍历顺序和去重语义，
     * 这样上层 Analysis API 才不会因为分两次查询而重新引入顺序漂移。
     */
    fun getTopLevelSymbols(packageFqName: FqName, name: Name): CaCfirTopLevelSymbolQueryResult {
        val classLikeSymbols = buildList {
            providersInLookupOrder.forEach { provider ->
                addAll(provider.getTopLevelClassifierSymbols(packageFqName, name))
            }
        }.distinctBy { symbol -> symbol.classId }

        val callableSymbols = buildList {
            providersInLookupOrder.forEach { provider ->
                provider.getTopLevelCallableSymbols(packageFqName, name).forEach { symbol ->
                    add(VisibleCallableSymbol(provider, symbol))
                }
            }
        }.distinctBy { entry -> entry.symbol.visibleSymbolKey(entry.provider) }
            .map(VisibleCallableSymbol::symbol)

        return CaCfirTopLevelSymbolQueryResult(
            classLikeSymbols = classLikeSymbols,
            callableSymbols = callableSymbols,
        )
    }
}

/**
 * 顶层 callable 在公开 Analysis API 里要求稳定去重。
 *
 * `callableId` 是主语义键；若底层尚未给出稳定 callableId，则退回到“类上下文 + 名字”这一最窄的
 * 语义键，而不是把 provider 身份泄漏到上层结果里。
 */
private data class VisibleCallableSymbol(
    val provider: CfirSymbolProvider,
    val symbol: CfirCallableSymbol<*>,
)

private fun CfirCallableSymbol<*>.visibleSymbolKey(provider: CfirSymbolProvider): String {
    return callableId?.toString() ?: buildString {
        append(provider.getContainingClassId(this@visibleSymbolKey)?.asString().orEmpty())
        append('#')
        append(name.asString())
    }
}

package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirSessionHolder
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScopeImpl
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.name.Name

/**
 * Scope 塔解析器，负责在 scope 塔中按层级搜索符号。
 *
 * 从最内层 scope 开始逐层向外搜索，返回第一个匹配层的所有符号。
 * Phase 2 仅支持按名称的简单查找，后续将通过 [collector] 集成候选收集和排序。
 *
 * 参考 K2 FirTowerResolver(components, resolutionStageRunner, collector)。
 */
class CfirTowerResolver(
    private val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    private val resolutionStageRunner: CfirResolutionStageRunner,
    private val collector: CfirCandidateCollector =
        CfirCandidateCollector(components, resolutionStageRunner),
) : CfirSessionHolder {

    override val session: CfirSession get() = components.session

    /**
     * 按名称查找变量/属性符号。
     *
     * 优先查找局部变量（CfirLocalScope 的 processVariablesByName），
     * 再查找属性（processPropertiesByName）。
     * 返回第一个匹配层的所有符号。
     */
    fun findVariables(name: Name): List<CfirCallableSymbol<*>> {
        val scopes = components.towerDataContext.allScopesReversed()
        for (scope in scopes) {
            val result = mutableListOf<CfirCallableSymbol<*>>()

            // 局部 scope 优先查找局部变量
            if (scope is CfirLocalScopeImpl) {
                scope.processVariablesByName(name) { result.add(it) }
            }

            // 也查找属性（类成员属性等）
            scope.processPropertiesByName(name) { result.add(it) }

            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    /** 按名称查找函数符号，返回第一个匹配层的所有符号 */
    fun findFunctions(name: Name): List<CfirFunctionSymbol> {
        val scopes = components.towerDataContext.allScopesReversed()
        for (scope in scopes) {
            val result = mutableListOf<CfirFunctionSymbol>()
            scope.processFunctionsByName(name) { result.add(it) }
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    /** 按名称查找类符号，返回第一个匹配层的所有符号 */
    fun findClassifiers(name: Name): List<CfirClassSymbol> {
        val scopes = components.towerDataContext.allScopesReversed()
        for (scope in scopes) {
            val result = mutableListOf<CfirClassSymbol>()
            scope.processClassifiersByName(name) { result.add(it) }
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    /**
     * 在指定的 scope 列表中查找变量/属性符号。
     *
     * 用于带接收者的属性访问（在接收者类型的成员 scope 中查找）。
     */
    fun findVariablesInScopes(name: Name, scopes: List<CfirScope>): List<CfirCallableSymbol<*>> {
        for (scope in scopes) {
            val result = mutableListOf<CfirCallableSymbol<*>>()
            scope.processPropertiesByName(name) { result.add(it) }
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    /**
     * 在指定的 scope 列表中查找函数符号。
     *
     * 用于带接收者的方法调用。
     */
    fun findFunctionsInScopes(name: Name, scopes: List<CfirScope>): List<CfirFunctionSymbol> {
        for (scope in scopes) {
            val result = mutableListOf<CfirFunctionSymbol>()
            scope.processFunctionsByName(name) { result.add(it) }
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    /** 重置收集器状态 */
    fun reset() {
        collector.newDataSet()
    }
}

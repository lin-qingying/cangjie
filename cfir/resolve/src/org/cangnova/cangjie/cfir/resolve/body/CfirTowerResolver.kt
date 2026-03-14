package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirSessionHolder
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCallInfo
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.tower.CfirTowerGroup
import org.cangnova.cangjie.cfir.scopes.*
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassDeclaredMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirExtendMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScopeImpl
import org.cangnova.cangjie.cfir.scopes.impl.CfirPackageMemberScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.name.Name

/**
 * Scope 塔解析器，负责在 scope 塔中按层级搜索符号。
 *
 * 支持两种模式：
 * 1. 旧版 findFunctions/findVariables — 简单按名称查找（向后兼容）
 * 2. runResolver — 完整的 Tower 遍历 + 候选收集（Phase 3 新增）
 *
 * 参考 K2 FirTowerResolver(components, resolutionStageRunner, collector)。
 */
class CfirTowerResolver(
    private val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    private val resolutionStageRunner: CfirResolutionStageRunner,
    internal val collector: CfirCandidateCollector =
        CfirCandidateCollector(components, resolutionStageRunner),
) : CfirSessionHolder {

    override val session: CfirSession get() = components.session

    // ---- Phase 3: 完整 Tower 遍历 ----

    /**
     * 执行完整的 Tower 遍历解析。
     *
     * 遍历 towerDataContext.allScopesReversed() 的每一层：
     * - 为每层分配 CfirTowerGroup
     * - 对每个匹配名称的符号创建 CfirCandidate
     * - 通过 collector 收集和排序候选
     * - 根据 shouldStopAtTheGroup 决定是否提前终止
     *
     * @param callInfo 调用信息
     * @param context 解析上下文
     */
    fun runResolver(callInfo: CfirCallInfo, context: CfirResolutionContext) {
        collector.newDataSet()

        val towerDataElements = components.towerDataContext.towerDataElements
        var localDepth = 0
        var importedDepth = 0

        // 从内到外遍历 scope 塔
        for (element in towerDataElements.asReversed()) {
            val scope = element.scope
            val group = classifyScope(scope, element.isLocal, localDepth, importedDepth)

            // 检查是否应在此层级停止
            if (collector.shouldStopAtTheGroup(group)) break

            // 在此 scope 中查找匹配名称的函数符号
            val symbols = mutableListOf<CfirCallableSymbol<*>>()
            scope.processFunctionsByName(callInfo.name) { symbols.add(it) }

            // 为每个匹配符号创建候选并提交收集
            for (symbol in symbols) {
                val candidate = CfirCandidate(
                    symbol = symbol,
                    callInfo = callInfo,
                    originScope = scope,
                )
                collector.consumeCandidate(group, candidate, context)
            }

            // 更新深度计数
            if (element.isLocal) localDepth++
            if (scope is CfirImportScope) importedDepth++
        }
    }

    /**
     * 根据 scope 类型和属性分配 TowerGroup。
     */
    private fun classifyScope(scope: CfirScope, isLocal: Boolean, localDepth: Int, importedDepth: Int): CfirTowerGroup {
        return when {
            scope is CfirClassDeclaredMemberScope -> CfirTowerGroup.MEMBER
            scope is CfirClassScope -> CfirTowerGroup.MEMBER
            isLocal || scope is CfirLocalScopeImpl || scope is CfirLocalScope -> CfirTowerGroup.local(localDepth)
            scope is CfirExtendMemberScope || scope is CfirExtendScope -> CfirTowerGroup.EXTEND
            scope is CfirImportScope -> CfirTowerGroup.imported(importedDepth)
            scope is CfirPackageMemberScope || scope is CfirPackageScope -> CfirTowerGroup.PACKAGE
            else -> CfirTowerGroup.PACKAGE // 保守策略：未知 scope 视为最低优先级
        }
    }

    // ---- 旧版 API（向后兼容） ----

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

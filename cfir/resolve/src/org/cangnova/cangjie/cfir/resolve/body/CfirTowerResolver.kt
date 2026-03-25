package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CandidateFactory
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidateCollector
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallInfo
import org.cangnova.cangjie.cfir.resolve.calls.tower.CfirTowerGroup
import org.cangnova.cangjie.cfir.scopes.*
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassDeclaredMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirExtendMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScopeImpl
import org.cangnova.cangjie.cfir.scopes.impl.CfirPackageMemberScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.name.Name

/**
 * scope 塔解析器，负责在 scope 塔中按层级搜索符号。
 * 既保留旧版按名称直接查找的接口，也支持 Phase 3 的完整 tower 遍历和候选收集。
 */
class CfirTowerResolver(
    private val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    private val resolutionStageRunner: ResolutionStageRunner,
    internal val collector: CfirCandidateCollector =
        CfirCandidateCollector(components, resolutionStageRunner),
) : SessionHolder {

    override val session: CfirSession get() = components.session

    // ---- Phase 3: 完整 tower 遍历 ----

    /**
     * 执行完整的 tower 遍历解析。
     * @param callInfo 调用信息
     * @param context 解析上下文
     */
    fun runResolver(callInfo: CallInfo, context: ResolutionContext) {
        collector.newDataSet()
        val candidateFactory = CandidateFactory(context)

        for (groupedScopes in buildTowerScopeGroups(components.towerDataContext.towerDataElements)) {
            if (collector.shouldStopAtTheGroup(groupedScopes.group)) break

            for (scope in groupedScopes.scopes) {
                val symbols = mutableListOf<CfirCallableSymbol<*>>()
                collectCallableSymbolsByName(scope, callInfo.name, symbols)

                for (symbol in symbols) {
                    val candidate = candidateFactory.createCandidate(
                        callInfo = callInfo,
                        symbol = symbol,
                        originScope = scope,
                    )
                    collector.consumeCandidate(groupedScopes.group, candidate, context)
                }
            }
        }
    }

    // ---- 旧版 API（向后兼容）----

    /**
     * 按名称查找变量或属性符号。
     */
    fun findVariables(name: Name): List<CfirCallableSymbol<*>> {
        val scopes = components.towerDataContext.towerDataElements.asReversed().flatMap { it.getAvailableScopes() }
        for (scope in scopes) {
            val result = mutableListOf<CfirCallableSymbol<*>>()

            // 局部 scope 优先查找局部变量
            if (scope is CfirLocalScopeImpl) {
                scope.processVariablesByName(name) {
                    if (!it.isInvokableSymbol()) {
                        result.add(it)
                    }
                }
            }

            // 同时查找属性
            scope.processPropertiesByName(name) {
                if (!it.isInvokableSymbol()) {
                    result.add(it)
                }
            }

            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    /** 按名称查找函数符号，返回首个匹配层级中的全部结果。 */
    fun findFunctions(name: Name): List<CfirFunctionSymbol<*>> {
        val scopes = components.towerDataContext.towerDataElements.asReversed().flatMap { it.getAvailableScopes() }
        for (scope in scopes) {
            val result = mutableListOf<CfirFunctionSymbol<*>>()
            scope.processFunctionsByName(name) { result.add(it) }
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    /** 按名称查找可调用符号（函数 + 构造器），返回首个匹配层级中的全部结果。 */
    fun findCallables(name: Name): List<CfirCallableSymbol<*>> {
        val scopes = components.towerDataContext.towerDataElements.asReversed().flatMap { it.getAvailableScopes() }
        for (scope in scopes) {
            val result = mutableListOf<CfirCallableSymbol<*>>()
            collectCallableSymbolsByName(scope, name, result)
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    /** 按名称查找类符号，返回首个匹配层级中的全部结果。 */
    fun findClassifiers(name: Name): List<CfirClassLikeSymbol<*>> {
        val scopes = components.towerDataContext.towerDataElements.asReversed().flatMap { it.getAvailableScopes() }
        for (scope in scopes) {
            val result = mutableListOf<CfirClassLikeSymbol<*>>()
            scope.processClassifiersByName(name) { result.add(it) }
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    /**
     * 在指定 scope 列表中查找变量或属性符号。
     */
    fun findVariablesInScopes(name: Name, scopes: List<CfirScope>): List<CfirCallableSymbol<*>> {
        for (scope in scopes) {
            val result = mutableListOf<CfirCallableSymbol<*>>()
            scope.processPropertiesByName(name) {
                if (!it.isInvokableSymbol()) {
                    result.add(it)
                }
            }
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    /**
     * 在指定 scope 列表中查找函数符号。
     */
    fun findFunctionsInScopes(name: Name, scopes: List<CfirScope>): List<CfirFunctionSymbol<*>> {
        for (scope in scopes) {
            val result = mutableListOf<CfirFunctionSymbol<*>>()
            scope.processFunctionsByName(name) { result.add(it) }
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    /** 重置收集器状态。 */
    fun reset() {
        collector.newDataSet()
    }

    private fun collectCallableSymbolsByName(
        scope: CfirScope,
        name: Name,
        sink: MutableList<CfirCallableSymbol<*>>,
    ) {
        val unique = LinkedHashSet<CfirCallableSymbol<*>>()
        scope.processCallablesByName(name) { symbol ->
            if (symbol.isInvokableSymbol()) {
                unique.add(symbol)
            }
        }
        scope.processFunctionsByName(name) { unique.add(it) }
        scope.processClassifiersByName(name) { classSymbol ->
            classSymbol.cfir.declarations
                .asSequence()
                .filterIsInstance<CfirConstructor>()
                .mapNotNull { it.symbol as? CfirCallableSymbol<*> }
                .forEach(unique::add)
        }
        sink.addAll(unique)
    }

    private fun CfirCallableSymbol<*>.isInvokableSymbol(): Boolean {
        if (!isBound) return false
        return when (cfir) {
            is CfirFunction, is CfirConstructor, is CfirEnumConstructor -> true
            else -> false
        }
    }
}

internal data class TowerScopeGroup(
    val group: CfirTowerGroup,
    val scopes: List<CfirScope>,
)

internal fun buildTowerScopeGroups(towerDataElements: List<CfirTowerDataElement>): List<TowerScopeGroup> {
    val groupedScopes = mutableListOf<TowerScopeGroup>()
    var localDepth = 0
    var importedDepth = 0

    fun appendScope(scope: CfirScope, group: CfirTowerGroup) {
        val previousGroup = groupedScopes.lastOrNull()
        if (previousGroup != null && previousGroup.group == group) {
            groupedScopes[groupedScopes.lastIndex] = previousGroup.copy(scopes = previousGroup.scopes + scope)
        } else {
            groupedScopes += TowerScopeGroup(group, listOf(scope))
        }
    }

    for (element in towerDataElements.asReversed()) {
        for (scope in element.getAvailableScopes().asReversed()) {
            val group = classifyScope(scope, element.isLocal, localDepth, importedDepth)
            appendScope(scope, group)

            if (scope is CfirImportScope) {
                importedDepth++
            }
        }

        if (element.isLocal) {
            localDepth++
        }
    }

    return groupedScopes
}

private fun classifyScope(scope: CfirScope, isLocal: Boolean, localDepth: Int, importedDepth: Int): CfirTowerGroup {
    return when {
        scope is CfirClassDeclaredMemberScope -> CfirTowerGroup.MEMBER
        scope is CfirClassScope -> CfirTowerGroup.MEMBER
        isLocal || scope is CfirLocalScopeImpl || scope is CfirLocalScope -> CfirTowerGroup.local(localDepth)
        scope is CfirExtendMemberScope || scope is CfirExtendScope -> CfirTowerGroup.EXTEND
        scope is CfirImportScope -> CfirTowerGroup.imported(importedDepth)
        scope is CfirPackageMemberScope || scope is CfirPackageScope -> CfirTowerGroup.PACKAGE
        else -> CfirTowerGroup.PACKAGE
    }
}

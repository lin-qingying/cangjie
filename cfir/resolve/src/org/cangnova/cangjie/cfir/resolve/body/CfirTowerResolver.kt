package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
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
 * scope 塔解析器，负责在 scope 塔中按层级搜索符号。
 * 既保留旧版按名称直接查找的接口，也支持 Phase 3 的完整 tower 遍历和候选收集。
 */
class CfirTowerResolver(
    private val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    private val resolutionStageRunner: CfirResolutionStageRunner,
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

            // 在当前 scope 中查找同名函数与构造器符号（对齐 Kotlin 的 functions+constructors 处理）。
            val symbols = mutableListOf<CfirCallableSymbol<*>>()
            collectCallableSymbolsByName(scope, callInfo.name, symbols)

            // 为每个匹配符号创建候选并提交收集
            for (symbol in symbols) {
                val candidate = CfirCandidate(
                    symbol = symbol,
                    dispatchReceiver = null,
                    givenExtensionReceiver = null,
                    explicitReceiverKind = org.cangnova.cangjie.cfir.constraints.ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                    callInfo = callInfo,
                    originScope = scope,
                    resolutionContext = context,
                    constraintSystem = components.inferenceComponents.createConstraintSystem(),
                )
                collector.consumeCandidate(group, candidate, context)
            }

            // 更新深度计数
            if (element.isLocal) localDepth++
            if (scope is CfirImportScope) importedDepth++
        }
    }

    /**
     * 根据 scope 类型和属性分配 [CfirTowerGroup]。
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

    // ---- 旧版 API（向后兼容）----

    /**
     * 按名称查找变量或属性符号。
     */
    fun findVariables(name: Name): List<CfirCallableSymbol<*>> {
        val scopes = components.towerDataContext.allScopesReversed()
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
    fun findFunctions(name: Name): List<CfirFunctionSymbol> {
        val scopes = components.towerDataContext.allScopesReversed()
        for (scope in scopes) {
            val result = mutableListOf<CfirFunctionSymbol>()
            scope.processFunctionsByName(name) { result.add(it) }
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    /** 按名称查找可调用符号（函数 + 构造器），返回首个匹配层级中的全部结果。 */
    fun findCallables(name: Name): List<CfirCallableSymbol<*>> {
        val scopes = components.towerDataContext.allScopesReversed()
        for (scope in scopes) {
            val result = mutableListOf<CfirCallableSymbol<*>>()
            collectCallableSymbolsByName(scope, name, result)
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    /** 按名称查找类符号，返回首个匹配层级中的全部结果。 */
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
    fun findFunctionsInScopes(name: Name, scopes: List<CfirScope>): List<CfirFunctionSymbol> {
        for (scope in scopes) {
            val result = mutableListOf<CfirFunctionSymbol>()
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


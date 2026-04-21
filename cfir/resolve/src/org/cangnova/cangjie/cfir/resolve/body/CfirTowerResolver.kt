package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CandidateFactory
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidateCollector
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallInfo
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.stages.ResolutionStageRunner
import org.cangnova.cangjie.cfir.resolve.calls.tower.CandidateFactoriesAndCollectors
import org.cangnova.cangjie.cfir.resolve.calls.tower.CfirTowerResolveTask
import org.cangnova.cangjie.cfir.resolve.calls.tower.TowerDataElementsForName
import org.cangnova.cangjie.cfir.resolve.calls.tower.TowerResolveManager
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScopeImpl
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.name.Name

class CfirTowerResolver(
    private val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    private val resolutionStageRunner: ResolutionStageRunner,
    internal val collector: CfirCandidateCollector =
        CfirCandidateCollector(components, resolutionStageRunner),
) : SessionHolder {

    override val session: CfirSession
        get() = components.session

    private val manager = TowerResolveManager(collector)

    fun runResolver(
        info: CallInfo,
        context: ResolutionContext,
        externalCollector: CfirCandidateCollector? = null,
        candidateFactory: CandidateFactory = CandidateFactory(context, info),
    ): CfirCandidateCollector {
        val resultCollector = externalCollector ?: collector
        val resolveManager = if (externalCollector == null) manager else TowerResolveManager(resultCollector)

        resultCollector.newDataSet()
        resolveManager.reset()

        val candidateFactoriesAndCollectors = CandidateFactoriesAndCollectors(candidateFactory, resultCollector)
        enqueueResolutionTasks(context, resolveManager, candidateFactoriesAndCollectors, info)
        resolveManager.runTasks()

        return resultCollector
    }

    private fun enqueueResolutionTasks(
        context: ResolutionContext,
        manager: TowerResolveManager,
        candidateFactoriesAndCollectors: CandidateFactoriesAndCollectors,
        info: CallInfo,
    ) {
        val mainTask = CfirTowerResolveTask(
            components = components,
            manager = manager,
            towerDataElementsForName = TowerDataElementsForName(info.name, components.towerDataContext),
            collector = candidateFactoriesAndCollectors.resultCollector,
            candidateFactory = candidateFactoriesAndCollectors.candidateFactory,
            context = context,
        )

        when (val receiver = info.explicitReceiver) {
            null -> manager.enqueueResolverTask {
                mainTask.runResolverForNoReceiver(info)
            }

            else -> manager.enqueueResolverTask {
                mainTask.runResolverForExpressionReceiver(info, receiver)
            }
        }
    }

    fun findVariables(name: Name): List<CfirCallableSymbol<*>> {
        val scopes = components.towerDataContext.towerDataElements.asReversed().flatMap { it.getAvailableScopes() }
        for (scope in scopes) {
            val result = mutableListOf<CfirCallableSymbol<*>>()

            if (scope is CfirLocalScopeImpl) {
                scope.processVariablesByName(name) { symbol ->
                    if (!symbol.isInvokableSymbol()) {
                        result += symbol
                    }
                }
            }

            scope.processCallablesByName(name) { symbol ->
                if (!symbol.isInvokableSymbol()) {
                    result += symbol
                }
            }

            if (result.isNotEmpty()) return result.distinct()
        }
        return emptyList()
    }

    fun findFunctions(name: Name): List<CfirFunctionSymbol<*>> {
        val scopes = components.towerDataContext.towerDataElements.asReversed().flatMap { it.getAvailableScopes() }
        for (scope in scopes) {
            val result = mutableListOf<CfirFunctionSymbol<*>>()
            scope.processFunctionsByName(name) { result += it }
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    fun findCallables(name: Name): List<CfirCallableSymbol<*>> {
        val scopes = components.towerDataContext.towerDataElements.asReversed().flatMap { it.getAvailableScopes() }
        for (scope in scopes) {
            val result = mutableListOf<CfirCallableSymbol<*>>()
            scope.processCallablesByName(name) { result += it }
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    fun findClassifiers(name: Name): List<CfirClassLikeSymbol<*>> {
        val scopes = components.towerDataContext.towerDataElements.asReversed().flatMap { it.getAvailableScopes() }
        for (scope in scopes) {
            val result = mutableListOf<CfirClassLikeSymbol<*>>()
            scope.processClassifiersByName(name) { result += it }
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    fun findVariablesInScopes(name: Name, scopes: List<CfirScope>): List<CfirCallableSymbol<*>> {
        for (scope in scopes) {
            val result = mutableListOf<CfirCallableSymbol<*>>()
            if (scope is CfirLocalScopeImpl) {
                scope.processVariablesByName(name) { result += it }
            }
            scope.processCallablesByName(name) { symbol ->
                if (!symbol.isInvokableSymbol()) {
                    result += symbol
                }
            }
            if (result.isNotEmpty()) return result.distinct()
        }
        return emptyList()
    }

    fun findFunctionsInScopes(name: Name, scopes: List<CfirScope>): List<CfirFunctionSymbol<*>> {
        for (scope in scopes) {
            val result = mutableListOf<CfirFunctionSymbol<*>>()
            scope.processFunctionsByName(name) { result += it }
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    fun reset() {
        collector.newDataSet()
        manager.reset()
    }

    private fun CfirCallableSymbol<*>.isInvokableSymbol(): Boolean {
        if (!isBound) return false
        return when (cfir) {
            is CfirFunction, is CfirConstructor, is CfirEnumConstructor -> true
            else -> false
        }
    }
}

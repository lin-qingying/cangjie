/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.calls.resolvedQualifierClassifier
import org.cangnova.cangjie.cfir.calls.resolvedQualifierSymbol
import org.cangnova.cangjie.cfir.calls.resolvedQualifierTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallInfo
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CandidateFactory
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidateCollector
import org.cangnova.cangjie.cfir.resolve.calls.stages.ResolutionStageRunner
import org.cangnova.cangjie.cfir.resolve.calls.tower.CandidateFactoriesAndCollectors
import org.cangnova.cangjie.cfir.resolve.calls.tower.CfirTowerResolveTask
import org.cangnova.cangjie.cfir.resolve.calls.tower.TowerDataElementsForName
import org.cangnova.cangjie.cfir.resolve.calls.tower.TowerResolveManager
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassifierSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.name.Name

/**
 * body resolve 阶段使用的 tower 调用解析入口。
 *
 * 该解析器负责把调用信息、tower data context、候选工厂和候选收集器串联起来，
 * 并通过 [TowerResolveManager] 按 tower group 优先级调度各层查找任务。
 */
class CfirTowerResolver(
    /**
     * body resolve transformer 提供的会话、文件和 tower data 组件。
     */
    private val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    /**
     * 候选阶段检查与约束系统推进器。
     */
    private val resolutionStageRunner: ResolutionStageRunner,
    /**
     * 调用方显式注入的候选收集器。
     *
     * 普通调用解析不注入收集器，每次 [runResolver] 都创建独立实例，避免候选阶段内
     * 递归解析嵌套调用时清空外层调用尚未完成规约的候选状态。
     */
    private val injectedCollector: CfirCandidateCollector? = null,
) : SessionHolder {

    /**
     * 当前解析器所属的 CFIR 会话。
     */
    override val session: CfirSession
        get() = components.session

    /**
     * 运行一次调用解析并返回收集到的候选。
     */
    fun runResolver(
        info: CallInfo,
        context: ResolutionContext,
        externalCollector: CfirCandidateCollector? = null,
        candidateFactory: CandidateFactory = CandidateFactory(context, info),
    ): CfirCandidateCollector {
        val resultCollector = externalCollector
            ?: injectedCollector
            ?: CfirCandidateCollector(components, resolutionStageRunner)
        val resolveManager = TowerResolveManager(resultCollector)

        resultCollector.newDataSet()
        resolveManager.reset()

        val candidateFactoriesAndCollectors = CandidateFactoriesAndCollectors(candidateFactory, resultCollector)
        enqueueResolutionTasks(context, resolveManager, candidateFactoriesAndCollectors, info)
        resolveManager.runTasks()

        return resultCollector
    }

    /**
     * 根据接收者形态注册 tower resolve 任务。
     */
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
                // 对齐 Kotlin FirTowerResolver：已解析的类型/包限定符不作为普通表达式接收者处理，
                // 而是从限定符自身的静态 callable scope 中收集候选。
                if (
                    receiver.resolvedQualifierClassifier(session) != null ||
                    receiver.resolvedQualifierTypeParameter() != null ||
                    receiver.importedPackageQualifierScopeOrNull(components.file, session) != null
                ) {
                    mainTask.runResolverForQualifierReceiver(info, receiver)
                } else {
                    mainTask.runResolverForExpressionReceiver(info, receiver)
                }
            }
        }
    }

    /**
     * 在当前 tower 可见作用域中查找非可调用调用形态的变量符号。
     */
    fun findVariables(name: Name): List<CfirCallableSymbol<*>> {
        val scopes = components.towerDataContext.towerDataElements.asReversed().flatMap { it.getAvailableScopes() }
        for (scope in scopes) {
            val result = mutableListOf<CfirCallableSymbol<*>>()

            if (scope is CfirLocalScope) {
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

    /**
     * 在当前 tower 可见作用域中查找函数符号。
     */
    fun findFunctions(name: Name): List<CfirFunctionSymbol<*>> {
        val scopes = components.towerDataContext.towerDataElements.asReversed().flatMap { it.getAvailableScopes() }
        for (scope in scopes) {
            val result = mutableListOf<CfirFunctionSymbol<*>>()
            scope.processFunctionsByName(name) { result += it }
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    /**
     * 在当前 tower 可见作用域中查找全部 callable 符号。
     */
    fun findCallables(name: Name): List<CfirCallableSymbol<*>> {
        val scopes = components.towerDataContext.towerDataElements.asReversed().flatMap { it.getAvailableScopes() }
        for (scope in scopes) {
            val result = mutableListOf<CfirCallableSymbol<*>>()
            scope.processCallablesByName(name) { result += it }
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    /**
     * 在当前 tower 可见作用域中查找分类器符号。
     */
    fun findClassifiers(name: Name): List<CfirClassifierSymbol<*>> {
        val scopes = components.towerDataContext.towerDataElements.asReversed().flatMap { it.getAvailableScopes() }
        for (scope in scopes) {
            val result = mutableListOf<CfirClassifierSymbol<*>>()
            scope.processClassifiersByNameWithSubstitution(name) { symbol, _ -> result += symbol }
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    /** 返回指定 classifier 在首个可见 tower scope 中携带的 use-site substitutor。 */
    fun findClassifierSubstitutor(
        name: Name,
        target: CfirClassifierSymbol<*>,
    ): org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor {
        val scopes = components.towerDataContext.towerDataElements.asReversed().flatMap { it.getAvailableScopes() }
        for (scope in scopes) {
            var result: org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor? = null
            scope.processClassifiersByNameWithSubstitution(name) { symbol, substitutor ->
                if (symbol == target && result == null) result = substitutor
            }
            if (result != null) return result!!
        }
        return org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor.Empty
    }

    /**
     * 在当前 tower 可见作用域中查找类型参数符号。
     */
    fun findTypeParameters(name: Name): List<CfirTypeParameterSymbol> {
        val scopes = components.towerDataContext.towerDataElements.asReversed().flatMap { it.getAvailableScopes() }
        for (scope in scopes) {
            val result = mutableListOf<CfirTypeParameterSymbol>()
            // 类型参数已经由 CfirTypeParameterScopeImpl 暴露到 classifier 主入口；
            // 这里也必须走同一入口，保证组合 scope 与直接 scope 的行为一致。
            scope.processClassifiersByNameWithSubstitution(name) { symbol, _ ->
                if (symbol is CfirTypeParameterSymbol) {
                    result += symbol
                }
            }
            if (result.isNotEmpty()) return result.distinct()
        }
        return emptyList()
    }

    /**
     * 在指定作用域列表中查找变量符号。
     */
    fun findVariablesInScopes(name: Name, scopes: List<CfirScope>): List<CfirCallableSymbol<*>> {
        for (scope in scopes) {
            val result = mutableListOf<CfirCallableSymbol<*>>()
            if (scope is CfirLocalScope) {
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

    /**
     * 在指定作用域列表中查找函数符号。
     */
    fun findFunctionsInScopes(name: Name, scopes: List<CfirScope>): List<CfirFunctionSymbol<*>> {
        for (scope in scopes) {
            val result = mutableListOf<CfirFunctionSymbol<*>>()
            scope.processFunctionsByName(name) { result += it }
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    /**
     * 判断 callable 符号是否代表可直接调用的函数、构造器或 enum 构造器。
     */
    private fun CfirCallableSymbol<*>.isInvokableSymbol(): Boolean {
        if (!isBound) return false
        return when (cfir) {
            is CfirFunction, is CfirConstructor, is CfirEnumConstructor -> true
            else -> false
        }
    }
}

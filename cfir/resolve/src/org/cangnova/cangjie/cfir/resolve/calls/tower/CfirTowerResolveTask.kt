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

package org.cangnova.cangjie.cfir.resolve.calls.tower

import org.cangnova.cangjie.cfir.calls.ExpressionReceiverValue
import org.cangnova.cangjie.cfir.calls.ImplicitReceiverValue
import org.cangnova.cangjie.cfir.calls.qualifierScopeOrNull
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.BodyResolveComponents
import org.cangnova.cangjie.cfir.resolve.body.CfirTowerDataContext
import org.cangnova.cangjie.cfir.resolve.body.importedPackageQualifierScopeOrNull
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallInfo
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CandidateFactory
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidateCollector
import org.cangnova.cangjie.cfir.scopes.CfirImportScope
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassDeclaredMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirExtendMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirPackageMemberScope
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind

/**
 * 针对单个调用名预筛选后的 tower data 视图。
 */
internal class TowerDataElementsForName(
    /**
     * 当前调用查找的名称。
     */
    name: Name,
    /**
     * 当前 body resolve 的 tower data 上下文。
     */
    towerDataContext: CfirTowerDataContext,
) {
    /**
     * 由内到外排列的非局部 tower data 元素。
     */
    val nonLocalTowerDataElements = towerDataContext.nonLocalTowerDataElements.asReversed()

    /**
     * 由内到外排列、且可能包含目标名称的局部作用域。
     */
    val reversedFilteredLocalScopes by lazy(LazyThreadSafetyMode.NONE) {
        buildList {
            val localScopesBase = towerDataContext.localScopes
            val lastIndex = localScopesBase.lastIndex
            for (i in lastIndex downTo 0) {
                val localScope = localScopesBase[i]
                if (localScope.mayContainName(name)) {
                    add(IndexedValue(lastIndex - i, localScope))
                }
            }
        }
    }
}

/**
 * tower resolve 任务的公共基类。
 */
internal abstract class CfirBaseTowerResolveTask(
    /**
     * tower level 构造所需的 body resolve 组件。
     */
    protected val components: BodyResolveComponents,
    /**
     * 当前任务使用的 tower 调度器。
     */
    private val manager: TowerResolveManager,
    /**
     * 针对当前调用名过滤后的 tower data。
     */
    protected val towerDataElementsForName: TowerDataElementsForName,
    /**
     * 当前任务写入的候选收集器。
     */
    private val collector: CfirCandidateCollector,
    /**
     * 候选创建工厂。
     */
    private val candidateFactory: CandidateFactory,
    /**
     * 当前调用解析上下文。
     */
    private val context: ResolutionContext,
) {
    /**
     * tower level 处理器。
     */
    private val handler = TowerLevelHandler()

    /**
     * 在处理前改写 tower group。
     */
    protected open fun interceptTowerGroup(towerGroup: CfirTowerGroup): CfirTowerGroup = towerGroup
    /**
     * 某个 tower level 成功产生候选后的回调。
     */
    protected open fun onSuccessfulLevel(towerGroup: CfirTowerGroup) {}

    /**
     * 处理单个 tower level 并按 group 优先级参与调度。
     */
    protected suspend fun processLevel(
        towerLevel: CfirTowerLevel,
        callInfo: CallInfo,
        group: CfirTowerGroup,
        explicitReceiverKind: ExplicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
        onEmptyLevel: () -> Unit = {},
    ) {
        val finalGroup = interceptTowerGroup(group)
        manager.requestGroup(finalGroup)

        val result = handler.handleLevel(
            collector = collector,
            candidateFactory = candidateFactory,
            info = callInfo,
            explicitReceiverKind = explicitReceiverKind,
            group = finalGroup,
            towerLevel = towerLevel,
            context = context,
        )
        if (collector.isSuccess) onSuccessfulLevel(finalGroup)
        if (result == ProcessResult.SCOPE_EMPTY) {
            onEmptyLevel()
        }
    }

    /**
     * 按局部作用域、非局部作用域和隐式接收者枚举 tower levels。
     */
    protected inline fun enumerateTowerLevels(
        onScope: (CfirScope, CfirTowerGroup) -> Unit,
        onImplicitReceiver: (ImplicitReceiverValue<*>, CfirTowerGroup) -> Unit,
    ) {
        for ((index, localScope) in towerDataElementsForName.reversedFilteredLocalScopes) {
            onScope(localScope, CfirTowerGroup.local(index))
        }

        var importedDepth = 0
        for (element in towerDataElementsForName.nonLocalTowerDataElements) {
            element.scope?.let { scope ->
                val group = classifyNonLocalScope(scope, importedDepth)
                onScope(scope, group)
                if (scope is CfirImportScope) {
                    importedDepth++
                }
            }

            element.implicitReceiver?.let { receiver ->
                onImplicitReceiver(receiver, CfirTowerGroup.IMPLICIT_MEMBER)
            }
        }
    }
}

/**
 * 默认 tower resolve 任务实现。
 */
internal open class CfirTowerResolveTask(
    components: BodyResolveComponents,
    manager: TowerResolveManager,
    towerDataElementsForName: TowerDataElementsForName,
    collector: CfirCandidateCollector,
    candidateFactory: CandidateFactory,
    context: ResolutionContext,
) : CfirBaseTowerResolveTask(
    components = components,
    manager = manager,
    towerDataElementsForName = towerDataElementsForName,
    collector = collector,
    candidateFactory = candidateFactory,
    context = context,
) {
    /**
     * 解析没有显式接收者的调用。
     */
    suspend fun runResolverForNoReceiver(info: CallInfo) {
        enumerateTowerLevels(
            onScope = { scope, group ->
                processLevel(ScopeBasedTowerLevel(components, scope), info, group)
            },
            onImplicitReceiver = { receiver, group ->
                processLevel(
                    DispatchReceiverMemberScopeTowerLevel(components, receiver),
                    info,
                    group,
                    explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                )
            },
        )
    }

    /**
     * 解析显式表达式接收者调用。
     */
    suspend fun runResolverForExpressionReceiver(
        info: CallInfo,
        receiverExpression: CfirExpression,
    ) {
        val explicitReceiver = ExpressionReceiverValue(receiverExpression)

        processLevel(
            FunctionTypeInvokeTowerLevel(receiverExpression),
            info,
            CfirTowerGroup.EXPLICIT_MEMBER,
            explicitReceiverKind = ExplicitReceiverKind.DISPATCH_RECEIVER,
        )

        processLevel(
            DispatchReceiverMemberScopeTowerLevel(components, explicitReceiver),
            info,
            CfirTowerGroup.EXPLICIT_MEMBER,
            explicitReceiverKind = ExplicitReceiverKind.DISPATCH_RECEIVER,
        )

        processLevel(
            CfirTypeVariableReceiverMemberScopeTowerLevel(components, explicitReceiver),
            info,
            CfirTowerGroup.EXPLICIT_MEMBER,
            explicitReceiverKind = ExplicitReceiverKind.DISPATCH_RECEIVER,
        )

        enumerateTowerLevels(
            onScope = { scope, group ->
                processLevel(
                    ScopeBasedTowerLevel(components, scope, givenExtensionReceiver = explicitReceiver),
                    info,
                    group,
                    explicitReceiverKind = ExplicitReceiverKind.EXTENSION_RECEIVER,
                )
            },
            onImplicitReceiver = { receiver, group ->
                processLevel(
                    DispatchReceiverMemberScopeTowerLevel(
                        components = components,
                        dispatchReceiver = receiver,
                        givenExtensionReceiver = explicitReceiver,
                    ),
                    info,
                    group,
                    explicitReceiverKind = ExplicitReceiverKind.EXTENSION_RECEIVER,
                )
            },
        )
    }

    /**
     * 解析类型、包或导入包限定符作为接收者的调用。
     */
    suspend fun runResolverForQualifierReceiver(
        info: CallInfo,
        receiverExpression: CfirExpression,
    ) {
        val packageQualifierScope = receiverExpression.importedPackageQualifierScopeOrNull(components.file, components.session)
        val callableScope = packageQualifierScope ?: receiverExpression.qualifierScopeOrNull(
            components.session,
            components.scopeSession,
        ) ?: return
        processLevel(
            ScopeBasedTowerLevel(
                components = components,
                scope = callableScope,
                dispatchReceiver = if (packageQualifierScope == null) ExpressionReceiverValue(receiverExpression) else null,
            ),
            info,
            CfirTowerGroup.EXPLICIT_MEMBER,
        )
    }
}

/**
 * 将非局部作用域分类到 tower group。
 */
private fun classifyNonLocalScope(scope: CfirScope, importedDepth: Int): CfirTowerGroup {
    return when (scope) {
        is CfirClassDeclaredMemberScope -> CfirTowerGroup.NON_LOCAL
        is CfirLocalScope -> CfirTowerGroup.local(0)
        is CfirExtendMemberScope -> CfirTowerGroup.EXTEND
        is CfirImportScope -> CfirTowerGroup.imported(importedDepth)
        is CfirPackageMemberScope -> CfirTowerGroup.PACKAGE
        else -> CfirTowerGroup.PACKAGE
    }
}

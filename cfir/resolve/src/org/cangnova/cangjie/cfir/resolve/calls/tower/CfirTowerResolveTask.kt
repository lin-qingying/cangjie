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

internal class TowerDataElementsForName(
    name: Name,
    towerDataContext: CfirTowerDataContext,
) {
    val nonLocalTowerDataElements = towerDataContext.nonLocalTowerDataElements.asReversed()

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

internal abstract class CfirBaseTowerResolveTask(
    protected val components: BodyResolveComponents,
    private val manager: TowerResolveManager,
    protected val towerDataElementsForName: TowerDataElementsForName,
    private val collector: CfirCandidateCollector,
    private val candidateFactory: CandidateFactory,
    private val context: ResolutionContext,
) {
    private val handler = TowerLevelHandler()

    protected open fun interceptTowerGroup(towerGroup: CfirTowerGroup): CfirTowerGroup = towerGroup
    protected open fun onSuccessfulLevel(towerGroup: CfirTowerGroup) {}

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

        enumerateTowerLevels(
            onScope = { scope, group ->
                processLevel(
                    ScopeBasedTowerLevel(components, scope, givenExtensionReceiver = explicitReceiver),
                    info,
                    group,
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
                    explicitReceiverKind = ExplicitReceiverKind.DISPATCH_RECEIVER,
                )
            },
        )
    }

    suspend fun runResolverForQualifierReceiver(
        info: CallInfo,
        receiverExpression: CfirExpression,
    ) {
        val packageQualifierScope = receiverExpression.importedPackageQualifierScopeOrNull(components.file, components.session)
        val callableScope = packageQualifierScope ?: receiverExpression.qualifierScopeOrNull(components.session) ?: return
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

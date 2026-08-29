package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.calls.qualifierScopeOrNull
import org.cangnova.cangjie.cfir.diagnostic.IllegalAccessNonStaticMember
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.resolve.providers.findExtendDeclarationSubstitution
import org.cangnova.cangjie.cfir.resolve.providers.semanticExtendedType
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessContext
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessKind
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityResult
import org.cangnova.cangjie.cfir.resolve.providers.getContainingExtend
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.calls.candidate.yieldIfNeed
import org.cangnova.cangjie.cfir.session.accessibilityChecker
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.name.OperatorNameConventions

/**
 * 检查仓颉 extend receiver。
 *
 * Kotlin FIR 在 callable 上直接保存 receiver parameter；仓颉 CFIR 把 receiver 类型保存在
 * owner extend 的 `extendedTypeRef` 中。因此本阶段以 owner extend 为接收者类型来源，
 * 再复用 providers 层的 extend target 匹配规则得到当前 use-site 的具体 receiver 类型。
 */
object CfirCheckExtensionReceiver : ResolutionStage() {
    context(sink: CheckerSink, context: ResolutionContext)
    /** 检查候选的给定 extension receiver 是否可转换为 owner extend 要求的接收者类型。 */
    override suspend fun check(candidate: Candidate) {
        val receiver = candidate.givenExtensionReceiver ?: return
        if (candidate.callInfo.name.asString() == "getOrThrow") {
            System.err.println(
                "GET_OR_THROW_EXTENSION symbol=${candidate.symbol} receiver=${receiver.expression} " +
                        "receiverType=${receiver.expression.coneTypeOrNull}",
            )
        }
        candidate.typeQualifierAccessedNonStaticExtensionMember(context)?.let { memberName ->
            sink.reportDiagnostic(IllegalAccessNonStaticMember(memberName))
            sink.yieldIfNeed()
            return
        }
        val expectedReceiverType = candidate.expectedExtensionReceiverType() ?: return
        val expectedType = candidate.substitutor.substituteOrSelf(expectedReceiverType)
        val actualType = receiver.expression.coneTypeOrNull ?: return

        if (actualType is org.cangnova.cangjie.cfir.types.ConeTypeVariableType &&
            actualType.typeConstructor.originalTypeParameter == null
        ) {
            System.err.println(
                "EXTENSION_RECEIVER_DEBUG symbol=${candidate.symbol} actual=$actualType expected=$expectedType " +
                        "vars=${candidate.system.currentStorage().notFixedTypeVariables.keys}",
            )
        }

        ArgumentCheckingProcessor.resolvePlainArgumentType(
            candidate = candidate,
            atom = receiver,
            argumentType = actualType,
            expectedType = expectedType,
            sink = sink,
            context = context,
            isReceiver = true,
            isDispatch = false,
            sourceForReceiver = candidate.callInfo.callSite.source,
        )

        if (actualType is org.cangnova.cangjie.cfir.types.ConeTypeVariableType &&
            actualType.typeConstructor.originalTypeParameter == null
        ) {
            System.err.println(
                "EXTENSION_RECEIVER_DEBUG_AFTER symbol=${candidate.symbol} " +
                        "constraints=${candidate.system.currentStorage().notFixedTypeVariables.values.map { it.constraints }}",
            )
        }

        candidate.chosenExtensionReceiver = receiver
        sink.yieldIfNeed()
    }

    context(context: ResolutionContext)
    /** 计算候选在当前 use-site 下实际期望的 extend receiver 类型。 */
    private fun Candidate.expectedExtensionReceiverType(): ConeCangJieType? {
        val callableSymbol = symbol as? CfirCallableSymbol<*> ?: return null
        val ownerExtend = callableSymbol.getContainingExtend()
            ?.takeIf {
                context.session.accessibilityChecker.checkExtend(
                    it,
                    CfirAccessContext(
                        useSiteFile = callInfo.containingFile,
                        containingDeclarations = callInfo.containingDeclarations,
                        receiverType = givenExtensionReceiver?.expression?.coneTypeOrNull,
                        kind = CfirAccessKind.EXTEND,
                    ),
                ) is CfirAccessibilityResult.Accessible
            }
            ?: return null
        val actualReceiverType = givenExtensionReceiver?.expression?.coneTypeOrNull
        if (actualReceiverType != null) {
            findExtendDeclarationSubstitution(context.session, ownerExtend, actualReceiverType)
                ?.substitutedReceiverType
                ?.let { return it }
        }
        return ownerExtend.semanticExtendedType(context.session)
    }

    /**
     * 类型名不能通过 extend receiver 访问实例 extend 成员。
     *
     * `Data.n` 这类 extend 属性不会作为 dispatch receiver 候选进入检查，
     * 因此必须在 extension receiver 阶段产出与普通成员一致的官方诊断。
     */
    private fun Candidate.typeQualifierAccessedNonStaticExtensionMember(context: ResolutionContext): org.cangnova.cangjie.name.Name? {
        val callableSymbol = symbol as? CfirCallableSymbol<*> ?: return null
        if (callableSymbol.cfir.status.isStatic) return null
        if (callInfo.explicitReceiver == null) return null
        if (!callInfo.isMemberSyntaxOrSubscriptAccess()) return null
        val receiverExpression = givenExtensionReceiver?.expression ?: return null
        if (receiverExpression.qualifierScopeOrNull(context.session, context.bodyResolveComponents.scopeSession) == null) {
            return null
        }
        return callableSymbol.name
    }

    /**
     * 普通成员访问和下标 get/set 才对应官方的非静态成员访问诊断。
     */
    private fun org.cangnova.cangjie.cfir.resolve.calls.candidate.CallInfo.isMemberSyntaxOrSubscriptAccess(): Boolean {
        if (origin != CfirFunctionCallOrigin.Operator) return true
        return name == OperatorNameConventions.GET || name == OperatorNameConventions.SET
    }
}

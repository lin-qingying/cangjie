package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.calls.isResolvedTypeQualifier
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.diagnostic.NonGenericFunctionWithTypeArguments
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.calls.candidate.yieldDiagnostic
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirMapTypeArguments.resolvedExplicitTypeArguments
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.providers.getContainingExtend
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.containsErrorType

/**
 * 对齐官方 InstCtxScope.SetRefDecl 的非泛型对象成员调用检查。
 *
 * 名称和类型限定符访问在 MapTypeArguments 检查数量；对象成员先完成值参数映射、
 * owner 替换和接收者检查，再判断实例化是否合法，不能让后续实参类型检查覆盖本诊断。
 */
object CfirCheckNonGenericMemberTypeArguments : ResolutionStage() {
    context(sink: CheckerSink, context: ResolutionContext)
    override suspend fun check(candidate: Candidate) {
        if (!candidate.isSuccessful) return
        val function = candidate.nonGenericInstanceMemberFunction() ?: return
        val typeArguments = candidate.resolvedExplicitTypeArguments()
        if (typeArguments.isEmpty()) return
        // 类型引用自身的错误已有独立诊断；值实参的错误则不能抑制实例化诊断。
        if (typeArguments.any { it.coneTypeOrNull?.containsErrorType() == true }) return

        // 泛型值的成员由上界 scope 提供，官方 IsGenericUpperBoundCall 直接使用
        // 上界的替换映射；包括上界类型的 extend 成员，不再检查此处额外的类型实参。
        val receiverType = candidate.callInfo.explicitReceiver?.coneTypeOrNull?.fullyExpandedType(context.session)
        if (receiverType is ConeTypeParameterType) return

        val ownerExtend = function.symbol.getContainingExtend()
        // 官方 GenerateExtendGenericTypeMapping 只在泛型 extend 中拒绝此类实参。
        if (ownerExtend != null && ownerExtend.typeParameters.isEmpty()) return

        sink.yieldDiagnostic(NonGenericFunctionWithTypeArguments)
    }
}

/** 区分真实的非泛型实例方法、类型/包限定调用和函数值的隐式 invoke。 */
internal fun Candidate.nonGenericInstanceMemberFunction(): CfirNamedFunction? {
    if (callInfo.callKind != CallKind.Function || callInfo.isImplicitInvoke) return null
    val receiver = callInfo.explicitReceiver ?: return null
    if (receiver.isResolvedTypeQualifier(callInfo.session)) return null
    if (dispatchReceiver == null && givenExtensionReceiver == null) return null
    val function = symbol.cfir as? CfirNamedFunction ?: return null
    return function.takeIf { it.typeParameters.isEmpty() }
}

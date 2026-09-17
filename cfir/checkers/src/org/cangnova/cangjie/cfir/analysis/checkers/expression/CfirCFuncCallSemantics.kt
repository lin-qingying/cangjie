package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.interopInfo
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol

/**
 * 调用级 CFunc 判定的唯一 owner。
 *
 * 声明调用读取 declaration ABI；函数值调用读取 explicit receiver 携带的完整
 * `ConeFunctionType.isCFunc`。不能从函数返回类型、callee 短名或调用文本反推。
 */
internal fun CfirFunctionCall.isCFuncCall(session: CfirSession): Boolean {
    val declaration = resolvedCalleeFunctionOrNull()
    if (declaration != null) {
        return declaration.status.isForeign || declaration.status.isC ||
            declaration.interopInfo?.resolvedAbi?.isCFunction == true
    }

    return sequenceOf(explicitReceiver, dispatchReceiver)
        .mapNotNull { receiver ->
            receiver?.coneTypeOrNull
                ?.fullyExpandedType(session)
                ?.let { it as? ConeFunctionType }
        }
        .any { it.isCFunc }
}

/** 取得调用引用已经确认的命名函数声明；未确认时返回 null。 */
internal fun CfirFunctionCall.resolvedCalleeFunctionOrNull(): CfirNamedFunction? {
    val symbol = when (val reference = calleeReference) {
        is CfirResolvedNamedReference -> reference.resolvedSymbol
        is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol
        is CfirResolvedAppliedCallableReference -> reference.resolvedSymbol
        else -> null
    } as? CfirFunctionSymbol<*>
    return symbol?.takeIf { it.isBound }?.cfir as? CfirNamedFunction
}

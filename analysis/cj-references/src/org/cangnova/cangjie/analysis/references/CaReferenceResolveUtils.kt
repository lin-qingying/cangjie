package org.cangnova.cangjie.analysis.references

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.resolution.CaCall
import org.cangnova.cangjie.analysis.api.resolution.singleCallOrNull
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjCallElement
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjValueArgument

/**
 * `cj-references` 与 Analysis API 之间的统一桥接辅助。
 *
 * 这些 helper 把 parent-level reference 共用的“调用点 -> 公开语义模型 -> 源码 PSI”
 * 逻辑集中到一处，避免每个 provider 各自拼接同一套协议。
 */
internal fun CjElement.resolveCallTargetPsis(
    callFilter: (CaCall) -> Boolean = { true },
): List<PsiElement> {
    return analyze(this) {
        val callInfo = resolveToCall() ?: return@analyze emptyList()
        val preferredCalls = buildList {
            callInfo.successfulCall?.let(::add)
            if (callInfo.successfulCall == null) {
                addAll(callInfo.calls)
            }
        }

        fun extractTargets(calls: List<CaCall>): List<PsiElement> = calls.asSequence()
            .filter(callFilter)
            .mapNotNull { call ->
                (call.target as? CaDeclarationSymbol)?.psi ?: call.target?.getOriginalPsi()
            }
            .toCollection(linkedSetOf())
            .toList()

        extractTargets(preferredCalls).ifEmpty {
            extractTargets(callInfo.calls)
        }
    }
}

/**
 * 统一解析“第 N 个实参最终映射到哪个形参声明”。
 */
internal fun CjElement.resolveMappedValueParameters(argumentIndex: Int): List<PsiElement> {
    return analyze(this) {
        val callInfo = resolveToCall() ?: return@analyze emptyList()
        val call = callInfo.successfulCall ?: callInfo.singleCallOrNull() ?: return@analyze emptyList()
        val parameterName = call.argumentMapping
            .firstOrNull { mapping -> mapping.argumentIndex == argumentIndex }
            ?.parameterName
            ?: return@analyze emptyList()

        val callablePsi = call.target?.getOriginalPsi() as? CjCallableDeclaration ?: return@analyze emptyList()
        callablePsi.valueParameters
            .filter { parameter -> parameter.nameAsSafeName == parameterName }
    }
}

/**
 * 读取值参数在调用实参列表中的源码顺序。
 */
internal fun CjValueArgument.argumentIndexInCall(): Int? {
    val callOwner = containingCallOwner() ?: return null
    return callOwner.valueArguments.indexOf(this).takeIf { index -> index >= 0 }
}

internal fun CjValueArgument.containingCallOwner(): CjCallElement? {
    return generateSequence(parent) { current -> current.parent }
        .filterIsInstance<CjCallElement>()
        .firstOrNull()
}



package org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirClassWithAllCallablesResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirPartialBodyResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirWholeElementResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLPartialBodyResolveRequest
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.asResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.throwUnexpectedCfirElementError
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.tryCollectDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.LLCfirResolveDesignationCollector.shouldBeResolved
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.canHaveDeferredReturnTypeCalculation
import org.cangnova.cangjie.cfir.declarations.*

/**
 * Collects [LLCfirResolveTarget] for requested [CfirElementWithResolveState].
 *
 * Effectively, this class is responsible for which elements can be lazily resolved and which cannot.
 *
 * @see LLCfirResolveTarget
 * @see shouldBeResolved
 */
internal object LLCfirResolveDesignationCollector {
    /**
     * 为单个 [target] 收集普通 lazy resolve 目标。
     *
     * 该入口只解析目标及其必要外层路径，不强制递归解析嵌套声明或全部 callable 成员。
     */
    fun getDesignationToResolve(target: CfirElementWithResolveState): LLCfirResolveTarget? {
        return getDesignationToResolve(target, CfirDesignation::asResolveTarget)
    }

    /**
     * 为类 [target] 收集包含全部 callable 成员的 lazy resolve 目标。
     */
    fun getDesignationToResolveWithCallableMembers(target: CfirClass): LLCfirResolveTarget? {
        return getDesignationToResolve(target, ::LLCfirClassWithAllCallablesResolveTarget)
    }

    /**
     * 为 [target] 收集递归解析目标，使目标及其嵌套声明一并推进阶段。
     */
    fun getDesignationToResolveRecursively(target: CfirElementWithResolveState): LLCfirResolveTarget? {
        return getDesignationToResolve(target, ::LLCfirWholeElementResolveTarget)
    }

    /**
     * 根据部分 body 解析请求收集目标 declaration 的 resolve target。
     *
     * 返回的目标会携带 [request] 中的语句范围和停止点，用于 resolver runner 中执行有限 body 分析。
     */
    fun getDesignationToResolveForPartialBody(request: LLPartialBodyResolveRequest): LLCfirResolveTarget? {
        return getDesignationToResolve(request.target) {
            LLCfirPartialBodyResolveTarget(it, request)
        }
    }

    /**
     * 收集 [target] 的 CFIR designation，并通过 [resolveTarget] 包装成具体低阶 resolve target。
     *
     * 如果目标来源不允许 lazy resolve，或者目标无需解析，则返回 `null`。
     */
    private fun getDesignationToResolve(
        target: CfirElementWithResolveState,
        resolveTarget: (CfirDesignation) -> LLCfirResolveTarget,
    ): LLCfirResolveTarget? {
        val designation = getCfirDesignationToResolve(target) ?: return null
        val llResolveTarget = resolveTarget(designation)
        return llResolveTarget
    }

    /**
     * 计算 [target] 真正应该解析的 declaration designation。
     *
     * 访问器、类型参数和值参数会提升到其包含声明；可延迟返回类型计算的 callable 可以直接作为目标；
     * 其他声明走通用 designation 收集逻辑。
     */
    private fun getCfirDesignationToResolve(target: CfirElementWithResolveState): CfirDesignation? {
        if (!target.shouldBeResolved()) {
            return null
        }

        return when (target) {
            is CfirPropertyAccessor -> getCfirDesignationToResolve(target.propertySymbol.cfir)
            is CfirTypeParameter -> getCfirDesignationToResolve(target.containingDeclarationSymbol.cfir)
            is CfirValueParameter -> getCfirDesignationToResolve(target.containingDeclarationSymbol.cfir)
            is CfirCallableDeclaration if target.canHaveDeferredReturnTypeCalculation -> CfirDesignation(target)
            else -> target.tryCollectDesignation()
        }
    }

    /**
     * @see isLazyResolvable
     */
    private fun CfirElementWithResolveState.shouldBeResolved() = when (this) {
        is CfirDeclaration -> shouldBeResolved()
        else -> throwUnexpectedCfirElementError(this)
    }

    /**
     * 判断声明来源是否允许 lazy resolve。
     *
     * 非 lazy-resolvable origin 必须已经处于 body resolve 阶段，否则说明调用方试图解析不可惰性推进的声明。
     */
    private fun CfirDeclaration.shouldBeResolved(): Boolean {
        if (!origin.isLazyResolvable) {
            @OptIn(ResolveStateAccess::class)
            check(resolvePhase == CfirResolvePhase.BODY_RESOLVE) {
                "Expected body resolve phase for origin $origin but found $resolveState"
            }

            return false
        }

        return true
    }
}



package org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.partialBodyAnalysisState
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.errorWithCfirSpecificEntries
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.isPartialBodyResolvable
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildLazyBlock
import org.cangnova.cangjie.cfir.psi

/**
 * Must be called in a write action.
 * @return **false** if it is not in-block modification
 */
internal fun invalidateAfterInBlockModification(declaration: CfirDeclaration): Boolean = when (declaration) {
    is CfirNamedFunction -> declaration.inBodyInvalidation()
    is CfirPropertyAccessor -> declaration.inBodyInvalidation()
    is CfirProperty -> declaration.inBodyInvalidation()
    is CfirCodeFragment -> declaration.inBodyInvalidation()
    else -> errorWithCfirSpecificEntries("Unknown declaration with body", cfir = declaration, psi = declaration.psi)
}

/**
 * Drop body and all related stuff.
 * We should drop:
 * * body
 * * control flow graph reference, because it depends on the body
 * * reduce phase if needed
 *
 * Depends on the body, but we shouldn't drop:
 * * implicit type, because the change mustn't change the resulting type
 *
 * Also, we shouldn't update somehow value parameters because they have their own "bodies" (a default value) and
 * changes in them are OOBM, so it is not our case.
 *
 * @return **false** if it is an out-of-block change
 */
private fun CfirNamedFunction.inBodyInvalidation(): Boolean {
    val body = body ?: return false
    invalidateBody(body)
    return true
}

/**
 * 将函数类声明的已解析函数体回退为惰性函数体，并清除依赖该函数体的控制流图。
 *
 * 如果传入的 [body] 已经是 [CfirLazyBlock]，说明函数体尚未真正完成解析，此时不需要回退解析阶段，
 * 返回 `null` 以便调用方只完成自身状态同步。对于已经解析过的函数体，本函数会把声明阶段降到
 * [phaseWithoutBody]，保证后续查询可以重新触发 body resolve。
 *
 * @return 实际回退到的解析阶段；如果函数体仍是惰性块则返回 `null`。
 */
private fun CfirFunction.invalidateBody(body: CfirBlock): CfirResolvePhase? {
    // the body is not yet resolved, so there is nothing to invalidate
    if (body is CfirLazyBlock) return null
    val newPhase = phaseWithoutBody

    decreasePhase(newPhase)
    replaceBody(buildLazyBlock())
    replaceControlFlowGraphReference(newControlFlowGraphReference = null)

    return newPhase
}

/**
 * Drop body and all related stuff.
 * We should drop:
 * * initializer expression
 * * control flow graph reference, because it depends on the initializer
 * * body resolution state
 * * reduce phase if needed
 *
 * Depends on the body, but we shouldn't drop:
 * * implicit type, because the change mustn't change the resulting type
 *
 * Also, we shouldn't update the property accessors because they don't depend on the initializer.
 * So it is fine to leave the phase of setter/getter/backing field as it is.
 *
 * @return **false** if it is an out-of-block change
 */
private fun CfirProperty.inBodyInvalidation(): Boolean {
    val getterBody = getter?.body
    val setterBody = setter?.body
    if (getterBody == null && setterBody == null) {
        return false
    }
    if (getterBody is CfirLazyBlock || setterBody is CfirLazyBlock) {
        return true
    }

    decreasePhase(phaseWithoutBody)
    replaceControlFlowGraphReference(null)
    replaceBodyResolveState(CfirPropertyBodyResolveState.NOTHING_RESOLVED)

    return true
}

/**
 * Drop body and all related stuff.
 * We should drop:
 * * body
 * * control flow graph reference, because it depends on the body
 * * property body resolution state
 * * reduce phase if needed
 *
 * Depends on the body, but we shouldn't drop:
 * * implicit type, because the change mustn't change the resulting type
 *
 * @return **false** if it is an out-of-block change
 */
private fun CfirPropertyAccessor.inBodyInvalidation(): Boolean {
    val body = body ?: return false
    val newPhase = invalidateBody(body) ?: return true

    val property = propertySymbol.cfir
    property.decreasePhase(newPhase)

    val newPropertyResolveState = if (isGetter) {
        CfirPropertyBodyResolveState.INITIALIZER_RESOLVED
    } else {
        CfirPropertyBodyResolveState.INITIALIZER_AND_GETTER_RESOLVED
    }

    property.replaceBodyResolveState(minOf(property.bodyResolveState, newPropertyResolveState))
    return true
}

/**
 * 使代码片段的已解析块失效，并把代码片段阶段回退到 body resolve 之前。
 *
 * 代码片段没有声明签名与函数体之间的边界，因此一旦块内容已解析，就需要整体替换为惰性块。
 * 若当前块已经是 [CfirLazyBlock]，则视为已有可重新解析入口，不再重复替换。
 *
 * @return 始终返回 `true`，表示该代码片段变更可以按块内修改处理。
 */
private fun CfirCodeFragment.inBodyInvalidation(): Boolean {
    if (block is CfirLazyBlock) {
        return true
    }

    decreasePhase(CfirResolvePhase.BODY_RESOLVE.previous)
    replaceBlock(buildLazyBlock())

    return true
}

/**
 * 表示移除函数体或初始化器之后声明应保留的最高解析阶段。
 *
 * 该阶段不会超过 [CfirResolvePhase.BODY_RESOLVE] 的前一阶段，同时也不会提升当前声明已有的
 * [CfirDeclaration.resolvePhase]，用于在失效时保持阶段单调回退而不引入额外解析。
 */
private val CfirDeclaration.phaseWithoutBody: CfirResolvePhase
    get() {
        return minOf(CfirResolvePhase.BODY_RESOLVE.previous, resolvePhase)
    }

/**
 * 将声明解析状态回退到 [newPhase]，并在部分 body 解析声明上同步清理缓存的部分解析状态。
 *
 * 只有已经进入 body resolve 前置阶段或更高阶段的部分可解析声明才需要清除 [partialBodyAnalysisState]；
 * 较早阶段的声明尚未产生可复用的部分 body 结果，不需要额外处理。
 */
private fun CfirDeclaration.decreasePhase(newPhase: CfirResolvePhase) {
    if (isPartialBodyResolvable) {
        val oldPhase = resolvePhase
        if (oldPhase >= CfirResolvePhase.BODY_RESOLVE.previous) {
            partialBodyAnalysisState = null
        }
    }

    @OptIn(ResolveStateAccess::class)
    resolveState = newPhase.asResolveState()
}

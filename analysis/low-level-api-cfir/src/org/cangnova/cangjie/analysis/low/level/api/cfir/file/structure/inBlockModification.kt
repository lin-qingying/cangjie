

package org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.partialBodyAnalysisState
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.errorWithCfirSpecificEntries
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.isPartialBodyResolvable
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildLazyBlock
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.resolve.dfa.controlFlowGraph
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraph
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import java.util.Collections
import java.util.IdentityHashMap

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

/** source 换代提交前先验证正文槽位存在，不能在源已经改写后才发现结构变化。 */
internal fun canInvalidateAfterInBlockModification(declaration: CfirDeclaration): Boolean = when (declaration) {
    is CfirNamedFunction -> declaration.body != null
    is CfirPropertyAccessor -> declaration.body != null
    is CfirProperty -> declaration.getter?.body != null || declaration.setter?.body != null
    is CfirCodeFragment -> true
    else -> false
}

/**
 * 在来源提交前按真实 CFG 子图 identity 求依赖闭包。CFIR 的 file/class 图保留成员子图，
 * 不能只清修改函数自身，也不能仅依据入口边的 isSubGraph 标志推测图归属。
 */
internal fun collectSourceGenerationCfgDependents(
    file: CfirFile,
    modified: Collection<CfirDeclaration>,
    discardedGraphs: MutableSet<ControlFlowGraph>,
): List<CfirDeclaration> {
    val owners = mutableListOf<CfirDeclaration>()
    file.accept(object : CfirDefaultVisitorVoid() {
        override fun visitElement(element: CfirElement) {
            if (element is CfirDeclaration && element is CfirControlFlowGraphOwner && element !in modified) owners += element
            element.acceptChildren(this)
        }

        override fun visitLazyBlock(lazyBlock: CfirLazyBlock) = Unit
    })
    fun dependsOnDiscarded(graph: ControlFlowGraph, visited: MutableSet<ControlFlowGraph>): Boolean {
        if (graph in discardedGraphs) return true
        if (!visited.add(graph)) return false
        return graph.sourceSubGraphs().any { dependsOnDiscarded(it, visited) }
    }
    val result = mutableListOf<CfirDeclaration>()
    var added: Boolean
    do {
        added = false
        owners.forEach { declaration ->
            val graph = (declaration as CfirControlFlowGraphOwner).controlFlowGraphReference?.controlFlowGraph
            if (graph != null && graph !in discardedGraphs && dependsOnDiscarded(graph, Collections.newSetFromMap(IdentityHashMap()))) {
                discardedGraphs += graph
                result += declaration
                added = true
            }
        }
    } while (added)
    return result
}

/** 图与自身阶段同时失效，后续容器请求从当前成员重建，不触碰已证明稳定的 header 和 symbol。 */
internal fun invalidateSourceGenerationContainerGraph(declaration: CfirDeclaration) {
    val owner = declaration as CfirControlFlowGraphOwner
    declaration.decreasePhase(minOf(declaration.resolvePhase, CfirResolvePhase.BODY_RESOLVE.previous))
    owner.replaceControlFlowGraphReference(null)
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
    getter?.let { accessor -> getterBody?.let(accessor::invalidateBody) }
    setter?.let { accessor -> setterBody?.let(accessor::invalidateBody) }
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
 * 该阶段不会超过当前 session 的 body resolve 前置阶段，同时也不会提升当前声明已有的
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

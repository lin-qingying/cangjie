@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.partialBodyAnalysisState
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.references.CfirControlFlowGraphReference
import org.cangnova.cangjie.cfir.resolve.dfa.controlFlowGraph
import org.cangnova.cangjie.cfir.resolve.dfa.createDfaSourceVisitor
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraph
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import java.util.Collections
import java.util.IdentityHashMap

/**
 * 整文件换源使用同一份实际失效计划：正文、隐藏图和容器依赖不能分别选择不同根集合。
 * 参数默认值属于独立正文，普通函数体修改保留它们及所有未改声明的 partial 状态。
 */
internal class LLCfirSourceGenerationInvalidationPlan(
    file: CfirFile,
    val declarations: List<CfirDeclaration>,
) {
    val discardedRoots: MutableSet<CfirElement> = Collections.newSetFromMap(IdentityHashMap())
    val discardedGraphs: MutableSet<ControlFlowGraph> = Collections.newSetFromMap(IdentityHashMap())
    val dependentContainers: List<CfirDeclaration>
    private val graphReferences = IdentityHashMap<CfirControlFlowGraphOwner, CfirControlFlowGraphReference>()

    init {
        fun graph(owner: CfirControlFlowGraphOwner) {
            owner.controlFlowGraphReference?.let { reference ->
                discardedRoots += reference
                graphReferences[owner] = reference
                reference.controlFlowGraph?.let(discardedGraphs::add)
            }
        }
        val bodyVisitor = object : CfirDefaultVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (!discardedRoots.add(element)) return
                if (element is CfirControlFlowGraphReference) {
                    element.controlFlowGraph?.let(discardedGraphs::add)
                    return
                }
                element.acceptSourceChildren(this)
            }

            override fun visitLazyBlock(lazyBlock: CfirLazyBlock) {
                discardedRoots += lazyBlock
            }
        }
        declarations.forEach { declaration ->
            when (declaration) {
                is CfirFunction -> {
                    declaration.body?.accept(bodyVisitor)
                    graph(declaration)
                }
                is CfirProperty -> {
                    declaration.getter?.let { accessor ->
                        accessor.body?.accept(bodyVisitor)
                        graph(accessor)
                    }
                    declaration.setter?.let { accessor ->
                        accessor.body?.accept(bodyVisitor)
                        graph(accessor)
                    }
                    graph(declaration)
                }
                is CfirCodeFragment -> declaration.block.accept(bodyVisitor)
                else -> error("Unexpected source-generation body owner: ${declaration::class.simpleName}")
            }
        }
        declarations.forEach { declaration ->
            declaration.partialBodyAnalysisState?.analysisStateSnapshot?.dataFlowAnalyzerContext?.acceptSourceChildren(
                createDfaSourceVisitor(
                    onElement = {}, onType = {},
                    canVisitGraph = { graph ->
                        if (graph.declaration === declaration || graph.declaration?.let { it in discardedRoots } == true) {
                            discardedGraphs += graph
                        }
                        true
                    },
                ),
            )
        }
        dependentContainers = collectSourceGenerationCfgDependents(file, declarations, discardedGraphs)
        dependentContainers.forEach { graph(it as CfirControlFlowGraphOwner) }
    }

    fun validate() {
        graphReferences.forEach { (owner, reference) -> check(owner.controlFlowGraphReference === reference) }
    }

    /** 退休父图须解除未改成员的跨图借用边，保留成员图、flow 及 IR 自身身份。 */
    fun retireGraphEdges() {
        validate()
        discardedGraphs.forEach { it.detachCrossGraphEdges() }
    }
}

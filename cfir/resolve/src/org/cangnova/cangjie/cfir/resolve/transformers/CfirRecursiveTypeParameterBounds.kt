package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRefsOwner
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.semantics.CfirGenericUpperBoundRecursionIssue
import org.cangnova.cangjie.cfir.semantics.ConeCyclicTypeBound
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.types.declaredUpperBoundConeTypeOrNull
import org.cangnova.cangjie.cfir.types.directComponentTypes

/**
 * 在 owner 的所有类型参数上界完成解析后，保存递归根因并切断类型系统可见的循环边。
 *
 * 类型系统、实例化检查和成员查找只读取替换后的错误上界；声明检查器仍能通过结构化
 * 诊断载荷读取原始上界并报告语言级递归约束诊断。
 */
internal fun CfirTypeParameterRefsOwner.unbindRecursiveTypeParameterBounds(session: CfirSession) {
    val parameters = typeParameters.filterIsInstance<CfirTypeParameter>()
    if (parameters.isEmpty()) return

    val originalBoundsBySymbol = parameters.associate { parameter ->
        parameter.symbol to parameter.originalUpperBounds()
    }
    val cycleEdges = findDirectUpperBoundCycleEdges(parameters, originalBoundsBySymbol)
    val issue = parameters.firstNotNullOfOrNull { parameter ->
        parameter.findRecursiveUpperBoundIssue(originalBoundsBySymbol, cycleEdges.keys, session)
    } ?: return

    for (parameter in parameters) {
        val originalBounds = originalBoundsBySymbol.getValue(parameter.symbol)
        val boundsToUnbind = cycleEdges[parameter.symbol].orEmpty().toMutableSet()
        if (issue is CfirGenericUpperBoundRecursionIssue.ClassIrrelevant && issue.parameterSymbol == parameter.symbol) {
            originalBounds.forEachIndexed { index, bound ->
                if (bound.declaredUpperBoundConeTypeOrNull() == issue.upperBound) {
                    boundsToUnbind += index
                }
            }
        }
        if (boundsToUnbind.isEmpty()) continue

        parameter.replaceBounds(
            originalBounds.mapIndexed { index, bound ->
                if (index !in boundsToUnbind) {
                    bound
                } else {
                    buildErrorTypeRef {
                        source = bound.source ?: parameter.source
                        diagnostic = ConeCyclicTypeBound(parameter.symbol, listOf(bound), issue)
                    }
                }
            },
        )
    }
}

private fun CfirTypeParameter.originalUpperBounds(): List<CfirTypeRef> {
    return bounds.flatMap { bound ->
        val cyclicBound = bound as? CfirErrorTypeRef
        (cyclicBound?.diagnostic as? ConeCyclicTypeBound)?.originalBounds ?: listOf(bound)
    }
}

private fun CfirTypeParameter.findRecursiveUpperBoundIssue(
    originalBoundsBySymbol: Map<CfirTypeParameterSymbol, List<CfirTypeRef>>,
    cycleEdgeSources: Set<CfirTypeParameterSymbol>,
    session: CfirSession,
): CfirGenericUpperBoundRecursionIssue? {
    for (boundRef in originalBoundsBySymbol.getValue(symbol)) {
        val boundType = boundRef.declaredUpperBoundConeTypeOrNull() ?: continue
        val directlyReferencedParameter = boundType.typeParameterSymbolOrNull()
        if (directlyReferencedParameter != null &&
            directlyReferencedParameter.reachesDirectUpperBoundCycle(cycleEdgeSources, originalBoundsBySymbol)
        ) {
            return CfirGenericUpperBoundRecursionIssue.Direct(symbol, directlyReferencedParameter)
        }
        if (!boundType.isClassLikeUpperBound(session) &&
            boundType.containsCurrentGenericParameterInUpperBounds(symbol, originalBoundsBySymbol)
        ) {
            return CfirGenericUpperBoundRecursionIssue.ClassIrrelevant(symbol, boundType)
        }
    }
    return null
}

/** 用三色 DFS 找到直接上界图中的回边，只切断闭合环所需的边。 */
private fun findDirectUpperBoundCycleEdges(
    parameters: List<CfirTypeParameter>,
    originalBoundsBySymbol: Map<CfirTypeParameterSymbol, List<CfirTypeRef>>,
): Map<CfirTypeParameterSymbol, Set<Int>> {
    val state = hashMapOf<CfirTypeParameterSymbol, UpperBoundVisitState>()
    val cycleEdges = linkedMapOf<CfirTypeParameterSymbol, MutableSet<Int>>()
    val stack = ArrayDeque<UpperBoundTraversalFrame>()
    for (parameter in parameters) {
        if (parameter.symbol in state) continue
        state[parameter.symbol] = UpperBoundVisitState.ACTIVE
        stack.addLast(
            UpperBoundTraversalFrame(
                parameter.symbol,
                parameter.symbol.directUpperBoundSuccessors(originalBoundsBySymbol),
            ),
        )

        while (stack.isNotEmpty()) {
            val frame = stack.last()
            if (!frame.successors.hasNext()) {
                state[frame.symbol] = UpperBoundVisitState.COMPLETE
                stack.removeLast()
                continue
            }

            val edge = frame.successors.next()
            when (state[edge.target]) {
                UpperBoundVisitState.ACTIVE -> {
                    if (frame.symbol in originalBoundsBySymbol) {
                        cycleEdges.getOrPut(frame.symbol, ::linkedSetOf) += edge.boundIndex
                    }
                }
                UpperBoundVisitState.COMPLETE -> Unit
                null -> {
                    state[edge.target] = UpperBoundVisitState.ACTIVE
                    stack.addLast(
                        UpperBoundTraversalFrame(
                            edge.target,
                            edge.target.directUpperBoundSuccessors(originalBoundsBySymbol),
                        ),
                    )
                }
            }
        }
    }

    return cycleEdges.mapValues { (_, indexes) -> indexes.toSet() }
}

private fun CfirTypeParameterSymbol.directUpperBoundSuccessors(
    originalBoundsBySymbol: Map<CfirTypeParameterSymbol, List<CfirTypeRef>>,
): Iterator<DirectUpperBoundEdge> = originalUpperBounds(originalBoundsBySymbol)
    .asSequence()
    .mapIndexedNotNull { index, boundRef ->
        boundRef.declaredUpperBoundConeTypeOrNull()
            ?.typeParameterSymbolOrNull()
            ?.let { DirectUpperBoundEdge(index, it) }
    }
    .iterator()

private data class DirectUpperBoundEdge(
    val boundIndex: Int,
    val target: CfirTypeParameterSymbol,
)

private enum class UpperBoundVisitState {
    ACTIVE,
    COMPLETE,
}

private fun CfirTypeParameterSymbol.reachesDirectUpperBoundCycle(
    cycleEdgeSources: Set<CfirTypeParameterSymbol>,
    originalBoundsBySymbol: Map<CfirTypeParameterSymbol, List<CfirTypeRef>>,
): Boolean {
    val visited = hashSetOf<CfirTypeParameterSymbol>()
    val pending = ArrayDeque<CfirTypeParameterSymbol>()
    pending.addLast(this)

    while (pending.isNotEmpty()) {
        val current = pending.removeLast()
        if (!visited.add(current)) continue
        if (current in cycleEdgeSources) return true
        for (edge in current.directUpperBoundSuccessors(originalBoundsBySymbol)) {
            pending.addLast(edge.target)
        }
    }

    return false
}

private data class UpperBoundTraversalFrame(
    val symbol: CfirTypeParameterSymbol,
    val successors: Iterator<DirectUpperBoundEdge>,
)

private fun CfirTypeParameterSymbol.originalUpperBounds(
    originalBoundsBySymbol: Map<CfirTypeParameterSymbol, List<CfirTypeRef>>,
): List<CfirTypeRef> = originalBoundsBySymbol[this] ?: cfir.originalUpperBounds()

private fun ConeCangJieType.containsCurrentGenericParameterInUpperBounds(
    currentParameter: CfirTypeParameterSymbol,
    originalBoundsBySymbol: Map<CfirTypeParameterSymbol, List<CfirTypeRef>>,
): Boolean {
    val visited = linkedSetOf<ConeCangJieType>()
    val queue = ArrayDeque<ConeCangJieType>()
    queue.addLast(this)

    while (queue.isNotEmpty()) {
        val currentType = queue.removeFirst()
        if (!visited.add(currentType)) continue
        if (currentType.typeParameterSymbolOrNull() == currentParameter) return true

        for (componentType in currentType.directComponentTypes()) {
            val componentParameter = componentType.typeParameterSymbolOrNull()
            if (componentParameter == null) {
                queue.addLast(componentType)
                continue
            }
            if (componentParameter == currentParameter) return true
            if (componentParameter.containingDeclarationSymbol != currentParameter.containingDeclarationSymbol) continue
            for (boundRef in componentParameter.originalUpperBounds(originalBoundsBySymbol)) {
                boundRef.declaredUpperBoundConeTypeOrNull()?.let(queue::addLast)
            }
        }
    }

    return false
}

private fun ConeCangJieType.isClassLikeUpperBound(session: CfirSession): Boolean =
    fullyExpandedType(session) is ConeClassLikeType

private fun ConeCangJieType.typeParameterSymbolOrNull(): CfirTypeParameterSymbol? =
    (this as? ConeTypeParameterType)
        ?.lookupTag
        ?.let { it as? ConeTypeParameterLookupTag }
        ?.typeParameterSymbol

package org.cangnova.cangjie.cfir.resolve.providers.macro

/**
 * 「偏移 → 展开面」的只读定位结果。
 *
 * 只回答**位置落没落在某个宏调用区间内、落在哪一个**，不携带展开产物：
 * `MacroSurface*` 的边界（不实现 `CfirElement`、不进 provider-visible CFIR）不因本查询而改变。
 */
sealed interface MacroSurfaceLookupResult {
    /** 位置不在任何宏调用区间内。 */
    data object NoSurface : MacroSurfaceLookupResult

    /** 唯一的最内层展开面。 */
    data class Innermost(val node: MacroCallNode) : MacroSurfaceLookupResult

    /**
     * 命中多个**互不嵌套**的展开面，无法判定最内层。
     *
     * 如实报歧义而不是按表面顺序任取一个：取错会把另一个展开面的区间当成当前位置的展开。
     */
    data class Ambiguous(val nodes: List<MacroCallNode>) : MacroSurfaceLookupResult
}

/**
 * 按源码偏移定位**最内层**宏展开面。
 *
 * 判据：取区间包含该偏移的全部节点，其中最内层是「被其余候选都包含」的那一个——
 * 森林的父子关系即区间包含关系。
 *
 * 区间按**闭区间**处理：`@A(...)` 的首尾字符也属于该调用点本身
 * （补全常发生在首字符之前、尾字符之后的位置上）。
 *
 * 只读既有 [MacroCallForest]：不新建缓存、不重新解析，代价与 surface 数同阶。
 * 没有 `sourceRange` 的 surface（raw builder 无法映射到源码）不参与定位——
 * 它们没有可比较的区间，参与进去只会制造假歧义。
 */
fun MacroCallForest.lookupSurfaceAt(offset: Int): MacroSurfaceLookupResult {
    val containing = allNodes.filter { node ->
        val range = node.surface.sourceRange ?: return@filter false
        offset >= range.startOffset && offset <= range.endOffset
    }
    if (containing.isEmpty()) return MacroSurfaceLookupResult.NoSurface
    val innermost = containing.filter { candidate ->
        containing.all { other -> other === candidate || other.isAncestorOf(candidate) }
    }
    return when (innermost.size) {
        1 -> MacroSurfaceLookupResult.Innermost(innermost.single())
        else -> MacroSurfaceLookupResult.Ambiguous(containing)
    }
}

/**
 * 该节点在森林中的**所属展开面链**上的父边通道。
 *
 * 返回 null 表示它是 root（没有父边，也就没有「位于哪条 payload 通道」的问题）。
 */
fun MacroCallNode.incomingChannel(): MacroPayloadChannel? {
    val parentNode = parent ?: return null
    return parentNode.childEdges.firstOrNull { it.child === this }?.channel
        ?: MacroPayloadChannel.UNRESOLVED
}

/** [ancestor] 是否为 [node] 的祖先（含自身）。 */
private fun MacroCallNode.isAncestorOf(node: MacroCallNode): Boolean {
    var current: MacroCallNode? = node
    while (current != null) {
        if (current === this) return true
        current = current.parent
    }
    return false
}

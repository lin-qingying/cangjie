

package org.cangnova.cangjie.generators.util

/**
 * 图节点抽象，用于“类/接口”求解问题。
 */
interface Node {
    /**
     * 当前节点的直接父节点。
     */
    val parents: List<Node>
    /**
     * 当前节点对应的原始节点。
     */
    val origin: Node
}

/**
 * 根据继承图与约束求解每个节点应为“类(true)”还是“接口(false)”。
 */
fun solveGraphForClassVsInterface(
    elements: List<Node>,
    requiredInterfaces: Collection<Node>,
    requiredClasses: Collection<Node>,
): List<Boolean> {
    val elementMapping = ElementMapping(elements)
    val solution = solve2sat(elements, elementMapping)
    processRequirementsFromConfig(solution, elementMapping, requiredInterfaces, requiredClasses)
    return solution
}

/**
 * 将节点与 2-SAT 变量下标互相映射的辅助结构。
 */
private class ElementMapping(val elements: Collection<Node>) {
    /**
     * 变量下标到节点的映射。
     */
    private val varToElements: Map<Int, Node> =
        elements.mapIndexed { index, element -> 2 * index to element.origin }.toMap() +
                elements.mapIndexed { index, element -> 2 * index + 1 to element }.toMap()

    /**
     * 节点到变量下标的映射。
     */
    private val elementsToVar: Map<Node, Int> =
        elements.mapIndexed { index, element -> element.origin to index }.toMap()

    /**
     * 返回节点对应的变量下标。
     */
    operator fun get(element: Node): Int = elementsToVar.getValue(element)
    /**
     * 返回变量下标对应的节点。
     */
    operator fun get(index: Int): Node = varToElements.getValue(index)

    /**
     * 参与求解的节点数量。
     */
    val size: Int = elements.size
}

/**
 * 将显式要求的接口/类约束应用到 2-SAT 初始解上。
 */
private fun processRequirementsFromConfig(
    solution: MutableList<Boolean>,
    elementMapping: ElementMapping,
    requiredInterfaces: Collection<Node>,
    requiredClasses: Collection<Node>,
) {
    fun forceParentsToBeInterfaces(element: Node) {
        val origin = element.origin
        val index = elementMapping[origin]
        if (!solution[index]) return
        solution[index] = false
        origin.parents.forEach(::forceParentsToBeInterfaces)
    }

    fun forceInheritorsToBeClasses(element: Node) {
        val queue = ArrayDeque<Node>()
        queue.add(element)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst().origin
            val index = elementMapping[current]
            if (solution[index]) continue
            solution[index] = true
            for (inheritor in elementMapping.elements) {
                if (current in inheritor.parents.map { it.origin }) {
                    queue.add(inheritor)
                }
            }
        }
    }

    requiredInterfaces.forEach(::forceParentsToBeInterfaces)
    requiredClasses.forEach(::forceInheritorsToBeClasses)
}

/**
 * 使用强连通分量求解类/接口分配的 2-SAT 问题。
 */
private fun solve2sat(elements: Collection<Node>, elementsToVar: ElementMapping): MutableList<Boolean> {
    val (g, gt) = buildGraphs(elements, elementsToVar)

    val used = g.indices.mapTo(mutableListOf()) { false }
    val order = mutableListOf<Int>()
    val comp = g.indices.mapTo(mutableListOf()) { -1 }
    val n = g.size

    fun dfs1(v: Int) {
        used[v] = true
        for (to in g[v]) {
            if (!used[to]) dfs1(to)
        }
        order += v
    }

    fun dfs2(v: Int, cl: Int) {
        comp[v] = cl
        for (to in gt[v]) {
            if (comp[to] == -1) dfs2(to, cl)
        }
    }

    for (i in g.indices) {
        if (!used[i]) dfs1(i)
    }

    var j = 0
    for (i in g.indices) {
        val v = order[n - i - 1]
        if (comp[v] == -1) dfs2(v, j++)
    }

    val res = (1..elements.size).mapTo(mutableListOf()) { false }
    for (i in 0 until n step 2) {
        if (comp[i] == comp[i + 1]) {
            error("No SAT solution for class/interface assignment")
        }
        res[i / 2] = comp[i] > comp[i + 1]
    }
    return res
}

/**
 * 根据继承关系构造 2-SAT 图和反图。
 */
private fun buildGraphs(
    elements: Collection<Node>,
    elementMapping: ElementMapping,
): Pair<List<MutableList<Int>>, List<MutableList<Int>>> {
    val g = (1..elementMapping.size * 2).map { mutableListOf<Int>() }
    val gt = (1..elementMapping.size * 2).map { mutableListOf<Int>() }

    fun Int.direct(): Int = this
    fun Int.invert(): Int = this + 1

    fun extractIndex(element: Node) = elementMapping[element] * 2

    for (element in elements) {
        val elementVar = extractIndex(element)
        for (parent in element.parents) {
            val parentVar = extractIndex(parent.origin)
            g[parentVar.direct()] += elementVar.direct()
            g[elementVar.invert()] += parentVar.invert()
        }

        for (i in 0 until element.parents.size) {
            for (j in i + 1 until element.parents.size) {
                val p1 = extractIndex(element.parents[i].origin)
                val p2 = extractIndex(element.parents[j].origin)
                g[p1.direct()] += p2.invert()
                g[p2.direct()] += p1.invert()
            }
        }
    }

    for (from in g.indices) {
        for (to in g[from]) {
            gt[to] += from
        }
    }

    return g to gt
}

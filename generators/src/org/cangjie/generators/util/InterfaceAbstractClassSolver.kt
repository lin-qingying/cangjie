/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangjie.generators.util

interface Node {
    val parents: List<Node>
    val origin: Node
}

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

private class ElementMapping(val elements: Collection<Node>) {
    private val varToElements: Map<Int, Node> =
        elements.mapIndexed { index, element -> 2 * index to element.origin }.toMap() +
                elements.mapIndexed { index, element -> 2 * index + 1 to element }.toMap()

    private val elementsToVar: Map<Node, Int> =
        elements.mapIndexed { index, element -> element.origin to index }.toMap()

    operator fun get(element: Node): Int = elementsToVar.getValue(element)
    operator fun get(index: Int): Node = varToElements.getValue(index)

    val size: Int = elements.size
}

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


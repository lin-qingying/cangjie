/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangjie.cfir.tree.generator.util

import org.cangjie.cfir.tree.generator.Model
import org.cangjie.generators.tree.ImplementationKind
import java.io.File

@Suppress("unused")
fun printHierarchyGraph(model: Model) {
    fun ImplementationKind.toColor(): String = when (this) {
        ImplementationKind.Interface -> "green"
        else -> "red"
    }

    val elements = model.elements

    data class Edge(val from: String, val to: String) {
        override fun toString(): String {
            return "$from -> $to"
        }
    }

    val (interfaces, classes) = elements.partition { it.kind == ImplementationKind.Interface }
    println("Interfaces: ${interfaces.size}")
    println("Classes: ${classes.size}")

    File("CfirTree.dot").printWriter().use { printer ->
        with(printer) {
            println("digraph CfirTree {")
            elements.forEach {
                println("    ${it.typeName} [color=${it.kind!!.toColor()}]")
            }
            println()
            val edges = mutableSetOf<Edge>()
            elements.forEach { element ->
                element.allParents.forEach { parent ->
                    edges += Edge(parent.typeName, element.typeName)
                }
            }
            edges.forEach {
                print("    ")
                println(it)
            }

            println("}")
        }
    }
}


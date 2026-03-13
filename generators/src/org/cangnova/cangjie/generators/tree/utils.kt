/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.generators.tree

/**
 * Returns a bottom-up hierarchy of inheritance, from this element, to its top-most base elements, recursively,
 * in a breadth first manner.
 */
fun <Element : AbstractElement<Element, *, *>> Element.elementAncestorsAndSelfBreadthFirst(): Sequence<Element> = sequence {
    val queue = ArrayDeque<Element>()
    val visited = hashSetOf<Element>()
    queue.add(this@elementAncestorsAndSelfBreadthFirst)

    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        if (!visited.add(current)) continue
        yield(current)
        current.elementParents.map { it.element }.forEach(queue::addLast)
    }
}

fun <Element : AbstractElement<Element, *, *>> Element.elementAncestorsDepthFirst(): Sequence<Element> = sequence {
    for (parent in elementParents) {
        yield(parent.element)
        yieldAll(parent.element.elementAncestorsDepthFirst())
    }
}

fun <Element : AbstractElement<Element, *, *>> Element.elementAncestorsAndSelfDepthFirst(): Sequence<Element> =
    sequenceOf(this) + elementAncestorsDepthFirst()

fun <Element : AbstractElement<Element, *, *>> Element.elementDescendantsDepthFirst(): Sequence<Element> = sequence {
    for (descendant in subElements) {
        yield(descendant)
        yieldAll(descendant.elementDescendantsDepthFirst())
    }
}

fun <Element : AbstractElement<Element, *, *>> Element.elementDescendantsAndSelfDepthFirst(): Sequence<Element> =
    sequenceOf(this) + elementDescendantsDepthFirst()

fun <Element : AbstractElement<Element, *, *>> Element.isSubclassOf(other: Element): Boolean =
    elementAncestorsAndSelfDepthFirst().any { it == other }

fun <Element : AbstractElement<Element, *, *>> Element.isSubclassOfAny(vararg elements: Element) =
    elements.any { isSubclassOf(it) }

fun <Element : AbstractElement<Element, *, *>> detectBaseTransformerTypes(model: Model<Element>) {
    val usedAsFieldType = hashSetOf<AbstractElement<*, *, *>>()
    for (element in model.elements) {
        for (field in element.allFields.filter { it.containsElement }) {
            if (!field.useInBaseTransformerDetection) continue
            val fieldElement = (field.typeRef as? ElementOrRef<*>)?.element
                ?: ((field as? ListField)?.baseType as? ElementOrRef<*>)?.element
                ?: continue
            if (fieldElement.isRootElement) continue
            usedAsFieldType.add(fieldElement)
        }
    }

    for (element in model.elements) {
        element.elementAncestorsDepthFirst().forEach {
            if (it in usedAsFieldType) {
                element.baseTransformerType = it
                return@forEach
            }
        }
    }
}

fun ClassOrElementRef.isSameClassAs(other: ClassOrElementRef): Boolean =
    packageName == other.packageName && typeName == other.typeName

operator fun <K, V, U> MutableMap<K, MutableMap<V, U>>.set(k1: K, k2: V, value: U) {
    this.putIfAbsent(k1, mutableMapOf())
    val map = getValue(k1)
    map[k2] = value
}

operator fun <K, V, U> Map<K, Map<V, U>>.get(k1: K, k2: V): U {
    return getValue(k1).getValue(k2)
}

val AbstractElement<*, *, *>.safeDecapitalizedName: String
    get() = when (name) {
        "Class" -> "klass"
        else -> name.replaceFirstChar(Char::lowercaseChar)
    }

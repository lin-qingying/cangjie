/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.generators.tree

/**
 * 以广度优先方式返回自底向上的继承层次：
 * 从当前元素开始，递归到最顶层父元素。
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

/**
 * 以深度优先顺序返回当前元素的所有父元素。
 *
 * 返回序列不包含当前元素本身；每个直接父元素会先于它自己的父元素出现。
 */
fun <Element : AbstractElement<Element, *, *>> Element.elementAncestorsDepthFirst(): Sequence<Element> = sequence {
    for (parent in elementParents) {
        yield(parent.element)
        yieldAll(parent.element.elementAncestorsDepthFirst())
    }
}

/**
 * 以深度优先顺序返回当前元素及其所有父元素。
 *
 * 当前元素固定作为序列第一项，后续顺序与 [elementAncestorsDepthFirst] 一致。
 */
fun <Element : AbstractElement<Element, *, *>> Element.elementAncestorsAndSelfDepthFirst(): Sequence<Element> =
    sequenceOf(this) + elementAncestorsDepthFirst()

/**
 * 以深度优先顺序返回当前元素的所有子元素。
 *
 * 返回序列不包含当前元素本身；每个直接子元素会先于它自己的子元素出现。
 */
fun <Element : AbstractElement<Element, *, *>> Element.elementDescendantsDepthFirst(): Sequence<Element> = sequence {
    for (descendant in subElements) {
        yield(descendant)
        yieldAll(descendant.elementDescendantsDepthFirst())
    }
}

/**
 * 以深度优先顺序返回当前元素及其所有子元素。
 *
 * 当前元素固定作为序列第一项，后续顺序与 [elementDescendantsDepthFirst] 一致。
 */
fun <Element : AbstractElement<Element, *, *>> Element.elementDescendantsAndSelfDepthFirst(): Sequence<Element> =
    sequenceOf(this) + elementDescendantsDepthFirst()

/**
 * 判断当前元素是否等于或继承自指定元素。
 */
fun <Element : AbstractElement<Element, *, *>> Element.isSubclassOf(other: Element): Boolean =
    elementAncestorsAndSelfDepthFirst().any { it == other }

/**
 * 判断当前元素是否等于或继承自任一候选元素。
 */
fun <Element : AbstractElement<Element, *, *>> Element.isSubclassOfAny(vararg elements: Element) =
    elements.any { isSubclassOf(it) }

/**
 * 推导每个元素在基础 transformer 中应使用的返回类型。
 *
 * 该算法先收集会作为字段类型出现的非根元素，再沿每个元素的父链寻找第一个被其他字段引用的祖先，
 * 将其记录到 [AbstractElement.baseTransformerType]，用于生成更窄的 transformer 返回类型。
 */
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

/**
 * 比较两个类/元素引用是否指向同一个生成类型。
 *
 * 该比较只使用包名和类型名，不比较类型实参、可空性或注解。
 */
fun ClassOrElementRef.isSameClassAs(other: ClassOrElementRef): Boolean =
    packageName == other.packageName && typeName == other.typeName

/**
 * 为二维可变映射写入值。
 *
 * 外层键不存在时会自动创建内层 [MutableMap]。
 */
operator fun <K, V, U> MutableMap<K, MutableMap<V, U>>.set(k1: K, k2: V, value: U) {
    this.putIfAbsent(k1, mutableMapOf())
    val map = getValue(k1)
    map[k2] = value
}

/**
 * 从二维映射中读取值。
 *
 * 任一层键不存在时沿用 [Map.getValue] 的失败语义，表示调用方配置了不存在的映射项。
 */
operator fun <K, V, U> Map<K, Map<V, U>>.get(k1: K, k2: V): U {
    return getValue(k1).getValue(k2)
}

/**
 * 适合生成局部变量或参数名的元素名。
 *
 * 对 Kotlin 关键字或常见冲突名称做定制转换，其余名称仅将首字符小写。
 */
val AbstractElement<*, *, *>.safeDecapitalizedName: String
    get() = when (name) {
        "Interface" -> "`interface`"
        "Class" -> "klass"
        else -> name.replaceFirstChar(Char::lowercaseChar)
    }

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.checkers.generator

import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses

/**
 * checker 组件生成器的完整配置快照。
 */
class CheckersConfiguration(
    /**
     * CFIR 元素类型到 checker 别名及是否生成 visit 方法的映射。
     */
    val aliases: Map<KClass<*>, Pair<String, Boolean>>,
    /**
     * 额外 checker 集合字段名到 checker 类型全限定名的映射。
     */
    val additionalCheckers: MutableMap<String, String>,
    /**
     * 需要复用其他 visit 入口的 CFIR 元素类型映射。
     */
    val visitAlso: Map<KClass<*>, String>,
) {
    /**
     * 每个已注册 CFIR 元素类型对应的已注册父类型列表。
     */
    val parentsMap: Map<KClass<*>, List<KClass<*>>>

    init {
        val parents: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
        for (firKClass in aliases.keys) {
            val allParents = mutableListOf<KClass<*>>()
            bfs(
                firKClass,
                childrenExtractor = { it.allSuperclasses }
            ) {
                if (it in aliases) {
                    allParents += it
                }
                true
            }
            parents[firKClass] = allParents
        }
        parentsMap = parents
    }
}

/**
 * 对类型层级执行广度优先遍历，并在 [process] 返回 false 时停止继续下探更深层级。
 */
private fun <T> bfs(start: T, childrenExtractor: (T) -> Collection<T>, process: (T) -> Boolean) {
    val queue = ArrayDeque<T>()
    val visited = mutableSetOf<T>()
    val levels = mutableMapOf(start to 0)
    queue.addLast(start)
    var levelToStop: Int? = null
    while (queue.isNotEmpty()) {
        val element = queue.removeFirst()
        if (!visited.add(element)) continue
        val level = levels.getValue(element)
        if (levelToStop != null && level > levelToStop) continue
        val shouldContinue = if (level > 0) process(element) else true
        if (shouldContinue) {
            val children = childrenExtractor(element)
            for (child in children) {
                levels[child] = level + 1
                queue.addLast(child)
            }
        } else {
            levelToStop = level
        }
    }
}

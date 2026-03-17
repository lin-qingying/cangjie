/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.generators.tree.imports

import java.util.SortedMap
import java.util.SortedSet

/**
 * 用于收集 [TypeRef] 所需导入，并输出 import 列表。
 */
internal class ImportCollector(currentPackage: String) : ImportCollecting {

    companion object {
        private val STAR = sortedSetOf("*")

        /**
         * 单个包的显式导入数量超过该阈值后，折叠为星号导入。
         */
        private const val STAR_COLLAPSE_THRESHOLD = 4
    }

    /**
     * 包名到导入实体集合的映射。
     */
    private val imports: SortedMap<String, SortedSet<String>> = sortedMapOf()

    /**
     * 这些包中的实体不会显式导入。
     *
     * 参见 [默认导入列表](https://kotlinlang.org/docs/packages.html#default-imports)。
     */
    private val ignoredPackages = hashSetOf(
        currentPackage,
        "kotlin",
        "kotlin.annotation",
        "kotlin.collections",
        "kotlin.comparisons",
        "kotlin.io",
        "kotlin.ranges",
        "kotlin.sequences",
        "kotlin.text",
        "java.lang",
    )

    private fun addImport(packageName: String, entity: String) {
        if (packageName in ignoredPackages) return
        val entities = imports.computeIfAbsent(packageName) { sortedSetOf() }
        if (entities === STAR) return
        if (entity == "*") {
            imports[packageName] = STAR
            return
        }
        entities.add(entity)
        if (entities.size > STAR_COLLAPSE_THRESHOLD) {
            imports[packageName] = STAR
        }
    }

    override fun addImport(importable: Importable) {
        addImport(importable.packageName, importable.typeName)
    }

    /**
     * 按字母序输出所有收集到的导入。
     *
     * @return 若至少输出了一条导入则为 `true`，否则为 `false`。
     */
    fun printAllImports(printer: Appendable): Boolean {
        var atLeastOneImport = false
        for ((packageName, entities) in imports) {
            for (entity in entities) {
                atLeastOneImport = true
                printer.append("import ", packageName, ".", entity, "\n")
            }
        }
        return atLeastOneImport
    }
}

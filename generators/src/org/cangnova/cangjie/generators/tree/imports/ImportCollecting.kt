/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.generators.tree.imports

import org.cangnova.cangjie.generators.tree.TypeRef
import org.cangnova.cangjie.generators.tree.TypeVariable
import org.cangnova.cangjie.generators.tree.joinToWithBuffer
import org.cangnova.cangjie.generators.tree.Variance

/**
 * 表示可以将类型和其他声明添加到当前文件的导入列表的上下文。
 */
interface ImportCollecting {

    /**
     * 添加单个显式导入。
     */
    fun addImport(importable: Importable)

    /**
     * 添加指定包的星号导入。
     */
    fun addStarImport(packageName: String) {
        addImport(ArbitraryImportable(packageName, "*"))
    }

    /**
     * 批量添加导入。
     */
    fun addAllImports(importables: Collection<Importable>) {
        importables.forEach(this::addImport)
    }

    /**
     * 空导入收集器，用于只渲染文本而不收集导入的场景。
     */
    object Empty : ImportCollecting {
        /**
         * 忽略单个导入。
         */
        override fun addImport(importable: Importable) {}
        /**
         * 忽略星号导入。
         */
        override fun addStarImport(packageName: String) {}
        /**
         * 忽略批量导入。
         */
        override fun addAllImports(importables: Collection<Importable>) {}
    }

    /**
     * 将此类型打印为字符串，包括所有参数和问号，同时递归地将引用的类型收集到此导入收集器中。
     */
    fun TypeRef.render(): String = buildString { renderTo(this, this@ImportCollecting) }

    /**
     * 渲染类型参数列表以及单上界。
     */
    fun List<TypeVariable>.typeParameters(end: String = ""): String = buildString {
        if (this@typeParameters.isEmpty()) return@buildString
        joinToWithBuffer(this, prefix = "<", postfix = ">") { param ->
            if (param.variance != Variance.INVARIANT) {
                append(param.variance.label)
                append(" ")
            }
            append(param.name)
            param.bounds.singleOrNull()?.let {
                append(" : ")
                it.renderTo(this, this@ImportCollecting)
            }
        }
        append(end)
    }

    /**
     * 渲染多重上界的 `where` 子句。
     */
    fun List<TypeVariable>.multipleUpperBoundsList(): String {
        val paramsWithMultipleUpperBounds = filter { it.bounds.size > 1 }.takeIf { it.isNotEmpty() } ?: return ""
        return buildString {
            append(" where ")
            paramsWithMultipleUpperBounds.joinToWithBuffer(this, separator = ", ") { param ->
                param.bounds.joinToWithBuffer(this) { bound ->
                    append(param.name)
                    append(" : ")
                    bound.renderTo(this, this@ImportCollecting)
                }
            }
        }
    }
}

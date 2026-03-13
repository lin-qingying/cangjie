/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangjie.generators.tree.imports

import org.cangjie.generators.tree.TypeRef
import org.cangjie.generators.tree.TypeVariable
import org.cangjie.generators.tree.joinToWithBuffer
import org.cangjie.generators.tree.Variance

/**
 * 表示可以将类型和其他声明添加到当前文件的导入列表的上下文。
 */
interface ImportCollecting {

    fun addImport(importable: Importable)

    fun addStarImport(packageName: String) {
        addImport(ArbitraryImportable(packageName, "*"))
    }

    fun addAllImports(importables: Collection<Importable>) {
        importables.forEach(this::addImport)
    }

    object Empty : ImportCollecting {
        override fun addImport(importable: Importable) {}
        override fun addStarImport(packageName: String) {}
        override fun addAllImports(importables: Collection<Importable>) {}
    }

    /**
     * 将此类型打印为字符串，包括所有参数和问号，同时递归地将引用的类型收集到此导入收集器中。
     */
    fun TypeRef.render(): String = buildString { renderTo(this, this@ImportCollecting) }

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

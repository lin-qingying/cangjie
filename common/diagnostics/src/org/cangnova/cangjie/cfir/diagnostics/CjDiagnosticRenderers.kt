/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.diagnostics

import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.cfir.diagnostics.rendering.ContextIndependentParameterRenderer
import org.cangnova.cangjie.cfir.diagnostics.rendering.Renderer
import org.cangnova.cangjie.name.ClassId

/**
 * 仓颉诊断常用参数渲染器集合。
 */
object CjDiagnosticRenderers {
    /**
     * 可空字符串渲染器，null 显示为 `"null"`。
     */
    val NULLABLE_STRING = Renderer<String?> { it ?: "null" }

    /**
     * 使用对象 `toString()` 的通用渲染器。
     */
    val TO_STRING = Renderer { element: Any? ->
        element.toString()
    }

    /**
     * 非空字符串前置冒号的可选渲染器。
     */
    val OPTIONAL_COLON_TO_STRING = Renderer { element: Any? ->
        val string = element.toString()
        if (string.isNotEmpty()) ": $string" else ""
    }

    /**
     * 始终渲染为空字符串的占位渲染器。
     */
    val EMPTY = Renderer { _: Any? -> "" }

    /**
     * 可见性渲染器，使用面向用户的展示名。
     */
    val VISIBILITY = Renderer<Visibility> { visibility ->
        visibility.externalDisplayName
    }

    /**
     * 明确不渲染参数内容的占位渲染器。
     */
    val NOT_RENDERED = Renderer<Any?> {
        ""
    }

    /**
     * 函数参数存在性渲染器，有参数时显示省略号。
     */
    val FUNCTION_PARAMETERS = Renderer { hasValueParameters: Boolean -> if (hasValueParameters) "..." else "" }

    /**
     * ClassId 完整限定名渲染器。
     */
    val CLASS_ID = Renderer<ClassId> { classId ->
        classId.asFqNameString()
    }

    /**
     * ClassId 相对类名渲染器。
     */
    val CLASS_ID_RELATIVE_NAME_ONLY = Renderer<ClassId> { classId ->
        classId.relativeClassName.asString()
    }

    /**
     * 构造集合参数渲染器，最多展示前三项并用省略号截断。
     */
    @Suppress("FunctionName")
    fun <T> COLLECTION(renderer: ContextIndependentParameterRenderer<T>): ContextIndependentParameterRenderer<Collection<T>> {
        return Renderer { list ->
            list.joinToString(prefix = "[", postfix = "]", separator = ", ", limit = 3, truncated = "...") {
                renderer.render(it)
            }
        }
    }
}

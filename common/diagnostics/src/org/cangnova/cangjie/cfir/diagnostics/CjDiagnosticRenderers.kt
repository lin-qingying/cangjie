/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.diagnostics

import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.cfir.diagnostics.rendering.ContextIndependentParameterRenderer
import org.cangnova.cangjie.cfir.diagnostics.rendering.Renderer
import org.cangnova.cangjie.name.ClassId

object CjDiagnosticRenderers {
    val NULLABLE_STRING = Renderer<String?> { it ?: "null" }

    val TO_STRING = Renderer { element: Any? ->
        element.toString()
    }

    val OPTIONAL_COLON_TO_STRING = Renderer { element: Any? ->
        val string = element.toString()
        if (string.isNotEmpty()) ": $string" else ""
    }

    val EMPTY = Renderer { _: Any? -> "" }

    val VISIBILITY = Renderer<Visibility> { visibility ->
        visibility.externalDisplayName
    }

    val NOT_RENDERED = Renderer<Any?> {
        ""
    }

    val FUNCTION_PARAMETERS = Renderer { hasValueParameters: Boolean -> if (hasValueParameters) "..." else "" }

    val CLASS_ID = Renderer<ClassId> { classId ->
        classId.asFqNameString()
    }

    val CLASS_ID_RELATIVE_NAME_ONLY = Renderer<ClassId> { classId ->
        classId.relativeClassName.asString()
    }

    @Suppress("FunctionName")
    fun <T> COLLECTION(renderer: ContextIndependentParameterRenderer<T>): ContextIndependentParameterRenderer<Collection<T>> {
        return Renderer { list ->
            list.joinToString(prefix = "[", postfix = "]", separator = ", ", limit = 3, truncated = "...") {
                renderer.render(it)
            }
        }
    }
}


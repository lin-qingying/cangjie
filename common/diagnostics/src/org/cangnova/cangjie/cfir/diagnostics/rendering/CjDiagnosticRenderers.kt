package org.cangnova.cangjie.cfir.diagnostics.rendering


import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * rendering 包内使用的基础诊断参数渲染器集合。
 */
object CjDiagnosticRenderers {

    /**
     * 通用 toString 渲染器。
     */
    val TO_STRING = Renderer { element: Any? ->
        element.toString()
    }
    /**
     * 字符串原样渲染器。
     */
    val RENDER_STRING = Renderer<String> { it }

    /**
     * 名称渲染器。
     */
    val RENDER_NAME = Renderer<Name> { it.asString() }

    /**
     * 可空名称渲染器，null 显示为匿名占位。
     */
    val RENDER_NULLABLE_NAME = Renderer<Name?> { it?.asString() ?: "<anonymous>" }

    /**
     * 可空 FqName 渲染器，null 显示为 unknown。
     */
    val RENDER_NULLABLE_FQNAME = Renderer<FqName?> { it?.asString() ?: "<unknown>" }

    /**
     * 名称集合渲染器。
     */
    val RENDER_NAME_LIST = Renderer<Collection<Name>> { names ->
        names.joinToString(", ") { it.asString() }
    }

    /**
     * 字符串集合渲染器。
     */
    val RENDER_STRING_LIST = Renderer<Collection<String>> { strings ->
        strings.joinToString(", ")
    }
}

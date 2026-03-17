package org.cangnova.cangjie.cfir.diagnostics.rendering

import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

object CfirDiagnosticRenderers {
    val RENDER_TYPE = ContextDependentRenderer { type: ConeCangjieType, _ ->
        type.toString()
    }

    val RENDER_STRING = Renderer<String> { it }

    val RENDER_NAME = Renderer<Name> { it.asString() }

    val RENDER_NULLABLE_NAME = Renderer<Name?> { it?.asString() ?: "<anonymous>" }

    val RENDER_NULLABLE_FQNAME = Renderer<FqName?> { it?.asString() ?: "<unknown>" }

    val RENDER_NAME_LIST = Renderer<Collection<Name>> { names ->
        names.joinToString(", ") { it.asString() }
    }

    val RENDER_STRING_LIST = Renderer<Collection<String>> { strings ->
        strings.joinToString(", ")
    }
}
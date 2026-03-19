package org.cangnova.cangjie.test.directives

import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer

object DiagnosticsDirectives : SimpleDirectivesContainer() {
    val DIAGNOSTICS by stringDirective(
        description = "Enables/disables diagnostics by name or severity (e.g. +errors, -UNUSED_VARIABLE).",
    )

    val RENDER_DIAGNOSTICS_FULL_TEXT by directive(
        description = "Dumps rendered diagnostics text to an expected side file.",
    )

    val RENDER_ALL_DIAGNOSTICS_FULL_TEXT by directive(
        description = "Keeps rendered diagnostics side file even if per-module render directive is absent.",
    )

    val MARK_DYNAMIC_CALLS by directive(
        description = "Enables debug markers for dynamic calls.",
    )
}

package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer

object FrontendDirectives : SimpleDirectivesContainer() {
    val CHECK_COMPILER_OUTPUT by directive(
        description = "Check compiler textual output against golden files when available."
    )
}

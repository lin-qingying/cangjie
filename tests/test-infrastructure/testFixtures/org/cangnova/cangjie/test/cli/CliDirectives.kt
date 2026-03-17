package org.cangnova.cangjie.test.cli

import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer

object CliDirectives : SimpleDirectivesContainer() {
    val CHECK_COMPILER_OUTPUT by directive(
        description = "Check compiler textual output against golden files when available."
    )
}

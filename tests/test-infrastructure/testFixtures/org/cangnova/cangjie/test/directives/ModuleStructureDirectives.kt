package org.cangnova.cangjie.test.directives

import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer

object ModuleStructureDirectives : SimpleDirectivesContainer() {
    val MODULE by stringDirective(
        """
            Usage: // MODULE: {name}[(dependencies)]
            Describes one module. If no targets are specified then <TODO>
        """.trimIndent()
    )

    val FILE by stringDirective(
        """
            Usage: // FILE: name.{kt|java}
            Declares file with specified name in current module
        """.trimIndent()
    )

    val SNIPPET by directive(
        """
            Usage: // SNIPPET
            Declares (next) snippet with auto-incremented number
        """.trimIndent()
    )
}

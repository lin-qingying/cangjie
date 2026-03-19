package org.cangnova.cangjie.test.directives

import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer

/**
 * Minimal test directives for Cangjie test infrastructure.
 */
object CangjieTestDirectives : SimpleDirectivesContainer() {
    val MODULE by stringDirective(
        description = "Start a new module section. Optional deps, e.g. `main(dep1, dep2)`.",
    )

    val FILE by stringDirective(
        description = "Start a new file section inside current module.",
    )

    val SNIPPET by directive(
        description = "Start a snippet module and auto-generate snippet file.",
    )

    val DEPENDS_ON by stringDirective(
        description = "Add dependencies for current module (comma/space separated).",
    )

    val WITH_STDLIB by directive(
        description = "Include stdlib in compilation.",
    )

    val IGNORE_ERRORS by directive(
        description = "Allow test data with compile errors.",
    )

    val LANGUAGE_VERSION by stringDirective(
        description = "Pin language version, e.g. `// LANGUAGE_VERSION: 1.0`.",
    )

    val EXPECT_COMPLETION_ITEM by stringDirective(
        description = "Declare expected completion item.",
    )

    val FIX_NAME by stringDirective(
        description = "Declare expected quick-fix name.",
    )

    val DUMP_CFIR by directive(
        description = "Enable CFIR dump and compare with expected file.",
    )

    val DUMP_CHIR by directive(
        description = "Enable CHIR dump and compare with expected file.",
    )
}

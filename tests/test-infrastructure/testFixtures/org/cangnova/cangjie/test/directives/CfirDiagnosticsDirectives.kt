package org.cangnova.cangjie.test.directives

import org.cangnova.cangjie.test.CfirParser
import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer

object CfirDiagnosticsDirectives : SimpleDirectivesContainer(){
    val RENDER_FIR_DECLARATION_ATTRIBUTES by directive(
        description = """
            Prints declaration attributes to dumps in compiled-source diagnostics tests
        """
    )
    val COMPARE_WITH_LIGHT_TREE by directive(
        description = "Enable comparing diagnostics between PSI and light tree modes",
        applicability = DirectiveApplicability.Global
    )
    val SUPPRESS_NO_TYPE_ALIAS_EXPANSION_MODE by stringDirective(
        description = """
            Suppresses AbstractFirLightTreeDiagnosticsWithoutAliasExpansionTest
        """
    )
    val CFIR_PARSER by enumDirective<CfirParser>(
        description = "Defines which parser should be used for the CFIR frontend"
    )

    val DISABLE_WITH_PARSER by enumDirective<CfirParser>(
        description = "Skips the current test when active parser equals this value."
    )

    val WITH_EXTRA_CHECKERS by directive(
        description = "Enables additional optional CFIR checkers in tests."
    )

    val WITH_EXPERIMENTAL_CHECKERS by directive(
        description = "Enables experimental CFIR checkers in tests."
    )

    val DUMP_INFERENCE_LOGS by directive(
        description = "Enables CFIR inference logger collection and dumps it to a side file.",
    )


    val SCOPE_DUMP by stringDirective(
        description = "Dump scope information for specified class-like FQNs. Syntax: SCOPE_DUMP: pkg.ClassLike:foo;bar. Empty value means dump all class-like declarations in current test files.",
    )

    val IGNORE_LEAKED_INTERNAL_TYPES by stringDirective(
        description = "Ignore failures in CfirResolvedTypesVerifier and document why.",
    )

    val VERIFY_RESOLVED_TYPES by directive(
        description = "Fails the test when resolved CFIR expressions or type refs still contain unresolved types.",
    )



    val RENDER_DIAGNOSTIC_ARGUMENTS by directive(
        description = "Forces rendering diagnostic arguments in test metadata.",
    )
}

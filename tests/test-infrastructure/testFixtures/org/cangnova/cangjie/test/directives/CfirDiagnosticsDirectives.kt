/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

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

    val LLT_COMPANION_SOURCES by directive(
        description = "Adds sibling .cj files from the same official LLT case directory as additional sources.",
    )

    val CHECK_PROGRAM_ENTRY by directive(
        description = "Enables executable-target program entry diagnostics for official LLT cases.",
    )

    val DUMP_INFERENCE_LOGS by directive(
        description = "Enables CFIR inference logger collection and dumps it to a side file.",
    )


    val SCOPE_DUMP by stringDirective(
        description = "Dump scope information for specified top-level class-like FQNs. Syntax: SCOPE_DUMP: pkg.ClassLike:foo;bar. Nested-class syntax is unsupported because Cangjie no longer models nested class declarations. Empty value means dump all class-like declarations in current test files.",
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

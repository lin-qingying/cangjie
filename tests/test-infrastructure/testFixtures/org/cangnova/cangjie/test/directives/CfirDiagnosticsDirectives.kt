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

/**
 * 提供 `CfirDiagnosticsDirectives` 单例，集中承载测试指令的共享状态、常量或默认行为。
 */
object CfirDiagnosticsDirectives : SimpleDirectivesContainer(){
    /**
     * 保存 `RENDER_FIR_DECLARATION_ATTRIBUTES`，供测试指令在测试执行期间读取或传递。
     */
    val RENDER_FIR_DECLARATION_ATTRIBUTES by directive(
        description = """
            Prints declaration attributes to dumps in compiled-source diagnostics tests
        """
    )
    /**
     * 保存 `COMPARE_WITH_LIGHT_TREE`，供测试指令在测试执行期间读取或传递。
     */
    val COMPARE_WITH_LIGHT_TREE by directive(
        description = "Enable comparing diagnostics between PSI and light tree modes",
        applicability = DirectiveApplicability.Global
    )
    /**
     * 保存 `SUPPRESS_NO_TYPE_ALIAS_EXPANSION_MODE`，供测试指令在测试执行期间读取或传递。
     */
    val SUPPRESS_NO_TYPE_ALIAS_EXPANSION_MODE by stringDirective(
        description = """
            Suppresses AbstractFirLightTreeDiagnosticsWithoutAliasExpansionTest
        """
    )
    /**
     * 保存 `CFIR_PARSER`，供测试指令在测试执行期间读取或传递。
     */
    val CFIR_PARSER by enumDirective<CfirParser>(
        description = "Defines which parser should be used for the CFIR frontend"
    )

    /**
     * 保存 `DISABLE_WITH_PARSER`，供测试指令在测试执行期间读取或传递。
     */
    val DISABLE_WITH_PARSER by enumDirective<CfirParser>(
        description = "Skips the current test when active parser equals this value."
    )

    /**
     * 保存 `WITH_EXTRA_CHECKERS`，供测试指令在测试执行期间读取或传递。
     */
    val WITH_EXTRA_CHECKERS by directive(
        description = "Enables additional optional CFIR checkers in tests."
    )

    /**
     * 保存 `WITH_EXPERIMENTAL_CHECKERS`，供测试指令在测试执行期间读取或传递。
     */
    val WITH_EXPERIMENTAL_CHECKERS by directive(
        description = "Enables experimental CFIR checkers in tests."
    )

    /**
     * 保存 `LLT_COMPANION_SOURCES`，供测试指令在测试执行期间读取或传递。
     */
    val LLT_COMPANION_SOURCES by directive(
        description = "Adds sibling .cj files from the same official LLT case directory as additional sources.",
    )

    /**
     * 保存 `CHECK_PROGRAM_ENTRY`，供测试指令在测试执行期间读取或传递。
     */
    val CHECK_PROGRAM_ENTRY by directive(
        description = "Enables executable-target program entry diagnostics for official LLT cases.",
    )

    /**
     * 保存 `DUMP_INFERENCE_LOGS`，供测试指令在测试执行期间读取或传递。
     */
    val DUMP_INFERENCE_LOGS by directive(
        description = "Enables CFIR inference logger collection and dumps it to a side file.",
    )


    /**
     * 保存 `SCOPE_DUMP`，供测试指令在测试执行期间读取或传递。
     */
    val SCOPE_DUMP by stringDirective(
        description = "Dump scope information for specified top-level class-like FQNs. Syntax: SCOPE_DUMP: pkg.ClassLike:foo;bar. Nested-class syntax is unsupported because Cangjie no longer models nested class declarations. Empty value means dump all class-like declarations in current test files.",
    )

    /**
     * 保存 `IGNORE_LEAKED_INTERNAL_TYPES`，供测试指令在测试执行期间读取或传递。
     */
    val IGNORE_LEAKED_INTERNAL_TYPES by stringDirective(
        description = "Ignore failures in CfirResolvedTypesVerifier and document why.",
    )

    /**
     * 保存 `VERIFY_RESOLVED_TYPES`，供测试指令在测试执行期间读取或传递。
     */
    val VERIFY_RESOLVED_TYPES by directive(
        description = "Fails the test when resolved CFIR expressions or type refs still contain unresolved types.",
    )



    /**
     * 保存 `RENDER_DIAGNOSTIC_ARGUMENTS`，供测试指令在测试执行期间读取或传递。
     */
    val RENDER_DIAGNOSTIC_ARGUMENTS by directive(
        description = "Forces rendering diagnostic arguments in test metadata.",
    )
}

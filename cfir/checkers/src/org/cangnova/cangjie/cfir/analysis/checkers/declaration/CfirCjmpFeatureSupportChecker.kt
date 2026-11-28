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
 */

package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.CjmpLanguageFeature
import org.cangnova.cangjie.cfir.analysis.checkers.MppCheckerKind
import org.cangnova.cangjie.cfir.analysis.checkers.CjmpGate
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.realSourceModifiers
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.supportsCjmpDeclarationModel
import org.cangnova.cangjie.supportsCjmpFeature

/** CJMP modifier 的版本支持由 feature gate 报告；语法规则归共享 parser 所有。 */
object CfirCjmpFeatureSupportChecker : CfirBasicDeclarationChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirDeclaration) {
        val hasCjmpModifier = declaration.source?.realSourceModifiers().orEmpty().any { modifier ->
            modifier.token == CjTokens.COMMON_KEYWORD || modifier.token == CjTokens.SPECIFIC_KEYWORD ||
                    (modifier.token == CjTokens.PLATFORM_KEYWORD &&
                            context.languageVersionSettings.supportsCjmpFeature(
                                CjmpLanguageFeature.CommonPlatformDeclarations,
                            ))
        }
        if (hasCjmpModifier && !context.languageVersionSettings.supportsCjmpDeclarationModel()) {
            CjmpGate.requireVersionSupport(declaration.source)
        }
    }
}

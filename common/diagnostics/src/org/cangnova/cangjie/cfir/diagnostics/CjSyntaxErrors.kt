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

package org.cangnova.cangjie.cfir.diagnostics

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.cfir.diagnostics.rendering.BaseDiagnosticRendererFactory

/**
 * 官方词法/语法前端诊断。
 *
 * LLT 官方测试数据保留了 cjc 的 `lex_*` 诊断名；这些诊断来自解析阶段，
 * 不属于 CFIR 语义 checker，因此独立于 [org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors]。
 */
object CjSyntaxErrors : CjDiagnosticsContainer() {
    val LEX_UNKNOWN_SUFFIX: CjDiagnosticFactory0 = CjDiagnosticFactory0(
        "lex_unknown_suffix",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        PsiElement::class,
        getRendererFactory(),
    )

    val LEX_UNEXPECTED_DIGIT: CjDiagnosticFactory0 = CjDiagnosticFactory0(
        "lex_unexpected_digit",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        PsiElement::class,
        getRendererFactory(),
    )

    val PARSE_UNEXPECTED_DECLARATION_IN_SCOPE: CjDiagnosticFactory0 = CjDiagnosticFactory0(
        "parse_unexpected_declaration_in_scope",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        PsiElement::class,
        getRendererFactory(),
    )

    val PARSE_EXPECTED_DECL: CjDiagnosticFactory0 = CjDiagnosticFactory0(
        "parse_expected_decl",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        PsiElement::class,
        getRendererFactory(),
    )

    val PARSE_EXPECTED_NAME: CjDiagnosticFactory0 = CjDiagnosticFactory0(
        "parse_expected_name",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        PsiElement::class,
        getRendererFactory(),
    )

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = CjSyntaxErrorsDefaultMessages

    fun factoryForParserMessage(message: String?): CjDiagnosticFactory0? {
        val normalized = message.orEmpty().lowercase()
        return when {
            "unknown suffix" in normalized -> LEX_UNKNOWN_SUFFIX
            "unexpected digit" in normalized -> LEX_UNEXPECTED_DIGIT
            "invalid declaration in scope" in normalized -> PARSE_UNEXPECTED_DECLARATION_IN_SCOPE
            "declaration is not allowed inside" in normalized -> PARSE_UNEXPECTED_DECLARATION_IN_SCOPE
            "expecting member declaration" in normalized -> PARSE_EXPECTED_DECL
            "expecting a cangjie identifier" in normalized -> PARSE_EXPECTED_NAME
            "expecting identifier" in normalized -> PARSE_EXPECTED_NAME
            "expecting an identifier" in normalized -> PARSE_EXPECTED_NAME
            "expecting property name" in normalized -> PARSE_EXPECTED_NAME
            else -> null
        }
    }
}

object CjSyntaxErrorsDefaultMessages : BaseDiagnosticRendererFactory() {
    override val MAP by CjDiagnosticFactoryToRendererMap("CjSyntaxErrors") { map ->
        map.put(CjSyntaxErrors.LEX_UNKNOWN_SUFFIX, "Unknown suffix for number literal")
        map.put(CjSyntaxErrors.LEX_UNEXPECTED_DIGIT, "Unexpected digit in number literal")
        map.put(CjSyntaxErrors.PARSE_UNEXPECTED_DECLARATION_IN_SCOPE, "Invalid declaration in scope")
        map.put(CjSyntaxErrors.PARSE_EXPECTED_DECL, "Expecting declaration")
        map.put(CjSyntaxErrors.PARSE_EXPECTED_NAME, "Expecting name")
    }
}

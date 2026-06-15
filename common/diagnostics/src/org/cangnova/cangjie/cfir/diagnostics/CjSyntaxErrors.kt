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

    val PARSE_MISSING_BODY: CjDiagnosticFactory0 = CjDiagnosticFactory0(
        "parse_missing_body",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        PsiElement::class,
        getRendererFactory(),
    )

    val PARSE_EXPECTED_LT_PAREN: CjDiagnosticFactory0 = CjDiagnosticFactory0(
        "parse_expected_lt_paren",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        PsiElement::class,
        getRendererFactory(),
    )

    val PARSE_EXPECTED_ONE_OF_IDENTIFIER_OR_PATTERN: CjDiagnosticFactory0 = CjDiagnosticFactory0(
        "parse_expected_one_of_identifier_or_pattern",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        PsiElement::class,
        getRendererFactory(),
    )

    val PARSE_EXPECTED_CHARACTER: CjDiagnosticFactory0 = CjDiagnosticFactory0(
        "parse_expected_character",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        PsiElement::class,
        getRendererFactory(),
    )

    val PARSE_EXPECTED_CHARACTER_AFTER: CjDiagnosticFactory0 = CjDiagnosticFactory0(
        "parse_expected_character_after",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        PsiElement::class,
        getRendererFactory(),
    )

    val PARSE_EXPECTED_RIGHT_DELIMITER: CjDiagnosticFactory0 = CjDiagnosticFactory0(
        "parse_expected_right_delimiter",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        PsiElement::class,
        getRendererFactory(),
    )

    val PARSE_EXPECTED_LEFT_PAREN_AFTER: CjDiagnosticFactory0 = CjDiagnosticFactory0(
        "parse_expected_left_paren_after",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        PsiElement::class,
        getRendererFactory(),
    )

    val PARSE_EXPECTED_LEFT_PAREN: CjDiagnosticFactory0 = CjDiagnosticFactory0(
        "parse_expected_left_paren",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        PsiElement::class,
        getRendererFactory(),
    )

    val PARSE_EXPECTED_TYPE: CjDiagnosticFactory0 = CjDiagnosticFactory0(
        "parse_expected_type",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        PsiElement::class,
        getRendererFactory(),
    )

    val PARSE_EXPECTED_EXPRESSION: CjDiagnosticFactory0 = CjDiagnosticFactory0(
        "parse_expected_expression",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        PsiElement::class,
        getRendererFactory(),
    )

    val PARSE_EXPECTED_EXPR_OR_DECL_IN: CjDiagnosticFactory0 = CjDiagnosticFactory0(
        "parse_expected_expr_or_decl_in",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        PsiElement::class,
        getRendererFactory(),
    )

    val PARSE_VARRAY_TYPE_PARAMETER: CjDiagnosticFactory0 = CjDiagnosticFactory0(
        "parse_varray_type_parameter",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        PsiElement::class,
        getRendererFactory(),
    )

    val PARSE_VARRAY_TYPE_ARGS_MISMATCH: CjDiagnosticFactory0 = CjDiagnosticFactory0(
        "parse_varray_type_args_mismatch",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        PsiElement::class,
        getRendererFactory(),
    )

    val PARSE_EXPECT_INTEGER_LITERAL_VARRAY: CjDiagnosticFactory0 = CjDiagnosticFactory0(
        "parse_expect_integer_literal_varray",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        PsiElement::class,
        getRendererFactory(),
    )

    val PARSE_VARRAY_WITH_PAREN: CjDiagnosticFactory0 = CjDiagnosticFactory0(
        "parse_varray_with_paren",
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
            "function body expected" in normalized -> PARSE_MISSING_BODY
            "expecting a pattern expression" in normalized -> PARSE_EXPECTED_ONE_OF_IDENTIFIER_OR_PATTERN
            "unexpected tokens" in normalized -> PARSE_EXPECTED_CHARACTER
            "expecting a condition in parentheses" in normalized -> PARSE_EXPECTED_LEFT_PAREN_AFTER
            "expecting '(' to open a loop range" in normalized -> PARSE_EXPECTED_LEFT_PAREN_AFTER
            "expecting '(' to open a parameter list" in normalized -> PARSE_EXPECTED_LEFT_PAREN
            normalized == "expecting '('" -> PARSE_EXPECTED_LEFT_PAREN
            "expected type parameters after 'varray' keyword" in normalized -> PARSE_VARRAY_TYPE_PARAMETER
            "expected varray type arguments" in normalized -> PARSE_VARRAY_TYPE_ARGS_MISMATCH
            "expected an integer literal than or equal to 0 after '$'" in normalized -> PARSE_EXPECT_INTEGER_LITERAL_VARRAY
            "expected '(' or '{' after 'varray' for 'varray' constructor" in normalized -> PARSE_VARRAY_WITH_PAREN
            normalized == "expecting type" || normalized == "type expected" -> PARSE_EXPECTED_TYPE
            "expecting type" in normalized -> PARSE_EXPECTED_TYPE
            "expecting expression" in normalized || "expecting an expression" in normalized -> PARSE_EXPECTED_EXPRESSION
            "expected expression or declaration" in normalized -> PARSE_EXPECTED_EXPR_OR_DECL_IN
            isRightDelimiterParserMessage(normalized) -> PARSE_EXPECTED_RIGHT_DELIMITER
            "'.', '(', '[', '{' or '?'" in normalized -> PARSE_EXPECTED_CHARACTER_AFTER
            "expecting a cangjie identifier" in normalized -> PARSE_EXPECTED_NAME
            "expecting identifier" in normalized -> PARSE_EXPECTED_NAME
            "expecting an identifier" in normalized -> PARSE_EXPECTED_NAME
            "expecting property name" in normalized -> PARSE_EXPECTED_NAME
            else -> null
        }
    }

    data class ParserDiagnostic(
        val factory: CjDiagnosticFactory0,
        val startOffset: Int,
        val endOffset: Int,
    )

    fun diagnosticsForParserError(
        code: CharSequence,
        startOffset: Int,
        endOffset: Int,
        message: String?,
    ): List<ParserDiagnostic> {
        val normalized = message.orEmpty().lowercase()
        val factory = factoryForParserMessage(message) ?: return emptyList()
        if (
            factory == PARSE_EXPECTED_CHARACTER &&
            (isUnexpectedVArrayConstructorToken(code, startOffset) || isMalformedGenericTailEquals(code, startOffset, endOffset))
        ) {
            return emptyList()
        }
        val malformedVariableTypeTail = malformedVariableTypeTailAsteriskOffset(code, startOffset)
        if (malformedVariableTypeTail != null) {
            return when (factory) {
                PARSE_EXPECTED_DECL, PARSE_EXPECTED_CHARACTER -> {
                    if (startOffset <= malformedVariableTypeTail) {
                        listOf(ParserDiagnostic(PARSE_EXPECTED_CHARACTER, malformedVariableTypeTail, malformedVariableTypeTail + 1))
                    } else {
                        emptyList()
                    }
                }
                else -> emptyList()
            }
        }

        val primary = when (factory) {
            PARSE_UNEXPECTED_DECLARATION_IN_SCOPE ->
                ParserDiagnostic(factory, startOffset, unexpectedDeclarationKeywordEndOffset(code, startOffset, endOffset))
            PARSE_EXPECTED_ONE_OF_IDENTIFIER_OR_PATTERN -> {
                val range = followingTokenRange(code, startOffset)
                ParserDiagnostic(factory, range.first, range.second)
            }
            PARSE_EXPECTED_LEFT_PAREN_AFTER,
            PARSE_EXPECTED_LEFT_PAREN,
            PARSE_EXPECTED_CHARACTER_AFTER,
            PARSE_EXPECTED_TYPE,
            PARSE_EXPECTED_EXPR_OR_DECL_IN,
            -> {
                val range = followingTokenRange(code, startOffset)
                ParserDiagnostic(factory, range.first, range.second)
            }
            PARSE_EXPECTED_EXPRESSION -> {
                val range = expectedExpressionRange(code, startOffset)
                ParserDiagnostic(factory, range.first, range.second)
            }
            PARSE_EXPECTED_RIGHT_DELIMITER -> {
                val range = rightDelimiterRange(code, startOffset)
                ParserDiagnostic(factory, range.first, range.second)
            }
            PARSE_VARRAY_WITH_PAREN -> {
                val range = varrayConstructorDelimiterRange(code, startOffset)
                ParserDiagnostic(factory, range.first, range.second)
            }
            PARSE_EXPECT_INTEGER_LITERAL_VARRAY -> {
                val dollar = previousNonWhitespaceOffset(code, startOffset)
                    ?.takeIf { code[it] == '$' }
                    ?: startOffset
                ParserDiagnostic(factory, dollar, (dollar + 1).coerceAtMost(code.length))
            }
            PARSE_EXPECTED_CHARACTER -> {
                val range = unexpectedCharacterRange(code, startOffset, endOffset)
                ParserDiagnostic(factory, range.first, range.second)
            }
            PARSE_MISSING_BODY -> {
                val diagnosticEnd = if (startOffset < code.length && code[startOffset] == '\n') {
                    startOffset + 1
                } else {
                    endOffset
                }
                ParserDiagnostic(factory, startOffset, diagnosticEnd)
            }
            else -> ParserDiagnostic(factory, startOffset, endOffset)
        }

        if (factory != PARSE_MISSING_BODY || "function body expected" !in normalized) {
            return listOf(primary)
        }

        val expectedLtParen = followingNonWhitespaceOffset(code, startOffset)
            ?.takeIf { it < code.length && code[it] == '}' }
            ?.let { ParserDiagnostic(PARSE_EXPECTED_LT_PAREN, it, it + 1) }

        return listOfNotNull(primary, expectedLtParen)
    }

    /**
     * 官方 cjc 对“语句作用域内出现声明”的主诊断范围落在声明关键字。
     * PSI 与 light-tree 的错误节点都可能覆盖更宽范围，因此在语法诊断映射层统一收窄。
     */
    private fun unexpectedDeclarationKeywordEndOffset(
        code: CharSequence,
        startOffset: Int,
        endOffset: Int,
    ): Int {
        val keyword = declarationKeywords.firstOrNull { keyword ->
            startOffset + keyword.length <= endOffset &&
                    code.subSequence(startOffset, startOffset + keyword.length).toString() == keyword
        } ?: return endOffset
        return startOffset + keyword.length
    }

    private fun followingTokenRange(code: CharSequence, offset: Int): Pair<Int, Int> {
        val start = followingNonWhitespaceOffset(code, offset) ?: offset
        if (start >= code.length) return start to start
        if (code[start] == '`') {
            val end = (start + 1 until code.length).firstOrNull { code[it] == '`' }?.plus(1)
                ?: (start + 1)
            return start to end
        }
        var end = start
        while (end < code.length && (code[end].isLetterOrDigit() || code[end] == '_')) {
            end++
        }
        return start to if (end > start) end else start + 1
    }

    private fun unexpectedCharacterRange(code: CharSequence, startOffset: Int, endOffset: Int): Pair<Int, Int> {
        val boundedEnd = endOffset.coerceAtMost(code.length)
        val equalsOffset = (startOffset until boundedEnd).firstOrNull { code[it] == '=' }
        if (equalsOffset != null) return equalsOffset to equalsOffset + 1
        val start = followingNonWhitespaceOffset(code, startOffset) ?: startOffset
        return start to (start + 1).coerceAtMost(code.length)
    }

    private fun expectedExpressionRange(code: CharSequence, offset: Int): Pair<Int, Int> {
        val previous = previousNonWhitespaceOffset(code, offset)
        val lineCommentStart = lineCommentStartAtOrAfterOrContainingOffset(code, offset)
        if (previous != null && previous in code.indices && code[previous] == '>' && lineCommentStart != null) {
            val nextLineStart = code.indexOf('\n', lineCommentStart).takeIf { it >= 0 }?.plus(1)
            if (nextLineStart != null) {
                return followingTokenRange(code, nextLineStart)
            }
        }
        return followingTokenRange(code, offset)
    }

    private fun lineCommentStartAtOrAfterOrContainingOffset(code: CharSequence, offset: Int): Int? {
        followingNonWhitespaceOffset(code, offset)
            ?.takeIf { startsWithLineComment(code, it) }
            ?.let { return it }

        if (code.isEmpty()) return null
        val boundedOffset = offset.coerceIn(0, code.length - 1)
        val lineStart = code.lastIndexOf('\n', boundedOffset).let { if (it < 0) 0 else it + 1 }
        val lineEnd = code.indexOf('\n', boundedOffset).let { if (it < 0) code.length else it }
        var current = lineStart
        while (current + 1 < lineEnd) {
            if (startsWithLineComment(code, current)) {
                return current.takeIf { it <= offset }
            }
            current++
        }
        return null
    }

    /**
     * cjc 在变量类型后遇到顶层 `*` 时只报告首个非法字符为 `parse_expected_character`，
     * 后续同一行恢复出的声明级错误不是独立官方诊断。
     */
    private fun malformedVariableTypeTailAsteriskOffset(code: CharSequence, offset: Int): Int? {
        val lineStart = code.lastIndexOf('\n', (offset - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        val lineEnd = code.indexOf('\n', offset.coerceAtLeast(0)).let { if (it < 0) code.length else it }
        val line = code.subSequence(lineStart, lineEnd).toString()
        val relativeOffset = offset - lineStart
        val colonIndex = line.indexOf(':')
        if (colonIndex < 0 || colonIndex >= relativeOffset) return null

        val declarationPrefix = line.substring(0, colonIndex)
        if (!variableDeclarationKeywordRegex.containsMatchIn(declarationPrefix)) return null

        val equalsIndex = line.indexOf('=', colonIndex + 1).let { if (it < 0) line.length else it }
        var angleDepth = 0
        var parenDepth = 0
        var bracketDepth = 0
        for (index in colonIndex + 1 until equalsIndex) {
            when (line[index]) {
                '<' -> angleDepth++
                '>' -> if (angleDepth > 0) angleDepth--
                '(' -> parenDepth++
                ')' -> if (parenDepth > 0) parenDepth--
                '[' -> bracketDepth++
                ']' -> if (bracketDepth > 0) bracketDepth--
                '*' -> if (angleDepth == 0 && parenDepth == 0 && bracketDepth == 0) {
                    val asteriskOffset = lineStart + index
                    return asteriskOffset.takeIf { offset >= it }
                }
            }
        }

        return null
    }

    private fun isRightDelimiterParserMessage(normalized: String): Boolean {
        return normalized == "expecting ')'" ||
                normalized == "expecting ']'" ||
                normalized == "expecting '}'" ||
                normalized == "expecting '>'" ||
                normalized == "missing ')'" ||
                normalized == "missing ']'" ||
                normalized == "missing '}'" ||
                normalized == "missing '>'" ||
                normalized == "should be '>'"
    }

    private fun followingNonWhitespaceOffset(code: CharSequence, offset: Int): Int? {
        var current = offset.coerceAtLeast(0)
        while (current < code.length && code[current].isWhitespace()) {
            current++
        }
        return current.takeIf { it < code.length }
    }

    private fun previousNonWhitespaceOffset(code: CharSequence, offset: Int): Int? {
        var current = (offset - 1).coerceAtMost(code.length - 1)
        while (current >= 0) {
            while (current >= 0 && code[current].isWhitespace()) {
                current--
            }
            if (current < 0) return null

            val lineStart = code.lastIndexOf('\n', current).let { if (it < 0) 0 else it + 1 }
            val linePrefix = code.subSequence(lineStart, current + 1).toString()
            val lineCommentStart = linePrefix.indexOf("//")
            if (lineCommentStart >= 0) {
                current = lineStart + lineCommentStart - 1
                continue
            }
            return current
        }
        return null
    }

    private fun isUnexpectedVArrayConstructorToken(code: CharSequence, offset: Int): Boolean {
        val previous = previousNonWhitespaceOffset(code, offset) ?: return false
        if (previous !in code.indices || code[previous] != '>') return false
        val lineStart = code.lastIndexOf('\n', previous).let { if (it < 0) 0 else it + 1 }
        val prefix = code.subSequence(lineStart, previous + 1).toString()
        return Regex("""\bVArray\s*<""").containsMatchIn(prefix)
    }

    private fun isMalformedGenericTailEquals(code: CharSequence, startOffset: Int, endOffset: Int): Boolean {
        val boundedStart = startOffset.coerceIn(0, code.length)
        val boundedEnd = endOffset.coerceIn(boundedStart, code.length)
        val equalsOffset = (boundedStart until boundedEnd).firstOrNull { code[it] == '=' }
            ?: boundedStart.takeIf { it in code.indices && code[it] == '=' }
            ?: return false
        val offset = equalsOffset
        val lineStart = code.lastIndexOf('\n', offset).let { if (it < 0) 0 else it + 1 }
        val prefix = code.subSequence(lineStart, offset).toString()
        return Regex("""<[^>\n]*,\s*\$\w+\s*>\s*$""").containsMatchIn(prefix)
    }

    private fun varrayConstructorDelimiterRange(code: CharSequence, offset: Int): Pair<Int, Int> {
        val previous = previousNonWhitespaceOffset(code, offset)
        if (previous != null && previous in code.indices && code[previous] == '>') {
            val next = followingNonWhitespaceOffset(code, offset) ?: offset
            val hasNewLineBeforeNext = (previous + 1 until next.coerceAtMost(code.length)).any { code[it] == '\n' }
            if (hasNewLineBeforeNext) {
                val diagnosticOffset = previous + 1
                return diagnosticOffset to diagnosticOffset
            }
        }
        return followingTokenRange(code, offset)
    }

    private fun rightDelimiterRange(code: CharSequence, offset: Int): Pair<Int, Int> {
        val previous = previousNonWhitespaceOffset(code, offset)
        if (previous != null && previous in code.indices) {
            val followingComment = followingNonWhitespaceOffset(code, offset)
                ?.takeIf { startsWithLineComment(code, it) }
            if (followingComment != null) {
                val diagnosticStart = previous + 1
                return diagnosticStart to followingComment.coerceAtLeast(diagnosticStart)
            }
            if (code[previous] == '$' && offset < code.length && code[offset].isDigit()) {
                return previous to previous + 1
            }
            val next = followingNonWhitespaceOffset(code, offset) ?: offset
            val hasNewLineBeforeNext = (previous + 1 until next.coerceAtMost(code.length)).any { code[it] == '\n' }
            if (code[previous] == '}' && hasNewLineBeforeNext) {
                val diagnosticStart = previous + 1
                val diagnosticEnd = if (diagnosticStart < code.length && code[diagnosticStart] != '\n') {
                    diagnosticStart + 1
                } else {
                    diagnosticStart
                }
                return diagnosticStart to diagnosticEnd.coerceAtMost(code.length)
            }
        }
        return followingTokenRange(code, offset)
    }

    private fun startsWithLineComment(code: CharSequence, offset: Int): Boolean =
        offset + 1 < code.length && code[offset] == '/' && code[offset + 1] == '/'

    private val declarationKeywords = listOf(
        "class",
        "struct",
        "interface",
        "enum",
        "func",
        "macro",
        "extend",
    )

    private val variableDeclarationKeywordRegex = Regex("""\b(var|let|prop)\b""")
}

object CjSyntaxErrorsDefaultMessages : BaseDiagnosticRendererFactory() {
    override val MAP by CjDiagnosticFactoryToRendererMap("CjSyntaxErrors") { map ->
        map.put(CjSyntaxErrors.LEX_UNKNOWN_SUFFIX, "Unknown suffix for number literal")
        map.put(CjSyntaxErrors.LEX_UNEXPECTED_DIGIT, "Unexpected digit in number literal")
        map.put(CjSyntaxErrors.PARSE_UNEXPECTED_DECLARATION_IN_SCOPE, "Invalid declaration in scope")
        map.put(CjSyntaxErrors.PARSE_EXPECTED_DECL, "Expecting declaration")
        map.put(CjSyntaxErrors.PARSE_EXPECTED_NAME, "Expecting name")
        map.put(CjSyntaxErrors.PARSE_MISSING_BODY, "Body is missing")
        map.put(CjSyntaxErrors.PARSE_EXPECTED_LT_PAREN, "Expected '(' or '<'")
        map.put(CjSyntaxErrors.PARSE_EXPECTED_ONE_OF_IDENTIFIER_OR_PATTERN, "Expected identifier or pattern")
        map.put(CjSyntaxErrors.PARSE_EXPECTED_CHARACTER, "Expected character")
        map.put(CjSyntaxErrors.PARSE_EXPECTED_CHARACTER_AFTER, "Expected character after token")
        map.put(CjSyntaxErrors.PARSE_EXPECTED_RIGHT_DELIMITER, "Expected right delimiter")
        map.put(CjSyntaxErrors.PARSE_EXPECTED_LEFT_PAREN_AFTER, "Expected '(' after keyword")
        map.put(CjSyntaxErrors.PARSE_EXPECTED_LEFT_PAREN, "Expected '('")
        map.put(CjSyntaxErrors.PARSE_EXPECTED_TYPE, "Expected type")
        map.put(CjSyntaxErrors.PARSE_EXPECTED_EXPRESSION, "Expected expression")
        map.put(CjSyntaxErrors.PARSE_EXPECTED_EXPR_OR_DECL_IN, "Expected expression or declaration")
        map.put(CjSyntaxErrors.PARSE_VARRAY_TYPE_PARAMETER, "Expected type parameters after 'VArray' keyword")
        map.put(CjSyntaxErrors.PARSE_VARRAY_TYPE_ARGS_MISMATCH, "Expected VArray type arguments")
        map.put(CjSyntaxErrors.PARSE_EXPECT_INTEGER_LITERAL_VARRAY, "Expected VArray size literal")
        map.put(CjSyntaxErrors.PARSE_VARRAY_WITH_PAREN, "Expected '(' or '{' after 'VArray' constructor")
    }
}

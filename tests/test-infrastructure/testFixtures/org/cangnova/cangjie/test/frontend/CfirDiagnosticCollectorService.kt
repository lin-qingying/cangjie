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

package org.cangnova.cangjie.test.frontend

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import org.cangnova.cangjie.CjPsiSourceFile
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.*
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cfir.pipeline.runCheckers
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.session.lazyDeclarationResolver
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement
import org.cangnova.cangjie.source.toCjPsiSourceElement
import org.cangnova.cangjie.test.services.TestService
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.sourceFileProvider
import org.cangnova.cangjie.test.services.toLightTreeShortName

private typealias CfirDiagnosticsMap = Map<CfirFile, List<CjDiagnostic>>

open class CfirDiagnosticCollectorService(
    @Suppress("UNUSED_PARAMETER") val testServices: TestServices,
) : TestService {
    open val reporterForLTSyntaxErrors: DiagnosticReporter = DiagnosticsCollectorImpl()

    private val cache: MutableMap<CfirOutputArtifact, CfirDiagnosticsMap> = mutableMapOf()

    open fun getFrontendDiagnosticsForModule(info: CfirOutputArtifact): CfirDiagnosticsMap {
        return cache.getOrPut(info) { computeDiagnostics(info) }
    }

    val containsErrorDiagnostics: Boolean
        get() = cache.values.any { perFile ->
            perFile.values.flatten().any { it.severity == Severity.ERROR }
        }

    fun containsErrors(info: CfirOutputArtifact): Boolean {
        return getFrontendDiagnosticsForModule(info).values.flatten().any { it.severity == Severity.ERROR }
    }

    private fun computeDiagnostics(info: CfirOutputArtifact): CfirDiagnosticsMap {
        val allFiles = info.partsForDependsOnModules.flatMap { it.firFilesByTestFile.values }
        val diagnosticsByFile = linkedMapOf<CfirFile, MutableList<CjDiagnostic>>()
        allFiles.forEach { diagnosticsByFile[it] = mutableListOf() }

        val platformPart = info.partsForDependsOnModules.last()
        val lazyDeclarationResolver = platformPart.session.lazyDeclarationResolver

        lazyDeclarationResolver.disableLazyResolveContractChecksInside {
            for (part in info.partsForDependsOnModules) {
                val diagnosticsCollector = DiagnosticsCollectorImpl()
                val diagnostics = part.session.runCheckers(
                    scopeSession = part.scopeSession,
                    firFiles = part.firFilesByTestFile.values,
                    diagnosticsCollector = diagnosticsCollector,
                )
                val syntaxDiagnosticsByFile = collectSyntaxDiagnostics(part)
                val filesWithSyntaxErrors = syntaxDiagnosticsByFile
                    .filterValues { diagnosticsForFile -> diagnosticsForFile.any { it.severity == Severity.ERROR } }
                    .keys

                appendComputedDiagnostics(diagnostics, diagnosticsByFile, filesWithSyntaxErrors)
                appendSyntaxDiagnostics(syntaxDiagnosticsByFile, diagnosticsByFile)
            }
        }

        return diagnosticsByFile.mapValues { (_, value) -> value.toList() }
    }

    private fun appendComputedDiagnostics(
        diagnostics: CfirDiagnosticsMap,
        destination: MutableMap<CfirFile, MutableList<CjDiagnostic>>,
        filesToSkip: Set<CfirFile>,
    ) {
        for ((file, fileDiagnostics) in diagnostics) {
            if (file in filesToSkip) continue
            if (fileDiagnostics.isEmpty()) continue
            destination.getOrPut(file) { mutableListOf() }.addAll(fileDiagnostics)
        }
    }

    private fun collectSyntaxDiagnostics(
        part: CfirOutputPartForDependsOnModule,
    ): Map<CfirFile, List<CjDiagnostic>> {
        val result = linkedMapOf<CfirFile, MutableList<CjDiagnostic>>()
        appendNumericLiteralLexDiagnostics(part, result)
        appendLightTreeSyntaxDiagnostics(part, result)
        appendPsiSyntaxDiagnostics(part, result)
        return result.mapValues { (_, value) -> value.toList() }
    }

    private fun appendSyntaxDiagnostics(
        diagnostics: Map<CfirFile, List<CjDiagnostic>>,
        destination: MutableMap<CfirFile, MutableList<CjDiagnostic>>,
    ) {
        for ((file, fileDiagnostics) in diagnostics) {
            if (fileDiagnostics.isEmpty()) continue
            destination.getOrPut(file) { mutableListOf() }.addAll(fileDiagnostics)
        }
    }

    private fun appendLightTreeSyntaxDiagnostics(
        part: CfirOutputPartForDependsOnModule,
        destination: MutableMap<CfirFile, MutableList<CjDiagnostic>>,
    ) {
        val lightTreeReporter = reporterForLTSyntaxErrors as? DiagnosticsCollectorImpl ?: return
        val diagnosticsByPath = lightTreeReporter.diagnosticsByFilePath

        for ((testFile, firFile) in part.firFilesByTestFile) {
            val path = "/${testFile.toLightTreeShortName()}"
            val diagnostics = diagnosticsByPath[path].orEmpty()
            if (diagnostics.isEmpty()) continue
            destination.getOrPut(firFile) { mutableListOf() }.addAll(diagnostics)
        }
    }

    private fun appendNumericLiteralLexDiagnostics(
        part: CfirOutputPartForDependsOnModule,
        destination: MutableMap<CfirFile, MutableList<CjDiagnostic>>,
    ) {
        for ((testFile, firFile) in part.firFilesByTestFile) {
            val diagnostics = collectNumericLiteralLexDiagnostics(
                code = testServices.sourceFileProvider.getContentOfSourceFile(testFile),
                firFile = firFile,
                part = part,
            )
            if (diagnostics.isEmpty()) continue
            destination.getOrPut(firFile) { mutableListOf() }.addAll(diagnostics)
        }
    }

    private fun appendPsiSyntaxDiagnostics(
        part: CfirOutputPartForDependsOnModule,
        destination: MutableMap<CfirFile, MutableList<CjDiagnostic>>,
    ) {
        for ((_, firFile) in part.firFilesByTestFile) {
            val psiFile = (firFile.sourceFile as? CjPsiSourceFile)?.psiFile ?: continue
            val diagnostics = collectPsiSyntaxDiagnostics(psiFile, firFile, part)
            if (diagnostics.isEmpty()) continue
            destination.getOrPut(firFile) { mutableListOf() }.addAll(diagnostics)
        }
    }

    private fun collectPsiSyntaxDiagnostics(
        root: PsiElement,
        firFile: CfirFile,
        part: CfirOutputPartForDependsOnModule,
    ): List<CjDiagnostic> {
        val collector = DiagnosticsCollectorImpl()
        val context = object : DiagnosticContext {
            override val languageVersionSettings = part.session.languageVersionSettings
            override val containingFilePath: String? = firFile.sourceFile?.path
            override fun isDiagnosticSuppressed(diagnostic: CjDiagnostic): Boolean = false
        }

        root.accept(object : com.intellij.psi.PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is PsiErrorElement) {
                    CjSyntaxErrors.diagnosticsForParserError(
                        code = element.containingFile.text,
                        startOffset = element.textRange.startOffset,
                        endOffset = element.textRange.endOffset,
                        message = element.errorDescription,
                    ).forEach { diagnostic ->
                        collector.reportOn(
                            CjOffsetsOnlySourceElement(diagnostic.startOffset, diagnostic.endOffset),
                            diagnostic.factory,
                            context,
                        )
                    }
                }
                super.visitElement(element)
            }
        })
        return collector.diagnostics
    }

    /**
     * 按官方 cjc Lexer 的数字字面量阶段补齐 LLT 词法诊断。
     *
     * 官方实现先消费整数部分和合法类型后缀，再处理小数、指数与浮点后缀；
     * 只有这些合法组成部分之后仍存在标识符样尾部时才报 `lex_unknown_suffix`。
     */
    private fun collectNumericLiteralLexDiagnostics(
        code: String,
        firFile: CfirFile,
        part: CfirOutputPartForDependsOnModule,
    ): List<CjDiagnostic> {
        val collector = DiagnosticsCollectorImpl()
        val context = object : DiagnosticContext {
            override val languageVersionSettings = part.session.languageVersionSettings
            override val containingFilePath: String? = firFile.sourceFile?.path
            override fun isDiagnosticSuppressed(diagnostic: CjDiagnostic): Boolean = false
        }

        var index = 0
        while (index < code.length) {
            index = when {
                code.startsWith("//", index) -> skipLineComment(code, index + 2)
                code.startsWith("/*", index) -> skipBlockComment(code, index + 2)
                code[index] == '"' || code[index] == '\'' -> skipQuotedLiteral(code, index)
                code[index].isIdentifierStart() -> skipIdentifier(code, index)
                code[index].isDigit() -> scanNumberLiteralDiagnostic(code, index, collector, context)
                else -> index + 1
            }
        }

        return collector.diagnostics
    }

    private fun scanNumberLiteralDiagnostic(
        code: String,
        start: Int,
        collector: DiagnosticsCollectorImpl,
        context: DiagnosticContext,
    ): Int {
        var index = start
        var base = 10
        var hasFloatShape = false

        if (code[index] == '0' && index + 1 < code.length) {
            when (code[index + 1].lowercaseChar()) {
                'x' -> {
                    base = 16
                    index += 2
                }
                'o' -> {
                    base = 8
                    index += 2
                }
                'b' -> {
                    base = 2
                    index += 2
                }
            }
        }

        val integerDigits = consumeNumberDigits(code, index, base, collector, context)
        if (integerDigits.reportedError) return integerDigits.nextIndex
        index = integerDigits.nextIndex

        if (index < code.length && code[index].isIntegerSuffixStart()) {
            return consumeIntegerSuffixOrReport(code, index, collector, context)
        }

        if (index < code.length && code[index] == '.' && !isRangeOperatorStart(code, index)) {
            val fractionStart = index + 1
            if (fractionStart < code.length && code[fractionStart].isNumberDigitForBase(base)) {
                hasFloatShape = true
                val fractionDigits = consumeNumberDigits(code, fractionStart, base, collector, context)
                if (fractionDigits.reportedError) return fractionDigits.nextIndex
                index = fractionDigits.nextIndex
            }
        }

        if (index < code.length && code[index].isExponentMarkerForBase(base)) {
            hasFloatShape = true
            index++
            if (index < code.length && code[index] == '-') index++

            val exponentStart = index
            val exponentDigits = consumeNumberDigits(code, index, 10, collector, context)
            if (exponentDigits.reportedError) return exponentDigits.nextIndex
            index = exponentDigits.nextIndex

            if (exponentStart == index) {
                reportLexDiagnostic(
                    collector,
                    context,
                    exponentStart - 1,
                    exponentStart,
                    CjSyntaxErrors.LEX_UNEXPECTED_DIGIT
                )
                return skipNumberSuffixTail(code, exponentStart)
            }
        }

        if (index < code.length && code[index] == 'f') {
            return consumeFloatSuffixOrReport(code, index, base, collector, context)
        }

        if (index < code.length && code[index].isIdentifierStart()) {
            val suffixStart = index
            val suffixEnd = skipNumberSuffixTail(code, index)
            reportLexDiagnostic(collector, context, suffixStart, suffixEnd, CjSyntaxErrors.LEX_UNKNOWN_SUFFIX)
            return suffixEnd
        }

        return when {
            hasFloatShape -> index
            index == start -> start + 1
            else -> index
        }
    }

    private data class NumberDigitsResult(
        val nextIndex: Int,
        val reportedError: Boolean,
    )

    private fun consumeNumberDigits(
        code: String,
        start: Int,
        base: Int,
        collector: DiagnosticsCollectorImpl,
        context: DiagnosticContext,
    ): NumberDigitsResult {
        var index = start
        while (index < code.length) {
            val ch = code[index]
            when {
                ch == '_' -> index++
                ch.isDigit() -> {
                    if (ch.digitToInt() >= base) {
                        reportLexDiagnostic(collector, context, index, index + 1, CjSyntaxErrors.LEX_UNEXPECTED_DIGIT)
                        return NumberDigitsResult(skipNumberSuffixTail(code, index + 1), reportedError = true)
                    }
                    index++
                }
                base == 16 && ch.isHexLetter() -> index++
                base != 16 && ch.isHexLetter() && ch.lowercaseChar() !in EXPONENT_OR_FLOAT_SUFFIX_STARTS -> {
                    reportLexDiagnostic(collector, context, index, index + 1, CjSyntaxErrors.LEX_UNEXPECTED_DIGIT)
                    return NumberDigitsResult(skipNumberSuffixTail(code, index + 1), reportedError = true)
                }

                else -> return NumberDigitsResult(index, reportedError = false)
            }
        }
        return NumberDigitsResult(index, reportedError = false)
    }

    private fun consumeIntegerSuffixOrReport(
        code: String,
        suffixStart: Int,
        collector: DiagnosticsCollectorImpl,
        context: DiagnosticContext,
    ): Int {
        val suffixEnd = skipIdentifier(code, suffixStart)
        val suffix = code.substring(suffixStart, suffixEnd)
        if (suffix in INTEGER_SUFFIXES) return suffixEnd
        reportLexDiagnostic(collector, context, suffixStart, suffixEnd, CjSyntaxErrors.LEX_UNKNOWN_SUFFIX)
        return suffixEnd
    }

    private fun consumeFloatSuffixOrReport(
        code: String,
        suffixStart: Int,
        base: Int,
        collector: DiagnosticsCollectorImpl,
        context: DiagnosticContext,
    ): Int {
        val suffixEnd = skipIdentifier(code, suffixStart)
        val suffix = code.substring(suffixStart, suffixEnd)
        if (base == 10 && suffix in FLOAT_SUFFIXES) return suffixEnd
        reportLexDiagnostic(collector, context, suffixStart, suffixEnd, CjSyntaxErrors.LEX_UNKNOWN_SUFFIX)
        return suffixEnd
    }

    private fun reportLexDiagnostic(
        collector: DiagnosticsCollectorImpl,
        context: DiagnosticContext,
        startOffset: Int,
        endOffset: Int,
        factory: org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory0,
    ) {
        collector.reportOn(
            CjOffsetsOnlySourceElement(startOffset, endOffset),
            factory,
            context,
        )
    }

    private fun skipNumberSuffixTail(code: String, start: Int): Int {
        var index = start
        while (index < code.length && (code[index].isLetterOrDigit() || code[index] == '_' || code[index] == '.')) {
            if (code[index] == '.' && index + 1 < code.length && code[index + 1] == '.') break
            index++
        }
        return index
    }

    private fun skipIdentifier(code: String, start: Int): Int {
        var index = start + 1
        while (index < code.length && code[index].isIdentifierPart()) index++
        return index
    }

    private fun skipLineComment(code: String, start: Int): Int {
        var index = start
        while (index < code.length && code[index] != '\n' && code[index] != '\r') index++
        return index
    }

    private fun skipBlockComment(code: String, start: Int): Int {
        var index = start
        while (index + 1 < code.length) {
            if (code[index] == '*' && code[index + 1] == '/') return index + 2
            index++
        }
        return code.length
    }

    private fun skipQuotedLiteral(code: String, start: Int): Int {
        val quote = code[start]
        var index = start + 1
        while (index < code.length) {
            if (code[index] == '\\') {
                index += 2
                continue
            }
            if (code[index] == quote) return index + 1
            index++
        }
        return code.length
    }

    private fun Char.isIdentifierStart(): Boolean = this == '_' || isLetter()

    private fun Char.isIdentifierPart(): Boolean = isIdentifierStart() || isDigit()

    private fun Char.isHexLetter(): Boolean = lowercaseChar() in 'a'..'f'

    private fun Char.isNumberDigitForBase(base: Int): Boolean = when {
        isDigit() -> digitToInt() < base
        base == 16 -> isHexLetter()
        else -> false
    }

    private fun Char.isIntegerSuffixStart(): Boolean = this == 'i' || this == 'u'

    private fun Char.isExponentMarkerForBase(base: Int): Boolean = when (base) {
        10 -> this == 'e' || this == 'E'
        16 -> this == 'p' || this == 'P'
        else -> false
    }

    private fun isRangeOperatorStart(code: String, index: Int): Boolean =
        index + 1 < code.length && code[index + 1] == '.'

    private companion object {
        val INTEGER_SUFFIXES: Set<String> = setOf("u8", "u16", "u32", "u64", "i8", "i16", "i32", "i64")
        val FLOAT_SUFFIXES: Set<String> = setOf("f16", "f32", "f64")
        val EXPONENT_OR_FLOAT_SUFFIX_STARTS: Set<Char> = setOf('e', 'f')
    }
}

private fun String.normalizePath(): String = replace('\\', '/')

val TestServices.cfirDiagnosticCollectorService: CfirDiagnosticCollectorService by TestServices.testServiceAccessor()

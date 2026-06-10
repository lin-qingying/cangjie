package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjSyntaxErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.Severity
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.pipeline.runCheckers
import org.cangnova.cangjie.cfir.session.lazyDeclarationResolver
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.CjPsiSourceFile
import org.cangnova.cangjie.test.services.TestService
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.sourceFileProvider
import org.cangnova.cangjie.test.services.toLightTreeShortName
import org.cangnova.cangjie.source.toCjPsiSourceElement
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement

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
                    val factory = CjSyntaxErrors.factoryForParserMessage(element.errorDescription)
                    if (factory != null) {
                        collector.reportOn(element.toCjPsiSourceElement(), factory, context)
                    }
                }
                super.visitElement(element)
            }
        })
        return collector.diagnostics
    }

    /**
     * 按官方 cjc Lexer 的数字字面量错误归类补齐 LLT 词法诊断。
     *
     * 这里不复刻完整 token 流，只处理官方 `ProcessDigits` 与
     * `ProcessNumberFloatSuffix` 对数字后紧邻非法字符的诊断位置：
     * 十进制数字后紧邻 `a..d` 是 `lex_unexpected_digit`，
     * 其他紧邻标识符样后缀是 `lex_unknown_suffix`。
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
        var reasonPoint = start

        if (code[index] == '0' && index + 1 < code.length) {
            when (code[index + 1].lowercaseChar()) {
                'x' -> {
                    base = 16
                    reasonPoint = index + 1
                    index += 2
                }
                'o' -> {
                    base = 8
                    reasonPoint = index + 1
                    index += 2
                }
                'b' -> {
                    base = 2
                    reasonPoint = index + 1
                    index += 2
                }
            }
        }

        while (index < code.length) {
            val ch = code[index]
            when {
                ch == '_' -> index++
                ch.isDigit() -> {
                    if (ch.digitToInt() >= base) {
                        reportLexDiagnostic(collector, context, index, index + 1, CjSyntaxErrors.LEX_UNEXPECTED_DIGIT)
                        return skipNumberSuffixTail(code, index + 1)
                    }
                    index++
                }
                base == 16 && ch.isHexLetter() -> index++
                base != 16 && ch.isHexLetter() && ch.lowercaseChar() !in setOf('e', 'f') -> {
                    reportLexDiagnostic(collector, context, index, index + 1, CjSyntaxErrors.LEX_UNEXPECTED_DIGIT)
                    return skipNumberSuffixTail(code, index + 1)
                }
                else -> break
            }
        }

        if (index < code.length && code[index].isIdentifierStart()) {
            val suffixStart = index
            val suffixEnd = skipNumberSuffixTail(code, index)
            reportLexDiagnostic(collector, context, suffixStart, suffixEnd, CjSyntaxErrors.LEX_UNKNOWN_SUFFIX)
            return suffixEnd
        }

        return if (index == start) reasonPoint + 1 else index
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
}

private fun String.normalizePath(): String = replace('\\', '/')

val TestServices.cfirDiagnosticCollectorService: CfirDiagnosticCollectorService by TestServices.testServiceAccessor()

package org.cangnova.cangjie.parsing

import com.intellij.psi.tree.IElementType
import com.intellij.lang.PsiBuilder
import com.intellij.psi.TokenType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.messages.CjParsingDiagnosticOrder
import org.cangnova.cangjie.messages.CjParsingDiagnosticReport
import org.cangnova.cangjie.messages.CjParsingDiagnosticScope
import java.util.Collections
import java.util.UUID

/**
 * 一个不可变全文输入的真实词法序列。每个独立 parser 根使用该序列的切片；
 * 同一输入在不同编译语境下保留词元身份，检查结果和 parser journal 身份仍各自独立。
 */
class CjLexicalSourceInput(val source: CjParsingSourceSnapshot) {
    class Token internal constructor(
        val id: CjSourceTokenId,
        val type: IElementType,
        val startOffset: Int,
        val endOffset: Int,
    )

    val tokens: List<Token>
    val descriptor: CjLexicalSourceDescriptor
    private val indicesByStart: Map<Int, Int>
    private val lexicalReports: Map<CjSourceTokenId, CjParsingDiagnosticReport>

    init {
        val lexer = CangJieLexer()
        lexer.start(source.text)
        val collected = mutableListOf<Token>()
        val reports = linkedMapOf<CjSourceTokenId, CjParsingDiagnosticReport>()
        val diagnosticScope = CjParsingDiagnosticScope(UUID.randomUUID())
        while (lexer.tokenType != null) {
            val token = Token(CjSourceTokenId(source.identity, collected.size), checkNotNull(lexer.tokenType),
                lexer.tokenStart, lexer.tokenEnd)
            collected += token
            lexicalDiagnosticForToken(token.type, source.text.substring(token.startOffset, token.endOffset),
                token.startOffset, token.endOffset)?.let { diagnostic ->
                reports[token.id] = CjParsingDiagnosticReport(diagnostic.copy(source = source),
                    CjParsingDiagnosticOrder(diagnosticScope, reports.size.toLong()))
            }
            lexer.advance()
        }
        tokens = Collections.unmodifiableList(collected)
        descriptor = CjLexicalSourceDescriptor(source.identity, tokens.size)
        indicesByStart = tokens.associate { it.startOffset to it.id.ordinal }
        lexicalReports = Collections.unmodifiableMap(reports)
    }

    /** 当前 producer 以自身词元边界取得实际身份；这个定位不能用于跨树选择声明 owner。 */
    fun tokenAt(startOffset: Int): Token = tokens[checkNotNull(indicesByStart[startOffset]) {
        "The parser anchor must be a token in its actual lexical input."
    }]

    fun contains(id: CjSourceTokenId): Boolean = id.sourceIdentity == source.identity && id.ordinal in tokens.indices

    /** 实际 AST 词元保存冻结词法报告的来源；移动后仍通过该载体读取原 producer，不能补序号。 */
    internal fun diagnosticOriginForToken(id: CjSourceTokenId): CjLexicalDiagnosticOrigin? {
        require(contains(id))
        val token = tokens[id.ordinal]
        check(token.id === id) { "A lexical carrier requires its actual frozen token object." }
        val report = lexicalReports[id] ?: return null
        return CjLexicalDiagnosticOrigin(source, token, report)
    }

    /** 词法 producer 已冻结的报告；树消费者只能通过实际词元身份读取，不能在访问时补序号。 */
    internal fun diagnosticForToken(id: CjSourceTokenId, context: CjParserSourceContext): CjParsingDiagnosticReport? {
        require(contains(id)) { "A lexical report must belong to its actual frozen input." }
        require(context.snapshot === source)
        val token = tokens[id.ordinal]
        require(token.startOffset >= context.inputStart && token.endOffset <= context.inputEnd) {
            "A lexical report must belong to the actual parser input view."
        }
        val report = lexicalReports[id] ?: return null
        // producer 保存全文位置；输入视图只适配坐标，source=null 明确表示本次输入，不伪装为继承来源。
        return report.copy(diagnostic = report.diagnostic.copy(
            startOffset = report.diagnostic.startOffset - context.inputStart,
            endOffset = report.diagnostic.endOffset - context.inputStart,
            source = null,
        ))
    }

    /**
     * 文法开始前验证完整 SDK raw 序列，包含其自动跳过的空白和注释。
     * 当前游标以前只能有真实 trivia，不能把已消费的声明或伪装为空白的输入发布为完整文件。
     */
    internal fun requireBuilderInput(builder: PsiBuilder, context: CjParserSourceContext, atEntry: Boolean) {
        require(source === context.snapshot)
        context.requireInput(builder.originalText)
        val first = boundaryAt(context.inputStart)
        val end = boundaryAt(context.inputEnd)
        val current = builder.rawTokenIndex()
        val rawOrigin = -current
        for (ordinal in first until end) {
            val relative = rawOrigin + ordinal - first
            val token = tokens[ordinal]
            val actual = checkNotNull(builder.rawLookup(relative)) { "The SDK token stream ended before its actual lexical input." }
            val start = builder.rawTokenTypeStart(relative) + context.inputStart
            val finish = builder.rawTokenTypeStart(relative + 1) + context.inputStart
            check(start == token.startOffset && finish == token.endOffset && matchesParserKind(token, actual)) {
                "The SDK token stream must retain every actual lexical token and its extent."
            }
            if (atEntry && ordinal - first < current) {
                check(token.type in CjTokens.ALL_WHITESPACE_TOKENS || token.type in CjTokens.COMMENTS ||
                    token.type == CjTokens.NL || token.type == TokenType.WHITE_SPACE) {
                    "A parser input cannot start after an already consumed grammar token."
                }
            }
        }
        check(builder.rawLookup(rawOrigin + end - first) == null &&
            builder.rawTokenTypeStart(rawOrigin + end - first) == context.inputEnd - context.inputStart) {
            "The SDK token stream must end at its exact lexical input boundary."
        }
    }

    /** 允许的 remap 来自真实文法；任意源词元不能被伪装为 SDK 空白。 */
    internal fun matchesParserKind(token: Token, parsed: IElementType): Boolean {
        val original = token.type
        if (parsed == original) return true
        if (original == CjTokens.NL && parsed == TokenType.WHITE_SPACE) return true
        if (original is CjKeywordToken && original.isSoft && parsed == CjTokens.IDENTIFIER) return true
        return original == CjTokens.IDENTIFIER && parsed is CjKeywordToken && parsed.isSoft &&
            source.text.substring(token.startOffset, token.endOffset) == parsed.value
    }

    /** 文法节点的消费边界以词元序号表达，诊断坐标仍单独保留原始 UTF-16 范围。 */
    fun boundaryAt(offset: Int): Int {
        require(offset in 0..source.text.length)
        if (offset == source.text.length) return tokens.size
        indicesByStart[offset]?.let { return it }
        val index = tokens.binarySearch { it.endOffset.compareTo(offset) }
        check(index >= 0) { "A syntax boundary must coincide with an actual token boundary." }
        return index + 1
    }
}

/** 原词元和报告只属于 producer 输入，当前显示坐标由实际 PSI 叶确定。 */
internal class CjLexicalDiagnosticOrigin(
    val source: CjParsingSourceSnapshot,
    val token: CjLexicalSourceInput.Token,
    val report: CjParsingDiagnosticReport,
) {
    init {
        require(token.id.sourceIdentity == source.identity && report.diagnostic.source === source)
        require(report.diagnostic.category == org.cangnova.cangjie.messages.ParsingDiagnostic.Category.LEX)
        require(report.diagnostic.rangeKind == org.cangnova.cangjie.messages.ParsingDiagnostic.RangeKind.POINT)
    }
}

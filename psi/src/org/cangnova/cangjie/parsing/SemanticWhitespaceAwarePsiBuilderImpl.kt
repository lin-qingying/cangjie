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

package org.cangnova.cangjie.parsing

import org.cangnova.cangjie.lexer.CjTokens
import com.intellij.lang.PsiBuilder
import com.intellij.lang.SyntaxTreeBuilder
import com.intellij.lang.impl.PsiBuilderAdapter
import com.intellij.lang.impl.PsiBuilderImpl
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.util.containers.Stack

/**
 * 表示 `SemanticWhitespaceAwarePsiBuilderImpl`，承载仓颉语法解析中的语法节点、索引桩或辅助模型。
 */
class SemanticWhitespaceAwarePsiBuilderImpl(
    delegate: PsiBuilder,
    sourceContext: CjParserSourceContext =
        (delegate as? CangJiePsiBuilder)?.sourceContext ?: CjParserSourceContext.complete(delegate.originalText),
    lexicalInput: CjLexicalSourceInput = (delegate as? CangJiePsiBuilder)?.lexicalInput
        ?: CjLexicalSourceInput(sourceContext.snapshot),
) :
    PsiBuilderAdapter(
        delegate,
    ),
    SemanticWhitespaceAwarePsiBuilder {

    /** 文件、截断视图与子 parser 的 mark 均进入这一个根事务。 */
    override val macroParsingJournal = MacroParsingJournal().also {
        it.initializeSource(sourceContext, delegate.originalText, (delegate as? CangJiePsiBuilder)?.offsetOrigin ?: 0, lexicalInput,
            verifyInput = { lexicalInput.requireBuilderInput(delegate, sourceContext, atEntry = false) })
        lexicalInput.requireBuilderInput(delegate, sourceContext, atEntry = true)
    }

    override fun mark(): PsiBuilder.Marker {
        // SDK 的首 marker 必须保留完整输入起点；后续空白边界由平台统一平衡。
        val marker = super.mark()
        return JournalMarker(
            marker,
            macroParsingJournal.checkpoint,
            marker.startOffset,
            lastConsumedTokenEndOffset,
        )
    }

    /** 位置错误使用本 builder 的真实逻辑 lookahead；显式原生位置由 range/point 报告入口保存。 */
    override fun error(message: String) {
        getTokenType()
        val cursor = rawTokenIndex()
        val carrier = super.mark()
        check(rawTokenIndex() == cursor) { "Creating a diagnostic carrier cannot advance the logical parser cursor." }
        val type = CjSyntaxErrorElementType(message, macroParsingJournal.nextDiagnosticOrder())
        carrier.done(type)
        macroParsingJournal.recordSyntaxError(type)
    }

    private var lastConsumedOffset: Int? = null

    override val lastConsumedTokenEndOffset: Int?
        get() = lastConsumedOffset

    /** SDK marker 可以归池复用；包装只保存当前生产事务的 checkpoint。 */
    private inner class JournalMarker(
        private val marker: PsiBuilder.Marker,
        private val checkpoint: Int,
        private val start: Int,
        private val lastConsumedTokenEndOffsetAtStart: Int?,
    ) : PsiBuilder.Marker by marker {
        private val journal: MacroParsingJournal get() = macroParsingJournal

        override fun precede(): PsiBuilder.Marker =
            JournalMarker(marker.precede(), checkpoint, start, lastConsumedTokenEndOffsetAtStart)

        override fun rollbackTo() {
            marker.rollbackTo()
            journal.rollback(checkpoint)
            lastConsumedOffset = lastConsumedTokenEndOffsetAtStart
        }

        /** done 的动态错误类型与 SDK error/errorBefore 共用实际生产计数。 */
        override fun done(type: IElementType) {
            marker.done(type)
            journal.recordSyntaxError(type)
        }

        override fun collapse(type: IElementType) {
            val end = rawCurrentOffset()
            marker.collapse(type)
            journal.discardCollapsedChildren(start, end)
            journal.recordSyntaxError(type)
        }

        override fun error(message: String) =
            done(CjSyntaxErrorElementType(message, journal.nextDiagnosticOrder()))

        override fun doneBefore(type: IElementType, before: PsiBuilder.Marker) =
            completeBefore(type, unwrap(before))

        override fun doneBefore(type: IElementType, before: PsiBuilder.Marker, errorMessage: String) =
            completeBeforeWithError(type, unwrap(before), errorMessage)

        override fun errorBefore(message: String, before: PsiBuilder.Marker) =
            completeBefore(CjSyntaxErrorElementType(message, journal.nextDiagnosticOrder()), unwrap(before))

        override fun doneBefore(type: IElementType, before: SyntaxTreeBuilder.Marker) =
            completeBefore(type, unwrap(before))

        override fun doneBefore(type: IElementType, before: SyntaxTreeBuilder.Marker, errorMessage: String) =
            completeBeforeWithError(type, unwrap(before), errorMessage)

        override fun errorBefore(message: String, before: SyntaxTreeBuilder.Marker) =
            completeBefore(CjSyntaxErrorElementType(message, journal.nextDiagnosticOrder()), unwrap(before))

        /** 对位 SDK 独立 ErrorItem：drop 普通父 marker 时该零宽报告仍作为子 production 保留。 */
        private fun completeBeforeWithError(type: IElementType, before: PsiBuilder.Marker, message: String) {
            val error = before.precede()
            val errorType = CjSyntaxErrorElementType(message, journal.nextDiagnosticOrder())
            error.doneBefore(errorType, before)
            journal.recordSyntaxError(errorType)
            completeBefore(type, before)
        }

        private fun completeBefore(type: IElementType, before: PsiBuilder.Marker) {
            marker.doneBefore(type, before)
            journal.recordSyntaxError(type)
        }

        private fun unwrap(before: SyntaxTreeBuilder.Marker): PsiBuilder.Marker {
            check(before is JournalMarker && before.journal === journal) {
                "Before markers must belong to the same parsing transaction."
            }
            return before.marker
        }

        // Production 的默认 getter 不携带实际 token 范围，必须完整委托 SDK marker。
        override fun getTokenType(): IElementType = marker.tokenType.actualAstElementType()
        override fun getStartOffset(): Int = marker.startOffset
        override fun getEndOffset(): Int = marker.endOffset
        override fun getStartIndex(): Int = marker.startIndex
        override fun getEndIndex(): Int = marker.endIndex
        override fun getErrorMessage(): String? = when (val kind = marker.tokenType) {
            is CjParsingErrorElementType -> kind.message
            else -> marker.errorMessage
        }
        override fun isCollapsed(): Boolean = marker.isCollapsed
    }

    /**
     * 保存 `joinComplexTokens` 的内部状态，供仓颉语法解析实现维护节点缓存或解析上下文。
     */
    private val joinComplexTokens = Stack<Boolean>()
    /**
     * 保存 `newlinesEnabled` 的内部状态，供仓颉语法解析实现维护节点缓存或解析上下文。
     */
    private val newlinesEnabled = Stack<Boolean>()
    /** 官方 skipNL 是词元游标模式，独立于语句边界的 newlinesEnabled 状态。 */
    private val nlTokenSkipping = Stack<Boolean>()
    /**
     * 保存 `delegateImpl` 的内部状态，供仓颉语法解析实现维护节点缓存或解析上下文。
     */
    private val delegateImpl: PsiBuilderImpl?

    init {
        newlinesEnabled.push(true)
        nlTokenSkipping.push(true)
        joinComplexTokens.push(true)
        delegateImpl =
            findPsiBuilderImpl(delegate)
    }

    /**
     * 实现 `isWhitespaceOrComment` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun isWhitespaceOrComment(elementType: IElementType): Boolean {
        assert(delegateImpl != null) { "PsiBuilderImpl not found" }
        return elementType === CjTokens.NL || delegateImpl!!.whitespaceOrComment(elementType)
    }

    /**
     * 实现 `newlineBeforeCurrentToken` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun newlineBeforeCurrentToken(): Boolean {
        if (!newlinesEnabled.peek()) return false

        if (eof()) return true

        for (i in 1..currentOffset) {
            val previousToken = rawLookup(-i)
            if (previousToken === CjTokens.NL) return true
            if (previousToken === CjTokens.BLOCK_COMMENT || previousToken === CjTokens.DOC_COMMENT || previousToken === CjTokens.EOL_COMMENT || previousToken === CjTokens.SHEBANG_COMMENT) {
                continue
            }
            if (previousToken !== TokenType.WHITE_SPACE) {
                break
            }
            val previousTokenStart = rawTokenTypeStart(-i)
            val previousTokenEnd = rawTokenTypeStart(-i + 1)
            assert(previousTokenStart >= 0)
            assert(previousTokenEnd < originalText.length)
            for (j in previousTokenStart until previousTokenEnd) {
                if (originalText[j] == '\n') {
                    return true
                }
            }
        }
        return false
    }

    /**
     * 实现 `disableNewlines` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun disableNewlines() {
        newlinesEnabled.push(false)
    }

    /**
     * 实现 `enableNewlines` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun enableNewlines() {
        newlinesEnabled.push(true)
    }

    /**
     * 实现 `restoreNewlinesState` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun restoreNewlinesState() {
        assert(newlinesEnabled.size > 1)
        newlinesEnabled.pop()
    }

    override val isNLTokenSkippingEnabled: Boolean
        get() = nlTokenSkipping.peek()

    override fun <T> withNLTokenSkipping(enabled: Boolean, block: () -> T): T {
        nlTokenSkipping.push(enabled)
        return try {
            block()
        } finally {
            check(nlTokenSkipping.size > 1)
            nlTokenSkipping.pop()
        }
    }

    /**
     * 执行 `joinComplexTokens` 内部辅助逻辑，支撑仓颉语法解析节点的结构解析与访问。
     */
    private fun joinComplexTokens(): Boolean {
        return joinComplexTokens.peek()
    }

    /**
     * 实现 `restoreJoiningComplexTokensState` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun restoreJoiningComplexTokensState() {
        joinComplexTokens.pop()
    }

    /**
     * 实现 `enableJoiningComplexTokens` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun enableJoiningComplexTokens() {
        joinComplexTokens.push(true)
    }

    /**
     * 实现 `disableJoiningComplexTokens` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun disableJoiningComplexTokens() {
        joinComplexTokens.push(false)
    }

    /**
     * 实现 `getTokenType` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getTokenType(): IElementType? {
        normalizeLogicalCursor()
        val tokenType = super.getTokenType()
        return if (!joinComplexTokens()) tokenType else getJoinedTokenType(tokenType, 1)
    }

    override fun getTokenText(): String? {
        normalizeLogicalCursor()
        return super.getTokenText()
    }

    override fun getCurrentOffset(): Int {
        normalizeLogicalCursor()
        return super.getCurrentOffset()
    }

    override fun eof(): Boolean {
        normalizeLogicalCursor()
        return super.eof()
    }

    /**
     * 仅将原生 skipNL 真正忽略的换行交给 SDK 的空白绑定协议。
     * 平台 remap 事务随 marker rollback 恢复 NL，opaque scanner 的显式换行保持原词元。
     */
    private fun normalizeLogicalCursor() {
        if (!isNLTokenSkippingEnabled) return
        while (super.getTokenType() === CjTokens.NL) {
            super<PsiBuilderAdapter>.remapCurrentTokenAndRestoreOnRollback(TokenType.WHITE_SPACE)
            super<PsiBuilderAdapter>.rawAdvanceLexer(1)
        }
    }

    /** Marker 收束只能读取物理 SDK 游标，不能因观察逻辑游标而把后续 NL 纳入节点。 */
    private fun rawCurrentOffset(): Int = super.getCurrentOffset()

    /**
     * 执行 `getJoinedTokenType` 内部辅助逻辑，支撑仓颉语法解析节点的结构解析与访问。
     */
    private fun getJoinedTokenType(rawTokenType: IElementType?, rawLookupSteps: Int): IElementType? {
//        if (rawTokenType === CjTokens.QUEST) {
//            val nextRawToken = rawLookup(rawLookupSteps)
//
//        } else if (rawTokenType === CjTokens.EXCL) {
//            val nextRawToken = rawLookup(rawLookupSteps)
//
//        }
        return rawTokenType
    }

    /**
     * 实现 `advanceLexer` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun advanceLexer() {
        normalizeLogicalCursor()
        recordCurrentTokenEndOffset()
        if (!joinComplexTokens()) {
            super.advanceLexer()
            return
        }
        super.advanceLexer()
    }

    private fun recordCurrentTokenEndOffset() {
        val type = super.getTokenType() ?: return
        val tokenText = super.getTokenText() ?: return
        val start = super.getCurrentOffset()
        val end = start + tokenText.length
        macroParsingJournal.captureSourceToken(start, end, type)
        lastConsumedOffset = end
    }

    /**
     * 实现 `getTokenText` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    /**
     * 实现 `lookAhead` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun lookAhead(steps: Int): IElementType? {
        normalizeLogicalCursor()
        val rawIndex = logicalLookAheadRawIndex(steps)
        val tokenType = rawLookup(rawIndex)
        return if (!joinComplexTokens()) tokenType else getJoinedTokenType(tokenType, 2)
    }

    override fun lookAheadTokenText(steps: Int): String? {
        normalizeLogicalCursor()
        val rawIndex = logicalLookAheadRawIndex(steps)
        if (rawLookup(rawIndex) == null) return null
        return originalText.substring(rawTokenTypeStart(rawIndex), rawTokenTypeStart(rawIndex + 1))
    }

    private fun logicalLookAheadRawIndex(steps: Int): Int {
        require(steps >= 0)
        var rawIndex = 0
        var remaining = steps
        while (true) {
            var type = rawLookup(rawIndex)
            while (type != null && shouldSkipForLogicalCursor(type)) {
                rawIndex++
                type = rawLookup(rawIndex)
            }
            if (remaining == 0 || type == null) return rawIndex
            rawIndex++
            remaining--
        }
    }

    private fun shouldSkipForLogicalCursor(elementType: IElementType): Boolean =
        elementType === CjTokens.NL && isNLTokenSkippingEnabled ||
            delegateImpl?.whitespaceOrComment(elementType) == true

    companion object {
        private fun findPsiBuilderImpl(builder: PsiBuilder): PsiBuilderImpl? {
            var builder: PsiBuilder? = builder
            while (true) {
                if (builder is PsiBuilderImpl) {
                    return builder
                }
                if (builder !is PsiBuilderAdapter) {
                    return null
                }
                builder = builder.delegate
            }
        }
    }
}

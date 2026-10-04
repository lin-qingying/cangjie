package org.cangnova.cangjie.ide.completion.impl.shared.insert

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.impl.DocumentImpl
import org.cangnova.cangjie.ide.completion.api.CaCompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CaCompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CaCompletionIdentityKind
import org.cangnova.cangjie.ide.completion.api.lookup.CaCompletionOrigin
import org.cangnova.cangjie.ide.completion.api.lookup.CaInsertionValidity
import org.cangnova.cangjie.ide.completion.impl.shared.caCompletionSnapshot
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 插入前校验回归。
 *
 * 校验的语义是「失效候选不得写文档」：文档版本变化必须拒绝写入，
 * 区间越界必须拒绝写入，而关键字等与周围文本无关的候选不受版本戳约束。
 */
class CaInsertionValidatorTest : CjParsingTestCase(
    dataPath = "",
    fileExt = "cj",
    fileType = CangJieFileType.INSTANCE,
    CangJieParserDefinition(),
) {
    /**
     * 初始化平台测试环境与待校验文档。
     */
    @BeforeEach
    fun setUpFixture() {
        setUp()
        // 轻量夹具不注册 EditorFactory（getInstance() 为 null）；
        // 校验器只依赖 Document 接口，用平台文档实现即可。
        document = DocumentImpl("alpha beta")
    }

    /**
     * 释放平台测试环境。
     */
    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

    /**
     * 版本戳一致且区间合法：允许写入。
     */
    @Test
    fun validWhenStampMatches() {
        val stamp = document.modificationStamp
        assertEquals(
            CaInsertionValidity.Valid,
            CaInsertionValidator.validate(symbolSnapshot(stamp), 0, 5, document, stamp),
        )
    }

    /**
     * 文档在分析后被改动：必须报告过期而不是硬插。
     */
    @Test
    fun staleWhenDocumentChanged() {
        val expected = document.modificationStamp
        // 文档修改要求 EDT + 命令上下文：工作线程必须切到 EDT，
        // 并用 WriteCommandAction 包裹，平台不允许裸写文档。
        java.awt.EventQueue.invokeAndWait {
            WriteCommandAction.runWriteCommandAction(project) {
                document.insertString(document.textLength, "!")
            }
        }
        val actual = document.modificationStamp
        assertEquals(
            CaInsertionValidity.StaleDocument(expected, actual),
            CaInsertionValidator.validate(symbolSnapshot(expected), 0, 5, document, actual),
        )
    }

    /**
     * 替换区间越界：必须拒绝写入，而不是抛 IndexOutOfBounds 升级成 IDE 故障。
     */
    @Test
    fun outOfRangeRejected() {
        val stamp = document.modificationStamp
        assertEquals(
            CaInsertionValidity.OutOfRange(0, 999, document.textLength),
            CaInsertionValidator.validate(symbolSnapshot(stamp), 0, 999, document, stamp),
        )
    }

    /**
     * 关键字候选不受版本戳约束：它只写入自身文本，与周围文本无关。
     */
    @Test
    fun keywordExemptFromStamp() {
        assertEquals(
            CaInsertionValidity.Valid,
            CaInsertionValidator.validate(keywordSnapshot(), 0, 5, document, document.modificationStamp + 1),
        )
    }

    /**
     * 关键字仍要过区间校验：豁免的是版本，不是越界。
     */
    @Test
    fun keywordStillRangeChecked() {
        assertEquals(
            CaInsertionValidity.OutOfRange(0, 999, document.textLength),
            CaInsertionValidator.validate(keywordSnapshot(), 0, 999, document, 0),
        )
    }

    /**
     * 待校验文档。
     */
    private lateinit var document: Document

    /**
     * 构造符号候选：身份非关键字，插入依赖文档版本。
     */
    private fun symbolSnapshot(stamp: Long): CaCompletionSnapshot = caCompletionSnapshot(
        lookupString = "alpha",
        kind = CaCompletionItemKind.LOCAL,
        origin = CaCompletionOrigin.LOCAL_SCOPE,
        documentStamp = stamp,
    ).build()

    /**
     * 构造关键字候选：身份为 KEYWORD，插入与周围文本无关。
     */
    private fun keywordSnapshot(): CaCompletionSnapshot = caCompletionSnapshot(
        lookupString = "return",
        kind = CaCompletionItemKind.KEYWORD,
        origin = CaCompletionOrigin.KEYWORD,
        identityKind = CaCompletionIdentityKind.KEYWORD,
    ).build()
}

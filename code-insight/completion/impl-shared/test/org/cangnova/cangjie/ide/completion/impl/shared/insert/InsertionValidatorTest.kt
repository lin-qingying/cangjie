package org.cangnova.cangjie.ide.completion.impl.shared.insert

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.impl.DocumentImpl
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionIdentityKind
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionOrigin
import org.cangnova.cangjie.ide.completion.api.lookup.InsertionValidity
import org.cangnova.cangjie.ide.completion.impl.shared.completionSnapshot
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
class InsertionValidatorTest : CjParsingTestCase(
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
            InsertionValidity.Valid,
            InsertionValidator.validate(symbolSnapshot(stamp), 0, 5, document, stamp),
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
            InsertionValidity.StaleDocument(expected, actual),
            InsertionValidator.validate(symbolSnapshot(expected), 0, 5, document, actual),
        )
    }

    /**
     * 替换区间越界：必须拒绝写入，而不是抛 IndexOutOfBounds 升级成 IDE 故障。
     */
    @Test
    fun outOfRangeRejected() {
        val stamp = document.modificationStamp
        assertEquals(
            InsertionValidity.OutOfRange(0, 999, document.textLength),
            InsertionValidator.validate(symbolSnapshot(stamp), 0, 999, document, stamp),
        )
    }

    /**
     * 关键字候选不受版本戳约束：它只写入自身文本，与周围文本无关。
     */
    @Test
    fun keywordExemptFromStamp() {
        assertEquals(
            InsertionValidity.Valid,
            InsertionValidator.validate(keywordSnapshot(), 0, 5, document, document.modificationStamp + 1),
        )
    }

    /**
     * 关键字仍要过区间校验：豁免的是版本，不是越界。
     */
    @Test
    fun keywordStillRangeChecked() {
        assertEquals(
            InsertionValidity.OutOfRange(0, 999, document.textLength),
            InsertionValidator.validate(keywordSnapshot(), 0, 999, document, 0),
        )
    }

    /**
     * 待校验文档。
     */
    private lateinit var document: Document

    /**
     * 构造符号候选：身份非关键字，插入依赖文档版本。
     */
    private fun symbolSnapshot(stamp: Long): CompletionSnapshot = completionSnapshot(
        lookupString = "alpha",
        kind = CompletionItemKind.LOCAL,
        origin = CompletionOrigin.LOCAL_SCOPE,
        documentStamp = stamp,
    ).build()

    /**
     * 构造关键字候选：身份为 KEYWORD，插入与周围文本无关。
     */
    private fun keywordSnapshot(): CompletionSnapshot = completionSnapshot(
        lookupString = "return",
        kind = CompletionItemKind.KEYWORD,
        origin = CompletionOrigin.KEYWORD,
        identityKind = CompletionIdentityKind.KEYWORD,
    ).build()
}

package org.cangnova.cangjie.ide.completion.impl.shared.context

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjDotQualifiedExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * 补全位置分类回归。
 *
 * 只验证位置形态判定：分类错误会让候选生成选错家族，
 * 而且这种错误在界面上表现为「少补了一些东西」，很难定位。
 */
class CaCompletionPositionContextTest : CjParsingTestCase(
    dataPath = "",
    fileExt = "cj",
    fileType = CangJieFileType.INSTANCE,
    CangJieParserDefinition(),
) {
    /**
     * 初始化 PSI 解析测试环境。
     */
    @BeforeEach
    fun setUpFixture() {
        setUp()
    }

    /**
     * 释放 PSI 解析测试环境。
     */
    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

    /**
     * 解析源码为仓颉文件。
     */
    private fun parse(text: String): CjFile = createPsiFile("completionContext", text) as CjFile

    /**
     * `receiver.` 之后属于成员访问，接收者一侧仍是普通表达式位置。
     */
    @Test
    fun classifiesMemberAccess() {
        val file = parse(
            """
            class Box {
                func unwrap(): Int64 {
                    1
                }
            }

            func use(box: Box): Int64 {
                box.unwrap()
            }
            """.trimIndent(),
        )

        val qualified = PsiTreeUtil.findChildrenOfType(file, CjDotQualifiedExpression::class.java).single()
        val selector = qualified.selectorExpression
        assertNotNull(selector, "限定表达式的选择器必须存在")

        val onSelector = CaCompletionPositionContexts.classify(selector)
        assertEquals(CaCompletionPositionKind.MEMBER_ACCESS, onSelector.kind)
        assertEquals(qualified.receiverExpression, onSelector.receiverExpression)

        // 接收者一侧仍是普通函数体位置：只有选择器一侧才构成成员访问。
        val onReceiver = CaCompletionPositionContexts.classify(qualified.receiverExpression)
        assertEquals(CaCompletionPositionKind.FUNCTION_BODY, onReceiver.kind)
        assertNull(onReceiver.receiverExpression, "接收者一侧不是成员访问位置")
    }

    /**
     * 函数体内部归为函数体位置。
     */
    @Test
    fun classifiesFunctionBody() {
        val file = parse(
            """
            func compute(): Int64 {
                let value = 1
                value
            }
            """.trimIndent(),
        )

        val usage = file.lastReference("value")
        val context = CaCompletionPositionContexts.classify(usage)
        assertEquals(CaCompletionPositionKind.FUNCTION_BODY, context.kind)
        assertNull(context.receiverExpression, "函数体位置不是成员访问")
    }

    /**
     * import 语句内部归为 import 位置。
     */
    @Test
    fun classifiesImport() {
        val file = parse(
            """
            package sample.imports

            import std.collection.ArrayList
            """.trimIndent(),
        )

        val context = CaCompletionPositionContexts.classify(file.lastReference("ArrayList"))
        assertEquals(CaCompletionPositionKind.IMPORT, context.kind)
    }
}

/**
 * 取得最后一个引用指定名字的表达式。
 */
private fun CjFile.lastReference(name: String): CjSimpleNameExpression =
    PsiTreeUtil.findChildrenOfType(this, CjSimpleNameExpression::class.java).last { it.referencedName == name }

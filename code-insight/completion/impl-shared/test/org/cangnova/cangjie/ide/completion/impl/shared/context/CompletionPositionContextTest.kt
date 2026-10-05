package org.cangnova.cangjie.ide.completion.impl.shared.context

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjDotQualifiedExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjPsiFactory
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
class CompletionPositionContextTest : CjParsingTestCase(
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

        val onSelector = CompletionPositionContexts.classify(selector)
        assertEquals(CompletionPositionKind.MEMBER_ACCESS, onSelector.kind)
        assertEquals(qualified.receiverExpression, onSelector.receiverExpression)

        // 接收者一侧仍是普通函数体位置：只有选择器一侧才构成成员访问。
        val onReceiver = CompletionPositionContexts.classify(qualified.receiverExpression)
        assertEquals(CompletionPositionKind.FUNCTION_BODY, onReceiver.kind)
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
        val context = CompletionPositionContexts.classify(usage)
        assertEquals(CompletionPositionKind.FUNCTION_BODY, context.kind)
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

        val context = CompletionPositionContexts.classify(file.lastReference("ArrayList"))
        assertEquals(CompletionPositionKind.IMPORT, context.kind)
    }

    /**
     * 表达式片段内部归为片段表达式位置，且不给出所属声明。
     */
    @Test
    fun classifiesExpressionCodeFragment() {
        val host = parse(
            """
            package sample.fragments

            func host(): Int64 {
                1
            }
            """.trimIndent(),
        )
        val fragment = CjPsiFactory.contextual(host).createExpressionCodeFragment("host()", host)
        val target = PsiTreeUtil.findChildrenOfType(fragment, CjSimpleNameExpression::class.java)
            .last { it.referencedName == "host" }

        val context = CompletionPositionContexts.classify(target)
        assertEquals(CompletionPositionKind.CODE_FRAGMENT_EXPRESSION, context.kind)
        assertNull(context.declaration, "片段表达式没有所属声明")
        assertNull(context.receiverExpression, "片段表达式不是成员访问位置")
    }

    /**
     * 块片段内部归为片段块位置。
     *
     * 片段里带 `let` 时必须仍然是片段形态——这正是 3.9 的验收点：
     * 片段内部也有块，若按普通块位置处理，片段就退化成函数体位置，
     * 丢掉「作用域只来自片段自身上下文」这一事实。
     */
    @Test
    fun classifiesBlockCodeFragment() {
        val host = parse(
            """
            package sample.fragments

            func host(): Int64 {
                1
            }
            """.trimIndent(),
        )
        val fragment = CjPsiFactory.contextual(host)
            .createBlockCodeFragment("let local = 1\nhost()", host)
        val target = PsiTreeUtil.findChildrenOfType(fragment, CjSimpleNameExpression::class.java)
            .last { it.referencedName == "host" }

        val context = CompletionPositionContexts.classify(target)
        assertEquals(CompletionPositionKind.CODE_FRAGMENT_BLOCK, context.kind)
        assertNull(context.receiverExpression, "片段块不是成员访问位置")
    }

    /**
     * 反例：普通文件里的块位置不得被误判成片段位置。
     */
    @Test
    fun doesNotClassifyOrdinaryFileAsFragment() {
        val file = parse(
            """
            package sample.fragments

            func ordinary(): Int64 {
                let local = 1
                local
            }
            """.trimIndent(),
        )

        val context = CompletionPositionContexts.classify(file.lastReference("local"))
        assertEquals(CompletionPositionKind.FUNCTION_BODY, context.kind)
    }
}

/**
 * 取得最后一个引用指定名字的表达式。
 */
private fun CjFile.lastReference(name: String): CjSimpleNameExpression =
    PsiTreeUtil.findChildrenOfType(this, CjSimpleNameExpression::class.java).last { it.referencedName == name }

package org.cangnova.cangjie.lexer.cdoc

import com.intellij.lang.documentation.ClientDocumentationSettings
import com.intellij.lang.documentation.DocumentationSettings
import com.intellij.mock.MockApplication
import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.impl.DefaultColorsScheme
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.cdoc.psi.CDoc
import org.cangnova.cangjie.lexer.cdoc.psi.api.CDocCommentDescriptor
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocCommentDescriptorImpl
import org.cangnova.cangjie.lexer.cdoc.render.CDocRenderer
import org.cangnova.cangjie.lexer.cdoc.render.CDocRenderer.renderCDoc
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjImplementationDetail
import org.cangnova.cangjie.psi.CjNonPublicApi
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * CDoc 渲染层回归：结构化 CDoc → Quick Doc HTML。
 *
 * 锁定两件事：
 * 1. 分区顺序与 KDoc 一致（参数 → 返回值 → 参见），与源码书写顺序无关；
 * 2. 正文走 Markdown 转 HTML，`[Link]` 变成 `psi_element://` 超链接。
 *
 * 环境说明：`CjPlatformLiteFixture` 只启动 MinimalApplication，`ClientDocumentationSettings`
 * 与 `EditorColorsManager` 由 [installDocumentationServices] 注册测试替身后才能测到
 * 行内代码与链接着色。围栏代码块仍不可测——它要求语言已注册 `lang.syntaxHighlighter`
 * 扩展，而 lite 夹具不加载插件 XML。
 */
@OptIn(CjNonPublicApi::class, CjImplementationDetail::class)
class CDocRendererTest : CjParsingTestCase(
    dataPath = "",
    fileExt = "cj",
    fileType = CangJieFileType.INSTANCE,
    CangJieParserDefinition(),
) {
    @BeforeEach
    fun setUpFixture() {
        setUp()
        installDocumentationServices()
    }

    /**
     * 注册 Quick Doc 渲染依赖的平台服务。
     *
     * `CjPlatformLiteFixture` 的 MockApplication 只注册核心服务：
     * - 缺 `ClientDocumentationSettings` → `appendStyledInlineCode` / `appendStyledCodeBlock` NPE；
     * - 缺 `EditorColorsManager` → 链接高亮（`getTargetLinkElementAttributes`）NPE。
     * 这里补最小实现，让配色分支进入回归。
     */
    private fun installDocumentationServices() {
        val application = ApplicationManager.getApplication() as? MockApplication ?: return
        if (application.getService(ClientDocumentationSettings::class.java) == null) {
            application.registerService(ClientDocumentationSettings::class.java, TestDocumentationSettings)
        }
        if (application.getService(EditorColorsManager::class.java) == null) {
            application.registerService(EditorColorsManager::class.java, TestEditorColorsManager())
        }
    }

    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

    /**
     * 分区顺序与 KDoc 一致，与源码书写顺序无关。
     *
     * 覆盖不含链接主语的分区（`@return` / `@since`）。`@param`、`@see`、`@throws` 走
     * `DocumentationManagerUtil` + `EditorColorsManager` 的链接高亮路径，需要 IDE 运行时的
     * 编辑器配色服务，本轻量 PSI 测试环境不提供，故不在断言范围内。
     */
    @Test
    fun sectionsAreOrderedLikeKDoc() {
        val html = renderHtmlFor(
            """
            /**
             * Greets the caller.
             * @since 1.0
             * @return rendered text
             */
            func describe(value: Int64): String {
                return "document"
            }
            """.trimIndent(),
        )

        assertTrue(html.contains("Returns:"), "应渲染返回值分区: $html")
        assertTrue(html.contains("rendered text"), "返回值分区应包含内容: $html")
        assertTrue(html.contains("Since:"), "应渲染起始版本分区: $html")
        assertTrue(html.indexOf("Returns:") < html.indexOf("Since:"), "返回值分区应排在起始版本之前: $html")
        assertTrue(html.contains("<div class='content'>"), "正文应包在 CONTENT 容器里: $html")
    }

    /**
     * 正文 Markdown 由 JetBrains 官方解析器转 HTML（与 KDocRenderer 同一套遍历规则）。
     *
     * 这里只覆盖不依赖 IDE 客户端设置的行内样式：行内代码与围栏代码块走高亮渲染，
     * 需要 `ClientDocumentationSettings`（IDE 运行时才存在），故不在本轻量测试环境中断言。
     */
    @Test
    fun bodyMarkdownIsConvertedToHtml() {
        val html = renderHtmlFor(
            """
            /**
             * Uses **bold** and *italic* here.
             */
            func markdown(): Unit {
            }
            """.trimIndent(),
        )

        assertTrue(html.contains("<strong>bold</strong>"), "强调应转为 <strong>: $html")
        assertTrue(html.contains("<em>italic</em>"), "斜体应转为 <em>: $html")
        assertTrue(html.startsWith("<div class='content'>"), "正文应包在 CONTENT 容器里: $html")
    }

    /**
     * `@param` 与 `@see` 走链接高亮路径：分区标题、行内代码主语与内容都要出现。
     */
    @Test
    fun sectionsWithLinkSubjectsAreRendered() {
        val html = renderHtmlFor(
            """
            class Document {
            }

            /**
             * Greets the caller.
             * @param [value] input value
             * @see [Document]
             */
            func describe(value: Int64): String {
                return "document"
            }
            """.trimIndent(),
        )

        assertTrue(html.contains("Parameters:"), "应渲染参数分区: $html")
        assertTrue(html.contains("- input value"), "参数分区应包含内容: $html")
        assertTrue(html.contains("See Also:"), "应渲染参见分区: $html")
        assertTrue(html.contains("Document"), "参见分区应包含链接目标: $html")
    }

    /**
     * 行内代码走高亮渲染：反引号被消费，内容保留。
     */
    @Test
    fun inlineCodeIsRenderedThroughHighlighter() {
        val html = renderHtmlFor(
            """
            /**
             * Uses `code` here.
             */
            func sample(): Unit {
            }
            """.trimIndent(),
        )

        assertTrue(!html.contains("`"), "行内代码的反引号应被消费: $html")
        assertTrue(html.contains("code"), "行内代码内容应保留: $html")
    }

    /**
     * 没有任何 CDoc 内容时返回 `null`。
     */
    @Test
    fun undocumentedDeclarationRendersNothing() {
        val descriptor = descriptorFor(
            """
            func plain(): Unit {
            }
            """.trimIndent(),
        )

        assertEquals(null, descriptor)
    }

    private fun renderHtmlFor(text: String): String {
        val descriptor = checkNotNull(descriptorFor(text)) { "期望存在可渲染的 CDoc" }
        return with(CDocRenderer) {
            buildString { renderCDoc(descriptor.primaryTag, descriptor.additionalSections) }
        }
    }

    private fun descriptorFor(text: String): CDocCommentDescriptor? {
        val file = createPsiFile("cdocRenderer", text) as CjFile
        val docComment = PsiTreeUtil.findChildrenOfType(file, CDoc::class.java).firstOrNull() ?: return null
        return CDocCommentDescriptorImpl(docComment.getDefaultSection(), docComment.getAllSections())
    }
}

/**
 * Quick Doc 渲染用的最小客户端文档设置：默认开启签名/代码块/链接高亮。
 */
private object TestDocumentationSettings : ClientDocumentationSettings {
    override fun isHighlightingOfQuickDocSignaturesEnabled(): Boolean = true

    override fun isHighlightingOfCodeBlocksEnabled(): Boolean = true

    override fun isSemanticHighlightingOfLinksEnabled(): Boolean = true

    override fun isCodeBackgroundEnabled(): Boolean = false

    override fun getInlineCodeHighlightingMode(): DocumentationSettings.InlineCodeHighlightingMode =
        DocumentationSettings.InlineCodeHighlightingMode.AS_DEFAULT_CODE
}

/**
 * 测试用配色管理：所有键都返回同一份空属性，避免依赖真实配色方案。
 */
private class TestEditorColorsManager : EditorColorsManager() {
    private val scheme: EditorColorsScheme = TestColorsScheme()

    override fun addColorScheme(scheme: EditorColorsScheme) = Unit

    override fun getAllSchemes(): Array<EditorColorsScheme> = arrayOf(scheme)

    override fun setGlobalScheme(scheme: EditorColorsScheme?) = Unit

    override fun setCurrentSchemeOnLafChange(scheme: EditorColorsScheme) = Unit

    override fun getGlobalScheme(): EditorColorsScheme = scheme

    override fun getActiveVisibleScheme(): EditorColorsScheme = scheme

    override fun getScheme(schemeName: String): EditorColorsScheme = scheme

    override fun isDefaultScheme(scheme: EditorColorsScheme): Boolean = true

    override fun isUseOnlyMonospacedFonts(): Boolean = false

    override fun setUseOnlyMonospacedFonts(value: Boolean) = Unit

    override fun getSchemeModificationCounter(): Long = 0
}

/**
 * 测试用配色方案：任意键都返回空属性，保证链接属性计算不会因缺省值缺失而 NPE。
 */
private class TestColorsScheme : DefaultColorsScheme() {
    override fun getAttributes(key: TextAttributesKey): TextAttributes = TextAttributes()

    override fun getColor(key: ColorKey?): java.awt.Color? = java.awt.Color.BLACK
}

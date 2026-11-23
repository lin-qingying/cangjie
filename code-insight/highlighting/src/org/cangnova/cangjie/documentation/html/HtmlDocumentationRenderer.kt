@file:OptIn(
    org.cangnova.cangjie.psi.CjNonPublicApi::class,
    org.cangnova.cangjie.psi.CjImplementationDetail::class,
)

package org.cangnova.cangjie.documentation.html

import com.intellij.lang.documentation.DocumentationMarkup.SECTION_END
import com.intellij.lang.documentation.DocumentationMarkup.SECTION_HEADER_START
import com.intellij.lang.documentation.DocumentationMarkup.SECTION_SEPARATOR
import com.intellij.lang.documentation.QuickDocHighlightingHelper.appendStyledCodeBlock
import com.intellij.lang.documentation.QuickDocHighlightingHelper.appendStyledFragment
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.documentation.api.AuthorsSection
import org.cangnova.cangjie.documentation.api.ConstructorSection
import org.cangnova.cangjie.documentation.api.DocumentationBody
import org.cangnova.cangjie.documentation.api.DocumentationContainer
import org.cangnova.cangjie.documentation.api.DocumentationContent
import org.cangnova.cangjie.documentation.api.DocumentationLink
import org.cangnova.cangjie.documentation.api.DocumentationRenderer
import org.cangnova.cangjie.documentation.api.DocumentationSection
import org.cangnova.cangjie.documentation.api.LinkEntry
import org.cangnova.cangjie.documentation.api.NamedEntry
import org.cangnova.cangjie.documentation.api.NamedEntrySection
import org.cangnova.cangjie.documentation.api.ParametersSection
import org.cangnova.cangjie.documentation.api.PropertiesSection
import org.cangnova.cangjie.documentation.api.ReturnsSection
import org.cangnova.cangjie.documentation.api.SampleEntry
import org.cangnova.cangjie.documentation.api.SamplesSection
import org.cangnova.cangjie.documentation.api.SeeAlsoSection
import org.cangnova.cangjie.documentation.api.SinceSection
import org.cangnova.cangjie.documentation.api.SuppressSection
import org.cangnova.cangjie.documentation.api.TextSection
import org.cangnova.cangjie.documentation.api.ThrowsSection
import org.cangnova.cangjie.documentation.api.TypeParametersSection
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.lexer.cdoc.CDocTemplate
import org.cangnova.cangjie.lexer.cdoc.insert
import org.cangnova.cangjie.lexer.cdoc.render.CDocRenderer
import org.cangnova.cangjie.psi.CjPsiBundle

/** 将格式无关的快速文档内容渲染为 IntelliJ Quick Doc HTML。 */
class HtmlDocumentationRenderer(
    private val project: Project,
    private val definitionRenderer: DefinitionRenderer = DefaultDefinitionRenderer,
    private val linkSupport: DocumentationLinkSupport? = null,
) : DocumentationRenderer<String> {
    override fun render(content: DocumentationContent): String = buildString {
        insert(CDocTemplate()) {
            definition {
                append(definitionRenderer.render(content))
                content.body?.enumOrdinal?.let { ordinal ->
                    append("<br>")
                    appendStyledFragment("// ", DefaultLanguageHighlighterColors.BLOCK_COMMENT)
                    appendStyledFragment(
                        CjPsiBundle.message("quick.doc.text.enum.ordinal", ordinal),
                        DefaultLanguageHighlighterColors.BLOCK_COMMENT,
                    )
                }
            }
            content.body
                ?.takeIf { it.description?.isNotBlank() == true || it.sections.isNotEmpty() }
                ?.let { body ->
                    description { append(renderBody(body)) }
                }
            content.container?.let { container ->
                containerInfo { appendContainer(container) }
            }
        }
    }

    /** 仅渲染正文与章节，供 `CDocRenderer.renderCDoc` 保持原有调用签名。 */
    fun renderBody(body: DocumentationBody): String = buildString {
        insert(CDocTemplate.DescriptionBodyTemplate.CangJie()) {
            content {
                body.description?.let { append(markdownToHtml(it, allowSingleParagraph = true)) }
            }
            sections {
                body.sections.forEach { section -> appendSection(section) }
            }
        }
    }

    private fun markdownToHtml(text: String, allowSingleParagraph: Boolean): String =
        CDocRenderer.markdownToHtml(text, project, allowSingleParagraph, linkSupport)

    private fun StringBuilder.appendSection(section: DocumentationSection) {
        when (section) {
            is NamedEntrySection -> appendNamedEntries(section)
            is TextSection -> appendTextSection(section)
            is AuthorsSection -> appendAuthors(section)
            is SeeAlsoSection -> appendSeeAlso(section.entries)
            is SamplesSection -> appendSamples(section.entries)
        }
    }

    private fun StringBuilder.appendNamedEntries(section: NamedEntrySection) {
        val title = when (section) {
            is ParametersSection -> CjPsiBundle.message("kdoc.section.title.parameters")
            is TypeParametersSection -> CjPsiBundle.message("kdoc.section.title.type.parameters")
            is PropertiesSection -> CjPsiBundle.message("kdoc.section.title.properties")
            is ThrowsSection -> CjPsiBundle.message("kdoc.section.title.throws")
        }
        appendSection(title) {
            section.entries.forEach { entry ->
                appendNamedEntry(entry)
            }
        }
    }

    private fun StringBuilder.appendTextSection(section: TextSection) {
        val title = when (section) {
            is ConstructorSection -> CjPsiBundle.message("kdoc.section.title.constructor")
            is ReturnsSection -> CjPsiBundle.message("kdoc.section.title.returns")
            is SinceSection -> CjPsiBundle.message("kdoc.section.title.since")
            is SuppressSection -> CjPsiBundle.message("kdoc.section.title.suppress")
        }
        appendSection(title) {
            append(markdownToHtml(section.markdown, allowSingleParagraph = false))
        }
    }

    private fun StringBuilder.appendAuthors(section: AuthorsSection) {
        appendSection(CjPsiBundle.message("kdoc.section.title.author")) {
            append(section.names.joinToString(", ") { name -> name.htmlEscape() })
        }
    }

    private fun StringBuilder.appendSeeAlso(entries: List<LinkEntry>) {
        appendSection(CjPsiBundle.message("kdoc.section.title.see.also")) {
            entries.forEachIndexed { index, entry ->
                appendLinkEntry(entry)
                if (index != entries.lastIndex) append(",<br>")
            }
        }
    }

    private fun StringBuilder.appendSamples(entries: List<SampleEntry>) {
        appendSection(CjPsiBundle.message("kdoc.section.title.samples")) {
            entries.forEach { entry ->
                appendSample(entry)
            }
        }
    }

    private fun StringBuilder.appendNamedEntry(entry: NamedEntry) {
        append("<p><code>")
        append(entry.link?.let { renderHyperlink(it, entry.name) } ?: entry.name.htmlEscape())
        append("</code>")
        if (entry.markdown.isNotBlank()) {
            append(" - ")
            append(markdownToHtml(entry.markdown, allowSingleParagraph = false))
        }
    }

    private fun StringBuilder.appendLinkEntry(entry: LinkEntry) {
        val link = entry.link
        if (link != null) {
            append(renderHyperlink(link, entry.label))
        } else if (entry.markdown.isNotBlank()) {
            append(markdownToHtml(entry.markdown, allowSingleParagraph = false))
        } else {
            append(entry.label.htmlEscape())
        }
    }

    private fun StringBuilder.appendSample(entry: SampleEntry) {
        append("<p>")
        if (entry.link == null) {
            append(entry.label.htmlEscape())
            return
        }

        append(renderHyperlink(entry.link, entry.label))
        val code = linkSupport?.sampleCode(entry.link) ?: return
        appendStyledCodeBlock(project, CangJieLanguage, CDocRenderer.trimCommonIndent(code).htmlEscape())
    }

    private fun StringBuilder.appendSection(title: String, body: StringBuilder.() -> Unit) {
        append(SECTION_HEADER_START, title, ":", SECTION_SEPARATOR)
        body()
        append(SECTION_END)
    }

    private fun StringBuilder.appendContainer(container: DocumentationContainer) {
        val icon = linkSupport?.containerIcon(container) ?: when (container) {
            is DocumentationContainer.Package -> "AllIcons.Nodes.Package"
            is DocumentationContainer.ClassLike -> "AllIcons.Nodes.Class"
        }
        append("<icon src=\"").append(icon.htmlEscape()).append("\"></icon>&nbsp;")
        val link = DocumentationLink(container.fqName, container.fqName)
        append(renderHyperlink(link, container.fqName))
        append("<br>")

        if (container is DocumentationContainer.Package && container.fileName != null) {
            val fileIcon = linkSupport?.fileIconSource() ?: "AllIcons.Nodes.File"
            append("<icon src=\"").append(fileIcon.htmlEscape()).append("\"></icon>&nbsp;")
            append(container.fileName.htmlEscape()).append("<br>")
        }
    }

    private fun renderHyperlink(link: DocumentationLink, label: String): String =
        linkSupport?.hyperlink(link, label) ?: label.htmlEscape()

    private fun String.htmlEscape(): String =
        replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}

/** 定义头由共享内容提供；renderer 负责放入正确的 HTML 上下文。 */
fun interface DefinitionRenderer {
    fun render(content: DocumentationContent): String
}

/** 默认定义头原样转义，不携带 PSI 或平台渲染策略。 */
object DefaultDefinitionRenderer : DefinitionRenderer {
    override fun render(content: DocumentationContent): String = content.definition
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}

/** 超链接、示例源码与宿主专属容器图标由平台宿主解析。 */
interface DocumentationLinkSupport {
    fun hyperlink(link: DocumentationLink, label: String): String?

    fun sampleCode(link: DocumentationLink): String?

    fun containerIcon(container: DocumentationContainer): String? = null

    fun fileIconSource(): String? = null
}

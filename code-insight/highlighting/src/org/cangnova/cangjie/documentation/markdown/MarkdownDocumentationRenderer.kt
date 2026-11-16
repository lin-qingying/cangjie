package org.cangnova.cangjie.documentation.markdown

import org.cangnova.cangjie.documentation.api.AuthorsSection
import org.cangnova.cangjie.documentation.api.ConstructorSection
import org.cangnova.cangjie.documentation.api.DocumentationBody
import org.cangnova.cangjie.documentation.api.DocumentationContent
import org.cangnova.cangjie.documentation.api.DocumentationRenderer
import org.cangnova.cangjie.documentation.api.DocumentationSection
import org.cangnova.cangjie.documentation.api.LinkEntry
import org.cangnova.cangjie.documentation.api.NamedEntry
import org.cangnova.cangjie.documentation.api.NamedEntrySection
import org.cangnova.cangjie.documentation.api.ParametersSection
import org.cangnova.cangjie.documentation.api.PropertiesSection
import org.cangnova.cangjie.documentation.api.ReturnsSection
import org.cangnova.cangjie.documentation.api.SamplesSection
import org.cangnova.cangjie.documentation.api.SeeAlsoSection
import org.cangnova.cangjie.documentation.api.SinceSection
import org.cangnova.cangjie.documentation.api.SuppressSection
import org.cangnova.cangjie.documentation.api.TextSection
import org.cangnova.cangjie.documentation.api.ThrowsSection
import org.cangnova.cangjie.documentation.api.TypeParametersSection
import org.cangnova.cangjie.psi.CjPsiBundle

/** 为 Markdown 章节标题提供可替换的本地化入口，便于测试稳定断言。 */
fun interface SectionTitles {
    fun title(section: DocumentationSection): String
}

/** 将共享文档内容渲染为适用于 LSP Hover 的 Markdown。 */
class MarkdownDocumentationRenderer(
    private val titles: SectionTitles = BundleSectionTitles,
) : DocumentationRenderer<String> {
    override fun render(content: DocumentationContent): String = buildString {
        append("```cangjie\n")
        append(content.definition)
        content.body?.enumOrdinal?.let { ordinal ->
            append("\n// ")
            append(CjPsiBundle.message("quick.doc.text.enum.ordinal", ordinal))
        }
        append("\n```")
        content.body?.let { body ->
            val renderedBody = renderBody(body)
            if (renderedBody.isNotBlank()) {
                append("\n\n")
                append(renderedBody)
            }
        }
    }

    /** 渲染 CDoc 摘要和章节，不添加声明定义头的代码围栏。 */
    fun renderBody(body: DocumentationBody): String = buildString {
        body.description?.takeIf(String::isNotBlank)?.let(::append)
        body.sections.forEach { section ->
            if (isNotEmpty()) append("\n\n")
            append("## ").append(titles.title(section))
            val renderedSection = renderSection(section)
            if (renderedSection.isNotBlank()) {
                append("\n").append(renderedSection)
            }
        }
    }

    private fun renderSection(section: DocumentationSection): String = when (section) {
        is NamedEntrySection -> section.entries.joinToString("\n") { entry -> entry.toMarkdown() }
        is TextSection -> section.markdown
        is AuthorsSection -> section.names.joinToString(", ")
        is SeeAlsoSection -> section.entries.joinToString("\n") { entry -> entry.toMarkdown() }
        is SamplesSection -> section.entries.joinToString("\n") { entry -> "- ${entry.label}" }
    }

    private fun NamedEntry.toMarkdown(): String =
        if (markdown.isBlank()) "- `$name`" else "- `$name` — $markdown"

    private fun LinkEntry.toMarkdown(): String =
        if (markdown.isBlank()) "- $label" else "- $label — $markdown"

    private object BundleSectionTitles : SectionTitles {
        override fun title(section: DocumentationSection): String = when (section) {
            is NamedEntrySection -> when (section) {
                is ParametersSection -> CjPsiBundle.message("kdoc.section.title.parameters")
                is TypeParametersSection -> CjPsiBundle.message("kdoc.section.title.type.parameters")
                is PropertiesSection -> CjPsiBundle.message("kdoc.section.title.properties")
                is ThrowsSection -> CjPsiBundle.message("kdoc.section.title.throws")
            }

            is TextSection -> when (section) {
                is ConstructorSection -> CjPsiBundle.message("kdoc.section.title.constructor")
                is ReturnsSection -> CjPsiBundle.message("kdoc.section.title.returns")
                is SinceSection -> CjPsiBundle.message("kdoc.section.title.since")
                is SuppressSection -> CjPsiBundle.message("kdoc.section.title.suppress")
            }

            is AuthorsSection -> CjPsiBundle.message("kdoc.section.title.author")
            is SeeAlsoSection -> CjPsiBundle.message("kdoc.section.title.see.also")
            is SamplesSection -> CjPsiBundle.message("kdoc.section.title.samples")
        }
    }
}

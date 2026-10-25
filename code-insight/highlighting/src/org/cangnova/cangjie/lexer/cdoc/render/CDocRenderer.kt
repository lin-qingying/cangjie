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

package org.cangnova.cangjie.lexer.cdoc.render

import com.intellij.codeInsight.documentation.DocumentationManagerUtil
import com.intellij.lang.documentation.DocumentationMarkup.SECTION_END
import com.intellij.lang.documentation.DocumentationMarkup.SECTION_HEADER_START
import com.intellij.lang.documentation.DocumentationMarkup.SECTION_SEPARATOR
import com.intellij.lang.documentation.QuickDocHighlightingHelper
import com.intellij.lang.documentation.QuickDocHighlightingHelper.appendStyledCodeBlock
import com.intellij.lang.documentation.QuickDocHighlightingHelper.appendStyledFragment
import com.intellij.lang.documentation.QuickDocHighlightingHelper.appendStyledInlineCode
import com.intellij.lang.documentation.QuickDocHighlightingHelper.appendStyledLinkFragment
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.codeinsight.highlighting.textAttributesKeyForCjElement
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.lexer.cdoc.CDocTemplate
import org.cangnova.cangjie.lexer.cdoc.insert
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocLink
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocName
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocSection
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocTag
import org.cangnova.cangjie.psi.CjBlockExpression
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjDeclarationWithBody
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjImplementationDetail
import org.cangnova.cangjie.psi.CjNonPublicApi
import org.cangnova.cangjie.psi.CjPsiBundle
import org.cangnova.cangjie.psi.CjTypeParameterListOwner
import org.cangnova.cangjie.psi.psiUtil.findDescendantOfType
import org.cangnova.cangjie.psi.psiUtil.getChildrenOfType
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser

/**
 * CDoc → Quick Doc 的渲染器。
 *
 * **来源**：本文件是 Kotlin `org.jetbrains.kotlin.idea.kdoc.KDocRenderer`
 * （`external/kotlin-plugin/highlighting/highlighting-shared/.../KDocRenderer.kt`）的平移移植。
 * 函数结构、分区顺序、Markdown 遍历规则、HTML 标签映射、链接高亮策略均与上游一致，
 * 仅做以下符号替换（不改变行为）：
 *
 * | 上游 | 本仓 | 原因 |
 * |---|---|---|
 * | `KDocTag` / `KDocSection` / `KDocLink` / `KDocName` | `CDoc*` 同名类 | 本仓命名空间 |
 * | `KotlinLanguage` | `CangJieLanguage` | 语言不同 |
 * | `KotlinBundle.message(...)` | `CjPsiBundle.message(...)` | 本仓资源束 |
 * | `KotlinHighlightingColors.*` | 平台标准 `DefaultLanguageHighlighterColors.*` / `CodeInsightColors.*` | 本仓未定义 KDoc 专用颜色键 |
 * | `KtNamedDeclaration` / `KtTypeParameterListOwner` / `KtDeclarationWithBody` | `CjDeclaration` / `CjTypeParameterListOwner` / `CjDeclarationWithBody` | PSI 类型不同 |
 * | `KDocReference` | `CDocReference` + `CaCfirCDocReference` | 本仓引用栈 |
 *
 * **已知差异（未移植）**：
 * - `receiver` 与 context parameters 分区上游存在，仓颉 `CDocKnownTag` 无对应标签，故不渲染；
 *
 * 链接解析与上游 K2 `KDocReferenceResolver` 对齐：单段名查可见作用域，限定名按路径下钻，
 * 实现见 `CaCfirCDocReference`。`extend` 成员链接需要位置感知的可见作用域视图，
 * 该缺口在 resolver 中明确记录。
 */
@CjNonPublicApi
@CjImplementationDetail
object CDocRenderer {

    /**
     * 渲染 Quick Doc 的 BODY：正文 + 分区（对位上游 `renderKDoc(contentTag, sections)`）。
     */
    fun StringBuilder.renderCDoc(
        contentTag: CDocTag,
        sections: List<CDocSection>,
    ) {
        insert(CDocTemplate.DescriptionBodyTemplate.CangJie()) {
            content {
                appendCDocContent(contentTag)
            }
            sections {
                appendCDocSections(sections)
            }
        }
    }

    private fun StringBuilder.appendCDocContent(docComment: CDocTag): StringBuilder =
        append(markdownToHtml(docComment, allowSingleParagraph = true))

    private fun StringBuilder.appendCDocSections(sections: List<CDocSection>) {
        val firstSection = sections.firstOrNull() ?: return
        val namedDeclaration = PsiTreeUtil.getParentOfType(
            firstSection, CjDeclaration::class.java, /* strict = */ false, CjFile::class.java
        )

        fun findTagsByName(name: String) =
            sequence { sections.forEach { yieldAll(it.findTagsByName(name)) } }

        fun findTagByName(name: String) = findTagsByName(name).firstOrNull()

        val typeParameterNames =
            (namedDeclaration as? CjTypeParameterListOwner)?.typeParameterList?.parameters
                ?.mapNotNull { it.name }
                ?.toSet() ?: emptySet()

        val paramTags = findTagsByName("param").filter { it.getSubjectName() != null }
        appendTagList(
            paramTags.filter { it.getSubjectName() !in typeParameterNames },
            CjPsiBundle.message("kdoc.section.title.parameters"),
            DefaultLanguageHighlighterColors.PARAMETER,
        )
        appendTagList(
            paramTags.filter { it.getSubjectName() in typeParameterNames },
            CjPsiBundle.message("kdoc.section.title.type.parameters"),
            DefaultLanguageHighlighterColors.PARAMETER,
        )

        val propertyTags = findTagsByName("property").filter { it.getSubjectName() != null }
        appendTagList(
            propertyTags,
            CjPsiBundle.message("kdoc.section.title.properties"),
            DefaultLanguageHighlighterColors.INSTANCE_FIELD,
        )

        appendTag(findTagByName("constructor"), CjPsiBundle.message("kdoc.section.title.constructor"))

        appendTag(findTagByName("return"), CjPsiBundle.message("kdoc.section.title.returns"))

        val throwTags = findTagsByName("throws").filter { it.getSubjectName() != null }
        val exceptionTags = findTagsByName("exception").filter { it.getSubjectName() != null }
        appendThrows(throwTags, exceptionTags)

        appendAuthors(findTagsByName("author"))
        appendTag(findTagByName("since"), CjPsiBundle.message("kdoc.section.title.since"))
        appendTag(findTagByName("suppress"), CjPsiBundle.message("kdoc.section.title.suppress"))

        appendSeeAlso(findTagsByName("see"))

        val sampleTags = findTagsByName("sample").filter { it.getSubjectLink() != null }
        appendSamplesList(sampleTags)
    }

    private fun StringBuilder.appendHyperlink(cDocLink: CDocLink) {
        val linkText = cDocLink.getLinkText()
        if (DumbService.isDumb(cDocLink.project)) {
            append(linkText)
        } else {
            DocumentationManagerUtil.createHyperlink(
                this,
                linkText,
                highlightQualifiedName(linkText, getTargetLinkElementAttributes(cDocLink.getTargetElement())),
                false,
            )
        }
    }

    private fun PsiReference.resolveToElement(): PsiElement? =
        (this as? PsiPolyVariantReference)?.multiResolve(false)?.firstOrNull()?.element ?: resolve()

    /**
     * 链接目标的行内属性（对位上游 `textAttributesKeyForKtElement(element)?.attributesKey`）。
     */
    private fun getTargetLinkElementAttributes(element: PsiElement?): TextAttributes {
        return element
            ?.let { textAttributesKeyForCjElement(it)?.attributesKey }
            ?.let { getTargetLinkElementAttributes(it) }
            ?: TextAttributes().apply {
                foregroundColor = EditorColorsManager.getInstance().globalScheme
                    .getColor(DefaultLanguageHighlighterColors.DOC_COMMENT_LINK)
            }
    }

    private fun getTargetLinkElementAttributes(key: TextAttributesKey): TextAttributes {
        return tuneAttributesForLink(EditorColorsManager.getInstance().globalScheme.getAttributes(key))
    }

    private fun highlightQualifiedName(qualifiedName: String, lastSegmentAttributes: TextAttributes): String {
        val linkComponents = qualifiedName.split(".")
        val qualifiedPath = linkComponents.subList(0, linkComponents.lastIndex)
        val elementName = linkComponents.last()
        return buildString {
            for (pathSegment in qualifiedPath) {
                val segmentAttributes = when {
                    pathSegment.isEmpty() || pathSegment.first().isLowerCase() -> DefaultLanguageHighlighterColors.IDENTIFIER
                    else -> DefaultLanguageHighlighterColors.CLASS_NAME
                }
                appendStyledLinkFragment(pathSegment, segmentAttributes)
                appendStyledLinkFragment(".", DefaultLanguageHighlighterColors.DOT)
            }
            appendStyledLinkFragment(elementName, lastSegmentAttributes)
        }
    }

    private fun CDocLink.getTargetElement(): PsiElement? {
        return getChildrenOfType<CDocName>().lastOrNull()?.references?.firstOrNull()?.resolveToElement()
    }

    private fun PsiElement.extractExampleText() = when (this) {
        is CjDeclarationWithBody -> {
            when (val body = bodyExpression) {
                is CjBlockExpression -> body.text.removeSurrounding("{", "}")
                else -> body?.text ?: text
            }
        }

        else -> text
    }

    private fun trimCommonIndent(text: String): String {
        fun String.leadingIndent() = indexOfFirst { !it.isWhitespace() }

        val lines = text.split('\n')
        val minIndent = lines.filter { it.trim().isNotEmpty() }.minOfOrNull(String::leadingIndent) ?: 0
        return lines.joinToString("\n") { it.drop(minIndent) }
    }

    private fun StringBuilder.appendSection(title: String, content: StringBuilder.() -> Unit) {
        append(SECTION_HEADER_START, title, ":", SECTION_SEPARATOR)
        content()
        append(SECTION_END)
    }

    private fun StringBuilder.appendSamplesList(sampleTags: Sequence<CDocTag>) {
        if (!sampleTags.any()) return

        appendSection(CjPsiBundle.message("kdoc.section.title.samples")) {
            sampleTags.forEach {
                it.getSubjectLink()?.let { subjectLink ->
                    append("<p>")
                    this@appendSamplesList.appendHyperlink(subjectLink)
                    this@appendSamplesList.appendStyledCodeBlock(
                        subjectLink.project,
                        CangJieLanguage,
                        if (DumbService.isDumb(subjectLink.project))
                            "// " + CjPsiBundle.message("kdoc.comment.unresolved")
                        else when (val target = subjectLink.getTargetElement()) {
                            null -> "// " + CjPsiBundle.message("kdoc.comment.unresolved")
                            else -> trimCommonIndent(target.extractExampleText()).htmlEscape()
                        }
                    )
                }
            }
        }
    }

    private fun StringBuilder.appendSeeAlso(seeTags: Sequence<CDocTag>) {
        if (!seeTags.any()) return

        val iterator = seeTags.iterator()

        appendSection(CjPsiBundle.message("kdoc.section.title.see.also")) {
            while (iterator.hasNext()) {
                val tag = iterator.next()
                val subjectName = tag.getSubjectName()
                val link = tag.getChildrenOfType<CDocLink>().lastOrNull()
                when {
                    link != null -> this.appendHyperlink(link)
                    subjectName != null -> DocumentationManagerUtil.createHyperlink(this, subjectName, subjectName, false)
                    else -> append(tag.getContent())
                }
                if (iterator.hasNext()) {
                    append(",<br>")
                }
            }
        }
    }

    private fun StringBuilder.appendAuthors(authorTags: Sequence<CDocTag>) {
        if (!authorTags.any()) return

        val iterator = authorTags.iterator()

        appendSection(CjPsiBundle.message("kdoc.section.title.author")) {
            while (iterator.hasNext()) {
                append(iterator.next().getContent())
                if (iterator.hasNext()) {
                    append(", ")
                }
            }
        }
    }

    private fun StringBuilder.appendThrows(throwsTags: Sequence<CDocTag>, exceptionsTags: Sequence<CDocTag>) {
        if (!throwsTags.any() && !exceptionsTags.any()) return

        appendSection(CjPsiBundle.message("kdoc.section.title.throws")) {

            fun CDocTag.append() {
                val subjectName = getSubjectName()
                if (subjectName != null) {
                    append("<p><code>")
                    val highlightedLinkLabel = highlightQualifiedName(
                        subjectName,
                        getTargetLinkElementAttributes(DefaultLanguageHighlighterColors.CLASS_NAME),
                    )
                    DocumentationManagerUtil.createHyperlink(this@appendSection, subjectName, highlightedLinkLabel, false)
                    append("</code>")
                    val exceptionDescription = markdownToHtml(this)
                    if (exceptionDescription.isNotBlank()) {
                        append(" - $exceptionDescription")
                    }
                }
            }

            throwsTags.forEach { it.append() }
            exceptionsTags.forEach { it.append() }
        }
    }

    private fun StringBuilder.appendTagList(
        tags: Sequence<CDocTag>,
        title: String,
        titleAttributes: TextAttributesKey,
    ) {
        if (!tags.any()) {
            return
        }

        appendSection(title) {
            tags.forEach {
                val subjectName = it.getSubjectName() ?: return@forEach

                append("<p><code>")
                when (val link = it.getChildrenOfType<CDocLink>().firstOrNull()) {
                    null -> appendStyledLinkFragment(subjectName, titleAttributes)
                    else -> appendHyperlink(link)
                }

                append("</code>")
                val elementDescription = markdownToHtml(it)
                if (elementDescription.isNotBlank()) {
                    append(" - $elementDescription")
                }
            }
        }
    }

    private fun StringBuilder.appendTag(tag: CDocTag?, title: String) {
        if (tag != null) {
            appendSection(title) {
                append(markdownToHtml(tag))
            }
        }
    }

    private fun markdownToHtml(comment: CDocTag, allowSingleParagraph: Boolean = false): String {
        val markdownTree = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(comment.getContent())
        val markdownNode = MarkdownNode(markdownTree, null, comment)

        // Avoid wrapping the entire converted contents in a <p> tag if it's just a single paragraph
        val maybeSingleParagraph = markdownNode.children.singleOrNull { it.type != MarkdownTokenTypes.EOL }

        val firstParagraphOmitted = when {
            maybeSingleParagraph != null && !allowSingleParagraph -> {
                maybeSingleParagraph.children.joinToString("") { if (it.text == "\n") " " else it.toHtml() }
            }

            else -> markdownNode.toHtml()
        }

        val topMarginOmitted = when {
            firstParagraphOmitted.startsWith("<p>") ->
                firstParagraphOmitted.replaceFirst("<p>", "<p style='margin-top:0;padding-top:0;'>")

            else -> firstParagraphOmitted
        }

        return topMarginOmitted
    }

    class MarkdownNode(val node: ASTNode, val parent: MarkdownNode?, val comment: CDocTag) {
        val children: List<MarkdownNode> = node.children.map { MarkdownNode(it, this, comment) }
        val endOffset: Int get() = node.endOffset
        val startOffset: Int get() = node.startOffset
        val type: IElementType get() = node.type
        val text: String get() = comment.getContent().substring(startOffset, endOffset)
        fun child(type: IElementType): MarkdownNode? = children.firstOrNull { it.type == type }
    }

    private fun MarkdownNode.visit(action: (MarkdownNode, () -> Unit) -> Unit) {
        action(this) {
            for (child in children) {
                child.visit(action)
            }
        }
    }

    private fun MarkdownNode.toHtml(): String {
        if (node.type == MarkdownTokenTypes.WHITE_SPACE) {
            return text   // do not trim trailing whitespace
        }

        val sb = StringBuilder()
        visit { node, processChildren ->
            fun wrapChildren(tag: String, newline: Boolean = false) {
                sb.append("<$tag>")
                processChildren()
                sb.append("</$tag>")
                if (newline) sb.appendLine()
            }

            val nodeType = node.type
            val nodeText = node.text
            when (nodeType) {
                MarkdownElementTypes.UNORDERED_LIST -> wrapChildren("ul", newline = true)
                MarkdownElementTypes.ORDERED_LIST -> wrapChildren("ol", newline = true)
                MarkdownElementTypes.LIST_ITEM -> wrapChildren("li")
                MarkdownElementTypes.EMPH -> wrapChildren("em")
                MarkdownElementTypes.STRONG -> wrapChildren("strong")
                GFMElementTypes.STRIKETHROUGH -> wrapChildren("del")
                MarkdownElementTypes.ATX_1 -> wrapChildren("h1")
                MarkdownElementTypes.ATX_2 -> wrapChildren("h2")
                MarkdownElementTypes.ATX_3 -> wrapChildren("h3")
                MarkdownElementTypes.ATX_4 -> wrapChildren("h4")
                MarkdownElementTypes.ATX_5 -> wrapChildren("h5")
                MarkdownElementTypes.ATX_6 -> wrapChildren("h6")
                MarkdownElementTypes.BLOCK_QUOTE -> wrapChildren("blockquote")
                MarkdownElementTypes.PARAGRAPH -> {
                    sb.trimEnd()
                    wrapChildren("p", newline = true)
                }

                MarkdownElementTypes.CODE_SPAN -> {
                    val startDelimiter = node.child(MarkdownTokenTypes.BACKTICK)?.text
                    if (startDelimiter != null) {
                        val text = node.text.substring(startDelimiter.length).removeSuffix(startDelimiter)
                        sb.appendStyledInlineCode(comment.project, CangJieLanguage, text)
                    }
                }

                MarkdownElementTypes.CODE_BLOCK,
                MarkdownElementTypes.CODE_FENCE -> {
                    sb.trimEnd()
                    var language: String? = null
                    val contents = StringBuilder()
                    node.children.forEach { child ->
                        when (child.type) {
                            MarkdownTokenTypes.CODE_FENCE_CONTENT,
                            MarkdownTokenTypes.CODE_LINE,
                            MarkdownTokenTypes.EOL -> contents.append(child.text)

                            MarkdownTokenTypes.FENCE_LANG -> language = child.text.trim().split(' ')[0]
                        }
                    }
                    sb.appendStyledCodeBlock(
                        project = comment.project,
                        language = QuickDocHighlightingHelper.guessLanguage(language) ?: CangJieLanguage,
                        code = contents,
                    )
                }

                MarkdownTokenTypes.FENCE_LANG,
                MarkdownTokenTypes.CODE_LINE,
                MarkdownTokenTypes.CODE_FENCE_CONTENT -> {
                    // skip：代码块内容已在 CODE_BLOCK / CODE_FENCE 分支整体输出
                }

                MarkdownElementTypes.SHORT_REFERENCE_LINK,
                MarkdownElementTypes.FULL_REFERENCE_LINK -> {
                    val linkLabelNode = node.child(MarkdownElementTypes.LINK_LABEL)
                    val linkLabelContent = linkLabelNode?.children
                        ?.dropWhile { it.type == MarkdownTokenTypes.LBRACKET }
                        ?.dropLastWhile { it.type == MarkdownTokenTypes.RBRACKET }
                    if (linkLabelContent != null) {
                        val label = linkLabelContent.joinToString(separator = "") { it.text }
                        val linkText = node.child(MarkdownElementTypes.LINK_TEXT)?.toHtml() ?: label
                        if (DumbService.isDumb(comment.project)) {
                            sb.append(linkText)
                        } else {
                            comment.findDescendantOfType<CDocName> { it.text == label }
                                ?.references
                                ?.firstOrNull()
                                ?.resolveToElement()
                                ?.let { resolvedLinkElement ->
                                    DocumentationManagerUtil.createHyperlink(
                                        sb,
                                        label,
                                        highlightQualifiedName(linkText, getTargetLinkElementAttributes(resolvedLinkElement)),
                                        false,
                                    )
                                }
                                ?: sb.appendStyledFragment(label, CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES)
                        }
                    } else {
                        sb.append(node.text)
                    }
                }

                MarkdownElementTypes.INLINE_LINK -> {
                    val label = node.child(MarkdownElementTypes.LINK_TEXT)?.toHtml()
                    val destination = node.child(MarkdownElementTypes.LINK_DESTINATION)?.text
                    if (label != null && destination != null) {
                        sb.append("<a href=\"$destination\">$label</a>")
                    } else {
                        sb.append(node.text)
                    }
                }

                MarkdownTokenTypes.TEXT,
                MarkdownTokenTypes.WHITE_SPACE,
                MarkdownTokenTypes.COLON,
                MarkdownTokenTypes.SINGLE_QUOTE,
                MarkdownTokenTypes.DOUBLE_QUOTE,
                MarkdownTokenTypes.LPAREN,
                MarkdownTokenTypes.RPAREN,
                MarkdownTokenTypes.LBRACKET,
                MarkdownTokenTypes.RBRACKET,
                MarkdownTokenTypes.EXCLAMATION_MARK,
                GFMTokenTypes.CHECK_BOX,
                GFMTokenTypes.GFM_AUTOLINK,
                GFMTokenTypes.DOLLAR -> {
                    sb.append(nodeText)
                }

                MarkdownTokenTypes.EOL -> {
                    sb.append(" ")
                }

                MarkdownTokenTypes.GT -> sb.append("&gt;")
                MarkdownTokenTypes.LT -> sb.append("&lt;")

                MarkdownElementTypes.LINK_TEXT -> {
                    val childrenWithoutBrackets = node.children.drop(1).dropLast(1)
                    for (child in childrenWithoutBrackets) {
                        sb.append(child.toHtml())
                    }
                }

                MarkdownTokenTypes.EMPH -> {
                    val parentNodeType = node.parent?.type
                    if (parentNodeType != MarkdownElementTypes.EMPH && parentNodeType != MarkdownElementTypes.STRONG) {
                        sb.append(node.text)
                    }
                }

                GFMTokenTypes.TILDE -> {
                    if (node.parent?.type != GFMElementTypes.STRIKETHROUGH) {
                        sb.append(node.text)
                    }
                }

                GFMElementTypes.TABLE -> {
                    val alignment: List<String> = getTableAlignment(node)
                    var addedBody = false
                    sb.append("<table>")

                    for (child in node.children) {
                        if (child.type == GFMElementTypes.HEADER) {
                            sb.append("<thead>")
                            processTableRow(sb, child, "th", alignment)
                            sb.append("</thead>")
                        } else if (child.type == GFMElementTypes.ROW) {
                            if (!addedBody) {
                                sb.append("<tbody>")
                                addedBody = true
                            }

                            processTableRow(sb, child, "td", alignment)
                        }
                    }

                    if (addedBody) {
                        sb.append("</tbody>")
                    }
                    sb.append("</table>")
                }

                else -> {
                    processChildren()
                }
            }
        }
        return sb.toString().trimEnd()
    }

    private fun StringBuilder.trimEnd() {
        while (isNotEmpty() && this[length - 1] == ' ') {
            deleteCharAt(length - 1)
        }
    }

    private fun String.htmlEscape(): String = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun processTableRow(sb: StringBuilder, node: MarkdownNode, cellTag: String, alignment: List<String>) {
        sb.append("<tr>")
        for ((i, child) in node.children.filter { it.type == GFMTokenTypes.CELL }.withIndex()) {
            val alignValue = alignment.getOrElse(i) { "" }
            val alignTag = if (alignValue.isEmpty()) "" else " align=\"$alignValue\""
            sb.append("<$cellTag$alignTag>")
            sb.append(child.toHtml())
            sb.append("</$cellTag>")
        }
        sb.append("</tr>")
    }

    private fun getTableAlignment(node: MarkdownNode): List<String> {
        val separatorRow = node.child(GFMTokenTypes.TABLE_SEPARATOR)
            ?: return emptyList()

        return separatorRow.text.split('|').filterNot { it.isBlank() }.map {
            val trimmed = it.trim()
            val left = trimmed.startsWith(':')
            val right = trimmed.endsWith(':')
            if (left && right) "center"
            else if (right) "right"
            else if (left) "left"
            else ""
        }
    }

    /**
     * If highlighted links has the same color as highlighted inline code blocks they will be indistinguishable.
     * In this case we should change link color to standard hyperlink color which we believe is apriori different.
     */
    private fun tuneAttributesForLink(attributes: TextAttributes): TextAttributes {
        val globalScheme = EditorColorsManager.getInstance().globalScheme
        if (attributes.foregroundColor == globalScheme.getAttributes(HighlighterColors.TEXT).foregroundColor
            || attributes.foregroundColor == globalScheme.getAttributes(DefaultLanguageHighlighterColors.IDENTIFIER).foregroundColor
        ) {
            val tuned = attributes.clone()
            tuned.foregroundColor = globalScheme.getAttributes(CodeInsightColors.HYPERLINK_ATTRIBUTES).foregroundColor
            return tuned
        }
        return attributes
    }
}

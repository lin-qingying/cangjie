@file:OptIn(
    org.cangnova.cangjie.psi.CjNonPublicApi::class,
    org.cangnova.cangjie.psi.CjImplementationDetail::class,
)

package org.cangnova.cangjie.documentation.cdoc

import org.cangnova.cangjie.documentation.api.AuthorsSection
import org.cangnova.cangjie.documentation.api.ConstructorSection
import org.cangnova.cangjie.documentation.api.DocumentationLink
import org.cangnova.cangjie.documentation.api.DocumentationSection
import org.cangnova.cangjie.documentation.api.LinkEntry
import org.cangnova.cangjie.documentation.api.NamedEntry
import org.cangnova.cangjie.documentation.api.ParametersSection
import org.cangnova.cangjie.documentation.api.PropertiesSection
import org.cangnova.cangjie.documentation.api.ReturnsSection
import org.cangnova.cangjie.documentation.api.SampleEntry
import org.cangnova.cangjie.documentation.api.SamplesSection
import org.cangnova.cangjie.documentation.api.SeeAlsoSection
import org.cangnova.cangjie.documentation.api.SinceSection
import org.cangnova.cangjie.documentation.api.SuppressSection
import org.cangnova.cangjie.documentation.api.ThrowsSection
import org.cangnova.cangjie.documentation.api.TypeParametersSection
import org.cangnova.cangjie.lexer.cdoc.psi.api.CDocCommentDescriptor
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocLink
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocName
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocSection
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocTag
import org.cangnova.cangjie.psi.psiUtil.getChildrenOfType

/** 提取默认主节摘要；当 primaryTag 本身是 tag 时不把它重复渲染为摘要。 */
fun buildDocumentationDescription(descriptor: CDocCommentDescriptor): String? {
    val primaryTag = descriptor.primaryTag
    if (primaryTag.name != null) return null
    return primaryTag.getContent().trim().takeIf(String::isNotBlank)
}

/** 将 CDoc 描述映射为有固定顺序的共享章节模型。 */
fun buildDocumentationModel(
    descriptor: CDocCommentDescriptor,
    typeParameterNames: Set<String>,
): List<DocumentationSection> {
    val sections = buildList {
        add(descriptor.primaryTag)
        addAll(descriptor.additionalSections.filterNot { it == descriptor.primaryTag })
    }

    fun tags(name: String): List<CDocTag> = sections.flatMap { section ->
        when (section) {
            is CDocSection -> section.findTagsByName(name)
            else -> section.getChildrenOfType<CDocTag>().filter { it.name == name }
        }
    }

    fun firstTag(name: String): CDocTag? = tags(name).firstOrNull()

    return buildList {
        val parameters = tags("param")
            .mapNotNull { it.toNamedEntry() }
        val (typeParameters, valueParameters) = parameters.partition { it.name in typeParameterNames }
        if (valueParameters.isNotEmpty()) add(ParametersSection(valueParameters))
        if (typeParameters.isNotEmpty()) add(TypeParametersSection(typeParameters))

        tags("property")
            .mapNotNull { it.toNamedEntry() }
            .takeIf { it.isNotEmpty() }
            ?.let { add(PropertiesSection(it)) }

        firstTag("constructor")?.textContent()?.takeIf(String::isNotBlank)?.let { add(ConstructorSection(it)) }
        firstTag("return")?.textContent()?.takeIf(String::isNotBlank)?.let { add(ReturnsSection(it)) }

        val exceptions = (tags("throws") + tags("exception"))
            .mapNotNull { it.toNamedEntry() }
        exceptions.takeIf { it.isNotEmpty() }?.let { add(ThrowsSection(it)) }

        tags("author")
            .map(CDocTag::textContent)
            .filter(String::isNotBlank)
            .takeIf { it.isNotEmpty() }
            ?.let { add(AuthorsSection(it)) }

        firstTag("since")?.textContent()?.takeIf(String::isNotBlank)?.let { add(SinceSection(it)) }
        firstTag("suppress")?.textContent()?.takeIf(String::isNotBlank)?.let { add(SuppressSection(it)) }

        tags("see")
            .mapNotNull { it.toLinkEntry() }
            .takeIf { it.isNotEmpty() }
            ?.let { add(SeeAlsoSection(it)) }

        tags("sample")
            .mapNotNull { it.getSubjectLink()?.toDocumentationLink() }
            .map { link -> SampleEntry(link.label, link) }
            .takeIf { it.isNotEmpty() }
            ?.let { add(SamplesSection(it)) }
    }
}

private fun CDocTag.toNamedEntry(): NamedEntry? {
    val name = getSubjectName()?.takeIf(String::isNotBlank) ?: return null
    val link = getChildrenOfType<CDocLink>().firstOrNull()?.toDocumentationLink()
    return NamedEntry(name, textContent(), link)
}

private fun CDocTag.toLinkEntry(): LinkEntry? {
    val link = getChildrenOfType<CDocLink>().lastOrNull()
    val label = link?.getLinkText()
        ?: getSubjectName()
        ?: textContent().takeIf(String::isNotBlank)
        ?: return null
    return LinkEntry(label, textContent(), link?.toDocumentationLink())
}

private fun CDocTag.textContent(): String = getContent().trim()

private fun CDocLink.toDocumentationLink(): DocumentationLink {
    val name = getChildrenOfType<CDocName>().lastOrNull()
    val qualifiedName = name?.getQualifiedName()?.joinToString(".")
    return DocumentationLink(
        label = getLinkText(),
        fqName = qualifiedName?.takeIf { '.' in it },
    )
}

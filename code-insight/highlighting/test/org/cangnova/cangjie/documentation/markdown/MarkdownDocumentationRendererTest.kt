package org.cangnova.cangjie.documentation.markdown

import org.cangnova.cangjie.documentation.api.AuthorsSection
import org.cangnova.cangjie.documentation.api.ConstructorSection
import org.cangnova.cangjie.documentation.api.DocumentationBody
import org.cangnova.cangjie.documentation.api.DocumentationContent
import org.cangnova.cangjie.documentation.api.DocumentationLink
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
import org.cangnova.cangjie.documentation.api.TextSection
import org.cangnova.cangjie.documentation.api.ThrowsSection
import org.cangnova.cangjie.documentation.api.TypeParametersSection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MarkdownDocumentationRendererTest {
    @Test
    fun `markdown renders shared definition summary and all section families`() {
        val renderer = MarkdownDocumentationRenderer(SectionTitles(::fixedTitle))
        val content = DocumentationContent(
            definition = "func convert(value: Payload): Payload",
            body = DocumentationBody(
                description = "Converts the payload.",
                sections = listOf(
                    ParametersSection(listOf(NamedEntry("value", "input payload", DocumentationLink("value", null)))),
                    TypeParametersSection(listOf(NamedEntry("T", "type description", null))),
                    PropertiesSection(listOf(NamedEntry("field", "property description", null))),
                    ThrowsSection(listOf(NamedEntry("Failure", "failure description", null))),
                    ConstructorSection("constructor details"),
                    ReturnsSection("converted payload"),
                    SinceSection("1.2"),
                    SuppressSection("internal only"),
                    AuthorsSection(listOf("Ada", "Grace")),
                    SeeAlsoSection(listOf(LinkEntry("Target", "related material", DocumentationLink("Target", null)))),
                    SamplesSection(listOf(SampleEntry("samples.example", DocumentationLink("samples.example", "samples.example")))),
                ),
            ),
            container = null,
        )

        assertEquals(
            """```cangjie
func convert(value: Payload): Payload
```

Converts the payload.

## Params
- `value` — input payload

## Type parameters
- `T` — type description

## Properties
- `field` — property description

## Throws
- `Failure` — failure description

## Constructor
constructor details

## Returns
converted payload

## Since
1.2

## Suppress
internal only

## Authors
Ada, Grace

## See Also
- Target — related material

## Samples
- samples.example""",
            renderer.render(content),
        )
    }

    @Test
    fun `hint and full empty body render only their available content`() {
        val renderer = MarkdownDocumentationRenderer(SectionTitles(::fixedTitle))
        val hint = DocumentationContent("func hinted()", body = null, container = null)
        val fullWithoutCdoc = DocumentationContent("func plain()", DocumentationBody(emptyList()), null)

        assertEquals("```cangjie\nfunc hinted()\n```", renderer.render(hint))
        assertEquals("```cangjie\nfunc plain()\n```", renderer.render(fullWithoutCdoc))
    }

    @Test
    fun `documentation links remain plain text rather than fabricated markdown links`() {
        val renderer = MarkdownDocumentationRenderer(SectionTitles(::fixedTitle))
        val output = renderer.render(
            DocumentationContent(
                definition = "func linked()",
                body = DocumentationBody(
                    listOf(
                        SeeAlsoSection(listOf(LinkEntry("pkg.Target", "see details", DocumentationLink("pkg.Target", "pkg.Target")))),
                    ),
                ),
                container = null,
            ),
        )

        assertEquals(true, output.contains("- pkg.Target — see details"))
        assertFalse(output.contains("]("))
    }

    @Test
    fun `full enum documentation keeps its ordinal while hints omit it`() {
        val renderer = MarkdownDocumentationRenderer(SectionTitles(::fixedTitle))
        val full = renderer.render(
            DocumentationContent(
                definition = "enum Red",
                body = DocumentationBody(emptyList(), enumOrdinal = 2),
                container = null,
            ),
        )
        val hint = renderer.render(DocumentationContent("enum Red", body = null, container = null))

        assertTrue(full.contains("ordinal: 2", ignoreCase = true))
        assertFalse(hint.contains("ordinal", ignoreCase = true))
    }

    private fun fixedTitle(section: org.cangnova.cangjie.documentation.api.DocumentationSection): String = when (section) {
        is ParametersSection -> "Params"
        is TypeParametersSection -> "Type parameters"
        is PropertiesSection -> "Properties"
        is ThrowsSection -> "Throws"
        is ConstructorSection -> "Constructor"
        is ReturnsSection -> "Returns"
        is SinceSection -> "Since"
        is SuppressSection -> "Suppress"
        is AuthorsSection -> "Authors"
        is SeeAlsoSection -> "See Also"
        is SamplesSection -> "Samples"
    }
}

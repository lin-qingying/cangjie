package org.cangnova.cangjie.documentation.html

import org.cangnova.cangjie.documentation.api.AuthorsSection
import org.cangnova.cangjie.documentation.api.ConstructorSection
import org.cangnova.cangjie.documentation.api.DocumentationBody
import org.cangnova.cangjie.documentation.api.DocumentationContainer
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
import org.cangnova.cangjie.documentation.api.ThrowsSection
import org.cangnova.cangjie.documentation.api.TypeParametersSection
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class HtmlDocumentationRendererTest : CjParsingTestCase(
    dataPath = "",
    fileExt = "cj",
    fileType = CangJieFileType.INSTANCE,
    CangJieParserDefinition(),
) {
    @BeforeEach
    fun setUpFixture() {
        setUp()
    }

    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

    @Test
    fun `HTML renderer dispatches both section families and all independent sections`() {
        val html = HtmlDocumentationRenderer(testProject()).renderBody(
            DocumentationBody(
                description = "Summary.",
                sections = listOf(
                    ParametersSection(listOf(NamedEntry("value", "input detail", null))),
                    TypeParametersSection(listOf(NamedEntry("T", "type detail", null))),
                    PropertiesSection(listOf(NamedEntry("field", "property detail", null))),
                    ThrowsSection(listOf(NamedEntry("Failure", "failure detail", null))),
                    ConstructorSection("constructor detail"),
                    ReturnsSection("return detail"),
                    SinceSection("1.2"),
                    SuppressSection("suppression detail"),
                    AuthorsSection(listOf("Ada", "Grace")),
                    SeeAlsoSection(listOf(LinkEntry("Target", "related detail", DocumentationLink("Target", null)))),
                    SamplesSection(listOf(SampleEntry("sample", null))),
                ),
            ),
        )

        assertContains(html, "<div class='content'>")
        listOf(
            "Summary.", "Params:", "value", "Type parameters:", "T", "Properties:", "field",
            "Throws:", "Failure", "Constructor:", "constructor detail", "Returns:", "return detail",
            "Since:", "1.2", "Suppress:", "suppression detail", "Authors:", "Ada, Grace",
            "See Also:", "Target", "Samples:", "sample",
        ).forEach { assertContains(html, it) }
    }

    @Test
    fun `complete HTML uses the injected definition and container links`() {
        val definitionRenderer = DefinitionRenderer { content -> "<b>${content.definition}</b>" }
        val linkSupport = object : DocumentationLinkSupport {
            override fun hyperlink(link: DocumentationLink, label: String): String =
                "<a href=\"doc:${link.fqName}\">$label</a>"

            override fun sampleCode(link: DocumentationLink): String? = null

            override fun containerIcon(container: DocumentationContainer): String = "Custom.Container"

            override fun fileIconSource(): String = "Custom.File"
        }
        val content = DocumentationContent(
            definition = "func escape()",
            body = DocumentationBody(emptyList()),
            container = DocumentationContainer.Package("sample.docs", "Docs.cj"),
        )

        val html = HtmlDocumentationRenderer(testProject(), definitionRenderer, linkSupport).render(content)

        assertContains(html, "<b>func escape()</b>")
        assertContains(html, "Custom.Container")
        assertContains(html, "<a href=\"doc:sample.docs\">sample.docs</a>")
        assertContains(html, "Custom.File")
        assertContains(html, "Docs.cj")
    }

    @Test
    fun `default definition renderer escapes HTML metacharacters`() {
        val rendered = DefaultDefinitionRenderer.render(
            DocumentationContent("func <T & U>", null, null),
        )

        assertEquals("func &lt;T &amp; U&gt;", rendered)
    }

    private fun testProject() = (createPsiFile("htmlRenderer", "func placeholder(): Unit {}") as CjFile).project
}

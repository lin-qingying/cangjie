package org.cangnova.cangjie.lsp.analysis

import com.google.gson.Gson
import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixture
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.HoverParams
import org.eclipse.lsp4j.MarkupContent
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private data class QuickDocumentationExpectationFile(
    val cases: List<QuickDocumentationExpectation>,
)

private data class QuickDocumentationExpectation(
    val name: String,
    val sectionTypes: List<String>,
    val sectionTitles: List<String>,
    val subjects: List<String>,
    val definitionText: String,
    val summary: String?,
)

/**
 * LSP 快速文档的跨端期望断言。
 */
class CangJieDocumentationHoverTest : AbstractLspIntegrationTest() {
    override val autoCreateDefaultSession: Boolean = false

    private val expectations: Map<String, QuickDocumentationExpectation> by lazy {
        val input = requireNotNull(javaClass.getResourceAsStream("/documentation/expectations.json")) {
            "Missing shared Quick Doc expectations resource"
        }
        input.bufferedReader(Charsets.UTF_8).use { reader ->
            Gson().fromJson(reader, QuickDocumentationExpectationFile::class.java).cases.associateBy { it.name }
        }
    }

    @Test
    fun `hover snapshots top level member and undocumented declarations`() {
        val fixture = LspWorkspaceFixtureBuilder()
            .source(
                "documentation.cj",
                """
                    package sample.documentation

                    public class Payload {}
                    public class Failure {}

                    /**
                     * Converts a payload.
                     * @param payload source payload
                     * @return converted payload
                     */
                    public func convert(payload: Payload): Payload { return payload }

                    public class Service {
                        /**
                         * Runs the operation.
                         * @throws Failure when the operation fails
                         * @sample sample.documentation.example
                         */
                        public func run(): Unit {}
                    }

                    public func example(): Unit {}

                    public func undocumented(): Int64 { return 0 }

                    public func probe(payload: Payload, service: Service): Int64 {
                        let converted = convert(payload)
                        service.run()
                        return undocumented()
                    }
                """.trimIndent(),
            )
            .build()

        fixture.use {
            createSession(fixture.initializeParams()).use { testSession ->
                val uri = fixture.uri("src/documentation.cj")
                testSession.openDocument(uri, fixture.text("src/documentation.cj"))

                assertMatches(testSession.hover(uri, fixture, "convert", occurrence = 2), expectations.getValue("topLevelFunction"))
                assertMatches(testSession.hover(uri, fixture, "run", occurrence = 2), expectations.getValue("classMember"))
                assertMatches(
                    testSession.hover(uri, fixture, "undocumented", occurrence = 2),
                    expectations.getValue("undocumentedDeclaration"),
                )
                assertMatches(
                    testSession.hover(uri, fixture, "Int64", occurrence = 1),
                    expectations.getValue("basicTypeInt64"),
                )
            }
        }
    }

    @Test
    fun `override hover displays the inherited base documentation`() {
        val fixture = LspWorkspaceFixtureBuilder()
            .source(
                "override.cj",
                """
                    package sample.documentation

                    public class Payload {}

                    public interface Base {
                        /**
                         * Base transform contract.
                         * @param [payload] base payload description
                         * @return base transform result
                         */
                        func transform(payload: Payload): Payload
                    }

                    public class Derived <: Base {
                        override func transform(payload: Payload): Payload { return payload }
                    }

                    public func use(value: Derived, payload: Payload): Payload {
                        return value.transform(payload)
                    }
                """.trimIndent(),
            )
            .build()

        fixture.use {
            createSession(fixture.initializeParams()).use { testSession ->
                val uri = fixture.uri("src/override.cj")
                testSession.openDocument(uri, fixture.text("src/override.cj"))

                val hover = hoverText(
                    testSession.hover(uri, fixture, "transform", relativePath = "src/override.cj", occurrence = 3),
                )
                assertTrue(hover.contains("Base transform contract."), "重写成员应显示基类 CDoc：$hover")
                assertTrue(hover.contains("## Params"), "继承的 @param 应使用共享章节格式：$hover")
                assertTrue(hover.contains("base payload description"), "继承的参数说明必须保留：$hover")
            }
        }
    }

    @Test
    fun `cross module hover restores symbols in each use site module`() {
        val fixture = LspWorkspaceFixtureBuilder()
            .addModule(name = "library-a", rootRelativePath = "library-a", sourceRoots = listOf("src"))
            .addModule(name = "library-b", rootRelativePath = "library-b", sourceRoots = listOf("src"))
            .addModule(
                name = "app-a",
                rootRelativePath = "app-a",
                sourceRoots = listOf("src"),
                requiredModuleNames = listOf("library-a"),
            )
            .addModule(
                name = "app-b",
                rootRelativePath = "app-b",
                sourceRoots = listOf("src"),
                requiredModuleNames = listOf("library-b"),
            )
            .file(
                "library-a/src/api.cj",
                """
                    package sample.shared

                    /** Library A declaration. */
                    public func selected(): Int64 { return 1 }
                """.trimIndent(),
            )
            .file(
                "library-b/src/api.cj",
                """
                    package sample.shared

                    /** Library B declaration. */
                    public func selected(): String { return "B" }
                """.trimIndent(),
            )
            .file(
                "app-a/src/use.cj",
                """
                    package sample.appa

                    import sample.shared.*

                    func useA(): Int64 { return selected() }
                """.trimIndent(),
            )
            .file(
                "app-b/src/use.cj",
                """
                    package sample.appb

                    import sample.shared.*

                    func useB(): String { return selected() }
                """.trimIndent(),
            )
            .build()

        fixture.use {
            createSession(fixture.initializeParams()).use { testSession ->
                val appAUri = fixture.uri("app-a/src/use.cj")
                val appBUri = fixture.uri("app-b/src/use.cj")
                testSession.openDocument(appAUri, fixture.text("app-a/src/use.cj"))
                testSession.openDocument(appBUri, fixture.text("app-b/src/use.cj"))
                testSession.openDocument(fixture.uri("library-a/src/api.cj"), fixture.text("library-a/src/api.cj"))
                testSession.openDocument(fixture.uri("library-b/src/api.cj"), fixture.text("library-b/src/api.cj"))

                val hoverA = hoverText(
                    testSession.hover(appAUri, fixture, "selected", relativePath = "app-a/src/use.cj"),
                )
                val hoverB = hoverText(
                    testSession.hover(appBUri, fixture, "selected", relativePath = "app-b/src/use.cj"),
                )
                assertTrue(hoverA.contains("func selected(): Int64"), "app-a 应恢复 library-a 的签名：$hoverA")
                assertTrue(hoverA.contains("Library A declaration."), "app-a 应读取 library-a 的 CDoc：$hoverA")
                assertTrue(hoverB.contains("func selected(): String"), "app-b 应恢复 library-b 的签名：$hoverB")
                assertTrue(hoverB.contains("Library B declaration."), "app-b 应读取 library-b 的 CDoc：$hoverB")
            }
        }
    }

    private fun hoverText(hover: Hover): String =
        (hover.contents?.right as? MarkupContent)?.value.orEmpty()

    private fun assertMatches(hover: Hover, expected: QuickDocumentationExpectation) {
        val markdown = hoverText(hover)
        val definition = markdown.substringAfter("```cangjie\n").substringBefore("\n```")
        assertEquals(expected.definitionText, definition, "定义头不符合 ${expected.name}")

        val headings = Regex("(?m)^## (.+)$").findAll(markdown).map { it.groupValues[1] }.toList()
        assertEquals(expected.sectionTitles, headings, "章节标题不符合 ${expected.name}")
        val sectionTypeByTitle = mapOf(
            "Params" to "ParametersSection",
            "Type parameters" to "TypeParametersSection",
            "Properties" to "PropertiesSection",
            "Throws" to "ThrowsSection",
            "Constructor" to "ConstructorSection",
            "Returns" to "ReturnsSection",
            "Since" to "SinceSection",
            "Suppress" to "SuppressSection",
            "Authors" to "AuthorsSection",
            "See Also" to "SeeAlsoSection",
            "Samples" to "SamplesSection",
        )
        assertEquals(expected.sectionTypes, headings.map { sectionTypeByTitle.getValue(it) }, "章节类型不符合 ${expected.name}")
        assertEquals(expected.subjects, extractSubjects(markdown), "条目 subject 不符合 ${expected.name}")
        expected.summary?.let { summary ->
            assertTrue(markdown.contains(summary), "摘要不符合 ${expected.name}: $markdown")
        }
    }

    private fun extractSubjects(markdown: String): List<String> {
        var insideSection = false
        return markdown.lineSequence().mapNotNull { line ->
            when {
                line.startsWith("## ") -> {
                    insideSection = true
                    null
                }

                insideSection && line.startsWith("- `") -> line.removePrefix("- `").substringBefore('`')
                insideSection && line.startsWith("- ") -> line.removePrefix("- ").substringBefore(" — ")
                else -> null
            }
        }.toList()
    }

    private fun org.cangnova.cangjie.lsp.framework.LspIntegrationTestSession.hover(
        uri: String,
        fixture: LspWorkspaceFixture,
        token: String,
        relativePath: String = "src/documentation.cj",
        occurrence: Int = 1,
    ): Hover = hover(
        HoverParams(
            TextDocumentIdentifier(uri),
            positionOf(fixture, relativePath, token, occurrence),
        ),
    )

    private fun positionOf(
        fixture: LspWorkspaceFixture,
        relativePath: String,
        token: String,
        occurrence: Int,
    ): org.eclipse.lsp4j.Position {
        val text = fixture.text(relativePath)
        var searchFrom = 0
        var offset = -1
        repeat(occurrence) {
            offset = text.indexOf(token, searchFrom)
            check(offset >= 0) { "Cannot find occurrence $occurrence of `$token` in $relativePath" }
            searchFrom = offset + token.length
        }
        return LspTextDocument(
            uri = fixture.uri(relativePath),
            languageId = "cangjie",
            version = 1,
            text = text,
        ).positionAt(offset)
    }
}

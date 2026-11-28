package org.cangnova.cangjie.documentation.cdoc

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.documentation.api.AuthorsSection
import org.cangnova.cangjie.documentation.api.ConstructorSection
import org.cangnova.cangjie.documentation.api.ParametersSection
import org.cangnova.cangjie.documentation.api.PropertiesSection
import org.cangnova.cangjie.documentation.api.ReturnsSection
import org.cangnova.cangjie.documentation.api.SamplesSection
import org.cangnova.cangjie.documentation.api.SeeAlsoSection
import org.cangnova.cangjie.documentation.api.SinceSection
import org.cangnova.cangjie.documentation.api.SuppressSection
import org.cangnova.cangjie.documentation.api.ThrowsSection
import org.cangnova.cangjie.documentation.api.TypeParametersSection
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.cdoc.psi.CDoc
import org.cangnova.cangjie.lexer.cdoc.psi.api.CDocCommentDescriptor
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocCommentDescriptorImpl
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjImplementationDetail
import org.cangnova.cangjie.psi.CjNonPublicApi
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(CjNonPublicApi::class, CjImplementationDetail::class)
class DocumentationModelTest : CjParsingTestCase(
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
    fun `all CDoc tags map to the ordered section model`() {
        val descriptor = descriptorFor(
            """
            /**
             * Summary.
             * @param [value] input details
             * @param [T] type details
             * @property [field] property details
             * @constructor construction details
             * @return output details
             * @throws [Failure] failure details
             * @exception [OtherFailure] other details
             * @author Ada
             * @author Grace
             * @since 1.2
             * @suppress internal detail
             * @see [Target] related details
             * @sample [samples.example]
             */
            func documented<T>(value: T): T { return value }
            """.trimIndent(),
        )
        val model = buildDocumentationModel(
            descriptor,
            typeParameterNames = setOf("T"),
        )

        assertEquals("Summary.", buildDocumentationDescription(descriptor))
        assertEquals(
            listOf(
                "ParametersSection",
                "TypeParametersSection",
                "PropertiesSection",
                "ConstructorSection",
                "ReturnsSection",
                "ThrowsSection",
                "AuthorsSection",
                "SinceSection",
                "SuppressSection",
                "SeeAlsoSection",
                "SamplesSection",
            ),
            model.map { it::class.simpleName },
        )
        assertEquals(listOf("value"), assertIs<ParametersSection>(model[0]).entries.map { it.name })
        assertEquals(listOf("T"), assertIs<TypeParametersSection>(model[1]).entries.map { it.name })
        assertEquals("field", assertIs<PropertiesSection>(model[2]).entries.single().name)
        assertEquals("construction details", assertIs<ConstructorSection>(model[3]).markdown)
        assertEquals("output details", assertIs<ReturnsSection>(model[4]).markdown)
        assertEquals(
            listOf("Failure", "OtherFailure"),
            assertIs<ThrowsSection>(model[5]).entries.map { it.name },
        )
        assertEquals(listOf("Ada", "Grace"), assertIs<AuthorsSection>(model[6]).names)
        assertEquals("1.2", assertIs<SinceSection>(model[7]).markdown)
        assertEquals("internal detail", assertIs<SuppressSection>(model[8]).markdown)
        assertEquals("Target", assertIs<SeeAlsoSection>(model[9]).entries.single().label)

        val sample = assertIs<SamplesSection>(model[10]).entries.single()
        assertEquals("samples.example", sample.label)
        assertEquals("samples.example", sample.link?.fqName)
    }

    @Test
    fun `parameter groups are disjoint and empty tags do not create sections`() {
        val model = buildDocumentationModel(
            descriptorFor(
                """
                /**
                 * @param [value] input details
                 * @param [T] type details
                 * @return
                 */
                func documented<T>(value: T): T { return value }
                """.trimIndent(),
            ),
            typeParameterNames = setOf("T"),
        )

        val valueNames = assertIs<ParametersSection>(model.single { it is ParametersSection }).entries.map { it.name }
        val typeNames = assertIs<TypeParametersSection>(model.single { it is TypeParametersSection }).entries.map { it.name }
        assertEquals(setOf("value"), valueNames.toSet())
        assertEquals(setOf("T"), typeNames.toSet())
        assertEquals(0, model.count { it is ReturnsSection })
    }

    @Test
    fun `description is absent for empty and tag primary content`() {
        val empty = descriptorFor(
            """
            /** */
            func empty(): Unit {}
            """.trimIndent(),
        )
        assertEquals(null, buildDocumentationDescription(empty))

        val full = descriptorFor(
            """
            /**
             * Summary.
             * @return result
             */
            func tagged(): Int64 { return 1 }
            """.trimIndent(),
        )
        val returnTag = full.additionalSections
            .flatMap { it.findTagsByName("return") }
            .single()
        assertEquals(
            null,
            buildDocumentationDescription(CDocCommentDescriptorImpl(returnTag, emptyList())),
        )
    }

    private fun descriptorFor(text: String): CDocCommentDescriptor {
        val file = createPsiFile("documentationModel", text) as CjFile
        val docComment = checkNotNull(PsiTreeUtil.findChildrenOfType(file, CDoc::class.java).firstOrNull())
        return CDocCommentDescriptorImpl(docComment.getDefaultSection(), docComment.getAllSections())
    }
}

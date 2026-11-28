package org.cangnova.cangjie.cfir.builder

import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(JUnit3RunnerWithInners::class)
class PsiMacroAnnotationSourceMappingTest : AbstractRawCfirBuilderTestCase() {
    fun testAnnotationInsideMacroAttributeKeepsItsHostSourceRange() {
        val sourceText = """
            @Annotation
            public class A { const init(_: Int64) {} }

            @A[{ =>
                @A[0] let x = 0
                x
            }()]
            class B {}

            main() {}
        """.trimIndent()
        val file = createCjFile("annotationMacroSource", sourceText)
        val metadata = CfirAnnotationMetadataBuilder()

        PsiRawCfirBuilder(
            createTestSession(),
            BodyBuildingMode.NORMAL,
            metadata,
        ).buildPreMacroCfirFile(file)

        val annotation = metadata.snapshots.single { it.rawSyntax == "@A[0]" }
        assertNotNull(annotation.annotationSource)
        assertEquals(sourceText.indexOf("@A[0]"), annotation.annotationSource.startOffset)
        assertEquals(sourceText.indexOf("@A[0]") + "@A[0]".length, annotation.annotationSource.endOffset)

        val annotationSurface = metadata.snapshots.single { it.rawSyntax.startsWith("@A[{") }
        assertTrue(
            annotationSurface.tokens.all { token ->
                token.sourceRange?.let { range ->
                    range.startOffset == token.startOffset && range.endOffset == token.endOffset
                } == true
            },
        )
    }
}

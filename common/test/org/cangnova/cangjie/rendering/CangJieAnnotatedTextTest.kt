package org.cangnova.cangjie.rendering

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CangJieAnnotatedTextTest {
    @Test
    fun `semantic ranges preserve exact rendered slices`() {
        val rendered = CangJieAnnotatedText(
            text = "func convert(payload: Payload)",
            annotations = listOf(
                CangJieTextAnnotation(5, 12, CangJieSemanticKind.FUNCTION),
                CangJieTextAnnotation(13, 20, CangJieSemanticKind.PARAMETER),
                CangJieTextAnnotation(22, 29, CangJieSemanticKind.CLASS),
            ),
        )

        assertEquals(
            listOf("convert", "payload", "Payload"),
            rendered.annotations.map { rendered.text.substring(it.startOffset, it.endOffset) },
        )
    }

    @Test
    fun `semantic ranges reject overlap and out of bounds offsets`() {
        assertFailsWith<IllegalArgumentException> {
            CangJieAnnotatedText(
                text = "foobar",
                annotations = listOf(
                    CangJieTextAnnotation(0, 4, CangJieSemanticKind.FUNCTION),
                    CangJieTextAnnotation(3, 6, CangJieSemanticKind.CLASS),
                ),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            CangJieAnnotatedText(
                text = "foo",
                annotations = listOf(CangJieTextAnnotation(1, 4, CangJieSemanticKind.CLASS)),
            )
        }
    }
}

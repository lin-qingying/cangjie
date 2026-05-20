package org.cangnova.cangjie.formatter

import kotlin.test.Test
import kotlin.test.assertEquals
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

class CangJieFormatterTest : CjParsingTestCase(
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
    fun testFormatsFunctionBodyAndBinarySpacing() {
        val file = createPsiFile(
            "formatting",
            """
            func main(){
            let value=1+2
            }
            """.trimIndent(),
        ) as CjFile

        val formatted = CangJieFormatter.format(file).trimEnd()
        assertEquals(
            """
            func main() {
                let value = 1 + 2
            }
            """.trimIndent(),
            formatted,
        )
    }
}

package org.cangnova.cangjie.psi

import com.intellij.psi.util.PsiTreeUtil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.stubs.CangJieExtendStub
import org.cangnova.cangjie.psi.stubs.CangJieFileStub
import org.cangnova.cangjie.psi.stubs.elements.CjFileStubBuilder
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

class ExtendIdParsingTest : CjParsingTestCase(
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
    fun testExtendIdIsStableAcrossPsiAndStub() {
        val file = createPsiFile(
            "extendIdStable",
            """
            package sample.extendid

            interface Beta {}
            interface Alpha {}
            class Host {}

            extend Host <: Beta & Alpha {}
            """.trimIndent(),
        ) as CjFile

        val extend = PsiTreeUtil.findChildOfType(file, CjExtend::class.java)
        val extendStub = (CjFileStubBuilder().buildStubTree(file) as? CangJieFileStub)
            ?.childrenStubs
            ?.filterIsInstance<CangJieExtendStub>()
            ?.singleOrNull()

        assertNotNull(extend, "extend declaration should be parsed")
        assertNotNull(extendStub, "extend stub should be built from source PSI")
        assertEquals("sample.extendid:Host<:Alpha&Beta", extend.getExtendId())
        assertEquals(extend.getExtendId(), extendStub.extendId)
    }
}

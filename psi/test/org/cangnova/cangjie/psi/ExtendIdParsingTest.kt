package org.cangnova.cangjie.psi

import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.io.AbstractStringEnumerator
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.stubs.CangJieExtendStub
import org.cangnova.cangjie.psi.stubs.CangJieFileStub
import org.cangnova.cangjie.psi.stubs.CangJiePackageDirectiveStub
import org.cangnova.cangjie.psi.stubs.elements.CjFileStubBuilder
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
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

    @Test
    fun testPackageDirectiveStubPersistsMacroPackageFlag() {
        val macroFile = createPsiFile(
            "macroPackageDirective",
            """
            macro package sample.macros

            public macro Make(input: Tokens): Tokens {
                return input
            }
            """.trimIndent(),
        ) as CjFile
        val ordinaryFile = createPsiFile(
            "ordinaryPackageDirective",
            """
            package sample.ordinary

            class Host {}
            """.trimIndent(),
        ) as CjFile

        val macroPackageDirectiveStub = packageDirectiveStub(macroFile)
        val ordinaryPackageDirectiveStub = packageDirectiveStub(ordinaryFile)

        assertEquals(true, macroFile.packageDirective?.isMacroPackage)
        assertEquals(false, ordinaryFile.packageDirective?.isMacroPackage)
        assertEquals(true, macroPackageDirectiveStub.isMacroPackage)
        assertEquals(false, ordinaryPackageDirectiveStub.isMacroPackage)
        assertEquals(true, serializeAndDeserialize(macroPackageDirectiveStub).isMacroPackage)
        assertEquals(false, serializeAndDeserialize(ordinaryPackageDirectiveStub).isMacroPackage)
    }

    private fun packageDirectiveStub(file: CjFile): CangJiePackageDirectiveStub {
        return (CjFileStubBuilder().buildStubTree(file) as? CangJieFileStub)
            ?.childrenStubs
            ?.filterIsInstance<CangJiePackageDirectiveStub>()
            ?.singleOrNull()
            ?: error("package directive stub should be built from source PSI")
    }

    private fun serializeAndDeserialize(stub: CangJiePackageDirectiveStub): CangJiePackageDirectiveStub {
        val byteStream = ByteArrayOutputStream()
        StubOutputStream(byteStream, UnusedStringEnumerator).use { output ->
            CjStubElementTypes.PACKAGE_DIRECTIVE.serialize(stub, output)
        }

        return StubInputStream(ByteArrayInputStream(byteStream.toByteArray()), UnusedStringEnumerator).use { input ->
            CjStubElementTypes.PACKAGE_DIRECTIVE.deserialize(input, stub.parentStub)
        }
    }

    private object UnusedStringEnumerator : AbstractStringEnumerator {
        override fun enumerate(value: String?): Int {
            throw IOException("Package directive macro flag test does not serialize strings")
        }

        override fun valueOf(idx: Int): String {
            throw IOException("Package directive macro flag test does not deserialize strings")
        }

        override fun markCorrupted() {
        }

        override fun force() {
        }

        override fun isDirty(): Boolean = false

        override fun close() {
        }
    }
}

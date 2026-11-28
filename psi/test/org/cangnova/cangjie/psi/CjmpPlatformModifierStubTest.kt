package org.cangnova.cangjie.psi

import com.intellij.psi.stubs.ObjectStubSerializer
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.AbstractStringEnumerator
import org.cangnova.cangjie.CjmpDeclarationRoleResolver
import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.stubs.CangJieFileStub
import org.cangnova.cangjie.psi.stubs.CangJieModifierListStub
import org.cangnova.cangjie.psi.stubs.elements.CjFileStubBuilder
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CjmpPlatformModifierStubTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun platformModifierSurvivesStubRoundTripAndVersionedRoleResolution() {
        val file = createPsiFile("platformStub", "platform func target(): Unit {}") as CjFile
        val original = CjFileStubBuilder().buildStubTree(file) as CangJieFileStub
        val restored = roundTrip(original) as CangJieFileStub
        assertNull(restored.psi)
        val modifier = descendants(restored).filterIsInstance<CangJieModifierListStub>()
            .single { it.hasModifier(CjTokens.PLATFORM_KEYWORD) }
        val psi = modifier.psi
        assertTrue(psi.hasModifier(CjTokens.PLATFORM_KEYWORD))
        assertFalse(psi.hasModifier(CjTokens.SPECIFIC_KEYWORD))

        fun role(version: LanguageVersion) = CjmpDeclarationRoleResolver.resolve(
            psi.hasModifier(CjTokens.COMMON_KEYWORD),
            psi.hasModifier(CjTokens.SPECIFIC_KEYWORD),
            psi.hasModifier(CjTokens.PLATFORM_KEYWORD),
            LanguageVersionSettingsImpl(version),
        )
        assertTrue(role(LanguageVersion.CANGJIE_1_1_0_ALPHA_34).isSpecific)
        assertFalse(role(LanguageVersion.CANGJIE_1_1_0_BETA_20).isSpecific)
        assertNull(restored.psi)
    }

    private fun descendants(root: StubElement<*>): List<StubElement<*>> =
        root.childrenStubs.flatMap { listOf(it) + descendants(it) }

    private fun roundTrip(
        original: StubElement<*>,
        parent: StubElement<*>? = null,
        strings: AbstractStringEnumerator = TestStrings(),
    ): StubElement<*> {
        @Suppress("DEPRECATION")
        val serializer = if (original is PsiFileStub<*>) original.type else original.stubType
        @Suppress("UNCHECKED_CAST")
        serializer as ObjectStubSerializer<StubElement<*>, StubElement<*>>
        val bytes = ByteArrayOutputStream()
        StubOutputStream(bytes, strings).use { serializer.serialize(original, it) }
        val restored = StubInputStream(ByteArrayInputStream(bytes.toByteArray()), strings).use {
            serializer.deserialize(it, parent).also { _ -> assertEquals(-1, it.read()) }
        }
        for (child in original.childrenStubs) roundTrip(child, restored, strings)
        return restored
    }

    private class TestStrings : AbstractStringEnumerator {
        private val ids = HashMap<String, Int>()
        private val values = mutableListOf<String>()
        override fun enumerate(value: String?): Int {
            if (value == null) return 0
            return ids.getOrPut(value) { values += value; values.size }
        }
        override fun valueOf(idx: Int): String? = if (idx == 0) null else values[idx - 1]
        override fun markCorrupted(): Unit = error("Unexpected persistent storage operation")
        override fun close(): Unit = error("Unexpected persistent storage operation")
        override fun isDirty(): Boolean = error("Unexpected persistent storage operation")
        override fun force(): Unit = error("Unexpected persistent storage operation")
    }
}

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

/**
 * 表示 `ExtendIdParsingTest`，承载PSI 测试中的语法节点、索引桩或辅助模型。
 */
class ExtendIdParsingTest : CjParsingTestCase(
    dataPath = "",
    fileExt = "cj",
    fileType = CangJieFileType.INSTANCE,
    CangJieParserDefinition(),
) {
    /**
     * 提供 `setUpFixture` 操作，封装PSI 测试节点的访问、构造或判断逻辑。
     */
    @BeforeEach
    fun setUpFixture() {
        setUp()
    }

    /**
     * 提供 `tearDownFixture` 操作，封装PSI 测试节点的访问、构造或判断逻辑。
     */
    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

    /**
     * 提供 `testExtendIdIsStableAcrossPsiAndStub` 操作，封装PSI 测试节点的访问、构造或判断逻辑。
     */
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

    /**
     * 提供 `testPackageDirectiveStubPersistsMacroPackageFlag` 操作，封装PSI 测试节点的访问、构造或判断逻辑。
     */
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

    /**
     * 执行 `packageDirectiveStub` 内部辅助逻辑，支撑PSI 测试节点的结构解析与访问。
     */
    private fun packageDirectiveStub(file: CjFile): CangJiePackageDirectiveStub {
        return (CjFileStubBuilder().buildStubTree(file) as? CangJieFileStub)
            ?.childrenStubs
            ?.filterIsInstance<CangJiePackageDirectiveStub>()
            ?.singleOrNull()
            ?: error("package directive stub should be built from source PSI")
    }

    /**
     * 执行 `serializeAndDeserialize` 内部辅助逻辑，支撑PSI 测试节点的结构解析与访问。
     */
    private fun serializeAndDeserialize(stub: CangJiePackageDirectiveStub): CangJiePackageDirectiveStub {
        val byteStream = ByteArrayOutputStream()
        StubOutputStream(byteStream, UnusedStringEnumerator).use { output ->
            CjStubElementTypes.PACKAGE_DIRECTIVE.serialize(stub, output)
        }

        return StubInputStream(ByteArrayInputStream(byteStream.toByteArray()), UnusedStringEnumerator).use { input ->
            CjStubElementTypes.PACKAGE_DIRECTIVE.deserialize(input, stub.parentStub)
        }
    }

    /**
     * 提供 `UnusedStringEnumerator` 单例，集中承载PSI 测试的共享状态、工厂或工具行为。
     */
    private object UnusedStringEnumerator : AbstractStringEnumerator {
        /**
         * 实现 `enumerate` 的PSI 测试协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        override fun enumerate(value: String?): Int {
            throw IOException("Package directive macro flag test does not serialize strings")
        }

        /**
         * 实现 `valueOf` 的PSI 测试协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        override fun valueOf(idx: Int): String {
            throw IOException("Package directive macro flag test does not deserialize strings")
        }

        /**
         * 实现 `markCorrupted` 的PSI 测试协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        override fun markCorrupted() {
        }

        /**
         * 实现 `force` 的PSI 测试协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        override fun force() {
        }

        /**
         * 实现 `isDirty` 的PSI 测试协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        override fun isDirty(): Boolean = false

        /**
         * 实现 `close` 的PSI 测试协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        override fun close() {
        }
    }
}

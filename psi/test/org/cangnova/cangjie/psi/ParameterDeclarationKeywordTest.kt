package org.cangnova.cangjie.psi

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.mock.MockApplication
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.stubs.SerializationManagerEx
import com.intellij.psi.stubs.SerializationManagerImpl
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubElementTypeHolderEP
import com.intellij.psi.stubs.StubIndexExtension
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.stubs.CangJieFileStub
import org.cangnova.cangjie.psi.stubs.CangJieParameterStub
import org.cangnova.cangjie.psi.stubs.CangJieModifierListStub
import org.cangnova.cangjie.psi.stubs.elements.CjFileStubBuilder
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import java.util.function.Supplier
import kotlin.test.Test
import kotlin.test.assertEquals as assertValueEquals
import kotlin.test.assertNull as assertNoValue
import kotlin.test.assertSame as assertSameValue
import kotlin.test.assertTrue as assertTrueValue

/** let/var 是参数声明关键字，const 仅是修饰符；真实 stub 必须保持该区别和声明所有者。 */
class ParameterDeclarationKeywordTest : CjParsingTestCase(
    dataPath = "", fileExt = "cj", fileType = CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    @Test
    fun primaryKeywordsAndMemberNamesSurviveTheProductionStubProtocol() = withSerializationManager { manager ->
        for ((index, keyword) in KEYWORDS.withIndex()) {
            val named = index % 2 == 1
            val (file, parameter) = parseParameter(Role.PRIMARY, keyword, named)
            val hasDeclarationKeyword = keyword == "let" || keyword == "var" || keyword.endsWith(" let") || keyword.endsWith(" var")
            assertValueEquals("value", parameter.name, keyword)
            assertValueEquals(hasDeclarationKeyword, parameter.hasLetOrVar(), keyword)
            assertValueEquals(keyword == "var" || keyword.endsWith(" var"), parameter.isMutable, keyword)
            assertValueEquals(named, parameter.isNamed, keyword)
            assertValueEquals(named, parameter.hasDefaultValue(), keyword)
            assertValueEquals(
                if (hasDeclarationKeyword) "parameter_keyword.Owner.value" else null,
                parameter.fqName?.asString(), keyword,
            )
            assertRoundTrip(manager, file, parameter, Role.PRIMARY)
        }
    }

    /** 非主构造非法拼写只验证真实恢复结果，不把错误包装里的 token 冒充直接参数关键字。 */
    @Test
    fun ordinaryAndSecondaryParametersKeepTheirFactsWithoutAcquiringMemberNames() = withSerializationManager { manager ->
        for (role in listOf(Role.ORDINARY, Role.SECONDARY)) {
            for (keyword in KEYWORDS) {
                val (file, parameter) = parseParameter(role, keyword, named = false)
                if (keyword.isEmpty() || keyword == "const") {
                    assertValueEquals(false, parameter.hasLetOrVar(), "$role $keyword")
                    assertValueEquals(false, parameter.isMutable, "$role $keyword")
                }
                assertNoValue(parameter.fqName, "$role $keyword cannot declare a member")
                assertRoundTrip(manager, file, parameter, role)
            }
        }
    }

    /** 参数 modifier 的持久化位与声明关键字分离，非法 modifier 也不能在 stub 中丢失源码事实。 */
    @Test
    fun constUnsafeAndInoutModifiersSurviveDetachedParameterStubs() = withSerializationManager { manager ->
        val prefixes = listOf("const", "unsafe", "inout", "unsafe inout const", "const unsafe inout let", "inout const unsafe var")
        for (role in Role.entries) {
            for (prefix in prefixes) {
                val (file, parameter) = parseParameter(role, prefix, named = true)
                for (token in STORED_MODIFIERS) {
                    assertValueEquals(token.value in prefix.split(' '), parameter.hasModifier(token), "$role $prefix ${token.value}")
                }
                assertValueEquals(
                    if (role == Role.PRIMARY && (prefix.endsWith(" let") || prefix.endsWith(" var"))) "parameter_keyword.Owner.value" else null,
                    parameter.fqName?.asString(), "$role $prefix",
                )
                assertRoundTrip(manager, file, parameter, role)
            }
        }
    }

    private fun parseParameter(role: Role, keyword: String, named: Boolean): Pair<CjFile, CjParameter> {
        val nameAndType = if (named) "value!: Int64 = 1" else "value: Int64"
        val parameterText = listOf(keyword, nameAndType).filter(String::isNotEmpty).joinToString(" ")
        val declaration = when (role) {
            Role.PRIMARY -> "class Owner { Owner($parameterText) {} }"
            Role.SECONDARY -> "class Owner { init($parameterText) {} }"
            Role.ORDINARY -> "func ordinary($parameterText): Unit {}"
        }
        val file = createPsiFile("parameterKeyword${role.name}", "package parameter_keyword\n$declaration") as CjFile
        val parameters = when (role) {
            Role.PRIMARY -> file.declarations.filterIsInstance<CjClass>().single().primaryConstructor!!.valueParameters
            Role.SECONDARY -> file.declarations.filterIsInstance<CjClass>().single().secondaryConstructors.single().valueParameters
            Role.ORDINARY -> file.declarations.filterIsInstance<CjNamedFunction>().single().valueParameters
        }
        assertValueEquals(1, parameters.size, declaration)
        if (keyword in listOf("", "let", "var")) {
            assertNoValue(parameters.single().modifierList, "没有实际 modifier 的参数不得生成空 MODIFIER_LIST：$declaration")
        }
        return file to parameters.single()
    }

    /** CjFileStubBuilder 调用真实 CjParameterElementType producer，整树管理器使用同一类型的序列化协议。 */
    private fun assertRoundTrip(manager: SerializationManagerImpl, file: CjFile, parameter: CjParameter, role: Role) {
        val expected = facts(parameter)
        val original = CjFileStubBuilder().buildStubTree(file) as CangJieFileStub
        val sourceStub = descendants(original).filterIsInstance<CangJieParameterStub>().single()
        assertSameValue(CjStubElementTypes.VALUE_PARAMETER, sourceStub.stubType)
        assertValueEquals(expected, facts(sourceStub), "参数 producer 必须保存真实 PSI 事实")
        val expectedModifiers = STORED_MODIFIERS.associateWith { parameter.hasModifier(it) }
        val sourceModifierStub = sourceStub.childrenStubs.filterIsInstance<CangJieModifierListStub>().singleOrNull()
        for ((token, present) in expectedModifiers) {
            assertValueEquals(present, sourceModifierStub?.hasModifier(token) == true, "modifier producer: ${token.value}")
        }

        val bytes = ByteArrayOutputStream().also { manager.serialize(original, it) }.toByteArray()
        val restored = ByteArrayInputStream(bytes).use { input ->
            (manager.deserialize(input) as CangJieFileStub).also {
                assertValueEquals(-1, input.read(), "实际整树协议必须消费全部数据")
            }
        }
        val restoredStub = descendants(restored).filterIsInstance<CangJieParameterStub>().single()
        assertSameValue(CjStubElementTypes.VALUE_PARAMETER, restoredStub.stubType)
        assertValueEquals(expected, facts(restoredStub), "参数 serializer/deserializer 不得改变布尔位或 FQName")
        val restoredModifierStub = restoredStub.childrenStubs.filterIsInstance<CangJieModifierListStub>().singleOrNull()
        for ((token, present) in expectedModifiers) {
            assertValueEquals(present, restoredModifierStub?.hasModifier(token) == true, "modifier serializer: ${token.value}")
        }
        assertNoValue(restored.psi, "恢复根没有源 PSI，不能借原始 AST 回答查询")
        val detachedParameter = restoredStub.psi
        assertSameValue(restoredStub, detachedParameter.stub)
        checkedDetached(restored, "name") { assertValueEquals(expected.name, detachedParameter.name) }
        checkedDetached(restored, "hasLetOrVar") { assertValueEquals(expected.hasLetOrVar, detachedParameter.hasLetOrVar()) }
        checkedDetached(restored, "isMutable") { assertValueEquals(expected.isMutable, detachedParameter.isMutable) }
        checkedDetached(restored, "isNamed") { assertValueEquals(expected.isNamed, detachedParameter.isNamed) }
        checkedDetached(restored, "hasDefaultValue") { assertValueEquals(expected.hasDefaultValue, detachedParameter.hasDefaultValue()) }
        checkedDetached(restored, "fqName") { assertValueEquals(expected.fqName, detachedParameter.fqName?.asString()) }
        for ((token, present) in expectedModifiers) {
            checkedDetached(restored, "modifier ${token.value}") {
                assertValueEquals(present, detachedParameter.hasModifier(token))
            }
        }
        checkedDetached(restored, "owner") {
            val owner = detachedParameter.ownerFunction
            assertSameValue(owner, detachedParameter.ownerDeclaration)
            when (role) {
                Role.PRIMARY -> {
                    assertTrueValue(owner is CjPrimaryConstructor)
                    assertValueEquals("Owner", owner.getContainingTypeStatement().name)
                }
                Role.SECONDARY -> {
                    assertTrueValue(owner is CjSecondaryConstructor)
                    assertValueEquals("Owner", owner.getContainingTypeStatement().name)
                }
                Role.ORDINARY -> {
                    assertTrueValue(owner is CjNamedFunction)
                    assertValueEquals("ordinary", owner.name)
                }
            }
        }
        assertSameValue(restoredStub, detachedParameter.stub)
    }

    private inline fun checkedDetached(root: CangJieFileStub, label: String, query: () -> Unit) {
        assertNoValue(root.psi, "查询前无源 PSI：$label")
        query()
        assertNoValue(root.psi, "查询后仍无源 PSI：$label")
    }

    private fun facts(parameter: CjParameter): ParameterFacts = ParameterFacts(
        parameter.name, parameter.hasLetOrVar(), parameter.isMutable, parameter.isNamed,
        parameter.hasDefaultValue(), parameter.fqName?.asString(),
    )

    private fun facts(stub: CangJieParameterStub): ParameterFacts = ParameterFacts(
        stub.name, stub.hasLetOrVar(), stub.isMutable(), stub.isNamed(), stub.hasDefaultValue(), stub.getFqName()?.asString(),
    )

    private fun descendants(root: StubElement<*>): Sequence<StubElement<*>> = sequence {
        for (child in root.childrenStubs) {
            yield(child)
            yieldAll(descendants(child))
        }
    }

    /** 与已有 WholeTree 测试一样，使用 SDK 的生产整树序列化管理器，不手写参数字段协议或构造 AST。 */
    private fun withSerializationManager(block: (SerializationManagerImpl) -> Unit) {
        val application = ApplicationManager.getApplication() as MockApplication
        CoreApplicationEnvironment.registerExtensionPoint(
            application.extensionArea, StubElementTypeHolderEP.EP_NAME, StubElementTypeHolderEP::class.java,
        )
        CoreApplicationEnvironment.registerExtensionPoint(
            application.extensionArea, StubIndexExtension.EP_NAME, StubIndexExtension::class.java,
        )
        val previousManager = application.getServiceIfCreated(SerializationManagerEx::class.java)
        val manager = SerializationManagerImpl(Supplier<Path?> { null }, false, application.getCoroutineScope())
        application.picoContainer.unregisterComponent(SerializationManagerEx::class.java.name)
        application.registerService(SerializationManagerEx::class.java, manager, testRootDisposable)
        manager.initialize()
        try {
            block(manager)
        } finally {
            application.picoContainer.unregisterComponent(SerializationManagerEx::class.java.name)
            if (previousManager != null) application.registerService(SerializationManagerEx::class.java, previousManager)
        }
    }

    private data class ParameterFacts(
        val name: String?,
        val hasLetOrVar: Boolean,
        val isMutable: Boolean,
        val isNamed: Boolean,
        val hasDefaultValue: Boolean,
        val fqName: String?,
    )

    private enum class Role { PRIMARY, ORDINARY, SECONDARY }

    private companion object {
        val KEYWORDS = listOf("", "const", "let", "var", "const let", "const var")
        val STORED_MODIFIERS = listOf(CjTokens.CONST_KEYWORD, CjTokens.UNSAFE_KEYWORD, CjTokens.INOUT_KEYWORD)
    }
}

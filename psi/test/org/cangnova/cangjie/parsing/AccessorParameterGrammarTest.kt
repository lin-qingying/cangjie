package org.cangnova.cangjie.parsing

import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.CjParsingDiagnosticRange
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals as assertValueEquals
import kotlin.test.assertFalse as assertFalseValue
import kotlin.test.assertNotNull as assertPresent
import kotlin.test.assertNull as assertNoValue
import kotlin.test.assertTrue as assertTrueValue

/**
 * 官方 v1.1.3 ParseDecl.cpp:2414–2471：accessor 参数仅名字，普通及 primary 参数继续解析类型。
 * 本测试只验证普通源码的 parser 文法，不把 getter 参数数量等后续语义检查混入参数节点构造。
 */
class AccessorParameterGrammarTest : CjParsingTestCase(
    dataPath = "", fileExt = "cj", fileType = CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    @Test
    fun typedDeclarationParametersKeepTheirTypeAndNamedDefaultSyntax() {
        for (role in TYPED_ROLES) {
            for (named in listOf(false, true)) {
                val parameterText = if (named) "value!: Int64 = 1" else "value: Int64"
                val parsed = parseBoth(role, parameterText)
                assertValueEquals(emptyList<ParsingDiagnostic>(), parsed.diagnostics, parsed.text)
                assertParameter(parsed, parameterText, "value", "Int64", named, if (named) "1" else null)
                assertFalseValue(parsed.parameters.single().hasLetOrVar())
            }
        }
    }

    /** ParseLetOrVarInParamList 仅赋予真实 class/struct primary 成员参数身份。 */
    @Test
    fun onlyClassAndStructPrimaryParametersAcceptLetOrVar() {
        for (keyword in listOf("let", "var")) {
            val parameterText = keyword + " value: Int64"
            for (role in listOf(Role.CLASS_PRIMARY, Role.STRUCT_PRIMARY)) {
                val parsed = parseBoth(role, parameterText)
                assertValueEquals(emptyList<ParsingDiagnostic>(), parsed.diagnostics, parsed.text)
                assertParameter(parsed, parameterText, "value", "Int64", false, null)
                assertTrueValue(parsed.parameters.single().hasLetOrVar())
                assertValueEquals(keyword == "var", parsed.parameters.single().isMutable)
            }
            for (role in listOf(Role.FUNCTION, Role.SECONDARY)) {
                val parsed = parseBoth(role, parameterText)
                val start = parsed.parameterStart
                assertValueEquals(listOf(completeExpected(parsed.sourceContext, ParsingDiagnostic(
                    start, start + keyword.length,
                    CangJieParsingBundle.message("parsing.error.parameter.expected.name.or.rparen", keyword),
                ))), parsed.diagnostics, parsed.text)
                assertParameter(parsed, parameterText, "value", "Int64", false, null)
            }
        }
    }

    @Test
    fun ordinaryAndPrimaryParametersCannotUseAccessorNameOnlyGrammar() {
        for (role in TYPED_ROLES) {
            val prefixes = if (role == Role.CLASS_PRIMARY || role == Role.STRUCT_PRIMARY) listOf("", "let ", "var ") else listOf("")
            for (prefix in prefixes) {
                val parameterText = prefix + "value"
                val parsed = parseBoth(role, parameterText)
                assertTrueValue(parsed.diagnostics.any {
                    it.message == CangJieParsingBundle.message("parsing.error.parameters.must.have.type.annotation")
                }, parsed.text)
                assertParameter(parsed, parameterText, "value", null, false, null)
            }
        }
    }

    @Test
    fun getterAndSetterParametersContainOnlyTheirOriginalNameNode() {
        val getterWithoutParameters = parseBoth(Role.GETTER, "")
        assertValueEquals(emptyList<ParsingDiagnostic>(), getterWithoutParameters.diagnostics)
        assertTrueValue(getterWithoutParameters.parameters.isEmpty())
        for (role in listOf(Role.GETTER, Role.SETTER)) {
            for (parameterName in listOf("value", "public")) {
                val parsed = parseBoth(role, parameterName)
                assertValueEquals(emptyList<ParsingDiagnostic>(), parsed.diagnostics, parsed.text)
                assertParameter(parsed, parameterName, parameterName, null, false, null)
                assertFalseValue(parsed.parameters.single().hasLetOrVar())
                assertFalseValue(parsed.parameters.single().isMutable)
            }
        }
    }

    /** accessor 不消费后缀；冒号、命名标记、默认值由参数列表边界报告，不能伪造成合法形参属性。 */
    @Test
    fun accessorTypeNamedMarkAndDefaultSuffixesAreRejectedOutsideTheParameter() {
        val suffixes = listOf(": Int64" to ":", "!" to "!", " = 1" to "=", "!: Int64 = 1" to "!")
        for (role in listOf(Role.GETTER, Role.SETTER)) {
            for ((suffix, firstToken) in suffixes) {
                val parsed = parseBoth(role, "value" + suffix)
                assertTrueValue(parsed.diagnostics.isNotEmpty(), parsed.text)
                assertParameter(parsed, "value", "value", null, false, null)
                val offset = parsed.parameterStart + "value".length + suffix.indexOf(firstToken)
                val expected = completeExpected(parsed.sourceContext, ParsingDiagnostic(
                    offset, offset + firstToken.length,
                    CangJieParsingBundle.message("parsing.error.expecting.found", "',' or ')'", firstToken),
                ))
                assertTrueValue(expected in parsed.diagnostics, "Missing accessor list-boundary diagnostic: " + parsed.text + "\n" + parsed.diagnostics)
            }
        }
    }

    /** 官方先消费 ELLIPSIS，再分派参数角色；这里只检查 token/节点，不判定变长参数语义合法。 */
    @Test
    fun typedParameterRolesKeepEllipsisWithoutATypeOrDefault() {
        for (role in TYPED_ROLES) {
            assertEllipsisParameter(parseBoth(role, "..."), expectNoOwnerDiagnostics = false)
        }
    }

    @Test
    fun getterAndSetterKeepTheSharedEllipsisParameterPrefix() {
        for (role in listOf(Role.GETTER, Role.SETTER)) {
            assertEllipsisParameter(parseBoth(role, "..."), expectNoOwnerDiagnostics = true)
        }
    }

    private fun assertEllipsisParameter(parsed: Parsed, expectNoOwnerDiagnostics: Boolean) {
        if (expectNoOwnerDiagnostics) {
            assertValueEquals(emptyList<ParsingDiagnostic>(), parsed.diagnostics, parsed.text)
        }
        // native CheckVariableParams 可在非 foreign 函数/构造器头报告 owner 级 parser 错误。
        // 此处验证公共参数 token 文法，不能用“所有诊断为空”固化该检查尚未迁入的现状。
        assertTrueValue(parsed.diagnostics.none {
            it.startOffset in parsed.parameterStart..(parsed.parameterStart + 3)
        }, parsed.text + "\n" + parsed.diagnostics)
        val parameter = parsed.parameters.single()
        assertValueEquals("...", parameter.text, parsed.text)
        assertValueEquals(parsed.parameterStart, parameter.textRange.startOffset, parsed.text)
        assertValueEquals(parsed.parameterStart + 3, parameter.textRange.endOffset, parsed.text)
        val ellipsis = assertPresent(parameter.node.findChildByType(CjTokens.ELLIPSIS))
        assertValueEquals(parameter.textRange, ellipsis.textRange, parsed.text)
        assertNoValue(parameter.typeReference, parsed.text)
        assertNoValue(parameter.defaultValue, parsed.text)
        assertFalseValue(parameter.isNamed, parsed.text)
        assertFalseValue(parameter.hasLetOrVar(), parsed.text)
    }

    private fun assertParameter(
        parsed: Parsed,
        expectedText: String,
        expectedName: String,
        expectedType: String?,
        expectedNamed: Boolean,
        expectedDefault: String?,
    ) {
        assertValueEquals(1, parsed.parameters.size, parsed.text)
        val parameter = parsed.parameters.single()
        assertValueEquals(expectedText, parameter.text, parsed.text)
        assertValueEquals(parsed.parameterStart, parameter.textRange.startOffset, parsed.text)
        assertValueEquals(parsed.parameterStart + expectedText.length, parameter.textRange.endOffset, parsed.text)
        assertValueEquals(expectedName, parameter.name, parsed.text)
        val name = assertPresent(parameter.nameIdentifier)
        val nameStart = parsed.parameterStart + expectedText.indexOf(expectedName)
        assertValueEquals(nameStart, name.textRange.startOffset, parsed.text)
        assertValueEquals(nameStart + expectedName.length, name.textRange.endOffset, parsed.text)
        assertValueEquals(expectedType, parameter.typeReference?.text, parsed.text)
        assertValueEquals(expectedNamed, parameter.isNamed, parsed.text)
        assertValueEquals(expectedDefault, parameter.defaultValue?.text, parsed.text)
        if (expectedType == null) assertNoValue(parameter.typeReference)
    }

    /** 两条入口都解析完整普通源码；LightTree 显式开启 marker 检查，不使用任何片段包装 API。 */
    private fun parseBoth(role: Role, parameterText: String): Parsed {
        val declaration = when (role) {
            Role.FUNCTION -> "func target(" + parameterText + "): Unit {}"
            Role.SECONDARY -> "class Owner { init(" + parameterText + ") {} }"
            Role.CLASS_PRIMARY -> "class Owner { Owner(" + parameterText + ") {} }"
            Role.STRUCT_PRIMARY -> "struct Owner { Owner(" + parameterText + ") {} }"
            Role.GETTER -> "class Owner { prop slot: Int64 { get(" + parameterText + ") { 0 } } }"
            Role.SETTER -> "class Owner { prop slot: Int64 { get() { 0 } set(" + parameterText + ") {} } }"
        }
        val text = declaration + "\nfunc following(): Unit {}"
        val parameterStart = text.indexOf("(" + parameterText + ")") + 1
        val file = createPsiFile("accessorParameter" + role.name, text) as CjFile
        val psiDiagnostics = file.collectParsingDiagnostics()
        val parameters = PsiTreeUtil.findChildrenOfType(file, CjParameter::class.java).sortedBy { it.textRange.startOffset }
        val psiNodes = parameters.map { ParameterNode(it.textRange.startOffset, it.textRange.endOffset, it.text) }
        val lightDiagnostics = mutableListOf<ParsingDiagnostic>()
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
        builder.setDebugMode(true)
        val lightResult = parseLightWithPsiInput(file, builder)
        lightDiagnostics += lightResult.diagnostics
        val tree = lightResult.file.tree
        val lightNodes = mutableListOf<ParameterNode>()
        collectParameters(tree.root, tree, text, lightNodes)
        val sourceContext = checkNotNull(publishedParserSeed(file).sourceContext)
        assertPublishedParsingDiagnosticParity(psiDiagnostics, lightDiagnostics, sourceContext, text)
        assertValueEquals(psiNodes, lightNodes.sortedBy { it.start }, text)
        assertTrueValue(file.declarations.filterIsInstance<CjNamedFunction>().any { it.name == "following" }, text)
        return Parsed(text, parameterStart, parameters, psiDiagnostics, sourceContext)
    }

    /**
     * 两处旧 DTO 期望均为 INPUT/RANGE 且没有 hint；只补全其规范主范围，其他字段原样保留。
     * 快照来自真实 PSI 输入，坐标只由独立期望的 offset 和冻结全文计算，不读取实际诊断字段。
     */
    private fun completeExpected(context: CjParserSourceContext, expected: ParsingDiagnostic): ParsingDiagnostic {
        require(expected.origin == ParsingDiagnostic.Origin.INPUT && expected.source == null)
        require(expected.rangeKind == ParsingDiagnostic.RangeKind.RANGE && expected.mainRange == null)
        require(expected.hints.isEmpty() && expected.nativeHints.isEmpty())
        val source = context.snapshot
        return expected.copy(mainRange = CjParsingDiagnosticRange(
            source,
            expectedNativePosition(source.text, context.inputStart + expected.startOffset),
            expectedNativePosition(source.text, context.inputStart + expected.endOffset),
        ))
    }

    /** 独立扫描 CR/LF/CRLF，列按 UTF-8 字节计算；不调用被测 nativePosition 或 collector。 */
    private fun expectedNativePosition(text: String, offset: Int): Pair<Int, Int> {
        require(offset in 0..text.length)
        var line = 1
        var lineStart = 0
        var index = 0
        while (index < offset) {
            if (text[index] == '\r' || text[index] == '\n') {
                val nextLine = index + if (text[index] == '\r' && index + 1 < text.length && text[index + 1] == '\n') 2 else 1
                if (nextLine > offset) break
                line++
                lineStart = nextLine
                index = nextLine
            } else {
                index++
            }
        }
        return line to text.substring(lineStart, offset).toByteArray(Charsets.UTF_8).size + 1
    }

    private fun collectParameters(
        node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>, text: String, output: MutableList<ParameterNode>,
    ) {
        if (node.tokenType == CjStubElementTypes.VALUE_PARAMETER) output += ParameterNode(node.startOffset, node.endOffset, text.substring(node.startOffset, node.endOffset))
        val ref = Ref<Array<LighterASTNode?>>()
        val count = tree.getChildren(node, ref)
        val children = ref.get() ?: return
        try {
            for (index in 0 until count) collectParameters(requireNotNull(children[index]), tree, text, output)
        } finally {
            tree.disposeChildren(children, count)
        }
    }

    private data class Parsed(
        val text: String,
        val parameterStart: Int,
        val parameters: List<CjParameter>,
        val diagnostics: List<ParsingDiagnostic>,
        val sourceContext: CjParserSourceContext,
    )
    private data class ParameterNode(val start: Int, val end: Int, val text: String)
    private enum class Role { FUNCTION, SECONDARY, CLASS_PRIMARY, STRUCT_PRIMARY, GETTER, SETTER }
    private companion object {
        val TYPED_ROLES = listOf(Role.FUNCTION, Role.SECONDARY, Role.CLASS_PRIMARY, Role.STRUCT_PRIMARY)
    }
}

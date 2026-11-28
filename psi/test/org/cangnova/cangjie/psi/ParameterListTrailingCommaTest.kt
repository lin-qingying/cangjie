package org.cangnova.cangjie.psi

import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.parsing.collectParsingDiagnostics
import org.cangnova.cangjie.parsing.assertPublishedParsingDiagnosticParity
import org.cangnova.cangjie.parsing.parseLightWithPsiInput
import org.cangnova.cangjie.parsing.publishedParserSeed
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

/** 参数尾逗号遵循官方 ParseZeroOrMoreSepTrailing，不应产生错误或额外参数。 */
class ParameterListTrailingCommaTest : CjParsingTestCase(
    dataPath = "",
    fileExt = "cj",
    fileType = CangJieFileType.INSTANCE,
    CangJieParserDefinition(),
) {
    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    /** 普通声明及函数类型均使用真实参数列表，尾逗号后没有空参数节点。 */
    @Test
    fun testTrailingCommaKeepsOnlyDeclaredParametersInBothParsers() {
        val cases = listOf(
            "func ordinary(value: Int64, other: UInt32,): Unit {}" to
                listOf("value: Int64", "other: UInt32"),
            "foreign func imported(\n    callback: Callback,\n): Unit" to
                listOf("callback: Callback"),
            "class Owner { Owner(let value: Int64, let other: UInt32,) {} }" to
                listOf("let value: Int64", "let other: UInt32"),
            "class Owner { init(value: Int64,) {} }" to listOf("value: Int64"),
            "type Callback = (Int64, UInt32,) -> Unit" to listOf("Int64", "UInt32"),
            "type Callback = CFunc<(Int64,) -> Unit>" to listOf("Int64"),
        )
        for ((index, case) in cases.withIndex()) {
            val (text, parameters) = case
            val result = parseBoth("trailingComma$index", text)
            kotlin.test.assertEquals(emptyList<ParsingDiagnostic>(), result.diagnostics, text)
            kotlin.test.assertEquals(listOf(parameters), result.parameterLists.map { it.parameters }, text)
        }
    }

    /** 空列表和仅有尾逗号的非空列表不同；空列表仍有零个真实参数。 */
    @Test
    fun testEmptyParameterListsRemainEmptyInBothParsers() {
        for ((index, text) in listOf(
            "func ordinary(): Unit {}",
            "foreign func imported(): Unit",
            "class Owner { Owner() {} }",
            "class Owner { init() {} }",
            "type Callback = () -> Unit",
            "type Callback = CFunc<() -> Unit>",
        ).withIndex()) {
            val result = parseBoth("emptyParameters$index", text)
            kotlin.test.assertEquals(emptyList<ParsingDiagnostic>(), result.diagnostics, text)
            kotlin.test.assertEquals(listOf(emptyList<String>()), result.parameterLists.map { it.parameters }, text)
        }
    }

    /** 前导逗号、连续逗号和只有逗号的列表仍非法，不约束未取证的错误恢复条数。 */
    @Test
    fun testLeadingAndRepeatedCommasRemainErrorsInBothParsers() {
        val declarations = listOf<(String) -> String>(
            { parameters -> "func ordinary($parameters): Unit {}" },
            { parameters -> "foreign func imported($parameters): Unit" },
            { parameters -> "class Owner { Owner($parameters) {} }" },
            { parameters -> "class Owner { init($parameters) {} }" },
        )
        var index = 0
        for (declaration in declarations) {
            for (parameters in listOf(", value: Int64", "value: Int64,, other: UInt32", ",")) {
                val text = declaration(parameters)
                kotlin.test.assertTrue(parseBoth("invalidParameterComma${index++}", text).diagnostics.isNotEmpty(), text)
            }
        }
        for (parameters in listOf(", Int64", "Int64,, UInt32", ",")) {
            val text = "type Callback = ($parameters) -> Unit"
            kotlin.test.assertTrue(parseBoth("invalidTypeParameterComma${index++}", text).diagnostics.isNotEmpty(), text)
        }
    }

    private data class ParameterListSnapshot(val start: Int, val end: Int, val parameters: List<String>)

    private data class ParsedParameters(
        val diagnostics: List<ParsingDiagnostic>,
        val parameterLists: List<ParameterListSnapshot>,
    )

    /** 同时比较诊断和直接参数节点，防止只消掉错误却保留虚假参数。 */
    private fun parseBoth(name: String, text: String): ParsedParameters {
        val file = createPsiFile(name, text) as CjFile
        val psiDiagnostics = file.collectParsingDiagnostics()
        val psiLists = PsiTreeUtil.findChildrenOfType(file, CjParameterList::class.java).map { list ->
            ParameterListSnapshot(list.textRange.startOffset, list.textRange.endOffset, list.parameters.map { it.text })
        }.sortedBy { it.start }

        val lightDiagnostics = mutableListOf<ParsingDiagnostic>()
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
        val lightResult = parseLightWithPsiInput(file, builder)
        lightDiagnostics += lightResult.diagnostics
        val tree = lightResult.file.tree
        val lightLists = mutableListOf<ParameterListSnapshot>()
        collectParameterLists(tree.root, tree, text, lightLists)

        assertPublishedParsingDiagnosticParity(psiDiagnostics, lightDiagnostics,
            checkNotNull(publishedParserSeed(file).sourceContext), text)
        kotlin.test.assertEquals(psiLists, lightLists.sortedBy { it.start }, text)
        return ParsedParameters(psiDiagnostics, psiLists)
    }

    /** 读取真实 LightTree 的直接参数；不把类型中的嵌套参数提升到外层列表。 */
    private fun collectParameterLists(
        node: LighterASTNode,
        tree: FlyweightCapableTreeStructure<LighterASTNode>,
        text: String,
        result: MutableList<ParameterListSnapshot>,
    ) {
        val ref = Ref<Array<LighterASTNode?>>()
        val count = tree.getChildren(node, ref)
        val children = ref.get() ?: return
        try {
            if (node.tokenType == CjStubElementTypes.VALUE_PARAMETER_LIST) {
                val parameters = (0 until count).mapNotNull { index ->
                    val child = checkNotNull(children[index])
                    if (child.tokenType == CjStubElementTypes.VALUE_PARAMETER) {
                        text.substring(child.startOffset, child.endOffset)
                    } else null
                }
                result += ParameterListSnapshot(node.startOffset, node.endOffset, parameters)
            }
            for (index in 0 until count) collectParameterLists(checkNotNull(children[index]), tree, text, result)
        } finally {
            tree.disposeChildren(children, count)
        }
    }
}

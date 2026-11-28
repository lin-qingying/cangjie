package org.cangnova.cangjie.psi

import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.messages.ParsingDiagnosticHint
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.parsing.CjmpParsingContext
import org.cangnova.cangjie.parsing.cjmpParsingContext
import org.cangnova.cangjie.parsing.collectParsingDiagnosticPublication
import org.cangnova.cangjie.parsing.assertLegacyParsingDiagnostics
import org.cangnova.cangjie.parsing.assertPublishedParsingDiagnosticParity
import org.cangnova.cangjie.parsing.parseLightWithPsiInput
import org.cangnova.cangjie.parsing.publishedParserSeed
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals as assertValueEquals
import kotlin.test.assertTrue as assertTrueValue

/** 固定官方 v1.1.3 ParseDecl.cpp:2414–2600 的参数规则；IDE 诊断覆盖完整 token。 */
class ParameterModifierDiagnosticsParsingTest : CjParsingTestCase(
    dataPath = "", fileExt = "cj", fileType = CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    @Test
    fun constAndDeclarationKeywordsHaveDifferentRulesInEachParameterOwner() {
        for (role in Role.entries) {
            // ParseLetOrVarInParamList 先报 const scope；同范围的后置 no-modifier 不进入 final。
            assertCase(role, "const value: Int32", listOf(constDeclaration(role)))
            for (keyword in listOf("let", "var")) {
                assertCase(role, "$keyword value: Int32", if (role.isPrimary) emptyList() else listOf(expectedParameter(keyword)))
                assertCase(
                    role, "const $keyword value: Int32",
                    if (role.isPrimary) listOf(constOnMember()) else listOf(noModifier("const"), expectedParameter(keyword)),
                )
            }
            assertCase(role, "public value: Int32", listOf(noModifier("public")))
            assertCase(role, "inout value: Int32", listOf(noModifier("inout")))
        }
    }

    /** Modifier::operator< 按 TokenKind 排序；no-modifier 的锚点不是源码第一个修饰符。 */
    @Test
    fun nonMemberModifierPriorityIsIndependentOfSourceOrder() {
        for (role in Role.entries) {
            assertCase(role, "private public value: Int32", listOf(noModifier("public")))
            assertCase(role, "public private value: Int32", listOf(noModifier("public")))
            assertCase(role, "const public value: Int32", listOf(noModifier("const")))
            // 仅尾 token 为 CONST 才有前置 scope 报告；const public 的期望不能照此裁剪。
            assertCase(role, "public const value: Int32", listOf(constDeclaration(role)))
            assertCase(role, "inout static value: Int32", listOf(noModifier("static")))
        }
    }

    /** 只有参数自身缺 name/colon 才置 HAS_BROKEN；const-only 的前置诊断仍保留。 */
    @Test
    fun missingNameOrColonSuppressesOnlyNonMemberModifierDiagnostics() {
        for (role in Role.entries) {
            for (parameter in listOf("const : Int32", "const value")) {
                assertCase(role, parameter, listOf(constDeclaration(role)), hasOtherSyntaxError = true)
            }
            for (parameter in listOf("inout : Int32", "inout value")) {
                assertCase(role, parameter, emptyList(), hasOtherSyntaxError = true)
            }
        }
        for (role in Role.entries.filter { it.isPrimary }) {
            for (parameter in listOf("const let : Int32", "const let value")) {
                assertCase(role, parameter, listOf(constOnMember()), hasOtherSyntaxError = true)
            }
        }
    }

    /** 无效 type/default 不跳过修饰符 producer；尾 CONST 的 final 同范围赢家仍为前置 scope 错误。 */
    @Test
    fun missingTypeOrInvalidDefaultDoesNotSuppressModifierDiagnostics() {
        for (role in Role.entries) {
            for (parameter in listOf("public value: ", "public value!: Int32 = ", "public value: Int32 = 1")) {
                assertCase(role, parameter, listOf(noModifier("public")), hasOtherSyntaxError = true)
            }
            for (parameter in listOf("const value: ", "const value!: Int32 = ")) {
                assertCase(role, parameter, listOf(constDeclaration(role)), hasOtherSyntaxError = true)
            }
        }
    }

    @Test
    fun primaryMembersCheckAllModifierKindsAndAccessConflicts() {
        for (role in Role.entries.filter { it.isPrimary }) {
            for (access in ACCESS_MODIFIERS) {
                assertCase(role, "$access let value: Int32", emptyList())
                assertCase(role, "$access const var value: Int32", listOf(constOnMember()))
            }
            for (modifier in MODIFIERS.filter { it != "const" && it !in ACCESS_MODIFIERS }) {
                assertCase(role, "$modifier let value: Int32", listOf(illegalMemberModifier(modifier)))
            }
            assertCase(role, "public private let value: Int32", listOf(conflicting("private", "public")))
            assertCase(role, "private public let value: Int32", listOf(conflicting("public", "private")))
            // ACCESS_PROTECTED 的冲突数组先 PUBLIC 后 PRIVATE；同范围仅发布 protected/public。
            assertCase(role, "private public protected let value: Int32", listOf(
                conflicting("public", "private"), conflicting("protected", "public"),
            ))
            assertCase(role, "static public let value: Int32", listOf(illegalMemberModifier("static")))
        }
    }

    /** 注释和空白不能扩宽 token 诊断；重复修饰符仅第一次参加后置检查。 */
    @Test
    fun modifierDiagnosticsKeepFullTokenRangesAcrossTriviaAndDuplicates() {
        for (role in Role.entries) {
            assertCase(role, "public /* gap */ const\n  value: Int32", listOf(constDeclaration(role)))
            assertCase(role, "const /* gap */ const value: Int32", listOf(
                noModifier("const"), duplicate("const", 1),
            ))
            assertCase(role, "public /* gap */ public value: Int32", listOf(noModifier("public"), duplicate("public", 1)))
            assertCase(
                role, "const /* gap */ let value: Int32",
                if (role.isPrimary) listOf(constOnMember()) else listOf(noModifier("const"), expectedParameter("let")),
            )
        }
        for (role in Role.entries.filter { it.isPrimary }) {
            assertCase(role, "private /* gap */ private public let value: Int32", listOf(
                duplicate("private", 1), conflicting("public", "private"),
            ))
            assertCase(role, "static /* gap */ static let value: Int32", listOf(
                illegalMemberModifier("static"), duplicate("static", 1),
            ))
        }
    }

    /** PUBLIC 等软关键字后接 ':'/'!' 是参数名；INOUT 是硬修饰符，已经由缺名组覆盖。 */
    @Test
    fun contextualKeywordsFollowedByColonOrNamedMarkRemainParameterNames() {
        for (role in Role.entries) {
            for (name in listOf("public", "private", "open", "common", "specific")) {
                assertCase(role, "$name : Int32", emptyList(), expectedFirstName = name)
                assertCase(role, "$name!: Int32 = 1", emptyList(), expectedFirstName = name)
            }
        }
    }

    @Test
    fun cjmpFileRoleFailurePrecedesGeneralPrimaryMemberModifierChecks() {
        for (role in Role.entries.filter { it.isPrimary }) {
            for (modifier in listOf("common", "specific")) {
                val key = if (modifier == "common") "common.in.non.common.file" else "specific.in.non.specific.file"
                val denied = ParsingDiagnostic(0, 0, CangJieParsingBundle.message("parsing.error.cjmp.$key"), ParsingDiagnostic.Origin.ORIGINAL_FILE)
                assertCase(
                    role, "$modifier let value: Int32", emptyList(),
                    context = CjmpParsingContext.Compilation(false, false, true), additionalExpected = listOf(denied),
                )
                assertCase(
                    role, "const $modifier let value: Int32", listOf(constOnMember()),
                    context = CjmpParsingContext.Compilation(false, false, true), additionalExpected = listOf(denied),
                )
                assertCase(
                    role, "$modifier let value: Int32", listOf(illegalMemberModifier(modifier)),
                    context = CjmpParsingContext.Compilation(
                        modifier == "common", modifier == "specific", true, parameterRoleMatching = true,
                    ),
                    additionalExpected = listOf(
                        primaryMemberParameterMismatch(role, modifier, "$modifier let value: Int32"),
                    ),
                )
            }
        }
    }

    @Test
    fun ordinaryFunctionAndSecondaryConstructorParametersKeepTheirSyntaxModifierError() {
        for (role in listOf(Role.ORDINARY, Role.SECONDARY)) {
            for (modifier in listOf("common", "specific")) {
                val context = CjmpParsingContext.Compilation(
                    compileCommon = modifier == "common",
                    compileSpecific = modifier == "specific",
                    enabled = true,
                    parameterRoleMatching = true,
                )
                assertCase(role, "$modifier value: Int32", listOf(noModifier(modifier)), context = context)
            }
        }
    }

    private fun assertCase(
        role: Role,
        parameter: String,
        expected: List<Expected>,
        hasOtherSyntaxError: Boolean = false,
        expectedFirstName: String? = null,
        context: CjmpParsingContext = CjmpParsingContext.SyntaxOnly,
        additionalExpected: List<ParsingDiagnostic> = emptyList(),
    ) {
        val following = if (role.isPrimary) "let after!: Int32 = 2" else "after!: Int32 = 2"
        val parameters = "$parameter, $following"
        val text = sourceText(role, parameters)
        val file = createPsiFile("parameterModifiers${role.name}", text) as CjFile
        file.cjmpParsingContext = context
        val psiPublication = file.collectParsingDiagnosticPublication()
        val psiDiagnostics = psiPublication.diagnostics
        val psiLists = PsiTreeUtil.findChildrenOfType(file, CjParameterList::class.java).toList()
        assertValueEquals(1, psiLists.size, text)
        val psiParameters = psiLists.single().parameters
        assertValueEquals(2, psiParameters.size, text)
        assertValueEquals("after", psiParameters.last().name, text)
        assertValueEquals("Int32", psiParameters.last().typeReference?.text, text)
        assertValueEquals("2", psiParameters.last().defaultValue?.text, text)
        if (expectedFirstName != null) assertValueEquals(expectedFirstName, psiParameters.first().name, text)
        val psiBodies = PsiTreeUtil.findChildrenOfType(file, CjBlockExpression::class.java).map { it.text }
        assertValueEquals(listOf("{ after }"), psiBodies, text)

        val lightDiagnostics = mutableListOf<ParsingDiagnostic>()
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
        builder.setDebugMode(true)
        val lightResult = parseLightWithPsiInput(file, builder)
        lightDiagnostics += lightResult.diagnostics
        val tree = lightResult.file.tree
        val lightParameters = mutableListOf<List<String>>()
        val lightBodies = mutableListOf<String>()
        collectStructure(tree.root, tree, text, lightParameters, lightBodies)
        assertValueEquals(listOf(psiParameters.map { it.text }), lightParameters, text)
        assertValueEquals(psiBodies, lightBodies, text)
        val sourceContext = checkNotNull(publishedParserSeed(file).sourceContext)
        assertPublishedParsingDiagnosticParity(sorted(psiDiagnostics), sorted(lightDiagnostics), sourceContext, text)

        val parameterStart = text.indexOf(parameter)
        val expectedDiagnostics = expected.map { expectation ->
            val match = Regex("\\b${Regex.escape(expectation.token)}\\b").findAll(parameter).elementAt(expectation.occurrence)
            val start = parameterStart + match.range.first
            val hints = expectation.hints.map { hint ->
                val hintMatch = Regex("\\b${Regex.escape(hint.token)}\\b").findAll(parameter).elementAt(hint.occurrence)
                val hintStart = parameterStart + hintMatch.range.first
                ParsingDiagnosticHint(hintStart, hintStart + hint.token.length, hint.label)
            }
            ParsingDiagnostic(start, start + expectation.token.length, expectation.message, hints = hints)
        } + additionalExpected
        if (hasOtherSyntaxError) {
            val modifierDiagnostics = psiDiagnostics.filter { it.message in parameterDiagnosticMessages }
            assertLegacyParsingDiagnostics(sorted(expectedDiagnostics), sorted(modifierDiagnostics), sourceContext, text)
            assertLegacyParsingDiagnostics(sorted(expectedDiagnostics),
                sorted(lightDiagnostics.filter { it.message in parameterDiagnosticMessages }), sourceContext, text)
            assertTrueValue(psiDiagnostics.any { it.message !in parameterDiagnosticMessages }, "Expected the malformed name/type/default to remain diagnosed: $text")
        } else {
            assertLegacyParsingDiagnostics(sorted(expectedDiagnostics), sorted(psiDiagnostics), sourceContext, text)
            assertLegacyParsingDiagnostics(sorted(expectedDiagnostics), sorted(lightDiagnostics), sourceContext, text)
        }
    }

    /** 读取真实轻树结构，确保 token 级错误包装没有吞掉后续参数或函数体。 */
    private fun collectStructure(
        node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>, text: String,
        parameters: MutableList<List<String>>, bodies: MutableList<String>,
    ) {
        val ref = Ref<Array<LighterASTNode?>>()
        val count = tree.getChildren(node, ref)
        val children = ref.get() ?: return
        try {
            if (node.tokenType == CjStubElementTypes.VALUE_PARAMETER_LIST) {
                parameters += (0 until count).mapNotNull { index ->
                    val child = requireNotNull(children[index])
                    if (child.tokenType == CjStubElementTypes.VALUE_PARAMETER) text.substring(child.startOffset, child.endOffset) else null
                }
            }
            if (node.tokenType == CjNodeTypes.BLOCK) bodies += text.substring(node.startOffset, node.endOffset)
            for (index in 0 until count) collectStructure(requireNotNull(children[index]), tree, text, parameters, bodies)
        } finally {
            tree.disposeChildren(children, count)
        }
    }

    private fun sorted(diagnostics: List<ParsingDiagnostic>): List<ParsingDiagnostic> =
        diagnostics.sortedWith(compareBy({ it.startOffset }, { it.endOffset }, { it.message }))

    private fun primaryMemberParameterMismatch(role: Role, modifier: String, parameter: String): ParsingDiagnostic {
        val text = sourceText(role, "$parameter, let after!: Int32 = 2")
        val start = text.indexOf(parameter)
        return ParsingDiagnostic(
            start,
            start + parameter.length,
            "parameter value is $modifier, but primary constructor is not $modifier",
        )
    }

    private fun sourceText(role: Role, parameters: String): String = when (role) {
        Role.PRIMARY_CLASS -> "class Owner { Owner($parameters) { after } }"
        Role.PRIMARY_STRUCT -> "struct Owner { Owner($parameters) { after } }"
        Role.SECONDARY -> "class Owner { init($parameters) { after } }"
        Role.ORDINARY -> "func target($parameters): Unit { after }"
    }

    private fun noModifier(token: String) = diagnostic("no.modifier", token, token)
    private fun expectedParameter(token: String) = diagnostic("expected.name.or.rparen", token, token)
    private fun constOnMember() = diagnostic("const.on.let.var", "const")
    private fun illegalMemberModifier(token: String) = diagnostic("illegal.member.modifier", token, token)
    /** ParserDiag.cpp:557–603：先提示此前保留的 modifier，再提示该成员参数自己的 let/var。 */
    private fun conflicting(token: String, previous: String, declarationKeyword: String = "let") =
        diagnostic("conflicting.member.modifiers", token, token, previous).copy(hints = listOf(
            ExpectedHint(previous, 0, CangJieParsingBundle.message("parsing.error.hint.conflicted.modifier")),
            ExpectedHint(declarationKeyword, 0, CangJieParsingBundle.message("parsing.error.hint.on", "variable declaration")),
        ))
    private fun duplicate(token: String, occurrence: Int) = diagnostic("duplicate.modifier", token, token).copy(occurrence = occurrence)
    private fun constDeclaration(role: Role, occurrence: Int = 0) = diagnostic(
        "const.declaration", "const",
        if (role.isPrimary) "const member variable declaration" else "const variable declaration",
        if (role.isPrimary) "primary constructor paramlist" else "paramlist",
    ).copy(occurrence = occurrence)

    private fun diagnostic(key: String, token: String, vararg arguments: Any): Expected =
        Expected(token, 0, CangJieParsingBundle.message("parsing.error.parameter.$key", *arguments))

    /** 仅在另有 name/type/default 语法错误的恢复组按已登记诊断族比较，不猜其错误恢复条数。 */
    private val parameterDiagnosticMessages: Set<String> by lazy {
        buildSet {
            add(constOnMember().message)
            Role.entries.forEach { add(constDeclaration(it).message) }
            MODIFIERS.forEach { token ->
                add(noModifier(token).message)
                add(illegalMemberModifier(token).message)
                add(duplicate(token, 0).message)
                MODIFIERS.forEach { previous -> add(conflicting(token, previous).message) }
            }
            listOf("let", "var").forEach { add(expectedParameter(it).message) }
        }
    }

    private data class Expected(
        val token: String,
        val occurrence: Int,
        val message: String,
        val hints: List<ExpectedHint> = emptyList(),
    )
    private data class ExpectedHint(val token: String, val occurrence: Int, val label: String)
    private enum class Role(val isPrimary: Boolean) {
        PRIMARY_CLASS(true), PRIMARY_STRUCT(true), ORDINARY(false), SECONDARY(false),
    }

    private companion object {
        val ACCESS_MODIFIERS = listOf("public", "private", "internal", "protected")
        val MODIFIERS = listOf("const", "static", "public", "private", "internal", "protected", "override", "redef",
            "abstract", "sealed", "open", "foreign", "inout", "mut", "unsafe", "operator", "common", "specific")
    }
}

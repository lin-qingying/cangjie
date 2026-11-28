package org.cangnova.cangjie.parsing

import com.intellij.lang.ASTNode
import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.CjParsingDiagnosticRange
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjMacroDeclaration
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.CjPrimaryConstructor
import org.cangnova.cangjie.psi.CjSecondaryConstructor
import org.cangnova.cangjie.psi.psiUtil.parsedDeclarationModifiersOrNull
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals as equal
import kotlin.test.assertIs
import kotlin.test.assertNotNull as present
import kotlin.test.assertSame as identical
import kotlin.test.assertTrue as truth

/** 官方 v1.1.3 ParseDecl.cpp 的完整列表边界、真实构造器身份及诊断坐标。 */
class CjVariadicDeclarationParsingTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun ordinaryFunctionsAndSecondaryConstructorsReportOnlyTheFirstEllipsis() {
        for (kind in listOf(Kind.FUNCTION, Kind.SECONDARY)) {
            for (owner in if (kind == Kind.SECONDARY) listOf("class", "struct") else listOf("class")) {
                check(kind, listOf(listOf("value: Int64", "...", "...")), owner = owner,
                    expected = listOf(nonForeign(kind)))
                check(kind, listOf(listOf("...")), owner = owner, expected = listOf(nonForeign(kind)))
            }
        }
    }

    @Test
    fun validForeignTailsRemainInSyntaxForTheRawProjection() {
        check(Kind.FUNCTION, listOf(listOf("first: Int64", "second: Int64", "...")), prefix = "foreign ")
        for (owner in listOf("class", "struct")) {
            check(Kind.SECONDARY, listOf(listOf("value: Int64", "...")), prefix = "foreign ", owner = owner,
                effectiveForeign = true)
        }
    }

    @Test
    fun foreignFirstAndMiddleErrorsKeepTheWholeListAndTheLaterLegalTail() {
        for (kind in listOf(Kind.FUNCTION, Kind.SECONDARY)) {
            check(kind, listOf(listOf("...")), prefix = "foreign ",
                expected = listOf(positionError(0, FIRST)), effectiveForeign = true)
            check(kind, listOf(listOf("first: Int64", "...", "last: Int64")), prefix = "foreign ",
                expected = listOf(positionError(0, NOT_LAST)), effectiveForeign = true)
            check(kind, listOf(listOf("...", "value: Int64", "...", "...")), prefix = "foreign ",
                expected = listOf(positionError(0, FIRST), positionError(1, NOT_LAST)), effectiveForeign = true)
        }
    }

    /** Owner(...) 才是 primary；func Owner(...) 仍是普通函数。 */
    @Test
    fun primaryConstructorsAlwaysUseTheNonForeignBranch() {
        for (owner in listOf("class", "struct")) {
            for (prefix in listOf("", "foreign ")) {
                check(Kind.PRIMARY, listOf(listOf("value: Int64", "...", "...")), prefix = prefix, owner = owner,
                    expected = listOf(nonForeign(Kind.PRIMARY)))
            }
        }
        val text = "class Owner { func Owner(value: Int64, ...) {} }"
        val parsed = parseBoth(text)
        equal(emptyList<CjPrimaryConstructor>(), PsiTreeUtil.findChildrenOfType(parsed.file, CjPrimaryConstructor::class.java).toList())
        val function = PsiTreeUtil.findChildrenOfType(parsed.file, CjNamedFunction::class.java).single()
        equal("Owner", function.name)
        assertParameters(function, listOf(listOf("value: Int64", "...")), text)
        val diagnostic = parsed.diagnostics.filter(::isVariadic).single()
        equal(Triple(14, 24, message(NON_FOREIGN)),
            Triple(diagnostic.startOffset, diagnostic.endOffset, diagnostic.message))
        val input = present(present(parsed.file.getCopyableUserData(ROOT_PARSER_SEED_KEY)).lexicalInput)
        equal(CjParsingDiagnosticRange(input.source, 1 to 15, 1 to 25), present(diagnostic.mainRange))
    }

    /** 多列表是恢复输入；普通函数仍可报告 currying 错误，但不可再逐列表检查省略号。 */
    @Test
    fun everyCompleteParameterListSurvivesWithoutVariadicDiagnostics() {
        val lists = listOf(listOf("first: Int64", "..."), listOf("...", "second: Int64"), listOf("last: Int64", "..."))
        for (kind in listOf(Kind.FUNCTION, Kind.PRIMARY, Kind.SECONDARY)) {
            for (prefix in listOf("", "foreign ")) {
                for (count in listOf(2, 3)) {
                    check(kind, lists.take(count), prefix = prefix,
                        effectiveForeign = kind != Kind.PRIMARY && prefix.isNotEmpty())
                }
            }
        }
    }

    @Test
    fun macroDefinitionsNeverRunTheForeignVariadicCheck() {
        check(Kind.MACRO, listOf(listOf("input: Tokens", "...")))
        check(Kind.MACRO, listOf(listOf("...")))
    }

    /** 文件角色拒绝清空返回属性；源代码仍含 foreign 也必须报告非 FOREIGN。 */
    @Test
    fun rejectedFunctionAndSecondaryRolesDoNotGrantForeignFromThePrefix() {
        val common = CjmpParsingContext.Compilation(true, false, true)
        for (kind in listOf(Kind.FUNCTION, Kind.SECONDARY)) {
            check(kind, listOf(listOf("value: Int64", "...")), prefix = "specific foreign ",
                context = common, expected = listOf(nonForeign(kind)), effectiveForeign = false)
        }
    }

    private fun check(
        kind: Kind,
        lists: List<List<String>>,
        prefix: String = "",
        owner: String = "class",
        context: CjmpParsingContext = CjmpParsingContext.SyntaxOnly,
        expected: List<(String) -> ParsingDiagnostic> = emptyList(),
        effectiveForeign: Boolean = prefix.isNotEmpty(),
    ) {
        val signature = lists.joinToString("") { it.joinToString(", ", "(", ")") }
        val member = when (kind) {
            Kind.FUNCTION -> prefix + "func target" + signature + ": Unit" + if (prefix.isEmpty()) " {}" else ""
            Kind.PRIMARY -> prefix + "Owner" + signature + " {}"
            Kind.SECONDARY -> prefix + "init" + signature + " {}"
            Kind.MACRO -> "public macro target" + signature + ": Tokens { input }"
        }
        val text = when (kind) {
            Kind.FUNCTION -> member + "\nfunc following(): Unit {}"
            Kind.MACRO -> "macro package variadic_tests\n" + member + "\nfunc following(): Unit {}"
            else -> "$owner Owner {\n$member\nfunc afterMember(): Unit {}\n}\nfunc following(): Unit {}"
        }
        val parsed = parseBoth(text, context)
        val declaration: CjCallableDeclaration = when (kind) {
            Kind.FUNCTION -> PsiTreeUtil.findChildrenOfType(parsed.file, CjNamedFunction::class.java).single { it.name == "target" }
            Kind.PRIMARY -> assertIs<CjPrimaryConstructor>(
                PsiTreeUtil.findChildrenOfType(parsed.file, CjPrimaryConstructor::class.java).single(), text)
            Kind.SECONDARY -> assertIs<CjSecondaryConstructor>(
                PsiTreeUtil.findChildrenOfType(parsed.file, CjSecondaryConstructor::class.java).single(), text)
            Kind.MACRO -> PsiTreeUtil.findChildrenOfType(parsed.file, CjMacroDeclaration::class.java).single()
        }
        assertParameters(declaration, lists, text)
        equal(expected.map { it(text) }.map { Triple(it.startOffset, it.endOffset, it.message) },
            parsed.diagnostics.filter(::isVariadic).map { Triple(it.startOffset, it.endOffset, it.message) }, text)
        if (kind == Kind.FUNCTION || kind == Kind.SECONDARY) {
            val modifiers = present(declaration.parsedDeclarationModifiersOrNull(), text)
            equal(effectiveForeign, CjModifierKind.FOREIGN in modifiers.attributes, text)
        }
        truth(PsiTreeUtil.findChildrenOfType(parsed.file, CjNamedFunction::class.java).any { it.name == "following" }, text)
        if (kind == Kind.PRIMARY || kind == Kind.SECONDARY) {
            truth(PsiTreeUtil.findChildrenOfType(parsed.file, CjNamedFunction::class.java).any { it.name == "afterMember" }, text)
        }
    }

    /** PSI 保留源码中的省略号和所有列表，删除仅属于 raw 参数投影。 */
    private fun assertParameters(declaration: CjCallableDeclaration, expected: List<List<String>>, text: String) {
        val lists = declaration.valueParameterLists
        equal(expected.map { it.joinToString(", ", "(", ")") }, lists.map { it.text }, text)
        equal(expected, lists.map { list -> list.parameters.map { it.text } }, text)
        for (list in lists) {
            identical(declaration, list.parent, text)
            for (parameter in list.parameters) identical(list, parameter.parent, text)
        }
    }

    private fun parseBoth(text: String, context: CjmpParsingContext = CjmpParsingContext.SyntaxOnly): Parsed {
        val file = createPsiFile("variadicDeclarations", text) as CjFile
        file.cjmpParsingContext = context
        val diagnostics = file.collectParsingDiagnostics()
        val psi = mutableListOf<Node>()
        fun visitPsi(node: ASTNode) {
            if (node.elementType in SIGNATURE_NODES) psi += Node(node.elementType, node.startOffset, node.startOffset + node.textLength)
            generateSequence(node.firstChildNode) { it.treeNext }.forEach(::visitPsi)
        }
        visitPsi(file.node)
        val lightDiagnostics = mutableListOf<ParsingDiagnostic>()
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
        builder.setDebugMode(true)
        // 两条前端消费同一份实际冻结输入，规范范围不能借两个匿名 namespace 判成相同来源。
        val seed = present(file.getCopyableUserData(ROOT_PARSER_SEED_KEY))
        val parsedLight = CangJieLightParser.parseBuffered(builder, cjmpContext = context,
            lexicalInput = present(seed.lexicalInput))
        lightDiagnostics.addAll(parsedLight.diagnostics)
        val tree = parsedLight.file.tree
        val light = mutableListOf<Node>()
        fun visitLight(node: LighterASTNode) {
            if (node.tokenType in SIGNATURE_NODES) light += Node(node.tokenType, node.startOffset, node.endOffset)
            val ref = Ref<Array<LighterASTNode?>>()
            val count = tree.getChildren(node, ref)
            val children = ref.get()
            if (children != null) {
                try { for (i in 0 until count) visitLight(requireNotNull(children[i])) }
                finally { tree.disposeChildren(children, count) }
            }
        }
        visitLight(tree.root)
        equal(psi, light, text)
        equal(diagnostics, lightDiagnostics, text)
        return Parsed(file, diagnostics)
    }

    private fun nonForeign(kind: Kind): (String) -> ParsingDiagnostic = { text ->
        val token = when (kind) {
            Kind.FUNCTION -> "func target"
            Kind.PRIMARY -> "Owner("
            Kind.SECONDARY -> "init("
            Kind.MACRO -> error("宏定义不执行此检查")
        }
        val start = text.indexOf(token)
        val length = if (kind == Kind.FUNCTION) token.length else token.length - 1
        ParsingDiagnostic(start, start + length, message(NON_FOREIGN))
    }

    /** ParseEllipsisParameter 先 Next，再保存 lookahead 的 begin/end；不是省略号 token 本身。 */
    private fun positionError(ordinal: Int, key: String): (String) -> ParsingDiagnostic = { text ->
        var start = -1
        repeat(ordinal + 1) { start = text.indexOf("...", start + 1) }
        require(start >= 0)
        start += 3
        while (text[start].isWhitespace()) start++
        ParsingDiagnostic(start, start + 1, message(key))
    }

    private fun isVariadic(diagnostic: ParsingDiagnostic): Boolean = diagnostic.message in
        listOf(NON_FOREIGN, FIRST, NOT_LAST).map(::message)

    private fun message(key: String): String = CangJieParsingBundle.message(key)
    private enum class Kind { FUNCTION, PRIMARY, SECONDARY, MACRO }
    private data class Node(val type: IElementType, val start: Int, val end: Int)
    private data class Parsed(val file: CjFile, val diagnostics: List<ParsingDiagnostic>)
    private companion object {
        const val NON_FOREIGN = "parsing.error.variadic.non.foreign"
        const val FIRST = "parsing.error.variadic.first.parameter"
        const val NOT_LAST = "parsing.error.variadic.not.last.parameter"
        val SIGNATURE_NODES = setOf(CjNodeTypes.FUNC, CjNodeTypes.PRIMARY_CONSTRUCTOR, CjNodeTypes.SECONDARY_CONSTRUCTOR,
            CjNodeTypes.MACRO, CjNodeTypes.VALUE_PARAMETER_LIST, CjNodeTypes.VALUE_PARAMETER)
    }
}

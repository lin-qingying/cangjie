package org.cangnova.cangjie.parsing

import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjPrimaryConstructor
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals as assertValueEquals
import kotlin.test.assertNotNull as assertPresent
import kotlin.test.assertTrue as assertTrueValue

/**
 * 官方 v1.1.3 ParserImpl.h 的 SeeingPrimaryConstructor 只接受 CLASS_BODY/STRUCT_BODY；
 * ParseMacroCallDeclInput 传递当前 scope，ParseFuncBody/ParseExpressionOrDeclarations 切换到函数作用域。
 * 本测试覆盖完整普通源码中的成员上下文边界，非法局部宏输入只要求不产生 primary 并保留可达语法。
 */
class MemberParsingContextIsolationTest : CjParsingTestCase(
    dataPath = "", fileExt = "cj", fileType = CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    @Test
    fun classAndStructMemberMacroInputsKeepPrimaryConstructorGrammar() {
        for (kind in OWNER_KINDS) {
            val text = source(kind, "@Member Owner() {}")
            val parsed = parseBoth("memberMacro" + kind, text)
            assertValueEquals(emptyList<ParsingDiagnostic>(), parsed.diagnostics, text)
            val macro = parsed.macro("Member")
            val constructor = macro.unwrappedDeclaration as? CjPrimaryConstructor
            assertPresent(constructor, text)
            assertValueEquals("Owner() {}", constructor.text, text)
            assertValueEquals(macro.input, constructor.parent, text)
            assertValueEquals(listOf(constructor), parsed.constructors, text)
            assertFollowingDeclarations(parsed)
        }
    }

    @Test
    fun functionBodiesDoNotInheritTheEnclosingPrimaryConstructorGrammar() {
        for (kind in OWNER_KINDS) {
            assertIsolated(kind, "func target(): Unit {\n" + LOCAL_STATEMENTS + "\n}", hasLocalStatement = true)
        }
    }

    @Test
    fun primaryAndSecondaryConstructorBodiesDoNotInheritMemberGrammar() {
        for (kind in OWNER_KINDS) {
            for (constructorName in listOf("Owner", "init")) {
                assertIsolated(
                    kind, constructorName + "() {\n" + LOCAL_STATEMENTS + "\n}",
                    enclosingPrimary = constructorName == "Owner", hasLocalStatement = true,
                )
            }
        }
    }

    @Test
    fun getterAndSetterBodiesDoNotInheritMemberGrammar() {
        for (kind in OWNER_KINDS) {
            for (accessor in listOf("get", "set")) {
                val getterBody = if (accessor == "get") LOCAL_STATEMENTS + "\n0" else "0"
                val setterBody = if (accessor == "set") LOCAL_STATEMENTS else ""
                assertIsolated(
                    kind,
                    "prop slot: Int64 {\nget() {\n" + getterBody + "\n}\nset(value) {\n" + setterBody + "\n}\n}",
                    hasLocalStatement = true,
                )
            }
        }
    }

    @Test
    fun lambdaBodiesDoNotInheritMemberGrammarFromFieldsOrFunctions() {
        for (kind in OWNER_KINDS) {
            val lambda = "{ =>\n" + LOCAL_STATEMENTS + "\n}"
            for (member in listOf("let action = " + lambda, "func target(): Unit { let action = " + lambda + " }")) {
                assertIsolated(kind, member, hasLocalStatement = true)
            }
        }
    }

    @Test
    fun fieldInitializerExpressionsDoNotInheritMemberGrammar() {
        for (kind in OWNER_KINDS) {
            assertIsolated(kind, "let value = @Local Owner() {}")
        }
    }

    @Test
    fun parameterDefaultExpressionsDoNotInheritMemberGrammar() {
        for (kind in OWNER_KINDS) {
            for (declaration in listOf("func target", "init", "Owner")) {
                val member = declaration + "(value!: Int64 = @Local Owner() {}) {}"
                val text = source(kind, member + "\n@AfterMember Owner() {}")
                val parsed = parseBoth("defaultExpression" + kind, text)
                val localMacro = parsed.macro("Local")
                assertTrueValue(PsiTreeUtil.findChildrenOfType(localMacro, CjPrimaryConstructor::class.java).isEmpty(), text)
                assertTrueValue(localMacro.unwrappedDeclaration !is CjPrimaryConstructor, text)
                val parameter = assertPresent(PsiTreeUtil.getParentOfType(localMacro, CjParameter::class.java), text)
                assertValueEquals("value", parameter.name, text)
                assertValueEquals(text.indexOf("value!"), parameter.textRange.startOffset, text)
                assertTrueValue(PsiTreeUtil.isAncestor(assertPresent(parameter.defaultValue), localMacro, false), text)
                // native 参数列表遇非逗号即结束，缺右括号时不会替非法默认值恢复整个外层构造器。
                // 留下的 Owner(){} 还可成为后续成员；这里只约束默认值内部不使用成员 primary 文法。
                val restored = assertPresent(parsed.macro("AfterMember").unwrappedDeclaration as? CjPrimaryConstructor, text)
                assertValueEquals("Owner() {}", restored.text, text)
                assertFollowingDeclarations(parsed)
            }
        }
    }

    /** 每个隔离案例之后再次解析成员宏，验证退出局部作用域后恢复原来的成员文法。 */
    private fun assertIsolated(
        kind: String,
        member: String,
        enclosingPrimary: Boolean = false,
        hasLocalStatement: Boolean = false,
    ) {
        val text = source(kind, member + "\n@AfterMember Owner() {}")
        val parsed = parseBoth("isolatedMember" + kind, text)
        val localMacro = parsed.macro("Local")
        assertTrueValue(PsiTreeUtil.findChildrenOfType(localMacro, CjPrimaryConstructor::class.java).isEmpty(), text)
        assertTrueValue(localMacro.unwrappedDeclaration !is CjPrimaryConstructor, text)

        val restoredMacro = parsed.macro("AfterMember")
        val restoredPrimary = assertPresent(restoredMacro.unwrappedDeclaration as? CjPrimaryConstructor, text)
        assertValueEquals("Owner() {}", restoredPrimary.text, text)
        val expectedConstructors = if (enclosingPrimary) {
            val enclosing = parsed.constructors.single { it.textRange.startOffset == text.indexOf(member) }
            assertValueEquals(member, enclosing.text, text)
            assertTrueValue(PsiTreeUtil.isAncestor(enclosing, localMacro, true), text)
            listOf(enclosing, restoredPrimary)
        } else {
            listOf(restoredPrimary)
        }
        assertValueEquals(expectedConstructors, parsed.constructors, text)
        if (hasLocalStatement) {
            assertTrueValue(PsiTreeUtil.findChildrenOfType(parsed.file, CjPatternVariable::class.java).any {
                it.text == "let resumed = 1" && it.textRange.startOffset > localMacro.textRange.startOffset
            }, text)
        }
        assertFollowingDeclarations(parsed)
    }

    private fun assertFollowingDeclarations(parsed: Parsed) {
        val owner = parsed.file.declarations.filterIsInstance<CjTypeStatement>().single()
        assertValueEquals("Owner", owner.name, parsed.text)
        val followingMember = assertPresent(owner.body, parsed.text).declarations.filterIsInstance<CjNamedFunction>()
            .single { it.name == "afterMember" }
        assertValueEquals(parsed.text.indexOf("func afterMember"), followingMember.textRange.startOffset, parsed.text)
        val following = parsed.file.declarations.filterIsInstance<CjNamedFunction>().single { it.name == "following" }
        assertValueEquals(parsed.text.indexOf("func following"), following.textRange.startOffset, parsed.text)
    }

    private fun source(kind: String, member: String): String =
        kind + " Owner {\n" + member + "\nfunc afterMember(): Unit {}\n}\nfunc following(): Unit {}"

    /** PSI 与启用 marker 检查的 LightTree 必须具有相同诊断和所有 primary 节点的原始范围。 */
    private fun parseBoth(name: String, text: String): Parsed {
        val file = createPsiFile(name, text) as CjFile
        val diagnostics = file.collectParsingDiagnostics()
        val constructors = PsiTreeUtil.findChildrenOfType(file, CjPrimaryConstructor::class.java).sortedBy { it.textRange.startOffset }
        val psiNodes = constructors.map { PrimaryNode(it.textRange.startOffset, it.textRange.endOffset, it.text) }
        val lightDiagnostics = mutableListOf<ParsingDiagnostic>()
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
        builder.setDebugMode(true)
        val lightResult = parseLightWithPsiInput(file, builder)
        lightDiagnostics += lightResult.diagnostics
        val tree = lightResult.file.tree
        val lightNodes = mutableListOf<PrimaryNode>()
        collectPrimaryNodes(tree.root, tree, text, lightNodes)
        assertPublishedParsingDiagnosticParity(diagnostics, lightDiagnostics,
            checkNotNull(publishedParserSeed(file).sourceContext), text)
        assertValueEquals(psiNodes, lightNodes.sortedBy { it.start }, text)
        return Parsed(text, file, constructors, diagnostics)
    }

    private fun collectPrimaryNodes(
        node: LighterASTNode,
        tree: FlyweightCapableTreeStructure<LighterASTNode>,
        text: String,
        output: MutableList<PrimaryNode>,
    ) {
        if (node.tokenType == CjNodeTypes.PRIMARY_CONSTRUCTOR) {
            output += PrimaryNode(node.startOffset, node.endOffset, text.substring(node.startOffset, node.endOffset))
        }
        val ref = Ref<Array<LighterASTNode?>>()
        val count = tree.getChildren(node, ref)
        val children = ref.get() ?: return
        try {
            for (index in 0 until count) collectPrimaryNodes(requireNotNull(children[index]), tree, text, output)
        } finally {
            tree.disposeChildren(children, count)
        }
    }

    private data class Parsed(
        val text: String, val file: CjFile, val constructors: List<CjPrimaryConstructor>, val diagnostics: List<ParsingDiagnostic>,
    ) {
        fun macro(name: String): CjMacroExpression = PsiTreeUtil.findChildrenOfType(file, CjMacroExpression::class.java)
            .single { it.referenceExpression?.text == name }
    }

    private data class PrimaryNode(val start: Int, val end: Int, val text: String)

    private companion object {
        val OWNER_KINDS = listOf("class", "struct")
        const val LOCAL_STATEMENTS = "@Local Owner() {}\nlet resumed = 1"
    }
}

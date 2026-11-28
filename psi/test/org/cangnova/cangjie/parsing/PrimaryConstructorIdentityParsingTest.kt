package org.cangnova.cangjie.parsing

import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.stubs.elements.CjFileStubBuilder
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals as equal
import kotlin.test.assertNotNull as present
import kotlin.test.assertTrue as truth

/** 名字候选按官方 v1.1.3 原始 UTF-8 字节判定；直接成员收尾与宏 input 身份分别验收。 */
class PrimaryConstructorIdentityParsingTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun directMembersUseRawSpellingAndUtf8ByteDistance() {
        for (kind in listOf("class", "struct")) {
            for (case in listOf(
                Case("Owner", "Owner", Identity.PRIMARY),
                Case("Owner", "OwneX", Identity.INVALID),
                Case("Owner", "Different", Identity.RECOVERED),
                Case("features", "features", Identity.PRIMARY),
                Case("feature", "features", Identity.INVALID),
                Case("`Owner`", "`Owner`", Identity.PRIMARY),
                Case("`Owner`", "Owner", Identity.RECOVERED),
                Case("Owner", "`Owner`", Identity.RECOVERED),
                Case("名", "名", Identity.PRIMARY),
                Case("名", "同", Identity.INVALID),
                Case("名", "字", Identity.RECOVERED),
            )) {
                val member = case.candidate + SIGNATURE
                val text = source(kind, case.owner, member)
                val parsed = parseBoth(text)
                assertIdentity(parsed, member, case.identity)
                if (case.identity == Identity.PRIMARY) equal(emptyList<ParsingDiagnostic>(), parsed.diagnostics, text)
                if (case.identity == Identity.INVALID) assertNameDiagnostic(parsed, case.candidate, case.owner.removeSurrounding("`"))
                assertFollowing(parsed)
            }
        }
    }

    @Test
    fun nearNameInvalidCarrierRetainsSyntaxButDoesNotPersistItsSignature() {
        for (kind in listOf("class", "struct")) {
            val member = "OwneX<T>(value: T) where T <: Bound { let retained = 1 }"
            val parsed = parseBoth(source(kind, "Owner", member))
            assertIdentity(parsed, member, Identity.INVALID)
            val invalid = PsiTreeUtil.findChildrenOfType(parsed.file, CjInvalidDeclaration::class.java).single()
            equal(member, invalid.text)
            equal(listOf("value"), PsiTreeUtil.findChildrenOfType(invalid, CjParameter::class.java).map { it.name })
            truth(PsiTreeUtil.findChildrenOfType(invalid, CjBlockExpression::class.java).any { it.text == "{ let retained = 1 }" })
            val stub = CjFileStubBuilder().buildStubTree(parsed.file)
            fun invalidStubs(node: com.intellij.psi.stubs.StubElement<*>): List<com.intellij.psi.stubs.StubElement<*>> =
                (if (node.stubType == CjStubElementTypes.INVALID_DECLARATION) listOf(node) else emptyList()) +
                    node.childrenStubs.flatMap(::invalidStubs)
            val invalidStub = invalidStubs(stub).single()
            truth(invalidStub.psi is CjInvalidDeclaration)
            equal(emptyList<com.intellij.psi.stubs.StubElement<*>>(), invalidStub.childrenStubs,
                "错误声明保留 AST 签名，持久化 stub 不索引其内部声明")
            assertFollowing(parsed)
        }
    }

    @Test
    fun allFourNativeFollowTokensEnterNearNameRecovery() {
        for (tail in listOf(
            "(value: Int64) {}", "<T>(value: T) {}", "[1](value: Int64) {}", "{ let retained = 1 }",
        )) {
            val parsed = parseBoth(source("class", "Owner", "OwneX$tail"))
            assertNameDiagnostic(parsed, "OwneX", "Owner")
            truth(parsed.psiNodes.any { it.type == CjNodeTypes.INVALID_DECLARATION }, parsed.text)
            assertFollowing(parsed)
        }
    }

    @Test
    fun noParenthesesMacroInputsKeepTheirOwnPrimaryIdentity() {
        for (kind in listOf("class", "struct")) {
            for (prefix in listOf("@M ", "@Outer @Inner ")) {
                for (candidate in listOf("Owner", "OwneX", "Different")) {
                    val member = prefix + candidate + SIGNATURE
                    val parsed = parseBoth(source(kind, "Owner", member))
                    val primary = parsed.psiNodes.filter { it.type == CjNodeTypes.PRIMARY_CONSTRUCTOR }
                    if (candidate == "Different") {
                        equal(emptyList<Node>(), primary, parsed.text)
                        truth(parsed.diagnostics.any {
                            it.message == CangJieParsingBundle.message("parsing.error.macro.input.declaration.expected")
                        }, parsed.text)
                    } else {
                        equal(listOf(candidate + SIGNATURE), primary.map { it.text(parsed.text) }, parsed.text)
                        truth(parsed.psiNodes.none { it.type == CjNodeTypes.INVALID_DECLARATION }, parsed.text)
                        if (candidate == "OwneX") assertNameDiagnostic(parsed, candidate, "Owner")
                        else equal(emptyList<ParsingDiagnostic>(), parsed.diagnostics, parsed.text)
                    }
                    assertFollowing(parsed)
                }
            }
        }
    }

    @Test
    fun farNameRecoveryStopsAtMacroExtendAndInoutDeclarationStarts() {
        for ((declaration, type) in listOf(
            "macro preserved(input: Tokens): Tokens { input }" to CjNodeTypes.MACRO,
            "extend Owner { func preserved(): Unit {} }" to CjNodeTypes.EXTEND,
            "inout func preserved(): Unit {}" to CjNodeTypes.FUNC,
        )) {
            val parsed = parseBoth(source("class", "Owner", "Different(value: Int64) {}\n$declaration"))
            truth(parsed.psiNodes.any {
                it.type == type && it.text(parsed.text).contains("preserved")
            }, "远错名字恢复不得吞掉后续声明：${parsed.text}")
            equal(emptyList<Node>(), parsed.psiNodes.filter { it.type == CjNodeTypes.PRIMARY_CONSTRUCTOR })
            assertFollowing(parsed)
        }
    }

    @Test
    fun misplacedMacroDefinitionsRetainTheirSignatureBodyAndFollowingDeclarations() {
        val macro = "macro preserved(input: Tokens): Tokens { input }"
        val cases = listOf("class", "struct", "interface", "extend", "func", "")
        for (scope in cases) {
            val text = when (scope) {
                "" -> "macro package primary_identity\npublic $macro\nfunc afterMember(): Unit {}\nfunc following(): Unit {}"
                "func" -> "func target(): Unit {\n$macro\nfunc afterMember(): Unit {}\n}\nfunc following(): Unit {}"
                else -> source(scope, "Owner", macro)
            }
            val parsed = parseBoth(text)
            val macros = parsed.psiNodes.filter { it.type == CjNodeTypes.MACRO }
            equal(1, macros.size, "$text\n${parsed.diagnostics}")
            val node = macros.single()
            equal(if (scope.isEmpty()) "public $macro" else macro, node.text(text), text)
            val declaration = PsiTreeUtil.findChildrenOfType(parsed.file, CjMacroDeclaration::class.java).single()
            equal("preserved", declaration.name, text)
            equal(listOf("input"), declaration.valueParameters.map { it.name }, text)
            equal("Tokens", declaration.valueParameters.single().typeReference?.text, text)
            equal("Tokens", declaration.typeReference?.text, text)
            equal("{ input }", declaration.bodyExpression?.text, text)
            if (scope.isEmpty()) equal(emptyList<ParsingDiagnostic>(), parsed.diagnostics, text)
            else {
                val scopeMessage = CangJieParsingBundle.message(
                    "parsing.error.invalid.declaration.in.class", "macro", CangJieParsingBundle.message("parsing.scope.$scope"),
                )
                val start = text.indexOf("macro preserved")
                truth(parsed.diagnostics.any {
                    it.message == scopeMessage && it.startOffset == start && it.endOffset == start + "macro".length
                }, text)
            }
            assertFollowing(parsed)
        }
    }

    @Test
    fun leadingMacroDeclarationsAreNotConsumedAsPackageHeaders() {
        for (prefix in listOf("", "public ")) {
            val macro = "${prefix}macro preserved(input: Tokens): Tokens { input }"
            val parsed = parseBoth("$macro\nfunc afterMember(): Unit {}\nfunc following(): Unit {}")
            equal(listOf(macro), parsed.psiNodes.filter { it.type == CjNodeTypes.MACRO }.map { it.text(parsed.text) }, parsed.text)
            equal(null, parsed.file.packageDirective?.nameIdentifier, parsed.text)
            assertFollowing(parsed)
        }
    }

    @Test
    fun localDeclarationStartsReachSharedDeclarationParsing() {
        for ((declaration, type) in listOf(
            "struct Nested {}" to CjNodeTypes.STRUCT,
            "enum Nested { First | Second }" to CjNodeTypes.ENUM,
            "extend Owner {}" to CjNodeTypes.EXTEND,
            "type Nested = Int64" to null,
            "main() {}" to null,
        )) {
            val parsed = parseBoth("func target(): Unit {\n$declaration\nfunc afterMember(): Unit {}\n}\nfunc following(): Unit {}")
            if (type != null) truth(parsed.psiNodes.any { it.type == type }, parsed.text)
            truth(parsed.diagnostics.isNotEmpty(), parsed.text)
            assertFollowing(parsed)
        }
    }

    @Test
    fun missingNamesAndNonPrimaryScopesNeverInventPrimaryIdentity() {
        for (kind in listOf("class", "struct", "interface", "extend")) {
            for (owner in if (kind in listOf("class", "struct")) listOf("Owner", "") else listOf("Owner")) {
                val member = if (owner.isEmpty()) "Candidate$SIGNATURE" else "func Owner(value: Int64) {}"
                val parsed = parseBoth(source(kind, owner, member))
                equal(emptyList<Node>(), parsed.psiNodes.filter { it.type == CjNodeTypes.PRIMARY_CONSTRUCTOR }, parsed.text)
                if (owner.isEmpty()) truth(parsed.diagnostics.isNotEmpty(), parsed.text)
                assertFollowing(parsed)
            }
        }
        for (kind in listOf("interface", "extend")) {
            val parsed = parseBoth(source(kind, "Owner", "Owner$SIGNATURE"))
            equal(emptyList<Node>(), parsed.psiNodes.filter { it.type == CjNodeTypes.PRIMARY_CONSTRUCTOR }, parsed.text)
            truth(parsed.diagnostics.isNotEmpty(), parsed.text)
            assertFollowing(parsed)
        }
        val enum = parseBoth("enum Owner { Owner(Int64) | Another }\nfunc following(): Unit {}")
        equal(emptyList<Node>(), enum.psiNodes.filter { it.type == CjNodeTypes.PRIMARY_CONSTRUCTOR })
        equal(emptyList<ParsingDiagnostic>(), enum.diagnostics)
        val local = parseBoth("class Owner { func target() { Owner(value: Int64) {} }\nfunc afterMember(): Unit {} }\nfunc following(): Unit {}")
        equal(emptyList<Node>(), local.psiNodes.filter { it.type == CjNodeTypes.PRIMARY_CONSTRUCTOR })
        assertFollowing(local)
    }

    private fun assertNameDiagnostic(parsed: Parsed, wrong: String, owner: String) {
        val message = CangJieParsingBundle.message("parsing.error.primary.constructor.name", wrong, owner)
        val start = parsed.text.indexOf(wrong, parsed.text.indexOf('{') + 1)
        truth(parsed.diagnostics.any { it.message == message && it.startOffset == start && it.endOffset == start + wrong.length }, parsed.text)
    }

    private fun assertIdentity(parsed: Parsed, member: String, identity: Identity) {
        val primaries = parsed.psiNodes.filter { it.type == CjNodeTypes.PRIMARY_CONSTRUCTOR }
        equal(if (identity == Identity.PRIMARY) listOf(member) else emptyList(), primaries.map { it.text(parsed.text) }, parsed.text)
        if (identity == Identity.INVALID) {
            equal(listOf(member), parsed.psiNodes.filter { it.type == CjNodeTypes.INVALID_DECLARATION }.map { it.text(parsed.text) }, parsed.text)
            truth(PsiTreeUtil.findChildrenOfType(parsed.file, CjInvalidDeclaration::class.java).single().text == member)
        }
    }

    private fun assertFollowing(parsed: Parsed) {
        for (name in listOf("afterMember", "following")) {
            truth(parsed.psiNodes.any { it.type == CjNodeTypes.FUNC && it.start == parsed.text.indexOf("func $name") }, parsed.text)
        }
    }

    private fun parseBoth(text: String): Parsed {
        val file = createPsiFile("primaryIdentity", text) as CjFile
        val diagnostics = file.collectParsingDiagnostics()
        fun psiNodes(node: com.intellij.lang.ASTNode): List<Node> =
            listOf(Node(node.elementType, node.startOffset, node.startOffset + node.textLength)) +
                generateSequence(node.firstChildNode) { it.treeNext }.flatMap { psiNodes(it).asSequence() }.toList()
        val psi = psiNodes(file.node)
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
        builder.setDebugMode(true)
        // 双入口共享真实源树已冻结的输入；匿名 snapshot 的对象身份不由文本相等替代。
        val input = present(present(file.getCopyableUserData(ROOT_PARSER_SEED_KEY)).lexicalInput)
        val parsedLight = CangJieLightParser.parseBuffered(builder, lexicalInput = input)
        val tree = parsedLight.file.tree
        val lt = mutableListOf<Node>()
        fun visit(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>) {
            lt += Node(node.tokenType, node.startOffset, node.endOffset)
            val ref = Ref<Array<LighterASTNode?>>()
            val count = tree.getChildren(node, ref)
            val children = ref.get() ?: return
            try { for (i in 0 until count) visit(requireNotNull(children[i]), tree) }
            finally { tree.disposeChildren(children, count) }
        }
        visit(tree.root, tree)
        val identities = setOf(CjNodeTypes.PRIMARY_CONSTRUCTOR, CjNodeTypes.INVALID_DECLARATION, CjNodeTypes.FUNC, CjNodeTypes.MACRO)
        equal(psi.filter { it.type in identities }, lt.filter { it.type in identities }, text)
        equal(diagnostics, parsedLight.diagnostics, text)
        return Parsed(text, file, psi, diagnostics)
    }

    private fun source(kind: String, owner: String, member: String): String =
        "$kind $owner {\n$member\nfunc afterMember(): Unit {}\n}\nfunc following(): Unit {}"

    private data class Node(val type: com.intellij.psi.tree.IElementType, val start: Int, val end: Int) {
        fun text(source: String): String = source.substring(start, end)
    }
    private data class Parsed(val text: String, val file: CjFile, val psiNodes: List<Node>, val diagnostics: List<ParsingDiagnostic>)
    private data class Case(val owner: String, val candidate: String, val identity: Identity)
    private enum class Identity { PRIMARY, INVALID, RECOVERED }
    private companion object { const val SIGNATURE = "(value: Int64) { let retained = 1 }" }
}

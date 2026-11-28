package org.cangnova.cangjie.parsing

import com.intellij.lang.ASTNode
import com.intellij.lang.impl.PsiBuilderImpl
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull as present
import kotlin.test.assertTrue

/** 标准 SDK builder 的公开入口也保留本次文法事实，不依赖 CangJiePsiBuilder 强转。 */
class CjSdkParsingPublicationTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun blockEntryPublishesActualBodySeedsOnTheSdkTree() {
        val source = "func f(): Unit { let value = 1 }"
        val tree = CangJieParser.parseBlockCodeFragment(builder(source), seed())
        assertEquals(source, tree.chars.toString())
        val function = tree.descendants().first { it.elementType == CjNodeTypes.FUNC }
        assertEquals(CjmpModifierDeclarationKind.FUNCTION,
            present(function.declarationModifiersForOwnerOrNull()).declarationKind)
        val block = function.descendants().first { it.elementType == CjNodeTypes.BLOCK }
        present(block.requireParserSeed().sourceContext)
        present(block.requireParserSeed().lexicalInput)
    }

    @Test
    fun expressionEntryRetainsTheSdkExpressionStructure() {
        val source = "1 + 2"
        val tree = CangJieParser.parseExpressionCodeFragment(builder(source), seed())
        assertEquals(source, tree.chars.toString())
        assertTrue(tree.descendants().any { it.elementType == CjNodeTypes.BINARY_EXPRESSION })
    }

    @Test
    fun typeEntryRetainsTheSdkTypeStructure() {
        val source = "Array<Int64>"
        val tree = CangJieParser.parseTypeCodeFragment(builder(source), seed())
        assertEquals(source, tree.chars.toString())
        assertTrue(tree.descendants().any { it.elementType == CjNodeTypes.TYPE_REFERENCE })
    }

    @Test
    fun anEmptyNativeMacroRetainsItsActualFileResultCarrier() {
        val entry = CjMacroFragmentContext.Nodes(
            MacroParsingScope.Declaration(CjScopeKind.TOPLEVEL), CjMacroFragmentParent.OTHER, null,
            CjMacroDeclarationPrefix.EMPTY,
        )
        val input = createMacroFragmentParserSeed(ParsingFileFacts(CjSourceKind.SOURCE, CjmpFileRole.NONE),
            CjmpParsingContext.SyntaxOnly, entry)
        val tree = CangJieParser.parseMacroCodeFragment(builder(""), input)
        val contents = present(tree.findChildByType(CjNodeTypes.MACRO_FRAGMENT_CONTENTS))
        val result = present(contents.getCopyableUserData(PARSED_FILE_RESULT_KEY))
        assertTrue(result.sourceText.isEmpty())
        present(result.macroFragmentInfo)
    }

    private fun builder(source: String): PsiBuilderImpl =
        PsiBuilderImpl(null, null, CangJieParserDefinition(), CangJieLexer(), null, source, null, null)

    private fun seed(): CjParserSeed = CjParserSeed(
        CjSourceKind.SOURCE, "", CjmpParsingContext.SyntaxOnly, CjmpFileRole.NONE, 0,
        AbstractCangJieParsing.ParsingContext.DEFAULT.copy(statementScope = CjScopeKind.FUNC_BODY),
    )

    private fun ASTNode.descendants(): Sequence<ASTNode> = sequence {
        yield(this@descendants)
        var child = firstChildNode
        while (child != null) {
            yieldAll(child.descendants())
            child = child.treeNext
        }
    }
}

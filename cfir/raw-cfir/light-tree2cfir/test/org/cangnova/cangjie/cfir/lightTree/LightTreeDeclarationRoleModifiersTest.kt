package org.cangnova.cangjie.cfir.lightTree

import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.parsing.CjmpParsingContext
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@RunWith(JUnit3RunnerWithInners::class)
class LightTreeDeclarationRoleModifiersTest : AbstractLightTree2CfirConverterTestCase() {
    fun testRoleIsTakenOnlyFromDeclarationKeyword() {
        val source = "public class Box { private func member() {} }"
        val parsed = parseLightFile(source)
        val tree = parsed.tree
        val declaration = findNode(tree, tree.root, CjNodeTypes.CLASS)
        val ownOffset = source.indexOf("class")
        val otherOwnerFacts = parsed.declarationModifiers - ownOffset
        assertFailsWith<IllegalStateException> { LightTreeModifierList.from(tree, declaration, otherOwnerFacts) }
        assertTrue(LightTreeModifierList.from(tree, declaration, parsed.declarationModifiers).isVisibilityExplicit)
    }

    fun testMissingDeclarationAndMemberParameterFactsAreErrors() {
        val source = "class Box { Box(public let value: Int64) {} }"
        val parsed = parseLightFile(source)
        val tree = parsed.tree
        val declaration = findNode(tree, tree.root, CjNodeTypes.CLASS)
        val parameter = findNode(tree, declaration, CjNodeTypes.VALUE_PARAMETER)
        assertFailsWith<IllegalStateException> { LightTreeModifierList.from(tree, declaration, emptyMap()) }
        assertFailsWith<IllegalStateException> { LightTreeModifierList.from(tree, parameter, emptyMap(), true) }
        assertTrue(LightTreeModifierList.from(tree, parameter, parsed.declarationModifiers, true).isVisibilityExplicit)
    }

    fun testOrdinaryParameterDoesNotRequireRoleFact() {
        val tree = parseLightTree("func f(value: Int64) {}")
        val parameter = findNode(tree, tree.root, CjNodeTypes.VALUE_PARAMETER)
        assertFalse(LightTreeModifierList.from(tree, parameter, emptyMap(), false).isVisibilityExplicit)
    }

    fun testRecoveredOrdinaryLetParameterDoesNotInventMemberRole() {
        val tree = parseLightTree("func f(public let value: Int64) {}")
        val parameter = findNode(tree, tree.root, CjNodeTypes.VALUE_PARAMETER)
        assertFailsWith<IllegalArgumentException> { LightTreeModifierList.from(tree, parameter, emptyMap()) }
        assertTrue(LightTreeModifierList.from(tree, parameter, emptyMap(), false).isVisibilityExplicit)
    }

    fun testRejectedForeignFunctionLosesInheritedAndExplicitModifiers() {
        fun parse(prefix: String) = CangJieLightParser.parse(
            PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), "$prefix foreign { public func f(): Unit }"),
            cjmpContext = CjmpParsingContext.Compilation(true, false, true),
        )
        val rejectedParse = parse("specific")
        val rejectedDeclaration = findNode(rejectedParse.tree, rejectedParse.tree.root, CjNodeTypes.FUNC)
        val rejected = LightTreeModifierList.from(rejectedParse.tree, rejectedDeclaration, rejectedParse.declarationModifiers)
        assertFalse(rejected.isForeign)
        assertFalse(rejected.isVisibilityExplicit)
        val acceptedParse = parse("common")
        val acceptedDeclaration = findNode(acceptedParse.tree, acceptedParse.tree.root, CjNodeTypes.FUNC)
        val accepted = LightTreeModifierList.from(acceptedParse.tree, acceptedDeclaration, acceptedParse.declarationModifiers)
        assertTrue(accepted.isForeign)
        assertTrue(accepted.isVisibilityExplicit)
    }

    private fun findNode(
        tree: FlyweightCapableTreeStructure<LighterASTNode>,
        root: LighterASTNode,
        type: IElementType,
    ): LighterASTNode {
        if (root.tokenType == type) return root
        for (child in tree.getChildrenArray(root)) {
            if (child == null) break
            val result = findNodeOrNull(tree, child, type)
            if (result != null) return result
        }
        error("Missing node: $type")
    }

    private fun findNodeOrNull(
        tree: FlyweightCapableTreeStructure<LighterASTNode>,
        root: LighterASTNode,
        type: IElementType,
    ): LighterASTNode? {
        if (root.tokenType == type) return root
        for (child in tree.getChildrenArray(root)) {
            if (child == null) break
            findNodeOrNull(tree, child, type)?.let { return it }
        }
        return null
    }
}

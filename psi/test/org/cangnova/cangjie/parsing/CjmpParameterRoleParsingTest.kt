package org.cangnova.cangjie.parsing

import com.intellij.lang.PsiBuilderFactory
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CjmpParameterRoleParsingTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun memberParametersRecordTheirOwnKeywordAndOrdinaryParametersDoNot() {
        val source = "class C { C(ordinary: Int64, common let first: Int64, specific var second: Int64) {} }"
        val first = source.indexOf("let first")
        val second = source.indexOf("var second")
        fun facts(context: CjmpParsingContext): Map<Int, CjmpDeclarationRoleCheck> {
            val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
            return CangJieLightParser.parse(builder, cjmpContext = context).declarationRoleChecks
        }
        val unchecked = facts(CjmpParsingContext.SyntaxOnly)
        assertEquals(CjmpDeclarationRoleCheck.NOT_CHECKED, unchecked[first])
        assertEquals(CjmpDeclarationRoleCheck.NOT_CHECKED, unchecked[second])
        assertNull(unchecked[source.indexOf("ordinary")])
        val checked = facts(CjmpParsingContext.Compilation(false, true, true))
        assertEquals(CjmpDeclarationRoleCheck.REJECTED, checked[first])
        assertEquals(CjmpDeclarationRoleCheck.ACCEPTED, checked[second])
    }

    @Test
    fun emptyModifierListSkipsRoleCheckButNonRoleModifierRunsIt() {
        val source = "class C { C(let first: Int64, public var second: Int64) {} }"
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        val facts = CangJieLightParser.parse(
            builder, cjmpContext = CjmpParsingContext.Compilation(false, true, true),
        ).declarationRoleChecks
        assertEquals(CjmpDeclarationRoleCheck.NOT_CHECKED, facts[source.indexOf("let first")])
        assertEquals(CjmpDeclarationRoleCheck.ACCEPTED, facts[source.indexOf("var second")])
    }
}

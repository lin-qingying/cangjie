package org.cangnova.cangjie.parsing

import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.util.TextRange
import com.intellij.psi.impl.BlockSupportImpl
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.awt.EventQueue
import java.lang.reflect.InvocationTargetException

/** 校验容器关键字坐标与直接成员语法事实，不从后代节点推导默认实现。 */
class CjmpNominalSyntaxFactsParsingTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun directMembersRetainModifiersAndSyntacticBodies() {
        val source = """
            interface Owner {
                common func declared(): Unit
                specific func implemented(): Unit {}
                prop abstractValue: Int64
                prop computed: Int64 { get() { 1 } }
                let plain: Int64
                let initialized: Int64 = 1
            }
        """.trimIndent()
        val members = requireNotNull(parse(source).cjmpNominalFacts[source.indexOf("interface")]).members
        assertEquals(listOf(CjNodeTypes.FUNC, CjNodeTypes.FUNC, CjNodeTypes.PROPERTY,
            CjNodeTypes.PROPERTY, CjNodeTypes.FIELD, CjNodeTypes.FIELD), members.map { it.declarationType })
        assertTrue(members[0].isCommon)
        assertFalse(members[0].isSpecific)
        assertFalse(members[0].hasFunctionBody)
        assertTrue(members[1].isSpecific)
        assertFalse(members[1].isCommon)
        assertTrue(members[1].hasFunctionBody)
        assertFalse(members[2].hasPropertyAccessor)
        assertTrue(members[3].hasPropertyAccessor)
        assertFalse(members[4].hasInitializer)
        assertTrue(members[5].hasInitializer)
    }

    @Test
    fun eachContainerUsesItsOwnKeywordOffset() {
        val source = """
            class Outer {
                func outer(): Unit {}
                class Inner { func inner(): Unit {} }
            }
            interface Face { func required(): Unit }
            struct Value { func value(): Unit {} }
            enum Choice { First | Second }
            extend Value { func extra(): Unit {} }
        """.trimIndent()
        val facts = parse(source).cjmpNominalFacts
        val offsets = listOf("class Outer", "class Inner", "interface Face", "struct Value", "enum Choice", "extend Value")
            .map(source::indexOf)
        assertEquals(offsets.toSet(), facts.keys)
        val outerFunctions = requireNotNull(facts[offsets[0]]).members.filter { it.declarationType == CjNodeTypes.FUNC }
        val innerFunctions = requireNotNull(facts[offsets[1]]).members.filter { it.declarationType == CjNodeTypes.FUNC }
        assertEquals(1, outerFunctions.size)
        assertEquals(1, innerFunctions.size)
        assertTrue(outerFunctions.single().hasFunctionBody)
        assertTrue(innerFunctions.single().hasFunctionBody)
        assertFalse(requireNotNull(facts[offsets[2]]).members.single { it.declarationType == CjNodeTypes.FUNC }.hasFunctionBody)
    }

    @Test
    fun repeatedParsesKeepIndependentSnapshots() {
        val first = "class Owner { func f(): Unit {} }"
        val second = "class Owner { func f(): Unit }"
        val initial = parse(first).cjmpNominalFacts
        val next = parse(second).cjmpNominalFacts
        assertTrue(requireNotNull(initial[0]).members.single().hasFunctionBody)
        assertFalse(requireNotNull(next[0]).members.single().hasFunctionBody)
        assertEquals(initial, parse(first).cjmpNominalFacts)
        assertTrue(requireNotNull(initial[0]).members.single().hasFunctionBody)
    }

    @Test
    fun legacyPlatformModifierFactsDoNotDependOnCompilationVersion() {
        val source = "class Owner { platform func legacy(): Unit {}\nfunc platform(): Unit {} }"
        val legacyCompilation = CjmpParsingContext.Compilation(
            compileCommon = false,
            compileSpecific = true,
            enabled = true,
            platformModifierSyntaxAvailable = true,
        )
        for (context in listOf(CjmpParsingContext.SyntaxOnly, legacyCompilation)) {
            val members = requireNotNull(parse(source, context).cjmpNominalFacts[0]).members
            assertEquals(listOf(CjNodeTypes.FUNC, CjNodeTypes.FUNC), members.map { it.declarationType }, "$context")
            assertTrue(members[0].isPlatform, "$context")
            assertFalse(members[0].isSpecific, "$context")
            assertFalse(members[0].isCommon, "$context")
            assertTrue(members[0].hasFunctionBody, "$context")
            assertFalse(members[1].isSpecific, "$context")
            assertFalse(members[1].isPlatform, "$context")
            assertFalse(members[1].isCommon, "$context")
            assertTrue(members[1].hasFunctionBody, "$context")
        }
        assertEquals(CjmpFileRole.NONE, parse("platform func legacy(): Unit {}").fileFacts.cjmpRole)
        val diagnostics = mutableListOf<ParsingDiagnostic>()
        val sourceWithBoth = "common platform class Owner {}"
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), sourceWithBoth)
        CangJieLightParser.parse(builder, errorListener = { diagnostics += it })
        assertTrue(diagnostics.isEmpty(), diagnostics.toString())
    }

    @Test
    fun modernCompilationDoesNotTreatLegacyPlatformAsModifier() {
        val source = "class Owner { platform func legacy(): Unit {}\nfunc platform(): Unit {} }"
        val context = CjmpParsingContext.Compilation(false, true, true, platformModifierSyntaxAvailable = false)
        val members = requireNotNull(parse(source, context).cjmpNominalFacts[0]).members
        val functions = members.filter { it.declarationType == CjNodeTypes.FUNC }
        assertEquals(2, functions.size)
        assertTrue(functions.all { !it.isSpecific && !it.isCommon && it.hasFunctionBody })
    }

    @Test
    fun nominalFactsParticipateInRollbackAndCollapse() {
        val source = "class First {}\nclass Second {}"
        val builder = SemanticWhitespaceAwarePsiBuilderImpl(CangJieLightParser.createBuilder(source))
        val parser = CangJieParsing.createForTopLevelNonLazy(builder)
        val firstParse = builder.mark()
        parser.parseFile()
        firstParse.rollbackTo()
        parser.parseFile()
        val journal = builder.macroParsingJournal
        val collapse = builder.mark()
        journal.discardCollapsedChildren(0, source.indexOf("class Second"))
        collapse.rollbackTo()
        builder.lightTree
        val data = journal.freeze(0)
        assertEquals(setOf(0, source.indexOf("class Second")), data.nominalFacts.keys)
        assertTrue(data.nominalFacts.values.all { it.members.isEmpty() })
        assertTrue(data.sourceTokens.values.all { requireNotNull(data.lexicalInput).contains(it) })
    }

    @Test
    fun primaryConstructorBodyIsRetainedAsSyntaxFact() {
        val context = CjmpParsingContext.Compilation(
            compileCommon = true, compileSpecific = false, enabled = true,
            primaryConstructorRoleModifiersAvailable = true,
        )
        val source = "common class Owner { common Owner() {} }"
        val facts = requireNotNull(parse(source, context).cjmpNominalFacts[source.indexOf("class")])
        val primary = facts.members.single { it.declarationType == CjNodeTypes.PRIMARY_CONSTRUCTOR }
        assertTrue(primary.isCommon)
        assertTrue(primary.hasFunctionBody)
    }

    @Test
    fun psiKeywordLeavesCarryTheSameFactsAsTheLightParse() {
        val source = "class Outer { class Inner { func f(): Unit {} } }\ninterface Face { func g(): Unit }"
        val file = CjPsiFactory(project, markGenerated = false).createFile(source)
        val expected = parse(source).cjmpNominalFacts
        for (offset in listOf(source.indexOf("class Outer"), source.indexOf("class Inner"), source.indexOf("interface Face"))) {
            val leaf = requireNotNull(file.findElementAt(offset))
            assertEquals(offset, leaf.textOffset)
            assertEquals(requireNotNull(expected[offset]), leaf.node.getCopyableUserData(CJMP_NOMINAL_SYNTAX_FACTS_KEY))
        }
        val copy = file.copy() as CjFile
        for ((offset, facts) in expected) {
            assertEquals(facts, requireNotNull(copy.findElementAt(offset)).node.getCopyableUserData(CJMP_NOMINAL_SYNTAX_FACTS_KEY))
        }
    }

    @Test
    fun incrementalReparseReplacesFactsOnUnchangedKeyword() {
        val oldText = "class Owner { let value: Int64 }\nclass Following {}"
        val insertion = oldText.indexOf(" }")
        val newText = oldText.replaceRange(insertion, insertion, " = 1")
        val file = CjPsiFactory(project, markGenerated = false).createFile(oldText)
        val oldFacts = requireNotNull(requireNotNull(file.findElementAt(0)).node.getCopyableUserData(CJMP_NOMINAL_SYNTAX_FACTS_KEY))
        assertFalse(oldFacts.members.single().hasInitializer)
        val result = BlockSupportImpl.reparse(
            file, file.node, TextRange(insertion, insertion), newText, EmptyProgressIndicator(), oldText,
        )
        assertEquals(oldText, file.text)
        assertEquals(oldFacts, requireNotNull(file.findElementAt(0)).node.getCopyableUserData(CJMP_NOMINAL_SYNTAX_FACTS_KEY))
        try {
            EventQueue.invokeAndWait {
                CommandProcessor.getInstance().executeCommand(project, {
                    ApplicationManager.getApplication().runWriteAction { result.log.doActualPsiChange(file) }
                }, "更新容器语法事实", null)
            }
        } catch (failure: InvocationTargetException) {
            throw failure.targetException
        }
        assertEquals(newText, file.text)
        val updated = requireNotNull(requireNotNull(file.findElementAt(0)).node.getCopyableUserData(CJMP_NOMINAL_SYNTAX_FACTS_KEY))
        assertTrue(updated.members.single().hasInitializer)
        assertFalse(oldFacts.members.single().hasInitializer)
        val expected = parse(newText).cjmpNominalFacts
        assertEquals(expected[0], updated)
        val followingOffset = newText.indexOf("class Following")
        assertEquals(expected[followingOffset], requireNotNull(file.findElementAt(followingOffset)).node.getCopyableUserData(CJMP_NOMINAL_SYNTAX_FACTS_KEY))
    }

    @Test
    fun incrementalPlatformModifierEditsReplaceKeywordSnapshot() {
        val original = "class Owner { func f(): Unit {} }"
        val position = original.indexOf("func f")
        val modified = original.replaceRange(position, position, "platform ")
        val file = CjPsiFactory(project, markGenerated = false).createFile(original)
        fun facts() = requireNotNull(requireNotNull(file.findElementAt(0)).node.getCopyableUserData(CJMP_NOMINAL_SYNTAX_FACTS_KEY))
        fun apply(oldText: String, newText: String, range: TextRange) {
            val result = BlockSupportImpl.reparse(file, file.node, range, newText, EmptyProgressIndicator(), oldText)
            try {
                EventQueue.invokeAndWait {
                    CommandProcessor.getInstance().executeCommand(project, {
                        ApplicationManager.getApplication().runWriteAction { result.log.doActualPsiChange(file) }
                    }, "更新成员修饰符", null)
                }
            } catch (failure: InvocationTargetException) {
                throw failure.targetException
            }
            assertEquals(newText, file.text)
        }
        val before = facts()
        assertFalse(before.members.single().isPlatform)
        apply(original, modified, TextRange(position, position))
        val withPlatform = facts()
        assertTrue(withPlatform.members.single().isPlatform)
        assertFalse(before.members.single().isPlatform)
        assertEquals(parse(modified).cjmpNominalFacts[0], withPlatform)
        apply(modified, original, TextRange(position, position + "platform ".length))
        assertFalse(facts().members.single().isPlatform)
        assertTrue(withPlatform.members.single().isPlatform)
        assertEquals(parse(original).cjmpNominalFacts[0], facts())
    }

    private fun parse(source: String, context: CjmpParsingContext = CjmpParsingContext.SyntaxOnly): ParsedCangJieLightFile {
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        builder.setDebugMode(true)
        return CangJieLightParser.parse(builder, cjmpContext = context)
    }
}
